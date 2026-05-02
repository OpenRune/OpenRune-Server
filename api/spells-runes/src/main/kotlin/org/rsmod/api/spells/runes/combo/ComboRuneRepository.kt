package org.rsmod.api.spells.runes.combo

import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.ComboRuneEnums.combo_runes

public class ComboRuneRepository {
    private lateinit var combos: Map<Int, ComboRune>
    public lateinit var comboRunes: List<ComboRune>
        private set

    public operator fun get(comboRune: ItemServerType): ComboRune? = combos[comboRune.id]

    internal fun init(combos: Map<ItemServerType, ComboRune>) {
        this.combos = combos.entries.associate { it.key.id to it.value }
        this.comboRunes = combos.values.toList()
    }

    internal fun init() {
        val combos = loadComboRunes()
        init(combos)
    }

    private fun loadComboRunes(): Map<ItemServerType, ComboRune> {
        val mapped = hashMapOf<ItemServerType, ComboRune>()
        val comboRuneList = combo_runes.filterValuesNotNull()
        for ((rune, comboRunesEnum) in comboRuneList) {
            val runeList = comboRunesEnum.filterValuesNotNull().values.toList()
            check(runeList.size == 2) { "Expected 2 rune values: $runeList (enum=$comboRunesEnum)" }
            mapped[rune] = ComboRune(rune, runeList[0], runeList[1])
        }
        return mapped
    }
}
