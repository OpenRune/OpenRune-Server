package org.rsmod.content.bosses.zulrah.pack

import com.fasterxml.jackson.dataformat.toml.TomlMapper
import dev.openrune.pack.PluginPacks
import java.io.File
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ZulrahPluginPackTest {
    @Test
    fun `pack is discovered through the native loader and resource convention`() {
        val packs = PluginPacks.discover(File("."))
        val pack = packs.active.filterIsInstance<ZulrahPluginPack>().single()
        assertEquals("zulrah", packs.nameOf(pack))
        val directory = requireNotNull(pack.configDirectory())
        assertTrue(File(directory, "zulrah_npcs.toml").isFile)
        assertTrue(pack.interfaces().isEmpty())
        assertTrue(pack.dbTables().isEmpty())
        assertTrue(pack.extraTasks().isEmpty())
        assertNull(pack.modelDirectory())
        assertNull(pack.spriteDirectory())
        assertNull(pack.cs2Directory())
    }

    @Test
    fun `npc overlay inherits the native melee minion and changes only its attack bonus`() {
        val directory = requireNotNull(ZulrahPluginPack().configDirectory())
        val config = TomlMapper().readTree(File(directory, "zulrah_npcs.toml"))
        assertEquals(setOf("npc"), config.fieldNames().asSequence().toSet())
        val rows = config["npc"].toList()
        assertEquals(listOf("npc.snakeboss_minion_melee"),
            rows.map { it["id"].asText() })
        for (row in rows) {
            assertEquals(row["id"], row["inherit"])
            assertEquals(setOf("id", "inherit", "params"), row.fieldNames().asSequence().toSet())
            assertEquals(setOf("param.attack_melee"), row["params"].fieldNames().asSequence().toSet())
            assertEquals(120, row["params"]["param.attack_melee"].asInt())
        }
    }
}
