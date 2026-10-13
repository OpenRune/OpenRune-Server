---
name: game-integration-test
description: How to write a Game integration test in OpenRune-Server — a JUnit 5 test in a
  module's `src/integration` that boots the game through `GameTestState` /
  `runGameTest(Script::class)`. Use when adding or editing a test for a PluginScript, an op
  handler (opNpc/opLoc/opHeld/ifButton), anything needing cache types or RSCM symbols, or any
  code that moves items (the dupe/loss test).
---

# Game integration tests

Proves behaviour **in the running game**: real cache, real game systems, the scripts under test
bound, real tick loop. Which layer to pick is decided by the "Tests" table in `AGENTS.md`; this
skill is only *how*.
Not this layer if: the logic can take its inputs as parameters (→ `unit-test`), or the question
is "does it survive save → load" (→ `persistence-test`).

## Where it goes

- `src/integration/kotlin/<same package as the code under test>/<Subject>Test.kt`, where
  `<Subject>` is the class under test's own name (`FenceCrossingScript` →
  `FenceCrossingScriptTest`, `Mining` → `MiningTest`). Don't add `Script` to match
  `*ScriptTest` siblings when the class isn't called that.
- The module's `build.gradle.kts` **must** apply the suite, or the test silently never compiles
  or runs:
  ```kotlin
  plugins {
      id("base-conventions")
      id("integration-test-suite")
  }
  ```
  The suite already puts `main`'s classes and the `api/testing` harness on the classpath. Add
  `integrationImplementation(...)` only for what the test itself names that `main` doesn't
  expose: a script class from another module (`projects.api.invPlugin` for `InvOpenScript`),
  or generated `*Row`/`*Enums` (`projects.api.generated`).
- Run: `./gradlew :content:generic:generic-locs:integration` (CI runs `./gradlew integration`).
- Needs `.data/game.key` and `.data/client.key` (gitignored). A fresh worktree or runner
  without them fails at boot: run `./gradlew generateRsa` (CI does) or copy them from the
  main checkout.

## Template

```kotlin
class FenceCrossingScriptTest {
    @Test
    fun GameTestState.`climbing a north-facing fence from the south lands north of it`() =
        runGameTest(FenceCrossingScript::class) {
            // given
            val fence = placeMapLoc(FENCE, BROKEN_FENCE, angle = LocAngle.North)
            player.teleport(FENCE.translateZ(-1))

            // when
            player.opLoc1(fence)
            advance(ticks = 5)

            // then
            assertEquals(FENCE.translateZ(1), player.coords)
        }

    private companion object {
        const val BROKEN_FENCE = "loc.gertrudefence"
        val FENCE = CoordGrid(0, 51, 54, 44, 36)
    }
}
```

## Harness cheatsheet

| Need | Use |
|---|---|
| Run with only your script's handlers bound | `runGameTest(TheScript::class) { … }` — always list the scripts; none listed = no handlers |
| Swap a binding / inject a test-owned dependency | `runInjectedGameTest(Dep::class, childModule, TheScript::class) { dep -> … }` |
| Parameterized | `@TestWithArgs(Provider::class)` on `` fun `name`(arg: T, state: GameTestState) = state.runGameTest { … } ``; provider is a `TestArgsProvider` returning `TestArgs(...)` |
| The player / extra players | `player`, `registerPlayer(...)` |
| World setup | `placeMapLoc(coords, "loc.x")`, `findLocType(content.x) { … }`, `spawnNpc(coords, type)`, `player.teleport`, `player.placeAt`, `allocZoneCollision` |
| Player setup | `player.stats[stats.x] = n`, `setBaseLevel`/`setCurrentLevel`, `setVarp`/`setVarBit`, `fillInv`, `clearInv`, `player.righthand = InvObj("obj.x")` |
| Drive input | `opLoc1..5`, `opNpc1..5`, `ifButton`, `ifButtonD`, `resumeCountDialog`, `moveGameClick` |
| Held (inventory) ops | `player.opHeld1..5(invSlot)` — also bind `HeldOpScript` and `InvOpenScript` in `runGameTest(...)`, or the op reaches no handler |
| Pass time | `advance(ticks = n)`, `advanceUntil({ cond })` |
| Control rolls | `random.next = 0`, `random.nextBoolean = true` (only for code using `GameRandom`) |
| Assert | `assertMessageSent`/`assertMessageNotSent`/`assertMessagesSent`, `assertModalOpen`, `assertContains`/`assertDoesNotContain(inv, obj)`, `player.count(obj)`, `assertExists(loc)`, `player.coords`, `player.assertThrows<T> { … }` |

### Traps

- **Unseedable rolls.** Code on `Random.Default` ignores `random` — drop tables (`api/drop-table`)
  and `skillSuccess()`. Pick a case that skips the roll (e.g. a rock with no gem table), assert
  only what holds for every roll, or test the maths as Unit. Don't fake determinism with retries.
- **Moves into an unallocated zone.** The scope's `teleport`/`telejump` allocate the
  destination's collision; a script's own move into a zone nothing has allocated silently does
  nothing. Call `allocZoneCollision(dest)` in `// given` for every coord the script moves to.
- **Start on the approach tile.** Place the player on the tile the op is used from (the loc's
  interaction side), not on or diagonal to the loc — otherwise the test also exercises
  pathing, takes more ticks and can fail for unrelated reasons.

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

## Dupe/loss tests (required for anything that moves items)

Drive the **real op handler**, not the operation or transaction class. A Unit test of the
transaction maths can add to this but never replaces it. One test per case:

- full inventory mid-transaction → the whole action rolls back, nothing lost;
- stack at `Int.MAX_VALUE` → no overflow, no loss;
- same-tick repeat op → applied once;
- source item gone between check and take (dropped, banked, consumed) → no item created;
- every case: `then` asserts the count of **each** obj before vs after.

Annotate the class `@Execution(ExecutionMode.SAME_THREAD)` — obj transactions share a reusable
index list and aren't thread-safe (see `InvTransactionsTest`).

## Do / don't

- **Do** pick types via `findLocType`/RSCM symbols (`"loc.x"`, `"obj.x"`); never raw ids.
- **Do** use named constants for coords and quantities (`SCREAMING_SNAKE_CASE` in a private
  companion).
- **Do** compute expected values from the setup (`val expected = player.count(obj) - 1`) in
  `// given`, before the op; `// then` only compares.
- **Don't** mock `Player`, `ProtectedAccess` or `ServerCacheManager` (the old
  `*InteractionTest` shape) — use `runGameTest`.
- **Don't** use `runBasicGameTest` / `runAdvancedGameTest` for new tests.
- **Don't** reset hidden state mid-test (`player.actionDelay = -1`) — that's a sign to split.
- **Don't** print or log in tests; fix the assertion.
- Found a real bug? Land the test `@Disabled("<bug>")` and record the bug — don't fix it in the
  test PR.
