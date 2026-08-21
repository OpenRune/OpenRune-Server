package org.rsmod.content.skills.sailing

import org.rsmod.game.loc.LocShape

data class DeckLoc(
    val loc: String,
    val dx: Int,
    val dz: Int,
    val level: Int,
    val shape: LocShape,
)
