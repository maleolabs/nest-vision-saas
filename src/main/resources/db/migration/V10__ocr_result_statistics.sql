-- Statistik observability OCR: sebelumnya hanya ada di log aplikasi (baris
-- [OCR] blur=... conf=... di stderr python) dan hilang setelah request selesai.
-- Disimpan supaya halaman hasil TryOCR bisa menampilkan dashboard statistik
-- (conf, durasi, blur, brightness, contrast, psm, engine, SR).
ALTER TABLE `ocr_result`
  ADD COLUMN `confidence` int DEFAULT NULL,
  ADD COLUMN `blur_score` double DEFAULT NULL,
  ADD COLUMN `brightness` double DEFAULT NULL,
  ADD COLUMN `contrast` double DEFAULT NULL,
  ADD COLUMN `psm_used` varchar(32) DEFAULT NULL,
  ADD COLUMN `engine_used` varchar(64) DEFAULT NULL,
  ADD COLUMN `super_resolution_applied` tinyint(1) DEFAULT NULL;
