package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterRumourHuntersTable {
    const val INDEX = 0
    const val NPC = 1
    const val NAME = 2
    const val LEVEL = 3
    const val MODIFIER = 4
    const val LOOT_SACK = 5

    private data class Hunter(val key: String, val name: String, val level: Int, val modifier: Int, val sack: String)

    private val HUNTERS =
        listOf(
            Hunter("gilman", "Gilman", 46, 50, "obj.hg_lootsack_t0"),
            Hunter("cervus", "Cervus", 57, 50, "obj.hg_lootsack_t1"),
            Hunter("ornus", "Ornus", 57, 50, "obj.hg_lootsack_t1"),
            Hunter("aco", "Aco", 72, 55, "obj.hg_lootsack_t2"),
            Hunter("teco", "Teco", 72, 55, "obj.hg_lootsack_t2"),
            Hunter("wolf", "Wolf", 91, 60, "obj.hg_lootsack_t3"),
        )

    fun hunterRumourHunters() =
        dbTable("dbtable.hunter_rumour_hunters", serverOnly = true) {
            column("index", INDEX, VarType.INT)
            column("npc", NPC, VarType.NPC)
            column("name", NAME, VarType.STRING)
            column("level", LEVEL, VarType.INT)
            column("modifier", MODIFIER, VarType.INT)
            column("loot_sack", LOOT_SACK, VarType.OBJ)

            HUNTERS.forEachIndexed { index, hunter ->
                row("dbrow.hunter_rumour_hunter_${hunter.key}") {
                    column(INDEX, index)
                    columnRSCM(NPC, "npc.hg_${hunter.key}")
                    column(NAME, hunter.name)
                    column(LEVEL, hunter.level)
                    column(MODIFIER, hunter.modifier)
                    columnRSCM(LOOT_SACK, hunter.sack)
                }
            }
        }
}

object HunterRumoursTable {
    const val INDEX = 0
    const val NAME = 1
    const val CREATURE = 2
    const val LEVEL = 3
    const val PART = 4
    const val PART_TEXT = 5
    const val RATE = 6
    const val PITY = 7
    const val HUNTERS = 8
    const val QUEST = 9
    const val HERBLORE_LEVEL = 10

    private data class Rumour(
        val key: String,
        val name: String,
        val creature: String,
        val level: Int,
        val part: String,
        val partText: String,
        val rate: Int,
        val pity: Int,
        val hunters: Int,
        val quest: String = "",
        val herbloreLevel: Int = 0,
    )

    const val EAGLES_PEAK = "quest_eaglespeak"
    const val BONE_VOYAGE = "quest_bonevoyage"

    private const val GILMAN = 1
    private const val CERVUS = 2
    private const val ORNUS = 4
    private const val ACO = 8
    private const val TECO = 16
    private const val WOLF = 32

    private val RUMOURS =
        listOf(
            Rumour("tropical_wagtail", "tropical wagtail", "npc.multicoloured_bird", 19, "obj.hg_tailfeather", "tail feathers", 20, 40, GILMAN),
            Rumour("grey_chinchompa", "grey chinchompa", "npc.hunting_chinchompa", 53, "obj.hg_chinchompatuft", "fur", 50, 100, GILMAN or CERVUS or ACO or TECO, EAGLES_PEAK),
            Rumour(
                "carnivorous_chinchompa",
                "red chinchompa",
                "npc.hunting_chinchompa_big",
                63,
                "obj.hg_chinchompatuft_red",
                "fur",
                50,
                100,
                GILMAN or CERVUS or ORNUS or ACO or TECO or WOLF,
                EAGLES_PEAK,
            ),
            Rumour("herbiboar", "herbiboar", "npc.fossil_herbiboar_visible", 80, "obj.hg_herbytuft", "fur", 7, 14, GILMAN or TECO or WOLF, BONE_VOYAGE, 31),
            Rumour("sapphire_glacialis", "sapphire glacialis", "npc.butterfly_glacialis", 25, "obj.hg_butterflywing_blue", "wings", 40, 80, GILMAN),
            Rumour("snowy_knight", "snowy knight", "npc.butterfly_snowy", 35, "obj.hg_butterflywing_white", "wings", 40, 80, GILMAN or ORNUS),
            Rumour("black_warlock", "black warlock", "npc.butterfly_warlock", 45, "obj.hg_butterflywing_black", "wings", 40, 80, GILMAN or CERVUS),
            Rumour("sunlight_moth", "sunlight moth", "npc.moth_sunlight", 75, "obj.hg_mothwing_sun", "wings", 40, 80, GILMAN or CERVUS or TECO),
            Rumour("embertailed_jerboa", "embertailed jerboa", "npc.varlamore_hunterjerboa01", 39, "obj.hg_jerboatail_large", "tail", 50, 100, GILMAN or ORNUS, EAGLES_PEAK),
            Rumour("moonlight_moth", "moonlight moth", "npc.moth_moonlight", 85, "obj.hg_mothwing_moon", "wings", 40, 80, GILMAN or ACO or WOLF),
        )

    fun hunterRumours() =
        dbTable("dbtable.hunter_rumours", serverOnly = true) {
            column("index", INDEX, VarType.INT)
            column("name", NAME, VarType.STRING)
            column("creature", CREATURE, VarType.NPC)
            column("level", LEVEL, VarType.INT)
            column("part", PART, VarType.OBJ)
            column("part_text", PART_TEXT, VarType.STRING)
            column("rate", RATE, VarType.INT)
            column("pity", PITY, VarType.INT)
            column("hunters", HUNTERS, VarType.INT)
            column("quest", QUEST, VarType.STRING)
            column("herblore_level", HERBLORE_LEVEL, VarType.INT)

            RUMOURS.forEachIndexed { index, rumour ->
                row("dbrow.hunter_rumour_${rumour.key}") {
                    column(INDEX, index + 1)
                    column(NAME, rumour.name)
                    columnRSCM(CREATURE, rumour.creature)
                    column(LEVEL, rumour.level)
                    columnRSCM(PART, rumour.part)
                    column(PART_TEXT, rumour.partText)
                    column(RATE, rumour.rate)
                    column(PITY, rumour.pity)
                    column(HUNTERS, rumour.hunters)
                    column(QUEST, rumour.quest)
                    column(HERBLORE_LEVEL, rumour.herbloreLevel)
                }
            }
        }
}
