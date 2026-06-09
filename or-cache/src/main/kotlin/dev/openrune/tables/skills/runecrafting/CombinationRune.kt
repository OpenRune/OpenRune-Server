package dev.openrune.tables.skills.runecrafting

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

enum class CombinationRuneData(val runeOutput: String, val level: Int, val xp: Double, val runeInput: String, val row : String, val talisman : String) {
    MIST_AIR(runeOutput = "obj.mistrune", level = 6, xp = 8.0, runeInput = "obj.waterrune","dbrow.mist_from_wateraltar","obj.water_talisman"),
    MIST_WATER(runeOutput = "obj.mistrune", level = 6, xp = 8.5, runeInput = "obj.airrune","dbrow.mist_from_airaltar","obj.air_talisman"),

    DUST_AIR(runeOutput = "obj.dustrune", level = 10, xp = 8.3, runeInput = "obj.earthrune","dbrow.dust_from_earthaltar","obj.earth_talisman"),
    DUST_EARTH(runeOutput = "obj.dustrune", level = 10, xp = 9.0, runeInput = "obj.airrune","dbrow.dust_from_airaltar","obj.air_talisman"),

    MUD_WATER(runeOutput = "obj.mudrune", level = 13, xp = 9.3, runeInput = "obj.earthrune","dbrow.mud_from_earthaltar","obj.earth_talisman"),
    MUD_EARTH(runeOutput = "obj.mudrune", level = 13, xp = 9.5, runeInput = "obj.waterrune","dbrow.mud_from_wateraltar","obj.water_talisman"),

    SMOKE_AIR(runeOutput = "obj.smokerune", level = 15, xp = 8.5, runeInput = "obj.firerune","dbrow.smoke_from_firealtar","obj.fire_talisman"),
    SMOKE_FIRE(runeOutput = "obj.smokerune", level = 15, xp = 9.5, runeInput = "obj.airrune","dbrow.smoke_from_airaltar","obj.air_talisman"),

    STEAM_WATER(runeOutput = "obj.steamrune", level = 19, xp = 9.5, runeInput = "obj.firerune","dbrow.steam_from_firealtar","obj.fire_talisman"),
    STEAM_FIRE(runeOutput = "obj.steamrune", level = 19, xp = 10.0, runeInput = "obj.waterrune","dbrow.steam_from_wateraltar","obj.water_talisman"),

    LAVA_EARTH(runeOutput = "obj.lavarune", level = 23, xp = 10.0, runeInput = "obj.firerune","dbrow.lava_from_firealtar","obj.fire_talisman"),
    LAVA_FIRE(runeOutput = "obj.lavarune", level = 23, xp = 10.5, runeInput = "obj.earthrune","dbrow.lava_from_earthaltar","obj.earth_talisman");

}

object CombinationRune {

    const val RUNE_OUTPUT = 0
    const val LEVEL = 1
    const val XP = 2
    const val RUNE_INPUT = 3
    const val TALISMAN = 4

    fun runecraftComboRune() = dbTable("dbtable.comborune_recipe", serverOnly = true) {

        column("rune_output", RUNE_OUTPUT, VarType.OBJ)
        column("level", LEVEL, VarType.INT)
        column("xp", XP, VarType.INT)
        column("rune_input", RUNE_INPUT, VarType.OBJ)
        column("talisman", TALISMAN, VarType.OBJ)

        CombinationRuneData.entries.forEach {
            row(it.row) {
                columnRSCM(RUNE_OUTPUT, it.runeOutput)
                column(LEVEL, it.level)
                column(XP, (it.xp * 10).toInt())
                columnRSCM(RUNE_INPUT, it.runeInput)
                columnRSCM(TALISMAN, it.talisman)
            }
        }

    }



}
