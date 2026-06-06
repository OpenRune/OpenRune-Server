package org.rsmod.api.droptable.toml

import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.PendingDropItemConfig
import org.rsmod.game.entity.Player

public interface DropTableTomlResolver {
    public fun sharedTable(name: String): RSWeightedTable<Player, DropRollItem>

    public fun applyHooks(config: PendingDropItemConfig, hooks: TomlDropHooks)

    public fun applyBrimstoneKeyRoll(
        builder: dtx.rs.RSPrerollTableBuilder<Player, DropRollItem>,
        konarTaskBonus: Boolean,
    )
}
