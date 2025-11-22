package org.alter.impl.skills.runecrafting

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

enum class Rune(
    val id: String,
    val essence: Array<String>,
    val level: Int,
    val xp: Int,
    val dbId: String
) {
    AIR(
        id = "items.mindrune",
        essence = arrayOf("items.blankrune", "items.blankrune_high"),
        level = 1,
        xp = 5,
        dbId = "dbrows.runecrafting_rune_air"
    ),
    MIND(
        id = "items.mindrune",
        essence = arrayOf("items.blankrune", "items.blankrune_high"),
        level = 2,
        xp = 5,
        dbId = "dbrows.runecrafting_rune_mind"
    ),
    WATER(
        id = "items.waterrune",
        essence = arrayOf("items.blankrune", "items.blankrune_high"),
        level = 5,
        xp = 6,
        dbId = "dbrows.runecrafting_rune_water"
    ),
    EARTH(
        id = "items.earthrune",
        essence = arrayOf("items.blankrune", "items.blankrune_high"),
        level = 9,
        xp = 6,
        dbId = "dbrows.runecrafting_rune_earth"
    ),
    FIRE(
        id = "items.firerune",
        essence = arrayOf("items.blankrune", "items.blankrune_high"),
        level = 14,
        xp = 7,
        dbId = "dbrows.runecrafting_rune_fire"
    ),
    BODY(
        id = "items.bodyrune",
        essence = arrayOf("items.blankrune", "items.blankrune_high"),
        level = 20,
        xp = 7,
        dbId = "dbrows.runecrafting_rune_body"
    ),
    COSMIC(
        id = "items.cosmicrune",
        essence = arrayOf("items.blankrune_high"),
        level = 27,
        xp = 8,
        dbId = "dbrows.runecrafting_rune_cosmic"
    ),
    CHAOS(
        id = "items.chaosrune",
        essence = arrayOf("items.blankrune_high"),
        level = 35,
        xp = 8,
        dbId = "dbrows.runecrafting_rune_chaos"
    ),
    ASTRAL(
        id = "items.astralrune",
        essence = arrayOf("items.blankrune_high"),
        level = 40,
        xp = 9,
        dbId = "dbrows.runecrafting_rune_astral"
    ),
    NATURE(
        id = "items.naturerune",
        essence = arrayOf("items.blankrune_high"),
        level = 44,
        xp = 9,
        dbId = "dbrows.runecrafting_rune_nature"
    ),
    LAW(
        id = "items.lawrune",
        essence = arrayOf("items.blankrune_high"),
        level = 54,
        xp = 9,
        dbId = "dbrows.runecrafting_rune_law"
    ),
    DEATH(
        id = "items.deathrune",
        essence = arrayOf("items.blankrune_high"),
        level = 65,
        xp = 10,
        dbId = "dbrows.runecrafting_rune_death"
    ),
    BLOOD(
        id = "items.bloodrune",
        essence = arrayOf("items.arceuus_essence_block_dark"),
        level = 77,
        xp = 24,
        dbId = "dbrows.runecrafting_rune_blood"
    ),
    SOUL(
        id = "items.soulrune",
        essence = arrayOf("items.arceuus_essence_block_dark"),
        level = 90,
        xp = 30,
        dbId = "dbrows.runecrafting_rune_soul"
    ),
    WRATH(
        id = "items.wrathrune",
        essence = arrayOf("items.blankrune_high"),
        level = 95,
        xp = 8,
        dbId = "dbrows.runecrafting_rune_wrath"
    );

    companion object {
        val values = enumValues<Rune>()
    }
}

object RunecraftRune {

    val ITEM = 0
    val ESSENCE = 1
    val LEVEL = 3
    val XP = 3

    fun runecraftRune() = dbTable("tables.runecrafting_runes") {
        column("rune_output", ITEM, VarType.OBJ)
        column("valid_essences", ESSENCE, VarType.LOC)
        column("xp", XP, VarType.INT)

        Rune.entries.forEach {
            row(it.dbId) {
                columnRSCM(ITEM, it.id)
                columnRSCM(ESSENCE, *it.essence)
                column(LEVEL, it.xp)
            }
        }


    }

}