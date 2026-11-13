package org.rsmod.content.skills.agility.pack.shortcut

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT_CROSSBOW_GRAPPLE
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.MITHRIL_GRAPPLE
import org.rsmod.content.skills.agility.pack.ObstacleStat
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.requirement
import org.rsmod.content.skills.agility.pack.stats
import org.rsmod.content.skills.agility.pack.tile

private const val WOBBLE_LEFT = "seq.human_wobbleandfall_l"
private const val WOBBLE_RIGHT = "seq.human_wobbleandfall_r"
private const val PLOP_SYNTH = "synth.pool_plop"
private const val JAGGED_ROCKS = "You push yourself through countless jagged rocks..."
private const val SLIDE_BACK =
    "You try to go up the crevice, but it's slippery and you slide back down. Looks like this " +
        "is one way only."

internal const val YANILLE_LEDGE_XP = 220
internal const val KARAMJA_LOG_XP = 40
internal const val MORT_MYRE_BRIDGE_XP = 150

internal val YANILLE_LEDGE_WAYS = both(tile(2580, 9520, 0), tile(2580, 9512, 0))
internal val KARAMJA_LOG_WAYS = both(tile(2910, 3049, 0), tile(2906, 3049, 0))
internal val MORT_MYRE_BRIDGE_WAYS =
    listOf(
        way(tile(3440, 3329, 0), tile(3440, 3331, 0)),
        way(tile(3441, 3329, 0), tile(3440, 3331, 0)),
        way(tile(3440, 3331, 0), tile(3441, 3329, 0)),
        way(tile(3441, 3331, 0), tile(3441, 3329, 0)),
    )

/*
 * Agility shortcuts the map transports used to teleport across, built from captures: the Karamja
 * jungle grapple, the Dagannoth crevice, the Asgarnian Ice Dungeon wyvern tunnel and the Burgh de
 * Rott fence. The Yanille dungeon ledge, the Karamja log and the Mort Myre bridge share rows of
 * their level and pay their own xp.
 */
internal fun DBTableBuilder.shortcutAdditions() {
    row("dbrow.agility_shortcut_karamja_jungle_grapple") {
        shortcut(level = 53, xp = 0, "loc.xbows_jungletree_karamja_basic")
        requirement(REQUIREMENT_CROSSBOW_GRAPPLE, MITHRIL_GRAPPLE)
        stats(ObstacleStat("stat.ranged", 42, 0), ObstacleStat("stat.strength", 21, 0))
        apRange(8)
        lanes(
            lane(tile(2874, 3142, 0)) {
                grappleSwim(
                    way(tile(2874, 3142, 0), tile(2874, 3136, 0)),
                    northChannel = true,
                    toIsland = true,
                )
            },
            lane(tile(2874, 3133, 0)) {
                grappleSwim(
                    way(tile(2874, 3133, 0), tile(2874, 3127, 0)),
                    northChannel = false,
                    toIsland = false,
                )
            },
        )
    }

    row("dbrow.agility_shortcut_dagannoth_crevice") {
        shortcut(level = 81, xp = 0, "loc.dagannoth_crevice")
        lanes(
            lane(tile(1852, 4406, 3)) {
                creviceCrawl(way(tile(1852, 4406, 3), tile(1862, 4406, 1)), xp = 100)
            },
            lane(tile(1862, 4406, 1)) { oneWayOnly(FACE_WEST, SLIDE_BACK) },
        )
    }

    row("dbrow.agility_shortcut_wyvern_cave_tunnels") {
        shortcut(
            level = 82,
            xp = 0,
            "loc.cavewall_shortcut_wyvern_west",
            "loc.cavewall_shortcut_wyvern_east",
        )
        // UNVERIFIED: wording kept from the old handler.
        levelRefusal(
            "The tunnel is very dark and dangerous. You'll need an Agility level of 82 to try " +
                "this.",
            "obj.agility_contortion",
        )
        val middle = tile(3026, 9563, 0)
        val toWyverns =
            listOf(JAGGED_ROCKS, "You brace yourself for the wyverns on the other side...")
        val fromWyverns = listOf(JAGGED_ROCKS, "You see bright torchlight on the other side...")
        lanes(
            *(9549..9551)
                .map { z ->
                    lane(tile(3013, z, 0)) {
                        longCrawl(
                            way(tile(3013, z, 0), tile(3025, 9554, 0)),
                            FACE_EAST,
                            SCRAMBLE,
                            via = middle,
                            texts = toWyverns,
                        )
                    }
                }
                .toTypedArray(),
            *(9553..9555)
                .map { z ->
                    lane(tile(3024, z, 0)) {
                        longCrawl(
                            way(tile(3024, z, 0), tile(3012, 9550, 0)),
                            FACE_WEST,
                            SCRAMBLE,
                            via = middle,
                            texts = fromWyverns,
                        )
                    }
                }
                .toTypedArray(),
        )
    }

    row("dbrow.agility_shortcut_burgh_de_rott_fence") {
        shortcut(level = 25, xp = 0, "loc.burgh_agility_shortcut_fence")
        varbitGates(Triple("varbit.myreque_2_quest", 40, Int.MAX_VALUE))
        lanes(
            lane(tile(3474, 3221, 0)) {
                fenceHurdle(way(tile(3474, 3221, 0), tile(3473, 3221, 0)), tile(3477, 3221, 0))
            },
            lane(tile(3473, 3221, 0)) {
                fenceHurdle(way(tile(3473, 3221, 0), tile(3475, 3221, 0)), tile(3471, 3221, 0))
            },
        )
    }
}

/** Captured on the Mort Myre bridge: the player falls short into the swamp between its halves. */
internal fun LaneBuilder.bridgeFall(way: Way) {
    exactMove(tile(3439, 3330, 0), delay1 = 58, delay2 = 60, angleTo(way.from, way.to))
    anim(if (way.to.z > way.from.z) WOBBLE_LEFT else WOBBLE_RIGHT, delay = 30)
    next()
    sound(JUMP_SYNTH)
    next()
    step(tile(3438, 3331, 0))
    sound(PLOP_SYNTH)
    next(3)
    message("You nearly drown in the disgusting swamp.")
    step(tile(3438, 3332, 0))
    resetAnim()
    hit()
    xp(20)
}
