CREATE TABLE open_code_usage_records
(
    session_id         TEXT                                   NOT NULL,
    prompt_id          TEXT                                   NOT NULL,
    plugin_version     TEXT                                   NOT NULL,
    model              TEXT                                   NOT NULL,
    llm_provider       TEXT                                   NOT NULL,
    timestamp          TIMESTAMP WITH TIME ZONE               NOT NULL,
    input_tokens       BIGINT                                 NOT NULL,
    output_tokens      BIGINT                                 NOT NULL,
    cache_read_tokens  BIGINT                                 NOT NULL,
    cache_write_tokens BIGINT                                 NOT NULL,
    cost_nano_cent     BIGINT                                 NOT NULL,
    co2_gram           DOUBLE PRECISION                       NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    PRIMARY KEY (session_id, prompt_id)
);

CREATE INDEX idx_open_code_usage_timestamp
    ON open_code_usage_records (timestamp DESC);