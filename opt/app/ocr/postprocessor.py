import re
from difflib import get_close_matches
from extractor.ktp_extractor import is_ktp_document, extract_ktp_header, normalize_key, EXPECTED_KEYS, extract_nik_regex

def clean_text(text: str) -> str:
    # keep printable, strip excessive garbage but preserve Indonesian chars
    # remove non-ascii control but keep common punctuation
    lines = []
    for line in text.splitlines():
        # replace common OCR artifacts: | -> I, but keep
        # remove only control chars, keep extended ascii for é etc
        line = re.sub(r'[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]', '', line)
        # normalize multiple spaces
        line = re.sub(r'\s+', ' ', line).strip()
        # skip pure noise lines (single char)
        if len(line) <= 1 and line not in EXPECTED_KEYS:
            continue
        lines.append(line)
    return "\n".join(lines).strip()

def correct_common_typos(value: str, key: str) -> str:
    # P1.4 / P2.8 typo correction per field
    if "nik" in key.lower():
        # NIK should be 16 digits
        value = value.replace('O','0').replace('o','0').replace('I','1').replace('l','1').replace('L','1').replace('B','8').replace('S','5').replace('Z','2').replace(' ','').replace('-','')
        value = re.sub(r'[^0-9]', '', value)
        if len(value) > 16:
            value = value[:16]
    return value.strip()

def split_robust(line: str):
    # Try multiple delimiters: colon, semicolon, bullet, double-space, dash surrounded
    # First try explicit delimiters
    for pat in [r'\s*:\s*', r'\s*;\s*', r'\s*·\s*', r'\s*\|\s*', r'\s{2,}', r'\s+-\s+']:
        parts = re.split(pat, line, maxsplit=1)
        if len(parts) == 2 and len(parts[0]) > 1 and len(parts[1]) > 0:
            # validate key looks like key (not too long numeric)
            if len(parts[0]) < 30:
                return parts
    # Fallback: no delimiter, try to infer key via fuzzy at start of line
    lower = line.lower()
    for exp in EXPECTED_KEYS:
        if lower.startswith(exp) or lower.startswith(exp.replace(" ", "")):
            # key without delimiter: e.g. "NIK 3201234567890123"
            rest = line[len(exp):].strip(" :.-\t")
            if rest:
                return [exp, rest]
    return None

def postprocess_ocr_text(raw_text: str) -> dict:
    cleaned_text = clean_text(raw_text)
    result = {}

    if is_ktp_document(cleaned_text):
        # Also try global NIK regex as fallback
        nik = extract_nik_regex(cleaned_text)
        if nik:
            result["nik"] = nik

        for line in cleaned_text.splitlines():
            if not line.strip():
                continue
            # header extraction (PROVINSI etc)
            extract_ktp_header(line, result)

            split = split_robust(line)
            if split:
                key_raw, value = split[0], split[1]
                key = normalize_key(key_raw)
                value = correct_common_typos(value, key)
                # only accept if key is known or close match, or value not empty
                if value and len(value) >= 1:
                    # avoid overwriting high-confidence NIK from regex with short garbage
                    if key == "nik" and "nik" in result and len(result["nik"]) == 16 and len(value) < 16:
                        continue
                    result[key] = value
            else:
                # line without known delimiter but contains key fuzzy inside
                low = line.lower()
                for exp in EXPECTED_KEYS:
                    if exp in low:
                        # try extract after key
                        idx = low.find(exp)
                        val = line[idx+len(exp):].strip(" :.-\t")
                        if val:
                            val = correct_common_typos(val, exp)
                            if exp not in result:
                                result[exp] = val
                        break

        # ensure NIK from regex wins if missing/short
        if nik and ( "nik" not in result or len(result["nik"]) < 16):
            result["nik"] = nik
    else:
        # Generic document: try to extract colon-based with robust split
        generic = {}
        for line in cleaned_text.splitlines():
            split = split_robust(line)
            if split:
                k, v = split[0].strip().lower(), split[1].strip()
                if k and v and len(v) > 1:
                    generic[k] = v
        if generic:
            result.update(generic)
        result["raw_text"] = cleaned_text

    return result
