-- Wall-clock selesai diproses (status DONE/FAILURE). Sebelumnya UI hanya menampilkan
-- OcrResult.duration (waktu 1 pass OCR engine), bukan total queue+OCR+parse.
ALTER TABLE `ocr_request`
  ADD COLUMN `completed_at` datetime(6) DEFAULT NULL;
