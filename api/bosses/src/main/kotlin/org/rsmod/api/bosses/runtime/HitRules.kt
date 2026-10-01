package org.rsmod.api.bosses.runtime

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.HitReaction
import org.rsmod.api.bosses.spec.HitType as BossHitType
import org.rsmod.api.bosses.spec.IncomingAction
import org.rsmod.api.combat.commons.DemonbaneChecks
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitType

/** Applies a spec's [BossSpec.incomingRules] and [BossSpec.hitReactions]; wired up by [BossCombat]. */
internal class HitRules(private val spec: BossSpec, private val deps: BossDeps) {
    private val reactions: List<Pair<HitReaction, List<ItemServerType>>> =
        spec.hitReactions.map { reaction ->
            reaction to reaction.withObj.map { name ->
                ServerCacheManager.getItem(name.asRSCM(RSCMType.OBJ)) ?: error("Obj type not found: $name")
            }
        }

    val hasReactions: Boolean
        get() = reactions.isNotEmpty()

    fun applyIncoming(event: NpcHitEvents.Modify, encounter: BossEncounter) {
        if (spec.incomingRules.isEmpty() || !event.hit.isFromPlayer) return
        val attacker = event.hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) } ?: return
        val context = with(event.hit) { HitContext(type, damage, righthandType(), secondaryType()) }
        val rule =
            spec.incomingRules.firstOrNull {
                encounter.evaluate(it.condition, attacker, hit = context)
            } ?: return
        for (action in rule.actions) {
            val style = action.style
            if (style != null && style.toEngine() != event.hit.type) continue
            val hit = event.hit
            when (action) {
                is IncomingAction.Cap -> hit.damage = min(hit.damage, action.max)
                is IncomingAction.ScalePercent -> hit.damage = hit.damage * action.percent / 100
                is IncomingAction.FloorPercentOfMaxHit -> {
                    val maxHit = playerMaxHit(attacker, event.npc, action.style)
                    val floor = (maxHit * action.percent + 99) / 100
                    if (hit.damage < floor) hit.damage = deps.random.of(floor, max(floor, maxHit))
                }
                is IncomingAction.Run -> run(event.npc, attacker, encounter, action)
            }
        }
    }

    fun react(event: NpcHitEvents.Impact, encounter: BossEncounter) {
        if (!event.hit.isFromPlayer || event.npc.hitpoints <= 0) return
        val attacker = event.hit.resolvePlayerSource(deps.playerList) ?: return
        val context = with(event.hit) { HitContext(type, damage, righthandType(), secondaryType()) }
        for ((reaction, objs) in reactions) {
            if (objs.isNotEmpty() && objs.none(event.hit::isSecondaryObj)) continue
            if (!encounter.evaluate(reaction.requires, attacker, hit = context)) continue
            EffectInterpreter(event.npc, attacker, spec, encounter, deps).run(null, reaction.effect)
        }
    }

    private fun run(npc: Npc, attacker: Player, encounter: BossEncounter, action: IncomingAction.Run) {
        EffectInterpreter(npc, attacker, spec, encounter, deps).run(null, action.effect)
    }

    private fun playerMaxHit(attacker: Player, npc: Npc, style: BossHitType): Int =
        when (style) {
            BossHitType.Ranged -> deps.maxHit.getRangedMaxHit(attacker, npc, null, null, 1.0, 0)
            BossHitType.Melee -> deps.maxHit.getMeleeMaxHit(attacker, npc, null, null, 1.0)
            else -> error("No player max hit for $style")
        }
}

/**
 * A player hit on the boss, for hit conditions: before the incoming rules settle it (`Modify`) or
 * as it landed (`Impact`). The objs are the ones snapshot when the hit was queued.
 */
class HitContext(
    val type: HitType,
    val damage: Int,
    val righthand: ItemServerType?,
    val secondary: ItemServerType?,
) {
    val demonbane: Boolean
        get() = DemonbaneChecks.isDemonbane(type, righthand, secondary)
}

internal fun BossHitType.toEngine(): HitType =
    when (this) {
        BossHitType.Melee -> HitType.Melee
        BossHitType.Ranged -> HitType.Ranged
        BossHitType.Typeless -> HitType.Typeless
        BossHitType.Magic,
        BossHitType.Dragonfire,
        BossHitType.DragonfireMetal,
        BossHitType.WyvernIce -> HitType.Magic
    }
