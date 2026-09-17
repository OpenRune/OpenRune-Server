import org.gradle.api.tasks.testing.Test

/**
 * Applied by test suites whose tests load the real server cache (e.g. via
 * `ServerCacheManager.init(240)`), so their `test` task's up-to-date check tracks the cache
 * inputs it actually reads instead of each module re-declaring the same two lines.
 */
tasks.withType<Test>().configureEach {
    workingDir(rootProject.projectDir)
    inputs.dir(rootProject.file(".data/cache/SERVER"))
    inputs.file(rootProject.file(".data/gamevals-binary/gamevals.dat"))
}
