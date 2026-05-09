# OSRS Wiki MCP Server

Stdio-based [Model Context Protocol](https://modelcontextprotocol.io/) server for **Old School RuneScape Wiki** search and pages, **gameval** lookup, and **cache** definition search. Intended for local use (IDEs, MCP clients); the main game server does **not** depend on this module.

| | |
| --- | --- |
| **Gradle project** | `:tools:osrs-mcp` |
| **Main class** | `org.rsmod.tools.mcp.wiki.MainKt` |
| **Transport** | stdio (`StdioServerTransport`) — reads MCP messages from stdin |

---

## Quick start

From the **repository root**:

1. **Run the server** (stdio; blocks until the client disconnects):

   ```bash
   ./gradlew :tools:osrs-mcp:run
   ```

2. **Build an install layout** (fat classpath under `lib/` — needed for `java -cp ...` in client configs):

   ```bash
   ./gradlew :tools:osrs-mcp:installDist
   ```

   Libraries are written to:

   `tools/osrs-mcp/build/install/osrs-mcp/lib/`

---

## MCP client configuration

Point your MCP client at Java with the install `lib/*` classpath and the main class above. Replace `<repo-root>` with your clone path (use `/` or `\\` as your OS expects).

**Example (JSON):**

```json
{
  "servers": {
    "osrs-mcp": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-cp",
        "<repo-root>/tools/osrs-mcp/build/install/osrs-mcp/lib/*",
        "org.rsmod.tools.mcp.wiki.MainKt"
      ],
      "env": {
        "RSPS_ROOT": "<repo-root>",
        "LOG_DIR": "<repo-root>/logs"
      }
    }
  }
}
```

**IntelliJ / Cursor:** add a stdio MCP server entry with the same `command`, `args`, and `env`. Run `installDist` first so `lib/*` exists.

**If `gameval_search` cannot find data:** ensure `<repo-root>/.data/gamevals-binary/` contains the expected files and that `RSPS_ROOT` is the repo root (see [Local data](#local-data-gameval_search) below).

---

## Tools

### `wiki_search`

Search the wiki; returns numbered hits (title, URL, optional snippet).

| Parameter | Type | Required | Notes |
| --- | --- | --- | --- |
| `query` | string | yes | non-empty |
| `limit` | int | no | default `5`, clamped `1`–`10` |

### `wiki_page`

Load a page by title; returns title, canonical URL, and cleaned text excerpt.

| Parameter | Type | Required | Notes |
| --- | --- | --- | --- |
| `title` | string | yes | non-empty |
| `maxChars` | int | no | default `6000`, clamped `500`–`20000` |

### `wiki_npc_spawns`

Parse `{{LocLine}}` spawn data from a wiki page.

| Parameter | Type | Required | Notes |
| --- | --- | --- | --- |
| `title` | string | yes | non-empty |
| `npcName` | string | no | exact match, case-insensitive |
| `location` | string | no | substring match, case-insensitive |

Output includes metadata and `x:...,y:...` style coordinate lists.

### `gameval_search`

Look up rows in decoded gameval tables.

| Parameter | Type | Required | Notes |
| --- | --- | --- | --- |
| `query` | string | no | — |
| `table` | string | no | — |
| `id` | int | no | — |
| `limit` | int | no | default `10`, clamped `1`–`50` |

**Constraint:** provide at least one of `query` or `id`.

**Output:** one exact match, or a short disambiguation list with a narrowing hint.

### `cache_search`

Search decoded cache definitions (npc, obj, item, etc.).

| Parameter | Type | Required | Notes |
| --- | --- | --- | --- |
| `cache` | string | yes | `LIVE` or `SERVER` |
| `type` | string | yes | `npc`, `obj`, `item`, `anim`, `enum`, `struct`, `healthbar`, `hitsplat`, `varbit`, `varp`, `dbrow`, `dbtable`, `all` |
| `query` | string | no | — |
| `id` | int | no | — |
| `limit` | int | no | default `25`, clamped `1`–`100` |

**Constraint:** provide at least one of `query` or `id`.

**Output:** ranked matches with type, id, name, summary, and full field dump.

---

## Local data (`gameval_search`)

**Files (under repo root):**

- `.data/gamevals-binary/gamevals.dat`
- `.data/gamevals-binary/gamevals_columns.dat`

**Root resolution order:**

1. Internal explicit `rootDir` (if set)
2. Parent of `LOG_DIR`, if the files exist there
3. `RSPS_ROOT`
4. Inferred classpath roots
5. Current working directory and parents

If nothing resolves, the server returns an error asking you to set `RSPS_ROOT` or an explicit root.

---

## Local cache (`cache_search`)

**Directories:**

- `.data/cache/LIVE`
- `.data/cache/SERVER`

**Also required:** `game.yml` at the repo root with a `revision:` field.

**Root resolution order:**

1. Parent of `LOG_DIR`, if the selected cache directory exists
2. `RSPS_ROOT`, if the selected cache exists
3. Current working directory and parents

If the chosen cache path is missing, the tool errors with the expected path.

---

## Logging

| | |
| --- | --- |
| **Default log file** | `logs/osrs-mcp.log` (under cwd or as resolved by logback) |
| **Override** | set environment variable `LOG_DIR` |

---

## Code map

| File | Role |
| --- | --- |
| `src/main/kotlin/org/rsmod/tools/mcp/wiki/Main.kt` | MCP bootstrap and tool registration |
| `src/main/kotlin/org/rsmod/tools/mcp/wiki/WikiTool.kt` | Wiki, gameval, and cache tool behavior and formatting |
| `src/main/kotlin/org/rsmod/tools/mcp/wiki/WikiClient.kt` | MediaWiki HTTP client |
| `src/main/kotlin/org/rsmod/tools/mcp/wiki/GameValTool.kt` | Gameval load and search |
| `src/main/kotlin/org/rsmod/tools/mcp/wiki/CacheTool.kt` | Cache snapshot indexing and definition lookup |
