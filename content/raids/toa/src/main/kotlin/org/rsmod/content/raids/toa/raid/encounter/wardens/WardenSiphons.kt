package org.rsmod.content.raids.toa.raid.encounter.wardens

import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.vars.intVarn
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.api.table.ToaWardensSiphonsRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

internal class WardenSiphons(private val room: WardensSecondEncounter) {
    private val deps = room.raid.deps
    private val orbs = ArrayList<Npc>()
    private val blockers = ArrayList<LocInfo>()
    private var token = 0
    private var hitsPerOrb = 1
    private var unreversed = 0
    private var pending = false
    private var succeeded = false

    var launched = 0
        private set

    var active = false
        private set

    private val insanity: Boolean
        get() = room.raid.isActive("dbrow.toa_invocation_insanity")

    fun reset() {
        token++
        pending = false
        active = false
        launched = 0
        clear()
    }

    fun launch(warden: Npc) {
        room.siphonLaunched()
        val gen = ++token
        active = true
        pending = true
        succeeded = false
        val players = room.targets().size.coerceAtLeast(1)
        val kind = if (players > 1) "group" else "solo"
        val row = "dbrow.toa_wardens_siphon_${kind}_${launched + 1}"
        val tiles = ToaWardensSiphonsRow.getRow(row).tiles.map(room::coords)
        launched++
        hitsPerOrb = hitsFor(players)
        unreversed = tiles.size

        warden.transmog(npcType(room.god.phaseThreeCharging), Int.MAX_VALUE)
        deps.worldRepo.soundArea(centre(), SynthRefs.CHARGE, radius = SOUND_RADIUS)
        after(INCOMING_DELAY, gen) { for (tile in tiles) incoming(tile) }
        after(SPAWN_DELAY, gen) { spawn(tiles) }
        after(SPAWN_DELAY + window(tiles.size), gen) { resolve(success = false) }
        after(BEAM_FIRST, gen) { beams(gen) }
        val set = launched
        room.schedule(PHANTOM_DELAY) { room.phantoms.summon(set) }
    }

    fun hit(orb: Npc, hit: Hit) {
        if (!pending || !hit.isFromPlayer || hit.damage <= 0 || orb !in orbs) return
        val taken = orb.hitsTaken + 1
        orb.hitsTaken = taken
        if (taken < hitsPerOrb) return
        orb.transmog(npcType(RED), Int.MAX_VALUE)
        if (--unreversed == 0) resolve(success = true)
    }

    fun finish() {
        val warden = room.warden ?: return
        active = false
        warden.transmog(npcType(room.god.phaseThree), Int.MAX_VALUE)
        if (insanity) room.schedule(KEEP_ROTATION_DELAY) { room.keepRotation() }
        for (player in room.targets()) {
            for (synth in SynthRefs.STAGGER) player.soundSynth(synth)
        }
        if (!succeeded) return
        val source = room.targets().firstOrNull() ?: return
        if (warden.hitpoints <= 0) return
        warden.queueHit(
            source = source,
            delay = REFLECT_DELAY,
            type = HitType.Typeless,
            damage = reflect(warden),
            modifier = NoopModifier,
        )
    }

    private fun resolve(success: Boolean) {
        if (!pending) return
        val warden = room.warden ?: return
        pending = false
        succeeded = success
        token++
        if (success) {
            for (orb in orbs) projectile(RETURN, orb.coords, source())
            deps.worldRepo.soundArea(centre(), SynthRefs.REVERSE, radius = SOUND_RADIUS)
        } else {
            warden.anim(RELEASE_SEQ)
            val damage = unreversed * FAIL_DAMAGE * if (insanity) INSANITY_FAIL_FACTOR else 1
            room.slams.floorBurst(damage)
        }
        clear()
        val target = room.targets().firstOrNull()
        if (target == null) {
            finish()
            return
        }
        deps.bossDeps.runAbility(warden, target, WardenFinalAbilities.RESOLVE)
    }

    private fun spawn(tiles: List<CoordGrid>) {
        for (tile in tiles) {
            orbs +=
                room.spawn(BLUE, tile, null).also { it.ignoreCombatInteractions = true }
            blockers +=
                deps.locRepo.add(
                    tile,
                    WardenCoreRefs.BLOCKER,
                    Int.MAX_VALUE,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
            for (player in room.targets()) {
                if (player.coords == tile) push(player, tile)
            }
        }
        deps.worldRepo.soundArea(centre(), SynthRefs.SPAWN, radius = SOUND_RADIUS)
    }

    private fun beams(gen: Int) {
        val from = beamSource()
        for (orb in orbs) {
            if (orb.isSlotAssigned) projectile(BEAM, from, orb.coords)
        }
        deps.worldRepo.soundArea(centre(), SynthRefs.BEAM, radius = SOUND_RADIUS)
        after(BEAM_STEP, gen) { beams(gen) }
    }

    private fun incoming(tile: CoordGrid) {
        deps.worldRepo.spotanimMap(spotanim(WardenSpots.SHADOW), tile)
        projectile(INCOMING, source(), tile)
    }

    private fun projectile(flight: Flight, from: CoordGrid, to: CoordGrid) {
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spotanim(flight.spot).id,
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

    @OptIn(InternalApi::class)
    private fun push(player: Player, tile: CoordGrid) {
        val dest = tile.translate(0, -PUSH_DISTANCE)
        player.abortRoute()
        deps.launcher.launchLenient(player) { telejump(dest, TeleportType.Exempt) }
        player.mes(WardenCoreRefs.PUSHED)
    }

    private fun clear() {
        for (orb in orbs) room.despawn(orb)
        orbs.clear()
        for (blocker in blockers) deps.locRepo.del(blocker, Int.MAX_VALUE)
        blockers.clear()
    }

    private fun after(ticks: Int, gen: Int, action: () -> Unit) {
        room.schedule(ticks) { if (gen == token && pending) action() }
    }

    private fun window(count: Int): Int =
        if (insanity) WINDOW_BASE + INSANITY_WINDOW_STEP * count else WINDOW_BASE + WINDOW_STEP * count

    private fun hitsFor(players: Int): Int =
        when {
            players >= LARGE_PARTY -> 4
            players >= MID_PARTY -> 3
            players >= SMALL_PARTY -> 2
            else -> 1
        }

    private fun reflect(warden: Npc): Int =
        REFLECT_BASE + (warden.baseHitpointsLvl - P3_BASE) * REFLECT_SLOPE_NUM / REFLECT_SLOPE_DEN

    private fun centre(): CoordGrid = room.coords(WARDEN_CENTRE)

    private fun source(): CoordGrid = room.coords(SOURCE)

    private fun beamSource(): CoordGrid = room.coords(BEAM_SOURCE)

    private class Flight(
        val spot: String,
        val startHeight: Int,
        val endHeight: Int,
        val startTime: Int,
        val endTime: Int,
        val angle: Int,
        val progress: Int,
    )

    private object SynthRefs {
        const val CHARGE = "synth.toa_wardens_charge"
        const val SPAWN = "synth.toa_wardens_siphon_spawn"
        const val BEAM = "synth.toa_wardens_siphon_beam"
        const val REVERSE = "synth.toa_wardens_siphon_reverse"
        val STAGGER =
            (1..6).map { "synth.toa_wardens_stagger_$it" }
    }

    private companion object {
        val NoopModifier = NpcHitModifier {}

        const val BLUE = "npc.wardens_p3_orb_blue"
        const val RED = "npc.wardens_p3_orb_red"
        const val RELEASE_SEQ = "seq.npc_wardens_release01"

        val WARDEN_CENTRE = CoordGrid(3936, 5154, 1)
        val SOURCE = CoordGrid(3936, 5156, 1)
        val BEAM_SOURCE = CoordGrid(3936, 5155, 1)

        val INCOMING = Flight("spotanim.fx_wardens_bomb03", 350, 80, 30, 120, 40, 64)
        val RETURN = Flight("spotanim.fx_wardens_bomb03", 80, 350, 0, 58, 40, 64)
        val BEAM = Flight("spotanim.fx_wardens_charging01", 400, 20, 0, 90, 12, 70)

        const val INCOMING_DELAY = 2
        const val SPAWN_DELAY = 6
        const val BEAM_FIRST = 8
        const val PHANTOM_DELAY = 3
        const val KEEP_ROTATION_DELAY = 1
        const val BEAM_STEP = 2
        const val WINDOW_BASE = 3
        const val WINDOW_STEP = 3
        const val INSANITY_WINDOW_STEP = 2
        const val SOUND_RADIUS = 15
        const val PUSH_DISTANCE = 2

        const val SMALL_PARTY = 4
        const val MID_PARTY = 6
        const val LARGE_PARTY = 8

        const val FAIL_DAMAGE = 11
        const val INSANITY_FAIL_FACTOR = 3

        const val REFLECT_DELAY = 1
        const val REFLECT_BASE = 50
        const val P3_BASE = 880
        const val REFLECT_SLOPE_NUM = 90
        const val REFLECT_SLOPE_DEN = 1760
    }
}

private var Npc.hitsTaken by intVarn("varn.toa_warden_orb_hits")
