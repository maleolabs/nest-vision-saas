"""
RapidOCR fallback for Ensemble. Install: pip install rapidocr_onnxruntime pyclipper shapely onnxruntime opencv-python-headless
Usage: python rapid_ocr.py /path/to/image.png
Outputs key: value lines similar to tesseract_ocr.py for postprocessor compatibility.
"""
import sys
import os

try:
    from rapidocr_onnxruntime import RapidOCR
    import cv2
    HAS_RAPID = True
except ImportError as e:
    HAS_RAPID = False
    print(f"ERROR: rapidocr not installed: {e}. Run pip install rapidocr_onnxruntime pyclipper shapely onnxruntime")
    sys.exit(1)

# singleton
ocr_engine = None
def get_ocr():
    global ocr_engine
    if ocr_engine is None:
        # latin handles Indonesian; det+cls+rec
        ocr_engine = RapidOCR(lang="latin")
    return ocr_engine

def main(image_path):
    if not os.path.isfile(image_path):
        print(f"ERROR: file not found {image_path}")
        sys.exit(1)
    engine = get_ocr()
    result, elapse = engine(image_path)
    # result: list of [box, text, score]
    lines = []
    for box, text, score in result if result else []:
        # filter very low conf
        if score is not None and float(score) < 0.3:
            continue
        lines.append(text)
    # Rapid gives tokens without ":". Reconstruct with spaces for postprocessor
    # But better keep original detection order (already top-to-bottom due to sort)
    text = "\n".join(lines)
    # Heuristic: Rapid splits "NIK" and number on separate lines -> join them for NIK detection
    # We'll also add a synthetic "NIK: <number>" line if we detect 16-digit isolated
    try:
        import re
        # find 16-digit candidate
        m = re.search(r'\b\d{16}\b', text.replace(" ", ""))
        # also search in lines
        for i, l in enumerate(lines):
            cleaned = re.sub(r'[^0-9]', '', l)
            if len(cleaned) == 16:
                # ensure NIK line exists
                if "NIK" not in text:
                    lines.insert(i, "NIK: " + cleaned)
                    text = "\n".join(lines)
                break
        # Heuristic 2: merge key on one line + value on next line (Rapid splits "NIK" and "7501..." )
        from extractor.ktp_extractor import EXPECTED_KEYS
        # normalize set for quick lookup
        expected_norm = {k.lower(): k for k in EXPECTED_KEYS}
        # also alias without slash/space
        expected_nospace = {k.lower().replace(" ", "").replace("/", "").replace(".", ""): k for k in EXPECTED_KEYS}
        merged = []
        i = 0
        while i < len(lines):
            cur = lines[i].strip()
            cur_low = cur.lower().strip()
            cur_nospace = cur_low.replace(" ", "").replace("/", "").replace(".", "")
            # is cur a key?
            is_key = cur_low in expected_norm or cur_nospace in expected_nospace
            # also check colon already
            has_delim = ":" in cur or ";" in cur or "·" in cur or "|" in cur
            if is_key and not has_delim and i + 1 < len(lines):
                nxt = lines[i+1].strip()
                nxt_low = nxt.lower().strip()
                nxt_nospace = nxt_low.replace(" ", "").replace("/", "").replace(".", "")
                nxt_is_key = nxt_low in expected_norm or nxt_nospace in expected_nospace or ":" in nxt
                if not nxt_is_key and len(nxt) > 0:
                    # find canonical key
                    canon = expected_norm.get(cur_low) or expected_nospace.get(cur_nospace) or cur
                    merged.append(f"{canon}: {nxt}")
                    i += 2
                    continue
            merged.append(cur)
            i += 1
        lines = merged
        text = "\n".join(lines)
        # Heuristic 3: fix "PROVINSIGORONTALO" -> "PROVINSI: GORONTALO" (no delimiter)
        new_lines = []
        for l in lines:
            low = l.lower().replace(" ", "")
            replaced = False
            for k in EXPECTED_KEYS:
                k_nospace = k.lower().replace(" ", "").replace("/", "")
                if low.startswith(k_nospace) and len(l) > len(k):
                    if ":" not in l and ";" not in l:
                        val = l[len(k):].strip()
                        new_lines.append(f"{k}: {val}" if val else l)
                        replaced = True
                        break
            if not replaced:
                new_lines.append(l)
        if len(new_lines) != len(lines):
            text = "\n".join(new_lines)
            lines = new_lines
    except Exception as e:
        sys.stderr.write(f"rapid post-heuristic failed: {e}\n")

    # Reuse postprocessor for KTP
    try:
        from postprocessor import postprocess_ocr_text
        data = postprocess_ocr_text(text)
        for k, v in data.items():
            print(f"{k}: {v}")
        if not data or "raw_text" in data:
            if not data:
                print(text)
    except Exception as e:
        sys.stderr.write(f"postprocess failed: {e}\n")
        print(text)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python rapid_ocr.py <image_path>")
        sys.exit(1)
    main(sys.argv[1])
