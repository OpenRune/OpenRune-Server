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
    const val PITY_OUTFIT = 11
    const val SAILING_LEVEL = 12

    private class Method(val rate: Int, val pity: Int, val pityOutfit: Int)

    private val SNARE = Method(20, 40, 38)
    private val BOX = Method(50, 100, 94)
    private val BUTTERFLY = Method(40, 80, 76)
    private val DEADFALL = Method(15, 30, 28)
    private val FALCONRY = Method(10, 20, 18)
    private val NET = Method(25, 50, 46)
    private val PIT = Method(15, 30, 28)
    private val GOAT = Method(48, 96, 90)
    private val TRACKING = Method(15, 30, 28)
    private val HERBIBOAR = Method(7, 14, 12)

    private class Rumour(
        val key: String,
        val name: String,
        val creature: String,
        val level: Int,
        val part: String,
        val partText: String,
        val method: Method,
        val hunters: Int,
        val quest: String = "",
        val herbloreLevel: Int = 0,
        val sailingLevel: Int = 0,
    )

    const val EAGLES_PEAK = "quest_eaglespeak"
    const val BONE_VOYAGE = "quest_bonevoyage"
    const val PRIEST_IN_PERIL = "quest_priestinperil"
    const val SHEEP_HERDER = "quest_sheepherder"

    private const val GILMAN = 1
    private const val CERVUS = 2
    private const val ORNUS = 4
    private const val ACO = 8
    private const val TECO = 16
    private const val WOLF = 32

    private const val FUR = "some of its fur"
    private const val WING = "one of its wings"
    private const val CLAW = "one of its claws"
    private const val HOOF = "one of its hooves"
    private const val TUFT = "obj.hg_kebbittuft"

    private val RUMOURS =
        listOf(
            Rumour("tropical_wagtail", "tropical wagtail", "npc.multicoloured_bird", 19, "obj.hg_tailfeather", "some of its tail feathers", SNARE, GILMAN),
            Rumour("wild_kebbit", "wild kebbit", "npc.huntingbeast_claws", 23, TUFT, FUR, DEADFALL, GILMAN),
            Rumour("sapphire_glacialis", "sapphire glacialis", "npc.butterfly_glacialis", 25, "obj.hg_butterflywing_blue", WING, BUTTERFLY, GILMAN),
            Rumour("swamp_lizard", "swamp lizard", "npc.salamander_green", 29, "obj.hg_lizardclaw", CLAW, NET, GILMAN or CERVUS, PRIEST_IN_PERIL),
            Rumour("spined_larupia", "spined larupia", "npc.hunting_jaguar", 31, "obj.hg_larupia_ear", "one of its ears", PIT, GILMAN or ORNUS),
            Rumour("barb_tailed_kebbit", "barb-tailed kebbit", "npc.huntingbeast_barbedtail", 33, TUFT, FUR, DEADFALL, GILMAN),
            Rumour("snowy_knight", "snowy knight", "npc.butterfly_snowy", 35, "obj.hg_butterflywing_white", WING, BUTTERFLY, GILMAN or ORNUS),
            Rumour("prickly_kebbit", "prickly kebbit", "npc.huntingbeast_spiky", 37, TUFT, FUR, DEADFALL, GILMAN),
            Rumour("embertailed_jerboa", "embertailed jerboa", "npc.varlamore_hunterjerboa01", 39, "obj.hg_jerboatail_large", "its tail", BOX, GILMAN or ORNUS, EAGLES_PEAK),
            Rumour("horned_graahk", "horned graahk", "npc.hunting_leopard", 41, "obj.hg_graahk_horn", "its horn", PIT, GILMAN or CERVUS),
            Rumour("spotted_kebbit", "spotted kebbit", "npc.huntingbeast_speedy", 43, TUFT, FUR, FALCONRY, GILMAN or CERVUS or ORNUS),
            Rumour("black_warlock", "black warlock", "npc.butterfly_warlock", 45, "obj.hg_butterflywing_black", WING, BUTTERFLY, GILMAN or CERVUS),
            Rumour("orange_salamander", "orange salamander", "npc.salamander_orange", 47, "obj.hg_lizardclaw_orange", CLAW, NET, GILMAN or CERVUS or ORNUS or ACO),
            Rumour("razor_backed_kebbit", "razor-backed kebbit", "tracking:razor-backed kebbit", 49, TUFT, FUR, TRACKING, GILMAN or CERVUS),
            Rumour("sabre_toothed_kebbit", "sabre-toothed kebbit", "npc.huntingbeast_sabreteeth", 51, TUFT, FUR, DEADFALL, GILMAN or CERVUS or ORNUS or ACO or TECO),
            Rumour("grey_chinchompa", "grey chinchompa", "npc.hunting_chinchompa", 53, "obj.hg_chinchompatuft", FUR, BOX, GILMAN or CERVUS or ACO or TECO, EAGLES_PEAK),
            Rumour("sabre_toothed_kyatt", "sabre-toothed kyatt", "npc.hunting_snow_tiger", 55, "obj.hg_kyatttooth", "one of its teeth", PIT, GILMAN or ORNUS or ACO or TECO),
            Rumour("dark_kebbit", "dark kebbit", "npc.huntingbeast_silent", 57, TUFT, FUR, FALCONRY, GILMAN or CERVUS or ACO or TECO),
            Rumour("pyre_fox", "pyre fox", "npc.varlamore_hunterfox01", 57, "obj.hg_foxfluff", FUR, DEADFALL, GILMAN or CERVUS or ORNUS),
            Rumour("red_salamander", "red salamander", "npc.salamander_red", 59, "obj.hg_lizardclaw_red", CLAW, NET, GILMAN or ORNUS or ACO or TECO or WOLF),
            Rumour("wyrmscraig_goat", "Wyrmscraig goat", "npc.goat_pit_goat", 60, "obj.hg_goat_pit_hoof", HOOF, GOAT, GILMAN or CERVUS or TECO, SHEEP_HERDER, sailingLevel = 62),
            Rumour(
                "carnivorous_chinchompa",
                "red chinchompa",
                "npc.hunting_chinchompa_big",
                63,
                "obj.hg_chinchompatuft_red",
                FUR,
                BOX,
                GILMAN or CERVUS or ORNUS or ACO or TECO or WOLF,
                EAGLES_PEAK,
            ),
            Rumour("dashing_kebbit", "dashing kebbit", "npc.huntingbeast_speedy2", 69, TUFT, FUR, FALCONRY, GILMAN or ACO or TECO or WOLF),
            Rumour("sunlight_antelope", "sunlight antelope", "npc.sunlight_antelope", 72, "obj.hg_antelopehoof_sun", HOOF, PIT, GILMAN or ACO or TECO or WOLF),
            Rumour("sunlight_moth", "sunlight moth", "npc.moth_sunlight", 75, "obj.hg_mothwing_sun", WING, BUTTERFLY, GILMAN or CERVUS or TECO),
            Rumour("tecu_salamander", "tecu salamander", "npc.salamander_mountain", 79, "obj.hg_lizardclaw_mountain", CLAW, NET, GILMAN or ACO or WOLF),
            Rumour("herbiboar", "herbiboar", "npc.fossil_herbiboar_visible", 80, "obj.hg_herbytuft", FUR, HERBIBOAR, GILMAN or TECO or WOLF, BONE_VOYAGE, 31),
            Rumour("moonlight_moth", "moonlight moth", "npc.moth_moonlight", 85, "obj.hg_mothwing_moon", WING, BUTTERFLY, GILMAN or ACO or WOLF),
            Rumour("moonlight_antelope", "moonlight antelope", "npc.moonlight_antelope", 91, "obj.hg_antelopehoof_moon", HOOF, PIT, GILMAN or WOLF),
        )

    fun hunterRumours() =
        dbTable("dbtable.hunter_rumours", serverOnly = true) {
            column("index", INDEX, VarType.INT)
            column("name", NAME, VarType.STRING)
            column("creature", CREATURE, VarType.STRING)
            column("level", LEVEL, VarType.INT)
            column("part", PART, VarType.OBJ)
            column("part_text", PART_TEXT, VarType.STRING)
            column("rate", RATE, VarType.INT)
            column("pity", PITY, VarType.INT)
            column("hunters", HUNTERS, VarType.INT)
            column("quest", QUEST, VarType.STRING)
            column("herblore_level", HERBLORE_LEVEL, VarType.INT)
            column("pity_outfit", PITY_OUTFIT, VarType.INT)
            column("sailing_level", SAILING_LEVEL, VarType.INT)

            RUMOURS.forEachIndexed { index, rumour ->
                row("dbrow.hunter_rumour_${rumour.key}") {
                    column(INDEX, index + 1)
                    column(NAME, rumour.name)
                    column(CREATURE, rumour.creature)
                    column(LEVEL, rumour.level)
                    columnRSCM(PART, rumour.part)
                    column(PART_TEXT, rumour.partText)
                    column(RATE, rumour.method.rate)
                    column(PITY, rumour.method.pity)
                    column(HUNTERS, rumour.hunters)
                    column(QUEST, rumour.quest)
                    column(HERBLORE_LEVEL, rumour.herbloreLevel)
                    column(PITY_OUTFIT, rumour.method.pityOutfit)
                    column(SAILING_LEVEL, rumour.sailingLevel)
                }
            }
        }
}
