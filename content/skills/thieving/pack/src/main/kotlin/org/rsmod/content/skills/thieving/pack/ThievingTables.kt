package org.rsmod.content.skills.thieving.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

private const val DEFAULT_SHOUT: String = "What do you think you're doing?"

/**
 * [low] and [high] are the level-1 and level-99 odds out of 256 fed to the shared skilling success
 * formula, taken from the wiki's own pickpocket charts.
 *
 * [npcs] is every npc that pickpockets as this target; each must carry Pickpocket on [op].
 */
data class PickpocketTarget(
    val displayName: String,
    val level: Int,
    val xp: Double,
    val low: Int,
    val high: Int,
    val stunTicks: Int,
    val stunDamage: Int,
    val npcs: List<String> = emptyList(),
    val op: Int = 3,
    val pouch: String? = null,
    val caughtShout: String = DEFAULT_SHOUT,
    val lowercaseName: Boolean = true,
)

/** Stealing is refused when one of [owners] or [guards] can see the player; a guard attacks. */
data class StallTarget(
    val loc: String,
    val level: Int,
    val xp: Double,
    val empty: String? = null,
    val respawn: Int = 20,
    val owners: List<String> = emptyList(),
    val guards: List<String> = emptyList(),
    val attemptMessage: String? = null,
)

data class CoinPouch(val obj: String, val coins: IntRange)

/**
 * Pickpocket targets, market stalls and the coin pouches pickpocketing hands out. Loot lives in
 * the thieving module's drop tables, keyed by these rows.
 *
 * Xp is stored multiplied by ten: the wiki quotes fractional values - a Workman is 10.4 - and the
 * xp column is an int. ThievingScript divides by ten when awarding it.
 */
object ThievingTables {
    const val PP_NAME = 0
    const val PP_LEVEL = 1
    const val PP_XP = 2
    const val PP_LOW = 3
    const val PP_HIGH = 4
    const val PP_STUN_TICKS = 5
    const val PP_STUN_DAMAGE = 6
    const val PP_NPCS = 7
    const val PP_POUCH = 8
    const val PP_CAUGHT_SHOUT = 9
    const val PP_LOWERCASE_NAME = 10
    const val PP_OP = 11

    const val STALL_LOC = 0
    const val STALL_LEVEL = 1
    const val STALL_XP = 2
    const val STALL_EMPTY = 3
    const val STALL_RESPAWN = 4
    const val STALL_OWNERS = 5
    const val STALL_GUARDS = 6
    const val STALL_ATTEMPT_MESSAGE = 7

    const val POUCH_OBJ = 0
    const val POUCH_COINS_MIN = 1
    const val POUCH_COINS_MAX = 2

    private fun rowName(displayName: String): String =
        "dbrow.thieving_" +
            displayName
                .lowercase()
                .map { if (it.isLetterOrDigit()) it else '_' }
                .joinToString("")
                .replace(Regex("_+"), "_")
                .trim('_')

    fun pickpockets() =
        dbTable("dbtable.thieving_pickpocket", serverOnly = true) {
            column("name", PP_NAME, VarType.STRING)
            column("level", PP_LEVEL, VarType.INT)
            column("xp", PP_XP, VarType.INT)
            column("low", PP_LOW, VarType.INT)
            column("high", PP_HIGH, VarType.INT)
            column("stun_ticks", PP_STUN_TICKS, VarType.INT)
            column("stun_damage", PP_STUN_DAMAGE, VarType.INT)
            column("npcs", PP_NPCS, VarType.NPC)
            column("pouch", PP_POUCH, VarType.OBJ)
            column("caught_shout", PP_CAUGHT_SHOUT, VarType.STRING)
            column("lowercase_name", PP_LOWERCASE_NAME, VarType.BOOLEAN)
            column("op", PP_OP, VarType.INT)

            for (target in pickpocketTargets) {
                row(rowName(target.displayName)) {
                    column(PP_NAME, target.displayName)
                    column(PP_LEVEL, target.level)
                    column(PP_XP, (target.xp * 10).toInt())
                    column(PP_LOW, target.low)
                    column(PP_HIGH, target.high)
                    column(PP_STUN_TICKS, target.stunTicks)
                    column(PP_STUN_DAMAGE, target.stunDamage)
                    if (target.npcs.isNotEmpty()) {
                        columnRSCM(PP_NPCS, *target.npcs.toTypedArray())
                    }
                    target.pouch?.let { columnRSCM(PP_POUCH, it) }
                    column(PP_CAUGHT_SHOUT, target.caughtShout)
                    column(PP_LOWERCASE_NAME, target.lowercaseName)
                    column(PP_OP, target.op)
                }
            }
        }

    fun stalls() =
        dbTable("dbtable.thieving_stall", serverOnly = true) {
            column("loc", STALL_LOC, VarType.LOC)
            column("level", STALL_LEVEL, VarType.INT)
            column("xp", STALL_XP, VarType.INT)
            column("empty", STALL_EMPTY, VarType.LOC)
            column("respawn", STALL_RESPAWN, VarType.INT)
            column("owners", STALL_OWNERS, VarType.NPC)
            column("guards", STALL_GUARDS, VarType.NPC)
            column("attempt_message", STALL_ATTEMPT_MESSAGE, VarType.STRING)

            for (stall in stallTargets) {
                row("dbrow." + stall.loc.removePrefix("loc.") + "_thieving") {
                    columnRSCM(STALL_LOC, stall.loc)
                    column(STALL_LEVEL, stall.level)
                    column(STALL_XP, (stall.xp * 10).toInt())
                    stall.empty?.let { columnRSCM(STALL_EMPTY, it) }
                    column(STALL_RESPAWN, stall.respawn)
                    if (stall.owners.isNotEmpty()) {
                        columnRSCM(STALL_OWNERS, *stall.owners.toTypedArray())
                    }
                    if (stall.guards.isNotEmpty()) {
                        columnRSCM(STALL_GUARDS, *stall.guards.toTypedArray())
                    }
                    stall.attemptMessage?.let { column(STALL_ATTEMPT_MESSAGE, it) }
                }
            }
        }

    fun coinPouches() =
        dbTable("dbtable.thieving_coin_pouch", serverOnly = true) {
            column("obj", POUCH_OBJ, VarType.OBJ)
            column("coins_min", POUCH_COINS_MIN, VarType.INT)
            column("coins_max", POUCH_COINS_MAX, VarType.INT)

            for (pouch in coinPouches) {
                row("dbrow.thieving_" + pouch.obj.removePrefix("obj.pickpocket_")) {
                    columnRSCM(POUCH_OBJ, pouch.obj)
                    column(POUCH_COINS_MIN, pouch.coins.first)
                    column(POUCH_COINS_MAX, pouch.coins.last)
                }
            }
        }

    internal val pickpocketTargets: List<PickpocketTarget> =
        listOf(
            PickpocketTarget(
                "Man",
                1,
                8.0,
                180,
                240,
                8,
                1,
                pouch = "obj.pickpocket_coin_pouch_citizen",
                npcs =
                    listOf(
                        "npc.varrock_man1",
                        "npc.man",
                        "npc.man2",
                        "npc.man3",
                        "npc.man4",
                        "npc.man4_for_musa_point",
                        "npc.man5",
                        "npc.man_indoor",
                        "npc.al_kharid_man",
                        "npc.falador_man1",
                        "npc.falador_man2",
                        "npc.falador_man3",
                        "npc.falador_doric_area_man1",
                        "npc.falador_doric_area_man2",
                        "npc.falador_doric_area_man3",
                        "npc.falador_doric_area_man4",
                        "npc.rimmington_hengel",
                        "npc.ardougnian_male1",
                        "npc.karamja_man",
                        "npc.death_man_indoors1",
                        "npc.zeah_man",
                        "npc.zeah_man2",
                        "npc.zeah_man3",
                        "npc.shayzien_man_1",
                        "npc.shayzien_man_2",
                    ),
            ),
            PickpocketTarget(
                "Woman",
                1,
                8.0,
                180,
                240,
                8,
                1,
                pouch = "obj.pickpocket_coin_pouch_citizen",
                npcs =
                    listOf(
                        "npc.varrock_woman1",
                        "npc.woman",
                        "npc.woman2",
                        "npc.woman3",
                        "npc.woman4",
                        "npc.falador_woman2",
                        "npc.rimmington_anja",
                        "npc.ardougnian_female1",
                        "npc.zeah_woman",
                        "npc.zeah_woman2",
                        "npc.zeah_woman3",
                        "npc.zeah_woman_outside",
                        "npc.shayzien_woman_1",
                        "npc.shayzien_woman_2",
                    ),
            ),
            PickpocketTarget(
                "Citizen",
                1,
                8.0,
                180,
                240,
                8,
                1,
                pouch = "obj.pickpocket_coin_pouch_citizen",
                npcs =
                    listOf(
                        "npc.vmq2_citizen_vis",
                        "npc.varlamore_citizen_poor_m_1",
                        "npc.varlamore_citizen_poor_m_2",
                        "npc.varlamore_citizen_poor_m_3",
                        "npc.varlamore_citizen_poor_m_4",
                        "npc.varlamore_citizen_poor_m_5",
                        "npc.varlamore_citizen_poor_f_1",
                        "npc.varlamore_citizen_poor_f_2",
                        "npc.varlamore_citizen_poor_f_3",
                        "npc.varlamore_citizen_poor_f_4",
                        "npc.varlamore_citizen_poor_f_5",
                        "npc.varlamore_citizen_normal_m_1",
                        "npc.varlamore_citizen_normal_m_2",
                        "npc.varlamore_citizen_normal_m_3",
                        "npc.varlamore_citizen_normal_m_4",
                        "npc.varlamore_citizen_normal_m_5",
                        "npc.varlamore_citizen_normal_f_1",
                        "npc.varlamore_citizen_normal_f_2",
                        "npc.varlamore_citizen_normal_f_3",
                        "npc.varlamore_citizen_normal_f_4",
                        "npc.varlamore_citizen_normal_f_5",
                        "npc.varlamore_citizen_rich_m_1",
                        "npc.varlamore_citizen_rich_m_2",
                        "npc.varlamore_citizen_rich_m_3",
                        "npc.varlamore_citizen_rich_m_4",
                        "npc.varlamore_citizen_rich_m_5",
                        "npc.varlamore_citizen_rich_f_1",
                        "npc.varlamore_citizen_rich_f_2",
                        "npc.varlamore_citizen_rich_f_3",
                        "npc.varlamore_citizen_rich_f_4",
                        "npc.varlamore_citizen_rich_f_5",
                        "npc.aldarin_citizen_poor_m_1",
                        "npc.aldarin_citizen_poor_m_2",
                        "npc.aldarin_citizen_poor_m_3",
                        "npc.aldarin_citizen_poor_f_1",
                        "npc.aldarin_citizen_poor_f_2",
                        "npc.aldarin_citizen_poor_f_3",
                        "npc.aldarin_citizen_normal_m_1",
                        "npc.aldarin_citizen_normal_m_2",
                        "npc.aldarin_citizen_normal_m_3",
                        "npc.aldarin_citizen_normal_m_4",
                        "npc.aldarin_citizen_normal_f_1",
                        "npc.aldarin_citizen_normal_f_2",
                        "npc.aldarin_citizen_normal_f_3",
                        "npc.aldarin_citizen_normal_f_4",
                        "npc.aldarin_citizen_normal_f_5",
                        "npc.aldarin_citizen_rich_m_1",
                        "npc.aldarin_citizen_rich_m_2",
                        "npc.aldarin_citizen_rich_f_1",
                        "npc.aldarin_citizen_rich_f_2",
                        "npc.auburn_citizen_normal_m_1",
                        "npc.auburn_citizen_normal_m_2",
                        "npc.auburn_citizen_normal_m_3",
                        "npc.auburn_citizen_normal_f_1",
                        "npc.auburn_citizen_normal_f_2",
                        "npc.auburn_citizen_normal_f_3",
                        "npc.auburn_citizen_rich_m_1",
                        "npc.auburn_citizen_rich_f_1",
                        "npc.kastori_citizen_normal_m_1",
                        "npc.kastori_citizen_normal_m_2",
                        "npc.kastori_citizen_normal_m_3",
                        "npc.kastori_citizen_normal_f_1",
                        "npc.kastori_citizen_normal_f_2",
                        "npc.kastori_citizen_normal_f_3",
                        "npc.kastori_citizen_rich_m_1",
                        "npc.kastori_citizen_rich_f_1",
                        "npc.tal_teklan_citizen_normal_m_1",
                        "npc.tal_teklan_citizen_normal_m_2",
                        "npc.tal_teklan_citizen_normal_m_3",
                        "npc.tal_teklan_citizen_normal_f_1",
                        "npc.tal_teklan_citizen_normal_f_2",
                        "npc.tal_teklan_citizen_normal_f_3",
                        "npc.tal_teklan_citizen_rich_m_1",
                        "npc.tal_teklan_citizen_rich_f_1",
                    ),
            ),
            PickpocketTarget(
                displayName = "Farmer",
                level = 10,
                xp = 14.5,
                low = 150,
                high = 240,
                stunTicks = 8,
                stunDamage = 1,
                pouch = "obj.pickpocket_coin_pouch_farmer",
                npcs =
                    listOf(
                        "npc.farmer1",
                        "npc.farmer2",
                        "npc.farmer3",
                        "npc.farmer4",
                        "npc.farmer1_f",
                        "npc.farmer2_f",
                        "npc.farmer3_f",
                        "npc.varlamore_farmer_m_1",
                        "npc.varlamore_farmer_m_2",
                        "npc.varlamore_farmer_m_3",
                        "npc.varlamore_farmer_m_4",
                        "npc.varlamore_farmer_f_1",
                        "npc.varlamore_farmer_f_2",
                        "npc.varlamore_farmer_f_3",
                        "npc.varlamore_farmer_f_4",
                        "npc.kastori_farmer_m_1",
                        "npc.kastori_farmer_m_2",
                        "npc.kastori_farmer_f_1",
                        "npc.kastori_farmer_f_2",
                        "npc.tal_teklan_farmer",
                    ),
            ),
            hamMember("Male H.A.M. Member"),
            hamMember("Female H.A.M. Member"),
            hamMember(
                "H.A.M. Member",
                listOf("npc.favour_male_ham_civilian", "npc.favour_female_ham_civilian"),
            ),
            PickpocketTarget(
                "Warrior woman",
                25,
                26.0,
                100,
                240,
                8,
                2,
                pouch = "obj.pickpocket_coin_pouch_warrior",
            ),
            PickpocketTarget(
                "Warrior",
                25,
                26.0,
                100,
                240,
                8,
                2,
                pouch = "obj.pickpocket_coin_pouch_warrior",
                npcs =
                    listOf(
                        "npc.warrior_man",
                        "npc.warrior_man_variant01",
                        "npc.warrior_man_variant02",
                        "npc.warrior_woman",
                        "npc.warrior_woman_variant01",
                        "npc.warrior_woman_variant02",
                        "npc.al_kharid_warrior",
                    ),
            ),
            PickpocketTarget(
                displayName = "Workman",
                level = 25,
                xp = 10.4,
                low = 150,
                high = 240,
                stunTicks = 7,
                stunDamage = 1,
            ),
            PickpocketTarget(
                "Villager",
                30,
                8.0,
                100,
                240,
                8,
                2,
                npcs =
                    listOf(
                        "npc.feud_villager_1_1",
                        "npc.feud_villager_1_2",
                        "npc.feud_villager_1_3",
                        "npc.feud_villager_2_1",
                        "npc.feud_villager_2_2",
                        "npc.feud_villager_2_3",
                        "npc.feud_villager_3_1",
                        "npc.feud_villager_3_2",
                        "npc.feud_villager_3_3",
                    ),
            ),
            PickpocketTarget(
                displayName = "Rogue",
                level = 32,
                xp = 36.5,
                low = 75,
                high = 240,
                stunTicks = 8,
                stunDamage = 2,
                pouch = "obj.pickpocket_coin_pouch_rogue",
                npcs = listOf("npc.rogue"),
            ),
            PickpocketTarget(
                displayName = "Cave goblin",
                level = 36,
                xp = 40.0,
                low = 150,
                high = 240,
                stunTicks = 7,
                stunDamage = 1,
                pouch = "obj.pickpocket_coin_pouch_cavegoblin",
                npcs =
                    listOf(
                        "npc.dorgesh_male_1",
                        "npc.dorgesh_male_2",
                        "npc.dorgesh_male_3",
                        "npc.dorgesh_male_4",
                        "npc.dorgesh_male_5",
                        "npc.dorgesh_male_6",
                        "npc.dorgesh_male_7",
                        "npc.dorgesh_male_8",
                        "npc.dorgesh_male_9",
                        "npc.dorgesh_female_1",
                        "npc.dorgesh_female_2",
                        "npc.dorgesh_female_3",
                        "npc.dorgesh_female_4",
                        "npc.dorgesh_female_5",
                        "npc.dorgesh_female_6",
                        "npc.dorgesh_female_7",
                        "npc.dorgesh_female_8",
                        "npc.dorgesh_female_9",
                    ),
            ),
            PickpocketTarget(
                displayName = "Master Farmer",
                level = 38,
                xp = 43.0,
                low = 90,
                high = 240,
                stunTicks = 8,
                stunDamage = 3,
                lowercaseName = false,
                caughtShout = "Cor blimey mate, what are ye doing in me pockets?",
                npcs =
                    listOf(
                        "npc.master_farmer_1",
                        "npc.master_farmer_2",
                        "npc.master_farmer_1_f",
                        "npc.master_farmer_2_f",
                        "npc.martin_the_master_farmer",
                        "npc.varlamore_master_farmer_m_1",
                        "npc.varlamore_master_farmer_m_2",
                        "npc.varlamore_master_farmer_m_3",
                        "npc.varlamore_master_farmer_m_4",
                        "npc.varlamore_master_farmer_f_1",
                        "npc.varlamore_master_farmer_f_2",
                        "npc.varlamore_master_farmer_f_3",
                        "npc.varlamore_master_farmer_f_4",
                        "npc.kastori_master_farmer_m_1",
                        "npc.kastori_master_farmer_m_2",
                        "npc.kastori_master_farmer_f_1",
                        "npc.kastori_master_farmer_f_2",
                    ),
            ),
            PickpocketTarget(
                "Guard",
                40,
                46.8,
                50,
                240,
                8,
                2,
                pouch = "obj.pickpocket_coin_pouch_guard",
                npcs =
                    listOf(
                        "npc.guard1",
                        "npc.guard1_variant01",
                        "npc.guard1_f",
                        "npc.guard1_f_variant01",
                        "npc.fai_varrock_guard",
                        "npc.fai_varrock_guard_captain",
                        "npc.fai_varrock_guard02",
                        "npc.fai_varrock_guard02_variant01",
                        "npc.fai_varrock_guard02_variant02",
                        "npc.fai_varrock_guard02_f",
                        "npc.fai_varrock_guard02_f_variant01",
                        "npc.fai_varrock_guard02_f_variant02",
                        "npc.fai_varrock_guard_captain02",
                        "npc.fai_falador_guard1",
                        "npc.fai_falador_guard1_variant01",
                        "npc.fai_falador_guard1_variant02",
                        "npc.fai_falador_guard1_f",
                        "npc.fai_falador_guard2",
                        "npc.fai_falador_guard2_f",
                        "npc.fai_falador_guard3",
                        "npc.fai_falador_guard3_f",
                        "npc.fai_falador_guard4",
                        "npc.fai_falador_guard4_f",
                        "npc.fai_falador_guard5",
                        "npc.fai_falador_guard6",
                        "npc.falador_doric_area_guard",
                        "npc.ardougne_guard",
                        "npc.ardougne_guard_variant01",
                        "npc.ardougne_guard_f",
                        "npc.ardougne_guard_f_variant01",
                        "npc.jail_guard_1",
                        "npc.jail_guard_2",
                        "npc.jail_guard_3",
                        "npc.jail_guard_4",
                        "npc.jail_guard_5",
                        "npc.hos_town_guard_01",
                        "npc.hos_town_guard_02",
                        "npc.hos_town_guard_03",
                        "npc.hos_town_guard_04",
                        "npc.kourend_guard_m1",
                        "npc.kourend_guard_m1_big",
                        "npc.kourend_guard_m2",
                        "npc.kourend_guard_m2_big",
                        "npc.kourend_guard_m3",
                        "npc.kourend_guard_m3_big",
                        "npc.kourend_guard_m4",
                        "npc.kourend_guard_m4_big",
                        "npc.kourend_guard_f1",
                        "npc.kourend_guard_f1_big",
                        "npc.kourend_guard_f2",
                        "npc.kourend_guard_f2_big",
                        "npc.kourend_guard_f3",
                        "npc.kourend_guard_f3_big",
                        "npc.kourend_guard_f4",
                        "npc.kourend_guard_f4_big",
                        "npc.varlamore_guard_m_1",
                        "npc.varlamore_guard_m_2",
                        "npc.varlamore_guard_m_3",
                        "npc.varlamore_guard_m_4",
                        "npc.varlamore_guard_m_5",
                        "npc.varlamore_guard_f_1",
                        "npc.varlamore_guard_f_2",
                        "npc.varlamore_guard_f_3",
                        "npc.varlamore_guard_f_4",
                        "npc.varlamore_guard_f_5",
                        "npc.aldarin_guard_m_1",
                        "npc.aldarin_guard_m_2",
                        "npc.aldarin_guard_m_3",
                        "npc.aldarin_guard_m_4",
                        "npc.aldarin_guard_m_5",
                        "npc.aldarin_guard_f_1",
                        "npc.aldarin_guard_f_2",
                        "npc.aldarin_guard_f_3",
                        "npc.aldarin_guard_f_4",
                        "npc.aldarin_guard_f_5",
                        "npc.auburnvale_guard_m_1",
                        "npc.auburnvale_guard_m_2",
                        "npc.auburnvale_guard_m_3",
                        "npc.auburnvale_guard_m_4",
                        "npc.auburnvale_guard_f_1",
                        "npc.auburnvale_guard_f_2",
                        "npc.auburnvale_guard_f_3",
                        "npc.auburnvale_guard_f_4",
                        "npc.tlati_guard_m_1",
                        "npc.tlati_guard_m_2",
                        "npc.tlati_guard_m_3",
                        "npc.tlati_guard_m_4",
                        "npc.tlati_guard_f_1",
                        "npc.tlati_guard_f_2",
                        "npc.tlati_guard_f_3",
                        "npc.tlati_guard_f_4",
                        "npc.port_roberts_guard_m1",
                        "npc.port_roberts_guard_m2",
                        "npc.port_roberts_guard_f1",
                        "npc.port_roberts_guard_f2",
                    ),
            ),
            PickpocketTarget(
                "Fremennik citizen",
                45,
                65.0,
                50,
                240,
                8,
                2,
                pouch = "obj.pickpocket_coin_pouch_fremennik",
                npcs =
                    listOf(
                        "npc.viking_man",
                        "npc.viking_man2",
                        "npc.viking_man3",
                        "npc.viking_man4",
                        "npc.viking_man5",
                        "npc.viking_woman2",
                        "npc.viking_woman3",
                        "npc.viking_woman4",
                        "npc.viking_woman_indoors",
                    ),
            ),
            PickpocketTarget(
                "Bearded Pollnivnian Bandit",
                45,
                65.0,
                50,
                240,
                8,
                5,
                pouch = "obj.pickpocket_coin_pouch_bandit2",
                lowercaseName = false,
                npcs = listOf("npc.feud_arabian_guard2_1", "npc.feud_arabian_guard2_2"),
            ),
            PickpocketTarget(
                "Wealthy citizen",
                50,
                96.0,
                35,
                200,
                7,
                3,
                pouch = "obj.pickpocket_coin_pouch_varlamore_wealthy",
                npcs =
                    listOf(
                        "npc.varlamore_wealthy_citizen_a",
                        "npc.varlamore_wealthy_citizen_b",
                        "npc.varlamore_wealthy_citizen_c",
                        "npc.varlamore_wealthy_citizen_d",
                    ),
                op = 1,
            ),
            PickpocketTarget(
                displayName = "Desert Bandit",
                level = 53,
                xp = 79.4,
                low = 50,
                high = 240,
                stunTicks = 8,
                stunDamage = 3,
                pouch = "obj.pickpocket_coin_pouch_desertbandit",
                lowercaseName = false,
                npcs =
                    listOf("npc.fourdiamonds_sword_bandit_1", "npc.fourdiamonds_sword_bandit_free"),
            ),
            PickpocketTarget(
                "Knight of Ardougne",
                55,
                84.3,
                50,
                240,
                8,
                3,
                pouch = "obj.pickpocket_coin_pouch_knight",
                lowercaseName = false,
                npcs =
                    listOf(
                        "npc.knight_of_ardougne",
                        "npc.knight_of_ardougne2",
                        "npc.knight_of_ardougne_f",
                        "npc.knight_of_ardougne_west_vis",
                        "npc.knight_of_ardougne_f_west_vis",
                    ),
            ),
            PickpocketTarget(
                "Knight of Varlamore",
                55,
                84.3,
                50,
                240,
                8,
                3,
                pouch = "obj.pickpocket_coin_pouch_knight",
                lowercaseName = false,
                npcs =
                    listOf(
                        "npc.varlamore_knight_m_1",
                        "npc.varlamore_knight_m_2",
                        "npc.varlamore_knight_m_3",
                        "npc.varlamore_knight_f_1",
                        "npc.varlamore_knight_f_2",
                        "npc.varlamore_knight_f_3",
                    ),
            ),
            PickpocketTarget(
                "Pollnivnian Bandit",
                55,
                84.3,
                50,
                240,
                8,
                5,
                pouch = "obj.pickpocket_coin_pouch_bandit",
                lowercaseName = false,
                npcs = listOf("npc.feud_arabian_guard1_1", "npc.feud_arabian_guard1_2"),
            ),
            PickpocketTarget(
                displayName = "Watchman",
                level = 65,
                xp = 137.5,
                low = 15,
                high = 160,
                stunTicks = 8,
                stunDamage = 3,
                pouch = "obj.pickpocket_coin_pouch_watchman",
                npcs = listOf("npc.yanille_watchman"),
            ),
            PickpocketTarget(
                "Menaphite Thug",
                65,
                137.5,
                50,
                160,
                8,
                5,
                pouch = "obj.pickpocket_coin_pouch_menaphite",
                lowercaseName = false,
                npcs = listOf("npc.feud_egyptian_doorman_2"),
            ),
            PickpocketTarget(
                displayName = "Paladin",
                level = 70,
                xp = 131.8,
                low = 35,
                high = 160,
                stunTicks = 8,
                stunDamage = 3,
                pouch = "obj.pickpocket_coin_pouch_paladin",
                npcs =
                    listOf(
                        "npc.paladin",
                        "npc.paladin_variant01",
                        "npc.paladin_variant02",
                        "npc.paladin2",
                        "npc.paladin_f",
                        "npc.paladin_f_variant01",
                        "npc.paladin_west_vis",
                        "npc.paladin_west_f_vis",
                    ),
            ),
            PickpocketTarget(
                displayName = "Gnome",
                level = 75,
                xp = 133.3,
                low = 33,
                high = 140,
                stunTicks = 8,
                stunDamage = 1,
                pouch = "obj.pickpocket_coin_pouch_gnome",
                npcs =
                    listOf(
                        "npc.gnome",
                        "npc.browclothedgnome",
                        "npc.darkskinned_gnome",
                        "npc.gnomefemale",
                        "npc.gnomefemale_dskinned",
                        "npc.gnomechild",
                        "npc.gnomechildgreen",
                        "npc.gnomechildblue",
                        "npc.grim_gnome_incage_1",
                    ),
            ),
            PickpocketTarget(
                displayName = "Hero",
                level = 80,
                xp = 163.3,
                low = 39,
                high = 160,
                stunTicks = 10,
                stunDamage = 3,
                pouch = "obj.pickpocket_coin_pouch_hero",
                npcs = listOf("npc.hero", "npc.hero_variant01", "npc.hero_f"),
            ),
            PickpocketTarget(
                displayName = "Vyre",
                level = 82,
                xp = 306.9,
                low = 8,
                high = 128,
                stunTicks = 10,
                stunDamage = 5,
                npcs =
                    listOf(
                        "npc.alek_constantine",
                        "npc.caninelle_draynar",
                        "npc.carnivus_belamorta",
                        "npc.crimsonette_van_marr",
                        "npc.diphylla_bechstein",
                        "npc.draconis_sanguine",
                        "npc.episcula_helsing",
                        "npc.grigor_rasputin",
                        "npc.haemas_lamescus",
                        "npc.lasenna_rasputin",
                        "npc.misdrievus_shadum",
                        "npc.mort_nightshade",
                        "npc.mortina_daubenton",
                        "npc.nakasa_jovkai",
                        "npc.natalidae_shadum",
                        "npc.noctillion_lugosi",
                        "npc.pipistrelle_draynar",
                        "npc.remus_kaninus",
                        "npc.valentin_rasputin",
                        "npc.valentina_diaemus",
                        "npc.vallessia_dracyula",
                        "npc.vallessia_von_pitt",
                        "npc.vampyressa_van_von",
                        "npc.vampyrus_diaemus",
                        "npc.violetta_sanguine",
                        "npc.vlad_bechstein",
                        "npc.vlad_diaemus",
                        "npc.von_van_von",
                        "npc.vonnetta_varnis",
                        "npc.vormar_vakan",
                    ),
                pouch = "obj.pickpocket_coin_pouch_vyre",
                lowercaseName = false,
            ),
            PickpocketTarget(
                displayName = "Elf",
                level = 85,
                xp = 353.3,
                low = 6,
                high = 100,
                stunTicks = 10,
                stunDamage = 5,
                npcs =
                    listOf(
                        "npc.prif_citizen_anaire",
                        "npc.prif_citizen_aranwe",
                        "npc.prif_citizen_aredhel",
                        "npc.prif_citizen_caranthir",
                        "npc.prif_citizen_celebrian",
                        "npc.prif_citizen_celegorm",
                        "npc.prif_citizen_cirdan",
                        "npc.prif_citizen_curufin",
                        "npc.prif_citizen_earwen",
                        "npc.prif_citizen_edrahil",
                        "npc.prif_citizen_elenwe",
                        "npc.prif_citizen_elladan",
                        "npc.prif_citizen_enel",
                        "npc.prif_citizen_enelye",
                        "npc.prif_citizen_enerdhil",
                        "npc.prif_citizen_erestor",
                        "npc.prif_citizen_feanor",
                        "npc.prif_citizen_findis",
                        "npc.prif_citizen_finduilas",
                        "npc.prif_citizen_fingolfin",
                        "npc.prif_citizen_fingon",
                        "npc.prif_citizen_galathil",
                        "npc.prif_citizen_gelmir",
                        "npc.prif_citizen_glorfindel",
                        "npc.prif_citizen_guilin",
                        "npc.prif_citizen_hendor",
                        "npc.prif_citizen_idril",
                        "npc.prif_citizen_imin",
                        "npc.prif_citizen_iminye",
                        "npc.prif_citizen_indis",
                        "npc.prif_citizen_ingwe",
                        "npc.prif_citizen_ingwion",
                        "npc.prif_citizen_lenwe",
                        "npc.prif_citizen_lindir",
                        "npc.prif_citizen_maeglin",
                        "npc.prif_citizen_mahtan",
                        "npc.prif_citizen_miriel",
                        "npc.prif_citizen_mithrellas",
                        "npc.prif_citizen_nellas",
                        "npc.prif_citizen_nerdanel",
                        "npc.prif_citizen_nimloth",
                        "npc.prif_citizen_oropher",
                        "npc.prif_citizen_orophin",
                        "npc.prif_citizen_saeros",
                        "npc.prif_citizen_salgant",
                        "npc.prif_citizen_tatie",
                        "npc.prif_citizen_thingol",
                        "npc.prif_citizen_turgon",
                        "npc.prif_citizen_vaire",
                    ),
                pouch = "obj.pickpocket_coin_pouch_elf",
                lowercaseName = false,
            ),
            PickpocketTarget(
                displayName = "TzHaar-Hur",
                level = 90,
                xp = 103.4,
                low = -200,
                high = 200,
                stunTicks = 10,
                stunDamage = 4,
                lowercaseName = false,
                npcs =
                    listOf(
                        "npc.tzhaar_hur_city1",
                        "npc.tzhaar_hur_city2",
                        "npc.tzhaar_hur_city3",
                        "npc.tzhaar_hur_city4",
                        "npc.tzhaar_hur_city5",
                        "npc.tzhaar_hur_city6",
                    ),
            ),
        )

    internal val stallTargets: List<StallTarget> =
        listOf(
            StallTarget(
                loc = "loc.cakethiefstall",
                level = 5,
                xp = 16.0,
                empty = "loc.bakerymarket",
                respawn = 4,
                owners = BAKERS,
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.tea_stall",
                level = 5,
                xp = 16.0,
                respawn = 4,
                owners = listOf("npc.tea_seller"),
            ),
            StallTarget(
                loc = "loc.silkthiefstall",
                level = 20,
                xp = 24.0,
                empty = "loc.market",
                respawn = 8,
                owners = listOf("npc.silk_merchant_ardougne", "npc.silk_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.rag_market_stall",
                level = 22,
                xp = 27.0,
                empty = "loc.rag_market_stall_empty",
                respawn = 8,
                owners = listOf("npc.rag_wine_merchant"),
                guards = DRAYNOR_MARKET_GUARDS,
                attemptMessage = "You attempt to steal something from the wine merchant's stall.",
            ),
            StallTarget(
                loc = "loc.seed_stall",
                level = 27,
                xp = 10.0,
                respawn = 5,
                owners = listOf("npc.seed_merchant"),
                guards = DRAYNOR_MARKET_GUARDS,
                attemptMessage = "You attempt to steal some seeds from the seed merchant's stall.",
            ),
            StallTarget(
                loc = "loc.furthiefstall",
                level = 35,
                xp = 45.0,
                empty = "loc.furmarket",
                respawn = 12,
                owners = listOf("npc.fur_merchant_ardougne", "npc.fur_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.silverthiefstall",
                level = 50,
                xp = 205.0,
                empty = "loc.market",
                respawn = 32,
                owners = listOf("npc.silver_merchant_ardougne"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.spicethiefstall",
                level = 65,
                xp = 92.0,
                empty = "loc.spicemarket",
                respawn = 10,
                owners = listOf("npc.spice_merchant_ardougne", "npc.spice_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.gemthiefstall",
                level = 75,
                xp = 408.0,
                empty = "loc.gemmarket",
                respawn = 100,
                owners = listOf("npc.gem_merchant_ardougne", "npc.gem_merchant"),
                guards = ARDOUGNE_MARKET_GUARDS,
            ),
            StallTarget(
                loc = "loc.prif_marketstall_silk",
                level = 20,
                xp = 24.0,
                empty = "loc.prif_marketstall_empty",
                respawn = 8,
                owners = listOf("npc.prif_silk"),
                guards = PRIFDDINAS_GUARDS,
            ),
            StallTarget(
                loc = "loc.prif_marketstall_silver",
                level = 50,
                xp = 205.0,
                empty = "loc.prif_marketstall_empty",
                respawn = 32,
                owners = listOf("npc.prif_silver"),
                guards = PRIFDDINAS_GUARDS,
            ),
            StallTarget(
                loc = "loc.prif_marketstall_spice",
                level = 65,
                xp = 92.0,
                empty = "loc.prif_marketstall_empty",
                respawn = 10,
                owners = listOf("npc.prif_spice"),
                guards = PRIFDDINAS_GUARDS,
            ),
            StallTarget(
                loc = "loc.prif_marketstall_gem",
                level = 75,
                xp = 408.0,
                empty = "loc.prif_marketstall_empty",
                respawn = 100,
                owners = listOf("npc.prif_gem"),
                guards = PRIFDDINAS_GUARDS,
            ),
            StallTarget(
                loc = "loc.viking_fish_market",
                level = 42,
                xp = 42.0,
                empty = "loc.viking_market",
                respawn = 12,
                owners = listOf("npc.viking_fish_monger"),
                guards = listOf("npc.viking_guard"),
            ),
            StallTarget(
                loc = "loc.viking_fur_market",
                level = 35,
                xp = 45.0,
                empty = "loc.viking_market",
                respawn = 12,
                owners = listOf("npc.viking_fur_monger"),
                guards = listOf("npc.viking_guard"),
            ),
            StallTarget(
                loc = "loc.misc_fish_market",
                level = 42,
                xp = 42.0,
                respawn = 12,
                owners = listOf("npc.misc_fish_monger"),
                guards = listOf("npc.royal_misc_guard"),
            ),
            StallTarget(
                loc = "loc.misc_veg_market",
                level = 2,
                xp = 10.0,
                respawn = 2,
                owners = listOf("npc.misc_veg_monger"),
                guards = listOf("npc.royal_misc_guard"),
            ),
            StallTarget(
                loc = "loc.etc_fish_market",
                level = 42,
                xp = 42.0,
                respawn = 12,
                owners = listOf("npc.etc_fish_monger"),
                guards = ETCETERIA_GUARDS,
            ),
            StallTarget(
                loc = "loc.etc_veg_market",
                level = 2,
                xp = 10.0,
                respawn = 2,
                owners = listOf("npc.etc_veg_monger"),
                guards = ETCETERIA_GUARDS,
            ),
            StallTarget(
                loc = "loc.dwarf_market_bakery",
                level = 5,
                xp = 16.0,
                empty = "loc.dwarf_market_empty_stall",
                respawn = 16,
            ),
            StallTarget(
                loc = "loc.dwarf_market_crafting",
                level = 5,
                xp = 20.0,
                empty = "loc.dwarf_market_empty_stall",
                respawn = 8,
            ),
            StallTarget(
                loc = "loc.xbows_dwarf_market",
                level = 49,
                xp = 52.0,
                respawn = 8,
            ),
            StallTarget(
                loc = "loc.dwarf_market_silver",
                level = 50,
                xp = 205.0,
                empty = "loc.dwarf_market_empty_stall",
                respawn = 32,
            ),
            StallTarget(
                loc = "loc.dwarf_market_gems",
                level = 75,
                xp = 408.0,
                empty = "loc.dwarf_market_empty_stall",
                respawn = 100,
            ),
            StallTarget(
                loc = "loc.hos_stall_bread",
                level = 5,
                xp = 16.0,
                empty = "loc.hos_stall_empty",
                respawn = 4,
            ),
            StallTarget(
                loc = "loc.hos_fruit_stall_02",
                level = 25,
                xp = 28.5,
                empty = "loc.hos_fruit_stall",
                respawn = 4,
                guards = listOf("npc.hosidius_guarddog"),
            ),
            StallTarget(
                loc = "loc.fish_stall_warrens",
                level = 42,
                xp = 42.0,
                respawn = 12,
                owners = listOf("npc.warrens_fishmonger"),
                guards = listOf("npc.warrens_thief_stall"),
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_bakers",
                level = 5,
                xp = 16.0,
                empty = "loc.fortis_market_stall",
                respawn = 4,
                owners = listOf("npc.fortis_shop_baker"),
                guards = FORTIS_GUARDS,
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_silk",
                level = 20,
                xp = 24.0,
                empty = "loc.fortis_market_stall",
                respawn = 8,
                owners = listOf("npc.fortis_shop_silk"),
                guards = FORTIS_GUARDS,
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_fur",
                level = 35,
                xp = 45.0,
                empty = "loc.fortis_market_stall",
                respawn = 12,
                owners = listOf("npc.fortis_shop_fur"),
                guards = FORTIS_GUARDS,
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_spice",
                level = 65,
                xp = 92.0,
                empty = "loc.fortis_market_stall",
                respawn = 10,
                owners = listOf("npc.fortis_shop_spices"),
                guards = FORTIS_GUARDS,
            ),
            StallTarget(
                loc = "loc.fortis_market_stall_gems",
                level = 75,
                xp = 408.0,
                empty = "loc.fortis_market_stall",
                respawn = 100,
                owners = listOf("npc.fortis_shop_gems"),
                guards = FORTIS_GUARDS,
            ),
        )

    internal val coinPouches: List<CoinPouch> =
        listOf(
            CoinPouch("obj.pickpocket_coin_pouch_citizen", 3..3),
            CoinPouch("obj.pickpocket_coin_pouch_farmer", 9..9),
            CoinPouch("obj.pickpocket_coin_pouch_ham", 1..21),
            CoinPouch("obj.pickpocket_coin_pouch_warrior", 18..18),
            CoinPouch("obj.pickpocket_coin_pouch_rogue", 25..40),
            CoinPouch("obj.pickpocket_coin_pouch_cavegoblin", 10..50),
            CoinPouch("obj.pickpocket_coin_pouch_guard", 30..30),
            CoinPouch("obj.pickpocket_coin_pouch_fremennik", 40..40),
            CoinPouch("obj.pickpocket_coin_pouch_bandit2", 40..40),
            CoinPouch("obj.pickpocket_coin_pouch_varlamore_wealthy", 85..85),
            CoinPouch("obj.pickpocket_coin_pouch_desertbandit", 30..30),
            CoinPouch("obj.pickpocket_coin_pouch_knight", 50..50),
            CoinPouch("obj.pickpocket_coin_pouch_bandit", 50..50),
            CoinPouch("obj.pickpocket_coin_pouch_watchman", 60..60),
            CoinPouch("obj.pickpocket_coin_pouch_menaphite", 60..60),
            CoinPouch("obj.pickpocket_coin_pouch_paladin", 80..80),
            CoinPouch("obj.pickpocket_coin_pouch_gnome", 300..300),
            CoinPouch("obj.pickpocket_coin_pouch_hero", 200..300),
            CoinPouch("obj.pickpocket_coin_pouch_vyre", 230..315),
            CoinPouch("obj.pickpocket_coin_pouch_elf", 280..350),
        )
}

private fun hamMember(displayName: String, npcs: List<String> = emptyList()) =
    PickpocketTarget(
        displayName = displayName,
        level = 15,
        xp = 22.2,
        low = 135,
        high = 239,
        stunTicks = 7,
        stunDamage = 1,
        pouch = "obj.pickpocket_coin_pouch_ham",
        lowercaseName = false,
        npcs = npcs,
        op = 1,
    )

private val BAKERS =
    listOf("npc.baker_merchant_ardougne", "npc.baker_merchant_ardougne2", "npc.baker_merchant")

private val ARDOUGNE_MARKET_GUARDS =
    listOf(
        "npc.ardougne_guard",
        "npc.ardougne_guard_variant01",
        "npc.ardougne_guard_f",
        "npc.ardougne_guard_f_variant01",
        "npc.knight_of_ardougne",
        "npc.knight_of_ardougne_f",
        "npc.paladin2",
        "npc.paladin_f_variant01",
    )

private val DRAYNOR_MARKET_GUARDS = listOf("npc.farming_market_guard")

private val PRIFDDINAS_GUARDS = (0..7).map { "npc.prif_guard$it" } + "npc.prif_city_guard"

private val ETCETERIA_GUARDS = listOf("npc.etc_guard1", "npc.etc_guard2")

private val FORTIS_GUARDS =
    (1..5).flatMap { listOf("npc.varlamore_guard_m_$it", "npc.varlamore_guard_f_$it") }
