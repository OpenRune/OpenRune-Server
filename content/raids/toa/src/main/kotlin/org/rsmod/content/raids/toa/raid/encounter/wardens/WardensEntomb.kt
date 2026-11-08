package org.rsmod.content.raids.toa.raid.encounter.wardens

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class WardensEntomb(private val room: WardensEncounter) {
    private val deps = room.raid.deps

    fun isEntombed(player: Player): Boolean = player.entombed

    fun land(player: Player, tile: CoordGrid?) {
        if (tile == null) return
        if (player.coords != tile) {
            deps.worldRepo.spotanimMap(spotanim(WardenSpots.ENTOMB_MISS), tile)
            return
        }
        player.entombed = true
        player.mes(WardenMessages.ENTOMBED)
        player.anim(WardenSeqs.ENTOMB)
        player.soundSynth(WardenSounds.ENTOMB)
        player.frozen = true
        player.routeDestination.clear()
        for (step in LOOP_START..LOOP_END) {
            room.schedule(step) { if (player.entombed) player.anim(WardenSeqs.ENTOMB_LOOP) }
        }
        room.schedule(DURATION) { release(player) }
    }

    fun clear() {
        for (player in room.players) {
            if (player.entombed) player.entombed = false
            player.frozen = false
        }
    }

    private fun release(player: Player) {
        if (!player.entombed) return
        player.entombed = false
        player.frozen = false
        player.anim(WardenSeqs.ENTOMB_END)
        player.spotanim(WardenSpots.ENTOMB_END)
        player.soundSynth(WardenSounds.ENTOMB_RELEASE)
        player.soundSynth(WardenSounds.ENTOMB_END, delay = END_SOUND_DELAY)
    }

    private companion object {
        const val DURATION = 6
        const val LOOP_START = 2
        const val LOOP_END = 5
        const val END_SOUND_DELAY = 20
    }
}

private var Player.entombed by boolVarBit("varbit.toa_wardens_entombed")
