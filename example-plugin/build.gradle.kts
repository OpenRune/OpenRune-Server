plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
}

// Fixed name (no version suffix) so ExternalPluginLoader always sees this as source
// "example-plugin", matching plugin.properties regardless of build/rebuild - copy over
// plugins/example-plugin.jar each time instead of having to rename it.
tasks.jar {
    archiveFileName.set("example-plugin.jar")
}
