package org.alter.game.pluginnew.event.impl

import dev.openrune.ServerCacheManager.getNpc
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.requireRSCM
import org.alter.rscm.RSCMType

class NpcAttackEvent(val npc: Npc, player: Player) : PlayerEvent(player)

class NpcClickEvent(
    val npc: Npc,
    val op: MenuOption,
    player: Player
) : EntityInteractionEvent<Npc>(npc, op, player) {

    val id : Int = npc.id

    override fun resolveOptionName(): String {
        val def = getNpc(id) ?: error("Npc not found for id=$id")
        return def.actions.getOrNull(op.id - 1) ?: error("No action found at index ${op.id} for npc id=$id")
    }
}

private fun npcOptionPredicate(
    idMatches: Boolean,
    optionName: String,
    options: Array<out String>
): Boolean {
    return idMatches && options.any { it.equals(optionName, ignoreCase = true) }
}

fun PluginEvent.onNpcOption(
    obj: String,
    vararg options: String,
    action: suspend NpcClickEvent.() -> Unit
): EventListener<NpcClickEvent> {
    requireRSCM(RSCMType.NPCTYPES, obj)

    return on<NpcClickEvent> {
        where {
            val matchesId = RSCM.getReverseMapping(RSCMType.NPCTYPES, id) == obj
            npcOptionPredicate(matchesId, optionName, options)
        }
        then(action)
    }
}

fun PluginEvent.onNpcOption(
    obj: Int,
    vararg options: String,
    action: suspend NpcClickEvent.() -> Unit
): EventListener<NpcClickEvent> {
    return on<NpcClickEvent> {
        where { npcOptionPredicate(id == obj, optionName, options) }
        then(action)
    }
}