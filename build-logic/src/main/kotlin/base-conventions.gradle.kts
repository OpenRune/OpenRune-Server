plugins {
    id("formatter-conventions")
    id("kotlin-conventions")
    id("publish-conventions")
    id("test-conventions")
}

if (path != ":or-cache") {
    dependencies {
        implementation(project(":or-cache"))
    }
}
