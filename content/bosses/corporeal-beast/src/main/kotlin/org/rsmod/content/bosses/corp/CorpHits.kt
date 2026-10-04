package org.rsmod.content.bosses.corp

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType

internal class CorpHits @Inject constructor(
    private val modifier: PlayerHitModifier,
    private val processor: InstantPlayerHitProcessor,
) {
    fun snapshot(npc: Npc, target: Player, style: HitType, damage: Int): Hit {
        val group = hitmark_groups.regular_damage
        val zero = hitmark_groups.zero_damage
        val mark = group.lit.asRSCM(RSCMType.HITMARK)
        val builder = HitBuilder(
            type = style, damage = if (style == HitType.Magic && target.vars["varbit.prayer_protectfrommagic"] == 1) damage.coerceAtLeast(0) * 2 / 3 else damage.coerceAtLeast(0), sourceUid = npc.uid.packed,
            sourceSlot = npc.slotId, isFromNpc = true, isFromPlayer = false, clientDelay = 0,
            righthandType = null, secondaryType = null,
            targetHitmark = mark, sourceHitmark = mark,
            publicHitmark = group.tint?.asRSCM(RSCMType.HITMARK) ?: mark,
            zeroDamageHitmarkLit = zero.lit.asRSCM(RSCMType.HITMARK),
            zeroDamageHitmarkTint = zero.tint?.asRSCM(RSCMType.HITMARK),
            maxDamageHitmarkLit = group.max?.asRSCM(RSCMType.HITMARK),
            targetMaxDamageThreshold = Int.MAX_VALUE, sourceMaxDamageThreshold = Int.MAX_VALUE,
        )
        builder.penetration = if (style == HitType.Magic) 100 else 0
        with(modifier) { builder.modify(target) }
        return builder.build()
    }

    fun impact(npc: Npc, target: Player, hit: Hit): Int {
        if (target.hitpoints <= 0) return 0
        val before = target.hitpoints
        with(processor) { target.process(hit) }
        target.combatPlayDefendAnim()
        if (npc.isSlotAssigned && npc.hitpoints > 0) target.queueCombatRetaliate(npc)
        return (before - target.hitpoints).coerceAtLeast(0)
    }
}
