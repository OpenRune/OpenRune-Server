# Custom core modifications

## Central startup and boot plugin lifecycle

- Paths: `api/net/src/main/kotlin/org/rsmod/api/net/central/embed/CentralEmbeddedLifecycle.kt`;
  `engine/plugin/src/main/kotlin/org/rsmod/plugin/loader/ExternalPluginLoader.kt`.
- Reason: startup failure must not delete persisted accounts, and boot-loaded plugins
  must receive their shutdown hook when unloaded.
- Behaviour: stop the failed Central instance, retain the original cause and any stop
  failure, and require deliberate database repair. Track boot-loaded script instances.
- Origin: recovered from `6fcd8545b6563ec09904db90461a951a1b9475b9` during the 2026-10-03 audit.
- Tests: `CentralStartupFailureTest`, `ExternalPluginBootLifecycleTest`; API net and
  engine plugin suites passed before committing.
- Upstream risk: Central startup/migration and external plugin lifecycle changes.
- Removal path: adopt equivalent upstream non-destructive startup and shutdown tracking,
  retaining these regression tests. No database migration or live deployment is performed.
