package org.rsmod.api.spells.runes.combo

import dev.openrune.types.ItemServerType

public data class ComboRune(
    val rune: ItemServerType,
    val first: ItemServerType,
    val second: ItemServerType,
)
