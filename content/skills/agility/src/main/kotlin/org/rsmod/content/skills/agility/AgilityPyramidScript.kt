package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.sign
import org.rsmod.api.enums.AgilityPyramidBlockEnums.agility_pyramid_block_periods
import org.rsmod.api.enums.AgilityPyramidBlockEnums.agility_pyramid_block_reach
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * The Agility Pyramid's parts that are not lanes: the climbing rocks that give the pyramid top once
 * a lap (https://oldschool.runescape.wiki/w/Pyramid_top), and the two pyramid blocks that slide
 * out of the wall on floors 1 and 3 and back on a fixed cycle, blocking the way while out. The
 * floor the player is on resets once they are back on the ground around the pyramid.
 */
class AgilityPyramidScript
@Inject
constructor(
    private val mapClock: MapClock,
    private val collision: CollisionFlagMap,
    private val worldRepo: WorldRepository,
    private val players: PlayerRegistry,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(CLIMBING_ROCKS) { climbToTop() }
        onPlayerCoordsChanged {
            if (player.coords.aroundPyramid() && player.vars[PYRAMID_LEVEL] != 0) {
                VarPlayerIntMapSetter.set(player, PYRAMID_LEVEL, 0)
            }
        }
        val reaches =
            agility_pyramid_block_reach.backing.entries.associate { (npc, reach) ->
                npc.id to checkNotNull(reach)
            }
        for ((type, period) in agility_pyramid_block_periods.backing.entries) {
            val cycle = checkNotNull(period)
            val reach = reaches.getValue(type.id)
            onAiTimer(type) { slide(npc, cycle, reach) }
        }
    }

    private suspend fun ProtectedAccess.climbToTop() {
        arriveDelay()
        anim(TOP_CLIMB)
        soundSynth(CLIMBING_SYNTH, loops = 4)
        delay(1)
        // UNVERIFIED: that climbing again in the same lap only plays the climb.
        if (vars[PYRAMID_TOP] != 0) {
            return
        }
        if (invAdd(inv, GOLD_PYRAMID).failure) {
            // UNVERIFIED: the wording, and that the top waits for a free slot.
            mes("You don't have enough inventory space.")
            return
        }
        completeSummitTask()
        vars[PYRAMID_TOP] = 1
        midiJingle(TOP_JINGLE)
        objbox(GOLD_PYRAMID, "You find a golden pyramid!")
    }

    private fun ProtectedAccess.completeSummitTask() {
        val tasks = vars[DESERT_DIARY]
        if (tasks and SUMMIT_TASK != 0) {
            return
        }
        vars[DESERT_DIARY] = tasks or SUMMIT_TASK
        vars[DESERT_MEDIUM_TASKS] += 1
        mes(
            "<col=dc143c>Well done! You have completed a medium task in the Desert area. Your " +
                "Achievement Diary has been updated.</col>"
        )
    }

    private fun CoordGrid.aroundPyramid(): Boolean =
        level == 0 && x in GROUND_MIN_X..GROUND_MAX_X && z in GROUND_MIN_Z..GROUND_MAX_Z

    private fun slide(block: Npc, period: Int, reach: CoordGrid) {
        val home = block.spawnCoords
        val phase = mapClock.cycle % period
        if (phase == 0) {
            worldRepo.soundArea(home, BLOCK_SYNTH, radius = BLOCK_SOUND_RADIUS)
        }
        val tiles = SLIDE_TILES.getOrElse(phase) { 0 }
        val dx = (reach.x - home.x).sign
        val dz = (reach.z - home.z).sign
        val dest = home.translate(dx * tiles, dz * tiles)
        if (dest == block.coords) {
            return
        }
        val outward = tiles > abs(block.coords.x - home.x) + abs(block.coords.z - home.z)
        block.teleport(collision, dest)
        if (outward) {
            val far = block.coords.translate(block.size - 1, block.size - 1)
            val zones = ZoneKey.covering(block.coords, far)
            players.findAll(zones).filter { it.isUnder(block) }.forEach { pushOff(it, dx, dz) }
        }
    }

    private fun Player.isUnder(block: Npc): Boolean =
        level == block.level &&
            x in block.x until block.x + block.size &&
            z in block.z until block.z + block.size

    private fun pushOff(player: Player, dx: Int, dz: Int) {
        protectedAccess.launch(player) { pushedOff(dx, dz) }
    }

    // UNVERIFIED: the whole push; the wiki only says it drops the player a floor for 8 damage.
    private suspend fun ProtectedAccess.pushedOff(dx: Int, dz: Int) {
        anim(PUSH)
        delay(1)
        val landing = coords.translate(dx * PUSH_TILES, dz * PUSH_TILES, -1)
        telejump(landing)
        anim(PUSH_LAND)
        vars[PYRAMID_LEVEL] = landing.level
        queueHit(1, HitType.Typeless, PUSH_DAMAGE)
    }

    private companion object {
        const val CLIMBING_ROCKS = "loc.agility_pyramid_wall_rocks"
        const val GOLD_PYRAMID = "obj.agility_pyramid_gold_pyramid"
        const val PYRAMID_TOP = "varbit.agility_pyramid_top"
        const val PYRAMID_LEVEL = "varbit.agility_pyramid_level"
        const val TOP_CLIMB = "seq.agility_pyramid_top_climb"
        const val CLIMBING_SYNTH = "synth.climbing_loop"
        const val TOP_JINGLE = "jingle.pyramid_top"
        const val DESERT_DIARY = "varp.desert_achievement_diary"
        const val DESERT_MEDIUM_TASKS = "varbit.desert_med_count"
        const val SUMMIT_TASK = 1 shl 12

        const val GROUND_MIN_X = 3328
        const val GROUND_MAX_X = 3391
        const val GROUND_MIN_Z = 2816
        const val GROUND_MAX_Z = 2879

        const val BLOCK_SYNTH = "synth.pyramid_block"
        const val BLOCK_SOUND_RADIUS = 5
        const val PUSH = "seq.agility_pyramid_block_push_1"
        const val PUSH_LAND = "seq.agility_pyramid_block_push_2"
        const val PUSH_TILES = 2
        const val PUSH_DAMAGE = 8

        val SLIDE_TILES = intArrayOf(1, 2, 2, 2, 1)
    }
}
