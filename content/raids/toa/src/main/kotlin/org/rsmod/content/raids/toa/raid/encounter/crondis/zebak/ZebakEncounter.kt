package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.ToaPath
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaBook
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaBossLoot
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.interact.InteractionPlayer
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.isWalkBlocked
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

    internal var damageFactor = 1.0
        private set

    internal var enraged = false
        private set

    internal var greatRoar: GreatRoar? = null
        private set

    internal var tidalWaves: TidalWaves? = null
        private set

    internal var nextSpecialIsRoar = false
        private set

    override val osmumtenDelay: Int = DEATH_MODEL_DELAY

    override val loot =
        ToaBossLoot(
            trackerNpc = ZebakNpcs.ZEBAK_DEAD,
            heroObj = ZebakObjs.FANG,
            book = ToaBook(ZebakObjs.BOOK, ZebakVarbits.BOOK_OWNED),
        )

    override val lootSource: Npc?
        get() = zebak

    override val roomPointsCap: Int = ZEBAK_POINTS_CAP

    private var defenceFloor = 0
    private var pathAttackSpeed = BASE_ATTACK_SPEED
    private var specialsQueued = 0
    private var specialsTriggered = 0
    private var hazardsDoneCycle = -1
    private val pendingAfterHazards = ArrayList<() -> Unit>()

    internal val specialRunning: Boolean
        get() = greatRoar != null || tidalWaves != null

    internal val attackSpeed: Int
        get() {
            val speed = if (enraged) pathAttackSpeed - ENRAGE_SPEEDUP else pathAttackSpeed
            return max(MIN_ATTACK_SPEED, speed)
        }

    internal val pathLevel: Int
        get() = raid.pathLevels[ToaPath.CRONDIS.ordinal]

    private val steps by lazy { StepValidator(deps.collision) }

    override fun onBuilt() {
        clearFight()
        spawnZebak()
        water.spawnCrocodiles()
    }

    override fun onStart() {
        val boss = zebak ?: return
        applyScaling(boss, teamSize)
        addLoc(ZebakLocs.BLOCKER, ZebakCoords.ZEBAK)
        addLoc(ZebakLocs.BLOCKER, ZebakCoords.TAIL)
        for (player in players) {
            openBar(player)
            player.midiSong(ZebakSynths.MIDI)
        }
        boss.ignoreCombatInteractions = false
        deps.bossDeps.encounterRegistry.remove(boss)
        deps.bossDeps.encounter(boss).attackRateOverride = attackSpeed
        deps.bossDeps.suppressAttacks(boss, FIRST_ATTACK_DELAY)
        bloodMagic.start()
        engage(boss, targets())
        schedule(1) { tick() }
    }

    override fun onEnter(player: Player) {
        for (seq in ZebakSeqs.PRELOAD) player.runClientScript(SCRIPT_SEQ_PREFETCH, seq)
        if (stage == ToaStage.STARTED) {
            openBar(player)
            player.midiSong(ZebakSynths.MIDI)
        }
    }

    override fun onLeave(player: Player) {
        closeBar(player)
        water.stopSwimming(player)
        zebak?.let { deps.bossDeps.bleeds.remove(it, player) }
        bloodMagic.forget(player)
    }

    override fun pointMultiplier(npc: Npc): Double = if (npc === zebak) ZEBAK_POINTS else 1.0

    override fun onComplete() {
        for (player in players) closeBar(player)
        clearFight()
        water.removeCrocodiles()
        playDeath()
        super.onComplete()
    }

    override fun onReset() {
        for (player in players) closeBar(player)
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
        applyScaling(boss, raid.players.size.coerceAtLeast(1))
        enraged = false
        specialsQueued = 0
        specialsTriggered = 0
        nextSpecialIsRoar = deps.random.of(0, 1) == 0
    }

    private fun lockFacingEast(npc: Npc) {
        npc.lockFacing(npc.coords.translate(npc.size, 0), targetWidth = 1, targetLength = npc.size)
    }

    private fun playDeath() {
        val boss = zebak ?: return
        if (!boss.isSlotAssigned) return
        owners.remove(boss)
        boss.ignoreCombatInteractions = true
        boss.noneMode()
        boss.hideAllOps()
        boss.anim(ZebakSeqs.DEATH)
        tail?.anim(ZebakSeqs.TAIL_DEATH)
        val tailNpc = tail
        schedule(DEATH_SHAKE_DELAY) {
            for (player in players) {
                Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, DEATH_SHAKE_LEFT_RIGHT, 0, 0)
                Camera.camShake(player, CamShakeAxis.UP_DOWN, DEATH_SHAKE_UP_DOWN, 0, 0)
                Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, DEATH_SHAKE_FORWARDS, 0, 0)
            }
        }
        schedule(DEATH_SHAKE_DELAY + 1) { for (player in players) Camera.camReset(player) }
        schedule(DEATH_MODEL_DELAY) {
            if (boss.isSlotAssigned) boss.transmog(npcType(ZebakNpcs.ZEBAK_DEAD), Int.MAX_VALUE)
            if (tailNpc != null && tailNpc.isSlotAssigned) {
                tailNpc.transmog(npcType(ZebakNpcs.TAIL_DEAD), Int.MAX_VALUE)
            }
        }
    }

    private fun applyScaling(npc: Npc, partySize: Int) {
        val type = npc.type
        val raidFactor = 1.0 + raid.settings.raidLevel * RAID_LEVEL_FACTOR
        val levelFactor =
            if (pathLevel > 0) PATH_LEVEL_FIRST + (pathLevel - 1) * PATH_LEVEL_EACH else 0.0
        val extra = (partySize - 1).coerceAtLeast(0)
        val first = min(extra, 2)
        val teamFactor = 1.0 + first * TEAM_HP_FIRST + (extra - first) * TEAM_HP_REST

        val hp = roundToTen(type.hitpoints * raidFactor * teamFactor * (1.0 + levelFactor))
        npc.baseHitpointsLvl = hp
        npc.hitpoints = hp
        val defence = floor(type.defence * raidFactor).toInt()
        npc.baseDefenceLvl = defence
        npc.defenceLvl = defence
        defenceFloor = floor((type.defence - MAX_DEFENCE_DRAIN) * raidFactor).toInt()
        val attack = floor(type.attack * raidFactor).toInt()
        npc.baseAttackLvl = attack
        npc.attackLvl = attack
        val ranged = floor(type.ranged * raidFactor).toInt()
        npc.baseRangedLvl = ranged
        npc.rangedLvl = ranged
        val magic = floor(type.magic * raidFactor).toInt()
        npc.baseMagicLvl = magic
        npc.magicLvl = magic

        damageFactor = min(MAX_DAMAGE_FACTOR, raidFactor + levelFactor)
        pathAttackSpeed = BASE_ATTACK_SPEED - min(2, pathLevel / 2)
    }

    private fun roundToTen(value: Double): Int = ((value + 5.0) / 10.0).toInt() * 10

    private fun holdDefenceFloor(boss: Npc) {
        if (boss.defenceLvl < defenceFloor) boss.defenceLvl = defenceFloor
    }

    internal fun maxHit(base: Int): Int = floor(base * damageFactor).toInt()

    internal fun rollScaled(base: Int): Int = deps.random.of(0, maxHit(base))

    internal fun rollScaled(min: Int, base: Int): Int = deps.random.of(min, maxHit(base).coerceAtLeast(min))

    private fun tick() {
        if (stage != ToaStage.STARTED) return
        val boss = zebak ?: return
        val targets = targets()
        val cycle = deps.mapClock.cycle
        holdDefenceFloor(boss)
        val landed = landings.take(cycle)
        if (targets.isNotEmpty() && boss.hitpoints > 0) for (action in landed) action()
        hazardsDoneCycle = cycle
        runAfterHazards()
        water.dropDeadSwimmers()
        engage(boss, targets)
        schedule(1) { tick() }
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

    private fun engage(boss: Npc, targets: List<Player>) {
        if (boss.hitpoints <= 0) return
        if (targets.isEmpty()) {
            if (boss.interaction != null) boss.noneMode()
            return
        }
        val current = (boss.interaction as? InteractionPlayer)?.target
        if (current != null && current in targets) return
        boss.apPlayer2(targets[deps.random.of(0, targets.lastIndex)], deps.aiInteractions)
    }

    internal fun targets(): List<Player> =
        players.filter { inChallengeArea(it) && !raid.isGhost(it) && !raid.isDying(it) }

    internal fun bossAlive(npc: Npc): Boolean =
        stage == ToaStage.STARTED && npc === zebak && npc.hitpoints > 0

    internal fun fighting(npc: Npc): Boolean = bossAlive(npc) && targets().isNotEmpty()

    private fun combatTick(access: StandardNpcAccess, target: Player) {
        if (!fighting(access.npc)) return
        if (bloodMagic.tick(paused = specialRunning)) {
            deps.bossDeps.runAbility(access, target, ZebakBoss.BLOOD_CAST)
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
        updateBars()
    }

    internal fun tailAnim(seq: String?) {
        val npc = tail ?: return
        if (seq == null) npc.resetAnim() else npc.anim(seq)
    }

    private fun zebakHit(boss: Npc, hit: Hit) {
        if (stage != ToaStage.STARTED || boss !== zebak) return
        holdDefenceFloor(boss)
        if (hit.damage > 0) {
            val centre = coords(ZebakCoords.CENTRE)
            deps.worldRepo.soundArea(centre, ZebakSynths.DAMAGED, radius = DAMAGED_SOUND_RADIUS)
        }
        updateBars()
        if (enraged || boss.hitpoints <= 0) return
        val threshold = SPECIAL_THRESHOLDS.getOrNull(specialsTriggered)
        if (threshold != null && boss.hitpoints <= boss.baseHitpointsLvl * threshold) {
            specialsTriggered++
            specialsQueued++
        }
    }

    private fun openBar(player: Player) {
        val npc = zebak ?: return
        deps.bossHpBar.onOpen(player, npc)
        deps.bossHpBar.onUpdate(player, npc)
    }

    internal fun updateBars() {
        val npc = zebak ?: return
        for (player in players) deps.bossHpBar.onUpdate(player, npc)
    }

    private fun closeBar(player: Player) {
        val npc = zebak ?: return
        deps.bossHpBar.onClose(player, npc, instant = true)
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
        updateBars()
        return null
    }

    internal fun spawn(type: String, tile: CoordGrid, facing: Direction? = null): Npc {
        val npc = Npc(type, tile)
        if (facing != null) npc.respawnDir = facing
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        adopt(npc)
        return npc
    }

    internal fun adopt(npc: Npc) {
        owners.values.removeIf { it.destroyed }
        npc.respawns = false
        owners[npc] = this
    }

    internal fun despawn(npc: Npc) {
        owners.remove(npc)
        if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun addLoc(type: String, static: CoordGrid) {
        val shape = LocShape.CentrepieceStraight
        deps.locRepo.add(coords(static), type, Int.MAX_VALUE, LocAngle.West, shape)
    }

    internal fun isOpenFloor(tile: CoordGrid): Boolean =
        !deps.collision.isWalkBlocked(tile) &&
            deps.locRepo.findExact(tile, LocShape.CentrepieceStraight) == null

    internal fun freeTiles(
        min: CoordGrid,
        max: CoordGrid,
        excludes: Collection<CoordGrid>,
    ): List<CoordGrid> {
        val from = coords(min)
        val to = coords(max)
        val tiles = ArrayList<CoordGrid>()
        for (x in from.x..to.x) {
            for (z in from.z..to.z) {
                val tile = CoordGrid(x, z, from.level)
                if (tile in excludes || tile in poison || !isOpenFloor(tile)) continue
                tiles += tile
            }
        }
        return deps.random.shuffled(tiles)
    }

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
        private const val RAID_LEVEL_FACTOR = 0.004
        private const val TEAM_HP_FIRST = 0.9
        private const val TEAM_HP_REST = 0.6
        private const val PATH_LEVEL_FIRST = 0.08
        private const val PATH_LEVEL_EACH = 0.05
        private const val MAX_DAMAGE_FACTOR = 2.5
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
        private const val SCRIPT_SEQ_PREFETCH = 1846
        private val SPECIAL_THRESHOLDS = doubleArrayOf(0.85, 0.70, 0.55, 0.40)
        private const val ENRAGE_THRESHOLD = 0.25
        private const val MAX_DEFENCE_DRAIN = 20
        private const val NOT_STARTED = "The fight hasn't started."

        private val owners = HashMap<Npc, ZebakEncounter>()

        internal fun roomOf(npc: Npc): ZebakEncounter? {
            val room = owners[npc] ?: return null
            if (room.destroyed) {
                owners.remove(npc)
                return null
            }
            return room
        }

        private fun roomOf(player: Player): ZebakEncounter? =
            player.currentRaid?.encounterOf(player) as? ZebakEncounter

        internal fun onZebakHit(npc: Npc, hit: Hit) {
            roomOf(npc)?.zebakHit(npc, hit)
        }

        internal fun onZebakDeath(npc: Npc) {
            val room = roomOf(npc) ?: return
            if (npc === room.zebak) room.complete()
        }

        internal fun onCombatTick(access: StandardNpcAccess, target: Player) {
            roomOf(access.npc)?.combatTick(access, target)
        }

        internal fun onCloudTick(npc: Npc) {
            roomOf(npc)?.bloodMagic?.cloudTick(npc)
        }

        internal fun onCloudDeath(npc: Npc) {
            roomOf(npc)?.bloodMagic?.removeCloud(npc)
        }

        internal fun onJugMoved(player: Player, jug: Npc, push: Boolean) {
            roomOf(jug)?.jugs?.move(player, jug, push)
        }

        internal fun onJugBroken(jug: Npc) {
            roomOf(jug)?.jugs?.hit(jug)
        }

        internal fun onJugAttack(player: Player, jug: Npc) {
            roomOf(jug)?.jugs?.attacking(player, jug)
        }

        internal fun onJugTick(jug: Npc) {
            roomOf(jug)?.jugs?.tick(jug)
        }

        internal fun onBoulderDeath(boulder: Npc) {
            roomOf(boulder)?.boulders?.remove(boulder)
        }

        internal fun onWaveTick(wave: Npc) {
            roomOf(wave)?.waves?.tick(wave)
        }

        internal fun onCrocTick(croc: Npc) {
            roomOf(croc)?.water?.crocodileTick(croc)
        }

        internal fun onCrocCombatTick(croc: Npc, target: Player): Boolean =
            roomOf(croc)?.water?.mayBite(target) ?: false

        internal fun onClimbRock(player: Player, rock: CoordGrid, angleId: Int) {
            roomOf(player)?.water?.climbOut(player, rock, angleId)
        }

        internal fun isSwimming(player: Player): Boolean =
            roomOf(player)?.water?.isSwimming(player) == true
    }
}
