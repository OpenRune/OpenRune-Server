package org.rsmod.content.raids.toa.raid.encounter.apmeken.puzzle

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.midiSong
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class ApmekenPuzzleEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaEncounter(raid, room, session, controllerId) {

    internal val baboons = ApmekenBaboons(this)
    internal val sight = ApmekenSight(this)
    private val crateHammers = HashSet<Player>()

    override val combatantSpecs: List<ToaCombatant.Spec>
        get() =
            ApmekenNpcs.ALL.map {
                ToaCombatant.Spec(it, pointMultiplier = BABOON_POINTS, scaleHitpoints = false)
            }

    override fun onStart() {
        for (player in players) player.midiSong(ApmekenMidis.PATH)
        baboons.clear()
        sight.start(deps.mapClock.cycle)
    }

    override fun onTick(targets: List<Player>) {
        val now = deps.mapClock.cycle
        sight.tick(now, targets)
        baboons.tick(now, targets)
    }

    override fun onComplete() {
        baboons.clear()
        sight.clear()
        openExitBarrier()
        takeBackItems()
    }

    override fun onReset() {
        baboons.clear()
        sight.clear()
    }

    internal fun grantHammer(player: Player) {
        crateHammers += player
    }

    private fun takeBackItems() {
        val potion = ApmekenObjs.POTION.asRSCM(RSCMType.OBJ)
        for (player in players) {
            for (slot in player.inv.indices) {
                if (player.inv[slot]?.id == potion) player.inv[slot] = null
            }
            if (player in crateHammers) {
                player.invDel(player.inv, ApmekenObjs.HAMMER, 1, strict = false)
            }
        }
        crateHammers.clear()
    }

    private fun openExitBarrier() {
        val shape = LocShape.CentrepieceStraight
        for (tile in ApmekenCoords.EXIT_BARRIER) {
            val real = coords(tile)
            deps.locRepo.findExact(real, shape)?.let { deps.locRepo.del(it, Int.MAX_VALUE) }
            deps.locRepo.add(real, ApmekenLocs.BARRIER_OPEN, Int.MAX_VALUE, LocAngle.South, shape)
        }
    }

    internal fun ventIndexAt(player: Player): Int = staticIndex(player.coords, ApmekenCoords.VENTS)

    internal fun supportIndexAt(loc: CoordGrid): Int =
        staticIndex(loc, ApmekenCoords.SUPPORTS)

    private fun staticIndex(coords: CoordGrid, list: List<CoordGrid>): Int =
        list.indexOf(staticCoords(coords))

    private companion object {
        const val BABOON_POINTS = 1.2
    }
}
