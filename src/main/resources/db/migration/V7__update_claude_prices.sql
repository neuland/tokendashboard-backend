INSERT INTO claude_prices (model, input_cent_per_million, output_cent_per_million, cache_read_cent_per_million,
                           valid_from, cache_write_5m_cent_per_million, cache_write_1h_cent_per_million)
VALUES ('fable', 1000, 5000, 25, '2026-09-01', 1250, 2000),
       ('opus', 400, 2000, 20, '2026-09-22', 500, 800),
       ('sonnet', 200, 1000, 10, '2026-10-07', 250, 400),
       ('haiku', 50, 250, 5, '2026-10-07', 63, 100);
