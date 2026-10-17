package org.rsmod.content.raids.toa.raid.encounter.wardens

import dev.openrune.types.MoveRestrict
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.interrupt
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.Arrival
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaFight
import org.rsmod.content.raids.toa.raid.encounter.ToaHpBar
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.isBeside
import org.rsmod.content.raids.toa.raid.travelInFight
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
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
    internal var activeWarden: Npc? = null
        private set

    internal var activeGod: WardenGod? = null
        private set

    private var wardenReady = false
    private var lastAttackTick = 0
    private var attackDueTick = 0
    private var lastWasSpecial = false
    internal val entomb = WardensEntomb(this)
    internal val core = WardenCore(this)
    private val restoration = WardensRestoration(this)
    internal val obeliskCycle = WardensObelisk(this)

    override val hpBar: ToaHpBar? by lazy {
        ToaHpBar(
            this,
            currentHp = { core.hudHp(it) },
            subject = { hudNpc ?: obelisk },
        )
    }

    override val fight =
        ToaFight(
            boss = { activeWarden ?: obelisk },
            combatants =
                listOf(ToaCombatant.Spec(OBELISK_INACTIVE)) +
                    WardenGod.entries.map { ToaCombatant.Spec(it.inactive, pathScaled = false) },
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

    override fun canEngage(boss: Npc): Boolean =
        wardenReady && boss === activeWarden && core.state == WardenCore.State.SHIELD

    internal val entombPhase: Boolean
        get() = core.realFraction < ENTOMB_BELOW

    internal fun rollSpecial(): Boolean {
        lastAttackTick = deps.mapClock.cycle
        attackDueTick = lastAttackTick + wardenAttackRate()
        val special = !lastWasSpecial && deps.random.of(SPECIAL_ODDS) == 0
        lastWasSpecial = special
        return special
    }

    override fun validateAttack(player: Player, npc: Npc): NpcAttackValidateResult {
        if (entomb.isEntombed(player)) return NpcAttackValidateResult.Deny(ENTOMBED_ATTACK)
        return NpcAttackValidateResult.Pass
    }

    internal fun debugRun(player: Player, ability: String): String? {
        val warden = activeWarden ?: return "No active Warden."
        deps.bossDeps.runAbility(warden, player, ability)
        return null
    }

    internal fun basicMaxHit(npc: Npc): Int = activeGod?.phaseTwoMaxHit ?: 0

    internal fun canMelee(target: Player): Boolean {
        val warden = activeWarden ?: return false
        return activeGod == WardenGod.TUMEKEN && warden.isBeside(target)
    }

    internal fun modifyWardenHit(npc: Npc, hit: HitBuilder) {
        if (npc !== activeWarden || !hit.isFromPlayer) return
        val god = activeGod ?: return
        if (!wardenReady) {
            hit.damage = 0
            return
        }
        core.modify(npc, hit, vulnerableStyle(god, npc))
    }

    internal fun wardenDefeated(npc: Npc) {
        if (npc !== activeWarden) return
        val god = activeGod ?: return
        raid.wardensRestored = god.other
        core.defeated()
        restoration.begin(god, npc, core.coreTile)
    }

    internal fun wardenHit(npc: Npc, hit: Hit) {
        if (npc !== activeWarden || stage != ToaStage.STARTED || !hit.isFromPlayer) return
        hpBar?.update()
        activeGod?.let { core.afterHit(npc, it) }
    }

    private fun vulnerableStyle(god: WardenGod, npc: Npc): HitType {
        val primary = npc.visType.id == npcType(god.phaseTwo).id
        if (primary) return god.phaseTwoStyle
        return if (god.phaseTwoStyle == HitType.Magic) HitType.Ranged else HitType.Magic
    }

    internal fun p3Hitpoints(): Int =
        activeWarden
            ?.let(::combatantOf)
            ?.scaledRaidHitpoints(WardensSecondEncounter.P3_BASE_HP, teamSize, 1.0)
            ?: WardensSecondEncounter.P3_BASE_HP

    internal fun stopAttacking(warden: Npc) {
        deps.bossDeps.interrupt(warden)
        warden.noneMode()
    }

    internal fun resumeAttacking(warden: Npc, delay: Int) {
        deps.bossDeps.suppressAttacks(warden, delay)
    }

    private fun readyWarden(warden: Npc) {
        warden.ignoreCombatInteractions = false
        deps.bossDeps.encounterRegistry.remove(warden)
        val encounter = deps.bossDeps.encounter(warden)
        encounter.attackRateOverride = wardenAttackRate()
        lastWasSpecial = false
        lastAttackTick = Int.MIN_VALUE / 2
        attackDueTick = deps.mapClock.cycle + FIRST_ATTACK_LEAD
        wardenReady = true
    }

    private fun wardenAttackRate(): Int {
        val base = if (activeGod == WardenGod.TUMEKEN) TUMEKEN_ATTACK_RATE else ATTACK_RATE
        return if (raid.isActive("dbrow.toa_invocation_acceleration")) base - ACCELERATION_REDUCTION else base
    }

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
            obeliskCycle.tick()
            steerChase(boss)
            return
        }
        boss.ignoreCombatInteractions = true
        charging.tick(deps.mapClock.cycle - startCycle)
    }

    private fun steerChase(boss: Npc) {
        if (activeGod != WardenGod.TUMEKEN || !wardenReady || boss !== activeWarden) return
        if (core.state != WardenCore.State.SHIELD) return
        val interaction = boss.interaction ?: return
        val now = deps.mapClock.cycle
        val holding = now < lastAttackTick + CHASE_LOCK || now >= attackDueTick
        interaction.apRange = if (holding) boss.attackRange else MELEE_AP_RANGE
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
                val combatant = combatantOf(it)
                combatant?.scale(teamSize, SHIELD_BASE_HP)
                core.arm(it, combatant, teamSize)
                hudNpc = it
                activeWarden = it
                activeGod = winner
                for (player in players) hpBar?.open(player)
                schedule(READY_DELAY - TRANSITION_DELAY) { readyWarden(it) }
            }
            obeliskCycle.start()
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
        hpBar?.complete()
        startFinalPhase()
    }

    @OptIn(InternalApi::class)
    fun startFinalPhase() {
        if (stage != ToaStage.STARTED) return
        val next = raid.advanceTo(ToaRoom.WARDENS_SECOND_ROOM) ?: return
        next.continueChallenge(this)
        stopTasks()
        for (player in players) {
            deps.launcher.launchLenient(player) { travelInFight(next) }
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
        lastWasSpecial = false
        entomb.clear()
        core.clear()
        obeliskCycle.stop()

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
        private const val ENTOMB_BELOW = 0.55
        private const val SPECIAL_ODDS = 5
        private const val ENTOMBED_ATTACK = "You cannot attack while entombed in stone."
        private const val READY_DELAY = 22
        private const val FIRST_ATTACK_LEAD = 1
        private const val CHASE_LOCK = 4
        private const val MELEE_AP_RANGE = 1
        private const val ATTACK_RATE = 7
        private const val TUMEKEN_ATTACK_RATE = 8
        private const val ACCELERATION_REDUCTION = 1
    }
}
