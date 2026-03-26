package org.alter.combat.spell

import org.alter.api.CombatAttributes
import org.alter.api.ext.getVarbit
import org.alter.game.combat.CombatSystem
import org.alter.game.combat.DisengageReason
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent
import org.alter.plugins.content.magic.MagicSpells

class AutocastPlugin : PluginEvent() {

    companion object {
        private const val AUTOCAST_VARBIT = "varbits.autocast_spell"
    }

    override fun init() {
        onEvent<PreAttackEvent>(priority = -10) {
            val player = attacker as? Player ?: return@onEvent
            if (player.attr[CombatAttributes.CASTING_SPELL] != null) return@onEvent

            val autoCastId = player.getVarbit(AUTOCAST_VARBIT)
            if (autoCastId == 0) return@onEvent

            val spell = CombatSpell.findByAutoCastId(autoCastId) ?: return@onEvent

            val combatSpells = MagicSpells.getCombatSpells()
            val metadata = combatSpells.values.firstOrNull {
                it.paramItem == spell.id
            }

            if (metadata != null) {
                if (!MagicSpells.canCast(player, metadata.lvl, metadata.items, metadata.spellbook)) {
                    player.writeMessage("You do not have enough runes to cast this spell.")
                    cancelled = true
                    cancelReason = "Insufficient runes for autocast"
                    CombatSystem.instance.disengage(player, DisengageReason.MANUAL)
                    return@onEvent
                }
                MagicSpells.removeRunes(player, metadata.items)
            }

            player.attr[CombatAttributes.CASTING_SPELL] = spell
        }
    }
}
