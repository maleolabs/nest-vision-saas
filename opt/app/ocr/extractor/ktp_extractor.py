import re
from difflib import get_close_matches

EXPECTED_KEYS = [
    "provinsi", "kabupaten", "kota", "nik", "nama", "tempat/tgl lahir", "jenis kelamin",
    "gol. darah", "alamat", "rt/rw", "kelurahan", "kecamatan", "agama", "status perkawinan",
    "pekerjaan", "kewarganegaraan", "berlaku hingga"
]

# Generic document keys fallback (for non-KTP)
GENERIC_KEYS = [
    "nama", "nik", "nomor", "alamat", "tempat/tgl lahir", "tanggal lahir", "jenis kelamin",
    "agama", "pekerjaan", "status", "kewarganegaraan"
]

# Aliases for fuzzy matching (common OCR misreads and abbreviations)
ALIASES = {
    "provinsi": ["provinsi", "proplnsi", "prov1nsi", "propinsi", "prov", "gorontalo", "prov1nsi"],
    "kabupaten": ["kabupaten", "kabupalen", "kabupat3n", "kab", "kab."],
    "kota": ["kota"],
    "nik": ["nik", "nlk", "nik.", "n1k", "nik:", "nik;"],
    "nama": ["nama", "narna", "nama:"],
    "tempat/tgl lahir": ["tempat/tgl lahir", "tempat tgl lahir", "ttl", "tempat tanggal lahir", "tempat/tgl lahir:", "ttl:"],
    "jenis kelamin": ["jenis kelamin", "jns kelamin", "kelamin", "jenis kelamın"],
    "gol. darah": ["gol. darah", "gol darah", "goldar", "gol.darah", "golongan darah"],
    "alamat": ["alamat", "alarnat", "alarnat :", "alamat:"],
    "rt/rw": ["rt/rw", "rt / rw", "rtrw", "rt rw", "rtrw:"],
    "kelurahan": ["kelurahan", "kelurahaan", "desa", "kel/desa", "kel/des", "kel/desa:", "desa:"],
    "kecamatan": ["kecamatan", "kecarnatan", "kec", "kecamatan:", "kec."],
    "agama": ["agama", "agarna", "agama:"],
    "status perkawinan": ["status perkawinan", "status kawin", "perkawinan", "status perkawinan:"],
    "pekerjaan": ["pekerjaan", "pekerjaaan", "pekerjaan:"],
    "kewarganegaraan": ["kewarganegaraan", "kewarganegaraaan", "kewarganegaraan:"],
    "berlaku hingga": ["berlaku hingga", "berlaku s/d", "berlaku", "berlaku hingga:", "berlaku hingga :"],
}

# Document type detection — vision now aware of multiple types
DOCUMENT_TYPES = {
    "KTP": ['NIK', 'PROVINSI', 'KABUPATEN', 'KELURAHAN', 'KECAMATAN', 'ALAMAT', 'AGAMA'],
    "KK": ['KARTU KELUARGA', 'NO. KK', 'KEPALA KELUARGA', 'ALAMAT'],
    "SIM": ['SURAT IZIN MENGEMUDI', 'SIM', 'BERLAKU HINGGA'],
    "PASPOR": ['PASPOR', 'PASSPORT', 'KEBANGSAAN', 'NATIONALITY'],
}

def detect_document_type(text: str) -> str:
    upper = text.upper()
    scores = {}
    for doc_type, keywords in DOCUMENT_TYPES.items():
        matches = 0
        for kw in keywords:
            if kw in upper:
                matches += 1
            elif re.search(kw[:3] + r".{0,2}" + kw[-2:], upper):
                matches += 0.5
        scores[doc_type] = matches
    best = max(scores, key=scores.get) if scores else "GENERIC"
    if scores.get(best, 0) >= 2.5:
        return best
    # fallback: if NIK present, assume KTP
    if 'NIK' in upper:
        return "KTP"
    return "GENERIC"

def is_ktp_document(text: str) -> bool:
    return detect_document_type(text) == "KTP"

def normalize_key(raw_key: str) -> str:
    key = raw_key.lower().strip()
    key = re.sub(r'[:\.\-]+$', '', key).strip()
    # direct alias match
    for canonical, aliases in ALIASES.items():
        if key == canonical:
            return canonical
        for alias in aliases:
            if key == alias or get_close_matches(key, [alias], n=1, cutoff=0.85):
                pass
    best_match = get_close_matches(key, EXPECTED_KEYS, n=1, cutoff=0.75)
    if best_match:
        return best_match[0]
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
    if len(set(nik)) == 1:
        return False
    try:
        prov = int(nik[0:2])
        if prov not in VALID_PROVINCES:
            return False
        dd = int(nik[6:8])
        mm = int(nik[8:10])
        yy = int(nik[10:12])
        if dd > 40:
            dd -= 40
        if not (1 <= dd <= 31 and 1 <= mm <= 12):
            return False
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
                    return fixed16
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

def _clean_region_value(val: str) -> str:
    # strip leading :.- and extra spaces, title case, remove OCR artifacts
    val = re.sub(r'^[\s:;\-\.]+', '', val)
    val = re.sub(r'\s+', ' ', val).strip(" :.-\t")
    val = re.sub(r'\s*\d{4,}$', '', val)
    # fix common OCR province/kab typos + missing space
    upper = val.upper().replace(' ', '')
    if 'GORONTALOUTARA' in upper or 'GORONTALO' in upper and 'UTARA' in upper:
        # ensure space: Gorontalo Utara
        if 'GORONTALOUTARA' in val.upper().replace(' ', '').replace('-',''):
            val = 'Gorontalo Utara'
        else:
            val = val.replace('GORGNTALG', 'GORONTALO').replace('GORGNTALO', 'GORONTALO').replace('GORONTALOUTARA', 'GORONTALO UTARA')
    else:
        val = val.replace('GORGNTALG', 'GORONTALO').replace('GORGNTALO', 'GORONTALO')
    # fix no-space kabupaten
    if val.upper().replace(' ', '') == 'GORONTALOUTARA':
        val = 'Gorontalo Utara'
    if val:
        # title case but keep correct spacing
        val = val.title().strip()
        # re-fix after title
        if val.lower() == 'gorontaloutara':
            val = 'Gorontalo Utara'
        return val
    return val

def extract_ktp_header(line: str, extracted: dict):
    """
    Header KTP tanpa ':' separator — 2 baris teratas + juga fallback untuk
    kecamatan/kelurahan yang kadang tanpa delimiter di KTP lama.
    Contoh:
      PROVINSI GORONTALO
      KABUPATEN GORONTALO UTARA
      Kecamatan : ATINGGOLA
      Kel/Desa : WAPALO
    Semua ditangani, baik pakai ':' maupun spasi biasa.
    """
    line_stripped = line.strip()
    upper = line_stripped.upper()
    # PROVINSI — tanpa atau dengan :
    m = re.match(r'^\s*PROVINSI\s*[:\-]?\s*(.+)$', line_stripped, flags=re.IGNORECASE)
    if m:
        val = _clean_region_value(m.group(1))
        if val and len(val) >= 3:
            extracted["provinsi"] = val
            return
    # also PROPINSI typo
    m = re.match(r'^\s*PROPINSI\s*[:\-]?\s*(.+)$', line_stripped, flags=re.IGNORECASE)
    if m:
        val = _clean_region_value(m.group(1))
        if val:
            extracted["provinsi"] = val
            return
    # KABUPATEN / KOTA — tanpa :
    m = re.match(r'^\s*KABUPATEN\s*[:\-]?\s*(.+)$', line_stripped, flags=re.IGNORECASE)
    if m:
        val = _clean_region_value(m.group(1))
        if val:
            extracted["kabupaten"] = val
            # also set kota for compatibility
            if "kota" not in extracted:
                extracted["kota"] = val
            return
    m = re.match(r'^\s*KOTA\s*[:\-]?\s*(.+)$', line_stripped, flags=re.IGNORECASE)
    if m:
        val = _clean_region_value(m.group(1))
        if val:
            if "kabupaten" not in extracted or "kota" in upper.lower():
                extracted["kabupaten"] = val
            extracted["kota"] = val
            return
    # Kecamatan / Kel/Desa header tanpa : (rare but handle)
    m = re.match(r'^\s*KECAMATAN\s*[:\-]?\s*(.+)$', line_stripped, flags=re.IGNORECASE)
    if m and "kecamatan" not in extracted:
        val = _clean_region_value(m.group(1))
        if val:
            extracted["kecamatan"] = val.title()
            return
    m = re.match(r'^\s*(KEL\/DESA|KEL\.?\/DESA|KELURAHAN|DESA)\s*[:\-]?\s*(.+)$', line_stripped, flags=re.IGNORECASE)
    if m and "kelurahan" not in extracted:
        val = _clean_region_value(m.group(2))
        if val:
            extracted["kelurahan"] = val.title()
            return
