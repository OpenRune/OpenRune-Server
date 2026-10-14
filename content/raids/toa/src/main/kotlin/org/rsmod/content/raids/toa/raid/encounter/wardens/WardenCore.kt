package org.rsmod.content.raids.toa.raid.encounter.wardens

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

internal class WardenCore(private val room: WardensEncounter) {
    enum class State {
        SHIELD,
        KNEELING,
        DOWN,
        REVIVING,
        DEFEATED,
    }

    var state = State.SHIELD
        private set

    private val deps = room.raid.deps
    private var shieldBroken = false
    private var shieldMax = 0
    private var realMax = 0
    private var realHp = 0
    private var core: Npc? = null
    private var blocker: LocInfo? = null
    private var shieldForm: String? = null

    var coreTile: CoordGrid? = null
        private set

    val realFraction: Double
        get() = if (realMax == 0) 1.0 else realHp.toDouble() / realMax

    val shielded: Boolean
        get() = state == State.SHIELD || state == State.KNEELING

    fun arm(warden: Npc, combatant: ToaCombatant?, teamSize: Int) {
        clear()
        shieldMax = warden.baseHitpointsLvl
        realMax =
            combatant?.scaledRaidHitpoints(REAL_BASE_HP, teamSize, REAL_RAID_HP_FACTOR)
                ?: REAL_BASE_HP
        realHp = realMax
    }

    fun clear() {
        core?.let(room::despawn)
        core = null
        removeBlocker()
        state = State.SHIELD
        shieldBroken = false
        shieldForm = null
    }

    fun hudHp(npc: Npc): Int =
        if (npc === room.activeWarden && shielded) shieldMax - npc.hitpoints else npc.hitpoints

    fun modify(warden: Npc, hit: HitBuilder, vulnerable: HitType) {
        when (state) {
            State.SHIELD -> {
                if (shieldBroken || hit.type != vulnerable) {
                    hit.damage = 0
                    return
                }
                hit.damage = min(hit.damage, warden.hitpoints)
                warden.hitpoints -= hit.damage
                if (warden.hitpoints <= 0) shieldBroken = true
            }
            else -> hit.damage = 0
        }
    }

    fun afterHit(warden: Npc, god: WardenGod) {
        if (state == State.DOWN && warden.hitpoints <= 0) room.wardenDefeated(warden)
        if (state == State.SHIELD && shieldBroken) kneel(warden, god)
    }

    fun mirror(coreNpc: Npc, hit: Hit) {
        if (coreNpc !== core) return
        if (state != State.DOWN) return
        val warden = room.activeWarden ?: return
        val player = hit.resolvePlayerSource(deps.bossDeps.playerList) ?: return
        val damage = min(hit.damage * MIRROR_FACTOR, warden.hitpoints)
        if (damage <= 0) return
        warden.hitpoints -= damage
        realHp = warden.hitpoints
        warden.queueHit(
            source = player,
            delay = MIRROR_DELAY,
            type = HitType.Typeless,
            damage = damage,
            modifier = NoopModifier,
        )
    }

    fun setRealFraction(fraction: Double) {
        realHp = (realMax * fraction).toInt().coerceIn(0, realMax)
        val warden = room.activeWarden ?: return
        if (state == State.DOWN) warden.hitpoints = realHp
    }

    fun defeated() {
        state = State.DEFEATED
        room.obeliskCycle.stop()
        core?.let(room::despawn)
        core = null
        removeBlocker()
    }

    private fun kneel(warden: Npc, god: WardenGod) {
        state = State.KNEELING
        room.obeliskCycle.coreOut()
        warden.ignoreCombatInteractions = true
        room.stopAttacking(warden)
        warden.anim(WardenCoreRefs.KNEEL)
        room.hpBar?.update()
        room.schedule(EXPOSE_DELAY) { if (state == State.KNEELING) expose(warden, god) }
    }

    private fun expose(warden: Npc, god: WardenGod) {
        state = State.DOWN
        shieldForm = if (warden.visType.id == npcType(god.phaseTwo).id) god.phaseTwo else god.phaseTwoAlt
        warden.baseHitpointsLvl = realMax
        warden.hitpoints = realHp
        warden.transmog(npcType(god.phaseTwoExposed), Int.MAX_VALUE)
        warden.removeHeadbar(headbarId(WardenCoreRefs.SHIELD_BAR))
        showHealthBar(warden)
        room.hpBar?.update()

        val centre = warden.coords.translate(WardenGod.WARDEN_RADIUS, WardenGod.WARDEN_RADIUS)
        val tile = centre.translate(-god.orbSide * CORE_OFFSET, CORE_OFFSET)
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spotanim(god.coreProjectile).id,
                startHeight = OUT_START_HEIGHT,
                endHeight = OUT_END_HEIGHT,
                startTime = 0,
                endTime = OUT_FLIGHT,
                angle = OUT_ANGLE,
                progress = OUT_PROGRESS,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = centre,
                endCoord = tile,
            )
        )
        room.schedule(CORE_SPAWN_DELAY) { if (state == State.DOWN) spawnCore(god, tile) }
        val window = returnWindow()
        room.schedule(window) { if (state == State.DOWN) revive(warden, god, tile, centre) }
    }

    private fun spawnCore(god: WardenGod, tile: CoordGrid) {
        coreTile = tile
        core = room.spawn(god.core, tile, null).also {
                it.vars[FORCED_MAX_HIT] = FORCED_MELEE
                it.ignoreCombatInteractions = true
            }
        blocker =
            deps.locRepo.add(
                tile,
                WardenCoreRefs.BLOCKER,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        deps.worldRepo.soundArea(tile, WardenCoreRefs.SPAWN_SOUND, radius = SOUND_RADIUS)
        for (player in room.targets()) {
            if (player.coords == tile) push(player, tile)
        }
    }

    @OptIn(InternalApi::class)
    private fun push(player: Player, tile: CoordGrid) {
        val dest = tile.translate(0, -PUSH_DISTANCE)
        player.abortRoute()
        deps.launcher.launchLenient(player) { telejump(dest, TeleportType.Exempt) }
        player.mes(WardenCoreRefs.PUSHED)
    }

    private fun revive(warden: Npc, god: WardenGod, tile: CoordGrid, centre: CoordGrid) {
        state = State.REVIVING
        realHp = warden.hitpoints
        core?.resetAnim()
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spotanim(god.coreProjectile).id,
                startHeight = RETURN_START_HEIGHT,
                endHeight = RETURN_END_HEIGHT,
                startTime = RETURN_START_TIME,
                endTime = RETURN_FLIGHT,
                angle = RETURN_ANGLE,
                progress = RETURN_PROGRESS,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = tile,
                endCoord = centre,
            )
        )
        deps.worldRepo.soundArea(tile, WardenCoreRefs.REVIVE_SOUND, radius = SOUND_RADIUS)
        room.schedule(CORE_REMOVE_DELAY) {
            core?.let(room::despawn)
            core = null
            removeBlocker()
        }
        room.schedule(STAND_UP_DELAY) { if (state == State.REVIVING) standUp(warden, god) }
    }

    private fun standUp(warden: Npc, god: WardenGod) {
        val previous = shieldForm ?: god.phaseTwo
        val next = if (previous == god.phaseTwo) god.phaseTwoAlt else god.phaseTwo
        warden.baseHitpointsLvl = shieldMax
        warden.hitpoints = shieldMax
        shieldBroken = false
        state = State.SHIELD
        warden.ignoreCombatInteractions = false
        room.obeliskCycle.coreBack()
        warden.removeHeadbar(headbarId(WardenCoreRefs.HEALTH_BAR))
        warden.anim(WardenCoreRefs.STAND_UP)
        warden.transmog(npcType(next), Int.MAX_VALUE)
        room.hpBar?.update()
        room.resumeAttacking(warden, FIRST_ATTACK_AFTER_STAND)
    }

    private fun returnWindow(): Int =
        when {
            realFraction > BAND_HIGH -> WINDOW_HIGH
            realFraction > BAND_MID -> WINDOW_MID
            else -> WINDOW_LOW
        }

    private fun showHealthBar(warden: Npc) {
        val bar = ServerCacheManager.getHealthBar(WardenCoreRefs.HEALTH_BAR.asRSCM(RSCMType.HEADBAR))
        if (bar == null) return
        val fill = bar.segments * warden.hitpoints / warden.baseHitpointsLvl
        warden.showHeadbar(
            Headbar.fromNoSource(
                self = bar.id,
                public = bar.id,
                startFill = fill,
                endFill = fill,
                startTime = 0,
                endTime = 0,
            )
        )
    }

    private fun headbarId(name: String): Int = name.asRSCM(RSCMType.HEADBAR)

    private fun removeBlocker() {
        blocker?.let { deps.locRepo.del(it, Int.MAX_VALUE) }
        blocker = null
    }

    private companion object {
        val NoopModifier = NpcHitModifier {}

        const val FORCED_MAX_HIT = "varn.forced_max_hit_styles"
        const val FORCED_MELEE = 1
        const val REAL_BASE_HP = 4500
        const val REAL_RAID_HP_FACTOR = 0.25
        const val MIRROR_FACTOR = 5
        const val MIRROR_DELAY = 1

        const val EXPOSE_DELAY = 5
        const val CORE_SPAWN_DELAY = 2
        const val CORE_OFFSET = 3
        const val PUSH_DISTANCE = 2
        const val CORE_REMOVE_DELAY = 1
        const val STAND_UP_DELAY = 3
        const val FIRST_ATTACK_AFTER_STAND = 4
        const val SOUND_RADIUS = 15

        const val OUT_START_HEIGHT = 380
        const val OUT_END_HEIGHT = 50
        const val OUT_FLIGHT = 60
        const val OUT_ANGLE = 127
        const val OUT_PROGRESS = 124

        const val RETURN_START_HEIGHT = 80
        const val RETURN_END_HEIGHT = 250
        const val RETURN_START_TIME = 60
        const val RETURN_FLIGHT = 90
        const val RETURN_ANGLE = 20
        const val RETURN_PROGRESS = 64

        const val BAND_HIGH = 0.8
        const val BAND_MID = 0.6
        const val WINDOW_HIGH = 24
        const val WINDOW_MID = 32
        const val WINDOW_LOW = 40
    }
}
