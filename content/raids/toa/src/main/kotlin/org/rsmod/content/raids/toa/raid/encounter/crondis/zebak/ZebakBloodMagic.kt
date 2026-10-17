package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import kotlin.math.floor
import kotlin.math.max
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.npc.vars.intVarn
import org.rsmod.api.npc.vars.typeCoordVarn
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

internal class ZebakBloodMagic(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private var countdown = -1
    private var castRequested = false
    private val clouds = ArrayList<Npc>()

    var nextIsBarrage = false
        private set

    var cloudsFromSouth = false
        private set

    fun start() {
        val active = room.raid.isActive("dbrow.toa_invocation_not_just_a_head")
        countdown = if (active) room.attackSpeed * EVERY - 1 else -1
    }

    fun tick(paused: Boolean): Boolean {
        if (castRequested) {
            castRequested = false
            return true
        }
        if (paused || countdown == -1 || --countdown != 0) return false
        countdown = room.attackSpeed * (if (room.enraged) EVERY_ENRAGED else EVERY) - 1
        return true
    }

    fun requestCast(barrage: Boolean) {
        nextIsBarrage = barrage
        castRequested = true
    }

    fun flipSpell() {
        nextIsBarrage = !nextIsBarrage
    }

    fun flipCloudSide() {
        cloudsFromSouth = !cloudsFromSouth
    }

    fun barrage() {
        val boss = room.zebak ?: return
        val targets = room.targets()
        val base = floor(BARRAGE_BASE_DAMAGE * room.raid.damageMultiplier).toInt()
        val radius = if (room.raid.isActive("dbrow.toa_invocation_arterial_spray")) 2 else 1
        var heal = 0
        for (player in targets) {
            val damage = deps.random.of(base, base + BARRAGE_DAMAGE_SPREAD)
            heal += barrageHit(boss, player, damage)
            player.spotanim(ZebakSpots.BLOOD_BARRAGE)
            player.combatPlayDefendAnim()
            for (other in targets) {
                if (other === player) continue
                if (player.coords.chebyshevDistance(other.coords) > radius) continue
                heal += barrageHit(boss, other, damage)
            }
        }
        if (heal > 0) {
            boss.heal(heal, showHitsplat = true)
            room.hpBar?.update()
        }
        for (player in targets) player.soundSynth(ZebakSynths.BLOOD_BARRAGE)
    }

    private fun barrageHit(boss: Npc, player: Player, damage: Int): Int {
        player.queueImpactHit(boss, 1, HitType.Magic, damage, deps.playerHitModifier)
        return if (player.vars[PROTECT_FROM_MAGIC] > 0) 0 else BARRAGE_HEAL
    }

    fun trackCloud(cloud: Npc) {
        cloud.switchTicks = deps.random.of(10, 20)
        cloud.startDelay = CLOUD_START_DELAY
        cloud.lastTile = cloud.coords
        clouds += cloud
    }

    fun cloudTick(cloud: Npc) {
        if (room.stage != ToaStage.STARTED) return
        if ((room.zebak?.hitpoints ?: 0) <= 0) return
        if (cloud !in clouds) return
        if (cloud.startDelay > 0) cloud.startDelay--

        val moved = (cloud.lastTile ?: cloud.coords).chebyshevDistance(cloud.coords)
        cloud.lastTile = cloud.coords
        if (moved > 0) cloud.queueNpcHit(1, HitType.Typeless, LEECH * moved, NOOP_NPC_MODIFIER)

        val targets = room.targets()
        cloud.switchTicks = max(0, cloud.switchTicks - 1)
        val current = cloud.currentTarget(targets)
        val switch = cloud.switchTicks == 0 || current == null
        if (cloud.switchTicks == 0) cloud.switchTicks = deps.random.of(10, 20)
        val target =
            if (switch && targets.isNotEmpty()) {
                nearestOther(cloud, current, targets).also { cloud.chasing = it?.uid }
            } else {
                current
            }

        if (target != null) {
            if (cloud.isBeside(target)) cloud.abortRoute() else cloud.walk(cloud.besideTile(target))
        }
        if (cloud.startDelay > 0) return

        for (player in targets) {
            if (!cloud.isBeside(player)) continue
            player.hitTypeless(LEECH)
            cloud.heal(LEECH, showHitsplat = true)
        }
    }

    private fun nearestOther(cloud: Npc, current: Player?, targets: List<Player>): Player? {
        var best = current
        var bestDistance = Int.MAX_VALUE
        for (player in deps.random.shuffled(targets)) {
            if (player === current) continue
            val distance = player.coords.chebyshevDistance(cloud.coords)
            if (distance < bestDistance) {
                bestDistance = distance
                best = player
            }
        }
        return best
    }

    fun cloudsAt(tile: CoordGrid): List<Npc> = clouds.filter { it.coords == tile }

    fun removeCloud(cloud: Npc) {
        clouds.remove(cloud)
        room.despawn(cloud)
    }

    fun forget(player: Player) {
        for (cloud in clouds) {
            if (cloud.chasing == player.uid) cloud.chasing = null
        }
    }

    fun clear() {
        for (cloud in clouds.toList()) removeCloud(cloud)
        countdown = -1
        castRequested = false
        nextIsBarrage = deps.random.of(0, 1) == 0
        cloudsFromSouth = deps.random.of(0, 1) == 0
    }

    private companion object {
        const val EVERY = 6
        const val EVERY_ENRAGED = 8
        const val BARRAGE_BASE_DAMAGE = 3
        const val BARRAGE_DAMAGE_SPREAD = 2
        const val BARRAGE_HEAL = 5
        const val CLOUD_START_DELAY = 4
        const val LEECH = 2
        const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"
    }
}

private var Npc.switchTicks by intVarn("varn.toa_zebak_cloud_switch")
private var Npc.startDelay by intVarn("varn.toa_zebak_cloud_delay")
private var Npc.lastTile by typeCoordVarn("varn.toa_zebak_cloud_last")
private var Npc.chasing by typePlayerUidVarn("varn.toa_zebak_cloud_target")

private fun Npc.currentTarget(targets: List<Player>): Player? =
    chasing?.let { uid -> targets.firstOrNull { it.uid == uid } }
