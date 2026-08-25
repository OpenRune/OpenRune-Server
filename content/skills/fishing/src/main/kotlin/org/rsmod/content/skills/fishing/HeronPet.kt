package org.rsmod.content.skills.fishing

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase

object HeronPet {
    const val PET_OBJ: String = "obj.skillpetfish"
    const val GREAT_BLUE_OBJ: String = "obj.skillpetfish_tempoross"

    private val baseChance: Map<String, Int> by lazy {
        enum<ItemServerType, Int>("heron_pet_chance")
            .backing
            .mapNotNull { (fish, base) -> base?.let { fish.internalName to it } }
            .toMap()
    }

    fun ProtectedAccess.rollHeron(fish: String, rarityMultiplier: Int = 1) {
        val base = baseChance[fish] ?: return
        val scaled = (base - player.statBase("stat.fishing") * 25).coerceAtLeast(1)
        val chance = scaled * rarityMultiplier
        if (random.of(chance) != 0 || inv.isFull()) {
            return
        }
        invAdd(inv, PET_OBJ, 1)
        // TODO: spawn the heron as a follower once a pet system exists; until then it always lands
        //  in the inventory.
        spam("You have a funny feeling like you're being followed.")
    }
}
