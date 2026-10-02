# Files

- [Server boot sequence](boot-sequence.md) - How GameServer starts OpenRune — install checks, plugin module discovery, Guice injector, config/cache/map loading, parallel plugin script startup, the boot gate, and the ServiceManager lifecycle including shutdown.
- [Events, coroutines and ProtectedAccess](events-and-coroutines.md) - How OpenRune dispatches gameplay events (unbound, keyed and suspend event maps on the EventBus), how tick-driven GameCoroutines suspend and resume, and what ProtectedAccess guarantees to content handlers.
- [Game cycle and tick processing](game-cycle.md) - The 600 ms game loop driven by GameService, the fixed ordering of world, NPC, controller and player processes inside GameCycle.tick, lifecycle events, error isolation, and the shutdown fast-forward.
- [Architecture overview](overview.md) - The module layering of OpenRune-Server — engine, api, content, server, or-cache and tools — how Gradle auto-includes nested subprojects, how content reaches the runtime classpath, and which way dependencies point.
