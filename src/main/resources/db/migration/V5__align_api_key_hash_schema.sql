-- Hash-only API keys: plaintext legacy column must accept NULL.
ALTER TABLE `api_keys` MODIFY COLUMN `api_key` VARCHAR(255) NULL;

-- Add hash uniqueness only when no unique index already covers key_hash.
SET @key_hash_unique_count = (
    SELECT COUNT(DISTINCT s.INDEX_NAME)
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'api_keys'
      AND s.COLUMN_NAME = 'key_hash'
      AND s.NON_UNIQUE = 0
);
SET @add_key_hash_unique = IF(
    @key_hash_unique_count = 0,
    'ALTER TABLE `api_keys` ADD UNIQUE INDEX `uk_api_keys_key_hash` (`key_hash`)',
    'SELECT 1'
);
PREPARE add_key_hash_unique FROM @add_key_hash_unique;
EXECUTE add_key_hash_unique;
DEALLOCATE PREPARE add_key_hash_unique;

-- Ensure client lookups use a nonunique index, without touching existing data.
SET @client_id_index_count = (
    SELECT COUNT(DISTINCT s.INDEX_NAME)
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'api_keys'
      AND s.COLUMN_NAME = 'client_id'
      AND s.SEQ_IN_INDEX = 1
      AND s.NON_UNIQUE = 1
);
SET @add_client_id_index = IF(
    @client_id_index_count = 0,
    'ALTER TABLE `api_keys` ADD INDEX `idx_api_keys_client_id` (`client_id`)',
    'SELECT 1'
);
PREPARE add_client_id_index FROM @add_client_id_index;
EXECUTE add_client_id_index;
DEALLOCATE PREPARE add_client_id_index;
