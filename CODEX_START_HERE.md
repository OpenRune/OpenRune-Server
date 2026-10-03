# CODEX START HERE

This repository is a long-term custom fork of:

- Upstream: `OpenRune/OpenRune-Server`
- Fork: `EvolvedMind/OpenRune-Server`

Before doing anything else, read:

```text
OpenRune_Fork_Development_Workflow.md
```

Treat that file as the permanent operating manual for this repository.

Then execute the following workflow immediately.

---

# PRIMARY GOALS

1. Keep OpenRune upstream easy to follow and integrate.
2. Keep all custom work clearly separated, traceable and documented.
3. Keep `main` stable and understandable.
4. Keep branches temporary and clean.
5. Make debugging and regression hunting easy.
6. Maintain a clear overview of:
   - what exists;
   - what is custom;
   - what comes from upstream;
   - what is hybrid;
   - what is being worked on;
   - what is planned;
   - what requires review.

---

# STARTUP PROCEDURE

At the beginning of every substantial session:

1. Inspect the current repository state.
2. Check the current branch.
3. Check whether the working tree is clean.
4. Fetch `origin`.
5. Fetch `upstream` if configured.
6. Read:
   - `OpenRune_Fork_Development_Workflow.md`
   - `CUSTOM_CONTENT.md` if it exists
   - `CUSTOM_PROGRESS.md` if it exists
   - relevant files under `docs/custom/`
7. Inspect recent commits relevant to the current task.
8. Determine whether existing branches need cleanup before creating a new one.

Do not ask for permission for normal safe inspection and analysis.

Proceed autonomously through all logically dependent steps.

Only stop for user input when an action is genuinely destructive, irreversible, ambiguous in a way that cannot be resolved from the repo, or requires a product decision.

---

# FIRST-TIME CLEANUP TASK

If the repository still contains many old branches, perform the branch audit before creating more branches.

For every non-`main` branch:

1. Compare it with `main`.
2. Inspect unique commits.
3. Inspect meaningful code differences.
4. Classify it as:

```text
SAFE TO DELETE
REVIEW FIRST
KEEP TEMPORARILY
```

Never classify based only on the branch name.

## SAFE TO DELETE

Use only when it is verified that:

- the work is already represented in `main`; or
- it was an obsolete experiment/probe; or
- it has been superseded; or
- it contains no unique work worth preserving.

After verification, delete the branch locally/remotely where appropriate.

## REVIEW FIRST

Use when the branch contains unique or uncertain work.

For each valuable change:

- preserve it;
- cherry-pick it if the commit is clean and focused; or
- re-implement the useful part cleanly on the current codebase;
- create a clear modern commit;
- update documentation where needed.

Only after the valuable work is safely preserved may the old branch be deleted.

## KEEP TEMPORARILY

Use only for active work that still has a clear purpose.

Examples:

```text
feature/doom
fix/pet-relog
```

Branches are temporary. Once merged and verified, remove them.

---

# DO NOT DO THESE THINGS

Do not:

- hard-reset `main` to `upstream/main`;
- force-push `main` during normal maintenance;
- blindly overwrite custom code with upstream code;
- delete branches without checking unique commits/code;
- create unnecessary new branches;
- keep stale Codex/test/experiment branches forever;
- make giant commits containing unrelated work;
- rewrite old shared history just to make it look cleaner;
- assume a clean Git merge means there is no functional conflict.

---

# UPSTREAM SYNC RULE

When upstream has new commits:

1. Inspect the incoming commits.
2. Inspect changed files/modules.
3. Check for overlap with custom content.
4. Merge upstream in a controlled way.
5. Resolve technical conflicts consciously.
6. Check for semantic/functional conflicts.
7. Retest affected custom systems.
8. Update custom documentation/progress.

Use recognizable merge history, for example:

```text
merge(upstream): sync OpenRune revision 241
```

---

# CRITICAL: OVERLAPPING FEATURES

If upstream adds a feature that already exists custom in this fork, do **not** automatically adopt upstream and do **not** automatically keep custom.

Run a:

```text
FEATURE EQUIVALENCE REVIEW
```

Examples:

- custom Zulrah already exists and upstream adds Zulrah;
- custom Doom already exists and upstream adds Doom;
- custom pet behavior overlaps with a new upstream pet implementation;
- custom commands overlap with upstream commands;
- custom systems depend on an upstream API that changed.

Compare at minimum:

- functionality;
- mechanics;
- correctness;
- OpenRune-native integration;
- architecture;
- core modifications;
- test coverage;
- edge cases;
- performance;
- maintainability;
- debugging/tooling;
- custom-only value.

Possible outcomes:

```text
CUSTOM WINS
UPSTREAM WINS
HYBRID
DEFERRED REVIEW
PARALLEL EXPERIMENT (temporary only)
```

Prefer the technically strongest final implementation, not the one that is simply "official".

A Git merge without conflicts can still create two competing implementations. Detect that.

---

# CUSTOM REPOSITORY DOCUMENTATION

Ensure these files exist and remain accurate:

```text
CUSTOM_CONTENT.md
CUSTOM_PROGRESS.md

docs/custom/
├── README.md
├── architecture.md
├── core-modifications.md
├── zulrah.md
├── pets.md
├── commands.md
└── other relevant custom-system docs
```

Do not create empty placeholder files unless they are useful.

---

# CUSTOM_CONTENT.md

Use this as the inventory of custom work.

It should clearly answer:

- What did this fork add?
- Where is it implemented?
- Why does it exist?
- What upstream systems does it depend on?
- Where is its detailed documentation?
- Is it custom, upstream, hybrid, or upstream + custom extensions?

---

# CUSTOM_PROGRESS.md

Use this as the live development dashboard and roadmap.

Supported states:

```text
⚪ NOT STARTED
🟣 PLANNED
🔵 IN PROGRESS
🟡 IMPLEMENTED / NEEDS TESTING
🟢 VERIFIED
🟠 NEEDS REVIEW
🔴 BLOCKED
🔁 UPSTREAM REVIEW
♻ HYBRID / MIGRATING
```

Supported origins:

```text
UPSTREAM
CUSTOM
HYBRID
UPSTREAM + CUSTOM EXTENSIONS
```

Track at minimum:

- active development;
- bosses;
- systems;
- skills where relevant;
- raids;
- minigames;
- roadmap;
- upstream review queue.

Use roadmap sections:

```text
NOW
NEXT
LATER
BACKLOG
```

When work changes state, update `CUSTOM_PROGRESS.md`.

---

# CORE MODIFICATIONS

Whenever custom work changes generic OpenRune/core code, record it in:

```text
docs/custom/core-modifications.md
```

For each core modification document:

- file/path;
- reason;
- related custom system;
- behavior change;
- introducing commit;
- upstream conflict risk;
- possible future removal path.

Prefer native OpenRune extension points over core changes whenever practical.

---

# BRANCH NAMING

Use:

```text
feature/<name>
fix/<name>
experiment/<name>
review/<name>
```

Examples:

```text
feature/doom
fix/zulrah-transition
review/zulrah-upstream
```

Do not create permanent `codex/*`, `fresh/*`, or random test branches as an archive mechanism.

If Codex creates a temporary branch, Codex is responsible for cleaning it up once it is no longer needed.

---

# COMMIT RULES

Use small, logical commits.

Preferred examples:

```text
feat(zulrah): add encounter skeleton
feat(zulrah): add four rotations
feat(pets): add follower teleport handling
fix(pets): prevent follower duplication
fix(zulrah): preserve phase state
docs(custom): document Zulrah dependencies
refactor(zulrah): adopt upstream instance lifecycle
merge(upstream): sync OpenRune revision 241
```

One logical purpose per commit.

Before committing:

1. inspect the diff;
2. make sure unrelated changes are excluded;
3. run relevant tests;
4. update docs if behavior/architecture changed.

---

# STABLE MAIN

`main` should represent a known stable server state.

Before merging feature/fix work into `main`:

- build/tests should pass where available;
- relevant manual checks should be documented;
- progress status should be updated;
- custom docs should be updated;
- no known accidental duplicate implementation should remain.

When a stable milestone is confirmed, create a meaningful tag when useful.

Examples:

```text
server-v0.3-pets
server-v0.4-zulrah
server-v0.5-doom
```

---

# DEBUGGING RULE

Use Git history as a debugging tool.

Keep commits granular enough that these commands are useful:

```bash
git log --oneline
git show <sha>
git diff <sha>^ <sha>
git bisect
```

When a regression is found, identify the smallest responsible commit or upstream merge where practical.

Document important regression fixes clearly.

---

# CONTINUOUS EXECUTION

After the repository is cleaned and structured:

1. identify current `NOW` work in `CUSTOM_PROGRESS.md`;
2. continue the highest-priority active task unless the user asked for a different task;
3. work in a suitable short-lived branch;
4. implement in logical slices;
5. test;
6. review;
7. commit;
8. update docs/progress;
9. merge only when stable;
10. clean the branch.

Do not repeatedly stop to ask "continue?" when the next step is obvious and safe.

---

# END-OF-SESSION REPORT

At the end of substantial repository work, report clearly:

- branches deleted;
- branches retained;
- unique code/commits preserved;
- commits created;
- documentation added/updated;
- progress/roadmap changes;
- upstream changes integrated;
- overlapping features reviewed;
- tests performed;
- remaining risks;
- next recommended development step.

Keep the report concise but specific.

---

# DEFINITION OF SUCCESS

This repository is healthy when someone can quickly answer:

```text
What comes from OpenRune?
What did we build ourselves?
What is hybrid?
What is currently stable?
What is being worked on?
What is planned next?
Which branch is active?
Which commit introduced a feature or regression?
Which custom systems are at risk from an upstream change?
Why was core code modified?
What is the last known-good server state?
```

If those questions are easy to answer, continue using this workflow.

If they are not, fix the repository organization before allowing more complexity to accumulate.
