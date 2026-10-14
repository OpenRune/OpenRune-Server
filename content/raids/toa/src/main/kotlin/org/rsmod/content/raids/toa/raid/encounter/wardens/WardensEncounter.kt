package org.rsmod.content.raids.toa.raid.encounter.wardens

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.Arrival
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaFight
import org.rsmod.content.raids.toa.raid.encounter.ToaHpBar
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.isBeside
import org.rsmod.content.raids.toa.raid.travel
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
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
    private var obeliskRemaining = 0
    private var arriveAtGhost = false
    private val blast = WardensObeliskBlast(this)
    private var hudNpc: Npc? = null
    private var activeWarden: Npc? = null
    private var activeGod: WardenGod? = null
    private var wardenReady = false

    override val hpBar: ToaHpBar? by lazy {
        ToaHpBar(
            this,
            currentHp = { if (shielded(it)) it.baseHitpointsLvl - it.hitpoints else it.hitpoints },
            subject = { hudNpc ?: obelisk },
        )
    }

    override val fight =
        ToaFight(
            boss = { activeWarden ?: obelisk },
            combatants = listOf(ToaCombatant.Spec(OBELISK_INACTIVE)),
            bar = { hudNpc ?: obelisk },
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

    override fun canEngage(boss: Npc): Boolean = wardenReady && boss === activeWarden

    internal fun basicMaxHit(npc: Npc): Int = activeGod?.phaseTwoMaxHit ?: 0

    internal fun canMelee(target: Player): Boolean {
        val warden = activeWarden ?: return false
        return activeGod == WardenGod.TUMEKEN && warden.isBeside(target)
    }

    internal fun modifyWardenHit(npc: Npc, hit: HitBuilder) {
        if (npc !== activeWarden || !hit.isFromPlayer) return
        val god = activeGod ?: return
        if (!wardenReady || hit.type != vulnerableStyle(god, npc)) {
            hit.damage = 0
            return
        }
        hit.damage = min(hit.damage, npc.hitpoints - 1)
    }

    internal fun wardenHit(npc: Npc, hit: Hit) {
        if (npc !== activeWarden || stage != ToaStage.STARTED || !hit.isFromPlayer) return
        hpBar?.update()
        showShieldHeadbar(npc, hit)
    }

    private fun showShieldHeadbar(npc: Npc, hit: Hit) {
        if (!shielded(npc)) return
        val bar = ServerCacheManager.getHealthBar(SHIELD_HEADBAR.asRSCM(RSCMType.HEADBAR)) ?: return
        val taken = npc.baseHitpointsLvl - npc.hitpoints
        val fill = taken * bar.segments / npc.baseHitpointsLvl
        val mark = hit.hitmark
        npc.showHeadbar(
            Headbar.fromPlayerSource(
                self = bar.id,
                public = bar.id,
                startFill = fill,
                endFill = fill,
                startTime = mark.delay,
                endTime = mark.delay,
                slotId = mark.playerSlot,
            )
        )
    }

    private fun shielded(npc: Npc): Boolean =
        WardenGod.entries.any {
            npc.visType.id == npcType(it.phaseTwo).id || npc.visType.id == npcType(it.phaseTwoAlt).id
        }

    private fun vulnerableStyle(god: WardenGod, npc: Npc): HitType {
        val primary = npc.visType.id == npcType(god.phaseTwo).id
        if (primary) return god.phaseTwoStyle
        return if (god.phaseTwoStyle == HitType.Magic) HitType.Ranged else HitType.Magic
    }

    private fun debugTick(boss: Npc, targets: List<Player>) {
        if (deps.mapClock.cycle % 5 != 0) return
        val enc = deps.bossDeps.encounterRegistry
        println(
            "[wardens-debug] tick cycle=${deps.mapClock.cycle} boss=${boss.visType.id} " +
                "isActive=${boss === activeWarden} ready=$wardenReady hp=${boss.hitpoints} " +
                "ignoreCombat=${boss.ignoreCombatInteractions} mode=${boss.mode} " +
                "interaction=${boss.interaction} coords=${boss.coords} " +
                "attackRange=${boss.attackRange} moveRestrict=${boss.moveRestrict} " +
                "targets=${targets.size} canEngage=${canEngage(boss)} " +
                "playerCoords=${targets.map { it.coords }}"
        )
        if (boss === activeWarden && wardenReady) {
            val e = runCatching { enc.of(boss) }.getOrNull()
            println(
                "[wardens-debug] encounter=${e != null} phase=${e?.currentPhaseName} " +
                    "busyUntil=${e?.busyUntil} lastAbility=${e?.lastAbilityTick} " +
                    "rate=${e?.attackRateOverride} next=${e?.nextAttackTick}"
            )
        }
    }

    private fun readyWarden(warden: Npc) {
        println(
            "[wardens-debug] readyWarden hp=${warden.hitpoints}/${warden.baseHitpointsLvl} " +
                "slot=${warden.isSlotAssigned} type=${warden.type.id} vis=${warden.visType.id} " +
                "active=${warden === activeWarden} stage=$stage"
        )
        warden.ignoreCombatInteractions = false
        deps.bossDeps.encounterRegistry.remove(warden)
        val encounter = deps.bossDeps.encounter(warden)
        encounter.attackRateOverride = wardenAttackRate()
        println(
            "[wardens-debug] encounter created phase=${encounter.currentPhaseName} " +
                "rate=${encounter.attackRateOverride} abilities=${encounter.spec.abilities.keys}"
        )
        wardenReady = true
    }

    private fun wardenAttackRate(): Int =
        if (raid.isActive(ToaInvocationKey.Acceleration)) ACCELERATED_ATTACK_RATE else ATTACK_RATE

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
        boss.hitpoints = 1 // TEMP: skip the obelisk fight while testing the Wardens
        obeliskRemaining = boss.hitpoints
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
        if (!phaseOne) {
            debugTick(boss, targets)
            return
        }
        boss.ignoreCombatInteractions = true
        charging.tick(deps.mapClock.cycle - startCycle)
    }

    internal fun modifyObeliskHit(npc: Npc, hit: HitBuilder) {
        if (npc !== obelisk || !hit.isFromPlayer) return
        if (!phaseOne || capped) {
            hit.damage = 0
            return
        }
        hit.damage = min(hit.damage, obeliskRemaining - 1)
        obeliskRemaining -= hit.damage
        if (obeliskRemaining <= 1) capped = true
    }

    internal fun obeliskHit(npc: Npc, hit: Hit) {
        if (npc !== obelisk || stage != ToaStage.STARTED || !hit.isFromPlayer) return
        if (phaseOne) npc.hitpoints = obeliskRemaining
        hpBar?.update()
        if (capped && phaseOne) beginTransition()
    }

    private fun beginTransition() {
        phaseOne = false
        val loser = loser()
        val winner = loser.other
        charging.stop()
        val obelisk = obelisk ?: return
        obelisk.hitpoints = 0
        hpBar?.update()
        obelisk.transmog(npcType(OBELISK_P2), Int.MAX_VALUE)
        obelisk.ignoreCombatInteractions = true
        obelisk.anim("seq.fx_obelisk_wardens01_charge")
        blast.detonate(loser)
        schedule(WALK_DELAY) {
            warden(winner)?.let {
                it.moveRestrict = MoveRestrict.Normal
                it.walk(coords(winner.arenaTile))
            }
        }
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
                ToaCombatant(this, it, ToaCombatant.Spec(winner.phaseTwo))
                    .scale(teamSize, SHIELD_BASE_HP)
                hudNpc = it
                activeWarden = it
                activeGod = winner
                for (player in players) hpBar?.open(player)
                println("[wardens-debug] scheduling ready for ${winner} at cycle ${deps.mapClock.cycle}")
                schedule(READY_DELAY - TRANSITION_DELAY) { readyWarden(it) }
            }
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
        hudNpc = null
        activeWarden = null
        activeGod = null
        wardenReady = false

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
        private const val SHIELD_BASE_HP = 140
        private const val WALK_DELAY = 15
        private const val SHIELD_HEADBAR = "headbar.charge_140"
        private const val READY_DELAY = 22
        private const val ATTACK_RATE = 7
        private const val ACCELERATED_ATTACK_RATE = 6
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
