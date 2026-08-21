"""
PaddleOCR fallback for Ensemble. Install: pip install paddlepaddle paddleocr
Usage: python paddle_ocr.py /path/to/image.png
Outputs key: value lines similar to tesseract_ocr.py
"""
import sys
import os
try:
    from paddleocr import PaddleOCR
    import cv2
    HAS_PADDLE = True
except ImportError:
    HAS_PADDLE = False
    print("ERROR: paddleocr not installed. Run pip install paddlepaddle paddleocr")
    sys.exit(1)

# singleton
ocr = None
def get_ocr():
    global ocr
    if ocr is None:
        # use_ind for Indonesian, but Paddle multilingual handles it
        ocr = PaddleOCR(use_angle_cls=True, lang='latin', show_log=False, use_gpu=False)
    return ocr

def main(image_path):
    if not os.path.isfile(image_path):
        print(f"ERROR: file not found {image_path}")
        sys.exit(1)
    engine = get_ocr()
    result = engine.ocr(image_path, cls=True)
    # result is list of pages -> list of [box, (text, conf)]
    lines = []
    for page in result:
        if not page:
            continue
        for box, (text, conf) in page:
            # conf is 0-1
            lines.append(text)
    text = "\n".join(lines)
    # Try to reuse postprocessor for KTP if available
    try:
        from postprocessor import postprocess_ocr_text
        data = postprocess_ocr_text(text)
        for k,v in data.items():
            print(f"{k}: {v}")
        if not data or "raw_text" in data:
            # also print raw
            if not data:
                print(text)
    except Exception as e:
        print(text)
        sys.stderr.write(f"postprocess failed: {e}\n")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python paddle_ocr.py <image_path>")
        sys.exit(1)
    main(sys.argv[1])
