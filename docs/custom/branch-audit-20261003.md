# Branch audit — 2026-10-03

Compared all **17 origin branches** (main plus 16 development branches) against
`origin/main` at `8dbb20cda` and accepted gameplay `444ead71b`. The pasted audit was
treated as a lead, not proof. Inspected unique commits, introduced-file blobs,
targeted semantic diffs, ancestry and current production successors.

The user then explicitly required preserving the current runtime. Therefore **no old
implementation is adopted**. REVIEW FIRST code is preserved as verified remote tags,
with unresolved product/compatibility decisions remaining in the review queue.
Deleting its branch reference does not delete its commits or tagged source.

All retired tips are preserved at `archive/20261003/<exact-old-branch-name>`.
Each remote annotated tag was dereferenced and checked against the original branch SHA.
A verified full Git bundle and JSON comparison evidence are also in the local
`outputs/repository-cleanup-20261003` directory.

| Branch | Classification | Exact tip | Finding |
|---|---|---|---|
| `chore/openrune-upstream-20260929` | REVIEW FIRST | `6a3590c24` | Old combined pet/Zulrah line; persistent death recovery, three-tile reach, lifecycle tests and Doom research differ from accepted implementation. Archived intact; do not adopt automatically. |
| `codex/commands-menu-native` | SAFE TO DELETE | `9530bf8bb` | Earlier stone-frame UI superseded by accepted steel-frame renderer, state-preserving paging and regression tests. Old argument/search implementation remains recoverable. |
| `codex/openrune-sync-zulrah-petfix-20260930` | SAFE TO DELETE | `462c3deb0` | All 21 branch-introduced paths are byte-identical to main; active line only adds subsequent fixes. |
| `codex/pet-relog-zulrah-reach-20261001` | KEEP TEMPORARILY | `444ead71b` | Accepted gameplay source and current organization work; retain until safely integrated in main. |
| `codex/preserve-data-plugin-lifecycle-20261001` | REVIEW FIRST | `6fcd8545b` | Unique non-destructive Central startup and boot-plugin shutdown fix. User required exact current runtime; not included. Original tests and code preserved by tag. |
| `feat/pet-followers` | SAFE TO DELETE | `624b3e3ac` | Every commit is an ancestor of the archived combined legacy line; old pet implementations/tests remain reachable there and at this exact tag. |
| `feat/zulrah-encounter` | SAFE TO DELETE | `2bd30bed6` | Every commit is an ancestor of the archived combined legacy line. Alternate encounter/recovery code preserved, not confused with current controller. |
| `feat/zulrah-official-contribution` | SAFE TO DELETE | `0b16a53e5` | Movement/recovery source is represented in main; two test differences are terminal newlines only. Commit also exists upstream. |
| `fix/commands-interface-contract` | SAFE TO DELETE | `8185d39a1` | Six of seven changed paths match main exactly. Remaining CI difference is superseded boot/runtime checking; active menu styling intentionally differs. |
| `fix/embedded-central-offline-badwords` | REVIEW FIRST | `cc5453fb8` | Unique opt-in NERO_LOCAL_DEV_OFFLINE_BADWORDS behaviour and two tests; archived rather than changing current login behaviour. |
| `fix/install-configuration-cache` | REVIEW FIRST | `a9a887180` | Unique JavaExec/Copy installer split plus configuration-cache dry-run check. Preserved; not imported or executed against current install. |
| `fix/keep-external-plugin-loaders-open` | SAFE TO DELETE | `fce43c976` | Loader and docs match main. Main adds deferred-class boot probe; unrelated invalid-NPC handling difference is retained only in archive. |
| `fresh/zulrah-pets-20260930` | REVIEW FIRST | `701b81172` | Contains PersonalCommands pet helpers not all present now. Current pet gallery/AdminCommands remain authoritative; exact helper source and fresh-check preserved. |
| `test/deferred-external-plugin-load` | SAFE TO DELETE | `7a2c3630f` | All four introduced paths match main byte for byte, including post-ready helper resolution proof. |
| `test/nero-launcher-contract-probe` | SAFE TO DELETE | `0658cc77b` | Isolated C# probe, not server gameplay. Production launcher has health/failure handling; original finite probe is archived as research, not claimed equivalent in every detail. |
| `test/nero-runtime-packaging-probe` | SAFE TO DELETE | `e9e820844` | Old standalone fat-JAR reproduction. Both actual Nero plugin builds now depend on runtimeClasspath and verify required classes; full reproduction retained in archive. |

`main` is retained. The active gameplay branch is retained until its verified merge.
Stale branch deletion is conditional on both the archive-tag hash and unchanged remote
head. No force push of main and no reset/rebase of shared history are used.

## Unresolved alternatives

- Legacy Zulrah recovery / rotations / lifecycle tests: compare separately before reuse.
- Central startup/plugin shutdown, offline login and installer fixes: preserved, not active.
- Personal pet helper commands: decide usefulness/rights/naming before any reimplementation.
- Doom research: retained as research, not marked as a working encounter.

## Organization boundary

This pass may change Markdown, the documentation inventory generator and its workflow,
and Git refs. Runtime Kotlin/Java/CS2, cache configuration, Gradle/runtime dependencies
and the installed server/launcher are preserved at the accepted baseline.

## Completed cleanup

All 15 retired branches were deleted atomically after remote archive tags were
verified against their original tips. Exact-tip leases protected against concurrent
updates. Remote heads now contain only main and the active integration branch.
The local retired lifecycle branch was removed after archive verification.
The accepted runtime and installed package were not changed.
