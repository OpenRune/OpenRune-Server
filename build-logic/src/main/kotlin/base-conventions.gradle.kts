plugins {
    id("formatter-conventions")
    id("kotlin-conventions")
    id("publish-conventions")
    id("test-conventions")
}

if (path != ":or-cache" && path != ":engine:map" && path != ":engine:routefinder") {
    dependencies {
        implementation(project(":or-cache"))
    }
}
