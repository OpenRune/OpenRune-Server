package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.map.CoordGrid

/**
 * One row per course. `helper_course` is the course's key in the client's agility helper enum,
 * whose values list each course's obstacle locs (`enum.gnome_stronghold_course_obstacles`).
 */
object AgilityCoursesTable {
    const val LEVEL = 0
    const val LAP_BONUS_XP = 1
    const val MARK_TILES = 2
    const val NAME = 3
    const val LAP_COUNT = 4
    const val HELPER_COURSE = 5
    const val MARK_CHANCE = 6
    const val MARK_LEVEL_PENALTY = 7
    const val MARK_REROLLS = 8
    const val MARK_SHORT_TIMER = 9
    const val QUEST = 10
    const val QUEST_STARTED = 11
    const val LEVEL_UNBOOSTABLE = 12
    const val LAP_STAT_XP = 13
    const val LAP_BONUS_PER_LEVEL = 14
    const val ANY_ORDER = 15
    const val PET_CHANCE = 16
    const val LAP_BEST = 17
    const val LAP_TIME_STYLE = 18
    const val HAND_IN_LAPS = 19
    const val LEG_BONUS_XP = 20
    const val LAP_COLOUR = 21
    const val NEEDS_LIGHT = 22
    const val NO_FOLLOWERS = 23

    fun courses() =
        dbTable("dbtable.agility_courses", serverOnly = true) {
            column("level", LEVEL, VarType.INT)
            column("lap_bonus_xp", LAP_BONUS_XP, VarType.INT)
            column("mark_tiles", MARK_TILES, VarType.COORDGRID)
            column("name", NAME, VarType.STRING)
            column("lap_count", LAP_COUNT, VarType.VARP)
            column("helper_course", HELPER_COURSE, VarType.INT)
            column("mark_chance", MARK_CHANCE, VarType.INT, VarType.INT)
            column("mark_level_penalty", MARK_LEVEL_PENALTY, VarType.BOOLEAN)
            column("mark_rerolls", MARK_REROLLS, VarType.INT, VarType.INT)
            column("mark_short_timer", MARK_SHORT_TIMER, VarType.INT, VarType.INT)
            column("quest", QUEST, VarType.DBROW)
            column("quest_started", QUEST_STARTED, VarType.BOOLEAN)
            column("level_unboostable", LEVEL_UNBOOSTABLE, VarType.BOOLEAN)
            column("lap_stat_xp", LAP_STAT_XP, VarType.INT, VarType.INT)
            column("lap_bonus_per_level", LAP_BONUS_PER_LEVEL, VarType.INT, VarType.INT)
            column("any_order", ANY_ORDER, VarType.BOOLEAN)
            column("pet_chance", PET_CHANCE, VarType.INT)
            column("lap_best", LAP_BEST, VarType.VARP)
            column("lap_time_style", LAP_TIME_STYLE, VarType.INT, VarType.INT)
            column("hand_in_laps", HAND_IN_LAPS, VarType.BOOLEAN)
            column("leg_bonus_xp", LEG_BONUS_XP, VarType.STAT, VarType.INT)
            column("lap_colour", LAP_COLOUR, VarType.INT)
            column("needs_light", NEEDS_LIGHT, VarType.BOOLEAN)
            column("no_followers", NO_FOLLOWERS, VarType.BOOLEAN)

            // UNVERIFIED: the Gnome Stronghold, Al Kharid and Pollnivneach mark tiles and the last
            // three of Canifis'.
            course(
                "dbrow.agility_course_gnome_stronghold",
                level = 1,
                name = "Gnome Stronghold Agility",
                helperCourse = 1,
                lapCount = "varp.agility_laps_gnome_stronghold",
                lapBonusXp = 500,
                markTiles = listOf(tile(2484, 3419, 2)),
                petChance = 35609,
            )
            course(
                "dbrow.agility_course_draynor",
                level = 1,
                name = "Draynor Village Rooftop",
                helperCourse = 3,
                lapCount = "varp.agility_laps_draynor",
                markTiles =
                    listOf(
                        tile(3098, 3281, 3),
                        tile(3089, 3274, 3),
                        tile(3094, 3266, 3),
                        tile(3097, 3259, 3),
                        tile(3100, 3257, 3),
                    ),
                petChance = 33005,
            )
            course(
                "dbrow.agility_course_al_kharid",
                level = 20,
                name = "Al Kharid Rooftop",
                helperCourse = 4,
                lapCount = "varp.agility_laps_al_kharid",
                markTiles =
                    listOf(
                        tile(3274, 3190, 3),
                        tile(3270, 3170, 3),
                        tile(3290, 3165, 3),
                        tile(3316, 3162, 1),
                        tile(3316, 3176, 2),
                        tile(3316, 3183, 3),
                        tile(3301, 3190, 3),
                    ),
                petChance = 26648,
            )
            course(
                "dbrow.agility_course_varrock",
                level = 30,
                name = "Varrock Rooftop",
                helperCourse = 7,
                lapCount = "varp.agility_laps_varrock",
                markTiles =
                    listOf(
                        tile(3195, 3416, 1),
                        tile(3191, 3394, 3),
                        tile(3186, 3395, 3),
                        tile(3220, 3402, 3),
                        tile(3238, 3408, 3),
                    ),
                petChance = 24410,
            )
            course(
                "dbrow.agility_course_canifis",
                level = 40,
                name = "Canifis Rooftop",
                helperCourse = 10,
                lapCount = "varp.agility_laps_canifis",
                markChance = 2 to 3,
                markLevelPenalty = false,
                markTiles =
                    listOf(
                        tile(3506, 3495, 2),
                        tile(3501, 3505, 2),
                        tile(3490, 3501, 2),
                        tile(3478, 3496, 3),
                        tile(3480, 3486, 2),
                        tile(3495, 3475, 3),
                        tile(3511, 3479, 2),
                    ),
                petChance = 36842,
            )
            course(
                "dbrow.agility_course_falador",
                level = 50,
                name = "Falador Rooftop",
                helperCourse = 12,
                lapCount = "varp.agility_laps_falador",
                markTiles =
                    listOf(
                        tile(3045, 3345, 3),
                        tile(3049, 3357, 3),
                        tile(3019, 3345, 3),
                        tile(3013, 3345, 3),
                        tile(3015, 3333, 3),
                    ),
                petChance = 26806,
            )
            course(
                "dbrow.agility_course_seers",
                level = 60,
                name = "Seers' Village Rooftop",
                helperCourse = 15,
                lapCount = "varp.agility_laps_seers",
                markRerolls =
                    listOf(
                        "varbit.kandarin_diary_easy_complete" to 5,
                        "varbit.kandarin_diary_medium_complete" to 10,
                        "varbit.kandarin_diary_hard_complete" to 15,
                    ),
                markTiles =
                    listOf(
                        tile(2727, 3493, 3),
                        tile(2709, 3493, 2),
                        tile(2706, 3493, 2),
                        tile(2712, 3481, 2),
                        tile(2698, 3465, 2),
                    ),
                petChance = 35205,
            )
            course(
                "dbrow.agility_course_pollnivneach",
                level = 70,
                name = "Pollnivneach Rooftop",
                helperCourse = 17,
                lapCount = "varp.agility_laps_pollnivneach",
                markShortTimer = "varbit.desert_diary_hard_complete" to 3,
                markTiles =
                    listOf(
                        tile(3348, 2966, 1),
                        tile(3354, 2975, 1),
                        tile(3361, 2980, 1),
                        tile(3368, 2975, 1),
                        tile(3357, 2983, 2),
                        tile(3364, 2993, 2),
                        tile(3357, 3002, 2),
                    ),
                petChance = 33422,
            )
            course(
                "dbrow.agility_course_rellekka",
                level = 80,
                name = "Rellekka Rooftop",
                helperCourse = 19,
                lapCount = "varp.agility_laps_rellekka",
                markChance = 2 to 5,
                markShortTimer = "varbit.fremennik_diary_hard_complete" to 3,
                markTiles =
                    listOf(
                        tile(2623, 3675, 3),
                        tile(2628, 3652, 3),
                        tile(2629, 3653, 3),
                        tile(2642, 3651, 3),
                    ),
                petChance = 31063,
            )
            course(
                "dbrow.agility_course_ardougne",
                level = 90,
                name = "Ardougne Rooftop",
                helperCourse = 20,
                lapCount = "varp.agility_laps_ardougne",
                markChance = 2 to 3,
                markRerolls = listOf("varbit.ardougne_diary_elite_complete" to 25),
                markShortTimer = "varbit.ardougne_diary_elite_complete" to 2,
                markTiles = listOf(tile(2657, 3318, 3)),
                petChance = 34440,
            )

            // UNVERIFIED: the lap count names (bar Wilderness and Prifddinas), the mark tiles and
            // the mark chance, which the wiki gives per hour rather than per lap.
            course(
                "dbrow.agility_course_barbarian",
                level = 35,
                name = "Barbarian Outpost",
                helperCourse = 9,
                lapCount = "varp.agility_laps_barbarian",
                lapBonusXp = 463,
                lapStatXp = "stat.strength" to 413,
                quest = "dbrow.miniquest_barcrawl",
                markTiles = listOf(tile(2532, 3546, 1)),
                petChance = 44376,
            )
            course(
                "dbrow.agility_course_wilderness",
                level = 52,
                name = "Wilderness Agility",
                helperCourse = 13,
                lapCount = "varp.agility_laps_wilderness",
                lapBonusXp = 4989,
                petChance = 34666,
            )
            course(
                "dbrow.agility_course_prifddinas",
                level = 75,
                name = "Prifddinas Agility Course",
                helperCourse = 18,
                lapCount = "varp.agility_laps_prifddinas",
                quest = "dbrow.quest_songoftheelves",
                petChance = 25146,
                lapBest = "varp.agility_best_lap_prifddinas",
                lapTimeStyle = LAP_TIME_SECONDS,
            )
            course(
                "dbrow.agility_course_ape_atoll",
                level = 48,
                name = "Ape Atoll",
                helperCourse = 11,
                lapCount = "varp.agility_laps_ape_atoll",
                lapBonusXp = 3000,
                levelUnboostable = true,
                quest = "dbrow.quest_monkeymadness1",
                questStarted = true,
                markTiles = listOf(tile(2743, 2738, 0)),
                petChance = 37720,
            )
            course(
                "dbrow.agility_course_werewolf",
                level = 60,
                name = "Werewolf Agility",
                helperCourse = 14,
                lapCount = "varp.agility_laps_werewolf",
                quest = "dbrow.quest_creatureoffenkenstrain",
                markTiles = listOf(tile(3540, 9888, 0)),
                petChance = 32597,
            )
            course(
                "dbrow.agility_course_shayzien_basic",
                level = 1,
                name = "Shayzien Basic Agility Course",
                helperCourse = 21,
                lapCount = "varp.agility_laps_shayzien_basic",
                markTiles = listOf(tile(1530, 3632, 2)),
                petChance = 31804,
            )
            course(
                "dbrow.agility_course_shayzien_advanced",
                level = 45,
                name = "Shayzien Advanced Agility Course",
                helperCourse = 21,
                lapCount = "varp.agility_laps_shayzien_advanced",
                markTiles = listOf(tile(1530, 3632, 2)),
                petChance = 29738,
            )
            // TODO(content): the wiki has no giant squirrel chance for the Colossal Wyrm courses.
            course(
                "dbrow.agility_course_wyrm_basic",
                level = 50,
                name = "Colossal Wyrm Agility Course (Basic)",
                helperCourse = 22,
                lapCount = "varp.agility_laps_wyrm_basic",
                quest = "dbrow.quest_childrenofthesun",
                lapBest = "varp.agility_best_lap_wyrm_basic",
                lapTimeStyle = LAP_TIME_HUNDREDTHS,
                lapColour = WYRM_RED,
            )
            course(
                "dbrow.agility_course_wyrm_advanced",
                level = 62,
                name = "Colossal Wyrm Agility Course (Advanced)",
                helperCourse = 22,
                lapCount = "varp.agility_laps_wyrm_advanced",
                quest = "dbrow.quest_childrenofthesun",
                lapBest = "varp.agility_best_lap_wyrm_advanced",
                lapTimeStyle = LAP_TIME_HUNDREDTHS,
                lapColour = WYRM_RED,
            )
            course(
                "dbrow.agility_course_penguin",
                level = 30,
                name = "Penguin Agility",
                helperCourse = 8,
                lapCount = "varp.agility_laps_penguin",
                quest = "dbrow.quest_coldwar",
                questStarted = true,
                petChance = 9779,
            )
            course(
                "dbrow.agility_course_brimhaven_arena",
                level = 1,
                name = "Agility Arena",
                helperCourse = 2,
                lapCount = "varp.agility_laps_brimhaven_arena",
                petChance = 26404,
            )
            course(
                "dbrow.agility_course_pyramid",
                level = 30,
                name = "Agility Pyramid",
                helperCourse = 6,
                lapCount = "varp.agility_laps_pyramid",
                lapBonusXp = 3000,
                lapBonusPerLevel = 80 to 10000,
                anyOrder = true,
                petChance = 9901,
            )

            // UNVERIFIED: the lap name, as no capture reaches Turgall, and the mark tile.
            course(
                "dbrow.agility_course_dorgesh_kaan",
                level = 70,
                name = "Dorgesh-Kaan Agility",
                helperCourse = 16,
                lapCount = "varp.agility_laps_dorgesh_kaan",
                quest = "dbrow.quest_deathtothedorgeshuun",
                handInLaps = true,
                legBonusXp = listOf("stat.agility" to 12160, "stat.ranged" to 5710),
                markTiles = listOf(tile(2740, 5214, 3)),
                petChance = 10561,
                needsLight = true,
                noFollowers = true,
            )
        }

    private const val LAP_RED = 0xff0000
    private const val WYRM_RED = 0xff3045

    private val LAP_TIME_HUNDREDTHS = WYRM_RED to 2

    // UNVERIFIED: that live rounds Prifddinas's whole seconds down.
    private val LAP_TIME_SECONDS = LAP_RED to 0

    private fun DBTableBuilder.course(
        row: String,
        level: Int,
        name: String,
        helperCourse: Int,
        lapCount: String,
        markTiles: List<CoordGrid> = emptyList(),
        lapBonusXp: Int = 0,
        lapStatXp: Pair<String, Int>? = null,
        quest: String? = null,
        questStarted: Boolean = false,
        levelUnboostable: Boolean = false,
        markChance: Pair<Int, Int> = 2 to 6,
        markLevelPenalty: Boolean = true,
        markRerolls: List<Pair<String, Int>> = emptyList(),
        markShortTimer: Pair<String, Int>? = null,
        lapBonusPerLevel: Pair<Int, Int>? = null,
        anyOrder: Boolean = false,
        petChance: Int? = null,
        lapBest: String? = null,
        lapTimeStyle: Pair<Int, Int>? = null,
        handInLaps: Boolean = false,
        legBonusXp: List<Pair<String, Int>> = emptyList(),
        lapColour: Int = LAP_RED,
        needsLight: Boolean = false,
        noFollowers: Boolean = false,
    ) {
        row(row) {
            column(LEVEL, level)
            column(LAP_COLOUR, lapColour)
            column(LAP_BONUS_XP, lapBonusXp)
            if (lapBonusPerLevel != null) {
                column(LAP_BONUS_PER_LEVEL, lapBonusPerLevel.first, lapBonusPerLevel.second)
            }
            if (anyOrder) {
                column(ANY_ORDER, true)
            }
            if (markTiles.isNotEmpty()) {
                column(MARK_TILES, markTiles.map { it.packed })
            }
            if (lapStatXp != null) {
                column(LAP_STAT_XP, lapStatXp.first.asRSCM(), lapStatXp.second)
            }
            if (quest != null) {
                columnRSCM(QUEST, quest)
                column(QUEST_STARTED, questStarted)
            }
            if (levelUnboostable) {
                column(LEVEL_UNBOOSTABLE, true)
            }
            column(NAME, name)
            columnRSCM(LAP_COUNT, lapCount)
            column(HELPER_COURSE, helperCourse)
            column(MARK_CHANCE, markChance.first, markChance.second)
            column(MARK_LEVEL_PENALTY, markLevelPenalty)
            if (markRerolls.isNotEmpty()) {
                val rerolls =
                    markRerolls.flatMap { (varbit, percent) -> listOf(varbit.asRSCM(), percent) }
                column(MARK_REROLLS, rerolls)
            }
            if (markShortTimer != null) {
                column(MARK_SHORT_TIMER, markShortTimer.first.asRSCM(), markShortTimer.second)
            }
            if (petChance != null) {
                column(PET_CHANCE, petChance)
            }
            if (lapBest != null) {
                columnRSCM(LAP_BEST, lapBest)
            }
            if (lapTimeStyle != null) {
                column(LAP_TIME_STYLE, lapTimeStyle.first, lapTimeStyle.second)
            }
            if (handInLaps) {
                column(HAND_IN_LAPS, true)
            }
            if (legBonusXp.isNotEmpty()) {
                column(LEG_BONUS_XP, legBonusXp.flatMap { (stat, xp) -> listOf(stat.asRSCM(), xp) })
            }
            if (needsLight) {
                column(NEEDS_LIGHT, true)
            }
            if (noFollowers) {
                column(NO_FOLLOWERS, true)
            }
        }
    }
}
