package org.rsmod.api.spells.runes.compact

import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.RuneEnums.rune_compact_ids

/**
 * Rune objs have a "compact id" used in places where storage needs to be efficient, such as rune
 * pouches. This class is responsible for providing access to these "compact ids."
 */
public class CompactRuneRepository {
    private lateinit var compactIds: Map<Int, Int>
    private lateinit var reverseLookup: Map<Int, ItemServerType>

    public operator fun get(rune: ItemServerType): Int? = compactIds[rune.id]

    internal fun init(compactIds: Map<ItemServerType, Int>) {
        this.compactIds = compactIds.entries.associate { it.key.id to it.value }
        this.reverseLookup = compactIds.entries.associate { it.value to it.key }
    }

    internal fun init() {
        val compactIds = loadCompactIds()
        init(compactIds)
    }

    private fun loadCompactIds(): Map<ItemServerType, Int> {
        val enum = rune_compact_ids.filterValuesNotNull()
        return HashMap(enum.backing)
    }
}
