# OSRS Wiki MCP Server

Local MCP stdio server for Old School RuneScape Wiki + gameval lookup.

## Scope

- Local usage first.
- Integrated in this repository as Gradle subproject `:tools:osrs-wiki-mcp`.
- Main game server runtime does not depend on this module.

## Quick Start (Repo Root)

Start MCP server via Gradle task:

- `:tools:osrs-wiki-mcp:run`

Build classpath libs for external MCP clients (required for `java -cp` launch):

- `:tools:osrs-wiki-mcp:installDist`

Classpath output:

- `tools/osrs-wiki-mcp/build/install/osrs-wiki-mcp/lib/*`

## MCP Client Command

Use Java classpath launch in MCP client config.

- command: `java`
- args: `-cp <repo-root>/tools/osrs-wiki-mcp/build/install/osrs-wiki-mcp/lib/* org.rsmod.tools.mcp.wiki.MainKt`

Transport is stdio (`StdioServerTransport`). Server waits for MCP requests on stdin.

## IntelliJ MCP Setup

1. Build classpath libs (`:tools:osrs-wiki-mcp:installDist`).
2. In IntelliJ, open MCP server settings for your chat client/plugin.
3. Add an `osrs-wiki-mcp` stdio server entry there (adjust paths for your machine).

Example:

```json
{
  "servers": {
    "osrs-wiki-mcp": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-cp",
        "<repo-root>/tools/osrs-wiki-mcp/build/install/osrs-wiki-mcp/lib/*",
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

Use your OS path style if you prefer (`/` or `\\`).

4. Apply changes and restart MCP servers from IntelliJ.
5. If `gameval_search` fails to resolve data files, verify `<repo-root>\\.data\\gamevals-binary\\` exists and `RSPS_ROOT` points to repo root.

## Tools

### `wiki_search`

- Input:
  - `query` (string, required, non-empty)
  - `limit` (int, optional, default `5`, clamped `1..10`)
- Output:
  - Numbered wiki hits with title, URL, optional snippet.

### `wiki_page`

- Input:
  - `title` (string, required, non-empty)
  - `maxChars` (int, optional, default `6000`, clamped `500..20000`)
- Output:
  - Title, canonical URL, cleaned text excerpt.

### `wiki_npc_spawns`

- Input:
  - `title` (string, required, non-empty)
  - `npcName` (string, optional, exact name match ignoring case)
  - `location` (string, optional, contains match ignoring case)
- Output:
  - Parsed `{{LocLine}}` spawn entries with metadata and `x:...,y:...` lists.

### `gameval_search`

- Input:
  - `query` (string, optional)
  - `table` (string, optional)
  - `id` (int, optional)
  - `limit` (int, optional, default `10`, clamped `1..50`)
- Rule:
  - Provide at least one of `query` or `id`.
- Output:
  - Single exact match, or disambiguated list with narrowing hint.

### `cache_search`

- Input:
  - `cache` (string, required, `LIVE` or `SERVER`)
  - `type` (string, required: `npc`, `obj`, `item`, `anim`, `enum`, `struct`, `healthbar`, `hitsplat`, `varbit`, `varp`, `dbrow`, `dbtable`, `all`)
  - `query` (string, optional)
  - `id` (int, optional)
  - `limit` (int, optional, default `25`, clamped `1..100`)
- Rule:
  - Provide at least one of `query` or `id`.
- Output:
  - Ranked matches from decoded cache definitions; includes type, id, name, summary, and full field data dump.

## Local Data Resolution (`gameval_search`)

Required files:

- `.data/gamevals-binary/gamevals.dat`
- `.data/gamevals-binary/gamevals_columns.dat`

Root discovery order:

1. explicit `rootDir` (internal path)
2. parent of `LOG_DIR` (if files exist)
3. `RSPS_ROOT`
4. inferred classpath roots
5. current working directory and parents

If not found, server returns error asking for `RSPS_ROOT` or explicit root.

## Logging

- Default: `logs/osrs-wiki-mcp.log`
- Override with env var `LOG_DIR`

## Local Cache Resolution (`cache_search`)

Required directories:

- `.data/cache/LIVE`
- `.data/cache/SERVER`

Additional requirement:

- `game.yml` must exist at repo root and contain `revision:`.

Root discovery order:

1. parent of `LOG_DIR` (if selected cache exists)
2. `RSPS_ROOT` (if selected cache exists)
3. current working directory and parents

If selected cache is missing, tool returns error with expected path.

## Code Map

- `src/main/kotlin/org/rsmod/tools/mcp/wiki/Main.kt` - MCP server bootstrap + tool registration
- `src/main/kotlin/org/rsmod/tools/mcp/wiki/WikiTool.kt` - wiki/gameval/cache tool behavior + formatting
- `src/main/kotlin/org/rsmod/tools/mcp/wiki/WikiClient.kt` - MediaWiki client
- `src/main/kotlin/org/rsmod/tools/mcp/wiki/GameValTool.kt` - gameval load/search
- `src/main/kotlin/org/rsmod/tools/mcp/wiki/CacheTool.kt` - cache snapshot indexing + searchable definition lookups
