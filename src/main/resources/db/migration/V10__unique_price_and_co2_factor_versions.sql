ALTER TABLE claude_prices ADD CONSTRAINT claude_prices_model_valid_from_unique UNIQUE (model, valid_from);
ALTER TABLE co2_factors ADD CONSTRAINT co2_factors_model_valid_from_unique UNIQUE (model, valid_from);
