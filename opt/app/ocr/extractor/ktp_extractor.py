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

VALID_PROVINCES = {11,12,13,14,15,16,17,18,19,21,31,32,33,34,35,36,51,52,53,61,62,63,64,65,71,72,73,74,75,76,81,82,91,92}

def is_valid_nik(nik: str) -> bool:
    """Checksum NIK: 16 digits, province code, date DDMMYY valid, not all same digit"""
    if nik is None or len(nik) != 16 or not nik.isdigit():
        return False
    # not all same digit (e.g. 0000000000000000)
    if len(set(nik)) == 1:
        return False
    try:
        prov = int(nik[0:2])
        if prov not in VALID_PROVINCES:
            return False
        # date part: nik[6:12] = DDMMYY (with gender offset: female +40 on DD)
        dd = int(nik[6:8])
        mm = int(nik[8:10])
        yy = int(nik[10:12])
        # adjust female
        if dd > 40:
            dd -= 40
        if not (1 <= dd <= 31 and 1 <= mm <= 12):
            return False
        # yy 00-99 is ok
        return True
    except Exception:
        return False


def extract_nik_regex(text: str) -> str:
    """Robust NIK extraction: 16 digits with typo tolerance + checksum validation"""
    candidates = re.findall(r'[\dOIlBZS\-\s]{14,20}', text)
    best_valid = None
    best_any = None
    for cand in candidates:
        fixed = cand.replace('O','0').replace('o','0').replace('I','1').replace('l','1').replace('L','1').replace('B','8').replace('S','5').replace('Z','2').replace(' ','').replace('-','')
        fixed = re.sub(r'[^0-9]', '', fixed)
        if len(fixed) >= 16:
            fixed16 = fixed[:16]
            if len(fixed16) == 16:
                if best_any is None:
                    best_any = fixed16
                if is_valid_nik(fixed16):
                    return fixed16  # return first valid immediately
                if best_valid is None and len(fixed) == 16:
                    best_valid = fixed
    if best_valid:
        return best_valid
    if best_any:
        return best_any
    m = re.search(r'\b\d{16}\b', text)
    if m:
        cand = m.group(0)
        if is_valid_nik(cand):
            return cand
        return cand
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
