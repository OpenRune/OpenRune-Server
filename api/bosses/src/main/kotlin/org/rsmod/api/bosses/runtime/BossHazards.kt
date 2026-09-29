package org.rsmod.api.bosses.runtime

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid

@Singleton
class BossHazards
@Inject
constructor(
    private val locRepo: LocRepository,
    private val playerList: PlayerList,
    private val mapClock: MapClock,
    private val worldQueues: WorldQueueList,
) {
    private val fields = HashMap<Npc, HashMap<CoordGrid, Hazard>>()

    private class Hazard(
        val loc: LocInfo,
        val placedAt: Int,
        val armDelay: Int,
        val duration: Int,
        val onStand: (Player) -> Unit,
    )

    fun place(
        owner: Npc,
        tile: CoordGrid,
        loc: String,
        armDelay: Int = 1,
        duration: Int = Int.MAX_VALUE,
        angle: LocAngle = LocAngle.West,
        onStand: (Player) -> Unit,
    ): Boolean {
        require(armDelay > 0) { "`armDelay` must be greater than 0. (armDelay=$armDelay)" }
        require(duration > 0) { "`duration` must be greater than 0. (duration=$duration)" }
        val field = fields.getOrPut(owner) { HashMap<CoordGrid, Hazard>().also { schedule(owner, it) } }
        if (tile in field) return false
        val info = locRepo.add(tile, loc, Int.MAX_VALUE, angle, LocShape.CentrepieceStraight)
        field[tile] = Hazard(info, mapClock.cycle, armDelay, duration, onStand)
        return true
    }

    fun contains(owner: Npc, tile: CoordGrid): Boolean = fields[owner]?.containsKey(tile) == true

    fun remove(owner: Npc, tile: CoordGrid): Boolean {
        val hazard = fields[owner]?.remove(tile) ?: return false
        locRepo.del(hazard.loc, Int.MAX_VALUE)
        return true
    }

    fun clear(owner: Npc) {
        val field = fields.remove(owner) ?: return
        for (hazard in field.values) locRepo.del(hazard.loc, Int.MAX_VALUE)
    }

    private fun schedule(owner: Npc, field: HashMap<CoordGrid, Hazard>) {
        worldQueues.add(1) { tick(owner, field) }
    }

    private fun tick(owner: Npc, field: HashMap<CoordGrid, Hazard>) {
        if (fields[owner] !== field) return
        if (!owner.isSlotAssigned) {
            clear(owner)
            return
        }
        val now = mapClock.cycle
        val iterator = field.values.iterator()
        while (iterator.hasNext()) {
            val hazard = iterator.next()
            if (now - hazard.placedAt < hazard.duration) continue
            iterator.remove()
            locRepo.del(hazard.loc, Int.MAX_VALUE)
        }
        if (field.isEmpty()) {
            fields.remove(owner)
            return
        }
        for (player in playerList) {
            if (player.hitpoints <= 0) continue
            val hazard = field[player.coords] ?: continue
            if (now - hazard.placedAt < hazard.armDelay) continue
            hazard.onStand(player)
        }
        schedule(owner, field)
    }
}
