package org.rsmod.content.other.toolbelt.pack

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import java.io.File

object ToolbeltPackSettings {
    private val mapper = ObjectMapper(TomlFactory())

    private const val PLUGIN = "content/other/toolbelt/src/main/resources"

    fun isEnabled(projectRoot: File = File("..")): Boolean {
        val file = File(projectRoot, "$PLUGIN/toolbelt.toml")
        if (!file.isFile) {
            return false
        }
        return runCatching {
            val root = mapper.readTree(file)
            root.path("is_enabled").asBoolean(true)
        }.getOrDefault(false)
    }

    fun configDirectory(projectRoot: File = File("..")): File =
        File(projectRoot, "$PLUGIN/pack/configs")

    fun spriteDirectory(projectRoot: File = File("..")): File =
        File(projectRoot, "$PLUGIN/pack/sprites")

    fun scriptDirectory(projectRoot: File = File("..")): File =
        File(projectRoot, "$PLUGIN/pack/scripts")

    fun cs2Directory(projectRoot: File = File("..")): File =
        File(projectRoot, "$PLUGIN/pack/cs2")
}
