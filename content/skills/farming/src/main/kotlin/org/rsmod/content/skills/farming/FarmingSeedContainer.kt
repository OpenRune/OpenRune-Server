package org.rsmod.content.skills.farming

import org.rsmod.game.entity.Player

/** A carried seed container extends planting without coupling Farming to its storage module. */
interface FarmingSeedContainer {
    fun recognises(item: Int): Boolean
    fun seeds(player: Player): List<Int>
    fun count(player: Player, seed: Int): Int
    fun consume(player: Player, seed: Int, amount: Int): Boolean
}
