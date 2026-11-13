package org.rsmod.content.other.treasuretrails

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

object ClueUri {
    internal const val SUMMONED_NPC = "varp.cluehelper_npc_id"
    private const val NO_NPC = -1

    fun isSummoned(player: Player, npc: Npc): Boolean = player.vars[SUMMONED_NPC] == npc.uid.packed

    fun summoned(player: Player): Int? =
        player.vars[SUMMONED_NPC].takeIf { it != 0 && it != NO_NPC }

    fun remember(player: Player, npc: Npc) {
        VarPlayerIntMapSetter.set(player, SUMMONED_NPC, npc.uid.packed)
    }

    fun clear(player: Player) {
        VarPlayerIntMapSetter.set(player, SUMMONED_NPC, NO_NPC)
    }
}
