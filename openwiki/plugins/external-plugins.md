---
type: subsystem
title: External and hot-loaded plugins
description: How ExternalPluginLoader discovers PluginModule and PluginScript classes from jars or class directories under plugins/, the plugin.properties manifest, boot-time merging into the main injector, hot load/reload/unload with child injectors and classloader-based handler removal, enable/disable state, and the example-plugin template.
tags: [plugins, hot-reload, classloader, external]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-5730d0f0749931c43a4ee86d
    resource: repo://docs/external-plugins.md
  - id: openwiki-source-27fd41608df35da6dff924c9
    resource: repo://engine/plugin/src/main/kotlin/org/rsmod/plugin/loader/ExternalPluginLoader.kt
  - id: openwiki-source-46b2c11dbb8c6d3bba621dd1
    resource: repo://engine/plugin/src/main/kotlin/org/rsmod/plugin/scripts/PluginScript.kt
  - id: openwiki-source-43732b2b08462cf50ea3e845
    resource: repo://server/app/src/main/kotlin/org/rsmod/server/app/GameServer.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# External and hot-loaded plugins

Built-in gameplay is compiled into `content/` and `api/` (see
[Writing content plugins](plugin-scripts.md)). External plugins are the *same* two class types —
`PluginModule` and `PluginScript` — packaged outside the build and discovered from the
`plugins/` directory at the repo root by `ExternalPluginLoader` (`engine/plugin`). Full user
guide: `docs/external-plugins.md`.

## Plugin sources

Each top-level entry in `plugins/` is one source: a `.jar` or a directory of compiled classes
(handy for pointing an IDE output folder at). The source's **file/directory name** is its
identity for every command.

Every source needs a `plugin.properties` at its root with non-blank `name`, `description`,
`revision` and `author`. Without a valid manifest the source is refused everywhere (it still
appears in `::plugins` as `<name> (no manifest)`).

Enabled/disabled flags persist in `plugins/plugins-state.properties` (auto-created; sources with
no entry default to enabled).

## Boot

At boot `GameServer` merges external plugins into the normal flow (see
[Server boot sequence](../architecture/boot-sequence.md)):

- `ExternalPluginLoader.loadModulesAtBoot()` — modules join the same
  `Guice.createInjector(...)` call as built-in modules;
- `loadScriptsAtBoot(injector)` — scripts are constructed and their `startup()` runs alongside
  built-in scripts.

Each source gets its own `URLClassLoader` (parent = the server's classloader) so plugin
dependencies stay isolated. Once the server is ready, `releaseAllClassLoaders()` closes every
plugin classloader so jars are no longer locked on disk (important on Windows) and can be replaced
without disabling first. Loaded classes keep working; only a class first touched *after* this
point would fail to load.

## Hot load, reload, unload

| Command | Effect |
|---|---|
| `::plugins` | list sources with enabled/loaded state; menu to enable/disable/load/reload |
| `::loadplugin name` / `::pluginreload name` | load, or reload if already loaded |
| `::pluginenable name` | mark enabled and load if not running |
| `::plugindisable name` | unload now and mark disabled for future boots |

`load(name, injector, scriptContext)`:

1. refuse if not found, disabled, or missing a valid manifest;
2. if already loaded → `unload` first;
3. new `URLClassLoader`; scan its modules and scripts. Modules from a hot-loaded source only
   apply to a **child injector** used for that source's scripts — they cannot add bindings
   visible to the rest of the server;
4. run each script's `startup()`.

`unload`:

1. run `PluginScript.shutdown()` on the source's scripts;
2. remove every `EventBus` subscriber, `CheatCommandMap` command and `EngineQueueCache` binding
   whose lambda/registration class came from that classloader (`removeByClassLoader`) — see
   [Events, coroutines and ProtectedAccess](../architecture/events-and-coroutines.md);
3. close the classloader.

Because identity is the file name, a rebuilt jar must land at the same path; the
`example-plugin` build pins `archiveFileName` to `example-plugin.jar` for that reason.

### What reload cannot undo

Handler registrations are removed automatically. Side effects of `startup()` — spawned entities,
mutated shared state, background coroutines — are not; override
`ScriptContext.shutdown()` in the script to clean them up. Built-in scripts never unload, so
their `shutdown()` is never called. A full restart remains the only guaranteed clean slate.

## example-plugin

`example-plugin/` is a buildable template and reload test:

```text
gradlew :example-plugin:jar
copy example-plugin/build/libs/example-plugin.jar → plugins/
::loadplugin example-plugin
::example   → "Example v1!"
```

Bump its `REVISION`, rebuild, overwrite the jar and `::pluginreload example-plugin`; `::example`
then answers "Example v2!", proving the old command was replaced rather than duplicated.
