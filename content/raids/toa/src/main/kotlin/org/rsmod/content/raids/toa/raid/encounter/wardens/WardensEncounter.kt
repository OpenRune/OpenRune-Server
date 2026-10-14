package org.rsmod.content.raids.toa.raid.encounter.wardens

import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.Arrival
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaFight
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.travel
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class WardensEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaBossEncounter(raid, room, session, controllerId) {

    internal var obelisk: Npc? = null
        private set

    private var osmumten: Npc? = null
    private val wardens = HashMap<WardenGod, Npc>()
    private val charging = WardensCharging(this)
    private var phaseOne = false
    private var capped = false
    private var arriveAtGhost = false

    override val fight =
        ToaFight(
            boss = { obelisk },
            combatants = listOf(ToaCombatant.Spec(OBELISK_INACTIVE)),
        )

    override fun arrival(): Arrival {
        if (!arriveAtGhost) return super.arrival()
        arriveAtGhost = false
        return Arrival(coords(GHOST_FRONT), Direction.South)
    }

    internal fun arriveInFrontOfGhost() {
        arriveAtGhost = true
    }

    override fun onBuilt() {
        spawnRoom()
    }

    override fun onReset() {
        spawnRoom()
    }

    override fun canEngage(boss: Npc): Boolean = false

    internal fun warden(god: WardenGod): Npc? = wardens[god]?.takeIf { it.isSlotAssigned }

    internal fun beginChallenge(player: Player) {
        if (stage != ToaStage.NOT_STARTED) return
        if (!inChallengeArea(player)) {
            player.mes("You need to be inside the challenge area to begin.")
            return
        }
        start()
    }

    override fun onFightStart(boss: Npc) {
        capped = false
        phaseOne = true
        boss.transmog(npcType(OBELISK_P1), Int.MAX_VALUE)
        for ((god, warden) in wardens) warden.transmog(npcType(god.active), Int.MAX_VALUE)
        osmumten?.let { ghost ->
            ghost.anim("seq.ghost_update_normal_despawn")
            schedule(OSMUMTEN_DESPAWN) { despawn(ghost) }
            osmumten = null
        }
        charging.start()
    }

    override fun onFightTick(boss: Npc, targets: List<Player>) {
        if (!phaseOne) return
        boss.ignoreCombatInteractions = true
        charging.tick(deps.mapClock.cycle - startCycle)
    }

    internal fun modifyObeliskHit(npc: Npc, hit: HitBuilder) {
        if (npc !== obelisk || !phaseOne || !hit.isFromPlayer) return
        if (capped) {
            hit.damage = 0
        } else if (hit.damage >= npc.hitpoints) {
            hit.damage = npc.hitpoints - 1
            capped = true
        }
    }

    internal fun obeliskHit(npc: Npc, hit: Hit) {
        if (npc !== obelisk || stage != ToaStage.STARTED || !hit.isFromPlayer) return
        hpBar?.update()
        if (capped && phaseOne) beginTransition()
    }

    private fun beginTransition() {
        phaseOne = false
        charging.stop()
        val obelisk = obelisk ?: return
        obelisk.transmog(npcType(OBELISK_P2), Int.MAX_VALUE)
        obelisk.ignoreCombatInteractions = true
        obelisk.anim("seq.fx_obelisk_wardens01_charge")
        val loser = loser()
        val winner = loser.other
        schedule(TRANSITION_DELAY) {
            for (player in players) {
                player.mes(
                    "<col=${winner.colour}>As ${loser.displayName} falls, " +
                        "${winner.displayName} powers up!</col>"
                )
            }
            warden(loser)?.let {
                it.anim("seq.npc_wardens_explosion01")
                it.spotanim("spotanim.spotanim_wardens01_explosion01_explode01")
                it.transmog(npcType(loser.destroyed), Int.MAX_VALUE)
            }
            warden(winner)?.let {
                it.anim("seq.npc_wardens_wake01")
                it.transmog(npcType(winner.phaseTwo), Int.MAX_VALUE)
            }
            hpBar?.update()
            chargeObelisk(obelisk)
        }
    }

    private fun chargeObelisk(obelisk: Npc) {
        val counts =
            if (raid.isActive(ToaInvocationKey.Acceleration)) ACCELERATED_CHARGE else ENTRY_CHARGE
        var delay = 0
        for ((seq, count) in CHARGE_SEQS.zip(counts)) {
            repeat(count) {
                schedule(delay) { if (obelisk.isSlotAssigned) obelisk.anim(seq) }
                delay += CHARGE_INTERVAL
            }
        }
    }

    private fun loser(): WardenGod {
        val elidinis = charging.lifetimeCharge(WardenGod.ELIDINIS)
        val tumeken = charging.lifetimeCharge(WardenGod.TUMEKEN)
        return when {
            elidinis < tumeken -> WardenGod.ELIDINIS
            tumeken < elidinis -> WardenGod.TUMEKEN
            else -> WardenGod.entries[deps.random.of(0, WardenGod.entries.lastIndex)]
        }
    }

    override fun debugComplete() {
        start()
        startFinalPhase()
    }

    @OptIn(InternalApi::class)
    fun startFinalPhase() {
        if (stage != ToaStage.STARTED) return
        val next = raid.advanceTo(ToaRoom.WARDENS_SECOND_ROOM) ?: return
        next.continueChallenge(this)
        stopTasks()
        for (player in players) {
            deps.launcher.launchLenient(player) { travel(next, fromLobby = false) }
        }
    }

    private fun spawnRoom() {
        charging.stop()
        osmumten?.let(::despawn)
        obelisk?.let(::despawn)
        for (warden in wardens.values) despawn(warden)
        wardens.clear()
        phaseOne = false
        capped = false

        val ghostTile = room.osmumtenTile
        if (ghostTile != null) {
            osmumten =
                spawn(OSMUMTEN, coords(ghostTile), Direction.North).also {
                    it.noneMode()
                }
        }
        obelisk =
            spawn(OBELISK_INACTIVE, coords(OBELISK_TILE)).also { it.ignoreCombatInteractions = true }
        for (god in WardenGod.entries) {
            wardens[god] =
                spawn(god.inactive, coords(god.tile), god.facing).also {
                    it.ignoreCombatInteractions = true
                }
        }
    }

    internal companion object {
        const val OSMUMTEN = "npc.toa_osmumten_wardens_vis"
        const val OBELISK_INACTIVE = "npc.toa_wardens_p1_obelisk_npc_inactive"
        const val OBELISK_P1 = "npc.toa_wardens_p1_obelisk_npc"
        const val OBELISK_P2 = "npc.toa_wardens_p2_obelisk_npc"

        private val OBELISK_TILE = CoordGrid(3807, 5153, 1)
        private val GHOST_FRONT = CoordGrid(3808, 5160, 1)
        private const val OSMUMTEN_DESPAWN = 2
        private const val TRANSITION_DELAY = 2
        private const val CHARGE_INTERVAL = 2
        private val CHARGE_SEQS =
            listOf(
                "seq.fx_obelisk_wardens01_charge_part1",
                "seq.fx_obelisk_wardens01_charge_part2",
                "seq.fx_obelisk_wardens01_charge_part3",
            )
        private val ENTRY_CHARGE = listOf(5, 9, 5)
        private val ACCELERATED_CHARGE = listOf(4, 4, 4)
    }
}
