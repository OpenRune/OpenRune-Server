package org.rsmod.content.raids.toa.raid.encounter.wardens

import kotlin.math.max
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ProjAnimType
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaPath
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.apmeken.baba.BabaRocks
import org.rsmod.content.raids.toa.raid.encounter.apmeken.baba.BabaSeqs
import org.rsmod.content.raids.toa.raid.encounter.apmeken.baba.BabaSpots
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.ZebakProjs
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.ZebakSpots
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.ZebakSynths
import org.rsmod.content.raids.toa.raid.encounter.het.akkha.AkkhaSeqs
import org.rsmod.content.raids.toa.raid.encounter.het.akkha.AkkhaSpots
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriSeqs
import org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri.KephriSpots
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.api.bosses.spec.HitType as SpecHit
import org.rsmod.game.map.Direction
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

internal class WardenPhantoms(private val room: WardensSecondEncounter) {
    private val deps = room.raid.deps
    private val phantoms = ArrayList<Phantom>()

    enum class Kind(val npc: String, val tile: CoordGrid) {
        ZEBAK("npc.toa_wardens_zebak", SECOND_SET_TILE),
        AKKHA("npc.toa_wardens_akkha", SECOND_SET_TILE),
        BABA("npc.toa_wardens_baba", THIRD_SET_TILE),
        KEPHRI("npc.toa_wardens_kephri", THIRD_SET_TILE),
    }

    private class Phantom(val kind: Kind, val npc: Npc, var cooldown: Int) {
        var ranged = true
        var shots = 0
    }

    private class Flight(
        val startHeight: Int,
        val endHeight: Int,
        val startTime: Int,
        val endTime: Int,
        val angle: Int,
        val progress: Int = 0,
    )

    private var enraged = false

    private val rate: Int
        get() {
            val insanity = room.raid.isActive(ToaInvocationKey.Insanity)
            return when {
                enraged && insanity -> FAST_ENRAGED_RATE
                enraged -> ENRAGED_RATE
                insanity -> FAST_RATE
                else -> RATE
            }
        }

    fun enrage() {
        enraged = true
        for (phantom in phantoms) phantom.cooldown = minOf(phantom.cooldown, rate)
    }

    fun summon(set: Int) {
        val kind = kindFor(set) ?: return
        if (phantoms.any { it.kind == kind }) return
        val npc =
            room.spawn(kind.npc, room.coords(kind.tile), Direction.South).also {
                it.faceSquare(room.coords(FACING))
                it.ignoreCombatInteractions = true
            }
        phantoms += Phantom(kind, npc, FIRST_ATTACK)
    }

    fun tick(targets: List<Player>) {
        if (targets.isEmpty()) return
        for (phantom in phantoms) {
            if (--phantom.cooldown > 0) continue
            phantom.cooldown = rate
            attack(phantom, targets)
        }
    }

    fun clear() {
        enraged = false
        for (phantom in phantoms) room.despawn(phantom.npc)
        phantoms.clear()
    }

    private fun kindFor(set: Int): Kind? =
        when (set) {
            SECOND_SET -> if (room.god == WardenGod.TUMEKEN) Kind.ZEBAK else Kind.AKKHA
            THIRD_SET -> if (room.god == WardenGod.TUMEKEN) Kind.BABA else Kind.KEPHRI
            else -> null
        }

    private fun attack(phantom: Phantom, targets: List<Player>) {
        when (phantom.kind) {
            Kind.ZEBAK -> zebak(phantom, targets)
            Kind.BABA -> baba(phantom, targets)
            Kind.AKKHA -> akkha(phantom, targets)
            Kind.KEPHRI -> kephri(phantom, targets)
        }
    }

    private fun zebak(phantom: Phantom, targets: List<Player>) {
        val npc = phantom.npc
        if (deps.random.of(0, 2) == 0) phantom.ranged = !phantom.ranged
        val ranged = phantom.ranged
        val apex = room.coords(ZEBAK_APEX)
        npc.anim(ZEBAK_SEQ)
        for (player in targets) {
            player.soundSynth(if (ranged) ZebakSynths.RANGE_SHOOT else ZebakSynths.MAGE_SHOOT)
            player.soundSynth(
                if (ranged) ZebakSynths.RANGE_SPLIT else ZebakSynths.MAGE_SPLIT,
                delay = ZEBAK_SPLIT_SOUND_DELAY,
            )
        }
        projectile(
            if (ranged) ZebakSpots.RANGE_INITIAL else ZebakSpots.MAGE_INITIAL,
            npc.coords,
            apex,
            projAnimType(ZebakProjs.INITIAL),
        )
        room.schedule(ZEBAK_SPLIT_DELAY) {
            deps.worldRepo.spotanimMap(
                spotanim(if (ranged) ZebakSpots.RANGE_SPLIT else ZebakSpots.MAGE_SPLIT),
                room.coords(ZEBAK_SPLIT),
                height = ZEBAK_SPLIT_HEIGHT,
            )
            for (player in room.targets()) fragment(npc, player, apex, ranged)
        }
    }

    private fun fragment(npc: Npc, player: Player, apex: CoordGrid, ranged: Boolean) {
        projectile(
            if (ranged) ZebakSpots.RANGE_FRAGMENT else ZebakSpots.MAGE_FRAGMENT,
            apex,
            player.coords,
            projAnimType(ZebakProjs.SPLIT),
            player,
        )
        player.soundSynth(ZebakSynths.PROJECTILE_IMPACT, delay = ZEBAK_IMPACT_DELAY)
        player.spotanim(
            if (ranged) ZebakSpots.RANGE_IMPACT else ZebakSpots.MAGE_IMPACT,
            delay = ZEBAK_IMPACT_DELAY,
            height = ZEBAK_IMPACT_HEIGHT,
        )
        val combatant = room.combatantOf(npc) ?: return
        val style = if (ranged) SpecHit.Ranged else SpecHit.Magic
        val damage =
            if (combatant.rollAccuracy(player, style, null)) combatant.rollScaled(ZEBAK_MAX_HIT) else 0
        player.queueImpactHit(
            npc,
            ZEBAK_HIT_DELAY,
            if (ranged) HitType.Ranged else HitType.Magic,
            damage,
            deps.playerHitModifier,
        )
    }

    private fun baba(phantom: Phantom, targets: List<Player>) {
        phantom.npc.anim(BabaSeqs.MELEE)
        val tier = (room.raid.pathLevels[ToaPath.APMEKEN.ordinal] / PATH_LEVELS_PER_TIER).coerceIn(0, BabaSpots.ROCK_FALL.lastIndex)
        val marked = targets.map { it.coords }.toSet()
        for (tile in marked) {
            deps.worldRepo.spotanimMap(
                spotanim(BabaSpots.ROCK_FALL[tier]),
                tile,
                delay = BabaRocks.MARK_DELAY_CYCLES,
            )
        }
        room.schedule(maxOf(BABA_MIN_FALL_TICKS, BABA_FALL_TICKS - BabaRocks.TICKS_PER_TIER * tier)) {
            for (player in room.targets()) {
                if (player.coords in marked) room.hazardDamage(player, BABA_DAMAGE, BABA_SPREAD)
            }
        }
    }

    private fun akkha(phantom: Phantom, targets: List<Player>) {
        val npc = phantom.npc
        if (room.raid.isActive(ToaInvocationKey.StayVigilant)) {
            phantom.ranged = deps.random.of(0, 1) == 0
        } else if (phantom.shots >= AKKHA_SHOTS_PER_STYLE) {
            phantom.ranged = !phantom.ranged
            phantom.shots = 0
            return
        }
        phantom.shots++
        val ranged = phantom.ranged
        val source = room.coords(AKKHA_SOURCE)
        npc.anim(if (ranged) AkkhaSeqs.RANGED else AkkhaSeqs.MAGIC)
        for (player in targets) {
            val distance = source.chebyshevDistance(player.coords)
            val flight = max(AKKHA_MIN_FLIGHT, distance * if (ranged) AKKHA_RANGED_STEP else AKKHA_MAGIC_STEP)
            projectile(
                if (ranged) AkkhaSpots.RANGED_TRAVEL else AkkhaSpots.MAGIC_TRAVEL,
                source,
                player.coords,
                Flight(AKKHA_START_HEIGHT, AKKHA_END_HEIGHT, 0, flight, AKKHA_ANGLE, AKKHA_PROGRESS),
            )
            strike(npc, player, if (ranged) HitType.Ranged else HitType.Magic, AKKHA_MAX_HIT, flight)
        }
    }

    private fun kephri(phantom: Phantom, targets: List<Player>) {
        val npc = phantom.npc
        val apex = room.coords(KEPHRI_APEX)
        npc.anim(KephriSeqs.AUTO)
        projectile(KephriSpots.AUTO_APEX, npc.coords, apex, KEPHRI_APEX_FLIGHT)
        val aimed = targets.map { it.coords }.toSet()
        room.schedule(KEPHRI_DROP_DELAY) {
            for (tile in aimed) {
                projectile(KephriSpots.AUTO_DROP, apex, tile, KEPHRI_DROP_FLIGHT)
            }
        }
        room.schedule(KEPHRI_DROP_DELAY + KEPHRI_BLAST_DELAY) {
            val blast = if (room.raid.isActive(ToaInvocationKey.AerialAssault)) aimed.flatMap(::area) else aimed
            for (tile in blast) {
                deps.worldRepo.spotanimMap(spotanim(KEPHRI_EXPLOSION), tile, height = KEPHRI_IMPACT_HEIGHT)
            }
            for (player in room.targets()) {
                if (player.coords in blast) room.hazardDamage(player, KEPHRI_DAMAGE, KEPHRI_SPREAD)
            }
        }
    }

    private fun area(centre: CoordGrid): List<CoordGrid> =
        (-1..1).flatMap { dx -> (-1..1).map { dz -> centre.translate(dx, dz) } }

    private fun strike(npc: Npc, player: Player, style: HitType, maxHit: Int, flightCycles: Int) {
        val damage = room.combatantOf(npc)?.rollScaled(maxHit) ?: 0
        val delay = Math.ceilDiv(flightCycles, CYCLES_PER_TICK)
        player.queueImpactHit(npc, delay, style, damage, deps.playerHitModifier)
    }

    private fun projectile(spot: String, from: CoordGrid, to: CoordGrid, flight: Flight) {
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spotanim(spot).id,
                startHeight = flight.startHeight,
                endHeight = flight.endHeight,
                startTime = flight.startTime,
                endTime = flight.endTime,
                angle = flight.angle,
                progress = flight.progress,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    private fun projectile(
        spot: String,
        from: CoordGrid,
        to: CoordGrid,
        type: ProjAnimType,
        player: Player? = null,
    ) {
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spotanim(spot).id,
                startHeight = type.startHeight,
                endHeight = type.endHeight,
                startTime = type.delay,
                endTime = ProjAnim.calculateEndTime(type, Bounds(from).distanceTo(Bounds(to))),
                angle = type.angle,
                progress = type.progress,
                sourceIndex = 0,
                targetIndex = player?.let { -(it.slotId + 1) } ?: 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    private fun projAnimType(name: String): ProjAnimType =
        ServerCacheManager.getProjectile(name.asRSCM(RSCMType.PROJANIM))
            ?: error("Projectile not found: $name")

    private companion object {
        val SECOND_SET_TILE = CoordGrid(3943, 5153, 1)
        val THIRD_SET_TILE = CoordGrid(3925, 5153, 1)
        val ZEBAK_APEX = CoordGrid(3941, 5159, 1)
        val ZEBAK_SPLIT = CoordGrid(3938, 5156, 1)
        val AKKHA_SOURCE = CoordGrid(3945, 5155, 1)
        val KEPHRI_APEX = CoordGrid(3928, 5156, 1)
        val FACING = CoordGrid(3936, 5161, 1)

        const val SECOND_SET = 2
        const val THIRD_SET = 3
        const val FIRST_ATTACK = 8
        const val RATE = 8
        const val FAST_RATE = 6
        const val ENRAGED_RATE = 5
        const val FAST_ENRAGED_RATE = 4
        const val CYCLES_PER_TICK = 30

        const val ZEBAK_SEQ = "seq.npc_zebak01_attack_ranged_enraged"
        const val ZEBAK_SPLIT_DELAY = 4
        const val ZEBAK_SPLIT_HEIGHT = 535
        const val ZEBAK_MAX_HIT = 16
        const val ZEBAK_HIT_DELAY = 4
        const val ZEBAK_IMPACT_DELAY = 90
        const val ZEBAK_IMPACT_HEIGHT = 90
        const val ZEBAK_SPLIT_SOUND_DELAY = 120

        const val BABA_FALL_TICKS = 5
        const val BABA_MIN_FALL_TICKS = 2
        const val PATH_LEVELS_PER_TIER = 2
        const val BABA_DAMAGE = 19
        const val BABA_SPREAD = 4

        const val AKKHA_SHOTS_PER_STYLE = 3
        const val AKKHA_MAX_HIT = 20
        const val AKKHA_START_HEIGHT = 404
        const val AKKHA_END_HEIGHT = 138
        const val AKKHA_ANGLE = 12
        const val AKKHA_PROGRESS = 32
        const val AKKHA_MIN_FLIGHT = 30
        const val AKKHA_RANGED_STEP = 6
        const val AKKHA_MAGIC_STEP = 10

        const val KEPHRI_DROP_DELAY = 3
        const val KEPHRI_BLAST_DELAY = 3
        const val KEPHRI_IMPACT_HEIGHT = 38
        const val KEPHRI_DAMAGE = 20
        const val KEPHRI_SPREAD = 2
        const val KEPHRI_EXPLOSION = "spotanim.vfx_scarab_explosion02"
        val KEPHRI_APEX_FLIGHT = Flight(700, 1000, 39, 90, 50)
        val KEPHRI_DROP_FLIGHT = Flight(1000, 38, 0, 120, 115)
    }
}
