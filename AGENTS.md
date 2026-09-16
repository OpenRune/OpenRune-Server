# OpenRune-Server — Agent Guide

OSRS-compatible game server (revision 240.2), Kotlin, modular fork of RSMod/Alter.
Gameplay ships as auto-loaded plugins in `content/` — core engine code rarely needs
touching.

## Build & run

- Java 21 toolchain (tools modules target JVM 17). Kotlin, Gradle.
- First-time setup: `gradlew install`
- Run the server: `gradlew run` (delegates to `:server:app:run`, main class
  `org.rsmod.server.app.GameServerKt`). Success prints "OpenRune Server Successfully initialized".
- Tests: JUnit 5, parallel by default. Run with `gradlew test` or per-module.
- Cache tasks (or-cache module): `buildCache`, `freshCache`, `cleanCs2`,
  `mergePluginGamevals`. Cache data comes from OpenRS2 `.data/`.
- Typical content loop: edit Kotlin in `content/` → `gradlew run` and test. If you
  touched a `-pack` module's resources or a `gamevals.toml`, rebuild the cache
  (`buildCache`) before running.

## Modules

| Module | Purpose |
|---|---|
| `engine/` | Game loop, event bus, routing, coroutines, `PluginScript` base + module framework |
| `api/` | ~40 submodules of DI-managed domain logic: combat, stats, shops, cache, player state, interactions. This is the API plugins consume |
| `content/` | Gameplay plugins by category: `areas`, `bosses`, `drops`, `events`, `interfaces`, `quest`, `skills`, `travel`, `other` |
| `server/` | Runtime: `app` (entry), `install`, `logging`, `services` (DB/central), `shared` (plugin loader) |
| `or-cache/` | Cache builder (main: `dev.openrune.CacheToolsKt`); merges gamevals from content `-pack` modules |
| `tools/` | `osrs-mcp`, `wiki-dumping`, progress generator |

## Writing content plugins

New gameplay = new `PluginScript` subclass in a `content/` submodule. No registration:
`PluginScriptLoader` classpath-scans for `PluginScript` subclasses at startup and Guice
injects `@Inject` constructors.

```kotlin
class Bob @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bob") { startDialogue(it.npc) }
        onOpNpc3("npc.bob") { player.openShop(it.npc) }
    }
}
```

Conventions:
- Package `org.rsmod.content.<category>.<subcategory>`; reference example:
  `content/areas/city/lumbridge/` (LumbridgeScript.kt, npcs/Bob.kt).
- Bind handlers in `startup()` with the DSL from `api/script`: `onOpNpc1..5`,
  `onOpLoc1..5`, `onOpHeldU`, `onItemOnItem`, `onUnimplementedOpNpcN`, etc.
  Identifiers are gameval symbols like `"npc.bob"`, `"loc.winch"`.
- Blocking flows (dialogue, animation waits) are `suspend` functions running through
  the coroutine/`ProtectedAccess` DSL.
- Content `-pack` sibling modules hold cache data (`src/main/resources`); script
  modules hold Kotlin. Pack modules are merged into the cache automatically.
- Drop tables: TOML or Kotlin `@RegisterDropTable` DSL — see `docs/drops.md`.

## State storage — attrs are a last resort

The `Attrs`/`AttributeKey` system is for state that genuinely doesn't fit anywhere
else. Reach for it last, not first:

- Basic state (enums, ints, booleans, timers, counters, flags) should go through
  varbits/varps, not attrs.
- Transient/session state (cleared on logout, not needed across ticks/reconnects)
  → temporary varbits/varps.
- Persistent state (needs to survive logout/save) → permanent varbits/varps.
- Only use `AttributeKey`/attrs when the data can't be represented as a varbit/varp
  (e.g. complex objects, non-serializable runtime-only references) — not as a
  shortcut to avoid registering a varbit/varp.

## Gamevals

Gamevals are the symbolic name → id mappings behind every `"npc.bob"` /
`"loc.winch"` style reference. Scripts and configs always use these symbols; never
hardcode raw numeric ids in content code.

- Storage: `.data/gamevals/*.rscm` text tables (`obj`, `area`, `category`, `content`,
  `currency`, `enum`, `dbtable`/`dbrow`, `param`, `stat`, `varbit`/`varp`/`varn`/
  `varobj`/`varcon`, `clientscript`, `timer`, `queue`, `controller`, `walktrigger`,
  etc.), one `name=id` per line. NPC/loc/interface-component symbols live in the
  binary dumps under `.data/gamevals-binary/` (`gamevals.dat`,
  `gamevals_components.dat`, `max-ids.toml`), produced from the cache.
- Reference format in scripts: `"<table>.<name>"`, e.g. `onOpNpc1("npc.bob")`,
  `onOpLoc1("loc.winch")`, and the same symbols in drop tables and TOML configs.
- Custom symbols for new content: add a `gamevals.toml` in the content module's
  `src/main/resources` with `[gamevals.<table>]` sections:

  ```toml
  [gamevals.obj]
  poh_tablet_shootingstar = 63477

  [gamevals.dbrow]
  shooting_star_dwarven_mine = 64678
  ```

  `PluginGamevalMerger` walks `content/` for these files and merges them into
  `.data/gamevals` during the cache build (`mergePluginGamevals` /
  `buildCache`) — re-run it after adding or changing symbols. Custom ids sit in the
  high range (~63000-65535 in existing modules) to stay clear of cache ids.
  Working examples: `content/events/shooting-stars`, `content/generic/generic-locs`,
  `content/interfaces/collection-log`.

## Interfaces & CS2 (clientscripts)

How custom interfaces flow through the stack.

Server side (Kotlin):

- Interface layouts are declared with the `buildInterface()` DSL inside a content
  module's `-pack` submodule: internal name, width/height, nested layers, text,
  fonts, scrollbars, and IfEvent click masks. The reference example in this repo is
  `content/other/spawn/pack/src/main/kotlin/org/rsmod/content/other/spawn/pack/SpawnInterface.kt`
  — a full 512x334 window with title bar, search bar, button row, scrollable grid
  and scrollbar, with the layout expressed as named constants; copy its structure.
  `content/other/toolbelt/pack/.../ToolbeltInterface.kt` is a second, smaller example.
- Open/close with the player UI API (`ifOpenMainSidePair` and friends); react with
  `onIfOpen("interface.bankside")` / `onIfClose(...)` in a `PluginScript`.
- The server drives client behaviour with
  `runClientScript(scriptId, ...args)` (`api/player-output/.../ClientScripts.kt`),
  which also has named wrappers for common scripts (menus, chatbox inits, etc.).

Client side (FluxRSClient repo):

- CS2 lives as `.rs2asm` assembly in `runelite-client/src/main/scripts` (one file
  per script, `.id` header = script id). The gradle `AssembleTask` compiles them
  into the client cache overlay at build time.
- `runelite-api/src/main/interfaces/interfaces.toml` maps interface names to ids and
  component names to child indices (`[bank] id=12 ... scrollbar=13`); it feeds both
  the script assembler and generated Java component constants. Add named entries
  here instead of hardcoding child indices in scripts.

Reference dumps — how Jagex actually built things:

- [Joshua-F/osrs-dumps](https://github.com/Joshua-F/osrs-dumps) holds raw dumps of
  the packed cache: decompiled CS2 under
  [`script/`](https://github.com/Joshua-F/osrs-dumps/tree/master/script) (one `.cs2`
  per clientscript, e.g.
  [`[clientscript,1v1arena_clear_opbutton].cs2`](https://github.com/Joshua-F/osrs-dumps/blob/master/script/%5Bclientscript%2C1v1arena_clear_opbutton%5D.cs2))
  and interface definitions under
  [`interface/`](https://github.com/Joshua-F/osrs-dumps/tree/master/interface)
  (one `.if3` per interface, e.g.
  [`1v1arena_results.if3`](https://github.com/Joshua-F/osrs-dumps/blob/master/interface/1v1arena_results.if3)).
- When building or debugging an interface or clientscript, look up the closest
  official equivalent there to see the real component tree, ops and script logic
  before writing your own.

## Interface design

Goal: a custom interface should be indistinguishable in style from a Jagex one.
[osrs.design](https://osrs.design) is the visual reference gallery; the working rules
are below so you rarely need to look things up.

### Borders and frames

- **Steel border** — the modern standard window frame: grey riveted metal edging,
  9-sliced (corner sprites + tiled edge sprites, never stretched). Use it for every
  new or reworked interface. Newer OSRS interfaces (collection log, settings,
  achievement diaries) all use it.
- **Iron border** — the darker legacy frame on old interfaces. Do not use it for new
  work; only keep it when editing an existing iron-framed interface where restyling
  is out of scope.
- **Thin border** — the 1px bevel box (light top/left, dark bottom/right inset look)
  for structure inside a window: list boxes, section panels, item wells, input
  areas. Reuse the shared `thin_border` components rather than drawing new rects.
- Border sprites are 9-slice sets: corners fixed, edges tiled. Layer order: frame on
  top, content clipped inside, background behind.

### Colors

- Orange `#FF981F` — static labels and titles (the classic interface orange).
- White `#FFFFFF` — dynamic values and body text.
- Yellow `#FFFF00` — hover state, selected options, clickable option text.
- Red `#FF0000` — warnings/errors/disabled destructive; green `#00FF00` — success or
  positive confirmation.
- Grey `#9F9F9F` — disabled/inactive text.
- All text over textured backgrounds gets the 1px black drop shadow (down-right);
  never plain unshadowed text on parchment/stone.
- Backgrounds come from the standard sprite fills (dark brown panel, parchment
  scroll) — reuse existing background sprites, never flat-color rectangles.

### Typography

- Cache fonts only: `p11` (small plain), `p12` (regular plain), `b12` (bold) and the
  quill fonts for quest/scroll styling. No custom fonts.
- Titles: centered at the top of the frame, orange, usually `b12`. Labels orange,
  values white, one size step apart at most. Don't mix more than two fonts per
  interface.

### Components — reuse, never rebuild

- **Interface frame**: reuse the standard frame component — its CS2 automatically
  builds the steel border, the centered title text AND the close button. Never add
  your own title text or close X on top of it; just supply the title string.
- **Buttons**: standard stone buttons (gold-to-yellow gradient text on stone) with
  hover state; primary/secondary/danger variants exist — copy from a shipped
  interface.
- **Scrollbar**: the standard 16px scrollbar (up/down arrows + draggable thumb) —
  wire the existing scrollbar component/CS2, don't build one.
- **Dropdowns**: reuse the existing dropdown layers (content layer + popup layer that
  overlays the list, as in the production interface's `category_dropdown_popup`).
- **Checkboxes/toggles, tabs, progress bars, tooltips**: all have standard sprites
  and scripts — lift them from bank/settings/collection log instead of inventing.
- **Sprites over rectangles**: build visuals from existing cache sprites that match
  the interface style (frames, fills, dividers, slot backgrounds, icons) instead of
  plain rectangle/filled-rect components. Rectangles are a last resort for things no
  sprite covers — and even then match the palette; a flat rect next to textured
  sprites reads instantly as custom.
- General rule: find the shipped interface closest to what you're building and copy
  its component structure, sprite ids and spacing, then change only the content.

### Standard sizes and metrics

Baseline numbers, taken from this repo's `SpawnInterface.kt` and the OSRS client:

- Full-screen modal window: **512x334** (the fixed-mode viewport; never exceed it —
  it must also be checked in resizable mode).
- Title bar strip: **36px** tall, title text centered in it.
- Content inset from the frame: **10px** on each side.
- Scrollbar: **16px** wide with a **2px** gap to the content it scrolls; flush right.
- Button rows: buttons ~**48px** wide on a **52px** pitch (4px gap), row height
  **20px** with 4px vertical padding.
- Interface gold/orange: **`0xff981f`** — same value everywhere (titles, labels,
  button text).

### Layout rules

- Title bar top-center and close button top-right come free from the frame
  component's CS2 — never place them manually. Keep consistent inner padding from
  the frame; scrollable lists sit flush against their scrollbar on the right.
- Express layout as named constants derived from each other (see
  `SpawnInterface.kt`: `CONTENT_W = WIDTH - INSET * 2`,
  `GRID_W = CONTENT_W - SCROLLBAR_W - SCROLLBAR_GAP`) so spacing stays consistent
  when sizes change.
- Verify with `screenshot {interfaceId}` + `dump_interface` against an existing
  Jagex interface of the same shape — bounds, spacing and colors should match.

## Config

- `game.yml`: name, `game-port: 43594`, `revision: 240.2`, environment, world id,
  central-server link (PostgreSQL). `game.example.yml` documents extra keys, e.g.
  `gameplay.quest-requirements.mode` (`assume-completed` / `respect-progress` /
  `virtual-completions`) and remote central config.

## Docs worth reading before touching a system

`docs/`: `drops.md` (drop table DSL + wiki dumper), `doors.md`, `gates.md` (quest and member
access gates), `instances.md`, `ironman.md`, `boss-hp-bar.md`, `quirks.md` (known OSRS
deviations), `external-plugins.md` (loading/hot-loading plugins from outside this repo's build),
`RELEASE_CI.md` (release zip CI). `PROGRESS.md` is generated by
`tools/progress/content-progress.mjs` — never edit by hand.

## Code style

- ktlint 1.5.0 via Spotless, ratcheted from `origin/main` (only changed files checked).
- `.editorconfig`: 4-space indent, 100-char lines, LF. Most ktlint standard rules are
  off; only the auto-correctable hygiene allow-list applies.

### Comments — less is more

Default to no comments; the code and its names should carry the meaning.

- Only document methods that are genuinely complex AND large — a short KDoc explaining
  the why/algorithm. Small or obvious methods get nothing, however important they are.
- No single-line comments restating what the next line does (`// open the shop`,
  `// loop over players`). Delete these on sight.
- No mid-method comments except inside truly complex logic where a non-obvious
  invariant, game quirk, or ordering constraint needs stating — and then one line,
  about the constraint, not the mechanics.
- Same rule for variables and properties: no comment on a field whose name already
  says what it is. Rename instead of commenting.
- Never leave narration comments about a change you just made ("now uses X",
  "fixed the bug where..."); that belongs in the commit message.

## Testing changes in a live client

The [OpenRune-Developer-Tools](https://github.com/OpenRune/OpenRune-Developer-Tools)
client plugin runs an MCP server inside the game client (`http://127.0.0.1:7780/mcp`)
that lets an AI agent drive and observe the game directly: screenshot interfaces,
inspect widgets by packed id, read varbits/varps/clientscript/chat history, click and
drag components, walk dialogue trees, interact with NPCs/objects/items, and block on
game conditions.

Every call is visible live on the dashboard at `http://127.0.0.1:7780/` — the user
watches there while an agent works, so there is no need to narrate each tool call.
It shows every MCP call as it happens with arguments, duration, pretty-printed JSON
results and screenshot thumbnails (click to zoom), plus session stats
(calls/errors/avg ms), a tool-name dropdown filter, errors-only toggle and
pause/follow controls. It keeps the last 100 calls, backed by `GET /log?after=<id>`
on the same port.

When verifying server content in-game, prefer these tools over asking the user to
test manually. The plugin self-updates from GitHub releases on client startup.

### Install / connect (one-time)

Download the latest release jar into each client's sideload folder:

The scripts below download the jar, register the MCP server with Claude Code, and
allow all `flux` tools so agents aren't stopped by per-call permission prompts. Run
them from the repo root.

Windows (PowerShell):

```powershell
foreach ($dir in ".runelite", ".rsprox") {
  $target = "$env:USERPROFILE\$dir\sideloaded-plugins"
  New-Item -ItemType Directory -Force $target | Out-Null
  curl.exe -L -o "$target\OpenRune-Developer-Tools.jar" `
    https://github.com/OpenRune/OpenRune-Developer-Tools/releases/latest/download/OpenRune-Developer-Tools.jar
}
claude mcp add --transport http flux http://127.0.0.1:7780/mcp
New-Item -ItemType Directory -Force .claude | Out-Null
if (-not (Test-Path .claude\settings.local.json)) {
  Set-Content -Encoding utf8 .claude\settings.local.json '{ "permissions": { "allow": ["mcp__flux"] } }'
}
```

Linux / macOS:

```bash
for dir in .runelite .rsprox; do
  mkdir -p "$HOME/$dir/sideloaded-plugins"
  curl -L -o "$HOME/$dir/sideloaded-plugins/OpenRune-Developer-Tools.jar" \
    https://github.com/OpenRune/OpenRune-Developer-Tools/releases/latest/download/OpenRune-Developer-Tools.jar
done
claude mcp add --transport http flux http://127.0.0.1:7780/mcp
mkdir -p .claude
[ -f .claude/settings.local.json ] || \
  echo '{ "permissions": { "allow": ["mcp__flux"] } }' > .claude/settings.local.json
```

Then start the client with `--developer-mode` (sideloaded plugins only load then)
and enable **OpenRune-DeveloperTools** once in the plugin sidebar (persists).

If `.claude/settings.local.json` already exists, the scripts leave it alone — merge
`"mcp__flux"` into its `permissions.allow` array manually.

### Example prompts

Plain requests map straight onto the tools — one or two calls, no screenshots needed:

| Ask | What the agent should do |
|---|---|
| "drop a shark" | `item_action {itemName: "shark", option: "Drop"}` |
| "give me the dialogue for the Wise Old Man" | `interact_npc {npcName: "wise old man"}` then `get_dialogue`, `select_option`/`continue_dialogue` through every branch, report the tree |
| "what right-click options does the banker have?" | `get_npc_menu {npcName: "banker"}` |
| "open the vote interface on the vote npc" | `get_npc_menu {npcName: "vote"}` then `click_menu_option {option: "Vote"}` |
| "chop a tree and prove it worked" | `interact_object {nameFilter: "tree", option: "Chop down"}` then `wait_for {condition: "chat_message", textContains: "logs"}` |
| "what's in my bank?" | `get_inventory {inventoryId: 95}` |
| "pick up the coins on the floor" | `list_ground_items {nameFilter: "coins"}` then `pickup_item` with the returned id/tile |
| "which varbits change when I toggle run?" | toggle via `click_component`, then `get_var_history {sinceMs: 3000}` |
| "does my new interface layout match the design?" | `screenshot {interfaceId: <id>}` + `dump_interface` and compare bounds |
| "walk to 3222, 3218" | `walk_to {x: 3222, y: 3218}` then `wait_for {condition: "at_tile", x: 3222, y: 3218}` |
| "spawn an abyssal whip" | `type_chat {text: "::item 4151"}` (server `::` commands) |

### Example verification flows

New NPC dialogue (after adding a `PluginScript` with `onOpNpc1`):

```
interact_npc {npcName: "bob"}
get_dialogue                       -> expect type "npc" with your text
select_option {option: 2}          -> walks a Chatmenu branch
continue_dialogue                  -> advances and returns the next state
```

Interface layout work:

```
list_interfaces                        -> find your interface (group) id
screenshot {interfaceId: 620}          -> tight PNG of the whole interface
dump_interface {interfaceId: 620}      -> every component with bounds/text; diff two dumps
get_widget {componentId: 40632324}     -> one component's values (packed id = group << 16 | child)
set_widget {componentId: ..., x: 12}   -> nudge layout client-side before editing server code
```

Skilling / mechanics:

```
interact_object {nameFilter: "tree", option: "Chop down"}
wait_for {condition: "chat_message", textContains: "some logs"}
get_var_history {sinceMs: 5000}        -> which varbits/varps the action changed
get_script_history {sinceMs: 3000}     -> which clientscripts an interface fired
item_action {itemName: "logs", option: "Drop"}
```

Combat / effects:

```
interact_npc {npcName: "goblin", option: "Attack"}
wait_for {condition: "npc_dead", npcName: "goblin", timeoutMs: 30000}
get_effect_history {sinceMs: 10000}    -> animations/gfx that played
get_projectile_history {sinceMs: 10000}
```

Movement and state: `walk_to {x, y}` + `wait_for {condition: "at_tile", x, y}`;
`get_client_state` for position/region/energy/camera; `get_skills` for levels/xp;
`get_inventory {inventoryId: 93}` for container contents. `::commands` go through
`type_chat {text: "::item 4151"}`.
