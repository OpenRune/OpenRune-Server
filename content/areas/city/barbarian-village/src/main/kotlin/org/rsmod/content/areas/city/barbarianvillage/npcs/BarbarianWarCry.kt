package org.rsmod.content.areas.city.barbarianvillage.npcs

import org.rsmod.api.combat.commons.npc.NpcAttackHook
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class BarbarianWarCry : NpcAttackHook {
    override fun onAttack(npc: Npc, target: Player) {
        if (BARBARIANS.any { npc.type.isType(it) }) {
            npc.say(WAR_CRY)
        }
    }

    private companion object {
        const val WAR_CRY = "YYEEEEEAAAARRRRGGHHHH!!!"
    }
}
