package org.rsmod.content.bosses.royaltitans

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.spells.attack.NpcSpellHitHook
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * The Royal Titans elementals use their own spell rules:
 * - hit chance is magic attack bonus + 30, with a 30% floor and x1.45 from the void mage set;
 * - the opposing-element spell (water on fire, fire on ice) always deals its maximum damage;
 * - that spell cleaves every same-element elemental in the 3x3 around the primary target.
 *
 * The cleave itself lives in [RoyalTitansElementals]; this hook supplies the accuracy, max-hit and
 * hit-queued rules.
 */
class RoyalTitansElementalSpells
@Inject
constructor(
    private val elementals: RoyalTitansElementals,
    private val bonuses: WornBonuses,
    private val deps: BossDeps,
) : NpcSpellHitHook {
    override fun rollHit(player: Player, npc: Npc, spell: ItemServerType): Boolean? {
        if (!elementals.isElemental(npc)) return null
        val chance = hitChancePercent(player)
        return deps.random.of(100) < chance
    }

    override fun alwaysMaxHit(player: Player, npc: Npc, spell: ItemServerType): Boolean {
        val element = elementals.elementOf(npc) ?: return false
        return element.isCleavedBy(spell)
    }

    override fun onHitQueued(
        player: Player,
        npc: Npc,
        spell: ItemServerType,
        damage: Int,
        hitDelay: Int,
    ) {
        elementals.cleave(player, npc, spell, damage, hitDelay)
    }

    private fun hitChancePercent(player: Player): Int {
        val base = (bonuses.offensiveMagicBonus(player) + 30).coerceAtLeast(30)
        val chance = if (player.hasVoidMageSet()) (base * 1.45).toInt() else base
        return chance.coerceAtMost(100)
    }

    private fun Player.hasVoidMageSet(): Boolean =
        EquipmentChecks.isVoidMageHelm(hat) &&
            EquipmentChecks.isVoidTop(torso) &&
            EquipmentChecks.isVoidRobe(legs) &&
            EquipmentChecks.isVoidGloves(hands)
}
