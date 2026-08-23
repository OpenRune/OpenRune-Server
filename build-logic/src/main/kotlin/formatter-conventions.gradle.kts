plugins {
    id("com.diffplug.spotless")
}

spotless {
    // Only enforce formatting on files changed relative to main. The codebase
    // predates enforcement and has drifted (632 files as of revision 240), so a
    // repo-wide check would fail every build; ratcheting stops new drift
    // without a disruptive one-time reformat.
    ratchetFrom("origin/main")

    kotlin {
        // Rule selection lives in .editorconfig: the standard ruleset is off,
        // with only hygiene rules switched back on.
        ktlint("1.5.0")
    }
}
