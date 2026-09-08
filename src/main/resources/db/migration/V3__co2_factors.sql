create table co2_factors
(
    model              text             not null,
    input_factor       double precision not null,
    output_factor      double precision not null,
    cache_read_factor  double precision not null,
    cache_write_factor double precision not null,
    valid_from         date             not null
);

INSERT INTO co2_factors (model, input_factor, output_factor, cache_read_factor, cache_write_factor, valid_from)
VALUES ('fable', 140, 2800, 1.4, 175, '2026-01-01'),
       ('opus', 70, 1400, 0.7, 87, '2026-01-01'),
       ('sonnet', 42, 840, 0.42, 52, '2026-01-01'),
       ('haiku', 14, 280, 0.14, 17, '2026-01-01');
