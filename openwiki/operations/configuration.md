---
type: operations
title: Configuration and operations
description: The game.yml schema (ServerConfig) and how it is loaded and validated, installation and setup tasks (install, cleanInstall, generateRsa, logback), on-disk data under .data, and the CI, release, formatting, gameval-conflict and content-progress workflows.
tags: [config, game-yml, install, ci, release, logging, operations]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-164e2da859b5277df81c7d94
    resource: repo://.github/workflows/ci.yml
  - id: openwiki-source-db56221ad82b2f10eb5277c0
    resource: repo://api/server-config/src/main/kotlin/org/rsmod/api/server/config/SameInstanceCentralConfigValidation.kt
  - id: openwiki-source-83956bf63e8741ca9baf65bf
    resource: repo://api/server-config/src/main/kotlin/org/rsmod/api/server/config/ServerConfig.kt
  - id: openwiki-source-abd124dff8acf6e5d6d7a581
    resource: repo://api/server-config/src/main/kotlin/org/rsmod/api/server/config/ServerConfigLoader.kt
  - id: openwiki-source-4963d5cec5f07743abcf6cb2
    resource: repo://api/server-config/src/main/kotlin/org/rsmod/api/server/config/ServerConfigModule.kt
  - id: openwiki-source-2a9daaac1604f238ef4c63fb
    resource: repo://build.gradle.kts
  - id: openwiki-source-3b89b0d7d54ddee15fdb2281
    resource: repo://docs/RELEASE_CI.md
  - id: openwiki-source-a12b23ba77e750e749f366e9
    resource: repo://server/install/src/main/kotlin/org/rsmod/server/install/GameServerLogbackCopy.kt
  - id: openwiki-source-351c1d72baa6befe37448a9c
    resource: repo://tools/progress/content-progress.mjs
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Configuration and operations

## game.yml

The server reads `game.yml` from the working directory (the repository root when using
`gradlew run`). `ServerConfigModule` provides `ServerConfig` via
`ServerConfigLoader.loadOrCreate(./game.yml)`; if the file is missing it writes a default one,
but `GameServer` already refuses to boot without it (see
[Server boot sequence](../architecture/boot-sequence.md)). The `install` task copies
`game.example.yml` to `game.yml`.

| Key | Meaning |
|---|---|
| `name` | world/server name |
| `game-port` | client port (default 43594) |
| `revision` | OSRS cache/protocol revision (241 in the example) |
| `environment` | cache environment, e.g. `LIVE` |
| `world` | world id |
| `gameplay.drop-rates.multiplier` | server-wide multiplier for drops flagged `boosted`; stacks with each player's `varp.drop_rate_multiplier` |
| `gameplay.quest-requirements.mode` | `assume-completed` (default), `respect-progress`, or `virtual-completions` (+ `virtual-completions` / `virtual-lines` lists) |
| `database.postgres` | game DB `jdbc-url`, `user`, `password` |
| `central` | OpenRune Central link: `same-instance`, `host`, `link-port` (9091), `http-port`, `world-key`, `postgres` (`jdbc-url`, `user`, `password`, `pool-size`, `embedded-pgdata-dir`, default `.data/postgres`) |
| `login-timing-logs`, `social-pm-trace-logs` | diagnostic logging toggles |

Unknown keys are ignored. Load-time behaviour:

- `SameInstanceCentralWorldMigrator` may migrate the world setting using `game.example.yml`
  before parsing;
- `SameInstanceCentralConfigValidation` rejects `central.same-instance: true` without a
  `central.postgres` block (at least `pool-size`), with a message explaining the fix.

Central and login behaviour are covered in
[Networking, login and central server link](../networking/protocol-and-central.md); the game
database in [Accounts and database persistence](../persistence/accounts-and-database.md).

## Setup tasks (root `build.gradle.kts`)

| Task | Effect |
|---|---|
| `install` | depends on `:or-cache:freshCache`; copies logback config, generates RSA keys, copies `game.example.yml` → `game.yml` |
| `cleanInstall` | removes partial/corrupt install artifacts, then runs `install` |
| `generateRsa` | creates `.data/game.key` / `.data/client.key` only if missing (`-Prsa="..."` passes args) |
| `setupLogbackNovice` / `setupLogbackAdvanced` | copy `logback.novice.xml` / `logback.advanced.xml` to `server/logging/src/main/resources/logback.xml`; skipped if it already exists |
| `run` | `:server:app:run` |
| `configureOsrsMcp`, `updateOsrsMcp`, `removeOsrsMcp`, `runMcp` | manage the `tools/osrs-mcp` MCP server for OSRS data |

Cache tasks (`buildCache`, `freshCache`, `cleanCs2`, `mergePluginGamevals`) are described in
[Cache build and gamevals](../cache/cache-build-and-gamevals.md).

## On-disk data (`.data/`)

`cache/LIVE`, `cache/SERVER`, `raw-cache/`, `gamevals/`, `gamevals-binary/`, RSA keys
(`game.key`, `client.key`), embedded PostgreSQL data (`postgres/`, `pgdata/`) and `saves/`.
`.data` is local state; the release bundle ships `.data` without `raw-cache`.

## Running

`server/app` runs with `-Xms512m -Xmx4g` and G1 periodic GC so memory used during cache decoding
is returned afterwards. Success is logged as `Server ready in <time> (<phase breakdown>)`.
`shadowJar` builds `server/app/build/libs/server.jar`, runnable with `java -jar server.jar` next
to `game.yml` and `.data/`.

## GitHub workflows (`.github/workflows`)

| Workflow | What it does |
|---|---|
| `ci.yml` | JDK 21; generates cache + gameval sources (`freshCache` or, on cache hit, `buildCache`, plus `mergePluginGamevals` — required because `api/generated` sources are gitignored), `assemble test`, then boots the server and requires `Server ready in` within ~300 s, uploading `boot.log` on failure |
| `format.yml` | `spotlessCheck` (ktlint 1.5.0, ratcheted from `origin/main`); reports files needing `gradlew spotlessApply` |
| `gameval-conflicts.yml` | runs `tools/scripts/fix_gameval_conflicts.py` on PRs; commits reassigned ids for same-repo branches, errors on forks |
| `release-server.yml` | on push to `production` or manual dispatch: `freshCache`, `mergePluginGamevals`, `shadowJar`, bundles `openrune-server-release.zip` (server.jar, game.yml, `.data/`), optionally creates a dated GitHub release |
| `content-progress.yml` | scheduled every 4 days: runs `tools/progress/content-progress.mjs` to regenerate `PROGRESS.md` and the README progress block (never edit those by hand; `--check` exits 1 when stale) |
| `openwiki-update.yml` | refreshes this generated wiki |

Because unmapped gamevals throw by design, configuration errors in this data-driven server
usually surface at boot; the CI boot step exists to catch them.
