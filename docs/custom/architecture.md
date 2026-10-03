# Architecture and integration

The server uses Kotlin content plugins, native APIs, cache pack modules and symbolic
gamevals. Native client interfaces are packed into the server cache; a changed
interface requires the matching cache and JAR, not a source-only copy.

Nero Studio is a separate repository with a launcher, client plugin, shared protocol
and server plugin. The authenticated bridge supplies object catalogue/editor data
and boss respawn deadlines. Game-thread snapshots cross into HTTP handlers. Do not
iterate mutable game entities directly on HTTP threads.

The accepted pair is pinned in [baseline.md](baseline.md). Keep the real native
117 HD plugin; the former duplicate integration must not return. Loot aggregation
and colours belong to native Ground Items; no GE-unavailable formatter is reintroduced.

For future changes: inspect both sides of a contract, build the server/cache first,
verify both plugin JAR contracts and tests against that exact JAR, boot in isolation,
and publish one paired, hash-checked package with rollback. Never run fresh-cache or
installation tasks against a live player session.

Use native extension points before changing generic APIs. Record every unavoidable
core change in [core-modifications.md](core-modifications.md).
