# OpenRune-Server

An OSRS-compatible game server. This glossary fixes the words used when talking about how it
is tested.

## Testing

**Test layer**:
One of the three kinds of automated test — Unit, Persistence, Game integration — chosen by
the cheapest one that can prove the behaviour.
_Avoid_: test type, test level

**Unit**:
A test that proves logic in isolation: plain JUnit 5 in `src/test`, never boots the game or
loads the cache; any cache values the logic reads are passed in as parameters.
_Avoid_: small test

**Persistence**:
A test that proves player state survives save → load on real Postgres: the real repository
and pipelines in `src/persistence`, a fresh database per test, no running game.
_Avoid_: DB test, SQL test, DAO test

**Game integration**:
A test that proves behaviour in the running game: real cache, real game systems, the
scripts under test bound, driven through `runGameTest` in `src/integration`. Its Gradle task
is `integration`.
_Avoid_: integration test (unqualified), end-to-end test

**Dupe/loss test**:
A Game integration test that drives the real op handler and proves an action that moves
items can neither duplicate nor lose them.
_Avoid_: item test

**Worked example**:
A test written to be copied: the model for how tests in its Test layer are shaped.
_Avoid_: sample test, reference test

**Given/when/then markers**:
The bare `// given`, `// when`, `// then` (or `// when / then`) lines that split a test body
into its phases — the only comments a test body may contain.
_Avoid_: AAA, arrange/act/assert
