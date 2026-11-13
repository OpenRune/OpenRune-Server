package org.rsmod.content.other.pets

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.NpcKillExtraDrops
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.PetDropsRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * Rolls boss pets listed in `dbtable.pet_drops` into the kill's loot. Pets that already sit in an
 * npc's drop table come out of that roll instead.
 */
@Singleton
class BossPetRoll @Inject constructor(private val random: GameRandom) : NpcKillExtraDrops {
    private val byNpc: Map<Int, Drop> =
        PetDropsRow.all().associate { it.npc.id to Drop(it.pet.internalName, it.rate) }

    override fun drops(player: Player, npc: Npc): List<DropRollItem> {
        val drop = byNpc[npc.id] ?: return emptyList()
        if (drop.rate <= 0 || random.of(drop.rate) != 0) {
            return emptyList()
        }
        return listOf(DropRollItem(drop.pet, 1))
    }

    private class Drop(val pet: String, val rate: Int)
}
