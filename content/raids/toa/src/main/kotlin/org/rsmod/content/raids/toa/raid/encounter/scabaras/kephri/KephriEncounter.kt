package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.stat.statHeal
import org.rsmod.api.npc.vars.boolVarn
import org.rsmod.api.player.midiSong
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaBook
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaBossLoot
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaDeath
import org.rsmod.content.raids.toa.raid.encounter.ToaFight
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class KephriEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaBossEncounter(raid, room, session, controllerId) {

    internal var kephri: Npc? = null
        private set

    private var breakPending = false

    internal val swarms = KephriSwarms(this)
    private var wokeCycle = NEVER
    internal val dung = KephriDung(this)
    internal val eggs = KephriEggs(this)
    internal val agile = KephriAgile(this)
    internal val guardians = KephriGuardians(this)

    private val phaseName: String
        get() = kephri?.let { deps.bossDeps.encounter(it).currentPhaseName } ?: KephriPhases.OPENING

    internal val isStunned: Boolean
        get() = phaseName in KephriPhases.STUNS

    private val isFinal: Boolean
        get() = phaseName == KephriPhases.FINAL

    internal fun bombWindow(): Boolean {
        if (stage != ToaStage.STARTED || isFinal) return false
        return isStunned || (wokeCycle != NEVER && deps.mapClock.cycle - wokeCycle <= BOMB_WAKE_GRACE)
    }

    internal fun eggsDue(boss: Npc): Boolean =
        boss.eggsNext && agile.count <= AGILE_CAP_BASE + teamSize

    internal fun startEggs(boss: Npc) = eggs.begin(boss)

    private fun transitionTo(boss: Npc, phase: String) =
        deps.bossDeps.encounter(boss).transitionTo(phase, deps.mapClock.cycle)

    override val fight =
        ToaFight(
            boss = { kephri },
            combatants =
                listOf(
                    ToaCombatant.Spec(KephriNpcs.SHIELDED),
                    ToaCombatant.Spec(KephriNpcs.GUARDIAN_MELEE, GUARDIAN_POINTS),
                    ToaCombatant.Spec(KephriNpcs.GUARDIAN_RANGED, GUARDIAN_POINTS),
                    ToaCombatant.Spec(KephriNpcs.GUARDIAN_MAGE, GUARDIAN_POINTS),
                ),
            music = KephriMidis.FIGHT,
            firstAttackDelay = FIRST_ATTACK_DELAY,
            attackRate = { ATTACK_RATE },
            death =
                ToaDeath(anim = KephriSeqs.DEATH, dead = KephriNpcs.DEAD, modelDelay = 0),
        )

    override val loot =
        ToaBossLoot(
            trackerNpc = KephriNpcs.DEAD,
            heroObj = KephriObjs.DUNG,
            book = ToaBook(KephriObjs.BOOK, KephriVarbits.BOOK_OWNED),
        )

    override fun onBuilt() {
        spawnKephri()
    }

    override fun onReset() {
        spawnKephri()
    }

    override fun onFightStart(boss: Npc) {
        resetState()
        swarms.startMedic()
        deps.locRepo.add(
            coords(KephriCoords.KEPHRI),
            KephriLocs.BLOCKER,
            Int.MAX_VALUE,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
    }

    override fun canEngage(boss: Npc): Boolean = !isStunned

    override fun onFightTick(boss: Npc, targets: List<Player>) {
        swarms.tick()
        dung.tick()
        agile.tick()
        guardians.tick()
    }

    override fun onFightComplete() {
        swarms.clear()
        dung.replaceWithInvisible()
        eggs.clear()
        agile.clear()
        guardians.clear()
    }

    internal fun maxHit(base: Int): Int = kephri?.let { combatantOf(it) }?.maxHit(base) ?: base

    internal fun healKephri(amount: Int) {
        val boss = kephri ?: return
        if (boss.hitpoints <= 0) return
        val base = boss.baseHitpointsLvl
        val cap = if (isFinal) base else base * OVERHEAL_PERCENT / 100
        val before = boss.hitpoints
        boss.hitpoints = (before + amount).coerceAtMost(cap)
        val healed = boss.hitpoints - before
        if (healed > 0) {
            val mark = HEAL_HITMARK.asRSCM(RSCMType.HITMARK)
            boss.showHitmark(Hitmark.fromNoSource(mark, mark, mark, healed, 0))
        }
        hpBar?.update()
    }

    internal fun shadow(centre: CoordGrid, spot: String) {
        val tiles =
            if (raid.isActive(ToaInvocationKey.AerialAssault)) {
                (-1..1).flatMap { dx -> (-1..1).map { dz -> centre.translate(dx, dz) } }
                    .filter { it == centre || isOpenFloor(it) }
            } else {
                listOf(centre)
            }
        for (tile in tiles) deps.worldRepo.spotanimMap(spotanim(spot), tile)
    }

    internal fun modifyKephriHit(boss: Npc, hit: HitBuilder) {
        if (boss !== kephri || isStunned || isFinal || hit.damage < boss.hitpoints) return
        hit.damage = (boss.hitpoints - 1).coerceAtLeast(0)
        breakPending = true
    }

    internal fun kephriHit(boss: Npc) {
        if (stage != ToaStage.STARTED || boss !== kephri) return
        hpBar?.update()
        if (!breakPending) return
        breakPending = false
        breakShield(boss)
    }

    internal fun kephriDeath(boss: Npc) {
        if (boss === kephri) complete()
    }

    private fun breakShield(boss: Npc) {
        val next = KephriPhases.AFTER_BREAK[phaseName] ?: return
        if (next == KephriPhases.FINAL) enterFinal(boss) else enterStun(boss, next)
    }

    private fun enterStun(boss: Npc, phase: String) {
        val stun = KephriPhases.STUNS.indexOf(phase) + 1
        transitionTo(boss, phase)
        boss.anim(KephriSeqs.STUNNED)
        boss.spotanim(KephriSpots.SHIELD_DEATH)
        boss.hitpoints = 1
        boss.noneMode()
        deps.bossDeps.encounter(boss).invulnerable = true
        hpBar?.update()
        dung.cancel()
        val ticks = STUN_TICKS.getValue(phase)
        swarms.begin(stun, ticks)
        guardians.spawnFor(stun)
        schedule(ticks) { wake(boss) }
    }

    private fun wake(boss: Npc) {
        if (boss !== kephri) return
        val next = KephriPhases.AFTER_STUN[phaseName] ?: return
        transitionTo(boss, next)
        boss.anim(KephriSeqs.WAKE)
        boss.spotanim(KephriSpots.SHIELD_REFORM)
        deps.bossDeps.encounter(boss).invulnerable = false
        wokeCycle = deps.mapClock.cycle
        deps.bossDeps.suppressAttacks(boss, WAKE_ATTACK_DELAY)
    }

    private fun enterFinal(boss: Npc) {
        transitionTo(boss, KephriPhases.FINAL)
        boss.spotanim(KephriSpots.SHIELD_DEATH)
        combatantOf(boss)?.scale(teamSize, npcType(KephriNpcs.ENRAGE).hitpoints)
        boss.statHeal(DEFENCE, 0, FULL_RESTORE)
        for (player in players) player.midiSong(KephriMidis.FIGHT)
        targets().firstOrNull()?.let { deps.bossDeps.runAbility(boss, it, KephriAbilities.FINAL_DUNG) }
        deps.bossDeps.suppressAttacks(boss, ATTACK_RATE)
        hpBar?.update()
        hpBar?.recolour()
    }

    private fun spawnKephri() {
        kephri?.let(::despawn)
        val boss = spawn(KephriNpcs.SHIELDED, coords(KephriCoords.KEPHRI), Direction.West)
        boss.ignoreCombatInteractions = true
        boss.apRequiresLineOfSight = false
        kephri = boss
        resetState()
    }

    private fun resetState() {
        wokeCycle = NEVER
        breakPending = false
        swarms.clear()
        dung.clear()
        eggs.clear()
        agile.clear()
        guardians.clear()
        kephri?.eggsNext = false
    }

    internal fun debugBreak(): String? {
        val boss = kephri
        if (stage != ToaStage.STARTED || boss == null) return NOT_STARTED
        if (isStunned) return "Kephri is already stunned."
        if (isFinal) return "Kephri is already in the final phase."
        boss.hitpoints = 1
        breakPending = true
        kephriHit(boss)
        return null
    }

    internal fun debugMechanic(name: String, player: Player): String? {
        val boss = kephri
        if (stage != ToaStage.STARTED || boss == null) return NOT_STARTED
        when (name) {
            "dung" -> deps.bossDeps.runAbility(boss, player, KephriAbilities.DUNG)
            "eggs" -> {
                boss.anim(KephriSeqs.THROW)
                eggs.begin(boss)
            }
            "bomb" -> deps.bossDeps.runAbility(boss, player, KephriAbilities.BOMBS)
            "swarm" -> swarms.spawnMedic()
            "spitting" -> guardians.spawnNow(KephriGuardians.Kind.SPITTING)
            "soldier" -> guardians.spawnNow(KephriGuardians.Kind.SOLDIER)
            "arcane" -> guardians.spawnNow(KephriGuardians.Kind.ARCANE)
            "agile" -> agile.hatch(boss.coords.translate(-2, 2))
            "final" -> {
                if (isFinal) return "Kephri is already in the final phase."
                enterFinal(boss)
            }
            else -> return "Unknown mechanic."
        }
        return null
    }

    internal fun debugShield(percent: Int): String? {
        val boss = kephri
        if (stage != ToaStage.STARTED || boss == null) return NOT_STARTED
        boss.hitpoints = (boss.baseHitpointsLvl * percent / 100).coerceIn(1, boss.baseHitpointsLvl)
        hpBar?.update()
        return null
    }

    private companion object {
        const val ATTACK_RATE = 6
        const val FIRST_ATTACK_DELAY = 5
        const val AGILE_CAP_BASE = 4
        const val NEVER = Int.MIN_VALUE
        const val BOMB_WAKE_GRACE = 2
        const val GUARDIAN_POINTS = 0.5
        const val OVERHEAL_PERCENT = 115
        const val HEAL_HITMARK = "hitmark.alt_charge_tint"

        const val WAKE_ATTACK_DELAY = 7
        val STUN_TICKS = mapOf(KephriPhases.STUN_1 to 54, KephriPhases.STUN_2 to 51)
        const val DEFENCE = "stat.defence"
        const val FULL_RESTORE = 100
        const val NOT_STARTED = "The fight isn't running."
    }
}

private var Npc.eggsNext by boolVarn(KephriVarns.EGGS_NEXT)
