package org.rsmod.api.spells.runes.fake

import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.FakeEnums.fake_runes

/**
 * Some features in the game use "fake" runes to ensure that, if players manage to smuggle them
 * outside their intended areas, they won't have unintended value or impact. For example, Barbarian
 * Assault provides runes during the game, but uses secondary "fake" variants instead of the
 * originals as a safeguard.
 */
public class FakeRuneRepository {
    private lateinit var fakes: Map<Int, ItemServerType>

    public operator fun get(rune: ItemServerType): ItemServerType? = fakes[rune.id]

    internal fun init() {
        val fakes = loadFakeRunes()
        this.fakes = fakes
    }

    private fun loadFakeRunes(): Map<Int, ItemServerType> {
        val enum = fake_runes.filterValuesNotNull()
        return enum.backing.entries.associate { it.key.id to it.value }
    }
}
