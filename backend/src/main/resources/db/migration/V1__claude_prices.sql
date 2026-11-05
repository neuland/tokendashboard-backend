CREATE TABLE claude_prices
(
    model                           TEXT    NOT NULL,
    input_cent_per_million          INTEGER NOT NULL,
    output_cent_per_million         INTEGER NOT NULL,
    cache_read_cent_per_million     INTEGER NOT NULL,
    valid_from                      DATE    NOT NULL,
    cache_write_5m_cent_per_million INTEGER NOT NULL,
    cache_write_1h_cent_per_million INTEGER NOT NULL
);

INSERT INTO claude_prices (model, input_cent_per_million, output_cent_per_million, cache_read_cent_per_million,
                           valid_from, cache_write_5m_cent_per_million, cache_write_1h_cent_per_million)
VALUES ('fable', 1000, 5000, 100, '2026-01-01', 1250, 2000),
       ('opus', 500, 2500, 50, '2026-01-01', 625, 1000),
       ('sonnet', 300, 1500, 30, '2026-01-01', 375, 600),
       ('haiku', 100, 500, 10, '2026-01-01', 125, 200),
       ('sonnet', 200, 1000, 20, '2026-08-12', 250, 400);
