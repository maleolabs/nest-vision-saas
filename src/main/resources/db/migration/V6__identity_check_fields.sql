ALTER TABLE `ocr_request`
  ADD COLUMN `identity_document_type` varchar(255) DEFAULT NULL,
  ADD COLUMN `identity_decision` varchar(255) DEFAULT NULL,
  ADD COLUMN `identity_reason_code` varchar(255) DEFAULT NULL;
