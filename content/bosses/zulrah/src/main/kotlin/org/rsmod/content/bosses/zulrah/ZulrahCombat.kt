package org.rsmod.content.bosses.zulrah

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.accuracy.magic.NvPMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.melee.NvPMeleeAccuracy
import org.rsmod.api.combat.formulas.accuracy.ranged.NvPRangedAccuracy
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.hit.takeInstantHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType

internal class ZulrahCombat @Inject constructor(
    private val magic: NvPMagicAccuracy,
    private val ranged: NvPRangedAccuracy,
    private val melee: NvPMeleeAccuracy,
    private val random: GameRandom,
    private val modifier: PlayerHitModifier,
    private val instantHits: InstantPlayerHitProcessor,
) {
    fun attack(npc: Npc, player: Player, type: HitType, maxHit: Int, delay: Int): Boolean {
        val chance = when (type) {
            HitType.Magic -> magic.getHitChance(npc, player)
            HitType.Ranged -> ranged.getHitChance(npc, player)
            HitType.Melee -> melee.getHitChance(npc, player, MeleeAttackType.Stab)
            else -> error("Unsupported Zulrah accuracy style: $type")
        }
        val accurate = AccuracyFormulae.isSuccessfulHit(chance, random)
        player.finishNpcHit(npc, delay, type, if (accurate) random.of(0..maxHit) else 0, modifier)
        return accurate
    }

    fun capIncoming(damage: Int): Int = if (damage > 50) random.of(45..50) else damage

    fun cloudDamage(player: Player) {
        player.takeInstantHit(HitType.Typeless, random.of(1..4), instantHits, hitmark_groups.venom)
    }

    fun tailHit(npc: Npc, player: Player, damage: IntRange): Hit {
        val group = hitmark_groups.regular_damage
        val zero = hitmark_groups.zero_damage
        val mark = group.lit.asRSCM(RSCMType.HITMARK)
        // Use the public hit builder and the injected native processor; no core overload is needed.
        val builder = HitBuilder(
            type = HitType.Typeless,
            damage = minOf(player.hitpoints, random.of(damage)),
            sourceUid = npc.uid.packed,
            sourceSlot = npc.slotId,
            isFromNpc = true,
            isFromPlayer = false,
            clientDelay = 0,
            righthandType = null,
            secondaryType = null,
            targetHitmark = mark,
            sourceHitmark = mark,
            publicHitmark = group.tint?.asRSCM(RSCMType.HITMARK) ?: mark,
            zeroDamageHitmarkLit = zero.lit.asRSCM(RSCMType.HITMARK),
            zeroDamageHitmarkTint = zero.tint?.asRSCM(RSCMType.HITMARK),
            maxDamageHitmarkLit = group.max?.asRSCM(RSCMType.HITMARK),
            targetMaxDamageThreshold = Int.MAX_VALUE,
            sourceMaxDamageThreshold = Int.MAX_VALUE,
        )
        with(modifier) { builder.modify(player) }
        val hit = builder.build()
        with(instantHits) { player.process(hit) }
        if (hit.damage > 0) {
            player.combatPlayDefendAnim()
            player.queueCombatRetaliate(npc)
        }
        return hit
    }
}
