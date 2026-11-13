package org.rsmod.api.droptable

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * Drops a kill hands out on top of the npc's drop table, in the same loot: boss pets from
 * `dbtable.pet_drops`, Gauntlet frames and components. Called once per kill the table is not
 * suppressed for, including npcs with no drop table.
 */
public fun interface NpcKillExtraDrops {
    public fun drops(player: Player, npc: Npc): List<DropRollItem>
}
