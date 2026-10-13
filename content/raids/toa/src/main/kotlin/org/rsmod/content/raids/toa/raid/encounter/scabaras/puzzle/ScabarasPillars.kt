package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.map.CoordGrid

internal class ScabarasPillars(private val room: ScabarasPuzzleEncounter) {
    private val deps = room.raid.deps
    private val obelisks = ArrayList<Npc>()
    private val lit = BooleanArray(ScabarasCoords.OBELISKS.size)
    private var prelit: Npc? = null
    private var slot = -1
    private var order = emptyList<Int>()
    private var litCount = 0
    private var rockfallReady = 0

    private val done: Boolean
        get() = slot >= 0 && room.completed[slot]

    fun build(slot: Int) {
        this.slot = slot
        litCount = 0
        lit.fill(false)
        order = deps.random.shuffled(lit.indices.toList())
        for (offset in ScabarasCoords.OBELISKS) {
            obelisks += room.spawn(ScabarasNpcs.OBELISK, room.coords(room.plate(slot, offset)))
        }
        val fixed = room.plate(slot, ScabarasCoords.OBELISK_PRELIT)
        prelit = room.spawn(ScabarasNpcs.OBELISK_LIT, room.coords(fixed))
    }

    fun clear() {
        for (npc in obelisks) room.despawn(npc)
        obelisks.clear()
        prelit?.let(room::despawn)
        prelit = null
    }

    fun hit(npc: Npc, hit: Hit) {
        npc.hitpoints = npc.type.hitpoints
        if (!hit.isFromPlayer || done || !room.canUse(slot)) return
        val index = obelisks.indexOf(npc)
        if (index < 0 || lit[index]) return
        if (index != order[litCount]) return wrong()
        lit[index] = true
        litCount++
        npc.transmog(npcType(ScabarasNpcs.OBELISK_LIT), Int.MAX_VALUE)
        for (player in room.challengePlayers) player.soundSynth(ScabarasSynths.STEP)
        if (litCount == order.size) room.completeSlot(slot)
    }

    fun debugOrder(): List<Int> = order

    private fun wrong() {
        val now = deps.mapClock.cycle
        if (now < rockfallReady) return
        rockfallReady = now + ROCKFALL_COOLDOWN
        litCount = 0
        lit.fill(false)
        for (npc in obelisks) npc.resetTransmog()
        rockfall()
    }

    private fun rockfall() {
        val min = room.plate(slot, AREA_MIN)
        val max = room.plate(slot, AREA_MAX)
        val inArea = room.challengePlayers.filter { room.slotOf(it) == slot }
        val tiles = HashSet<CoordGrid>(room.freeTiles(min, max, emptyList()).take(ROCK_TILES))
        for (player in inArea) tiles += player.coords
        for (tile in tiles) {
            deps.worldRepo.spotanimMap(spotanim(ScabarasSpots.ROCKFALL), tile, 0, ROCK_GFX_DELAY)
        }
        for (player in inArea) {
            player.soundSynth(ScabarasSynths.ROCKFALL)
            room.shake(player)
        }
        room.schedule(ROCKFALL_DELAY) { damage(tiles) }
    }

    private fun damage(tiles: Set<CoordGrid>) {
        for (player in room.challengePlayers) {
            if (player.coords in tiles) room.hazardDamage(player, ROCK_DAMAGE, ROCK_SPREAD)
        }
    }

    private companion object {
        const val ROCKFALL_COOLDOWN = 5
        const val ROCKFALL_DELAY = 5
        const val ROCK_TILES = 7
        const val ROCK_GFX_DELAY = 20
        const val ROCK_DAMAGE = 7
        const val ROCK_SPREAD = 6
        val AREA_MIN = 1 to 1
        val AREA_MAX = 5 to 5
    }
}
