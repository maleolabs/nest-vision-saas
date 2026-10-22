-- V1: baseline schema.
--
-- Kenapa ada file ini:
-- Sebelumnya project hanya punya migration delta (V2..V5) dengan
-- `spring.flyway.baseline-version=1` dan asumsi "schema legacy sudah ada"
-- (dibuat manual / oleh Hibernate ddl-auto=update). Akibatnya database KOSONG
-- tidak bisa di-bootstrap: Flyway jalan sebelum Hibernate, jadi V2 langsung
-- gagal dengan "table api_keys doesn't exist".
--
-- Kontrak file ini:
-- 1. Bentuknya adalah schema LEGACY (sebelum V2..V5). Delta V2..V5 tetap
--    dijalankan sesudahnya, sehingga hasil akhirnya sama untuk instalasi baru
--    maupun database yang sudah dibaseline.
-- 2. Database lama TIDAK menjalankan file ini: `baseline-on-migrate=true` +
--    `baseline-version=1` membuat database non-empty tanpa history dibaseline
--    di versi 1 (V1 dianggap sudah diterapkan).
-- 3. DDL diturunkan dari entity JPA (Hibernate) supaya `ddl-auto=validate`
--    lolos. Kalau entity berubah, tambahkan migration baru — jangan edit file ini
--    karena sudah diterapkan di instalasi baru.

CREATE TABLE `account_types` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `daily_limit` int NOT NULL,
  `description` varchar(255) NOT NULL,
  `icon` varchar(255) NOT NULL,
  `name` varchar(150) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Legacy api_keys: plaintext key wajib (V5 melonggarkan ke NULL), belum ada
-- `name` (V2) dan `key_hash` (V3), dan client_id masih UNIQUE single-column
-- (V4 yang melepasnya lalu menggantinya dengan index nonunique).
CREATE TABLE `api_keys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `api_key` varchar(255) NOT NULL,
  `client_id` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_keys_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `users` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','CLIENT') NOT NULL,
  `status` enum('ACTIVE','DISABLED','EXPIRED','LOCKED','PENDING') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `clients` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) NOT NULL,
  `client_id` varchar(100) NOT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `industry` varchar(255) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `account_type_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_clients_client_id` (`client_id`),
  UNIQUE KEY `uk_clients_email` (`email`),
  UNIQUE KEY `uk_clients_user_id` (`user_id`),
  KEY `idx_clients_account_type_id` (`account_type_id`),
  CONSTRAINT `fk_clients_account_type` FOREIGN KEY (`account_type_id`) REFERENCES `account_types` (`id`),
  CONSTRAINT `fk_clients_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `api_request_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` varchar(255) DEFAULT NULL,
  `endpoint` varchar(255) NOT NULL,
  `http_method` varchar(255) NOT NULL,
  `ip_address` varchar(255) NOT NULL,
  `request_time` datetime(6) DEFAULT NULL,
  `response_status` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ocr_request` (
  `id` binary(16) NOT NULL,
  `client_id` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `status` enum('DONE','FAILURE','PROCESSING','WAITING') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ocr_result` (
  `id` binary(16) NOT NULL,
  `duration` bigint DEFAULT NULL,
  `error_message` varchar(255) DEFAULT NULL,
  `is_success` bit(1) NOT NULL,
  `original_extracted_text` text,
  `ocr_request_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ocr_result_ocr_request_id` (`ocr_request_id`),
  CONSTRAINT `fk_ocr_result_ocr_request` FOREIGN KEY (`ocr_request_id`) REFERENCES `ocr_request` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `extracted_text` (
  `id` binary(16) NOT NULL,
  `text_key` varchar(100) NOT NULL,
  `text_value` text NOT NULL,
  `ocr_result_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_extracted_text_ocr_result_id` (`ocr_result_id`),
  CONSTRAINT `fk_extracted_text_ocr_result` FOREIGN KEY (`ocr_result_id`) REFERENCES `ocr_result` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `request_usages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` varchar(255) NOT NULL,
  `request_count` int NOT NULL,
  `request_date` date NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_usages_client_date` (`client_id`, `request_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `verification_codes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_verification_codes_user_id` (`user_id`),
  CONSTRAINT `fk_verification_codes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
