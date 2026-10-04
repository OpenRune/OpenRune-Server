package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.respawn.BossRespawnPolicy
import org.rsmod.api.npc.respawn.BossRespawnTimers
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.util.Bounds

/** Keeps each encounter tied to its original map whirlpools, including their respawn lifecycle. */
@Singleton
@OptIn(org.rsmod.annotations.InternalApi::class)
internal class KrakenController @Inject constructor(
    private val deps: BossDeps,
    private val interactions: AiPlayerInteractions,
    private val playerInteractions: NpcInteractions,
    private val respawns: BossRespawnTimers,
) {
    internal data class Actor(val pool: Npc, val npc: Npc, val kind: KrakenKind, val owner: Player,
        val centre: Npc, var dying: Boolean = false)
    private val pools = java.util.Collections.newSetFromMap(IdentityHashMap<Npc, Boolean>())
    private val actors = IdentityHashMap<Npc, Actor>()
    private val testers = java.util.Collections.newSetFromMap(IdentityHashMap<Player, Boolean>())

    private fun inCove(player: Player) = player.coords.level == 0 && player.coords.x in 2240..2303 &&
        player.coords.z in 9984..10047

    fun setTesting(player: Player, enabled: Boolean) {
        if (player.modLevel != Rights.ADMINISTRATOR) return
        if (!enabled) {
            testers.remove(player)
            player.mes("Kraken test access disabled.")
        } else if (!inCove(player)) {
            player.mes("Enter Kraken Cove first, then use ::krakentest.")
        } else {
            testers += player
            player.mes("Kraken test access enabled until you leave. Your Slayer task is unchanged.")
        }
    }

    fun allowed(player: Player) = KrakenRules.allowed(player) ||
        (player in testers && player.modLevel == Rights.ADMINISTRATOR && inCove(player))

    fun created(npc: Npc) {
        if (KrakenKind.entries.any { it.poolId == npc.type.id }) {
            pools += npc
            npc.movementLocked = true
        }
    }

    fun deleted(npc: Npc) {
        pools.remove(npc)
        actors.remove(npc)?.let { restore(it.pool, 1) }
    }

    private fun nearby(a: Npc, b: Npc) = Bounds(a.spawnCoords).isWithinDistance(Bounds(b.spawnCoords), 16)
    private fun centre(pool: Npc, kind: KrakenKind): Npc? = when (kind) {
        KrakenKind.TENTACLE -> pools.firstOrNull { it.type.id == KrakenKind.BOSS.poolId && nearby(it, pool) }
        else -> pool
    }

    fun canDisturb(player: Player, pool: Npc): Boolean {
        if (!allowed(player)) {
            player.mes("You need level 87 Slayer and a cave kraken task to disturb this whirlpool.")
            return false
        }
        val kind = KrakenKind.entries.firstOrNull { it.poolId == pool.type.id } ?: return false
        val centre = centre(pool, kind) ?: return false
        if (actors.values.any { it.centre === centre && it.owner !== player }) {
            player.mes("Someone else is already fighting this kraken.")
            return false
        }
        return pool in pools && !pool.hidden && actors.values.none { it.pool === pool }
    }

    fun disturb(player: Player, pool: Npc, explosive: Boolean = false): Boolean {
        if (!canDisturb(player, pool)) return false
        val kind = KrakenKind.entries.single { it.poolId == pool.type.id }
        if (explosive && kind != KrakenKind.BOSS) {
            player.mes("Use the fishing explosive on the large Kraken whirlpool.")
            return false
        }
        val centre = centre(pool, kind) ?: return false
        if (kind == KrakenKind.BOSS) {
            val tentacles = pools.filter { it.type.id == KrakenKind.TENTACLE.poolId && nearby(it, pool) }
            if (tentacles.size != 4) {
                player.mes("The surrounding whirlpools are not ready yet.")
                return false
            }
            if (explosive) tentacles.filter { p -> actors.values.none { it.pool === p } }
                .forEach { awaken(player, it, KrakenKind.TENTACLE, centre) }
            if (tentacles.any { p -> actors.values.none { it.pool === p } }) {
                player.mes("Disturb all four smaller whirlpools first, or use a fishing explosive.")
                return false
            }
        }
        awaken(player, pool, kind, centre)
        return true
    }

    private fun awaken(player: Player, pool: Npc, kind: KrakenKind, centre: Npc) {
        val npc = Npc(checkNotNull(ServerCacheManager.getNpc(kind.activeId)), pool.spawnCoords)
        deps.npcRepo.hide(pool, Int.MAX_VALUE)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npc.movementLocked = true
        npc.apRangeOverride = 10
        actors[npc] = Actor(pool, npc, kind, player, centre)
        npc.anim(kind.spawn)
        deps.encounter(npc).busyUntil = deps.mapClock.cycle + kind.spawnTicks
        npc.movementLocked = true
        npc.apPlayer2(player, interactions)
        if (kind != KrakenKind.TENTACLE) {
            val actor = checkNotNull(actors[npc])
            deps.worldQueues.add(kind.spawnTicks) {
                if (actors[npc] === actor && valid(actor) && !actor.dying)
                    playerInteractions.interact(player, npc, InteractionOp.Op2)
            }
        }
    }

    fun modify(npc: Npc, hit: HitBuilder) {
        val actor = actors[npc]
        val kind = KrakenKind.entries.firstOrNull { it.activeId == npc.type.id } ?: return
        val foreignHit = actor != null && hit.isFromPlayer && hit.sourceUid != actor.owner.uid.packed
        hit.damage = if (actor?.dying == true || foreignHit) 0 else KrakenRules.damage(kind, hit.type, hit.damage)
    }

    fun attack(npc: Npc, target: Player) {
        val actor = actors[npc] ?: return
        if (actor.dying || !valid(actor)) return
        if (target !== actor.owner) {
            npc.apPlayer2(actor.owner, interactions)
            return
        }
        npc.anim(actor.kind.attack)
        val accurate = if (actor.kind == KrakenKind.CAVE) deps.accuracy.rollMagicAccuracy(npc, target, deps.random)
            else deps.accuracy.rollMagicalRangedAccuracy(npc, target, deps.random)
        val damage = if (accurate)
            deps.random.of(0..actor.kind.maximum) else 0
        val projectile = if (actor.kind == KrakenKind.BOSS) "spotanim.firewave_travel" else "spotanim.waterwave_travel"
        deps.worldRepo.projAnim(ProjAnim(projectile.asRSCM(), 40, 30, 0, 60, 15, 64, 0,
            -(target.slotId + 1), npc.coords.translate(npc.size / 2, npc.size / 2), target.coords))
        deps.worldQueues.add(2) {
            if (actors[npc] === actor && !actor.dying && valid(actor))
                target.finishNpcHit(npc, 0, if (actor.kind == KrakenKind.CAVE) HitType.Magic else HitType.Typeless,
                    damage, deps.playerHitModifier)
        }
    }

    fun owner(npc: Npc): Player? = actors[npc]?.owner
    fun beginDeath(npc: Npc): Actor? = actors[npc]?.also { actor ->
        actor.dying = true
        if (actor.kind == KrakenKind.BOSS) {
            actors.values.filter { it.centre === actor.centre && it !== actor }.forEach {
                it.dying = true
                it.npc.anim(it.kind.death)
            }
        }
    }

    fun finishDeath(actor: Actor) {
        val ticks = if (actor.kind == KrakenKind.CAVE) 25 else BossRespawnPolicy.OTHER_BOSS_TICKS
        if (actor.kind == KrakenKind.BOSS) {
            respawns.schedule(actor.npc, ticks, retainAfterDelete = true)
            actors.values.filter { it.centre === actor.centre && it !== actor }.toList().forEach { remove(it, ticks) }
        }
        remove(actor, ticks)
    }

    private fun restore(pool: Npc, delay: Int) {
        if (pool in pools && pool.isSlotAssigned) pool.lifecycleRevealCycle = deps.mapClock.cycle + delay
    }
    private fun remove(actor: Actor, delay: Int) {
        actors.remove(actor.npc)
        if (actor.npc.isSlotAssigned) deps.npcRepo.del(actor.npc, Int.MAX_VALUE)
        restore(actor.pool, delay)
    }
    private fun valid(actor: Actor) = actor.centre.isSlotAssigned && actor.owner.isSlotAssigned && !actor.owner.pendingLogout &&
        !actor.owner.loggingOut && actor.owner.hitpoints > 0 &&
        Bounds(actor.owner.coords).isWithinDistance(Bounds(actor.centre.coords, actor.centre.size), 16)

    fun tick() {
        testers.removeIf { !it.isSlotAssigned || it.pendingLogout || it.loggingOut || !inCove(it) }
        actors.values.filter { !valid(it) }.toList().forEach { remove(it, 1) }
    }
}
