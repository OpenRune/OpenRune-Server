package dev.openrune.gamevals

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PluginGamevalMergerTest {
    @Test
    fun `merges content and api plugin gamevals`(@TempDir root: File) {
        writeToml(root, "content/other/foo", "[gamevals.varp]\nfoo_state=65001\n")
        writeToml(root, "api/bar", "[gamevals.varp]\nbar_state=65002\n")

        PluginGamevalMerger.merge(root)

        val varps = File(root, ".data/gamevals/varp.rscm").readLines().filter(String::isNotBlank)
        assertEquals(setOf("foo_state=65001", "bar_state=65002"), varps.toSet())
    }

    private fun writeToml(root: File, module: String, text: String) {
        val file = File(root, "$module/src/main/resources/gamevals.toml")
        file.parentFile.mkdirs()
        file.writeText(text)
    }
}
