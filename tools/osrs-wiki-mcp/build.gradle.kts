plugins {
    id("org.jetbrains.kotlin.jvm")
    id("application")
}


dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.12.0")
    implementation("io.ktor:ktor-client-cio:3.3.3")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation(rootProject.libs.or2.all.cache)

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:3.3.3")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.rsmod.tools.mcp.wiki.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
