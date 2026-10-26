ALTER TABLE api_keys ADD COLUMN secret_nonce VARCHAR(24) NULL, ADD COLUMN secret_ciphertext TEXT NULL;
