package org.rsmod.api.droptable.toml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DropTableTomlBoostedTest {
    private val mapper =
        ObjectMapper(TomlFactory())
            .registerKotlinModule()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    private val toml =
        """
        id = "Test"
        npcs = ["npc.test"]

        [main]
        total = 128

        [[main.entries]]
        weight = 1
        boosted = true
        obj = "obj.dragon_axe"

        [[main.entries]]
        weight = 127
        obj = "obj.bones"

        [[main.separate_rolls]]
        numerator = 1
        denominator = 512
        boosted = true

        [[main.separate_rolls.entries]]
        weight = 1
        obj = "obj.abyssal_whip"

        [[tertiary]]
        numerator = 1
        denominator = 128
        boosted = true
        obj = "obj.dragon_chainbody"

        [[tertiary]]
        numerator = 1
        denominator = 5000
        obj = "obj.pet"
        """.trimIndent()

    @Test
    fun `boosted keys parse onto entries, separate rolls and tertiaries`() {
        val def = mapper.readValue<TomlDropTableDef>(toml)

        assertEquals(listOf(true, false), def.main!!.entries.map { it.boosted })
        assertTrue(def.main!!.separateRolls.single().boosted)
        assertEquals(listOf(true, false), def.tertiary.map { it.boosted })
    }

    @Test
    fun `writer round-trips the flag and omits it when false`() {
        val def = mapper.readValue<TomlDropTableDef>(toml)
        val written = DropTableTomlWriter.write(def)

        assertEquals(3, Regex("^boosted = true$", RegexOption.MULTILINE).findAll(written).count())
        assertEquals(def, mapper.readValue<TomlDropTableDef>(written))
        assertFalse("boosted" in DropTableTomlWriter.write(def.copy(main = null, tertiary = emptyList())))
    }
}
