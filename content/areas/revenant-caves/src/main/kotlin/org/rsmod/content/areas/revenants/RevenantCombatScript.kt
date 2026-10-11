package org.rsmod.content.areas.revenants

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.heal
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.*
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class RevenantCombatScript @Inject constructor(private val deps: BossDeps, private val npcs: NpcList) : PluginScript() {
    private var running = false
    private val provoked = java.util.IdentityHashMap<Npc, Int>()
    private val heals = java.util.IdentityHashMap<Npc, HealState>()
    internal val specs = RevenantSpecies.entries.map { species -> boss(species.npcType) {
        stats(attackRate = 5, aggressionRadius = 8)
        val attack = ability("attack") { include(external("revenants.normal")) }
        phase("combat") { weightedSelectorRandom { +random(attack, weight = 1) } }
    } }
    override fun ScriptContext.startup() {
        running = true
        deps.extensionRegistry.register("revenants.normal") { _, npc, player, _ -> attack(npc, player) }
        for (spec in specs) BossCombat.register(this, spec, deps, onModifyHit = { if (hit.isFromPlayer) provoked[npc] = hit.sourceUid ?: 0 }, onCombatTick = { target ->
            if (target.isInPvpCombat() || (RevenantCharges.protected(target) && provoked[npc] != target.uid.packed)) {
                deps.encounter(npc).busyUntil = deps.mapClock.cycle + 1
                noneMode()
            }
        })
        onEvent<PlayerHitEvents.Modify> {
            val source = hit.sourceSlot?.let { npcs[it] }
            if (hit.isFromNpc && source != null && source.uid.packed == hit.sourceUid && RevenantSpecies.of(source) != null) {
                hit.damage = RevenantCharges.reduceDamage(player, hit.damage)
            }
        }
        onEvent<NpcStateEvents.Create> { if (RevenantSpecies.of(npc) != null) npc.apRangeOverride = 8 }
        onEvent<NpcStateEvents.Respawn> { if (RevenantSpecies.of(npc) != null) reset(npc) }
        onEvent<NpcStateEvents.Delete> { if (RevenantSpecies.of(npc) != null) reset(npc) }
    }
    override fun ScriptContext.shutdown() {
        running = false; heals.clear(); provoked.clear(); deps.extensionRegistry.unregister("revenants.normal")
    }
    private fun reset(npc: Npc) { heals.remove(npc); provoked.remove(npc); npc.apRangeOverride = 8 }
    private fun attack(npc: Npc, target: Player) {
        val species = RevenantSpecies.of(npc) ?: return
        if (!running || target.pendingLogout || target.hitpoints == 0 || target.isInPvpCombat()) return
        healIfReady(npc)
        val type = style(npc, target)
        npc.anim(RSCM.getReverseMapping(RSCMType.SEQ, npc.param(params.attack_anim).id))
        val accurate = when (type) {
            HitType.Melee -> deps.accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Slash, deps.random)
            HitType.Ranged -> deps.accuracy.rollRangedAccuracy(npc, target, deps.random)
            else -> deps.accuracy.rollMagicAccuracy(npc, target, deps.random)
        }
        val damage = if (accurate) deps.random.of(species.maxHit + 1) else 0
        if (type == HitType.Melee) { target.finishNpcHit(npc, 1, type, damage, deps.playerHitModifier); return }
        val encounter = deps.encounter(npc)
        val uid = target.uid; val originalTile = target.coords
        deps.bossProjectile("spotanim.revenant_magic_travel".asRSCM(), npc.coords.translate(npc.size / 2, npc.size / 2), target.coords,
            startHeight = 80, endHeight = 30, delay = 30, travel = 60, curve = 15, homing = target)
        deps.worldQueues.add(3) {
            val victim = uid.resolve(deps.playerList)
            if (!running || !npc.isSlotAssigned || npc.hitpoints == 0 || !deps.encounterRegistry.isActive(encounter) ||
                victim == null || victim.hitpoints == 0 || victim.pendingLogout || victim.isInPvpCombat() ||
                victim.coords.level != npc.coords.level || victim.coords.chebyshevDistance(npc.coords) > 16) return@add
            val victims = if (type == HitType.Magic) (listOf(victim) + deps.playerList.filter {
                it !== victim && it.coords == originalTile && it.hitpoints > 0 && !it.isInPvpCombat()
            }).take(10) else listOf(victim)
            for (player in victims) {
                player.finishNpcHit(npc, 1, type, damage, deps.playerHitModifier)
                if (type == HitType.Magic && accurate) {
                    player.spotanim("spotanim.revenant_magic_impact")
                    CombatEffects.freeze(player, 5)
                }
            }
        }
    }
    internal fun healIfReady(npc: Npc): Boolean {
        val state = heals[npc] ?: HealState(deps.random.of(8, 25)).also { heals[npc] = it }
        if (npc.hitpoints * 2 >= npc.type.hitpoints || state.remaining == 0 || deps.mapClock.cycle < state.nextTick) return false
        npc.spotanim("spotanim.saradomin_lightning")
        npc.heal(20, showHitsplat = true); state.remaining--; state.nextTick = deps.mapClock.cycle + 15
        return true
    }
    internal companion object {
        private class HealState(var remaining: Int, var nextTick: Int = 0)
                fun style(npc: Npc, player: Player): HitType {
            val types = listOf(HitType.Magic, HitType.Ranged, HitType.Melee)
            return types.filter { it != HitType.Melee || player.coords.chebyshevDistance(npc.coords) <= npc.size }.minBy { type ->
                val prayer = when (type) { HitType.Melee -> "varbit.prayer_protectfrommelee"; HitType.Ranged -> "varbit.prayer_protectfrommissiles"; else -> "varbit.prayer_protectfrommagic" }
                val defence = when (type) { HitType.Melee -> params.defence_slash; HitType.Ranged -> params.defence_ranged; else -> params.defence_magic }
                player.worn.objs.filterNotNull().sumOf { getInvObj(it).param(defence) } + if (player.vars[prayer] == 1) 100_000 else 0
            }
        }
    }
}
