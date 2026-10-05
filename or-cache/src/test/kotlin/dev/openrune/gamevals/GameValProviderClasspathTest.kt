package dev.openrune.gamevals

import java.io.File
import java.net.URLClassLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GameValProviderClasspathTest {
    @Test
    fun `classpath gamevals fill in only assigned keys no file source declares`(@TempDir tmp: File) {
        val plugin = File(tmp, "content/plugin/src/main/resources").apply { mkdirs() }
        File(plugin, "gamevals.toml").writeText("[gamevals.varp]\nshared=70001\n")
        val gamevals = File(tmp, "gamevals").apply { mkdirs() }
        File(gamevals, "varp.rscm").writeText("merged=70004\n")
        val jar = File(tmp, "jar").apply { mkdirs() }
        File(jar, "gamevals.toml")
            .writeText(
                "[gamevals.varp]\nshared=70002\nmerged=70005\nextra=70003\nstale=70001\n" +
                    "unassigned=-1\n"
            )

        val provider =
            URLClassLoader(arrayOf(jar.toURI().toURL()), null).use { loader ->
                GameValProvider(gamevalResources = loader).apply {
                    val none = File(tmp, "none")
                    load(none, none, File(tmp, "content"), none, gamevals)
                }
            }

        val varps = provider.mappings.getValue("varp")
        assertEquals(70001, varps["varp.shared"])
        assertEquals(70004, varps["varp.merged"])
        assertEquals(70003, varps["varp.extra"])
        assertFalse("varp.stale" in varps)
        assertFalse("varp.unassigned" in varps)
    }

    @Test
    fun `every plugin gamevals toml is packaged as a resource`() {
        val repoRoot =
            generateSequence(File("").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile && File(it, "content").isDirectory }
        val misplaced =
            listOf("content", "api").flatMap { dir ->
                File(repoRoot, dir)
                    .walk()
                    .onEnter { it.name != "build" && it.name != "out" }
                    .filter { it.isFile && it.name == "gamevals.toml" }
                    .map { it.relativeTo(repoRoot).invariantSeparatorsPath }
                    .filterNot { it.endsWith("src/main/resources/gamevals.toml") }
                    .toList()
            }
        assertEquals(emptyList<String>(), misplaced)
    }
}
