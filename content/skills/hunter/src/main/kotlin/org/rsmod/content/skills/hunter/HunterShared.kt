package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.random.GameRandom
import org.rsmod.game.inv.Inventory

// Rules shared by every hunter technique. Design notes: docs/hunter.md.

/** The `maxLevel` SkillingSuccessRate interpolates against; published charts run to level 99. */
internal const val MAX_HUNTER_LEVEL: Int = 99

/** A fixed quantity must consume no random draw - tests script the RNG as a draw sequence. */
internal fun rollQuantity(random: GameRandom, quantity: IntRange): Int =
    if (quantity.first == quantity.last) quantity.first else random.of(quantity)

/** A stackable already held needs no free slot, whatever the count. */
internal fun hunterInvSlotsNeeded(inv: Inventory, internal: String, count: Int): Int {
    val stackable = ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ))?.isStackable == true
    return when {
        !stackable -> count
        inv.contains(internal) -> 0
        else -> 1
    }
}
