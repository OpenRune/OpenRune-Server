package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.npc.respawn.BossRespawnPolicy
import org.rsmod.api.npc.respawn.BossRespawnTimers
import org.rsmod.api.player.feet
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
internal class AraxxorController @Inject constructor(
    private val instances: InstanceManager,
    private val deps: BossDeps,
    private val interactions: AiPlayerInteractions,
    private val combat: AraxxorCombat,
    private val npcHitModifier: NpcHitModifier,
    private val routes: RouteFactory,
    private val respawns: BossRespawnTimers,
) {
    private class Fight(val owner: Player, val session: InstanceSession, val boss: Npc, val cycle: AraxxorCycle) {
        val acid = mutableMapOf<CoordGrid, LocInfo>()
        var acidTicks = 0
        var corpse: Npc? = null
        var reward: (() -> Unit)? = null
        var respawnAt = Int.MAX_VALUE
        val eggs = mutableMapOf<Int, Npc>()
        val eggHealth = IntArray(9) { AraxxorCycle.EGG_HP }
        val spiders = mutableMapOf<Npc, AraxyteKind>()
        val exploding = mutableSetOf<Npc>()
        val hatching = mutableSetOf<Npc>()
        var pendingSpecial: AraxxorSpecial? = null
        var bootsReady = 0
    }

    private val fights = mutableMapOf<InstanceId, Fight>()
    private val actors = mutableMapOf<Npc, Fight>()

    fun spawn(owner: Player, session: InstanceSession) {
        if (!valid(owner, session) || session.id in fights) return
        val tile = instances.resolveCoord(session, AraxxorArena.bossSpawn) ?: return
        val boss = spawnNpc(AraxxorAssets.BOSS, tile, session)
        check(instances.registerSessionNpc(owner, boss))
        val cycle = AraxxorCycle(AraxyteKind.entries[deps.random.of(0..2)])
        val fight = Fight(owner, session, boss, cycle)
        fights[session.id] = fight
        actors[boss] = fight
        cycle.eggs.forEachIndexed { index, kind ->
            val eggTile = checkNotNull(instances.resolveCoord(session, AraxxorArena.eggs[index]))
            val egg = spawnNpc(kind.egg, eggTile, session)
            egg.movementLocked = true
            fight.eggs[index] = egg
            actors[egg] = fight
        }
        boss.apRangeOverride = 8
        boss.anim("seq.npc_araxxor_01_spawn_01")
        deps.encounter(boss).busyUntil = deps.mapClock.cycle + 5
        boss.apPlayer2(owner, interactions)
    }

    private fun spawnNpc(symbol: String, tile: CoordGrid, session: InstanceSession): Npc {
        val cached = checkNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
        val type = cached.copy(wanderRange = 0, maxRange = 64, defaultMode = NpcMode.None)
            .also { it.paramMap = cached.paramMap }
        return Npc(type, tile).also {
            deps.npcRepo.add(it, Int.MAX_VALUE)
            it.respawns = false
            if (symbol != AraxxorAssets.BOSS) instances.attachNpc(session.id, it)
        }
    }

    private fun valid(owner: Player, session: InstanceSession): Boolean =
        session.key == AraxxorArena.KEY && session.owner == owner.uuid &&
            instances.sessionForId(session.id) === session &&
            instances.sessionForPlayer(owner) === session && owner.uuid in session.occupants &&
            !owner.loggingOut && !owner.pendingLogout && owner.hitpoints > 0 &&
            deps.playerList.any { it === owner } &&
            instances.resolveCoord(session, AraxxorArena.arrival)?.let {
                owner.coords.level == it.level && owner.coords.chebyshevDistance(it) <= 48
            } == true

    private fun active(fight: Fight): Boolean = fights[fight.session.id] === fight &&
        valid(fight.owner, fight.session) && fight.boss.isSlotAssigned && fight.boss.hitpoints > 0 &&
        fight.cycle.phase in setOf(AraxxorPhase.NORMAL, AraxxorPhase.ENRAGED)

    fun owns(npc: Npc): Boolean = npc in actors

    fun modifyHit(npc: Npc, hit: HitBuilder) {
        val fight = actors[npc] ?: return
        if (!active(fight) || npc === fight.corpse || npc in fight.hatching ||
            (hit.isFromPlayer && hit.sourceUid != fight.owner.uid.packed)) {
            hit.damage = 0
            return
        }
        if (npc === fight.boss && hit.isFromPlayer && hit.damage > 0) {
            val mirror = fight.spiders.entries.firstOrNull {
                it.value == AraxyteKind.MIRRORBACK && it.key.isSlotAssigned && it.key.hitpoints > 0
            }?.key
            if (mirror != null) {
                val redirected = minOf(mirror.hitpoints, hit.damage / 5)
                hit.damage -= redirected
                mirror.queueHit(0, HitType.Typeless, redirected, npcHitModifier)
                reflect(fight, mirror, redirected / 2)
            }
        } else if (fight.spiders[npc] == AraxyteKind.MIRRORBACK && hit.isFromPlayer &&
            hit.type == HitType.Melee && Bounds(fight.owner.coords).isWithinDistance(Bounds(npc.coords, npc.size), 1)) {
            reflect(fight, npc, minOf(hit.damage, npc.hitpoints) / 2)
        }
    }

    fun attack(npc: Npc, target: Player) {
        val fight = actors[npc]
        if (fight != null && (!active(fight) || target !== fight.owner)) return
        if (fight?.pendingSpecial != null && fight.cycle.phase == AraxxorPhase.NORMAL) {
            val special = fight.pendingSpecial!!
            fight.pendingSpecial = null
            special(fight, special)
            return
        }
        val style = combat.style(npc, target)
        if (fight?.cycle?.phase == AraxxorPhase.ENRAGED && style == HitType.Melee) {
            cleave(fight)
            return
        }
        npc.anim(when (style) {
            HitType.Melee -> AraxxorAssets.MELEE
            HitType.Ranged -> AraxxorAssets.RANGED
            else -> AraxxorAssets.MAGIC
        })
        val delay = if (style == HitType.Melee) 1 else 2
        val hit = combat.roll(npc, target, style)
        if (style != HitType.Melee) {
            projectile(npc, target.coords, if (style == HitType.Ranged) AraxxorAssets.RANGED_PROJECTILE
                else AraxxorAssets.MAGIC_PROJECTILE, delay, target)
        }
        val epoch = fight?.cycle?.epoch
        deps.worldQueues.add(delay) {
            if (fight != null) {
                if (!active(fight) || !fight.cycle.acceptsCallback(epoch!!)) return@add
            } else if (!npc.isSlotAssigned || npc.hitpoints <= 0 || target.hitpoints <= 0 ||
                target.loggingOut || target.pendingLogout || target.coords.level != npc.coords.level ||
                target.coords.chebyshevDistance(npc.coords) > 20) return@add
            if (style == HitType.Ranged && fight != null && blockWithBoots(fight)) return@add
            combat.impact(npc, target, hit)
            if (hit.damage > 0) {
                if (style == HitType.Ranged) target.statSub("stat.defence", target.stat("stat.defence") * 9 / 100, 0)
                if (style == HitType.Magic) {
                    target.spotanim(AraxxorAssets.MAGIC_IMPACT)
                    target.statSub("stat.prayer", target.stat("stat.prayer") * 9 / 100, 0)
                }
            }
        }
        if (fight != null && fight.cycle.phase == AraxxorPhase.NORMAL) {
            for ((index, egg) in fight.eggs) {
                val hp = if (egg.isSlotAssigned) egg.hitpoints.coerceAtLeast(0) else 0
                fight.cycle.damageEgg(index, (fight.eggHealth[index] - hp).coerceAtLeast(0))
                fight.eggHealth[index] = hp
            }
            val step = fight.cycle.standardAttack()
            step.hatch?.let { hatch(fight, it) }
            fight.pendingSpecial = step.special
        }
    }

    private fun hatch(fight: Fight, hatch: AraxxorCycle.Hatch) {
        val egg = fight.eggs.remove(hatch.index) ?: return
        val tile = egg.coords
        fight.hatching += egg
        egg.hideAllOps()
        egg.anim("seq.egg_araxyte_hatch_01")
        deps.worldQueues.add(2) {
            if (!active(fight) || actors[egg] !== fight) return@add
            removeActor(egg)
            if (hatch.hitpoints <= 0) return@add
            val spider = spawnNpc(hatch.kind.spider, tile, fight.session)
            spider.hitpoints = hatch.hitpoints
            spider.anim("seq.npc_araxyte02_hatch")
            fight.spiders[spider] = hatch.kind
            actors[spider] = fight
            deps.encounter(spider).busyUntil = deps.mapClock.cycle + 3
            if (hatch.kind == AraxyteKind.ACIDIC) {
                spider.apRangeOverride = 8
                spider.apPlayer2(fight.owner, interactions)
            }
        }
    }

    fun spiderAttack(npc: Npc, target: Player) {
        val fight = actors[npc] ?: return
        if (!active(fight) || target !== fight.owner || fight.spiders[npc] != AraxyteKind.ACIDIC) return
        npc.anim("seq.npc_araxyte_acidic_01_attack")
        val max = if (target.vars["varbit.prayer_protectfrommissiles"] == 1) 0 else 7
        val hit = combat.roll(npc, target, HitType.Ranged, max)
        projectile(npc, target.coords, AraxxorAssets.RANGED_PROJECTILE, 1, target)
        deps.worldQueues.add(1) {
            if (active(fight) && actors[npc] === fight && npc.isSlotAssigned && npc.hitpoints > 0) {
                combat.impact(npc, target, hit)
            }
        }
    }

    private fun reflect(fight: Fight, npc: Npc, damage: Int) {
        if (damage <= 0) return
        val hit = combat.snapshot(npc, fight.owner, HitType.Typeless, damage)
        deps.worldQueues.add(0) {
            if (active(fight)) combat.impact(npc, fight.owner, hit)
        }
    }

    private fun special(fight: Fight, special: AraxxorSpecial) {
        val boss = fight.boss
        val tile = fight.owner.coords
        val epoch = fight.cycle.epoch
        when (special) {
            AraxxorSpecial.ACID_TRAIL -> {
                boss.anim("seq.npc_araxxor_01_attack_acid_leak_01")
                projectile(boss, tile, AraxxorAssets.ACID_PROJECTILE, 2, fight.owner)
                for (delay in 2..7) deps.worldQueues.add(delay) {
                    if (active(fight) && fight.cycle.acceptsCallback(epoch)) addAcid(fight, fight.owner.coords)
                }
            }
            AraxxorSpecial.ACID_SPRAY -> {
                boss.anim("seq.npc_araxxor_01_attack_acid_spray_01")
                for (dx in -3..3) for (dz in -3..3) {
                    if (deps.random.of(0..2) != 0) continue
                    val impact = tile.translate(dx, dz)
                    projectile(boss, impact, AraxxorAssets.ACID_PROJECTILE, 3)
                    deps.worldQueues.add(3) {
                        if (!active(fight) || !fight.cycle.acceptsCallback(epoch)) return@add
                        acidSplash(fight, impact)
                    }
                }
            }
            AraxxorSpecial.ACID_BALL -> {
                boss.anim("seq.npc_araxxor_01_acid_cannon_01")
                val centre = boss.coords.translate(boss.size / 2, boss.size / 2)
                val steps = StepValidator(deps.collision)
                var from = centre
                val trajectory = AraxxorAttackRules.ray(centre, tile, 32).takeWhile { to ->
                    val pass = steps.canTravel(from.level, from.x, from.z, to.x - from.x, to.z - from.z)
                    from = to
                    pass
                }
                if (trajectory.isEmpty()) return
                projectile(boss, trajectory.last(), AraxxorAssets.ACID_PROJECTILE, trajectory.size + 1)
                for ((index, to) in trajectory.withIndex()) {
                    val distance = index + 1
                    deps.worldQueues.add(distance + 1) {
                        if (!active(fight) || !fight.cycle.acceptsCallback(epoch)) return@add
                        if (fight.owner.coords.chebyshevDistance(to) <= 1) {
                            combat.impact(boss, fight.owner, combat.snapshot(boss, fight.owner, HitType.Typeless, 18))
                            PlayerVenom.tryVenom(fight.owner)
                        }
                        addAcid(fight, to)
                    }
                }
                deps.worldQueues.add(trajectory.size + 2) {
                    if (!active(fight) || !fight.cycle.acceptsCallback(epoch)) return@add
                    val wall = trajectory.last()
                    for (sx in -3..3) for (sz in -3..3) {
                        if (deps.random.of(0..2) == 0) acidSplash(fight, wall.translate(sx, sz))
                    }
                }
            }
        }
    }

    private fun acidSplash(fight: Fight, tile: CoordGrid) {
        if (fight.owner.coords == tile) {
            combat.impact(fight.boss, fight.owner,
                combat.snapshot(fight.boss, fight.owner, HitType.Typeless, deps.random.of(4..8)))
            PlayerVenom.tryVenom(fight.owner)
        }
        addAcid(fight, tile)
    }

    private fun cleave(fight: Fight) {
        val boss = fight.boss
        val target = fight.owner.coords
        val tiles = AraxxorAttackRules.cleave(boss.coords, boss.size, target)
        val pools = if (Bounds(target).isWithinDistance(Bounds(boss.coords, boss.size), 0)) listOf(target) else tiles
        val hit = combat.roll(boss, fight.owner, HitType.Melee)
        val epoch = fight.cycle.epoch
        boss.anim(AraxxorAssets.CLEAVE)
        boss.say("Skree!")
        deps.worldQueues.add(2) {
            if (!active(fight) || !fight.cycle.acceptsCallback(epoch)) return@add
            if (fight.owner.coords in tiles && !blockWithBoots(fight)) combat.impact(boss, fight.owner, hit)
            pools.forEach { addAcid(fight, it) }
            if (tiles.any { Bounds(it).isWithinDistance(Bounds(boss.coords, boss.size), 0) }) {
                boss.queueHit(0, HitType.Typeless, deps.random.of(8..12), npcHitModifier)
            }
        }
    }

    private fun projectile(npc: Npc, tile: CoordGrid, spot: String, ticks: Int, target: Player? = null) {
        deps.worldRepo.projAnim(ProjAnim(
            spot.asRSCM(RSCMType.SPOTANIM), 70, 20, 0, ticks * 30, 15, 64, 0,
            target?.let { -(it.slotId + 1) } ?: 0, npc.coords.translate(npc.size / 2, npc.size / 2), tile,
        ))
    }

    private fun addAcid(fight: Fight, tile: CoordGrid) {
        if (tile in fight.acid) return
        val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.GROUND_DECOR or CollisionFlag.LOC
        if (deps.collision[tile.x, tile.z, tile.level] and blocked != 0) return
        fight.acid[tile] = deps.locRepo.add(tile, AraxxorAssets.ACID, Int.MAX_VALUE,
            LocAngle[0], LocShape.CentrepieceStraight)
    }

    private fun blockWithBoots(fight: Fight): Boolean {
        if (!fight.owner.feet.isType("obj.aranea_boots") || deps.mapClock.cycle < fight.bootsReady) return false
        fight.bootsReady = deps.mapClock.cycle + 8
        fight.owner.mes("Your Aranea boots let you avoid the spider-based attack.")
        return true
    }

    fun tick() {
        for (fight in fights.values.toList()) {
            if (!valid(fight.owner, fight.session)) {
                end(fight.session.id)
                continue
            }
            if (fight.cycle.phase == AraxxorPhase.FINISHED) {
                if (deps.mapClock.cycle >= fight.respawnAt) {
                    end(fight.session.id)
                    spawn(fight.owner, fight.session)
                }
                continue
            }
            if (!active(fight)) continue
            tickSpiders(fight)
            if (fight.cycle.updateHealth(fight.boss.hitpoints)) {
                fight.boss.anim(AraxxorAssets.ENRAGE)
                fight.boss.defenceLvl += 35
                fight.boss.magicLvl += 28
                fight.boss.rangedLvl += 31
                deps.encounter(fight.boss).attackRateOverride = 4
                deps.encounter(fight.boss).busyUntil = deps.mapClock.cycle + 4
            }
            if (fight.owner.coords in fight.acid) {
                val damage = 4 + fight.acidTicks++
                combat.impact(fight.boss, fight.owner,
                    combat.snapshot(fight.boss, fight.owner, HitType.Typeless, damage))
                PlayerVenom.tryVenom(fight.owner)
            } else fight.acidTicks = 0
        }
    }

    private fun tickSpiders(fight: Fight) {
        for ((spider, kind) in fight.spiders.toMap()) {
            if (!spider.isSlotAssigned || spider.hitpoints <= 0 || spider in fight.exploding) continue
            if (deps.mapClock.cycle < deps.encounter(spider).busyUntil) continue
            if (kind == AraxyteKind.ACIDIC) continue
            if (kind == AraxyteKind.RUPTURA &&
                Bounds(fight.owner.coords).isWithinDistance(Bounds(spider.coords, spider.size), 1)) {
                fight.exploding += spider
                spider.movementLocked = true
                spider.anim("seq.npc_araxyte_explode")
                deps.worldQueues.add(2) {
                    if (!active(fight) || actors[spider] !== fight || !spider.isSlotAssigned || spider.hitpoints <= 0) return@add
                    explode(fight, spider)
                }
            } else if (deps.mapClock.cycle % 2 == 0) {
                spider.walkTo(routes, fight.owner.coords)
            }
        }
    }

    private fun explode(fight: Fight, spider: Npc) {
        val health = spider.hitpoints.coerceIn(0, AraxxorCycle.SPIDER_HP)
        val centre = spider.coords.translate(spider.size / 2, spider.size / 2)
        val distance = fight.owner.coords.chebyshevDistance(centre)
        if (distance <= 3) {
            val maximum = when (distance) { 0, 1 -> 80; 2 -> 40; else -> 7 }
            combat.impact(spider, fight.owner,
                combat.snapshot(spider, fight.owner, HitType.Typeless, maximum * health / AraxxorCycle.SPIDER_HP))
        }
        val targets = listOf(fight.boss) + fight.eggs.values + fight.spiders.keys.filter { it !== spider }
        for (target in targets) {
            if (!target.isSlotAssigned || target.hitpoints <= 0) continue
            val bounds = Bounds(target.coords, target.size)
            val maximum = when {
                Bounds(centre).isWithinDistance(bounds, 0) -> 80
                Bounds(centre).isWithinDistance(bounds, 1) -> 64
                Bounds(centre).isWithinDistance(bounds, 3) -> 33
                else -> continue
            }
            target.queueHit(0, HitType.Typeless, maximum * health / AraxxorCycle.SPIDER_HP, npcHitModifier)
        }
        spider.spotanim("spotanim.araxyte_explosive_spider_explosion")
        spider.hitpoints = 0
        spider.queueDeath()
    }

    fun minionDeath(npc: Npc): Boolean {
        val fight = actors[npc] ?: return false
        val egg = fight.eggs.entries.firstOrNull { it.value === npc }
        if (egg != null) {
            fight.cycle.damageEgg(egg.key, AraxxorCycle.EGG_HP)
            fight.eggs.remove(egg.key)
        }
        if (fight.spiders.remove(npc) == AraxyteKind.ACIDIC && active(fight)) {
            for (dx in -3..3) for (dz in -3..3) {
                if (deps.random.of(0..2) == 0) acidSplash(fight, npc.coords.translate(dx, dz))
            }
        }
        fight.exploding.remove(npc)
        return true
    }

    fun removeActor(npc: Npc) {
        val fight = actors.remove(npc) ?: return
        fight.spiders.remove(npc)
        fight.exploding.remove(npc)
        fight.hatching.remove(npc)
        instances.detachNpc(npc)
        if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    fun beginDeath(npc: Npc): Boolean {
        val fight = actors[npc] ?: return false
        if (npc !== fight.boss || !valid(fight.owner, fight.session) || !fight.cycle.die()) return false
        instances.handleBossKill(npc, deps.mapClock.cycle)
        clearAcid(fight)
        (fight.eggs.values.toList() + fight.spiders.keys.toList() + fight.hatching.toList()).forEach(::removeActor)
        fight.eggs.clear()
        return true
    }

    fun finishDeath(npc: Npc, reward: () -> Unit) {
        val fight = actors[npc] ?: return
        if (!valid(fight.owner, fight.session) || fight.cycle.phase != AraxxorPhase.CORPSE || fight.corpse != null) return
        val corpseSize = checkNotNull(ServerCacheManager.getNpc(AraxxorAssets.CORPSE.asRSCM())).size
        val offset = (npc.size - corpseSize) / 2
        val corpse = spawnNpc(AraxxorAssets.CORPSE, npc.coords.translate(offset, offset), fight.session)
        corpse.hitpoints = 1
        fight.corpse = corpse
        fight.reward = reward
        actors[corpse] = fight
        deps.npcRepo.hide(npc, Int.MAX_VALUE)
    }

    fun harvest(player: Player, corpse: Npc): Boolean {
        val fight = actors[corpse] ?: return false
        if (fight.owner !== player || !valid(player, fight.session) || fight.corpse !== corpse ||
            !corpse.isSlotAssigned || !fight.cycle.claim()) return false
        val reward = fight.reward
        fight.reward = null
        corpse.hideAllOps()
        corpse.anim(AraxxorAssets.HARVEST)
        fight.respawnAt = deps.mapClock.cycle + BossRespawnPolicy.OTHER_BOSS_TICKS
        respawns.schedule(fight.boss, BossRespawnPolicy.OTHER_BOSS_TICKS)
        reward?.invoke()
        return true
    }

    fun deleted(npc: Npc) {
        val fight = actors.remove(npc) ?: return
        instances.detachNpc(npc)
        if (npc === fight.boss || npc === fight.corpse) end(fight.session.id)
    }

    fun end(id: InstanceId) {
        val fight = fights.remove(id) ?: return
        fight.cycle.dispose()
        respawns.cancel(fight.boss)
        fight.reward = null
        clearAcid(fight)
        for (npc in actors.filterValues { it === fight }.keys.toList()) {
            actors.remove(npc)
            instances.detachNpc(npc)
            if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private fun clearAcid(fight: Fight) {
        fight.acid.values.forEach { deps.locRepo.del(it, Int.MAX_VALUE) }
        fight.acid.clear()
    }
}
