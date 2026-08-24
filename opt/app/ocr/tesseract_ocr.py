import os
import sys
import shutil
import re
import time
import cv2
import numpy as np
import pytesseract
from postprocessor import postprocess_ocr_text
from extractor.ktp_extractor import normalize_key

# --- Tesseract binary auto-config (Windows + Linux) ---
_tesseract_cmd = os.environ.get("TESSERACT_CMD", "").strip()
if _tesseract_cmd and os.path.isfile(_tesseract_cmd):
    pytesseract.pytesseract.tesseract_cmd = _tesseract_cmd
elif os.name == "nt":
    _candidates = [
        r"C:\Program Files\Tesseract-OCR\tesseract.exe",
        r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe",
        os.path.join(os.environ.get("LOCALAPPDATA", ""), "Programs", "tesseract", "tesseract.exe"),
        os.path.join(os.environ.get("USERPROFILE", ""), "scoop", "apps", "tesseract", "current", "tesseract.exe"),
    ]
    _which = shutil.which("tesseract")
    if _which:
        _candidates.insert(0, _which)
    for _c in _candidates:
        if _c and os.path.isfile(_c):
            pytesseract.pytesseract.tesseract_cmd = _c
            break

for _k in ("TESSDATA_PREFIX", "TESSERACT_DATAPATH"):
    _v = os.environ.get(_k, "").strip()
    if _v:
        os.environ["TESSDATA_PREFIX"] = _v
        break

# --- Wall-clock budget (OCR_TIME_BUDGET, seconds, default 60) ---
def get_time_budget():
    try:
        v = float(os.environ.get("OCR_TIME_BUDGET", "60"))
        return v if v > 0 else 60.0
    except (TypeError, ValueError):
        return 60.0

def deadline_exceeded(deadline):
    """True when the monotonic deadline has passed (None deadline = unlimited)."""
    return deadline is not None and time.monotonic() >= deadline

def ts_timeout(deadline, cap=8):
    """pytesseract timeout capped by remaining budget (min 2s), budget sliced /3 to avoid starving sweep."""
    if deadline is None:
        return cap
    remaining = deadline - time.monotonic()
    # reserve budget across remaining passes; never 1s (tesseract needs >=2s for 1600px)
    return max(2, min(cap, int(max(2, remaining / 3))))

# --- Quality assessment ---
def laplacian_variance(gray):
    return cv2.Laplacian(gray, cv2.CV_64F).var()

def assess_quality(gray):
    blur = laplacian_variance(gray)
    mean = float(np.mean(gray))
    std = float(np.std(gray))
    is_blurry = blur < 100
    is_low_contrast = std < 30
    is_dark_bright = mean < 40 or mean > 220
    return {"blur": blur, "brightness": mean, "contrast": std,
            "needs_preprocess": is_blurry or is_low_contrast or is_dark_bright,
            "needs_sr": blur < 50}

# --- Helper: order 4 points tl,tr,br,bl ---
def order_points(pts):
    rect = np.zeros((4, 2), dtype="float32")
    s = pts.sum(axis=1)
    rect[0] = pts[np.argmin(s)]  # tl smallest sum
    rect[2] = pts[np.argmax(s)]  # br largest sum
    diff = np.diff(pts, axis=1)
    rect[1] = pts[np.argmin(diff)]  # tr
    rect[3] = pts[np.argmax(diff)]  # bl
    return rect

# --- A: Perspective correction ---
def perspective_correction(gray):
    """Detect KTP quadrilateral and warp to rectangle. Returns warped or original."""
    try:
        # Use CLAHE-like contrast boost for edge detection
        blur = cv2.GaussianBlur(gray, (5,5), 0)
        edged = cv2.Canny(blur, 50, 150)
        # Dilate edges to close gaps
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5,5))
        edged = cv2.dilate(edged, kernel, iterations=1)
        contours, _ = cv2.findContours(edged, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        if not contours:
            return gray, False
        # Sort by area descending
        contours = sorted(contours, key=cv2.contourArea, reverse=True)
        h, w = gray.shape[:2]
        img_area = h*w
        for cnt in contours[:5]:
            area = cv2.contourArea(cnt)
            # KTP should be large: 5% - 97% of image (relaxed for phone photos with background)
            if area < img_area*0.05 or area > img_area*0.97:
                continue
            peri = cv2.arcLength(cnt, True)
            approx = cv2.approxPolyDP(cnt, 0.02*peri, True)
            if len(approx) == 4:
                pts = approx.reshape(4,2).astype("float32")
                rect = order_points(pts)
                (tl, tr, br, bl) = rect
                # Compute new size: max width/height
                widthA = np.linalg.norm(br - bl)
                widthB = np.linalg.norm(tr - tl)
                maxW = max(int(widthA), int(widthB))
                heightA = np.linalg.norm(tr - br)
                heightB = np.linalg.norm(tl - bl)
                maxH = max(int(heightA), int(heightB))
                # Keep aspect ~ 1.6 (KTP) but clamp
                maxW = max(maxW, 600)
                maxH = max(maxH, 400)
                # Don't upscale too much
                maxW = min(maxW, w*2)
                maxH = min(maxH, h*2)
                dst = np.array([[0,0],[maxW-1,0],[maxW-1,maxH-1],[0,maxH-1]], dtype="float32")
                M = cv2.getPerspectiveTransform(rect, dst)
                warped = cv2.warpPerspective(gray, M, (maxW, maxH), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE, borderValue=255)
                sys.stderr.write(f"[PERSPECTIVE] corrected 4pts area={area:.0f} -> {maxW}x{maxH}\n")
                return warped, True
        return gray, False
    except Exception as e:
        sys.stderr.write(f"[PERSPECTIVE] failed: {e}\n")
        return gray, False

# --- B: Hough large-angle deskew (10-45) + minAreaRect for small ---
def deskew_hough(gray):
    """Hough line based deskew for large angles 10-45 deg, fallback to minAreaRect."""
    try:
        _, binary = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
        # Hough path for large angles
        # Detect horizontal-ish lines (text lines)
        edges = cv2.Canny(binary, 50, 150, apertureSize=3)
        lines = cv2.HoughLinesP(edges, 1, np.pi/180, threshold=100, minLineLength=gray.shape[1]*0.4, maxLineGap=10)
        if lines is not None and len(lines) >= 3:
            angles = []
            for x1,y1,x2,y2 in lines[:,0]:
                angle = np.degrees(np.arctan2(y2 - y1, x2 - x1))
                # Normalize to -45..45
                if angle < -45: angle += 90
                if angle > 45: angle -= 90
                # Only consider near-horizontal lines (-45 to 45, but close to 0)
                if abs(angle) < 45:
                    angles.append(angle)
            if len(angles) >= 3:
                # Use median for robustness
                median_angle = float(np.median(angles))
                # Also compute std to check consistency
                std = float(np.std(angles))
                if 0.5 < abs(median_angle) < 45 and std < 10:
                    (h, w) = gray.shape[:2]
                    center = (w // 2, h // 2)
                    M = cv2.getRotationMatrix2D(center, median_angle, 1.0)
                    corrected = cv2.warpAffine(gray, M, (w, h), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE, borderValue=255)
                    sys.stderr.write(f"[DESKEW-HOUGH] angle={median_angle:.2f} std={std:.2f} lines={len(angles)}\n")
                    return corrected, median_angle
        # Fallback to minAreaRect for small angles 0.5-45
        coords = np.column_stack(np.where(binary > 0))
        if len(coords) >= 100:
            rect = cv2.minAreaRect(coords)
            angle = rect[-1]
            if angle < -45:
                angle += 90
            if 0.5 < abs(angle) < 45:
                (h, w) = gray.shape[:2]
                center = (w // 2, h // 2)
                M = cv2.getRotationMatrix2D(center, angle, 1.0)
                sys.stderr.write(f"[DESKEW-RECT] angle={angle:.2f}\n")
                return cv2.warpAffine(gray, M, (w, h), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE, borderValue=255), angle
        return gray, 0.0
    except Exception as e:
        sys.stderr.write(f"[DESKEW] failed: {e}\n")
        return gray, 0.0

# --- C: OSD orientation 0/90/180/270 ---
def correct_orientation(gray, lang="ind", deadline=None):
    """Detect 90/180/270 rotation via Tesseract OSD. Brute-force guarded: skip if blurry or budget low."""
    try:
        if deadline_exceeded(deadline):
            sys.stderr.write("[OSD] budget exhausted before orientation check, skip\n")
            return gray, 0
        # Guard: if very blurry, OSD is unreliable and brute will waste budget -> skip
        try:
            bv = laplacian_variance(gray)
            if bv < 50:
                sys.stderr.write(f"[OSD] skip brute, too blurry (var={bv:.1f})\n")
                # still try single OSD, but no brute
                try:
                    osd = pytesseract.image_to_osd(gray, output_type=pytesseract.Output.DICT, config='--psm 0', timeout=ts_timeout(deadline, cap=6))
                    angle = int(osd.get('orientation', 0))
                    conf = float(osd.get('orientation_conf', 0))
                    sys.stderr.write(f"[OSD] angle={angle} conf={conf:.1f} (blurry guard)\n")
                    if angle != 0 and conf > 3.0:
                        if angle == 90: gray = cv2.rotate(gray, cv2.ROTATE_90_CLOCKWISE)
                        elif angle == 180: gray = cv2.rotate(gray, cv2.ROTATE_180)
                        elif angle == 270: gray = cv2.rotate(gray, cv2.ROTATE_90_COUNTERCLOCKWISE)
                        return gray, angle
                except Exception as e:
                    sys.stderr.write(f"[OSD] pytesseract failed (blurry): {e}\n")
                return gray, 0
        except Exception:
            pass
        # Try OSD via pytesseract
        osd_confident_upright = False
        try:
            osd = pytesseract.image_to_osd(gray, output_type=pytesseract.Output.DICT, config='--psm 0', timeout=ts_timeout(deadline, cap=6))
            # osd dict: 'orientation': 0/90/180/270, 'orientation_conf': float
            angle = int(osd.get('orientation', 0))
            conf = float(osd.get('orientation_conf', 0))
            sys.stderr.write(f"[OSD] angle={angle} conf={conf:.1f}\n")
            if angle != 0 and conf > 2.0:  # threshold low because ind OSD weak
                if angle == 90:
                    gray = cv2.rotate(gray, cv2.ROTATE_90_CLOCKWISE)
                elif angle == 180:
                    gray = cv2.rotate(gray, cv2.ROTATE_180)
                elif angle == 270:
                    gray = cv2.rotate(gray, cv2.ROTATE_90_COUNTERCLOCKWISE)
                return gray, angle
            if angle == 0 and conf > 2.0:
                # OSD confidently says the image is upright: no brute-force needed.
                osd_confident_upright = True
        except Exception as e:
            sys.stderr.write(f"[OSD] pytesseract failed: {e}\n")
        if osd_confident_upright:
            return gray, 0
        # Fallback brute-force: only if we have >15s budget left, and max 2 candidates (90,180) not 3
        remaining = (deadline - time.monotonic()) if deadline else 60
        if remaining < 15:
            sys.stderr.write(f"[OSD-BRUTE] skip, remaining {remaining:.1f}s <15s\n")
            return gray, 0
        h, w = gray.shape[:2]
        best = gray
        best_len = 0
        best_angle = 0
        thumb = cv2.resize(gray, (400, int(400*h/w))) if w>h else cv2.resize(gray, (int(400*w/h), 400))
        brute_attempts = 0
        for ang, code in [(90, cv2.ROTATE_90_CLOCKWISE), (180, cv2.ROTATE_180)]:  # 270 rarely needed for KTP
            if deadline_exceeded(deadline):
                sys.stderr.write(f"[OSD-BRUTE] deadline reached after {brute_attempts} candidate(s), keep best so far\n")
                break
            brute_attempts += 1
            try:
                rot = cv2.rotate(thumb, code)
                txt = pytesseract.image_to_string(rot, config='--oem 1 --psm 6 -l ind', timeout=ts_timeout(deadline, cap=5))
                l = len(txt.strip())
                if l > best_len and l > 20:
                    best_len = l
                    best_angle = ang
            except Exception:
                pass
        # require stronger evidence: len>40 (was 30)
        if best_angle != 0 and best_len > 40:
            sys.stderr.write(f"[OSD-BRUTE] chose angle={best_angle} len={best_len}\n")
            if best_angle == 90:
                return cv2.rotate(gray, cv2.ROTATE_90_CLOCKWISE), 90
            elif best_angle == 180:
                return cv2.rotate(gray, cv2.ROTATE_180), 180
        return gray, 0
    except Exception as e:
        sys.stderr.write(f"[OSD] failed: {e}\n")
        return gray, 0

def upscale_if_needed(gray, threshold=1000):
    h, w = gray.shape[:2]
    if w < threshold or h < threshold // 2:
        scale = max(2.0, threshold / max(w, h))
        scale = min(scale, 2.5)
        return cv2.resize(gray, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)
    return gray

def preprocess_image_v2(img_path, apply_sr=False, deadline=None):
    img = cv2.imread(img_path, cv2.IMREAD_GRAYSCALE)
    if img is None:
        sys.exit("Error: Image not found or broken.")

    q = assess_quality(img)
    # 0. Perspective correction first (A) - before upscale to keep coords accurate
    # Only if enabled and image large enough
    if os.environ.get("OCR_PERSPECTIVE_ENABLED", "true").lower() != "false":
        img, persp_done = perspective_correction(img)
    else:
        persp_done = False

    # 0b. Orientation 90/180/270 (C) - before upscale
    if os.environ.get("OCR_OSD_ENABLED", "true").lower() != "false":
        img, osd_angle = correct_orientation(img, deadline=deadline)
    else:
        osd_angle = 0

    # upscale for DPI 300
    img = upscale_if_needed(img)

    # optional super-resolution for extreme blur (P2.7) — capped to avoid 3200px blowup
    if apply_sr or q["needs_sr"]:
        try:
            h, w = img.shape[:2]
            sr_model_path = os.environ.get("ESRGAN_MODEL", "")
            if sr_model_path and os.path.isfile(sr_model_path):
                sr = cv2.dnn_superres.DnnSuperResImpl_create()
                sr.readModel(sr_model_path)
                sr.setModel("edsr", 2)
                img = sr.upsample(img)
            else:
                # For large images (>1500px), don't 2x upscale — just sharpen in place
                if max(h, w) > 1500:
                    kernel = np.array([[0,-1,0],[-1,5,-1],[0,-1,0]])
                    img = cv2.filter2D(img, -1, kernel)
                    # light contrast boost instead of size doubling
                    img = cv2.convertScaleAbs(img, alpha=1.1, beta=5)
                elif max(h, w) > 1000:
                    # moderate 1.5x for medium images
                    img = cv2.resize(img, None, fx=1.5, fy=1.5, interpolation=cv2.INTER_CUBIC)
                    kernel = np.array([[0,-1,0],[-1,5,-1],[0,-1,0]])
                    img = cv2.filter2D(img, -1, kernel)
                else:
                    img = cv2.resize(img, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)
                    kernel = np.array([[0,-1,0],[-1,5,-1],[0,-1,0]])
                    img = cv2.filter2D(img, -1, kernel)
            # cap max dimension to 2200 to keep tesseract fast
            h2, w2 = img.shape[:2]
            if max(h2, w2) > 2200:
                scale = 2200 / max(h2, w2)
                img = cv2.resize(img, None, fx=scale, fy=scale, interpolation=cv2.INTER_AREA)
        except Exception as e:
            sys.stderr.write(f"SR failed: {e}\n")

    # CLAHE for uneven light
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
    img = clahe.apply(img)

    # Denoise preserve edges
    img = cv2.fastNlMeansDenoising(img, None, 10, 7, 21)

    # B: Hough deskew 10-45 + small angle
    max_angle = float(os.environ.get("OCR_DESKEW_MAX_ANGLE", "45"))
    img, deskew_angle = deskew_hough(img)
    # Clamp: if detected > max_angle, revert (should not happen due to check)
    if abs(deskew_angle) > max_angle:
        sys.stderr.write(f"[DESKEW] angle {deskew_angle:.1f} > max {max_angle}, revert\n")
        # revert not easy, keep as is for now

    # Adaptive vs OTSU by quality
    q2 = assess_quality(img)
    if q2["contrast"] < 30 or q2["blur"] < 100:
        thresh = cv2.adaptiveThreshold(img, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 10)
    else:
        blur = cv2.GaussianBlur(img, (3,3), 0)
        _, thresh = cv2.threshold(blur, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (2,2))
    opened = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel)
    bordered = cv2.copyMakeBorder(opened, 10,10,10,10, cv2.BORDER_CONSTANT, value=255)

    blur_version = cv2.bilateralFilter(img, 11, 17, 17)
    adaptive_blur = cv2.adaptiveThreshold(blur_version, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)

    return {
        "primary": bordered,
        "adaptive": adaptive_blur,
        "clahe": img,
        "blur": q2["blur"],
        "brightness": q2["brightness"],
        "contrast": q2["contrast"],
        "perspective": persp_done,
        "osd_angle": osd_angle,
        "deskew_angle": deskew_angle
    }

def correct_nik_typos(text):
    lines = []
    for line in text.splitlines():
        if re.search(r'NIK|nik', line):
            m = re.search(r'([\dOIlBZS]{14,18})', line)
            if m:
                raw = m.group(1)
                fixed = raw.replace('O','0').replace('o','0').replace('I','1').replace('l','1').replace('L','1').replace('B','8').replace('S','5').replace('Z','2')
                fixed = re.sub(r'[^0-9]', '', fixed)
                if len(fixed) >= 16:
                    fixed = fixed[:16]
                line = line.replace(raw, fixed)
        lines.append(line)
    return "\n".join(lines)

def score_text(text):
    if not text or not text.strip():
        return 0
    alpha_num = sum(1 for c in text if c.isalnum() or c in ":/")
    ratio = alpha_num / max(1, len(text))
    len_score = min(20, len(text)/5)
    noisy = sum(1 for l in text.splitlines() if 0 < len(l.strip()) <= 2)
    return ratio*70 + len_score - noisy*2

def ocr_with_confidence(img, config, deadline=None):
    """One OCR pass. Timeout generous enough for 1600px; budget checked before call."""
    def _timeout():
        if deadline is None: return 12
        remaining = deadline - time.monotonic()
        # if <3s left, give whatever remains (min 3s), else 10s
        if remaining < 4:
            return max(3, int(remaining))
        return min(12, int(remaining))
    try:
        data = pytesseract.image_to_data(img, config=config, output_type=pytesseract.Output.DICT, timeout=_timeout())
        confs = [int(c) for c in data['conf'] if int(c) != -1]
        avg_conf = sum(confs)/len(confs) if confs else 0
        text = pytesseract.image_to_string(img, config=config, timeout=_timeout())
        return text, avg_conf
    except Exception:
        try:
            text = pytesseract.image_to_string(img, config=config, timeout=_timeout())
            return text, score_text(text)
        except Exception:
            return "", 0

def ocr_multi_psm(images, lang="ind", deadline=None):
    """Sweep image variants x PSM modes. Limited to 4 passes max to fit budget: primary[6,3] + clahe[6,3].
    Early-exit on high confidence. Bounded by deadline."""
    # trimmed to 2 PSM x 2 variants = 4 passes (was 15 passes) to allow 10s per pass within 60s
    variants = [("primary", [6, 3]), ("clahe", [6, 3])]
    # include adaptive only if primary failed and budget remains
    best_text, best_conf, best_psm = "", -1, 6
    oem = 1
    passes = 0
    early_exit = False
    budget_hit = False
    for img_key, psms in variants:
        img = images.get(img_key)
        if img is None:
            continue
        for psm in psms:
            if deadline_exceeded(deadline):
                budget_hit = True
                break
            cfg = f'--oem {oem} --psm {psm} -l {lang} -c preserve_interword_spaces=1 -c user_defined_dpi=300'
            passes += 1
            text, conf = ocr_with_confidence(img, cfg, deadline=deadline)
            if conf < 10:
                conf = score_text(text)
            text_corr = correct_nik_typos(text)
            if any(k in text_corr.upper() for k in ["NIK","PROVINSI","KABUPATEN"]):
                conf += 5
            if conf > best_conf and len(text_corr.strip()) > 5:
                best_conf, best_text, best_psm = conf, text_corr, psm
            if best_conf > 85 and len(best_text) > 50:
                early_exit = True
                break
        if early_exit or best_conf > 85:
            break
    # fallback adaptive only if nothing good yet and budget left >10s
    if not early_exit and best_conf < 60 and not deadline_exceeded(deadline):
        remaining = (deadline - time.monotonic()) if deadline else 60
        if remaining > 10 and images.get("adaptive") is not None:
            for psm in [6]:
                if deadline_exceeded(deadline): break
                cfg = f'--oem {oem} --psm {psm} -l {lang} -c preserve_interword_spaces=1 -c user_defined_dpi=300'
                passes += 1
                text, conf = ocr_with_confidence(images["adaptive"], cfg, deadline=deadline)
                if conf < 10: conf = score_text(text)
                text_corr = correct_nik_typos(text)
                if any(k in text_corr.upper() for k in ["NIK","PROVINSI","KABUPATEN"]):
                    conf += 5
                if conf > best_conf and len(text_corr.strip()) > 5:
                    best_conf, best_text, best_psm = conf, text_corr, psm
    stats = {"passes": passes, "early_exit": early_exit, "budget_hit": budget_hit}
    return best_text, best_conf, best_psm, stats

def main(image_path):
    t0 = time.monotonic()
    budget = get_time_budget()
    deadline = t0 + budget

    tmp = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
    q0 = assess_quality(tmp) if tmp is not None else {"needs_sr": False, "blur": 0}
    need_sr = q0.get("blur", 100) < 50

    images = preprocess_image_v2(image_path, apply_sr=need_sr, deadline=deadline)
    text, conf, psm, sweep = ocr_multi_psm(images, deadline=deadline)

    if not text.strip():
        # Emergency single pass: use remaining budget directly (not sliced)
        remaining = int(max(5, (deadline - time.monotonic()) if deadline else 20))
        try:
            text = pytesseract.image_to_string(
                images["primary"], config='--oem 1 --psm 6 -l ind',
                timeout=remaining)
        except Exception:
            text = ""

    text = correct_nik_typos(text)
    data_text = postprocess_ocr_text(text)

    elapsed = time.monotonic() - t0
    sys.stderr.write(f"[OCR] blur={images['blur']:.1f} brightness={images['brightness']:.1f} contrast={images['contrast']:.1f} conf={conf:.1f} psm={psm} sr={need_sr} persp={images.get('perspective')} osd={images.get('osd_angle')} deskew={images.get('deskew_angle'):.1f}\n")
    sys.stderr.write(f"[BUDGET] elapsed={elapsed:.1f}s passes={sweep['passes']} early_exit={'true' if sweep['early_exit'] else 'false'} budget_hit={'true' if sweep['budget_hit'] else 'false'} limit={budget:.0f}s\n")

    for key, value in data_text.items():
        print(f"{key}: {value}")
    if "raw_text" not in data_text and len(data_text)==0:
        print(text)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit("ERROR: Please enter the image path as an argument.")
    main(sys.argv[1])
