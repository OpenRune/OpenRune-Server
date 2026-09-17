# External Plugins

This document explains how to ship a plugin as a standalone artifact that the server loads from
outside its own build, and what you can and can't do with it once the server is already running.

A working, buildable starter template lives at `example-plugin/` — read its doc comment for exact
steps, edit it freely. Short version:

```
gradlew :example-plugin:jar
# copy example-plugin/build/libs/example-plugin.jar into plugins/
::loadplugin example-plugin
::example                    -> "Example v1!"
```

It also doubles as a reload test out of the box: bump the `REVISION` constant, rebuild, copy the
jar over the old one in `plugins/`, and run `::pluginreload example-plugin` while the server keeps
running — `::example` should now reply "Example v2!", confirming the old command handler was
replaced rather than duplicated.

---

## Overview

Normally, gameplay ships as `PluginScript`/`PluginModule` classes compiled directly into this
repo's `content`/`api` modules (see `AGENTS.md`). External plugins are the same two class types,
but discovered from a `plugins/` directory at the repo root instead of the server's own build
output — nothing here changes how you *write* a plugin, only how it's packaged and loaded.

A plugin source under `plugins/` is either:

- a `.jar` file, or
- a plain directory of compiled `.class` files (e.g. point your IDE/build tool's output directory
  straight at a folder under `plugins/` — no packaging step needed while iterating).

Each top-level file or directory directly under `plugins/` is one plugin source, discovered by
`ExternalPluginLoader` (`engine/plugin/src/main/kotlin/org/rsmod/plugin/loader/ExternalPluginLoader.kt`).

---

## The manifest

Every plugin source must have a `plugin.properties` at its root (top level of the jar, or top
level of the directory) with all four of these fields set:

```properties
name=My Plugin
description=Does something cool
revision=1
author=SomeDev
```

A source with no `plugin.properties`, or with any of the four fields blank, is refused everywhere
— it won't load at boot, and `::loadplugin`/`::pluginenable`/`::pluginreload`/the `::plugins` menu
will all refuse it too. It still shows up in `::plugins` (as `<name> (no manifest)`) so you can see
it's there and fix it, rather than it silently vanishing.

---

## What gets picked up

`ExternalPluginLoader` scans each source with its own isolated classloader (so an external
plugin's own dependencies don't leak onto the server's classpath, and vice versa) and looks for:

- **`PluginModule`** subclasses — Guice bindings, same as a built-in module.
- **`PluginScript`** subclasses — gameplay logic, same as a built-in script.

Sources present in `plugins/` at boot participate exactly like built-in plugins: their modules are
merged into the same `Guice.createInjector(...)` call, and their scripts are constructed and
started right alongside the built-in ones.

Right after boot fully finishes (once you see `Server ready in ...`), the server automatically
closes every currently-loaded plugin's classloader — including ones loaded at boot, not just
hot-loaded ones. This releases the jar file on disk (on Windows, an open `URLClassLoader` locks its
jar against being overwritten) so you can rebuild and replace *any* plugin's jar at any time
without first having to `::plugindisable` it. The plugin keeps running unaffected: its classes are
already loaded and don't need the file handle anymore. The one edge case this doesn't cover is a
plugin that lazily reaches a helper class it hadn't touched yet *after* this point — that load
would fail, since a closed classloader can't read new classes from its jar. In practice this is
rare (a Kotlin lambda's class loads when the lambda literal is evaluated, not when its body later
runs, so almost everything a script does in `startup()` is already resident by boot's end).

---

## Loading, reloading, and unloading while the server is running

A source added to `plugins/` after boot doesn't require a restart — load it with:

```
::loadplugin name
```

`name` is the source's **file or directory name** — not the `name` field inside its
`plugin.properties` (that one's just a display label). This also works for a source that's on disk
but wasn't enabled at boot (see below).

This matters for reload: identity is the filename, so to reload a plugin you rebuild, the new jar
has to land at the **same path** as the one already loaded (e.g. always `plugins/my-plugin.jar`,
not `plugins/my-plugin-1.2.0.jar` one time and `plugins/my-plugin-1.3.0.jar` the next) — otherwise
the loader sees it as a brand new, never-loaded source instead of a reload of the existing one. If
you build with Gradle's default `jar` task, the output filename includes the project version by
default (`my-plugin-0.0.1-SNAPSHOT.jar`), which changes on every version bump — `example-plugin`
pins `tasks.jar { archiveFileName.set("example-plugin.jar") }` in its `build.gradle.kts`
specifically so the filename stays fixed across rebuilds; copy that pattern in your own plugin's
build.

Because the main Guice injector already exists by the time you do this, any `PluginModule`s the
newly-loaded source declares only take effect in a **child injector** scoped to that source's own
scripts — they can't introduce bindings visible anywhere else in the running server. A hot-loaded
`PluginScript` that only depends on services the server already provides (the common case) behaves
exactly like a built-in one.

Calling `::loadplugin`/`::pluginreload` on a source that's **already loaded** reloads it:
[`PluginScript.shutdown`](#cleaning-up-before-a-reload) runs on its scripts, then every `EventBus`
subscriber, every `CheatCommandMap` command, and every `EngineQueueCache` binding whose backing
code (or, for the queue cache, classloader recorded at registration) came from that source's
classloader is unregistered, and the old classloader is closed. The source is then re-scanned from
disk with a fresh classloader and its scripts' `startup()` runs again. This is how you pick up a
code change to a plugin without restarting the server — rebuild the source (or just rebuild your
IDE's output directory, for a directory source) and run `::loadplugin name`/`::pluginreload name`
again.

The old classloader is explicitly closed as part of unload specifically so this works on
**Windows**: a `URLClassLoader` opened over a jar keeps that file open (and thus locked against
being overwritten) until it's closed — without this, rebuilding a jar and copying it over an
already-loaded plugin would fail with `FileSystemException: ... used by another process`, since
just dropping the reference and waiting on GC doesn't release the OS file handle in any bounded
time.

`::plugindisable name` (and the Disable option in the `::plugins` menu) unloads an already-running
source immediately, the same way, in addition to marking it disabled so it won't load again at the
next restart.

---

## Enabling and disabling

Every plugin source has a persisted enabled/disabled flag, stored in
`plugins/plugins-state.properties` (created automatically — don't hand-edit it while the server is
running). A source with no entry in that file defaults to enabled.

Commands:

| Command | Effect |
|---|---|
| `::plugins` | Lists every plugin source with its enabled/loaded state, and opens a menu to enable/disable/load/reload one |
| `::pluginenable name` | Marks a source enabled; loads it immediately if it isn't already running |
| `::plugindisable name` | Unloads a source if it's running, and marks it disabled so it won't load again at the next restart or via `::loadplugin`/`::pluginenable` |
| `::pluginreload name` | Loads a source, or reloads it (unload + fresh load) if it's already running |
| `::loadplugin name` | Same as `::pluginreload` |

---

## Cleaning up before a reload

Reloading automatically unregisters everything registered through `EventBus` (`onOpNpc1`,
`onEvent`, ...), `CheatCommandMap` (`onCommand`), and `EngineQueueCache` (`onArea`, `onZone`,
`onAdvanceStat`, and friends) — you don't need to do anything for these.

What it can't automatically undo is a side effect your script caused *outside* registering a
handler — an entity it spawned, shared state it mutated, a background coroutine it started. The
engine has no general way to know about or reverse that, so `PluginScript` has an opt-in hook for
it:

```kotlin
class MyPlugin @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        // ... register handlers, spawn something, etc.
    }

    override fun ScriptContext.shutdown() {
        // Called right before this script is unregistered during a reload/unload.
        // Clean up whatever startup() did that isn't just handler registration.
    }
}
```

`shutdown()` defaults to doing nothing. If your plugin only registers handlers (the common case),
you don't need to override it — reload already handles that part. If it does more, and you want
`::pluginreload` to leave things clean instead of accumulating stale state across reloads,
override it.

Either way, reload only changes what runs for *future* dispatch. It can't retroactively fix state
the old code already left behind before you added (or if you never add) a `shutdown()` override —
a full server restart is still the reliable way back to a clean slate.
