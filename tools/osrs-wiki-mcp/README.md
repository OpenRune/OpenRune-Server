# OSRS Wiki MCP Server

Local MCP stdio server for Old School RuneScape Wiki + gameval lookup.

## Scope

- Local usage first.
- Integrated in this repository as Gradle subproject `:tools:osrs-wiki-mcp`.
- Main game server runtime does not depend on this module.

## Quick Start (Repo Root)

Start MCP server via Gradle task:

- `:tools:osrs-wiki-mcp:run`

Build local launcher script (optional) via Gradle task:

- `:tools:osrs-wiki-mcp:installDist`

Launcher output:

- `tools/osrs-wiki-mcp/build/install/osrs-wiki-mcp/bin/osrs-wiki-mcp.bat` (Windows)
- `tools/osrs-wiki-mcp/build/install/osrs-wiki-mcp/bin/osrs-wiki-mcp` (Linux/macOS)

## MCP Client Command

Use either command style in MCP client config.

Gradle-backed launch:

- run task `:tools:osrs-wiki-mcp:run`

Installed launcher:

- use generated launcher under `tools/osrs-wiki-mcp/build/install/osrs-wiki-mcp/bin`
- Windows command path: `.../bin/osrs-wiki-mcp.bat`
- Linux/macOS command path: `.../bin/osrs-wiki-mcp`

Transport is stdio (`StdioServerTransport`). Server waits for MCP requests on stdin.

## IntelliJ Copilot MCP Setup

1. Generate launcher (`:tools:osrs-wiki-mcp:installDist`).
2. Open Copilot MCP config file:
   - `%LOCALAPPDATA%\\github-copilot\\intellij\\mcp.json`
3. Add server entry (adjust paths for your machine).

Windows example:

```json
{
  "servers": {
    "osrs-wiki-mcp": {
      "type": "stdio",
      "command": "<repo-root>\\tools\\osrs-wiki-mcp\\build\\install\\osrs-wiki-mcp\\bin\\osrs-wiki-mcp.bat",
      "args": [],
      "env": {
        "RSPS_ROOT": "<repo-root>",
        "LOG_DIR": "<repo-root>\\logs"
      }
    }
  }
}
```

Linux/macOS example:

```json
{
  "servers": {
    "osrs-wiki-mcp": {
      "type": "stdio",
      "command": "<repo-root>/tools/osrs-wiki-mcp/build/install/osrs-wiki-mcp/bin/osrs-wiki-mcp",
      "args": [],
      "env": {
        "RSPS_ROOT": "<repo-root>",
        "LOG_DIR": "<repo-root>/logs"
      }
    }
  }
}
```

4. Reload Copilot/IntelliJ so MCP servers restart.
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

## Code Map

- `src/main/kotlin/org/rsmod/tools/mcp/wiki/Main.kt` - MCP server bootstrap + tool registration
- `src/main/kotlin/org/rsmod/tools/mcp/wiki/WikiToolService.kt` - tool behavior + formatting
- `src/main/kotlin/org/rsmod/tools/mcp/wiki/wiki/OsrsWikiClient.kt` - MediaWiki client
- `src/main/kotlin/org/rsmod/tools/mcp/wiki/GamevalIndex.kt` - gameval load/search
