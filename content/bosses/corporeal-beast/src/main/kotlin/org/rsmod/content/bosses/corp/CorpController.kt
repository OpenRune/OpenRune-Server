package org.rsmod.content.bosses.corp

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

@Singleton
@OptIn(org.rsmod.annotations.InternalApi::class)
internal class CorpController @Inject constructor(
    private val deps: BossDeps,
    private val hits: CorpHits,
    private val interactions: AiPlayerInteractions,
) {
    internal class Fight(val boss: Npc, var lastHit: Int) {
        val participants = mutableSetOf<Long>()
        var target: Player? = null
        var core: Npc? = null
        var coreNext = 0
        var coreSlowed = false
        var poisonUsed = false
        var dying = false
        var stompAt = lastHit + 7
        var regenAt = lastHit + 20
    }
    private val fights = IdentityHashMap<Npc, Fight>()
    fun created(npc: Npc) {
        if (npc.type.id != CorpRules.BOSS.asRSCM()) return
        fights[npc]?.let { removeCore(it) }
        fights[npc] = Fight(npc, deps.mapClock.cycle)
        npc.apRangeOverride = 10
        npc.regenClock = Int.MAX_VALUE
    }
    private fun fight(npc: Npc): Fight = fights[npc] ?: run { created(npc); checkNotNull(fights[npc]) }
    private fun players(f: Fight) = deps.playerList.filter { valid(it, f) }
    private fun valid(p: Player, f: Fight) = p.isSlotAssigned && !p.pendingLogout && !p.loggingOut &&
        p.hitpoints > 0 && CorpRules.inRoom(p.coords, f.boss.spawnCoords)
    private fun alive(f: Fight) = fights[f.boss] === f && f.boss.isSlotAssigned && f.boss.hitpoints > 0 && !f.dying
    fun damaged(npc: Npc, hit: Hit) {
        val f = fight(npc)
        if (hit.isFromPlayer) hit.resolvePlayerSource(deps.playerList)?.uuid?.let { f.participants += it }
        if (hit.damage > 0) f.lastHit = deps.mapClock.cycle
        if (hit.damage >= 32) tryCore(f)
    }
    fun attack(npc: Npc, target: Player) {
        val f = fight(npc)
        if (!alive(f) || !valid(target, f)) return
        deps.encounter(npc).attackRateOverride = if (deps.random.of(0..9) == 0) 3 else 4
        val melee = Bounds(npc.coords, npc.size).isWithinDistance(Bounds(target.coords), 1)
        f.target = target
        target.uuid?.let { f.participants += it }
        val style = CorpRules.style(melee, deps.random.of(0..99))
        if (style == 0) {
            npc.anim("seq.corpbeast_swiping_attack")
            val damage = if (deps.accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, deps.random)) deps.random.of(0..CorpRules.meleeMax(npc.strengthLvl)) else 0
            pending(f, target, hits.snapshot(npc, target, HitType.Melee, damage), 1)
        } else {
            npc.anim("seq.corpbeast_sprite_shoot_$style")
            val spot = when (style) { 1 -> "spotanim.corp_spirit_beast_strong_proj"; 2 -> "spotanim.corp_spirit_beast_mid_proj"; else -> "spotanim.corp_spirit_beast_weak_proj" }
            val tile = target.coords
            projectile(npc.coords.translate(2, 2), tile, spot, if (style == 3) 0 else -(target.slotId + 1), 60)
            if (style == 3) {
                deps.worldQueues.add(2) { if (alive(f)) {
                    splash(f, tile, true)
                    val offsets = listOf(-2 to -2, 0 to -3, 2 to -2, -2 to 2, 0 to 3, 2 to 2)
                    for ((dx, dz) in offsets) {
                        val to = tile.translate(dx, dz)
                        projectile(tile, to, "spotanim.corp_spirit_beast_weak_proj", 0, 30)
                        deps.worldQueues.add(1) { if (alive(f)) splash(f, to, false) }
                    }
                } }
            } else {
                val maximum = if (style == 1) 65 else 55
                val damage = if (deps.accuracy.rollMagicAccuracy(npc, target, deps.random)) deps.random.of(0..maximum) else 0
                pending(f, target, hits.snapshot(npc, target, HitType.Magic, damage), 2, style == 2)
            }
        }
        if (npc.hitpoints < 1000) tryCore(f)
    }
    private fun pending(f: Fight, target: Player, hit: Hit, delay: Int, drain: Boolean = false) {
        deps.worldQueues.add(delay) {
            if (alive(f) && valid(target, f)) {
                val dealt = hits.impact(f.boss, target, hit)
                if (drain && dealt > 0) {
                    heal(f, dealt / 2)
                    if (deps.random.of(0..1) == 0) target.statSub(if (deps.random.of(0..1) == 0) "stat.magic" else "stat.prayer", deps.random.of(1..2), 0)
                }
            }
        }
    }
    private fun splash(f: Fight, tile: CoordGrid, main: Boolean) {
        deps.worldRepo.spotanimMap(dev.openrune.types.aconverted.SpotanimType("spotanim.corp_magic_splash".asRSCM()), tile, 0, 0)
        for (p in players(f)) {
            val maximum = CorpRules.splashMaximum(tile, p.coords, main)
            if (maximum > 0) { p.uuid?.let { f.participants += it }; hits.impact(f.boss, p, hits.snapshot(f.boss, p, HitType.Magic, deps.random.of(0..maximum))) }
        }
    }
    private fun projectile(from: CoordGrid, to: CoordGrid, spot: String, target: Int, duration: Int) {
        deps.worldRepo.projAnim(ProjAnim(spot.asRSCM(), 40, 0, 0, duration, 15, 64, 0, target, from, to))
    }
    private fun heal(f: Fight, amount: Int) { f.boss.hitpoints = (f.boss.hitpoints + amount).coerceAtMost(f.boss.baseHitpointsLvl) }
    private fun tryCore(f: Fight) {
        if (!alive(f) || f.core != null || deps.random.of(0..7) != 0) return
        val target = players(f).maxWithOrNull(compareBy<Player> { it.coords.z }.thenBy { it.coords.x }) ?: return
        val core = Npc(checkNotNull(ServerCacheManager.getNpc(CorpRules.CORE.asRSCM())), target.coords)
        deps.npcRepo.add(core, Int.MAX_VALUE)
        core.respawns = false; core.movementLocked = true; core.ignoreCombatInteractions = true
        f.core = core; f.poisonUsed = false; f.coreSlowed = false
        deps.npcRepo.hide(core, 2)
        projectile(f.boss.coords.translate(2, 2), target.coords, "spotanim.dark_core_jump", 0, 60)
        f.coreNext = deps.mapClock.cycle + 2
    }
    private fun coreTick(f: Fight, now: Int) {
        val core = f.core ?: return
        if (!core.isSlotAssigned || core.hitpoints <= 0) { removeCore(f); return }
        val targets = players(f)
        val adjacent = targets.filter { Bounds(core.coords).isWithinDistance(Bounds(it.coords), 1) }
        if (f.coreSlowed && adjacent.isEmpty()) { f.coreSlowed = false; f.coreNext = now }
        if (now < f.coreNext) return
        if (adjacent.isEmpty()) {
            val target = targets.maxWithOrNull(compareBy<Player> { it.coords.z }.thenBy { it.coords.x }) ?: return
            f.coreSlowed = false
            projectile(core.coords, target.coords, "spotanim.dark_core_jump", 0, 60)
            deps.npcRepo.hide(core, 2)
            core.teleport(deps.collision, target.coords)
            f.coreNext = now + 2
        } else {
            if (!f.poisonUsed && core.vars["varn.poison_severity"] > 0) { f.poisonUsed = true; f.coreSlowed = true }
            for (p in adjacent) heal(f, hits.impact(core, p, hits.snapshot(core, p, HitType.Typeless, deps.random.of(5..13))))
            f.coreNext = now + if (f.coreSlowed) 100 else 2
        }
    }
    fun tick() {
        val now = deps.mapClock.cycle
        for (f in fights.values.toList()) {
            if (!alive(f)) continue
            val ps = players(f)
            val npc = f.boss
            npc.regenClock = Int.MAX_VALUE
            if (!CorpRules.inRoom(npc.coords, npc.spawnCoords)) npc.teleport(deps.collision, npc.spawnCoords)
            if (now >= f.regenAt) {
                if (npc.attackLvl < npc.baseAttackLvl) npc.attackLvl++
                if (npc.strengthLvl < npc.baseStrengthLvl) npc.strengthLvl++
                if (npc.defenceLvl < npc.baseDefenceLvl) npc.defenceLvl++
                if (npc.magicLvl < npc.baseMagicLvl) npc.magicLvl++
                if (npc.rangedLvl < npc.baseRangedLvl) npc.rangedLvl++
                f.regenAt = now + 20
            }
            if (now >= f.stompAt) {
                f.stompAt = now + 7
                val under = ps.filter { it.coords.x in npc.coords.x until npc.coords.x + npc.size && it.coords.z in npc.coords.z until npc.coords.z + npc.size }
                if (under.isNotEmpty()) {
                    npc.anim("seq.corpbeast_stomp_attack")
                    deps.worldRepo.spotanimMap(dev.openrune.types.aconverted.SpotanimType("spotanim.corp_ground_stomp".asRSCM()), npc.coords.translate(2, 2), 0, 0)
                    for (p in under) { p.uuid?.let { f.participants += it }; hits.impact(npc, p, hits.snapshot(npc, p, HitType.Typeless, deps.random.of(30..51))) }
                }
                heal(f, if (ps.isEmpty()) 25 else CorpRules.crowdHeal(ps.size))
            }
            if (ps.isEmpty()) {
                f.target = null
                removeCore(f)
                if (now - f.lastHit >= 300) { heal(f, npc.baseHitpointsLvl); npc.clearHeroPoints(); f.participants.clear() }
            } else {
                if (f.target == null || !valid(f.target!!, f) || !npc.hasInteraction()) {
                    f.target = ps.first()
                    npc.apPlayer2(f.target!!, interactions)
                }
                coreTick(f, now)
            }
        }
    }
    private fun removeCore(f: Fight) { f.core?.let { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }; f.core = null }
    fun canReward(npc: Npc): Boolean = fights[npc]?.let { f ->
        npc.spawnCoords.z !in 4352..4415 || f.participants.size <= 1
    } ?: true
    fun dying(npc: Npc) { fights[npc]?.let { it.dying = true; removeCore(it) } }
    fun deleted(npc: Npc) { fights.remove(npc)?.let { removeCore(it) }; fights.values.filter { it.core === npc }.forEach { it.core = null } }
    fun occupants(origin: CoordGrid) = deps.playerList.count { it.isSlotAssigned && CorpRules.inRoom(it.coords, origin) }
}
