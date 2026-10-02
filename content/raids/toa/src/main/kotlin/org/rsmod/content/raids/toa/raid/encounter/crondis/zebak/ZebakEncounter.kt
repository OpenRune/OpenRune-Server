package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import kotlin.math.max
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.HpThresholds
import org.rsmod.content.raids.toa.raid.encounter.ToaBook
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaBossLoot
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaDeath
import org.rsmod.content.raids.toa.raid.encounter.ToaFight
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.StepValidator

class ZebakEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaBossEncounter(raid, room, region, controllerId) {

    internal var zebak: Npc? = null
        private set

    internal var tail: Npc? = null
        private set

    internal val autos = ZebakAutos(this)
    internal val poison = ZebakPoison(this)
    internal val bloodMagic = ZebakBloodMagic(this)
    internal val boulders = ZebakBoulders(this)
    internal val jugs = ZebakJugs(this)
    internal val waves = ZebakWaves(this)
    internal val water = ZebakWater(this)
    internal val landings = ZebakLandings()

    internal var enraged = false
        private set

    internal var greatRoar: GreatRoar? = null
        private set

    internal var tidalWaves: TidalWaves? = null
        private set

    internal var nextSpecialIsRoar = false
        private set

    override val fight =
        ToaFight(
            boss = { zebak },
            combatants =
                listOf(
                    ToaCombatant.Spec(
                        ZebakNpcs.ZEBAK,
                        defenceCap = MAX_DEFENCE_DRAIN,
                        pointMultiplier = ZEBAK_POINTS,
                    )
                ),
            music = ZebakSynths.MIDI,
            firstAttackDelay = FIRST_ATTACK_DELAY,
            attackRate = { attackSpeed },
            death =
                ToaDeath(
                    anim = ZebakSeqs.DEATH,
                    dead = ZebakNpcs.ZEBAK_DEAD,
                    modelDelay = DEATH_MODEL_DELAY,
                    shake =
                        ToaDeath.Shake(
                            delay = DEATH_SHAKE_DELAY,
                            leftRight = DEATH_SHAKE_LEFT_RIGHT,
                            upDown = DEATH_SHAKE_UP_DOWN,
                            forwards = DEATH_SHAKE_FORWARDS,
                        ),
                    parts =
                        listOf(ToaDeath.Part({ tail }, ZebakSeqs.TAIL_DEATH, ZebakNpcs.TAIL_DEAD)),
                ),
        )

    override val loot =
        ToaBossLoot(
            trackerNpc = ZebakNpcs.ZEBAK_DEAD,
            heroObj = ZebakObjs.FANG,
            book = ToaBook(ZebakObjs.BOOK, ZebakVarbits.BOOK_OWNED),
        )

    override val roomPointsCap: Int = ZEBAK_POINTS_CAP

    private val specials = HpThresholds(SPECIAL_THRESHOLDS, catchUp = true)
    private var specialsQueued = 0
    private var hazardsDoneCycle = -1
    private val pendingAfterHazards = ArrayList<() -> Unit>()

    internal val specialRunning: Boolean
        get() = greatRoar != null || tidalWaves != null

    private val pathAttackSpeed: Int
        get() = BASE_ATTACK_SPEED - pathTier

    internal val attackSpeed: Int
        get() {
            val speed = if (enraged) pathAttackSpeed - ENRAGE_SPEEDUP else pathAttackSpeed
            return max(MIN_ATTACK_SPEED, speed)
        }

    private val steps by lazy { StepValidator(deps.collision) }

    override fun onBuilt() {
        clearFight()
        spawnZebak()
        water.spawnCrocodiles()
    }

    override fun onFightStart(boss: Npc) {
        addLoc(ZebakLocs.BLOCKER, ZebakCoords.ZEBAK)
        addLoc(ZebakLocs.BLOCKER, ZebakCoords.TAIL)
        bloodMagic.start()
    }

    override fun onLeave(player: Player) {
        water.stopSwimming(player)
        zebak?.let { deps.bossDeps.bleeds.remove(it, player) }
        bloodMagic.forget(player)
    }

    override fun onFightComplete() {
        clearFight()
        water.removeCrocodiles()
    }

    override fun validateAttack(player: Player, npc: Npc): NpcAttackValidateResult {
        if (water.isSwimming(player)) return NpcAttackValidateResult.Deny(SWIMMING_ATTACK)
        jugs.attacking(player, npc)
        return NpcAttackValidateResult.Pass
    }

    override fun onReset() {
        clearFight()
        spawnZebak()
    }

    private fun clearFight() {
        greatRoar = null
        tidalWaves = null
        hazardsDoneCycle = -1
        landings.clear()
        pendingAfterHazards.clear()
        zebak?.let { deps.bossDeps.bleeds.clear(it) }
        autos.clear()
        bloodMagic.clear()
        jugs.clear()
        boulders.clear()
        waves.clear()
        water.clear()
        poison.clear()
    }

    private fun spawnZebak() {
        zebak?.let(::despawn)
        tail?.let(::despawn)
        val boss = spawn(ZebakNpcs.ZEBAK, coords(ZebakCoords.ZEBAK))
        boss.ignoreCombatInteractions = true
        boss.apRequiresLineOfSight = false
        lockFacingEast(boss)
        zebak = boss
        tail = spawn(ZebakNpcs.TAIL, coords(ZebakCoords.TAIL))
        enraged = false
        specialsQueued = 0
        specials.reset()
        nextSpecialIsRoar = deps.random.of(0, 1) == 0
    }

    private fun lockFacingEast(npc: Npc) {
        npc.lockFacing(npc.coords.translate(npc.size, 0), targetWidth = 1, targetLength = npc.size)
    }

    internal fun maxHit(base: Int): Int = zebak?.let { combatantOf(it) }?.maxHit(base) ?: base

    override fun onFightTick(boss: Npc, targets: List<Player>) {
        val cycle = deps.mapClock.cycle
        val landed = landings.take(cycle)
        if (targets.isNotEmpty() && boss.hitpoints > 0) for (action in landed) action()
        hazardsDoneCycle = cycle
        runAfterHazards()
        water.dropDeadSwimmers()
    }

    internal fun afterHazards(action: () -> Unit) {
        if (hazardsDoneCycle == deps.mapClock.cycle) action() else pendingAfterHazards += action
    }

    private fun runAfterHazards() {
        if (pendingAfterHazards.isEmpty()) return
        val due = pendingAfterHazards.toList()
        pendingAfterHazards.clear()
        for (action in due) action()
    }

    internal fun combatTick(access: StandardNpcAccess, target: Player) {
        if (!fighting(access.npc)) return
        if (bloodMagic.tick(paused = specialRunning)) {
            deps.bossDeps.runAbility(access.npc, target, ZebakBoss.BLOOD_CAST)
        }
    }

    internal fun specialReady(npc: Npc): Boolean =
        fighting(npc) && !enraged && !specialRunning && specialsQueued > 0

    internal fun enrageDue(npc: Npc): Boolean =
        bossAlive(npc) && !enraged && npc.hitpoints <= npc.baseHitpointsLvl * ENRAGE_THRESHOLD

    internal fun beginSpecial(roar: Boolean) {
        specialsQueued = max(0, specialsQueued - 1)
        nextSpecialIsRoar = !roar
        if (roar) greatRoar = GreatRoar(this) else tidalWaves = TidalWaves(this)
    }

    internal fun endSpecial() {
        greatRoar = null
        tidalWaves = null
    }

    internal fun enrage(boss: Npc) {
        if (boss !== zebak || enraged) return
        val speed = attackSpeed
        enraged = true
        deps.bossDeps.encounter(boss).attackRateOverride = attackSpeed
        deps.bossDeps.suppressAttacks(boss, speed)
        hpBar?.update()
    }

    internal fun tailAnim(seq: String?) {
        val npc = tail ?: return
        if (seq == null) npc.resetAnim() else npc.anim(seq)
    }

    internal fun zebakHit(boss: Npc, hit: Hit) {
        if (stage != ToaStage.STARTED || boss !== zebak) return
        combatantOf(boss)?.holdDefenceFloor()
        if (hit.damage > 0) {
            val centre = coords(ZebakCoords.CENTRE)
            deps.worldRepo.soundArea(centre, ZebakSynths.DAMAGED, radius = DAMAGED_SOUND_RADIUS)
        }
        hpBar?.update()
        if (enraged || boss.hitpoints <= 0) return
        if (specials.crossed(boss)) specialsQueued++
    }

    internal fun debugSpecial(roar: Boolean): String? {
        val boss = zebak ?: return NOT_STARTED
        if (stage != ToaStage.STARTED) return NOT_STARTED
        if (enraged) return "Zebak is enraged: no more specials."
        if (specialRunning) return "A special is already running."
        if (specialsQueued > 0) return "A special is already queued."
        nextSpecialIsRoar = roar
        specialsQueued++
        deps.bossDeps.encounter(boss).lastAbilityTick = 0
        return null
    }

    internal fun debugBloodMagic(barrage: Boolean): String? {
        if (stage != ToaStage.STARTED) return NOT_STARTED
        bloodMagic.requestCast(barrage)
        return null
    }

    internal fun debugEnrage(): String? {
        val boss = zebak ?: return NOT_STARTED
        if (stage != ToaStage.STARTED) return NOT_STARTED
        if (enraged) return "Zebak is already enraged."
        boss.hitpoints = min(boss.hitpoints, (boss.baseHitpointsLvl * ENRAGE_THRESHOLD).toInt())
        hpBar?.update()
        return null
    }

    private fun addLoc(type: String, static: CoordGrid) {
        val shape = LocShape.CentrepieceStraight
        deps.locRepo.add(coords(static), type, Int.MAX_VALUE, LocAngle.West, shape)
    }

    override fun isTaken(tile: CoordGrid): Boolean = tile in poison

    internal fun canStep(from: CoordGrid, dx: Int, dz: Int): Boolean =
        steps.canTravel(from.level, from.x, from.z, dx, dz)

    @OptIn(InternalApi::class)
    internal fun pushPlayer(player: Player, dest: CoordGrid, facing: Direction, synth: String) {
        val moved = dest != player.coords
        if (!moved) player.abortRoute()
        deps.launcher.launchLenient(player) {
            if (moved) telejump(dest, TeleportType.Exempt)
            player.faceDirection(facing)
            player.combatPlayDefendAnim()
        }
        player.soundSynth(synth)
    }

    companion object {
        private const val BASE_ATTACK_SPEED = 7
        private const val MIN_ATTACK_SPEED = 2
        private const val ENRAGE_SPEEDUP = 3
        private const val FIRST_ATTACK_DELAY = 10
        private const val ZEBAK_POINTS = 1.5
        private const val ZEBAK_POINTS_CAP = 10_000
        private const val DEATH_SHAKE_DELAY = 2
        private const val DEATH_SHAKE_LEFT_RIGHT = 5
        private const val DEATH_SHAKE_UP_DOWN = 5
        private const val DEATH_SHAKE_FORWARDS = 2
        private const val DEATH_MODEL_DELAY = 3
        private const val DAMAGED_SOUND_RADIUS = 10
        private val SPECIAL_THRESHOLDS = doubleArrayOf(0.85, 0.70, 0.55, 0.40)
        private const val ENRAGE_THRESHOLD = 0.25
        private const val MAX_DEFENCE_DRAIN = 20
        private const val NOT_STARTED = "The fight hasn't started."
        private const val SWIMMING_ATTACK =
            "I can't hit him from here. I'll have to get back onto the island!"
    }
}
