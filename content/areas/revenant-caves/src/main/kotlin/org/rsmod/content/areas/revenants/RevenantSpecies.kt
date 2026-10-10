package org.rsmod.content.areas.revenants

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.entity.Npc

internal enum class RevenantSpecies(val symbol: String, val maxHit: Int) {
    Imp("imp", 2), Goblin("goblin", 3), Pyrefiend("pyrefiend", 5),
    Hobgoblin("hobgoblin", 8), Cyclops("cyclops", 16), Hellhound("hellhound", 20),
    Demon("demon", 20), Ork("ork", 20), DarkBeast("darkbeast", 29),
    Knight("knight", 27), Dragon("dragon", 30);

    val npcType get() = "npc.wild_cave_$symbol"
    val id get() = npcType.asRSCM()
    companion object {
        fun of(npc: Npc): RevenantSpecies? = entries.firstOrNull { it.id == npc.type.id }
        const val MALEDICTUS = "npc.wild_cave_superior"
    }
}
