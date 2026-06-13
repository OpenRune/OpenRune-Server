package dev.openrune.tables.skills.runecrafting

import dev.openrune.definition.util.VarType
import dev.openrune.tables.production.productionTable

enum class CombinationRuneData(
    val runeOutput: String,
    val level: Int,
    val xp: Double,
    val runeInput: String,
    val row: String,
    val talisman: String,
) {
    MIST_AIR(runeOutput = "obj.mistrune", level = 6, xp = 8.0, runeInput = "obj.waterrune", row = "dbrow.mist_from_wateraltar", talisman = "obj.water_talisman"),
    MIST_WATER(runeOutput = "obj.mistrune", level = 6, xp = 8.5, runeInput = "obj.airrune", row = "dbrow.mist_from_airaltar", talisman = "obj.air_talisman"),

    DUST_AIR(runeOutput = "obj.dustrune", level = 10, xp = 8.3, runeInput = "obj.earthrune", row = "dbrow.dust_from_earthaltar", talisman = "obj.earth_talisman"),
    DUST_EARTH(runeOutput = "obj.dustrune", level = 10, xp = 9.0, runeInput = "obj.airrune", row = "dbrow.dust_from_airaltar", talisman = "obj.air_talisman"),

    MUD_WATER(runeOutput = "obj.mudrune", level = 13, xp = 9.3, runeInput = "obj.earthrune", row = "dbrow.mud_from_earthaltar", talisman = "obj.earth_talisman"),
    MUD_EARTH(runeOutput = "obj.mudrune", level = 13, xp = 9.5, runeInput = "obj.waterrune", row = "dbrow.mud_from_wateraltar", talisman = "obj.water_talisman"),

    SMOKE_AIR(runeOutput = "obj.smokerune", level = 15, xp = 8.5, runeInput = "obj.firerune", row = "dbrow.smoke_from_firealtar", talisman = "obj.fire_talisman"),
    SMOKE_FIRE(runeOutput = "obj.smokerune", level = 15, xp = 9.5, runeInput = "obj.airrune", row = "dbrow.smoke_from_airaltar", talisman = "obj.air_talisman"),

    STEAM_WATER(runeOutput = "obj.steamrune", level = 19, xp = 9.5, runeInput = "obj.firerune", row = "dbrow.steam_from_firealtar", talisman = "obj.fire_talisman"),
    STEAM_FIRE(runeOutput = "obj.steamrune", level = 19, xp = 10.0, runeInput = "obj.waterrune", row = "dbrow.steam_from_wateraltar", talisman = "obj.water_talisman"),

    LAVA_EARTH(runeOutput = "obj.lavarune", level = 23, xp = 10.0, runeInput = "obj.firerune", row = "dbrow.lava_from_firealtar", talisman = "obj.fire_talisman"),
    LAVA_FIRE(runeOutput = "obj.lavarune", level = 23, xp = 10.5, runeInput = "obj.earthrune", row = "dbrow.lava_from_earthaltar", talisman = "obj.earth_talisman");
}

object CombinationRune {

    const val COL_TALISMAN = 7

    fun runecraftComboRune() = productionTable(
        "dbtable.comborune_recipe",
        serverOnly = true,
        defaultCategory = "Combination Runes",
        extraColumns = {
            column("talisman", COL_TALISMAN, VarType.OBJ)
        },
    ) {
        CombinationRuneData.entries.forEach { combo ->
            row(combo.row) {
                production {
                    input(combo.runeInput)
                    statReq("stat.runecrafting", combo.level)
                    xp((combo.xp * 10).toInt())
                    output(combo.runeOutput)
                }
                columnRSCM(COL_TALISMAN, combo.talisman)
            }
        }
    }
}
