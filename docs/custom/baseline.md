# Accepted runtime baseline

The user explicitly accepted the current work on 2026-10-03 and required exact
preservation during organization. This overrides suggestions to adopt old fixes.

| Part | Exact reference |
|---|---|
| Server runtime | `444ead71bec435e3eba0c378aa6718f7a7ac57d4` |
| Nero Studio | `0efcb039c539a469a1dab2c4c654c61ecf41aa51` |
| Revision | 240 |
| Server JAR SHA-256 | `41cb04e279533408aea110291d7ff10157f01be9714f283edd7a2cf2a5039c82` |
| Cache/mapping fingerprint | `61657fde9e8f2372b610772b4f04e6c936923dd0cc6cb7747b04e3b6276b8a5d` |
| Local package | `outputs/guides-cape-update-20261003` in the Codex workspace |
| Rollback checkpoint | `outputs/checkpoint-20261003-voor-guides-cape-update` |

The existing installer manifest contains all target/payload hashes. Package payloads
were rechecked during organization. No installer was executed and no live process,
account database, cache or launcher was replaced.

Validation of this runtime: 121 selected server tests, 86 Nero tests, cache compile,
plugin artifact verification, isolated database/server boot, installer/rollback
fixtures and live hash preflight. GitHub server CI, formatting and gameval checks
on `444ead71b` all completed successfully. This does not certify every OSRS mechanic;
feature-specific limitations stay in CUSTOM_PROGRESS.md.

Tag `server-r240-accepted-20261003` identifies this exact source, not later docs commits.
The paired Studio tag uses the same name in its own repository.

During audit, the legacy lifecycle change was briefly committed as `d7d5a5e5b`,
then fully reverted by `2bd931b55` on the user's instruction. Neither was installed.
The runtime diff back to `444ead71b` was empty. Retain that transparent history.

## Recover historical branch work

`archive/20261003/<old-branch-name>` points at each retired branch tip. Create a
short-lived review branch from the tag only when reviewing that implementation.
Do not replace accepted code by checking out an archive into the installed server.
A verified full Git bundle and original ref manifest also exist at
`outputs/repository-cleanup-20261003` in the Codex workspace.
