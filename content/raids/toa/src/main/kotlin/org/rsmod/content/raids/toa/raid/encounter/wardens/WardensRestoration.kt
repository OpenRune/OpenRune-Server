package org.rsmod.content.raids.toa.raid.encounter.wardens

import org.rsmod.api.npc.heal
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

internal class WardensRestoration(private val room: WardensEncounter) {
    private val deps = room.raid.deps

    fun begin(winner: WardenGod, p2: Npc, coreTile: CoordGrid?) {
        val restored = winner.other
        val restoredNpc = room.warden(restored)
        room.stopAttacking(p2)
        p2.ignoreCombatInteractions = true

        coreTile?.let {
            deps.worldRepo.spotanimMap(spotanim(CORE_BURST), it, height = CORE_BURST_HEIGHT, delay = CORE_BURST_DELAY)
        }
        deps.worldRepo.soundArea(coreTile ?: p2.coords, WardenCoreRefs.REVIVE_SOUND, radius = SOUND_RADIUS)

        room.schedule(RESTORE_TICK) { announce(winner, restored, p2, restoredNpc) }
        room.schedule(HEAL_TICK) { heal(restoredNpc) }
        room.schedule(OBELISK_DEATH_TICK) { obeliskDeath() }
        room.schedule(CAM_RESET_TICK) { shake(reset = true) }
        room.schedule(TRAVEL_TICK) {
            room.hpBar?.complete()
            room.startFinalPhase()
        }
    }

    private fun announce(winner: WardenGod, restored: WardenGod, p2: Npc, restoredNpc: Npc?) {
        for (player in room.players) {
            player.mes(
                "<col=${restored.colour}>${winner.displayName} uses the last of its power to " +
                    "restore ${restored.displayName}!</col>"
            )
            player.soundSynth(RESTORE_START_SOUND, delay = RESTORE_START_DELAY)
            player.soundSynth(RESTORE_LOOP_SOUND, delay = RESTORE_LOOP_DELAY)
            player.soundSynth(RESTORE_END_SOUND, delay = RESTORE_END_DELAY)
        }
        restoredNpc?.anim(WAKE_SEQ, delay = WAKE_DELAY)
        restoredNpc?.spotanim(WAKE_SPOT, delay = WAKE_DELAY)
        p2.anim(DEATH_SEQ)
    }

    private fun heal(restoredNpc: Npc?) {
        restoredNpc?.let {
            it.baseHitpointsLvl = room.p3Hitpoints()
            it.hitpoints = 0
            it.heal(it.baseHitpointsLvl, showHitsplat = true)
        }
        val centre = WardenGod.OBELISK_CENTRE.let(room::coords)
        for (step in 1..BLAST_COUNT) {
            val tile = centre.translate(randomOffset(), randomOffset())
            val delay = BLAST_STEP * (1 + deps.random.of(BLAST_COUNT_DELAYS))
            val spot = BLASTS[deps.random.of(BLASTS.size)]
            deps.worldRepo.spotanimMap(spotanim(spot), tile, delay = delay)
        }
        for (step in 1..BLAST_SOUNDS) {
            deps.worldRepo.soundArea(
                centre,
                BLAST_SOUND,
                delay = BLAST_STEP * step,
                radius = SOUND_RADIUS,
            )
        }
        for (player in room.players) player.soundSynth(HEAL_SOUND)
        shake(reset = false)
    }

    private fun obeliskDeath() {
        room.obelisk?.anim(OBELISK_DEATH_SEQ)
        val centre = room.coords(WardenGod.OBELISK_CENTRE)
        for (step in 1..DEATH_SOUNDS) {
            deps.worldRepo.soundArea(
                centre,
                BLAST_SOUND,
                delay = BLAST_STEP * step,
                radius = SOUND_RADIUS,
            )
        }
        for (player in room.players) player.soundSynth(OBELISK_DEATH_SOUND)
        for (tick in 0 until DEATH_TICKS) {
            room.schedule(tick) {
                repeat(DEATH_WARNS) {
                    val tile = centre.translate(ringOffset(), ringOffset())
                    deps.worldRepo.spotanimMap(spotanim(LIGHTNING_WARN), tile)
                }
                val tile = centre.translate(ringOffset(), ringOffset())
                deps.worldRepo.spotanimMap(spotanim(BLASTS[deps.random.of(BLASTS.size)]), tile)
            }
        }
    }

    private fun shake(reset: Boolean) {
        for (player in room.players) {
            if (reset) {
                Camera.camReset(player)
            } else {
                Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, SHAKE_LEFT_RIGHT, 0, 0)
                Camera.camShake(player, CamShakeAxis.UP_DOWN, SHAKE_UP_DOWN, 0, 0)
                Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, SHAKE_FORWARDS, 0, 0)
            }
        }
    }

    private fun randomOffset(): Int = deps.random.of(-BLAST_RADIUS, BLAST_RADIUS)

    private fun ringOffset(): Int = deps.random.of(-RING_RADIUS, RING_RADIUS)

    private companion object {
        const val RESTORE_TICK = 1
        const val HEAL_TICK = 7
        const val OBELISK_DEATH_TICK = 10
        const val CAM_RESET_TICK = 12
        const val TRAVEL_TICK = 13

        const val CORE_BURST = "spotanim.vfx_scarab_explosion01"
        const val CORE_BURST_HEIGHT = 50
        const val CORE_BURST_DELAY = 30
        const val SOUND_RADIUS = 15

        const val WAKE_SEQ = "seq.npc_wardens_wake02"
        const val WAKE_SPOT = "spotanim.spotanim_wardens01_wakeup02"
        const val WAKE_DELAY = 120
        const val DEATH_SEQ = "seq.npc_wardens_death01"
        const val OBELISK_DEATH_SEQ = "seq.fx_obelisk_wardens02_death"
        const val LIGHTNING_WARN = "spotanim.wardens_lightning_warn"

        const val RESTORE_START_SOUND = "synth.toa_wardens_restore_start"
        const val RESTORE_LOOP_SOUND = "synth.toa_wardens_restore_loop"
        const val RESTORE_END_SOUND = "synth.toa_wardens_restore_end"
        const val RESTORE_START_DELAY = 125
        const val RESTORE_LOOP_DELAY = 249
        const val RESTORE_END_DELAY = 254
        const val BLAST_SOUND = "synth.toa_wardens_obelisk_blast"
        const val OBELISK_DEATH_SOUND = "synth.toa_wardens_obelisk_death"
        const val HEAL_SOUND = "synth.selfheal"

        const val BLAST_COUNT = 24
        const val BLAST_COUNT_DELAYS = 7
        const val BLAST_STEP = 30
        const val BLAST_RADIUS = 4
        const val BLAST_SOUNDS = 7
        const val DEATH_SOUNDS = 3
        const val DEATH_TICKS = 3
        const val DEATH_WARNS = 2
        const val RING_RADIUS = 2

        const val SHAKE_LEFT_RIGHT = 5
        const val SHAKE_UP_DOWN = 4
        const val SHAKE_FORWARDS = 3

        val BLASTS =
            listOf(
                "spotanim.vfx_scarab_explosion01",
                "spotanim.vfx_scarab_explosion02",
                "spotanim.vfx_scarab_explosion03",
                "spotanim.vfx_scarab_explosion04",
            )
    }
}
