"""Regresi OSD brute-force orientation scoring.

Bug lama: brute-force memilih rotasi berdasarkan len(txt) > 40. Teks rotasi-
salah menghasilkan karakter noise yang LEBIH PANJANG, sehingga KTP2 yang
sudah tegak diputar 180° padahal sudah benar.

Fix: orientation_score() skoring berbasis keyword KTP + NIK 16-digit —
sinyal yang hanya muncul saat orientasi benar.
"""
import sys, os
import importlib.util

_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
_SPEC = importlib.util.spec_from_file_location(
    "tesseract_ocr", os.path.join(_ROOT, 'opt', 'app', 'ocr', 'tesseract_ocr.py'))
tesseract_ocr = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(tesseract_ocr)
orientation_score = tesseract_ocr.orientation_score


def test_keyword_hits_score():
    """Keyword KTP yang hanya terbaca di orientasi benar memberi skor."""
    text = "PROVINSI GORONTALO\nNIK 7501070410020002\nnama: MARIJ"
    s = orientation_score(text)
    assert s >= 50.0, f"keyword+NIK should score high, got {s}"
    assert s > 0


def test_nik_gives_strongest_signal():
    """NIK 16-digit = 50 poin, sinyal terkuat."""
    text = "random noise\n7501070410020002\nmore noise"
    s = orientation_score(text)
    # 50 (NIK) + keyword "nik" ikut jika ada; minimal 50
    assert s >= 50.0, f"NIK alone should score >=50, got {s}"


def test_noise_alone_scores_zero():
    """Karakter noise rotasi-salah tak ada keyword/NIK -> skor 0.

    Inilah bedanya vs metric lama: panjang teks noise bisa > 40 (memenuhi
    ambang lama), tapi tetap skor 0 disini.
    """
    noise = "xBzq7!@#kRpm2vLwQzXy7QsW4dLfHnBqRsTuVwXyZ0123456789abcdefghij" * 3
    s = orientation_score(noise)
    assert s == 0.0, f"noise tanpa keyword harus 0, got {s}"


def test_empty_and_none():
    assert orientation_score("") == 0.0
    assert orientation_score(None) == 0.0


def test_keyword_partial_match():
    """Beberapa keyword = beberapa poin, cukup untuk ambang 30."""
    text = "PROVINSI GORONTALO\nkabupaten: GORONTALO UTARA"
    s = orientation_score(text)
    # 2 keyword x 10 = 20; tidak sampai 30 -> tidak cukup untuk rotate
    assert s == 20.0, f"2 keyword = 20 poin, got {s}"

    text2 = text + "\nNIK 7501070410020002"
    s2 = orientation_score(text2)
    assert s2 >= 70.0, f"2 keyword + NIK >= 70, got {s2}"


def test_scores_monotonic_with_signal():
    """Skor meningkat seiring sinyal orientasi benar."""
    low = orientation_score("noise text here")
    mid = orientation_score("nama: MARIJ\nalamat: JALAN")
    high = orientation_score("nik: 7501070410020002\nprovinsi: GORONTALO")
    assert low < mid < high


if __name__ == "__main__":
    test_keyword_hits_score()
    test_nik_gives_strongest_signal()
    test_noise_alone_scores_zero()
    test_empty_and_none()
    test_keyword_partial_match()
    test_scores_monotonic_with_signal()
    print("ALL OSD SCORE TESTS PASSED")
