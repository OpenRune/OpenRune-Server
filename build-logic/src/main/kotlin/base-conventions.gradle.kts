plugins {
    id("formatter-conventions")
    id("kotlin-conventions")
    id("publish-conventions")
    id("test-conventions")
}

if (path != ":or-cache" &&
    path != ":or-cache:pack-api" &&
    path != ":engine:map" &&
    path != ":engine:routefinder" &&
    !path.endsWith(":pack")
) {
    dependencies {
        implementation(project(":or-cache"))
    }
}
