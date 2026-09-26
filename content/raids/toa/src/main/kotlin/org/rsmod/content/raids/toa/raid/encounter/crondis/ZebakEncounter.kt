package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.raids.toa.raid.ToaPath
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.StepValidator

/**
 * Zebak, the Crondis boss. Port of Offline_Scape ZebakEncounter + Zebak, corrected by Jesse's
 * capture (zebak-capture-v2) and the OSRS Wiki.
 *
 * This class owns the fight's lifecycle, tick loop, scaling and special scheduling. Each mechanic
 * lives in its own component: [autos], [poison], [bloodMagic], [boulders], [jugs], [waves],
 * [water], plus the two specials [GreatRoar] and [TidalWaves].
 *
 * Zebak never uses the engine's combat: ZebakScript binds onAiOpPlayer2 to a no-op and his attacks
 * come from [tick]. Npc behaviour (NoMove, no regen, AI timers) is in toa_zebak.toml.
 */
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

    /** Offline_Scape TOANPC.damageFactor. */
    internal var damageFactor = 1.0
        private set

    internal var enraged = false
        private set

    internal var attackCountdown = 0

    private var pathAttackSpeed = BASE_ATTACK_SPEED
    private var special: ZebakSpecial? = null
    private var specialsQueued = 0
    private var specialsTriggered = 0
    private var nextSpecialIsRoar = false

    internal val usingSpecial: Boolean
        get() = special != null

    internal val attackSpeed: Int
        get() {
            special?.attackSpeed?.let { return it }
            val speed = if (enraged) pathAttackSpeed - ENRAGE_SPEEDUP else pathAttackSpeed
            return max(MIN_ATTACK_SPEED, speed)
        }

    internal val pathLevel: Int
        get() = raid.pathLevels[ToaPath.CRONDIS.ordinal]

    private val steps by lazy { StepValidator(deps.collision) }

    // ---- Lifecycle ----

    override fun onBuilt() {
        clearFight()
        spawnZebak()
        water.spawnCrocodiles()
    }

    override fun onStart() {
        val boss = zebak ?: return
        applyScaling(boss, teamSize)
        // Capture: size-9 blockers under Zebak and the tail from the challenge start.
        addLoc(ZebakLocs.BLOCKER, ZebakCoords.ZEBAK)
        addLoc(ZebakLocs.BLOCKER, ZebakCoords.TAIL)
        for (player in players) {
            openBar(player)
            player.midiSong(ZebakSynths.MIDI)
            player.toaDamageTakenCurrent = 0
        }
        attackCountdown = FIRST_ATTACK_DELAY
        bloodMagic.start()
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
        autos.forget(player)
        bloodMagic.forget(player)
    }

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
        endSpecial()
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
        zebak = boss
        tail = spawn(ZebakNpcs.TAIL, coords(ZebakCoords.TAIL))
        applyScaling(boss, raid.players.size.coerceAtLeast(1))
        enraged = false
        endSpecial()
        specialsQueued = 0
        specialsTriggered = 0
        nextSpecialIsRoar = deps.random.of(0, 1) == 0
    }

    private fun playDeath() {
        val boss = zebak ?: return
        if (!boss.isSlotAssigned) return
        owners.remove(boss)
        boss.noneMode()
        boss.hideAllOps()
        boss.anim(ZebakSeqs.DEATH)
        tail?.anim(ZebakSeqs.TAIL_DEATH)
        val tailNpc = tail
        schedule(DEATH_MODEL_DELAY) {
            if (boss.isSlotAssigned) boss.transmog(npcType(ZebakNpcs.ZEBAK_DEAD), Int.MAX_VALUE)
            if (tailNpc != null && tailNpc.isSlotAssigned) {
                tailNpc.transmog(npcType(ZebakNpcs.TAIL_DEAD), Int.MAX_VALUE)
            }
        }
    }

    // ---- Scaling (OSRS Wiki, Tombs of Amascut) ----

    /**
     * Raid level +0.4% per level; party +90% hp for players 2-3, +60% after (Offline_Scape: +90%
     * each); path level +8% then +5% per level (Offline_Scape, unverified). Hp rounds to the
     * nearest 10 (capture: 640 at raid level 25 solo). Accuracy scales via the combat levels.
     */
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

    internal fun maxHit(base: Int): Int = floor(base * damageFactor).toInt()

    // ---- Tick loop ----

    private fun tick() {
        if (stage != ToaStage.STARTED) return
        val boss = zebak ?: return
        val targets = targets()

        poison.tick(targets)
        autos.tickBleeding(targets)
        special?.let { if (!it.step()) endSpecial() }
        water.dropDeadSwimmers()

        if (targets.isNotEmpty() && boss.hitpoints > 0) {
            if (!usingSpecial) bloodMagic.tick()
            if (--attackCountdown <= 0) {
                attackCountdown = attackSpeed
                val started = !enraged && !usingSpecial && specialsQueued > 0 && startSpecial()
                if (!started) autos.attack(boss, targets)
            }
        }

        schedule(1) { tick() }
    }

    /** Players Zebak can hit: alive, not a ghost, inside the challenge area. */
    internal fun targets(): List<Player> =
        players.filter { inChallengeArea(it) && !raid.isGhost(it) && !raid.isDying(it) }

    // ---- Specials ----

    /** The queued special replaces an auto; they alternate, starting at random (Offline_Scape). */
    private fun startSpecial(): Boolean {
        specialsQueued--
        val next = if (nextSpecialIsRoar) GreatRoar(this) else TidalWaves(this)
        nextSpecialIsRoar = !nextSpecialIsRoar
        special = next
        if (!next.step()) endSpecial()
        return true
    }

    private fun endSpecial() {
        special = null
    }

    // ---- Zebak taking damage ----

    /** Capture: area sound 6590 on damage. Wiki: specials at 85/70/55/40%, enrage at ~25%. */
    private fun zebakHit(boss: Npc, hit: Hit) {
        if (stage != ToaStage.STARTED || boss !== zebak) return
        if (hit.damage > 0) {
            val centre = coords(ZebakCoords.CENTRE)
            deps.worldRepo.soundArea(centre, ZebakSynths.DAMAGED, radius = DAMAGED_SOUND_RADIUS)
        }
        updateBars()
        if (enraged || boss.hitpoints <= 0) return
        val max = boss.baseHitpointsLvl
        // Offline_Scape: at most one special queued per hit.
        val threshold = SPECIAL_THRESHOLDS.getOrNull(specialsTriggered)
        if (threshold != null && boss.hitpoints <= max * threshold) {
            specialsTriggered++
            specialsQueued++
        }
        if (boss.hitpoints <= max * ENRAGE_THRESHOLD) enrage(boss)
    }

    /** Wiki: the enraged npc, faster attacks. Offline_Scape: -3 attack speed, no more specials. */
    private fun enrage(boss: Npc) {
        enraged = true
        attackCountdown = min(attackCountdown, attackSpeed)
        boss.transmog(npcType(ZebakNpcs.ZEBAK_ENRAGED), Int.MAX_VALUE)
        for (player in targets()) player.soundSynth(ZebakSynths.FINAL_PHASE)
    }

    // ---- Boss bar ----

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

    // ---- Shared helpers for the components ----

    /** A room npc owned by this room (for event routing) that never respawns by itself. */
    internal fun spawn(type: String, tile: CoordGrid): Npc {
        val npc = Npc(type, tile)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        owners[npc] = this
        return npc
    }

    internal fun despawn(npc: Npc) {
        owners.remove(npc)
        if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun addLoc(type: String, static: CoordGrid) {
        val shape = LocShape.CentrepieceStraight
        deps.locRepo.add(coords(static), type, Int.MAX_VALUE, LocAngle.West, shape)
    }

    /** Walkable with no centrepiece loc (Offline_Scape: no type-10 object, floor free). */
    internal fun isOpenFloor(tile: CoordGrid): Boolean =
        !deps.collision.isWalkBlocked(tile) &&
            deps.locRepo.findExact(tile, LocShape.CentrepieceStraight) == null

    /** Open, acid-free tiles in a static rectangle, shuffled, in instance coords. */
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

    /** One step, walls between tiles included (Offline_Scape checkWalkStep). */
    internal fun canStep(from: CoordGrid, dx: Int, dz: Int): Boolean =
        steps.canTravel(from.level, from.x, from.z, dx, dz)

    /** Offline_Scape's knockbacks: move if [dest] differs, face [facing], push anim and [synth]. */
    @OptIn(InternalApi::class)
    internal fun pushPlayer(player: Player, dest: CoordGrid, facing: Direction, synth: String) {
        val moved = dest != player.coords
        if (!moved) player.abortRoute()
        deps.launcher.launchLenient(player) {
            if (moved) telejump(dest, TeleportType.Exempt)
            player.faceDirection(facing)
            anim(ZebakSeqs.PLAYER_PUSHED)
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

        /** Capture: first attack 10 ticks after "Challenge started" (Offline_Scape: 7). */
        private const val FIRST_ATTACK_DELAY = 10
        private const val DEATH_MODEL_DELAY = 3
        private const val DAMAGED_SOUND_RADIUS = 10
        private const val SCRIPT_SEQ_PREFETCH = 1846

        private val SPECIAL_THRESHOLDS = doubleArrayOf(0.85, 0.70, 0.55, 0.40)
        private const val ENRAGE_THRESHOLD = 0.25

        // ---- Event routing (ZebakScript, ZebakSwimAttackHook) ----

        private val owners = HashMap<Npc, ZebakEncounter>()

        private fun roomOf(npc: Npc): ZebakEncounter? {
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
            roomOf(jug)?.jugs?.shatter(jug, jug.coords)
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

        internal fun onClimbRock(player: Player, rock: CoordGrid, angleId: Int) {
            roomOf(player)?.water?.climbOut(player, rock, angleId)
        }

        internal fun isSwimming(player: Player): Boolean =
            roomOf(player)?.water?.isSwimming(player) == true

        /**
         * Capture: every damaging hit updates toa_damage_taken and toa_damage_taken_current (reset
         * at the challenge start). Read as the player's damage taken. TODO: confirm, and whether
         * other rooms do this too.
         */
        internal fun onPlayerDamaged(player: Player, damage: Int) {
            val room = roomOf(player) ?: return
            if (room.stage != ToaStage.STARTED) return
            player.toaDamageTaken = (player.toaDamageTaken + damage).coerceAtMost(TAKEN_MAX)
            player.toaDamageTakenCurrent =
                (player.toaDamageTakenCurrent + damage).coerceAtMost(TAKEN_CURRENT_MAX)
        }

        /** Their bit widths (osrs-dumps config/dump.varbit). */
        private const val TAKEN_MAX = 65_535
        private const val TAKEN_CURRENT_MAX = 32_767
    }
}

private var Player.toaDamageTaken by intVarBit("varbit.toa_damage_taken")
private var Player.toaDamageTakenCurrent by intVarBit("varbit.toa_damage_taken_current")
