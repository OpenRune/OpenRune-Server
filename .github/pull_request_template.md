## Summary

<!-- What does this change and why? Link issues with `Fixes #123`. -->

## Type of change

- [ ] Bug fix
- [ ] New content (skill, quest, boss, area, interface)
- [ ] Engine / API change
- [ ] Cache / gamevals / pack data
- [ ] Docs / tooling

## How it was tested

<!-- Live client steps, unit tests, or logs/screenshots. Content should be verified in-game end-to-end without cheats where possible. -->

## Checklist

- [ ] Builds and the server boots
- [ ] Tests only cover complex logic and anything that moves items (no dupes)
- [ ] Gamevals symbols instead of raw ids; game data in the cache, not Kotlin maps
- [ ] State in varbits/varps, not attrs
- [ ] Matches the OSRS wiki (deviations noted in `docs/quirks.md`)
- [ ] New content checked against rsprox dumps where possible (anims, projectiles, sounds, timings)
- [ ] No unrelated or hand-edited generated changes

## Screenshots / notes for reviewers

<!-- Optional: interface screenshots, before/after, follow-ups, known gaps. -->
