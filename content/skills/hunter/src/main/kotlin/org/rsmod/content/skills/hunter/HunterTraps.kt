package org.rsmod.content.skills.hunter

internal const val STAT_HUNTER: String = "stat.hunter"
internal const val QUEUE_TRAP: String = "queue.hunter_trap"

internal const val ANIM_LAY_TRAP: String = "seq.human_laytrap"
internal const val ANIM_DISMANTLE_SNARE: String = "seq.human_hunting_dismantle_net"
internal const val ANIM_DISMANTLE_BOX: String = "seq.hunting_setting_trap_small"

data class TrapLoot(val obj: String, val amount: IntRange = 1..1)

/**
 * [low] and [high] are the level-1 and level-99 odds out of 256 that the shared skilling success
 * formula takes, straight off the wiki's catch chance charts. A value over 256 is a rate the
 * formula clamps to certain, which is how the early birds stop failing.
 */
data class HunterCreature(
    val npc: String,
    val name: String,
    val level: Int,
    val xp: Double,
    val fullLoc: String,
    val loot: List<TrapLoot>,
    val low: Int,
    val high: Int,
)

data class TrapKind(
    val item: String,
    val level: Int,
    val activeLoc: String,
    val brokenLoc: String,
    val dismantleAnim: String,
    val creatures: List<HunterCreature>,
)

private fun bird(
    npc: String,
    name: String,
    level: Int,
    xp: Double,
    fullLoc: String,
    feather: String,
    low: Int,
    high: Int,
) =
    HunterCreature(
        npc = npc,
        name = name,
        level = level,
        xp = xp,
        fullLoc = fullLoc,
        loot =
            listOf(
                TrapLoot(feather, 6..12),
                TrapLoot("obj.spit_raw_bird_meat"),
                TrapLoot("obj.bones"),
            ),
        low = low,
        high = high,
    )

object HunterTraps {
    val kinds: List<TrapKind> =
        listOf(
            TrapKind(
                item = "obj.hunting_ojibway_bird_snare",
                level = 1,
                activeLoc = "loc.hunting_ojibway_trap",
                brokenLoc = "loc.hunting_ojibway_trap_broken",
                dismantleAnim = ANIM_DISMANTLE_SNARE,
                creatures =
                    listOf(
                        bird(
                            npc = "npc.hunting_bird_jungle",
                            name = "crimson swift",
                            level = 1,
                            xp = 34.0,
                            fullLoc = "loc.hunting_ojibway_trap_full_jungle",
                            feather = "obj.hunting_jungle_feather",
                            low = 100,
                            high = 420,
                        ),
                        bird(
                            npc = "npc.hunting_bird_desert",
                            name = "golden warbler",
                            level = 5,
                            xp = 47.0,
                            fullLoc = "loc.hunting_ojibway_trap_full_desert",
                            feather = "obj.hunting_desert_feather",
                            low = 92,
                            high = 400,
                        ),
                        bird(
                            npc = "npc.hunting_bird_woodland",
                            name = "copper longtail",
                            level = 9,
                            xp = 61.0,
                            fullLoc = "loc.hunting_ojibway_trap_full_woodland",
                            feather = "obj.hunting_woodland_feather",
                            low = 85,
                            high = 390,
                        ),
                        bird(
                            npc = "npc.hunting_bird_polar",
                            name = "cerulean twitch",
                            level = 11,
                            xp = 64.5,
                            fullLoc = "loc.hunting_ojibway_trap_full_polar",
                            feather = "obj.hunting_polar_feather",
                            low = 82,
                            high = 380,
                        ),
                        bird(
                            npc = "npc.multicoloured_bird",
                            name = "tropical wagtail",
                            level = 19,
                            xp = 95.8,
                            fullLoc = "loc.hunting_ojibway_trap_full_coloured",
                            feather = "obj.hunting_stripy_bird_feather",
                            low = 75,
                            high = 370,
                        ),
                    ),
            ),
            TrapKind(
                item = "obj.hunting_box_trap",
                level = 27,
                activeLoc = "loc.hunting_boxtrap_empty",
                brokenLoc = "loc.hunting_boxtrap_failed",
                dismantleAnim = ANIM_DISMANTLE_BOX,
                creatures =
                    listOf(
                        HunterCreature(
                            npc = "npc.hunting_ferret",
                            name = "ferret",
                            level = 27,
                            xp = 115.0,
                            fullLoc = "loc.hunting_boxtrap_full_ferret",
                            loot = listOf(TrapLoot("obj.hunting_ferret")),
                            // ponytail: the wiki has no ferret chart, so these two are a guess
                            // sitting between the birds and the chinchompas.
                            low = 60,
                            high = 350,
                        ),
                        HunterCreature(
                            npc = "npc.hunting_chinchompa",
                            name = "chinchompa",
                            level = 53,
                            xp = 198.4,
                            fullLoc = "loc.hunting_boxtrap_full_chinchompa",
                            loot = listOf(TrapLoot("obj.chinchompa_captured")),
                            low = 6,
                            high = 268,
                        ),
                        HunterCreature(
                            npc = "npc.hunting_chinchompa_big",
                            name = "red chinchompa",
                            level = 63,
                            xp = 265.0,
                            fullLoc = "loc.hunting_boxtrap_full_chinchompa_big",
                            loot = listOf(TrapLoot("obj.chinchompa_big_captured")),
                            low = -78,
                            high = 228,
                        ),
                        HunterCreature(
                            npc = "npc.hunting_chinchompa_black",
                            name = "black chinchompa",
                            level = 73,
                            xp = 315.4,
                            fullLoc = "loc.hunting_boxtrap_full_chinchompa_black",
                            loot = listOf(TrapLoot("obj.chinchompa_black")),
                            low = -78,
                            high = 228,
                        ),
                    ),
            ),
        )

    fun maxTraps(hunterLevel: Int): Int =
        when {
            hunterLevel >= 80 -> 5
            hunterLevel >= 60 -> 4
            hunterLevel >= 40 -> 3
            hunterLevel >= 20 -> 2
            else -> 1
        }
}
