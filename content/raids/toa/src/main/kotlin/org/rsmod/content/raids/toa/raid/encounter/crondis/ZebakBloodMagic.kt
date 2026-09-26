package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import kotlin.math.floor
import kotlin.math.max
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

/**
 * Not Just a Head (Offline_Scape sendBloodSpell / BloodCloud): every 6 attack cycles (8 enraged),
 * alternating a blood barrage and blood clouds. Arterial Spray widens the barrage splash; Blood
 * Thinners makes three small clouds.
 */
internal class ZebakBloodMagic(private val room: ZebakEncounter) {
    private val deps = room.raid.deps

    /** Ticks to the next spell; -1 without the invocation. */
    private var countdown = -1
    private var nextIsBarrage = false
    private var cloudsFromSouth = false
    private val clouds = HashMap<Npc, CloudState>()

    private class CloudState(var switchTicks: Int, var startDelay: Int, var target: Player? = null)

    fun start() {
        val active = room.raid.isActive(ZebakInvocations.NOT_JUST_A_HEAD)
        countdown = if (active) room.attackSpeed * EVERY - 1 else -1
    }

    fun tick() {
        if (countdown == -1 || --countdown != 0) return
        countdown = room.attackSpeed * (if (room.enraged) EVERY_ENRAGED else EVERY) - 1
        cast()
    }

    private fun cast() {
        val spot = spotanim(ZebakSpots.BLOOD_BARRAGE)
        for (tile in ZebakCoords.BLOOD_SPELL) deps.worldRepo.spotanimMap(spot, room.coords(tile))
        room.schedule(CAST_DELAY) {
            val targets = room.targets()
            if (targets.isEmpty()) return@schedule
            if (nextIsBarrage) barrage(targets) else spawnClouds()
            nextIsBarrage = !nextIsBarrage
        }
    }

    /**
     * 7-14 magic (scaled) on every target, and again on anyone within 1 of them (2 with Arterial
     * Spray). Zebak heals two thirds of each hit taken without Protect from Magic.
     */
    private fun barrage(targets: List<Player>) {
        val boss = room.zebak ?: return
        val base = floor(BARRAGE_BASE_DAMAGE * room.raid.damageMultiplier).toInt()
        val radius = if (room.raid.isActive(ZebakInvocations.ARTERIAL_SPRAY)) 2 else 1
        var heal = 0
        for (player in targets) {
            val damage = deps.random.of(base, base + BARRAGE_DAMAGE_SPREAD)
            heal += barrageHit(boss, player, damage)
            player.spotanim(ZebakSpots.BLOOD_BARRAGE)
            for (other in targets) {
                if (other === player || chebyshev(player.coords, other.coords) > radius) continue
                heal += barrageHit(boss, other, damage)
            }
        }
        if (heal > 0) {
            boss.heal(heal, showHitsplat = true)
            room.updateBars()
        }
        for (player in targets) player.soundSynth(ZebakSynths.BLOOD_BARRAGE)
    }

    /** Lands this tick (delay 1). Returns what Zebak heals from it. */
    private fun barrageHit(boss: Npc, player: Player, damage: Int): Int {
        player.queueImpactHit(boss, 1, HitType.Magic, damage, deps.playerHitModifier)
        return if (player.vars[PROTECT_FROM_MAGIC] > 0) 0 else (damage * BARRAGE_HEAL_RATIO).toInt()
    }

    private fun spawnClouds() {
        val base = ZebakCoords.BLOOD_CLOUDS[if (cloudsFromSouth) 1 else 0]
        if (room.raid.isActive(ZebakInvocations.BLOOD_THINNERS)) {
            for (i in 0 until 3) {
                val dz = if (cloudsFromSouth) i / 2 else -(i / 2)
                spawnCloud(ZebakNpcs.BLOOD_CLOUD_SMALL, base.translate(i, dz))
            }
        } else {
            spawnCloud(ZebakNpcs.BLOOD_CLOUD, base)
        }
        cloudsFromSouth = !cloudsFromSouth
    }

    private fun spawnCloud(type: String, static: CoordGrid) {
        val cloud = room.spawn(type, room.coords(static))
        clouds[cloud] = CloudState(deps.random.of(10, 20), CLOUD_START_DELAY)
    }

    /**
     * Once a tick (config `timer = 1`). Follows the nearest player, re-picked every 10-20 ticks.
     * After 4 ticks: 2 damage to each adjacent player and 2 heal, or 2 damage to itself if nobody
     * is adjacent.
     */
    fun cloudTick(cloud: Npc) {
        if (room.stage != ToaStage.STARTED) return
        if ((room.zebak?.hitpoints ?: 0) <= 0) return
        val state = clouds[cloud] ?: return
        if (state.startDelay > 0) state.startDelay--

        val targets = room.targets()
        state.switchTicks = max(0, state.switchTicks - 1)
        val switch = state.switchTicks == 0 || state.target.let { it == null || it !in targets }
        if (state.switchTicks == 0) state.switchTicks = deps.random.of(10, 20)
        if (switch && targets.isNotEmpty()) {
            state.target = nearestOther(cloud, state.target, targets)
        }

        val target = state.target
        if (target != null && !cloud.isBeside(target)) cloud.walk(target.coords)
        if (state.startDelay > 0) return

        var leeched = false
        for (player in targets) {
            if (!cloud.isBeside(player)) continue
            leeched = true
            player.hitTypeless(LEECH)
            cloud.heal(LEECH, showHitsplat = true)
        }
        if (!leeched) cloud.queueNpcHit(1, HitType.Typeless, LEECH, NOOP_NPC_MODIFIER)
    }

    /** Offline_Scape: the nearest player other than the current target, random among ties. */
    private fun nearestOther(cloud: Npc, current: Player?, targets: List<Player>): Player? {
        var best = current
        var bestDistance = Int.MAX_VALUE
        for (player in deps.random.shuffled(targets)) {
            if (player === current) continue
            val distance = chebyshev(player.coords, cloud.coords)
            if (distance < bestDistance) {
                bestDistance = distance
                best = player
            }
        }
        return best
    }

    fun cloudsAt(tile: CoordGrid): List<Npc> = clouds.keys.filter { it.coords == tile }

    fun removeCloud(cloud: Npc) {
        clouds.remove(cloud)
        room.despawn(cloud)
    }

    fun forget(player: Player) {
        for (state in clouds.values) {
            if (state.target === player) state.target = null
        }
    }

    fun clear() {
        for (cloud in clouds.keys.toList()) removeCloud(cloud)
        countdown = -1
        nextIsBarrage = deps.random.of(0, 1) == 0
        cloudsFromSouth = deps.random.of(0, 1) == 0
    }

    private companion object {
        const val EVERY = 6
        const val EVERY_ENRAGED = 8
        const val CAST_DELAY = 2
        const val BARRAGE_BASE_DAMAGE = 7
        const val BARRAGE_DAMAGE_SPREAD = 7
        const val BARRAGE_HEAL_RATIO = 0.66
        const val CLOUD_START_DELAY = 4
        const val LEECH = 2
        const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"
    }
}
