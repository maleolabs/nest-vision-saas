import re
from difflib import get_close_matches

EXPECTED_KEYS = [
    "provinsi", "kabupaten", "kota", "nik", "nama", "tempat/tgl lahir", "jenis kelamin",
    "gol. darah", "alamat", "rt/rw", "kelurahan", "kecamatan", "agama", "status perkawinan",
    "pekerjaan", "kewarganegaraan", "berlaku hingga"
]

def is_ktp_document(text: str) -> bool:
    keywords = ['NIK', 'PROVINSI', 'KABUPATEN', 'KELURAHAN', 'KECAMATAN']
    matches = sum(1 for kw in keywords if kw in text.upper())
    return matches >= 3

def normalize_key(raw_key: str) -> str:
    key = raw_key.lower().strip()
    best_match = get_close_matches(key, EXPECTED_KEYS, n=1, cutoff=0.6)
    return best_match[0] if best_match else key

def extract_ktp_header(line: str, extracted: dict):
    line = line.strip().upper()
    if line.startswith("PROVINSI "):
        extracted["provinsi"] = line.replace("PROVINSI", "").strip().title()
    elif line.startswith("KABUPATEN "):
        extracted["kabupaten"] = line.replace("KABUPATEN", "").strip().title()
    elif line.startswith("KOTA "):
        extracted["kabupaten"] = line.replace("KOTA", "").strip().title()