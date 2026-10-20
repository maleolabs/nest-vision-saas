-- api_keys supports overlapping active keys during rotation. Drop only a
-- single-column UNIQUE index on client_id; preserve unrelated indexes.
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
    ORDER BY s.INDEX_NAME
    LIMIT 1
);
SET @drop_client_id_unique = IFNULL(@drop_client_id_unique, 'SELECT 1');
PREPARE drop_client_id_unique FROM @drop_client_id_unique;
EXECUTE drop_client_id_unique;
DEALLOCATE PREPARE drop_client_id_unique;
