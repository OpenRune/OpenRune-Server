package org.rsmod.content.skills.hunter

internal const val STAT_HUNTER: String = "stat.hunter"
internal const val QUEUE_TRAP: String = "queue.hunter_trap"

internal const val ANIM_LAY_TRAP: String = "seq.human_laytrap"
internal const val ANIM_DISMANTLE_SNARE: String = "seq.human_hunting_dismantle_net"
internal const val ANIM_DISMANTLE_BOX: String = "seq.hunting_setting_trap_small"

data class TrapLoot(val obj: String, val amount: IntRange = 1..1)

/**
 * [catchChance] is the odds at exactly [level]; every level above that adds a relative 2%, capped at
 * [MAX_CATCH_CHANCE]. These are the numbers to turn if a creature feels too fast or too slow.
 */
data class HunterCreature(
    val npc: String,
    val name: String,
    val level: Int,
    val xp: Double,
    val fullLoc: String,
    val loot: List<TrapLoot>,
    val catchChance: Double,
)

data class TrapKind(
    val item: String,
    val level: Int,
    val activeLoc: String,
    val brokenLoc: String,
    val dismantleAnim: String,
    val creatures: List<HunterCreature>,
)

internal const val MAX_CATCH_CHANCE: Double = 0.9

private fun bird(
    npc: String,
    name: String,
    level: Int,
    xp: Double,
    fullLoc: String,
    feather: String,
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
        catchChance = 0.5,
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
                        ),
                        bird(
                            npc = "npc.hunting_bird_desert",
                            name = "golden warbler",
                            level = 5,
                            xp = 47.0,
                            fullLoc = "loc.hunting_ojibway_trap_full_desert",
                            feather = "obj.hunting_desert_feather",
                        ),
                        bird(
                            npc = "npc.hunting_bird_woodland",
                            name = "copper longtail",
                            level = 9,
                            xp = 61.0,
                            fullLoc = "loc.hunting_ojibway_trap_full_woodland",
                            feather = "obj.hunting_woodland_feather",
                        ),
                        bird(
                            npc = "npc.hunting_bird_polar",
                            name = "cerulean twitch",
                            level = 11,
                            xp = 64.5,
                            fullLoc = "loc.hunting_ojibway_trap_full_polar",
                            feather = "obj.hunting_polar_feather",
                        ),
                        bird(
                            npc = "npc.multicoloured_bird",
                            name = "tropical wagtail",
                            level = 19,
                            xp = 95.8,
                            fullLoc = "loc.hunting_ojibway_trap_full_coloured",
                            feather = "obj.hunting_stripy_bird_feather",
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
                            catchChance = 0.55,
                        ),
                        HunterCreature(
                            npc = "npc.hunting_chinchompa",
                            name = "chinchompa",
                            level = 53,
                            xp = 198.4,
                            fullLoc = "loc.hunting_boxtrap_full_chinchompa",
                            loot = listOf(TrapLoot("obj.chinchompa_captured")),
                            catchChance = 0.55,
                        ),
                        HunterCreature(
                            npc = "npc.hunting_chinchompa_big",
                            name = "red chinchompa",
                            level = 63,
                            xp = 265.0,
                            fullLoc = "loc.hunting_boxtrap_full_chinchompa_big",
                            loot = listOf(TrapLoot("obj.chinchompa_big_captured")),
                            catchChance = 0.55,
                        ),
                        HunterCreature(
                            npc = "npc.hunting_chinchompa_black",
                            name = "black chinchompa",
                            level = 73,
                            xp = 315.4,
                            fullLoc = "loc.hunting_boxtrap_full_chinchompa_black",
                            loot = listOf(TrapLoot("obj.chinchompa_black")),
                            catchChance = 0.6,
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
