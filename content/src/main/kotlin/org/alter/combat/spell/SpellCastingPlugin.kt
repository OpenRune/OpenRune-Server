package org.alter.combat.spell

import org.alter.api.CombatAttributes
import org.alter.game.combat.CombatSystem
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.SpellOnNpcEvent
import org.alter.game.pluginnew.event.impl.SpellOnPlayerEvent
import org.alter.plugins.content.magic.MagicSpells

class SpellCastingPlugin : PluginEvent() {

    override fun init() {
        onEvent<SpellOnNpcEvent> {
            castSpellOnTarget(player, npc, interfaceId, componentId)
        }

        onEvent<SpellOnPlayerEvent> {
            castSpellOnTarget(player, target, interfaceId, componentId)
        }
    }

    private fun castSpellOnTarget(
        player: Player,
        target: Pawn,
        interfaceId: Int,
        componentId: Int,
    ) {
        val combatSpells = MagicSpells.getCombatSpells()
        val metadata = combatSpells.values.firstOrNull {
            it.interfaceId == interfaceId && it.component == componentId
        } ?: return

        val spell = CombatSpell.findByItemId(metadata.paramItem) ?: return

        if (!MagicSpells.canCast(player, metadata.lvl, metadata.items, metadata.spellbook)) return

        MagicSpells.removeRunes(player, metadata.items)

        player.attr[CombatAttributes.CASTING_SPELL] = spell

        val strategy = CombatSystem.instance.resolveStrategy(player)
        val style = CombatSystem.instance.resolveCombatStyle(player)
        CombatSystem.instance.engage(player, target, strategy, style)
    }
}
