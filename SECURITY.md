# Security Policy

## Reporting a vulnerability

Please do **not** file security issues as public GitHub issues. Instead,
report them to `tokendashboard@neuland-bfi.de`. You can expect an
acknowledgement within a few working days.

- A description of the issue and its potential impact
- Steps to reproduce (a minimal repro is ideal)
- The version/commit affected

We'll acknowledge reports as quickly as we can and keep you updated as we work on a
fix. Please give us a reasonable amount of time to address the issue before any
public disclosure.

## Security model

The backend has **no authentication by design** and is intended to run only
on a trusted network (VPN). See [decision #4](docs/decisions.md#4-no-authentication-no-userteam-attribution)
for the reasoning behind this choice.

Therefore **the network is the only access control**. Anyone who can reach
the port can POST usage data to the ingest endpoints and can read all
aggregated figures.

What that means concretely:

- Ingested data is **not authenticated** — a client on the network can submit
  arbitrary token counts, which will be aggregated into the dashboard.
  Treat the figures as an internal estimate, not as an audited record.
- There is **no rate limiting**. Per-request limits exist (8 MiB body,
  5000 prompts per report, `Content-Length` required) but nothing bounds how
  many requests a client may send. Sustained ingest can grow the database
  without bound.
- No prompt content is ever collected or stored — only token counts.
  Usage records carry no user ID, so usage cannot be traced to a person.

## What an operator must provide

Deploying this safely requires the operator to provide what the application
itself does not:

- **Network isolation** — a VPN or equivalent boundary that restricts who can
  reach the service. This is the entire access control for this project.
- **Database backups** — the application has no built-in backup mechanism.
- **Disk monitoring** on the Postgres volume — because there is no rate
  limiting, sustained ingest traffic can grow the database without bound.

## Supported versions

Only the latest release is supported. This project has no release-branch
process, so fixes are applied to the current `main` branch only.
