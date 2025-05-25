import sys
import cv2
import pytesseract

def preprocess_image(img_path):
    img = cv2.imread(img_path, cv2.IMREAD_GRAYSCALE)
    if img is None:
        sys.exit("Error: Image not found or broken.")

    blur = cv2.bilateralFilter(img, 11, 17, 17)
    thresh = cv2.adaptiveThreshold(blur, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                   cv2.THRESH_BINARY, 11, 2)
    return blur, thresh

def ocr_with_fallback(blur, thresh, config):
    text_blur = pytesseract.image_to_string(blur, config=config)
    text_thresh = pytesseract.image_to_string(thresh, config=config)
    return text_thresh if len(text_thresh) > len(text_blur) else text_blur

def main(image_path):
    blur, thresh = preprocess_image(image_path)
    config = '--oem 3 --psm 6 -l ind'
    result = ocr_with_fallback(blur, thresh, config)
    print(result)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit("ERROR: Please enter the image path as an argument.")
    main(sys.argv[1])
