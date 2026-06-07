# Release CI

OpenRune Server ships runnable releases through the [Release Server](../workflows/release-server.yml) GitHub Actions workflow. Each run produces a self-contained zip you can extract and start with `java -jar server.jar`.

## What the workflow does

1. **Checkout** the requested ref (branch, tag, or SHA).
2. **Prepare config** by copying `game.example.yml` to `game.yml`.
3. **Build artifacts** with Gradle:
   - `:or-cache:freshCache` — rebuilds the compiled cache under `.data/`
   - `:or-cache:mergePluginGamevals` — merges `content/**/gamevals.toml` into `.data/gamevals/*.rscm`
   - `:server:app:shadowJar` — produces the fat jar as `server/app/build/libs/server.jar`
4. **Bundle the release** into `openrune-server-release.zip` containing:
   - `server.jar`
   - `game.yml`
   - `.data/` (compiled cache, gamevals, RSA keys; `raw-cache` sources are excluded)
5. **Upload** the zip as a workflow artifact (kept for 14 days).
6. **Create a GitHub release** (when enabled) tagged `YYYY-MM-DD-<branch>-<short-sha>` with the zip attached.

## When releases are created automatically

| Trigger | GitHub release | Typical use |
|---------|----------------|-------------|
| Push to `production` | Yes | Stable / live server builds |
| `workflow_dispatch` with **Create release** checked | Yes | Manual release from any branch |
| `workflow_dispatch` with **Create release** unchecked | No (artifact only) | Test a build without publishing |

Pushes to `main` or feature branches do **not** trigger this workflow. Use a manual run when you want a build from development branches.

## Running a manual release

1. Open **Actions → Release Server** in GitHub.
2. Click **Run workflow**.
3. Choose the branch to build from the dropdown (for example `main`, `production`, or a feature branch).
4. Optionally set **ref** to a specific tag or commit SHA instead of the branch tip.
5. Leave **Create release** checked to publish a GitHub release, or uncheck it to only upload the workflow artifact.
6. Start the run and wait for the build to finish (cache compilation can take a while).

Download the result from either:

- **Releases** (if a GitHub release was created), or
- **Actions → the workflow run → Artifacts → openrune-server-release**

## Building from different branches

### Production (stable)

- **Automatic:** merge or push to `production` and the workflow runs on its own.
- **Manual:** run the workflow with the branch set to `production`.
- Use this for builds you deploy to a live server.

### Main / development

- Run the workflow manually from the `main` branch (or any feature branch).
- Uncheck **Create release** if you only want to verify the build or share the artifact internally.
- Check **Create release** when you want a tagged pre-release build from `main`.

### Specific commit or tag

Set the **ref** input to a full SHA, short SHA, or tag name (for example `v1.0.0`). The workflow checks out that ref before building.

Example: build exactly what is on `main` at commit `abc1234` even if `main` has moved since:

```
ref: abc1234
```

## Building locally (same output as CI)

From the repository root:

```bash
cp game.example.yml game.yml
./gradlew :or-cache:freshCache :or-cache:mergePluginGamevals :server:app:shadowJar --no-daemon
```

The runnable jar is written to:

```
server/app/build/libs/server.jar
```

To assemble a release bundle like CI:

```bash
mkdir -p release-bundle/.data
cp server/app/build/libs/server.jar release-bundle/
cp game.yml release-bundle/
rsync -a --exclude 'raw-cache' .data/ release-bundle/.data/
cd release-bundle && zip -r ../openrune-server-release.zip .
```

Run the server from the extracted bundle directory:

```bash
java -jar server.jar
```

## Jar naming

The shadow jar task in `server/app/build.gradle.kts` sets `archiveFileName` to `server.jar`. CI and local `shadowJar` builds both use that name. Do not rename the jar in the workflow unless you also update the Gradle task.

## Troubleshooting

| Problem | Likely cause |
|---------|----------------|
| Workflow not listed under Actions | Workflow file must exist on the default branch (`main`) before GitHub exposes it for manual runs. |
| `freshCache` times out | Cache build is heavy; the job allows up to 120 minutes. Re-run if GitHub runner load caused a slow build. |
| Missing RSA keys at runtime | The server generates keys on first startup if `.data/game.key` is absent. |
| Plugin gamevals missing in release | Ensure `:or-cache:mergePluginGamevals` ran before bundling `.data/`. |
| Wrong branch in release title | When using **ref**, the release name reflects the `ref` value, not only the workflow dropdown branch. |
