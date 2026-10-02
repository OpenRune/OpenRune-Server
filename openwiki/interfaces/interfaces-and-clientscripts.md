---
type: subsystem
title: Interfaces and clientscripts
description: How custom interfaces and CS2 clientscripts are authored in -pack modules (.if3 definitions, cs2 sources and symbol files), compiled into the cache, opened and wired from PluginScripts (ifOpen*, ifSetEvents, onIfModalButton, onIfClose, runClientScript), and the visual conventions to follow.
tags: [interfaces, cs2, clientscripts, ui, packs]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-1a201b11741922d65098e4ff
    resource: repo://api/player-output/src/main/kotlin/org/rsmod/api/player/output/ClientScripts.kt
  - id: openwiki-source-76ab5e99fb04b5dec247aca7
    resource: repo://api/player/src/main/kotlin/org/rsmod/api/player/ui/PlayerInterfaceExtensions.kt
  - id: openwiki-source-73c0a9ffc8c888c2ec8279d0
    resource: repo://content/other/spawn/pack/src/main/resources/pack/configs/spawn.toml
  - id: openwiki-source-a7605a66ae809a51390b2a0c
    resource: repo://content/other/spawn/pack/src/main/resources/pack/interfaces/spawn_menu.if3
  - id: openwiki-source-d013a6b2507c81de8454805e
    resource: repo://content/other/spawn/src/main/kotlin/org/rsmod/content/other/spawn/SpawnMenuScript.kt
  - id: openwiki-source-e047734423639c9255eb5e5b
    resource: repo://or-cache/src/main/kotlin/dev/openrune/CacheTools.kt
  - id: openwiki-source-4e493fd0520e2277be56aea3
    resource: repo://or-cache/src/main/kotlin/dev/openrune/pack/PluginPacks.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Interfaces and clientscripts

Interfaces involve two halves: **cache data** (the component tree and clientscripts, built into
the cache from a `-pack` module) and **server logic** (a `PluginScript` that opens the interface,
enables click events and reacts to them).

## Cache side: the pack module

The reference example is the admin item spawner, `content/other/spawn`:

```text
content/other/spawn/
├── src/main/kotlin/.../SpawnMenuScript.kt          server logic
└── pack/
    ├── src/main/kotlin/.../SpawnPluginPack.kt      class SpawnPluginPack : PluginPack()
    └── src/main/resources/pack/
        ├── interfaces/spawn_menu.if3               component tree
        ├── cs2/script/[clientscript,spawn_menu_*].cs2, [proc,...].cs2
        └── configs/spawn.toml                      inv / varp / varbit definitions
```

### `.if3` interface definitions

An `.if3` file declares one `[[interface]]` (gameval `name`, `width`/`height`, `on_load` script
call with its component arguments) followed by `[[component]]` entries (`name`, `type` such as
`layer`/`text`/graphic, `parent`, `x`/`y`/`width`/`height`, `options`, `text`, `font`, alignment,
`color`, `no_click_through`...). Components are referenced in Kotlin as
`"component.<interface>:<name>"`.

During `buildCache`, `PluginPacks.interfaceTasks()` creates one `PackIfType` task from every
active pack's `interfaces/` directory plus any `InterfaceType`s a pack returns from
`PluginPack.interfaces()` (a Kotlin builder path that is still supported by the pack API).
Interfaces are client data only — they are packed into the LIVE cache, not the SERVER cache.
See [Cache build and gamevals](../cache/cache-build-and-gamevals.md).

### CS2 sources and symbols

`PluginPacks.cs2Overrides` hands every pack's `cs2/script/` directory and loose `.cs2` files to the
CS2 compiler (Neptune) in place. Symbols for compilation come from three layers, later winning:

1. dumped `symbols/` files in the CS2 working directory;
2. symbols derived from gamevals for custom ids above the cache's max base id (so new
   interfaces/components/invs/etc. need no hand-written `.sym` files);
3. each pack's `cs2/symbols/*.sym` files — still required for tables gamevals cannot express
   (`varp`, `varc`, `param`, `dbcolumn`, `if_script`, `clientscript`, `commands`), e.g.
   `content/quest/pack/.../cs2/symbols/clientscript.sym`.

The default game scripts are unpacked first (`UnpackDefaultCs2`), so a pack script with the same
name overrides the official one. `gradlew cleanCs2` wipes the CS2 working directory.

For the real component trees and script logic of official interfaces, consult the
[Joshua-F/osrs-dumps](https://github.com/Joshua-F/osrs-dumps) `interface/` and `script/` dumps;
a local `osrs-dumps/` checkout may also be present in the repo root.

## Server side: wiring in a PluginScript

UI helpers live in `api/player/.../ui/PlayerInterfaceExtensions.kt` and on `ProtectedAccess`:

| Need | API |
|---|---|
| open | `ifOpenMainModal`, `ifOpenMain`, `ifOpenSide`, `ifOpenMainSidePair`, `ifOpenOverlay`, `ifOpenFullOverlay` |
| close | `ifClose`, `ifCloseModals`, `ifCloseInputDialog` |
| enable clicks | `ifSetEvents("component.x:y", slotRange, IfEvent.Op1, ...)` — without this the client sends nothing |
| react | `onIfModalButton("component.x:y")`, `onIfOpen(...)`, `onIfClose(...)`, `onIfScriptTrigger<Args>(...)` |
| drive client logic | `player.runClientScript(id, args...)` (`api/player-output/.../ClientScripts.kt`) plus named wrappers on `ClientScripts` |
| item grids | `invTransmit` / `startInvTransmit` / `stopInvTransmit` |

`SpawnMenuScript` shows the full pattern: `onCommand("spawn")` launches protected access and
`ifOpenMainModal("interface.spawn_menu")`, sets `IfEvent.Op1` on every grid slot and
`ScriptTrigger` on the search text, transmits a temp inventory (`inv.spawn_results`, defined in
the pack's `configs/spawn.toml`) to fill the grid, sends labels with
`runClientScript("clientscript.spawn_menu_labels".asRSCM(RSCMType.CLIENTSCRIPT), ...)`, and
cleans up state in `onIfClose`. Modal-button handlers run as `ProtectedAccess`, so a suspension
such as `countDialog(...)` is aborted automatically if the modal is closed (see
[Events, coroutines and ProtectedAccess](../architecture/events-and-coroutines.md)).

Built-in game interfaces (bank, equipment, prayer, settings, collection log, combat
achievements, world map, xp drops...) live under `content/interfaces/`.

## Design conventions

New interfaces should be indistinguishable from Jagex ones (`AGENTS.md` has the full rules):

- reuse the standard steel-border frame component (its CS2 draws the border, title and close
  button — never add your own); never the legacy iron border for new work;
- standard sizes: full modal 512×334, 36 px title strip, 10 px content inset, 16 px scrollbar
  with a 2 px gap;
- colours: orange `#FF981F` labels/titles, white values, yellow hover/selected, red errors, green
  success, grey disabled; text over textures gets a 1 px black shadow;
- cache fonts only (`p11`, `p12`, `b12`, quill fonts); sprites over flat rectangles; reuse
  standard scrollbar, buttons, dropdowns, checkboxes and tabs.

Verify layouts in a live client with the OpenRune Developer Tools MCP (`screenshot`,
`dump_interface`); see [Testing](../testing/testing.md).
