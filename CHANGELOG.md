# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Backend and frontend share one version and are released together; each release is a
`vX.Y.Z` tag on `main`. Decisions referenced below live in
[`backend/docs/decisions.md`](backend/docs/decisions.md) unless noted otherwise.

## [1.0.0] - 2026-11-06

The first release. Earlier states of the two former repositories were development
builds (0.x) without a release.

### Backend

#### Changed

- **Cost and CO₂ are calculated on read instead of being stored per prompt.**
  Usage records now hold only what the plugins report: token counts, plus the
  provider's own raw cost where one exists (Copilot, OpenCode). Cost and CO₂ are
  derived when usage is queried, from the price and CO₂ factor versions valid on
  each usage day. Adding or correcting a price or factor therefore applies to
  all historical figures immediately, without re-ingesting or backfilling data.
  See decision 9.
- **Ingest no longer looks up prices or CO₂ factors.** Plugin requests only
  write token counts, so ingest time no longer depends on reference data.
- **Cost is rounded to whole cents once per usage day and model, for every
  provider**, and all totals sum these values. Every endpoint now reports the
  same figure for the same data, regardless of grouping. Figures may differ
  from the 0.x builds by a few cents. See decision 11.
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

#### API

- The provider usage response (`GET /api/usage/{provider}/v1`) names its
  active-user count `activeUsersLast4Weeks`, as the frontend already expected;
  the 0.x builds sent it as `activeUsersFor4Weeks`. All other response formats are
  unchanged.

#### Removed

- Stored cost and CO₂ columns: `claude_usage_records.cost_usd_cent`,
  `claude_usage_records.co2_gram`, `copilot_usage_records.co2_gram` and
  `open_code_usage_records.co2_gram`.

#### Added

- Unique constraints on `(model, valid_from)` for `claude_prices` and
  `co2_factors`. Two versions for the same model family and day would make the
  applied price or factor ambiguous.

### Frontend

#### Changed

- **New design.** An app shell with a sidebar that collapses to an icon rail,
  the key figures as a band under the page title, a per-model table that can show
  every token type, and a day/week switch for the history charts.
- **Token figures count input and output only**, unless marked "incl. cache";
  the FAQ explains why.
- **The dashboard's own logo** is the favicon and stands next to the company
  name; the neuland logo moved to the "A project by" credit.

### Repository and deployment

- **Backend and frontend live in one repository**, `backend/` and `frontend/`,
  merged with both histories. The former `tokendashboard-backend` repository was
  renamed and redirects here; `tokendashboard-frontend` is archived. See
  [`docs/decisions.md`](docs/decisions.md).
- **One CI workflow per side**, each running only when its folder changed, and one
  shared devcontainer for both.
- **Published images** for every release, for amd64 and arm64:
  `ghcr.io/neuland/tokendashboard/backend` and `ghcr.io/neuland/tokendashboard/frontend`,
  tagged `X.Y.Z`, `X.Y`, `X` and `latest`. The frontend image carries no company name;
  set one by building it yourself (`PUBLIC_COMPANY_NAME`).
- **`docker-compose.yml`** starts the whole stack with a database in one command. On
  its own it runs the published images, so trying the dashboard needs no source code;
  each GitHub Release attaches a copy pinned to its version. In a checkout,
  `docker-compose.override.yml` builds both images from the source instead.

### Upgrading from the 0.x builds

- **Back up the database before upgrading.** The migrations drop the stored
  cost and CO₂ columns; this cannot be undone.
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
