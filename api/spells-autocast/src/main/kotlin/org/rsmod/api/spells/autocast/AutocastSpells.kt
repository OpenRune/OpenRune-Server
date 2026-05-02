package org.rsmod.api.spells.autocast

import dev.openrune.types.ItemServerType
import jakarta.inject.Singleton
import org.rsmod.api.enums.AutocastEnums.autocast_restricted_spells
import org.rsmod.api.enums.AutocastEnums.autocast_spells

@Singleton
public class AutocastSpells {
    private lateinit var spells: Map<Int, ItemServerType>
    private lateinit var restricted: Set<Int>

    public operator fun get(autocastId: Int): ItemServerType? = spells[autocastId]

    public fun isRestrictedSpell(spell: ItemServerType): Boolean = spell.id in restricted

    internal fun startup() {
        val spells = loadAutocastSpells()
        this.spells = spells

        val restricted = loadRestrictedSpells()
        this.restricted = restricted
    }

    private fun loadAutocastSpells(): Map<Int, ItemServerType> {
        val enum = autocast_spells.filterValuesNotNull()
        return HashMap(enum.backing)
    }

    private fun loadRestrictedSpells(): Set<Int> {
        val enum = autocast_restricted_spells.filterValuesNotNull()
        return enum.keys.map(ItemServerType::id).toHashSet()
    }
}
