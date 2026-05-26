plugins {
    id("base-conventions")
}

dependencies {
    implementation(project(":tools:osrs-mcp"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("io.ktor:ktor-client-cio:3.3.3")
}


