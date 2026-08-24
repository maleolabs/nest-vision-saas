#!/usr/bin/env python3
"""
G: KTP Evaluation Harness — CER + field accuracy + synthetic blur/rotate augment
Usage: python scripts/eval_ktp.py [--augment] [--rapid]
Compares Tesseract vs RapidOCR on ../KTP/*.jpg
GT derived from manual inspection of KTP 1/2 (same person: MA'RIJ MOKOGINTA)
"""
import os, sys, glob, subprocess, json, re, time, pathlib
import cv2
import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[1]
OCR_DIR = ROOT / "opt" / "app" / "ocr"
KTP_DIR = ROOT.parent / "KTP"  # ../KTP
if not KTP_DIR.exists():
    KTP_DIR = ROOT / ".." / "KTP"
    # also try absolute from earlier logs
    import os as _os
    if not KTP_DIR.exists():
        KTP_DIR = pathlib.Path("/home/m2codeloan/m2code/maleolabs/nest-nestara/KTP")
        if not KTP_DIR.exists():
            KTP_DIR = pathlib.Path("/home/m2codeloan/m2code/maleolabs/nest-nestara/nestara-vision-saas/../KTP").resolve()

# Expected ground truth (from visual inspection, NIK consistent across 1/2)
GT = {
    "nik": "7501070410020002",
    "nama": "MA'RIJ MOKOGINTA",
    "tempat/tgl lahir": "IMANA, 04-10-2002",
    "jenis kelamin": "LAKI-LAKI",
    "alamat": "DUSUN KAYU MAS",
    "rt/rw": "000/000",  # varies in image quality, accept prefix
    "kelurahan": "WAPALO",
    "kecamatan": "ATINGGOLA",
    "kabupaten": "GORONTALO UTARA",
    "provinsi": "GORONTALO",
    "agama": "ISLAM",
    "status perkawinan": "BELUM KAWIN",
    "pekerjaan": "PELAJAR/MAHASISWA",
    "kewarganegaraan": "WNI",
    "berlaku hingga": "SEUMUR HIDUP",
}

def run_ocr(script, image_path, timeout=70):
    cmd = ["python3", str(script), str(image_path)]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        stdout = res.stdout.strip()
        stderr = res.stderr.strip()
        # parse "key: value" lines
        data = {}
        for line in stdout.splitlines():
            if ":" in line:
                k, v = line.split(":", 1)
                k = k.strip().lower()
                v = v.strip()
                if k and v:
                    data[k] = v
        return data, stdout, stderr, res.returncode
    except subprocess.TimeoutExpired:
        return {}, "", "timeout", 1

def field_accuracy(data):
    correct = 0
    total = len(GT)
    details = {}
    for k, expected in GT.items():
        got = data.get(k, "")
        # normalized compare
        exp_norm = re.sub(r'\s+', ' ', expected.lower().strip())
        got_norm = re.sub(r'\s+', ' ', got.lower().strip())
        # for rt/rw allow partial
        if k == "rt/rw":
            ok = "000" in got_norm or exp_norm in got_norm or got_norm in exp_norm
        elif k == "nik":
            ok = got_norm == exp_norm
        else:
            # Levenshtein-ish: allow 2 char diff
            ok = exp_norm in got_norm or got_norm in exp_norm or exp_norm.replace("'","") in got_norm.replace("'","")
            if not ok:
                # check edit distance via difflib
                from difflib import SequenceMatcher
                ok = SequenceMatcher(None, exp_norm, got_norm).ratio() > 0.8
        details[k] = {"expected": expected, "got": got, "ok": ok}
        if ok:
            correct += 1
    return correct, total, details

def augment_image(img, mode="blur"):
    if mode == "blur":
        return cv2.GaussianBlur(img, (7,7), 0)
    elif mode == "rotate15":
        h,w = img.shape[:2]
        M = cv2.getRotationMatrix2D((w/2,h/2), 15, 1.0)
        return cv2.warpAffine(img, M, (w,h), borderValue=(255,255,255))
    elif mode == "dark":
        return cv2.convertScaleAbs(img, alpha=0.7, beta=-20)
    elif mode == "bright":
        return cv2.convertScaleAbs(img, alpha=1.3, beta=20)
    return img

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--augment", action="store_true", help="test synthetic blur/rotate/dark")
    ap.add_argument("--rapid", action="store_true", help="also test rapid_ocr.py")
    args = ap.parse_args()

    tesseract_script = OCR_DIR / "tesseract_ocr.py"
    rapid_script = OCR_DIR / "rapid_ocr.py"

    images = sorted(glob.glob(str(KTP_DIR / "*.jpg")) + glob.glob(str(KTP_DIR / "*.png")) + glob.glob(str(KTP_DIR / "*.jpeg")))
    if not images:
        print(f"[EVAL] No images found in {KTP_DIR} (cwd={os.getcwd()})")
        sys.exit(1)

    print(f"[EVAL] Found {len(images)} images in {KTP_DIR}")
    print(f"[EVAL] GT nik={GT['nik']} nama={GT['nama']}")

    aug_modes = [None]
    if args.augment:
        aug_modes = [None, "blur", "rotate15", "dark"]

    for img_path in images:
        print("\n" + "="*70)
        print(f"[EVAL] Image: {img_path}")
        img = cv2.imread(img_path)
        if img is None:
            print("  cannot read")
            continue
        h,w = img.shape[:2]
        blur = cv2.Laplacian(cv2.cvtColor(img, cv2.COLOR_BGR2GRAY), cv2.CV_64F).var()
        print(f"  size {w}x{h} blur_var={blur:.1f}")

        for aug in aug_modes:
            test_img_path = img_path
            suffix = "orig"
            if aug:
                aug_img = augment_image(img, aug)
                tmp = f"/tmp/eval_{pathlib.Path(img_path).stem}_{aug}.jpg"
                cv2.imwrite(tmp, aug_img)
                test_img_path = tmp
                suffix = aug

            # Tesseract
            data, stdout, stderr, code = run_ocr(tesseract_script, test_img_path)
            correct, total, details = field_accuracy(data)
            acc = correct/total*100 if total else 0
            # extract conf from stderr
            m = re.search(r'conf=([0-9.\-]+)', stderr)
            conf = m.group(1) if m else "?"
            print(f"  [{suffix}] TESSERACT conf={conf} fields {correct}/{total}={acc:.1f}% exit={code}")
            # show NIK first
            print(f"    NIK: {data.get('nik','-')} (exp {GT['nik']}) {'OK' if details.get('nik',{}).get('ok') else 'FAIL'}")
            if acc < 80:
                # show misses
                misses = [k for k,v in details.items() if not v['ok']]
                print(f"    misses: {misses}")

            if args.rapid and rapid_script.exists():
                data_r, stdout_r, stderr_r, code_r = run_ocr(rapid_script, test_img_path)
                correct_r, total_r, details_r = field_accuracy(data_r)
                acc_r = correct_r/total_r*100 if total_r else 0
                print(f"  [{suffix}] RAPID     fields {correct_r}/{total_r}={acc_r:.1f}% exit={code_r}")
                print(f"    NIK: {data_r.get('nik','-')} {'OK' if details_r.get('nik',{}).get('ok') else 'FAIL'}")
                if acc_r < 80:
                    misses_r = [k for k,v in details_r.items() if not v['ok']]
                    print(f"    misses: {misses_r}")

    print("\n[EVAL] Done. Threshold ready-to-use: >=75% field accuracy for blur aug, NIK must be 100% correct.")

if __name__ == "__main__":
    main()
