package org.rsmod.api.droptable.toml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dtx.core.RollResult
import dtx.rs.RSPrerollTableBuilder
import dtx.rs.RSWeightedTable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.PendingDropItemConfig
import org.rsmod.game.entity.Player

class DropTableMainRollsTest {
    private val mapper =
        ObjectMapper(TomlFactory())
            .registerKotlinModule()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    @Test
    fun `existing tables keep one main reward`() {
        val definition = decode(tableText())
        val table = DropTableTomlParser.parse(definition, NoHooks)

        assertEquals(1, definition.mainRolls)
        assertEquals(1, table.mainRolls)
        assertEquals(listOf("obj.coins"), rolledItems(definition).map { it.obj })
        assertFalse(DropTableTomlWriter.write(definition).contains("main_rolls"))
    }

    @Test
    fun `main rolls survive canonical TOML round trip`() {
        val definition = decode(tableText("main_rolls = 2"))
        val serialized = DropTableTomlWriter.write(definition)
        val restored = decode(serialized)

        assertTrue(serialized.indexOf("main_rolls = 2") < serialized.indexOf("[main]"))
        assertEquals(definition, restored)
        assertEquals(2, DropTableTomlParser.parse(restored, NoHooks).mainRolls)
    }

    @Test
    fun `two main rolls do not multiply other loot stages`() {
        val definition =
            decode(
                """
                id = "Two rewards"
                npcs = ["npc.snakeboss_boss_ranged"]
                main_rolls = 2

                [[guaranteed]]
                obj = "obj.snakeboss_scale"
                count = "100..299"

                [[pre_roll]]
                numerator = 1
                denominator = 1
                obj = "obj.pre_roll_reward"

                [main]
                total = 1

                [[main.entries]]
                weight = 1
                obj = "obj.cert_flax"
                count = 1000

                [[main.separate_rolls]]
                numerator = 1
                denominator = 1

                [[main.separate_rolls.entries]]
                weight = 1
                obj = "obj.magic_fang"

                [[tertiary]]
                numerator = 1
                denominator = 1
                obj = "obj.snakepet"
                """.trimIndent(),
            )

        val drops = rolledItems(definition)

        assertEquals(
            mapOf(
                "obj.snakeboss_scale" to 1,
                "obj.pre_roll_reward" to 1,
                "obj.cert_flax" to 2,
                "obj.magic_fang" to 1,
                "obj.snakepet" to 1,
            ),
            drops.groupingBy { it.obj }.eachCount(),
        )
        assertEquals(100..299, drops.single { it.obj == "obj.snakeboss_scale" }.count)
        assertEquals(
            listOf(1000..1000, 1000..1000),
            drops.filter { it.obj == "obj.cert_flax" }.map { it.count },
        )
    }

    @Test
    fun `zero main rolls fail with source context`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                DropTableTomlParser.parse(
                    decode(tableText("main_rolls = 0")),
                    NoHooks,
                    "drops/tables/invalid.toml",
                )
            }

        assertTrue(exception.message.orEmpty().contains("drops/tables/invalid.toml"))
        assertTrue(exception.message.orEmpty().contains("main_rolls"))
    }

    @Test
    fun `negative main rolls fail before rolling`() {
        assertThrows(IllegalArgumentException::class.java) {
            DropTableTomlParser.parse(decode(tableText("main_rolls = -1")), NoHooks)
        }
    }

    private fun decode(text: String): TomlDropTableDef = mapper.readValue(text)

    private fun rolledItems(definition: TomlDropTableDef): List<DropRollItem> =
        when (val result = DropTableTomlParser.parse(definition, NoHooks).roll(Player())) {
            is RollResult.Nothing -> emptyList()
            is RollResult.Single -> listOf(result.result)
            is RollResult.ListOf -> result.results
        }

    private fun tableText(rootFields: String = ""): String =
        """
        id = "Main roll test"
        npcs = ["npc.snakeboss_boss_ranged"]
        $rootFields

        [main]
        total = 1

        [[main.entries]]
        weight = 1
        obj = "obj.coins"
        """.trimIndent()

    private object NoHooks : DropTableTomlResolver {
        override fun sharedTable(name: String): RSWeightedTable<Player, DropRollItem> =
            error("Unexpected shared table: $name")

        override fun applyHooks(config: PendingDropItemConfig, hooks: TomlDropHooks) = Unit

        override fun applyBrimstoneKeyRoll(
            builder: RSPrerollTableBuilder<Player, DropRollItem>,
            konarTaskBonus: Boolean,
        ): Unit = error("Unexpected brimstone key roll")
    }
}
