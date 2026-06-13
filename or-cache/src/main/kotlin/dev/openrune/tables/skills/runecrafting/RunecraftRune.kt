package dev.openrune.tables.skills.runecrafting

import dev.openrune.definition.util.VarType
import dev.openrune.tables.production.productionTable

enum class Rune(
    val id: String,
    val essence: List<String>,
    val level: Int,
    val xp: Int,
    val dbId: String,
    val extract: String
) {
    AIR(
        id = "obj.airrune",
        essence = listOf("obj.blankrune", "obj.blankrune_high"),
        level = 1,
        xp = 5,
        dbId = "dbrow.runecrafting_rune_air",
        extract = "obj.scar_extract_warped"
    ),
    MIND(
        id = "obj.mindrune",
        essence = listOf("obj.blankrune", "obj.blankrune_high"),
        level = 2,
        xp = 5,
        dbId = "dbrow.runecrafting_rune_mind",
        extract = "obj.scar_extract_warped"
    ),
    WATER(
        id = "obj.waterrune",
        essence = listOf("obj.blankrune", "obj.blankrune_high"),
        level = 5,
        xp = 6,
        dbId = "dbrow.runecrafting_rune_water",
        extract = "obj.scar_extract_warped"
    ),
    EARTH(
        id = "obj.earthrune",
        essence = listOf("obj.blankrune", "obj.blankrune_high"),
        level = 9,
        xp = 6,
        dbId = "dbrow.runecrafting_rune_earth",
        extract = "obj.scar_extract_warped"
    ),
    FIRE(
        id = "obj.firerune",
        essence = listOf("obj.blankrune", "obj.blankrune_high"),
        level = 14,
        xp = 7,
        dbId = "dbrow.runecrafting_rune_fire",
        extract = "obj.scar_extract_warped"
    ),
    BODY(
        id = "obj.bodyrune",
        essence = listOf("obj.blankrune", "obj.blankrune_high"),
        level = 20,
        xp = 7,
        dbId = "dbrow.runecrafting_rune_body",
        extract = "obj.scar_extract_warped"
    ),
    COSMIC(
        id = "obj.cosmicrune",
        essence = listOf("obj.blankrune_high"),
        level = 27,
        xp = 8,
        dbId = "dbrow.runecrafting_rune_cosmic",
        extract = "obj.scar_extract_twisted"
    ),
    CHAOS(
        id = "obj.chaosrune",
        essence = listOf("obj.blankrune_high"),
        level = 35,
        xp = 8,
        dbId = "dbrow.runecrafting_rune_chaos",
        extract = "obj.scar_extract_twisted"
    ),
    SUNFIRE(
        id = "obj.sunfirerune",
        essence = listOf("obj.blankrune_high"),
        level = 33,
        xp = 9,
        dbId = "dbrow.runecrafting_rune_sunfire",
        extract = "obj.scar_extract_twisted"
    ),
    ASTRAL(
        id = "obj.astralrune",
        essence = listOf("obj.blankrune_high"),
        level = 40,
        xp = 9,
        dbId = "dbrow.runecrafting_rune_astral",
        extract = "obj.scar_extract_mangled"
    ),
    NATURE(
        id = "obj.naturerune",
        essence = listOf("obj.blankrune_high"),
        level = 44,
        xp = 9,
        dbId = "dbrow.runecrafting_rune_nature",
        extract = "obj.scar_extract_mangled"
    ),
    LAW(
        id = "obj.lawrune",
        essence = listOf("obj.blankrune_high"),
        level = 54,
        xp = 9,
        dbId = "dbrow.runecrafting_rune_law",
        extract = "obj.scar_extract_mangled"
    ),
    DEATH(
        id = "obj.deathrune",
        essence = listOf("obj.blankrune_high"),
        level = 65,
        xp = 10,
        dbId = "dbrow.runecrafting_rune_death",
        extract = "obj.scar_extract_mangled"
    ),
    BLOOD(
        id = "obj.bloodrune",
        essence = listOf("obj.blankrune_high"),
        level = 77,
        xp = 10,
        dbId = "dbrow.runecrafting_rune_blood",
        extract = "obj.scar_extract_scarred"
    ),
    BLOOD_DARK(
        id = "obj.bloodrune",
        essence = listOf("obj.bigblankrune"),
        level = 77,
        xp = 24,
        dbId = "dbrow.runecrafting_rune_blood_dark",
        extract = "obj.scar_extract_scarred"
    ),
    SOUL(
        id = "obj.soulrune",
        essence = listOf("obj.bigblankrune"),
        level = 90,
        xp = 30,
        dbId = "dbrow.runecrafting_rune_soul",
        extract = "obj.scar_extract_scarred"
    ),
    AETHER(
        id = "obj.aetherrune",
        essence = listOf("obj.gotr_guardian_essence"),
        level = 90,
        xp = 20,
        dbId = "dbrow.runecrafting_rune_aether",
        extract = "obj.scar_extract_scarred"
    ),
    WRATH(
        id = "obj.wrathrune",
        essence = listOf("obj.blankrune_high"),
        level = 95,
        xp = 8,
        dbId = "dbrow.runecrafting_rune_wrath",
        extract = "obj.scar_extract_scarred"
    );

    companion object {
        val values = enumValues<Rune>()
    }
}

object RunecraftRune {

    const val COL_EXTRACT = 7

    fun runecraftRune() = productionTable(
        "dbtable.runecrafting_runes",
        serverOnly = true,
        defaultCategory = "Runecraft",
        extraColumns = {
            column("extract", COL_EXTRACT, VarType.OBJ)
        },
    ) {
        Rune.entries.forEach { rune ->
            row(rune.dbId) {
                production {
                    input(rune.essence)
                    statReq("stat.runecrafting", rune.level)
                    xp(rune.xp)
                    output(rune.id)
                }
                columnRSCM(COL_EXTRACT, rune.extract)
            }
        }
    }
}
