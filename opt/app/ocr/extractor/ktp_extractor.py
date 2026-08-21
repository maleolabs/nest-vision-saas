import re
from difflib import get_close_matches

EXPECTED_KEYS = [
    "provinsi", "kabupaten", "kota", "nik", "nama", "tempat/tgl lahir", "jenis kelamin",
    "gol. darah", "alamat", "rt/rw", "kelurahan", "kecamatan", "agama", "status perkawinan",
    "pekerjaan", "kewarganegaraan", "berlaku hingga"
]

# Aliases for fuzzy matching (common OCR misreads and abbreviations)
ALIASES = {
    "provinsi": ["provinsi", "proplnsi", "prov1nsi", "propinsi", "prov"],
    "kabupaten": ["kabupaten", "kabupalen", "kabupat3n", "kab"],
    "kota": ["kota"],
    "nik": ["nik", "nlk", "nik.", "n1k"],
    "nama": ["nama", "narna"],
    "tempat/tgl lahir": ["tempat/tgl lahir", "tempat tgl lahir", "ttl", "tempat tanggal lahir"],
    "jenis kelamin": ["jenis kelamin", "jns kelamin", "kelamin"],
    "gol. darah": ["gol. darah", "gol darah", "goldar"],
    "alamat": ["alamat", "alarnat"],
    "rt/rw": ["rt/rw", "rt / rw", "rtrw", "rt rw"],
    "kelurahan": ["kelurahan", "kelurahaan", "desa", "kel/desa"],
    "kecamatan": ["kecamatan", "kecarnatan", "kec"],
    "agama": ["agama", "agarna"],
    "status perkawinan": ["status perkawinan", "status kawin", "perkawinan"],
    "pekerjaan": ["pekerjaan", "pekerjaaan"],
    "kewarganegaraan": ["kewarganegaraan", "kewarganegaraaan"],
    "berlaku hingga": ["berlaku hingga", "berlaku s/d", "berlaku"],
}

def is_ktp_document(text: str) -> bool:
    # More robust: handle OCR typos in keywords
    upper = text.upper()
    # fuzzy keywords
    keywords = ['NIK', 'PROVINSI', 'KABUPATEN', 'KELURAHAN', 'KECAMATAN', 'KOTA', 'ALAMAT', 'AGAMA']
    # count with close match
    matches = 0
    for kw in keywords:
        if kw in upper:
            matches += 1
        else:
            # check close occurrence via regex tolerant
            if re.search(kw[:3] + r".{0,2}" + kw[-2:], upper):
                matches += 0.5
    return matches >= 2.5

def normalize_key(raw_key: str) -> str:
    key = raw_key.lower().strip()
    # remove trailing punctuation
    key = re.sub(r'[:\.\-]+$', '', key).strip()
    # direct alias match
    for canonical, aliases in ALIASES.items():
        if key == canonical:
            return canonical
        for alias in aliases:
            if key == alias or get_close_matches(key, [alias], n=1, cutoff=0.85):
                # verify
                pass
    # general close match with higher cutoff 0.75 (was 0.6 too loose)
    best_match = get_close_matches(key, EXPECTED_KEYS, n=1, cutoff=0.75)
    if best_match:
        return best_match[0]
    # fallback check aliases flat
    flat = []
    for canonical, aliases in ALIASES.items():
        flat.extend(aliases)
        if key in aliases:
            return canonical
    best_alias = get_close_matches(key, flat, n=1, cutoff=0.75)
    if best_alias:
        for canonical, aliases in ALIASES.items():
            if best_alias[0] in aliases:
                return canonical
    return key

def extract_nik_regex(text: str) -> str:
    """Robust NIK extraction: 16 digits with typo tolerance"""
    # clean typo first
    # find 16 consecutive digits with possible OCR misreads
    candidates = re.findall(r'[\dOIlBZS\-\s]{14,20}', text)
    for cand in candidates:
        fixed = cand.replace('O','0').replace('o','0').replace('I','1').replace('l','1').replace('L','1').replace('B','8').replace('S','5').replace('Z','2').replace(' ','').replace('-','')
        fixed = re.sub(r'[^0-9]', '', fixed)
        if len(fixed) == 16:
            # validate Indonesian NIK: starts with plausible province code
            return fixed
        if len(fixed) >= 16:
            return fixed[:16]
    # stricter 16 digits
    m = re.search(r'\b\d{16}\b', text)
    if m:
        return m.group(0)
    return None

def extract_ktp_header(line: str, extracted: dict):
    line_stripped = line.strip()
    upper = line_stripped.upper()
    # handle PROVINSI with OCR typo
    if re.match(r'^\s*PROVINSI', upper) or re.match(r'^\s*PROPINSI', upper):
        val = re.sub(r'^\s*PRO[VP]INSI\s*', '', line_stripped, flags=re.IGNORECASE).strip(" :.-")
        if val:
            extracted["provinsi"] = val.title()
    elif re.match(r'^\s*KABUPATEN', upper):
        val = re.sub(r'^\s*KABUPATEN\s*', '', line_stripped, flags=re.IGNORECASE).strip(" :.-")
        if val:
            extracted["kabupaten"] = val.title()
    elif re.match(r'^\s*KOTA', upper):
        # avoid overriding kabupaten if already set to kota
        val = re.sub(r'^\s*KOTA\s*', '', line_stripped, flags=re.IGNORECASE).strip(" :.-")
        if val:
            # if kabupaten not set or is city
            if "kabupaten" not in extracted or "kota" in upper.lower():
                extracted["kabupaten"] = val.title()
            extracted["kota"] = val.title()
