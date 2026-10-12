---
name: persistence-test
description: How to write a Persistence test in OpenRune-Server — a JUnit 5 test in a module's
  `src/persistence` that saves and loads player state through the real
  `CharacterAccountRepository` and pipelines on an embedded Postgres, using
  `PersistenceTestExtension`. Use when changing what a character saves or loads (pipelines,
  appliers, repository SQL, schema or world-type isolation) or how a save commits or rolls back.
---

# Persistence tests

Proves **player state survives save → load on real Postgres**: zonky PG 14, the real Flyway
schema, the real repository and pipelines — no `GameServer`, no `GameTestState`. Which layer to
pick is decided by the "Tests" table in `AGENTS.md`; this skill is only *how*.
Not this layer if: the logic can take its inputs as parameters (→ `unit-test`), or the question
is how the game behaves while the player is online (→ `game-integration-test`).

## Where it goes

- `src/persistence/kotlin/<same package as the code under test>/<Subject>Test.kt`
  (e.g. `CharacterInventoryPipeline` → `CharacterInventoryPipelineTest`).
- The module's `build.gradle.kts` must apply the suite (it also adds `:api:testing-persistence`):
  ```kotlin
  plugins {
      id("base-conventions")
      id("persistence-test-suite")
  }
  ```
  Extra deps go in `persistenceImplementation(...)`. Only `api/account` applies it today.
- Run: `./gradlew :api:account:persistence` (CI runs `./gradlew persistence`).

## Template

```kotlin
@ExtendWith(PersistenceTestExtension::class)
class CharacterAccountRepositoryTest {
    @Test
    fun `a saved position loads back unchanged`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, NAME)
        val player = Player().apply { coords = SAVED_COORDS }

        // when
        save(db, repository, pipelines, character, player)
        val loaded = load(db, repository, pipelines, NAME)

        // then
        assertEquals(SAVED_COORDS, loaded.coords)
    }

    private companion object {
        const val NAME = "persist_test"
        val SAVED_COORDS = CoordGrid(0, 50, 50, 21, 11)
    }
}
```

`createCharacter`, `save` and `load` are the helpers under "Fixture and builders".

## Harness cheatsheet

| Need | Use |
|---|---|
| A database | `@ExtendWith(PersistenceTestExtension::class)`; a fresh DB cloned from a migrated template for every test method |
| The seam | Test parameters: `GameDatabase`, `CharacterAccountRepository`, `Set<CharacterDataStage.Pipeline>` |
| A transaction | `db.withTransactionBlocking(schema) { connection -> … }` (`schema` = `WorldType.key`) |
| Save | `repository.save(connection, player, accountId, characterId, gameWorldId)` then each `pipeline.save(connection, player, characterId)` — one transaction, as `AccountSavingService` does |
| Load | `repository.selectAndCreateMetadataList(connection, name)`, each `pipeline.append(connection, metadata)`, then `metadata.forEach { it.apply(player) }` onto a fresh `Player()` |
| Rows the code can't produce / can't read back | Raw SQL on the `connection` (e.g. stale rows after an inventory shrinks) |
| Cache / RSCM symbols | Already loaded by the extension (it reuses `api/testing`'s cache bootstrap) |

## Given / when / then

- Bare lowercase markers on their own line: `// given`, `// when`, `// then`. Nothing after them.
  These are the **only** comments a test body may contain.
- `// given` is the first line of the body; one blank line before each later marker; no other
  blank lines inside a block.
- Omit `// given` only when there is no setup. Use one `// when / then` only when the act *is*
  the assertion (`assertThrows`, `assertDoesNotThrow`). Otherwise `// when` and `// then` are
  always present.
- One when/then per test — split multi-phase tests; prior steps a later step needs go into a
  helper or `// given`. Repeated `// when` / `// then` pairs only when the sequence itself is
  under test (the assertion is about ordering); `// given` never appears after the first
  `// when`.
- Inside `runGameTest { }`: markers go inside the lambda; the `runGameTest(Script::class)` call
  is harness, not given. World setup (placing locs, spawning, teleporting, filling inventories)
  is given. The op under test plus the `advance` ticks it needs to resolve is when. Then holds
  only assertions and never advances ticks.
- Parameterized tests (`@TestWithArgs`, `@ParameterizedTest`): same rules. Turning args into
  state is given; args used as-is means no setup, so `// given` is omitted. Provider classes
  and their DSL get no markers.
- Every new test method uses the markers, even when added to an existing unmarked file; leave
  its neighbours as they are.
- Test names: backtick sentences stating the scenario and outcome.

In this layer the round trip is the act: building the character and its state is given, the
save plus the load is when, and then only inspects the loaded `Player` (or table rows).

## Fixture and builders

- `PersistenceTestExtension` (module `api/testing-persistence`) starts one Postgres per test
  JVM (`fsync=off`), migrates a seed DB once through `GameDatabase.connect()`, clones it with
  `CREATE DATABASE … TEMPLATE` before each test method and drops it after. Tests never share
  rows, so they need no cleanup and no ordering.
- Each test builds its own small state in `// given` and saves it through the real repository
  and pipelines. There is no shared golden fixture.
- Until the extension ships shared save/load helpers, keep them as private helpers in the test
  class, composed only from the calls in the cheatsheet, so the test exercises exactly what
  `AccountSavingService` and `AccountLoaderService` do.
- Raw SQL only for rows the code can't produce, or to assert table contents the load path
  doesn't expose.

## Do / don't

- **Do** assert Temp varps/invs are *absent* after load when testing a full round trip; they
  are deliberately not saved.
- **Do** save each world type through its own schema (`WorldType.key`) when testing isolation.
- **Don't** start `GameTestState` or `GameServer`, or bind `GameDatabaseModule` /
  `AccountSavingModule` — the extension hands you the seam.
- **Don't** use the process-wide `EmbeddedSameInstancePostgres`, or run saves concurrently.
- **Don't** test the loader/saver services' retry and callback threading here.
- **Don't** print or log in tests; fix the assertion.
- Found a real bug? Land the test `@Disabled("<bug>")` and record the bug — don't fix it in the
  test PR.
