package org.rsmod.api.spells.runes.subs

import dev.openrune.types.ItemServerType
import org.rsmod.api.spells.runes.subs.configs.runesub_enums

public class RuneSubstituteRepository {
    private lateinit var subs: Map<Int, List<ItemServerType>>

    public operator fun get(baseRune: ItemServerType): List<ItemServerType>? = subs[baseRune.id]

    internal fun init(subs: Map<Int, List<ItemServerType>>) {
        this.subs = subs
    }

    internal fun init() {
        val subs = loadRuneSubstitutes()
        init(subs)
    }

    private fun loadRuneSubstitutes(): Map<Int, List<ItemServerType>> {
        val mapped = hashMapOf<Int, List<ItemServerType>>()

        val runeList = runesub_enums.runes.filterValuesNotNull()
        for ((rune, subEnum) in runeList) {
            val subList = subEnum.filterValuesNotNull()
            mapped[rune.id] = subList.values.toList()
        }

        return mapped
    }
}
