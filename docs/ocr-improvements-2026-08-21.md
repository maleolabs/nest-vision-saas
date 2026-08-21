# OCR Improvements - 11 Poin (Blur/ Low-Quality Images)

## Ringkasan Implementasi 2026-08-21

### P1.1 Preprocessing Overhaul
- **Java `TesseractOcrService.preprocessImage()`**: fix double `cvtColor` bug, hapus `dilate(3x3)` yang bikin huruf nempel, ganti pipeline: `Upscale (2-2.5x cubic) -> CLAHE (2.0,8x8) -> fastNlMeansDenoising (10,7,21) -> deskew (minAreaRect) -> adaptiveThreshold vs OTSU by quality -> MORPH_OPEN (2x2) -> white border 10px`.
- **Python `tesseract_ocr.py`**: `preprocess_image_v2()` baru dengan `upscale_if_needed`, `CLAHE`, `denoise`, `deskew`, quality-aware threshold, `MORPH_OPEN`, SR hook. `laplacian_variance` + `assess_quality()`.

### P1.2 Multi-PSM + Confidence
- Java: loop PSM [6,4,3,1,11] dengan scoring `ratio*70 + lenScore - noisePenalty`, pilih best, early exit jika >85.
- Python: `ocr_with_confidence()` pakai `image_to_data` conf avg, fallback `score_text()`. `ocr_multi_psm()` coba 3 image variant x 5 PSM = 15 pass, pilih conf tertinggi. `oem 1` (LSTM only) + `user_defined_dpi 300`.

### P1.3 Parser Tolerant
- `ExtractedTextMapper.robustSplit()`: coba delimiters `: ; | double-space -` , normalize `·•->:`, fuzzy key via Levenshtein <=3, `filterParsedDataByRequiredKeys` case-insensitive + fuzzy.
- `postprocessor.py`: `clean_text` preserve punctuation, `split_robust()` multi-delimiter, fallback key-in-line fuzzy, generic doc fallback.
- `ExtractedTextCleaner`: allow `-,/.` dan `len>=2` (keep RT/RW), `linesCleaner` di-enable kembali di `OcrProcessorService`.

### P1.4 NIK Whitelist + Typo Fix
- `correct_nik_typos` / `correctValue`: `O->0, I/l/L->1, B->8, S->5, Z->2`, strip non-digit, cap 16 digit. `tessedit_char_whitelist` hint via config.

### P2.5 Quality Gate Auto-Preprocess
- `ImageQualityAssessor.java`: `laplacian variance` (blur <100 = blurry), brightness, contrast (<30 low). `needsPreprocessing()` trigger. `TesseractOcrService` auto-enable preprocess jika blur/lowContrast, `PyTesseractOcrService` log hook, `OcrProcessorService.usePreprocessedImage()` heuristic.

### P2.6 Ensemble Engine
- `PaddleOcrService.java` (disabled by default, `ocr.paddle.enabled=false`) via `paddle_ocr.py`.
- `EnsembleOcrService.java` (`ensembleOcrService`): primary `pyTesseract` -> if conf <60 fallback `nativeTesseract` (forced preprocess) -> `paddle`. Pilih best by conf/len. Di-wire sebagai primary di `OcrProcessorService` (`@Qualifier("ensembleOcrService")`).
- `PythonOcrExcecutor`: pisah stderr untuk metrics `[OCR] blur=... conf=... psm=...`.

### P2.7 Super-Resolution
- Python `preprocess_image_v2(apply_sr)`: jika `blur <50` trigger SR: coba `cv2.dnn_superres EDSR x2` jika model ada (`ESRGAN_MODEL`), else `2x cubic + sharpen kernel`. Config `ocr.sr.enabled`, `OCR_SR_ENABLED`.

### P2.8 KTP Extractor Kuat
- `ktp_extractor.py`: `ALIASES` map, `EXPECTED_KEYS` fuzzy cutoff 0.75 (was 0.6), `extract_nik_regex()` tolerant 14-20 chars + typo fix + 16-digit validate, `is_ktp_document` tolerant 2.5/8 keywords, header regex `PROVINSI/PROPINSI/KABUPATEN/KOTA` tolerant.

### P3.9 Vision LLM Scaffold
- `LlmVisionExtractorService.java`: `ocr.llm.enabled=false` default, `gpt-4o-mini`/`qwen-vl` via OpenAI-compatible `base-url`, prompt KTP JSON, `Base64` image, `response_format json_object`. Hook di `Ensemble` bisa dipanggil jika conf < threshold. TODO: implement WebClient call (sudah ada `spring-boot-starter-webflux`).

### P3.10 Training Custom (Doc)
- Untuk deploy production: kumpulkan 200+ KTP blur, augmentasi (blur, rotate 5°, brightness ±30%), fine-tune `tesseract --psm 6 -l ind` via `tesstrain` atau fine-tune `TrOCR` (`microsoft/trocr-base-printed`). Simpan di `tessdata/ind.traineddata` custom. Checklist: `combine_tessdata`, test via `tesseract --oem 1`.

### P3.11 Observability
- `OcrResultDto`: `confidence, blurScore, brightness, contrast, psmUsed, engineUsed, superResolutionApplied`.
- `PythonOcrExcecutor`: log `[OCR]` metrics ke stderr, `OcrResultServiceImpl`: `log.info OCR result: req=... conf=... blur=... engine=...`.
- `OcrProcessorService`: log cleaner & parse counts.
- Config baru di `application.properties`: `ocr.preprocessing.*`, `ocr.ensemble.*`, `ocr.paddle.*`, `ocr.sr.*`, `ocr.llm.*`.

## Cara Aktifkan

```properties
# minimal (sudah default)
OCR_PREPROCESSING_ENABLED=true
OCR_ENSEMBLE_FALLBACK=true
OCR_CONF_THRESHOLD=60

# optional paddle
OCR_PADDLE_ENABLED=true
pip install paddlepaddle paddleocr

# optional SR
OCR_SR_ENABLED=true
ESRGAN_MODEL=/models/EDSR_x2.pb

# optional LLM
OCR_LLM_ENABLED=true
OCR_LLM_API_KEY=sk-...
OCR_LLM_MODEL=gpt-4o-mini
```

## Test Blur

```bash
python3 opt/app/ocr/tesseract_ocr.py test-blur.jpg
# lihat stderr: [OCR] blur=42.1 brightness=... conf=78.3 psm=6 sr=true
python3 opt/app/ocr/paddle_ocr.py test-blur.jpg
```

## Next
- Implementasi WebClient di `LlmVisionExtractorService.extractFromImageBase64`
- Kumpulkan metrics di Grafana: `blurScore` vs `conf` vs `engine`
- Fine-tune tessdata dengan dataset KTP blur internal
