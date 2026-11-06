---
name: release
description: Prepare a release of TokenDashboard (backend and frontend share one version) — set the version in all build files, write the CHANGELOG.md section from the commits since the last release, and check everything release.yml checks. Use when asked to prepare, cut or bump a release or version. Stops before commit, tag and push.
---

# Prepare a release

Backend and frontend share one version and are released together. A release is a
`vX.Y.Z` tag on a commit in `main`; `.github/workflows/release.yml` does the rest
(images, GitHub Release). This skill prepares everything up to that tag. It never
commits, tags or pushes: the user does that, see the last step.

## 1. Settle the version

- Last release: `git describe --tags --abbrev=0 --match 'v*' origin/main` (none yet → everything
  since the start is new).
- Changes since then: `git log --oneline <last-tag>..HEAD -- backend/` and `-- frontend/`,
  plus root files (`docker-compose*.yml`, `.github/`, `README.md`).
- Semantic Versioning: a breaking change to the API, the plugin ingest format, the
  configuration or the database that needs manual steps → major; new features → minor;
  only fixes → patch. A pre-release is `X.Y.Z-rc.N`.
- If the user named no version, propose one with a one-line reason and wait for the answer.

## 2. Set it in both build files

- `backend/build.gradle.kts`: the line `version = "X.Y.Z"`.
- `frontend/`: `npm version X.Y.Z --no-git-tag-version` — updates `package.json` and
  both version fields in `package-lock.json`, without a commit or tag. Don't edit these
  two by hand.

## 3. Write the changelog section

Add `## [X.Y.Z] - YYYY-MM-DD` (today) to `CHANGELOG.md`, directly above the previous
release's section. If a `## [Unreleased]` section exists, turn it into this one instead.

Follow the structure and voice of the existing sections — read the previous one first:

- `### Backend`, `### Frontend`, `### Repository and deployment` — only those with changes.
- Below each: `#### Added`, `#### Changed`, `#### Fixed`, `#### Removed`, and `#### API`
  for changes to response or request formats.
- Each entry starts with a **bold one-sentence summary** of what changes for someone
  running or using the dashboard, then the why or the consequence. Not a commit list:
  merge related commits, leave out refactorings, tests and CI changes that change
  nothing for users.
- Reference decisions by number ("See decision 9"); backend decisions live in
  `backend/docs/decisions.md`, repository-wide ones in `docs/decisions.md`.
- Anything that needs action when upgrading (migration, changed configuration) goes into
  an `### Upgrading from X.Y` section.

Show the section to the user and adjust it before going on.

## 4. Check what release.yml will check

Run from the repository root, with `VERSION=X.Y.Z`:

```bash
# Versions match
test "$(sed -nE 's/^version = "(.*)"$/\1/p' backend/build.gradle.kts)" = "$VERSION" \
  && test "$(node -p "require('./frontend/package.json').version")" = "$VERSION" \
  && echo "versions ok"

# Changelog section exists and is not empty
awk -v version="$VERSION" 'index($0, "## [" version "]") == 1 { found = 1; next }
  found && /^## \[/ { exit } found { print }' CHANGELOG.md | grep -q '[^[:space:]]' \
  && echo "changelog ok"

# docker-compose.yml still has both image lines the release pins
test "$(grep -c '${TOKENDASHBOARD_VERSION:-latest}' docker-compose.yml)" -eq 2 && echo "compose ok"
```

Then run the tests of both sides (`./gradlew test` in `backend/`, `npm test` and
`npm run lint` in `frontend/`).

## 5. Hand over — do not commit, tag or push

Report that the release is ready to commit and print the remaining steps for the user:

```bash
git add backend/build.gradle.kts frontend/package.json frontend/package-lock.json CHANGELOG.md
git commit -m "Release X.Y.Z"
# open a pull request into main and merge it, then:
git switch main && git pull
git tag vX.Y.Z
git push origin vX.Y.Z
```

The tag push starts `release.yml`. It fails without publishing anything if the tag is not
on `main`, the versions do not match or the changelog section is missing.
