import re
from difflib import get_close_matches
from extractor.ktp_extractor import is_ktp_document, extract_ktp_header, normalize_key

def clean_text(text: str) -> str:
    return re.sub(r'[^\x00-\x7F]+', '', text).strip()

def postprocess_ocr_text(raw_text: str) -> dict:
    lines = raw_text.splitlines()
    cleaned_text = "\n".join([clean_text(line) for line in lines])
    result = {}

    if is_ktp_document(cleaned_text):
        for line in cleaned_text.splitlines():
            if not line.strip():
                continue

            extract_ktp_header(line, result)

            parts = re.split(r':|\s{2,}', line, maxsplit=1)
            if len(parts) == 2:
                key = normalize_key(parts[0])
                value = parts[1].strip()
                result[key] = value
    else:
        result["raw_text"] = cleaned_text

    return result