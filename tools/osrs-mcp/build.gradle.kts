import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.tasks.JavaExec

plugins {
    id("base-conventions")
    application
}

private val osrsMcpMainClass = "org.rsmod.tools.mcp.wiki.MainKt"

application {
    mainClass.set(osrsMcpMainClass)
}

tasks.named<JavaExec>("run") {
    group = "application"
    description = "Runs the RS Mod game server (via :server:app:run). For MCP use runMcp / ./gradlew runMcp."
    dependsOn(":server:app:run")
    onlyIf { false }
}

tasks.register<JavaExec>("runMcp") {
    group = "MCP"
    description = "Runs the osrs-mcp stdio MCP server."
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
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

private fun osrsMcpClasspathArg(repoRoot: java.io.File): String =
    repoRoot.resolve("tools/osrs-mcp/build/install/osrs-mcp/lib").absolutePath.replace('\\', '/') + "/*"

@Suppress("UNCHECKED_CAST")
private fun readJsonMap(file: java.io.File): MutableMap<String, Any> {
    if (!file.exists() || file.length() == 0L) {
        return mutableMapOf()
    }
    val parsed = JsonSlurper().parse(file)
    return (parsed as? MutableMap<String, Any>) ?: mutableMapOf()
}

private val knownOsrsMcpClients = setOf("cursor", "vscode", "github", "claude", "intellij", "all")

private val osrsMcpServerName = "osrs-mcp"

/** Avoid half-written JSON if the process or IDE reads the file mid-write (e.g. Cursor restart). */
private fun writeTextAtomically(targetFile: java.io.File, text: String) {
    val parent = targetFile.parentFile ?: error("No parent directory for ${targetFile.absolutePath}")
    parent.mkdirs()
    val targetPath = targetFile.toPath()
    val tempPath = Files.createTempFile(parent.toPath(), ".mcp-write-", ".tmp")
    try {
        Files.writeString(tempPath, text, StandardCharsets.UTF_8)
        try {
            Files.move(
                tempPath,
                targetPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(tempPath)
    }
}

/**
 * Returns [client] property if set; otherwise prompts on the console (numbers 1-5 or names).
 * Fails with a clear message when stdin is not interactive (e.g. CI): use -Pclient=...
 */
private fun promptOsrsMcpClient(project: Project): String {
    val explicit = (project.findProperty("client") as String?)?.trim()?.takeIf { it.isNotEmpty() }
    if (explicit != null) {
        return explicit.lowercase()
    }

    println()
    println("configureOsrsMcp -- where should osrs-mcp MCP config be written?")
    println("  1) Cursor               -> .cursor/mcp.json")
    println("  2) VS Code / Copilot    -> .vscode/mcp.json")
    println("  3) Claude Desktop       -> tools/osrs-mcp/build/mcp-config/*.fragment.json (merge manually)")
    println("  4) IntelliJ / JetBrains -> tools/osrs-mcp/build/mcp-config/intellij-*.md")
    println("  5) All of the above")
    println("  0) Cancel")
    print("Enter 1-5, comma list (e.g. 1,3), or a name (cursor, vscode, ...). Default 1: ")
    System.out.flush()

    val line =
        try {
            System.console()?.readLine() ?: readLine()
        } catch (_: Exception) {
            null
        }?.trim().orEmpty()

    if (line == "0") {
        throw GradleException("configureOsrsMcp cancelled.")
    }
    if (line.isEmpty()) {
        return "cursor"
    }

    val lower = line.lowercase()
    if (lower in knownOsrsMcpClients) {
        return lower
    }

    if (line.any { it.isDigit() }) {
        val parts = line.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val mapped =
            parts.map { part ->
                when (part) {
                    "1" -> "cursor"
                    "2" -> "vscode"
                    "3" -> "claude"
                    "4" -> "intellij"
                    "5" -> "all"
                    else ->
                        throw GradleException(
                            "Invalid menu choice '$part'. Use digits 1-5, comma-separated, or -Pclient=cursor|...",
                        )
                }
            }
        if (mapped.contains("all")) {
            return "all"
        }
        return mapped.distinct().joinToString(",")
    }

    throw GradleException(
        "Unrecognised choice '$line'. Pick 1-5, use comma digits (e.g. 1,3), or pass -Pclient=cursor|vscode|github|claude|intellij|all",
    )
}

/** @return true if the server entry was present and the file would be / was updated */
private fun removeMcpServerEntry(
    targetFile: java.io.File,
    topLevelKey: String,
    serverName: String,
    dryRun: Boolean,
    logPrefix: String,
): Boolean {
    if (!targetFile.exists() || targetFile.length() == 0L) {
        return false
    }
    val root = readJsonMap(targetFile)
    @Suppress("UNCHECKED_CAST")
    val existing = root[topLevelKey] as? Map<String, Any> ?: return false
    if (serverName !in existing) {
        return false
    }
    val servers = existing.toMutableMap()
    servers.remove(serverName)
    root[topLevelKey] = servers
    val text = JsonOutput.prettyPrint(JsonOutput.toJson(root))
    if (dryRun) {
        println("$logPrefix Would remove '$serverName' from ${targetFile.absolutePath}:\n$text")
    } else {
        writeTextAtomically(targetFile, text)
        println("$logPrefix Removed '$serverName' from ${targetFile.absolutePath}")
    }
    return true
}

private fun claudeDesktopConfigCandidates(): List<File> {
    val home = File(System.getProperty("user.home"))
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> {
            val appdata = System.getenv("APPDATA")
            if (appdata != null) {
                listOf(File(appdata, "Claude/claude_desktop_config.json"))
            } else {
                emptyList()
            }
        }
        os.contains("mac") -> {
            listOf(home.resolve("Library/Application Support/Claude/claude_desktop_config.json"))
        }
        else -> {
            listOf(
                home.resolve(".config/Claude/claude_desktop_config.json"),
                home.resolve("Library/Application Support/Claude/claude_desktop_config.json"),
            )
        }
    }
}

/**
 * Removes [osrsMcpServerName] from the first existing Claude Desktop config on this machine.
 * @return true if an entry was present (or would be removed in dryRun).
 */
private fun removeOsrsMcpFromClaudeDesktopGlobal(
    dryRun: Boolean,
    logPrefix: String,
): Boolean {
    val file =
        claudeDesktopConfigCandidates().firstOrNull { it.exists() && it.length() > 0L }
            ?: return false
    val root = readJsonMap(file)
    @Suppress("UNCHECKED_CAST")
    val existing = root["mcpServers"] as? Map<String, Any> ?: return false
    if (osrsMcpServerName !in existing) {
        return false
    }
    val mcpServers = existing.toMutableMap()
    mcpServers.remove(osrsMcpServerName)
    root["mcpServers"] = mcpServers
    val text = JsonOutput.prettyPrint(JsonOutput.toJson(root))
    if (dryRun) {
        println("$logPrefix Would remove osrs-mcp from Claude Desktop ${file.absolutePath}:\n$text")
        return true
    }
    val bak = File(file.parentFile, file.name + ".osrs-mcp.bak")
    file.copyTo(bak, overwrite = true)
    println("$logPrefix Backup written: ${bak.absolutePath}")
    writeTextAtomically(file, text)
    println("$logPrefix Removed osrs-mcp from Claude Desktop: ${file.absolutePath}")
    return true
}

private fun deleteOsrsMcpPath(path: java.io.File, dryRun: Boolean, logPrefix: String): Boolean {
    if (!path.exists()) {
        return false
    }
    if (dryRun) {
        println("$logPrefix Would delete ${path.absolutePath}")
        return true
    }
    val ok =
        if (path.isDirectory) {
            path.deleteRecursively()
        } else {
            path.delete()
        }
    if (ok) {
        println("$logPrefix Deleted ${path.absolutePath}")
    } else {
        println("$logPrefix Failed to delete ${path.absolutePath}")
    }
    return ok
}

private fun mergeServerEntry(
    targetFile: java.io.File,
    topLevelKey: String,
    serverName: String,
    serverEntry: Map<String, Any>,
    dryRun: Boolean,
    logPrefix: String,
) {
    val root = readJsonMap(targetFile)
    @Suppress("UNCHECKED_CAST")
    val existing = (root[topLevelKey] as? Map<String, Any>) ?: emptyMap()
    val servers = existing.toMutableMap()
    servers[serverName] = serverEntry
    root[topLevelKey] = servers
    val text = JsonOutput.prettyPrint(JsonOutput.toJson(root))
    if (dryRun) {
        println("$logPrefix Would write ${targetFile.absolutePath}:\n$text")
        return
    }
    targetFile.parentFile.mkdirs()
    writeTextAtomically(targetFile, text)
    println("$logPrefix Wrote ${targetFile.absolutePath}")
}

/** Applies osrs-mcp entries for the given client list (comma-separated names, or `all`). */
private fun Project.applyOsrsMcpClientConfiguration(
    client: String,
    dryRun: Boolean,
    logPrefix: String,
) {
    val debug = findProperty("debugMcp")?.toString()?.equals("true", ignoreCase = true) == true
    val repoRoot = rootProject.projectDir.absoluteFile.normalize()
    val absCp = osrsMcpClasspathArg(repoRoot)
    val wf = "\${workspaceFolder}"

    val envCursorOrVscode = mutableMapOf<String, String>()
    envCursorOrVscode["RSPS_ROOT"] = wf
    envCursorOrVscode["LOG_DIR"] = "$wf/logs"
    if (debug) {
        envCursorOrVscode["OSRS_MCP_DEBUG_TOOLS"] = "1"
    }

    val cursorEntry =
        mapOf(
            "type" to "stdio",
            "command" to "\${env:JAVA_HOME}/bin/java",
            "args" to
                listOf(
                    "-cp",
                    "$wf/tools/osrs-mcp/build/install/osrs-mcp/lib/*",
                    osrsMcpMainClass,
                ),
            "env" to envCursorOrVscode,
        )

    val vscodeEntry =
        mapOf(
            "type" to "stdio",
            "command" to "\${env:JAVA_HOME}/bin/java",
            "args" to
                listOf(
                    "-cp",
                    "$wf/tools/osrs-mcp/build/install/osrs-mcp/lib/*",
                    osrsMcpMainClass,
                ),
            "env" to envCursorOrVscode,
        )

    val envClaude =
        mutableMapOf(
            "RSPS_ROOT" to repoRoot.absolutePath.replace('\\', '/'),
            "LOG_DIR" to repoRoot.resolve("logs").absolutePath.replace('\\', '/'),
        )
    if (debug) {
        envClaude["OSRS_MCP_DEBUG_TOOLS"] = "1"
    }
    val claudeEntry =
        mapOf(
            "type" to "stdio",
            "command" to "\${env:JAVA_HOME}/bin/java",
            "args" to listOf("-cp", absCp, osrsMcpMainClass),
            "env" to envClaude,
        )

    val clients = client.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    val unknown = clients.filter { it !in knownOsrsMcpClients }
    if (unknown.isNotEmpty()) {
        throw GradleException(
            "$logPrefix Unknown client(s): $unknown. Use: ${knownOsrsMcpClients.joinToString()}",
        )
    }
    val expandAll = clients.contains("all")
    val wantCursor = expandAll || clients.contains("cursor")
    val wantVscode = expandAll || clients.any { it == "vscode" || it == "github" }
    val wantClaude = expandAll || clients.contains("claude")
    val wantIntellij = expandAll || clients.contains("intellij")

    if (wantCursor) {
        mergeServerEntry(
            rootProject.file(".cursor/mcp.json"),
            "mcpServers",
            osrsMcpServerName,
            cursorEntry,
            dryRun,
            logPrefix,
        )
    }
    if (wantVscode) {
        mergeServerEntry(
            rootProject.file(".vscode/mcp.json"),
            "servers",
            osrsMcpServerName,
            vscodeEntry,
            dryRun,
            logPrefix,
        )
    }
    if (wantClaude) {
        val outDir = layout.buildDirectory.dir("mcp-config").get().asFile
        outDir.mkdirs()
        val fragment = mapOf("mcpServers" to mapOf(osrsMcpServerName to claudeEntry))
        val fragFile = outDir.resolve("claude_desktop_config.osrs-mcp.fragment.json")
        val fragText = JsonOutput.prettyPrint(JsonOutput.toJson(fragment))
        if (dryRun) {
            println("$logPrefix Would write ${fragFile.absolutePath}:\n$fragText")
        } else {
            writeTextAtomically(fragFile, fragText)
            println("$logPrefix Wrote Claude merge fragment: ${fragFile.absolutePath}")
        }
        println(
            "$logPrefix Claude Desktop: merge the \"mcpServers\" entry from that file into your " +
                "claude_desktop_config.json (see %APPDATA%\\Claude\\ on Windows), then restart Claude.",
        )
    }
    if (wantIntellij) {
        val outDir = layout.buildDirectory.dir("mcp-config").get().asFile
        outDir.mkdirs()
        val md =
            buildString {
                appendLine("# IntelliJ / JetBrains AI Gateway - osrs-mcp (manual)")
                appendLine()
                appendLine("Add a **stdio** MCP server with:")
                appendLine(
                    "- **Command:** `" + "${'$'}{env:JAVA_HOME}/bin/java" + "` (JDK 17+; set JAVA_HOME)",
                )
                appendLine(
                    "- **Arguments:** `-cp` `" +
                        absCp +
                        "` `" +
                        osrsMcpMainClass +
                        "`",
                )
                appendLine("- **Environment:**")
                appendLine("  - `RSPS_ROOT` = `${repoRoot.absolutePath.replace('\\', '/')}`")
                appendLine("  - `LOG_DIR` = `${repoRoot.resolve("logs").absolutePath.replace('\\', '/')}`")
                if (debug) {
                    appendLine("  - `OSRS_MCP_DEBUG_TOOLS` = `1`")
                }
                appendLine()
                appendLine("Rebuild classpath after dependency changes: `./gradlew :tools:osrs-mcp:installDist`")
            }
        val mdFile = outDir.resolve("intellij-osrs-mcp.md")
        if (dryRun) {
            println("$logPrefix Would write ${mdFile.absolutePath}:\n$md")
        } else {
            writeTextAtomically(mdFile, md)
            println("$logPrefix Wrote ${mdFile.absolutePath}")
        }
    }
}

/** Removes osrs-mcp entries / generated snippets; optionally patches Claude Desktop global config. */
private fun Project.removeOsrsMcpClientConfiguration(
    client: String,
    dryRun: Boolean,
    logPrefix: String,
    removeInstallLayout: Boolean,
    patchClaudeGlobal: Boolean,
) {
    val clients = client.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    val unknown = clients.filter { it !in knownOsrsMcpClients }
    if (unknown.isNotEmpty()) {
        throw GradleException(
            "$logPrefix Unknown client(s): $unknown. Use: ${knownOsrsMcpClients.joinToString()}",
        )
    }
    val expandAll = clients.contains("all")
    val wantCursor = expandAll || clients.contains("cursor")
    val wantVscode = expandAll || clients.any { it == "vscode" || it == "github" }
    val wantClaude = expandAll || clients.contains("claude")
    val wantIntellij = expandAll || clients.contains("intellij")

    if (wantCursor) {
        removeMcpServerEntry(
            rootProject.file(".cursor/mcp.json"),
            "mcpServers",
            osrsMcpServerName,
            dryRun,
            logPrefix,
        )
    }
    if (wantVscode) {
        removeMcpServerEntry(
            rootProject.file(".vscode/mcp.json"),
            "servers",
            osrsMcpServerName,
            dryRun,
            logPrefix,
        )
    }
    val mcpConfigDir = layout.buildDirectory.dir("mcp-config").get().asFile
    if (wantClaude) {
        deleteOsrsMcpPath(mcpConfigDir.resolve("claude_desktop_config.osrs-mcp.fragment.json"), dryRun, logPrefix)
        if (patchClaudeGlobal) {
            removeOsrsMcpFromClaudeDesktopGlobal(dryRun, logPrefix)
        }
    }
    if (wantIntellij) {
        deleteOsrsMcpPath(mcpConfigDir.resolve("intellij-osrs-mcp.md"), dryRun, logPrefix)
    }
    if (removeInstallLayout) {
        val installDir = layout.buildDirectory.dir("install/osrs-mcp").get().asFile
        deleteOsrsMcpPath(installDir, dryRun, logPrefix)
    }
}

tasks.register("configureOsrsMcp") {
    group = "MCP"
    description =
        "Merges osrs-mcp stdio config. Run with no args for an interactive menu, or " +
            "-Pclient=cursor|vscode|github|claude|intellij|all [-PdebugMcp=true] [-PdryRun=true]. Depends on installDist."
    dependsOn(tasks.named("installDist"))

    doLast {
        val client = promptOsrsMcpClient(project)
        val dryRun = project.findProperty("dryRun")?.toString()?.equals("true", ignoreCase = true) == true
        project.applyOsrsMcpClientConfiguration(client, dryRun, "[configureOsrsMcp]")
    }
}

/**
 * When `-Pclient` is omitted: merges Cursor/VS Code only if the repo already has an `osrs-mcp` entry
 * in those JSON files; always regenerates Claude + IntelliJ fragments (absolute classpath).
 */
private fun Project.detectOsrsMcpUpdateClients(): String {
    val forced = (findProperty("client") as String?)?.trim()?.takeIf { it.isNotEmpty() }
    if (forced != null) {
        return forced.lowercase()
    }
    val parts = linkedSetOf<String>()
    val cursorFile = rootProject.file(".cursor/mcp.json")
    if (cursorFile.exists()) {
        val text = runCatching { cursorFile.readText() }.getOrDefault("")
        if (osrsMcpServerName in text) {
            parts.add("cursor")
        }
    }
    val vscodeFile = rootProject.file(".vscode/mcp.json")
    if (vscodeFile.exists()) {
        val text = runCatching { vscodeFile.readText() }.getOrDefault("")
        if (osrsMcpServerName in text) {
            parts.add("vscode")
        }
    }
    parts.add("claude")
    parts.add("intellij")
    return parts.joinToString(",")
}

/** When `-Pclient` is omitted, removes from every supported place (`all`). */
private fun Project.resolveOsrsMcpRemoveClientCsv(): String {
    val forced = (findProperty("client") as String?)?.trim()?.takeIf { it.isNotEmpty() }
    return if (forced != null) forced.lowercase() else "all"
}

tasks.register("updateOsrsMcp") {
    group = "MCP"
    description =
        "Runs installDist, then refreshes MCP config for detected clients (Cursor/VS Code when " +
            "an osrs-mcp entry exists; always Claude + IntelliJ fragments). Override with " +
            "-Pclient=cursor,vscode,... [-PdebugMcp=true] [-PdryRun=true]."
    dependsOn(tasks.named("installDist"))

    doLast {
        val clientCsv = project.detectOsrsMcpUpdateClients()
        val dryRun = project.findProperty("dryRun")?.toString()?.equals("true", ignoreCase = true) == true
        println("[updateOsrsMcp] Clients: $clientCsv")
        project.applyOsrsMcpClientConfiguration(clientCsv, dryRun, "[updateOsrsMcp]")
    }
}

tasks.register("removeOsrsMcp") {
    group = "MCP"
    description =
        "Removes osrs-mcp everywhere by default (-Pclient omitted = all): Cursor, VS Code, repo " +
            "Claude fragment + IntelliJ notes, and Claude Desktop global claude_desktop_config.json when " +
            "found. Optional -PremoveInstall=true. -PskipClaudeGlobal=true skips editing AppData/config. " +
            "[-PdryRun=true]"
    doLast {
        val clientCsv = project.resolveOsrsMcpRemoveClientCsv()
        val dryRun = project.findProperty("dryRun")?.toString()?.equals("true", ignoreCase = true) == true
        val removeInstall =
            project.findProperty("removeInstall")?.toString()?.equals("true", ignoreCase = true) == true
        val skipClaudeGlobal =
            project.findProperty("skipClaudeGlobal")?.toString()?.equals("true", ignoreCase = true) == true
        println(
            "[removeOsrsMcp] Clients: $clientCsv  removeInstall=$removeInstall  skipClaudeGlobal=$skipClaudeGlobal",
        )
        project.removeOsrsMcpClientConfiguration(
            clientCsv,
            dryRun,
            "[removeOsrsMcp]",
            removeInstall,
            patchClaudeGlobal = !skipClaudeGlobal,
        )
    }
}
