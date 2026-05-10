# OSRS Wiki MCP Server

Stdio [MCP](https://modelcontextprotocol.io/) server: OSRS Wiki search/pages, **`wiki_npc_spawns`**, **gameval** lookup, and **decoded cache** search. For local IDE use only; the game server does not depend on this module.

| | |
| --- | --- |
| **Gradle** | `:tools:osrs-mcp` |
| **Main** | `org.rsmod.tools.mcp.wiki.MainKt` |
| **Transport** | stdio (`StdioServerTransport`) |

## Quick start

From repo root:

```bash
./gradlew :tools:osrs-mcp:runMcp
```

Blocks until the MCP client disconnects. Jars for `java -cp`:

```bash
./gradlew :tools:osrs-mcp:installDist
```

Output: `tools/osrs-mcp/build/install/osrs-mcp/lib/`

**Wire into Cursor / VS Code / Claude / IntelliJ:** `./gradlew :tools:osrs-mcp:configureOsrsMcp` (interactive menu) or `./gradlew configureOsrsMcp`. Non-interactive: `-Pclient=cursor` (also `vscode`, `claude`, `intellij`, `all`). Depends on `installDist`.

**Refresh configs after dependency changes:** `./gradlew updateOsrsMcp` (optional `-Pclient=...`, `-PdryRun=true`).

**Remove wiring:** `./gradlew removeOsrsMcp` — strips `osrs-mcp` from `.cursor/mcp.json`, `.vscode/mcp.json`, repo Claude fragment / IntelliJ notes; optionally Claude Desktop global config (backup `.osrs-mcp.bak`). Flags: `-Pclient=`, `-PskipClaudeGlobal=true`, `-PremoveInstall=true`, `-PdryRun=true`.

Use the repo folder as the workspace root so `${workspaceFolder}` in generated JSON matches your clone.

## Client config (minimal)

Replace `<repo-root>` with your clone path. Run `installDist` first.

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

If **`gameval_search`** finds nothing: set `RSPS_ROOT` to the repo root and ensure `.data/gamevals-binary/gamevals.dat` (and `gamevals_columns.dat`) exist.

## Tools (summary)

| Tool | Role |
| --- | --- |
| `wiki_search` | Wiki search (`query`, `limit` 1–10) |
| `wiki_page` | Page text by title (`title`, `maxChars`) |
| `wiki_npc_spawns` | `{{LocLine}}` spawns + infobox NPC ids → `npc.*` via gamevals (`title`, optional `npcName`, `location`) |
| `gameval_search` | Merged gamevals (`query` and/or `id`, optional `table`, `limit`) |
| `gameval_reload` | Reload gamevals from disk |
| `cache_reload` | Drop `cache_search` indexes |
| `reload_all` | `gameval_reload` + `cache_reload` |
| `cache_search` | Decoded cache (`cache`: `LIVE` \| `SERVER`, `type`: `npc`, `obj`, …, `query` and/or `id`, `limit`) |

**`cache_search`:** require `cache` + `type`, and at least one of `query` or `id`. Long `data:` lines are truncated (~800 chars); **search by `id`** for a single compact hit.

## Example output

### `wiki_npc_spawns` — `title`: `King Black Dragon`

Wiki: [King Black Dragon](https://oldschool.runescape.wiki/w/King_Black_Dragon)

```
Found 1 spawn entries on 'King Black Dragon':
1. King Black Dragon
   Location: King Black Dragon Lair (Wilderness)
   Levels: 276
   Members: Yes
   Map ID: 26
   Plane: 0
   Spawn count: 1
   Coordinates: x:3109,y:10265

== Infobox NPC IDs (resolved via loaded gamevals) ==
id:
  - id 239 -> npc.king_dragon
  - id 2642 -> npc.twocats_kbd_cutscene
```

Gameval resolution needs `.data/gamevals-binary/` (and merged content/RSCM as elsewhere in this repo).

### `cache_search` — SERVER npc id 239 (`npc.king_dragon`)

```json
{ "cache": "SERVER", "type": "npc", "id": 239 }
```

```
Cache: SERVER
Found 1 cache matches; showing 1.
1. [npc] 239 - King Black Dragon
   combat=276, size=5, hp=240
   data: id=239; name=King Black Dragon; size=5; category=347; models=[17414, 17415, 17429, 17422, 17423]; chatheadModels=null; standAnim=90; rotateLeftAnim=-1; rotateRightAnim=-1; walkAnim=4635; rotateBackAnim=-1; walkLeftAnim=-1; walkRightAnim=-1; actions=EntityOpsDefinition(ops=[1=Attack], subOps=[], conditionalOps=[], conditionalSubOps=[]); originalColours=null; modifiedColours=null; originalTextureColours=null; modifiedTextureColours=null; multiVarBit=-1; multiVarp=-1; multiDefault=-1; transforms=null; isMinimapVisible=true; combatLevel=276; widthScale=128; heightScale=128; renderPriority=0; ambient=0; contrast=0; headIconGraphics=null; headIconIndexes=null; rotation=32; ... (truncated; search by id for full data)
Rerun with narrower filters for a smaller result set.
```

Requires `.data/cache/SERVER`, repo `game.yml` with `revision:`, and root resolution (same order as below).

## Local data (`gameval_search`)

Paths under repo root:

- `.data/gamevals-binary/gamevals.dat`
- `.data/gamevals-binary/gamevals_columns.dat`

Root: parent of `LOG_DIR` if dats exist there → `RSPS_ROOT` → classpath-derived roots → cwd walk. **`gameval_reload`** picks up edits without restarting MCP.

## Local cache (`cache_search`)

- `.data/cache/LIVE` and/or `.data/cache/SERVER`
- `game.yml` with `revision:`

Root: parent of `LOG_DIR` if that cache dir exists → `RSPS_ROOT` → cwd walk. **`cache_reload`** (or **`reload_all`**) after replacing cache exports or bumping revision.

## Logging

| | |
| --- | --- |
| Default | `logs/osrs-mcp.log` |
| Override | `LOG_DIR` |

## Code map

| File | Role |
| --- | --- |
| `Main.kt` | MCP bootstrap |
| `McpServerTools.kt` | Tool registration |
| `WikiTool.kt` | Wiki / gameval / cache formatting |
| `WikiClient.kt` | MediaWiki HTTP |
| `WikiInfoboxNpcIds.kt` | Infobox id lines |
| `GameValTool.kt` | Gameval load + search |
| `CacheTool.kt` | Cache index + lookup |
