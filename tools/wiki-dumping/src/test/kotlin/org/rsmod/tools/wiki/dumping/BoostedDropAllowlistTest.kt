package org.rsmod.tools.wiki.dumping

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.droptable.toml.DropTableTomlWriter

class BoostedDropAllowlistTest {
    private val rules =
        BoostedDropAllowlist.parse(
            """
            [[boosted]]
            objs = ["obj.abyssal_whip"]
            npcs = ["npc.slayer_abyssal*"]

            [[boosted]]
            objs = ["obj.kraken_tentacle"]
            rolls = ["tertiary"]
            """.trimIndent(),
        )

    private fun entry(obj: String, weight: Int = 1, outOf: Int? = null) =
        ResolvedDropEntry(
            obj = obj,
            quantity = "1",
            weight = weight,
            outOf = outOf,
            wikiName = obj,
        )

    private fun spec(npc: String) =
        GeneratedDropTableSpec(
            tableVarName = "TestDropTable",
            tableIdentifier = "Test",
            npcRscmKeys = listOf(npc),
            guaranteed = emptyList(),
            main = emptyList(),
            separateRolls =
                listOf(
                    SeparateRollSpec("Weapons", 1, 512, listOf(entry("obj.abyssal_whip"))),
                    SeparateRollSpec("Other", 1, 32000, listOf(entry("obj.abyssal_dagger"))),
                ),
            tertiary =
                listOf(
                    entry("obj.kraken_tentacle", outOf = 400),
                    entry("obj.bones", outOf = 3),
                ),
        )

    @Test
    fun `flags only matching rolls`() {
        val result = BoostedDropAllowlist.apply(spec("npc.slayer_abyssal"), rules)

        assertEquals(listOf(true, false), result.separateRolls.map { it.boosted })
        assertEquals(listOf(true, false), result.tertiary.map { it.boosted })
    }

    @Test
    fun `npc glob and roll kind narrow a rule`() {
        val result = BoostedDropAllowlist.apply(spec("npc.goblin"), rules)

        assertEquals(listOf(false, false), result.separateRolls.map { it.boosted })
        assertEquals(listOf(true, false), result.tertiary.map { it.boosted })
    }

    @Test
    fun `toml output carries the flag and re-dumping is identical`() {
        fun dump(): String =
            DropTableTomlWriter.write(
                DropTableTomlExporter.exportTable(
                    BoostedDropAllowlist.apply(spec("npc.slayer_abyssal"), rules),
                ),
            )

        val first = dump()
        assertEquals(first, dump())
        assertEquals(2, Regex("^boosted = true$", RegexOption.MULTILINE).findAll(first).count())
    }

    @Test
    fun `unflagged tables have no boosted keys`() {
        val toml =
            DropTableTomlWriter.write(DropTableTomlExporter.exportTable(spec("npc.goblin")))

        assertFalse("boosted" in toml)
    }

    @Test
    fun `shipped allowlist parses and every rule names objs`() {
        assertTrue(BoostedDropAllowlist.rules.isNotEmpty())
        assertTrue(BoostedDropAllowlist.rules.all { it.objs.isNotEmpty() })
    }
}
