CREATE TABLE copilot_usage_records
(
    session_id         TEXT PRIMARY KEY,
    plugin_version     TEXT                                   NOT NULL,
    model              TEXT                                   NOT NULL,
    timestamp          TIMESTAMP WITH TIME ZONE               NOT NULL,
    input_tokens       BIGINT                                 NOT NULL,
    output_tokens      BIGINT                                 NOT NULL,
    cache_read_tokens  BIGINT                                 NOT NULL,
    cache_write_tokens BIGINT                                 NOT NULL,
    nano_aiu           BIGINT                                 NOT NULL,
    co2_gram           DOUBLE PRECISION                       NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

CREATE INDEX idx_copilot_usage_timestamp
    ON copilot_usage_records (timestamp DESC);