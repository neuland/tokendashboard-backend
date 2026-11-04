# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - Unreleased

### Changed

- **Cost and CO₂ are calculated on read instead of being stored per prompt.**
  Usage records now hold only what the plugins report: token counts, plus the
  provider's own raw cost where one exists (Copilot, OpenCode). Cost and CO₂ are
  derived when usage is queried, from the price and CO₂ factor versions valid on
  each usage day. Adding or correcting a price or factor therefore applies to
  all historical figures immediately, without re-ingesting or backfilling data.
  See decision 9 in `docs/decisions.md`.
- **Ingest no longer looks up prices or CO₂ factors.** Plugin requests only
  write token counts, so ingest time no longer depends on reference data.
- **Cost is rounded to whole cents once per usage day and model, for every
  provider**, and all totals sum these values. Every endpoint now reports the
  same figure for the same data, regardless of grouping. Figures may differ
  from 1.x by a few cents. See decision 11.
- **Usage dated before the first price or CO₂ factor version of a model family
  uses that family's oldest version** instead of `0`. A family without any
  version still yields `0`. See decision 10.
- **Missing prices or CO₂ factors are no longer logged at ingest.** A model
  showing usage with zero cost in the dashboard is the signal; inserting the
  missing version fixes all historical figures.
- **`REPORTING_TIME_ZONE` determines the day boundaries of all usage views.**
  Because nothing is stored per day, a changed time zone takes effect on the
  next request after a restart. See decision 15.
- **On startup, the application (re)creates database statistics** on the usage
  tables for the configured reporting time zone and analyzes those tables.
  They let PostgreSQL estimate the number of day/model groups correctly, which
  keeps the daily aggregation in memory instead of sorting the whole queried
  range.

### Removed

- Stored cost and CO₂ columns: `claude_usage_records.cost_usd_cent`,
  `claude_usage_records.co2_gram`, `copilot_usage_records.co2_gram` and
  `open_code_usage_records.co2_gram`.

### Added

- Unique constraints on `(model, valid_from)` for `claude_prices` and
  `co2_factors`. Two versions for the same model family and day would make the
  applied price or factor ambiguous.

### Upgrading from 1.x

- **Back up the database before upgrading.** The migrations drop the stored
  cost and CO₂ columns; this cannot be undone. The REST API's response
  formats are unchanged.
- **The migration fails if `claude_prices` or `co2_factors` already contain
  duplicate versions.** Check for them before upgrading and remove the wrong
  rows:

  ```sql
  SELECT model, valid_from, COUNT(*) FROM claude_prices GROUP BY model, valid_from HAVING COUNT(*) > 1;
  SELECT model, valid_from, COUNT(*) FROM co2_factors   GROUP BY model, valid_from HAVING COUNT(*) > 1;
  ```

### Scaling

Every usage query aggregates the raw usage records of the requested date range
per day and model. Its cost therefore grows with the number of records **in
the requested range**, not with the total size of the database: the dashboard's
default range of four weeks stays equally fast as history accumulates, as long
as the usage per four weeks stays roughly the same.

On typical hardware, ranges with up to several hundred thousand usage records
are answered well within a second. Beyond that, response time grows linearly,
reaching seconds at a few million records per range. Deployments that regularly
query ranges of that size should switch to **pre-aggregated daily totals**: a
table per provider holding token sums per day and model, kept up to date on
ingest, from which cost and CO₂ are derived the same way. The change is
confined to the database adapters behind the usage repository ports; domain
logic and the API stay untouched.
