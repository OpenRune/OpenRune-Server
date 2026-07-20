package org.rsmod.content.skills.shootingstars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.random.Random
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.table.ShootingStarLocationsRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

val MINING_STAR = AttributeKey<Boolean>()
val DISCOVERER_BONUS_REMAINING =
    AttributeKey<Int>(persistenceKey = "shooting_star_discoverer_bonus")
val ALL_TIME_TOTAL_DUST = AttributeKey<Int>(persistenceKey = "shooting_star_all_time_dust")
val SEEN_SHOOTING_STAR = AttributeKey<Boolean>(persistenceKey = "shooting_star_seen")

@Singleton
class ShootingStarManager
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val playerList: PlayerList,
    private val mapClock: MapClock,
    private val collision: CollisionFlagMap,
) {
    var active = false
        private set

    var currentLocation: ShootingStarLocationsRow? = null
        private set

    var nextLocation: ShootingStarLocationsRow? = null
        private set

    var stageIndex = 0
        private set

    var discoveredBy: String? = null
        private set

    private var starLoc: LocInfo? = null
    private var landingLoc: LocInfo? = null
    private var headbarNpc: Npc? = null
    private var shadowNpc: Npc? = null
    private var nextSpawnCycle = 0
    private var bootSpawnCycle = 0
    private var bootSpawned = false
    private var impactEndCycle = 0
    private var crashWarningCycle = 0
    private var pendingCrashLocation: ShootingStarLocationsRow? = null
    private var layerStartedCycle = 0
    private var layerMiningStarted = false
    private var underLevelProgressCycles = 0
    private var settings: ShootingstarsSettings = ShootingstarsSettings()

    fun bindSettings(settings: ShootingstarsSettings) {
        this.settings = settings
    }

    fun currentStage(): ShootingStarStage = ShootingStarStages.ALL[stageIndex]

    fun percentageToNextLevel(): Int {
        val elapsed = layerElapsedCycles()
        return ((elapsed.toDouble() / ShootingStarStages.LAYER_DURATION_CYCLES) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    fun isActiveStar(loc: BoundLocInfo): Boolean {
        if (!active) return false
        val current = starLoc ?: return false
        return loc.coords == current.coords && loc.id == current.id
    }

    fun isActiveStarCoords(coords: CoordGrid): Boolean = active && starLoc?.coords == coords

    fun currentBoundLoc(): BoundLocInfo? {
        val loc = starLoc ?: return null
        val type = ServerCacheManager.getObject(loc.id) ?: return null
        return BoundLocInfo(loc, type)
    }

    /** Free standable tile near the active star (not on the star itself). */
    fun teleportDestination(): CoordGrid? {
        val centre = currentLocation?.coords ?: return null
        val starSize = currentBoundLoc()?.let { maxOf(it.width, it.length) }?.coerceAtLeast(1) ?: 2

        for (radius in starSize..TELEPORT_SEARCH_RADIUS) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) != radius) {
                        continue
                    }
                    val tile = centre.translate(dx, dz)
                    if (!isStandableTeleportTile(tile)) {
                        continue
                    }
                    return tile
                }
            }
        }
        return null
    }

    private fun isStandableTeleportTile(tile: CoordGrid): Boolean {
        if (!collision.isZoneValid(tile) || collision.isWalkBlocked(tile)) {
            return false
        }
        // Avoid standing on the star footprint.
        val star = starLoc ?: return true
        val type = ServerCacheManager.getObject(star.id) ?: return tile != star.coords
        val bound = BoundLocInfo(star, type)
        val onStar =
            tile.x in bound.coords.x until (bound.coords.x + bound.width) &&
                tile.z in bound.coords.z until (bound.coords.z + bound.length)
        return !onStar
    }

    fun tick() {
        if (!bootSpawned) {
            if (bootSpawnCycle == 0) {
                bootSpawnCycle = mapClock.cycle + settings.rollBootSpawnDelayCycles()
                scheduleCrashWarning(bootSpawnCycle)
            }
            if (crashWarningCycle > 0 && mapClock.cycle >= crashWarningCycle) {
                crashWarningCycle = 0
                broadcast(CRASH_WARNING_MESSAGE)
            }
            if (mapClock.cycle >= bootSpawnCycle) {
                bootSpawned = true
                beginCrash(forced = null)
                return
            }
        }

        if (crashWarningCycle > 0 && mapClock.cycle >= crashWarningCycle) {
            crashWarningCycle = 0
            broadcast(CRASH_WARNING_MESSAGE)
        }

        val pending = pendingCrashLocation
        if (pending != null && shadowNpc != null) {
            advanceShadow(pending)
            return
        }
        if (pending != null && impactEndCycle > 0 && mapClock.cycle >= impactEndCycle) {
            impactEndCycle = 0
            pendingCrashLocation = null
            finishCrash(pending)
            return
        }

        if (active && layerMiningStarted) {
            updateHeadbar()
            if (layerElapsedCycles() >= ShootingStarStages.LAYER_DURATION_CYCLES) {
                advanceStage()
            }
        }

        if (nextSpawnCycle > 0 && mapClock.cycle >= nextSpawnCycle) {
            nextSpawnCycle = 0
            beginCrash(forced = nextLocation)
        }
    }

    fun spawnStar(forced: ShootingStarLocationsRow? = null) {
        crashWarningCycle = 0
        cancelCrashIntro()
        beginCrash(forced)
    }

    private fun beginCrash(forced: ShootingStarLocationsRow?) {
        cancelCrashIntro()
        val location = pickLocation(forced)
        pendingCrashLocation = location
        crashWarningCycle = 0

        worldRepo.soundArea(
            location.coords,
            CRASH_WHISTLE_SYNTH,
            delay = 0,
            loops = 1,
            radius = CRASH_SOUND_RADIUS,
        )

        val type =
            ServerCacheManager.getNpc("npc.star_invisible_2x2_npc".asRSCM(RSCMType.NPC))
                ?: run {
                    startLandingImpact(location)
                    return
                }

        // Illerai: spawn Shadow 6 tiles west, then fly it into the crash site.
        val start = location.coords.translateX(-SHADOW_APPROACH_TILES)
        val npc = Npc(type, start)
        npc.mode = NpcMode.None
        npc.moveRestrict = MoveRestrict.PassThru
        npcRepo.add(npc, Int.MAX_VALUE)
        shadowNpc = npc
    }

    private fun advanceShadow(location: ShootingStarLocationsRow) {
        val npc = shadowNpc ?: return
        val dest = location.coords
        if (npc.coords == dest) {
            onShadowArrived(location)
            return
        }

        val dx = (dest.x - npc.coords.x).coerceIn(-1, 1)
        val dz = (dest.z - npc.coords.z).coerceIn(-1, 1)
        // Ignore collision so the shadow "flies" through the sky like Illerai.
        npc.teleport(collision, npc.coords.translate(dx, dz))

        if (npc.coords == dest) {
            onShadowArrived(location)
        }
    }

    private fun onShadowArrived(location: ShootingStarLocationsRow) {
        if (pendingCrashLocation?.rowId != location.rowId) return
        removeShadowNpc()
        startLandingImpact(location)
    }

    private fun startLandingImpact(location: ShootingStarLocationsRow) {
        val coords = location.coords
        landingLoc =
            locRepo.add(
                coords,
                LANDING_LOC,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        landingLoc?.let { worldRepo.locAnim(it, CRASH_IMPACT_ANIM) }
        worldRepo.soundArea(
            coords,
            CRASH_IMPACT_SYNTH,
            delay = 0,
            loops = 1,
            radius = CRASH_SOUND_RADIUS,
        )

        // tickDuration is game cycles; totalDelay is client frames and must not be used here.
        val animDelay =
            ServerCacheManager.getAnim(CRASH_IMPACT_ANIM.asRSCM(RSCMType.SEQ))
                ?.tickDuration
                ?.takeIf { it > 0 }
                ?: CRASH_IMPACT_FALLBACK_CYCLES
        impactEndCycle = mapClock.cycle + animDelay.coerceIn(1, CRASH_IMPACT_MAX_CYCLES)
    }

    private fun finishCrash(location: ShootingStarLocationsRow) {
        removeLandingLoc()
        removeShadowNpc()

        if (active) {
            clearStar(announce = true, replacedByNewStar = true)
        }

        discoveredBy = null
        stageIndex = ShootingStarStages.rollInitialStageIndex()
        layerMiningStarted = false
        layerStartedCycle = 0
        underLevelProgressCycles = 0
        active = true
        currentLocation = location
        nextLocation = null

        val coords = location.coords
        starLoc =
            locRepo.add(
                coords,
                currentStage().loc,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        spawnHeadbarNpc(coords)
        scheduleNextWave()
        broadcast("A shooting star just crashed ${location.desc}!")
    }

    private fun cancelCrashIntro() {
        pendingCrashLocation = null
        impactEndCycle = 0
        removeShadowNpc()
        removeLandingLoc()
    }

    private fun removeShadowNpc() {
        val npc = shadowNpc ?: return
        shadowNpc = null
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun removeLandingLoc() {
        landingLoc?.let { locRepo.del(it, Int.MAX_VALUE) }
        landingLoc = null
    }

    private fun pickLocation(forced: ShootingStarLocationsRow?): ShootingStarLocationsRow {
        if (forced != null) return forced
        val all = ShootingStarLocationsRow.all()
        return all.filter { it.rowId != currentLocation?.rowId }.randomOrNull() ?: all.random()
    }

    private fun scheduleNextWave() {
        val interval = settings.spawnIntervalCycles()
        val variation = settings.spawnVariationCycles()
        val delta =
            if (variation <= 0) {
                interval
            } else {
                interval + Random.nextInt(-variation, variation + 1)
            }
        nextSpawnCycle = mapClock.cycle + delta.coerceAtLeast(1)
        scheduleCrashWarning(nextSpawnCycle)
        val all = ShootingStarLocationsRow.all()
        val candidates = all.filter { it.rowId != currentLocation?.rowId }.ifEmpty { all }
        nextLocation = candidates.random()
    }

    private fun scheduleCrashWarning(landCycle: Int) {
        val warnAt = landCycle - CRASH_WARNING_CYCLES
        crashWarningCycle = if (warnAt > mapClock.cycle) warnAt else 0
    }

    private fun broadcast(text: String) {
        for (player in playerList) {
            player.mes(text, ChatType.Broadcast)
        }
    }

    fun noteDiscovery(player: Player) {
        player.attr[SEEN_SHOOTING_STAR] = true
        if (discoveredBy != null) return
        discoveredBy = player.displayName
        player.attr[DISCOVERER_BONUS_REMAINING] = DISCOVERER_BONUS_DUST
        player.mes("You are the first player to mine this star!")
        player.mes(
            "Congratulations! You were the first person to find this star! " +
                "Your stardust will be doubled for the next $DISCOVERER_BONUS_DUST dust you mine!",
        )
    }

    fun startLayerMining() {
        if (layerMiningStarted) return
        layerMiningStarted = true
        layerStartedCycle = mapClock.cycle
    }

    fun chipUnderLevelProgress() {
        if (layerMiningStarted) return
        underLevelProgressCycles =
            (underLevelProgressCycles + UNDER_LEVEL_CHIP_CYCLES)
                .coerceAtMost(ShootingStarStages.LAYER_DURATION_CYCLES - 1)
        updateHeadbar()
    }

    fun stardustAmountFor(player: Player): Int {
        val remaining = player.attr[DISCOVERER_BONUS_REMAINING] ?: 0
        return if (remaining > 0) 2 else 1
    }

    fun consumeDiscovererBonus(player: Player) {
        val remaining = player.attr[DISCOVERER_BONUS_REMAINING] ?: 0
        if (remaining <= 0) return
        player.attr[DISCOVERER_BONUS_REMAINING] = (remaining - 1).coerceAtLeast(0)
    }

    fun clearStar(announce: Boolean, replacedByNewStar: Boolean = false) {
        for (player in playerList) {
            if (player.attr[MINING_STAR] != true) {
                continue
            }
            player.attr[MINING_STAR] = false
            player.cancelActiveCoroutine()
            player.clearInteraction()
            player.resetAnim()
            if (announce) {
                player.mes(
                    if (replacedByNewStar) {
                        "The star has been replaced by a new crashing star!"
                    } else {
                        "The star has been mined completely!"
                    },
                )
            }
        }

        starLoc?.let { locRepo.del(it, Int.MAX_VALUE) }
        starLoc = null
        removeHeadbarNpc()
        active = false
        layerMiningStarted = false
        layerStartedCycle = 0
        underLevelProgressCycles = 0
    }

    private fun advanceStage() {
        stageIndex++
        layerMiningStarted = false
        layerStartedCycle = 0
        underLevelProgressCycles = 0

        if (stageIndex >= ShootingStarStages.ALL.size) {
            clearStar(announce = true)
            return
        }

        val coords = currentLocation?.coords ?: return
        starLoc?.let { locRepo.del(it, Int.MAX_VALUE) }
        starLoc =
            locRepo.add(
                coords,
                currentStage().loc,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        spawnHeadbarNpc(coords)

        for (player in playerList) {
            if (player.attr[MINING_STAR] == true) {
                player.mes("The star has depleted slightly.")
            }
        }
    }

    private fun layerElapsedCycles(): Int {
        val activeElapsed =
            if (layerMiningStarted && layerStartedCycle > 0) {
                (mapClock.cycle - layerStartedCycle).coerceAtLeast(0)
            } else {
                0
            }
        return (activeElapsed + underLevelProgressCycles)
            .coerceAtMost(ShootingStarStages.LAYER_DURATION_CYCLES)
    }

    private fun spawnHeadbarNpc(coords: CoordGrid) {
        removeHeadbarNpc()
        val type =
            ServerCacheManager.getNpc("npc.star_headbar_npc".asRSCM(RSCMType.NPC)) ?: return
        val npc = Npc(type, coords)
        npc.mode = NpcMode.None
        npc.hitpoints = ShootingStarStages.LAYER_DURATION_CYCLES
        npc.baseHitpointsLvl = ShootingStarStages.LAYER_DURATION_CYCLES
        npcRepo.add(npc, Int.MAX_VALUE)
        headbarNpc = npc
        updateHeadbar()
    }

    private fun removeHeadbarNpc() {
        val npc = headbarNpc ?: return
        npcRepo.del(npc, Int.MAX_VALUE)
        headbarNpc = null
    }

    private fun updateHeadbar() {
        val npc = headbarNpc ?: return
        val maxHp = ShootingStarStages.LAYER_DURATION_CYCLES
        val curHp = (maxHp - layerElapsedCycles()).coerceAtLeast(0)
        npc.hitpoints = curHp
        npc.baseHitpointsLvl = maxHp

        val headbarType =
            ServerCacheManager.getHealthBar("headbar.shooting_star".asRSCM(RSCMType.HEADBAR))
                ?: return
        val fill = if (maxHp <= 0) 0 else (curHp * headbarType.segments) / maxHp
        npc.showHeadbar(
            Headbar.fromNoSource(
                self = headbarType.id,
                public = headbarType.id,
                startFill = fill,
                endFill = fill,
                startTime = 0,
                endTime = 0,
            ),
        )
    }

    companion object {
        const val DISCOVERER_BONUS_DUST = 300
        private const val UNDER_LEVEL_CHIP_CYCLES = 7
        private const val SHADOW_APPROACH_TILES = 6
        private const val TELEPORT_SEARCH_RADIUS = 12
        private const val CRASH_SOUND_RADIUS = 15
        private const val CRASH_WARNING_CYCLES = 500
        private const val CRASH_IMPACT_FALLBACK_CYCLES = 5
        private const val CRASH_IMPACT_MAX_CYCLES = 10
        private const val CRASH_WARNING_MESSAGE =
            "A shooting star will crash-land in 5 minutes. Keep an eye out for more information!"
        private const val LANDING_LOC = "loc.star_landing_star"
        private const val CRASH_WHISTLE_SYNTH = "synth.shooting_star_whisle"
        private const val CRASH_IMPACT_SYNTH = "synth.shooting_star_crash"
        private const val CRASH_IMPACT_ANIM = "seq.game_star_meteor_impact"
    }
}
