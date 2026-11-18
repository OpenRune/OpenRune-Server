package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.table.agility.AgilityPengCrushersRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * The crushers in the Penguin course's pool (`dbtable.agility_peng_crushers`): each slides a tile
 * out of the pool's side for two ticks on its own cycle and knocks a player it slides onto a tile
 * on, swimming backwards, for a fixed hit
 * (https://oldschool.runescape.wiki/w/Penguin_Agility_Course).
 */
class PenguinCrushersScript
@Inject
constructor(
    private val mapClock: MapClock,
    private val collision: CollisionFlagMap,
    private val worldRepo: WorldRepository,
    private val players: PlayerRegistry,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (crusher in AgilityPengCrushersRow.all()) {
            onAiTimer(crusher.npc) { slide(npc, crusher) }
        }
    }

    private fun slide(npc: Npc, crusher: AgilityPengCrushersRow) {
        val home = npc.spawnCoords
        val out = (mapClock.cycle - crusher.phase).mod(crusher.period) < OUT_TICKS
        val dest = if (out) home.translateZ(crusher.push) else home
        if (dest == npc.coords) {
            return
        }
        npc.teleport(collision, dest)
        if (!out) {
            return
        }
        worldRepo.soundArea(home, CRUSHER_SYNTH, radius = CRUSHER_SOUND_RADIUS)
        for (player in players.findAll(listOf(ZoneKey.from(dest)))) {
            if (player.coords == dest) {
                protectedAccess.launch(player) { knockedBack(crusher.push) }
            }
        }
    }

    // UNVERIFIED: the hit is always the 4 captured, and lands a tick after the push, not with it.
    private fun ProtectedAccess.knockedBack(push: Int) {
        val from = coords
        val facing = if (push > 0) FACE_SOUTH else FACE_NORTH
        exactMove(from, from.translateZ(push), 0, KNOCK_BACK_CYCLES, facing, TeleportType.Exempt)
        anim(SWIM_BACKWARD)
        queueHit(1, HitType.Typeless, CRUSH_DAMAGE)
    }

    private companion object {
        const val CRUSHER_SYNTH = "synth.peng_block_move"
        const val SWIM_BACKWARD = "seq.peng_swim_walk_backward"
        const val CRUSHER_SOUND_RADIUS = 5
        const val OUT_TICKS = 2
        const val KNOCK_BACK_CYCLES = 29
        const val CRUSH_DAMAGE = 4
        const val FACE_SOUTH = 0
        const val FACE_NORTH = 1024
    }
}
