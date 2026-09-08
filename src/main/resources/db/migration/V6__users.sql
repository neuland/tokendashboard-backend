CREATE TABLE users
(
    id              TEXT NOT NULL,
    first_data_sent DATE NOT NULL,
    last_data_sent  DATE NOT NULL,
    provider        TEXT NOT NULL,
    plugin_version  TEXT NOT NULL,
    PRIMARY KEY (id, provider)
);

CREATE INDEX idx_users_provider_last_data_sent
    ON users (provider, last_data_sent);

