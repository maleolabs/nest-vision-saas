-- api_keys supports overlapping active keys during rotation. The expected
-- legacy schema has exactly one single-column UNIQUE index on client_id.
SET @legacy_client_id_unique_count = (
    SELECT COUNT(DISTINCT s.INDEX_NAME)
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'api_keys'
      AND s.COLUMN_NAME = 'client_id'
      AND s.NON_UNIQUE = 0
      AND s.SEQ_IN_INDEX = 1
      AND (
          SELECT COUNT(*)
          FROM information_schema.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = s.TABLE_SCHEMA
            AND s2.TABLE_NAME = s.TABLE_NAME
            AND s2.INDEX_NAME = s.INDEX_NAME
      ) = 1
);
SET @legacy_schema_check = IF(
    @legacy_client_id_unique_count = 1,
    'SELECT 1',
    'SELECT * FROM `v4_expected_one_legacy_client_id_unique_index`'
);
PREPARE legacy_schema_check FROM @legacy_schema_check;
EXECUTE legacy_schema_check;
DEALLOCATE PREPARE legacy_schema_check;

SET @drop_client_id_unique = (
    SELECT CONCAT('ALTER TABLE `api_keys` DROP INDEX `', s.INDEX_NAME, '`')
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'api_keys'
      AND s.COLUMN_NAME = 'client_id'
      AND s.NON_UNIQUE = 0
      AND s.SEQ_IN_INDEX = 1
      AND (
          SELECT COUNT(*)
          FROM information_schema.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = s.TABLE_SCHEMA
            AND s2.TABLE_NAME = s.TABLE_NAME
            AND s2.INDEX_NAME = s.INDEX_NAME
      ) = 1
      AND s.INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);
PREPARE drop_client_id_unique FROM @drop_client_id_unique;
EXECUTE drop_client_id_unique;
DEALLOCATE PREPARE drop_client_id_unique;

ALTER TABLE `api_keys` ADD INDEX `idx_api_keys_client_id` (`client_id`);

SET @client_id_non_unique_count = (
    SELECT COUNT(DISTINCT s.INDEX_NAME)
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'api_keys'
      AND s.COLUMN_NAME = 'client_id'
      AND s.NON_UNIQUE = 1
      AND s.SEQ_IN_INDEX = 1
      AND (
          SELECT COUNT(*)
          FROM information_schema.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = s.TABLE_SCHEMA
            AND s2.TABLE_NAME = s.TABLE_NAME
            AND s2.INDEX_NAME = s.INDEX_NAME
      ) = 1
);
SET @postcondition_check = IF(
    @client_id_non_unique_count = 1,
    'SELECT 1',
    'SELECT * FROM `v4_failed_client_id_index_postcondition`'
);
PREPARE postcondition_check FROM @postcondition_check;
EXECUTE postcondition_check;
DEALLOCATE PREPARE postcondition_check;
