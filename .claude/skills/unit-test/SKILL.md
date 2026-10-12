---
name: unit-test
description: How to write a Unit test in OpenRune-Server — a plain JUnit 5 test in a module's
  `src/test` that never boots the game or loads the cache. Use when adding or editing a test for
  a formula, roll, XP/level curve, probability or drop-rate maths, geometry/pathing, or a
  multi-branch state machine whose inputs can be passed in as parameters.
---

# Unit tests

Proves **logic in isolation**: no `GameTestState`, no Guice injector, no game loop, no cache.
Which layer to pick is decided by the "Tests" table in `AGENTS.md`; this skill is only *how*.
Not this layer if: the code needs cache types, RSCM symbols, scripts, ops or moves items
(→ `game-integration-test`), or the question is "does it survive save → load"
(→ `persistence-test`).

## Where it goes

- `src/test/kotlin/<same package as the code under test>/<Subject>Test.kt`
  (e.g. `PlayerSkillXPTable` → `PlayerSkillXPTableTest`).
- No extra plugin: `id("base-conventions")` already brings JUnit 5 (including
  `@ParameterizedTest`) through `test-conventions`.
- Run: `./gradlew :engine:game:test --tests '*PlayerSkillXPTableTest'` (CI runs
  `./gradlew test`).

## Template

```kotlin
class PlayerSkillXPTableTest {
    @ParameterizedTest(name = "level {0} starts at {1} xp")
    @CsvSource("1, 0", "2, 83", "50, 101333", "99, 13034431")
    fun `each level starts at its wiki xp threshold`(level: Int, xp: Int) {
        // when
        val threshold = PlayerSkillXPTable.getXPFromLevel(level)

        // then
        assertEquals(xp, threshold)
    }

    @Test
    fun `one xp below a threshold is still the previous level`() {
        // given
        val justBelow = PlayerSkillXPTable.getXPFromLevel(MAX_LEVEL) - 1

        // when
        val level = PlayerSkillXPTable.getLevelFromXP(justBelow)

        // then
        assertEquals(MAX_LEVEL - 1, level)
    }

    @Test
    fun `level zero is rejected`() {
        // when / then
        assertThrows<IllegalArgumentException> { PlayerSkillXPTable.getXPFromLevel(0) }
    }

    private companion object {
        const val MAX_LEVEL = 99
    }
}
```

## Harness cheatsheet

| Need | Use |
|---|---|
| One case | `@Test` |
| A table of cases | `@ParameterizedTest(name = …)` + `@CsvSource` (or `@MethodSource` for objects) |
| Expected failure | `assertThrows<T> { … }` (`org.junit.jupiter.api.assertThrows`) |
| Assert | `org.junit.jupiter.api.Assertions.*` (`assertEquals`, `assertTrue`, …) |
| Cache values the logic reads | Pass them in as parameters; if that's impossible, it's Game integration |
| Deterministic rolls | Pass a `FixedRandom` or `SequenceRandom` (`GameRandom`s from `testImplementation(projects.api.testing.testRandom)`) into the code; never `Random.Default` |

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

## Table-driven tests

Prefer one `@ParameterizedTest` over a run of near-identical `@Test`s when only the inputs and
expected value change: boundaries (0, exact threshold, threshold − 1, max), each tier of a rate
table, each branch of a formula. Give it a `name` pattern so failures name the case. Keep
distinct behaviours (a guard, a clamp, an exception) in their own `@Test`.

## Do / don't

- **Do** test the boundaries where rounding or off-by-one hides: 0, 1, exact thresholds, max,
  just past max.
- **Do** use named constants for inputs that mean something (`SCREAMING_SNAKE_CASE` in a private
  companion); literals are fine in a `@CsvSource` table.
- **Don't** boot the game, call `ServerCacheManager` or use RSCM symbols — move the test to
  Game integration instead of mocking them.
- **Don't** mock `Player`, `ProtectedAccess` or `ServerCacheManager` to test script behaviour
  (the old `*InteractionTest` shape) — that's `runGameTest`.
- **Don't** count a Unit test of transaction maths as the dupe/loss proof; that is always a
  Game integration test.
- **Don't** print or log in tests; fix the assertion.
- Found a real bug? Land the test `@Disabled("<bug>")` and record the bug — don't fix it in the
  test PR.
