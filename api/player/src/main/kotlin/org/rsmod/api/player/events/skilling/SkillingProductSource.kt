package org.rsmod.api.player.events.skilling

import dev.openrune.types.ItemServerType
import org.rsmod.api.table.mining.MiningRocksRow
import org.rsmod.game.loc.BoundLocInfo

public sealed class SkillingProductSource {
    public data class Mining(
        public val rock: BoundLocInfo,
        public val rockData: MiningRocksRow,
    ) : SkillingProductSource()

    public data class Woodcutting(
        public val tree: BoundLocInfo,
        public val productType: ItemServerType,
    ) : SkillingProductSource()
}
