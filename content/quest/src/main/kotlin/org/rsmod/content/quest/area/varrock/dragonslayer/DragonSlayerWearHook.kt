package org.rsmod.content.quest.area.varrock.dragonslayer

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.enums.NamedEnums.dragon_slayer_wear_locked
import org.rsmod.api.player.hook.HeldEquipValidateHook
import org.rsmod.game.entity.Player

/** Rune platebodies, green d'hide bodies and dragon platebodies need Dragon Slayer I done. */
class DragonSlayerWearHook @Inject constructor(private val quest: DragonSlayerQuest) :
    HeldEquipValidateHook {
    override fun validate(player: Player, type: ItemServerType): String? {
        if (quest.isComplete(player)) {
            return null
        }
        val locked = dragon_slayer_wear_locked.backing.values.any { it?.id == type.id }
        return if (locked) REFUSAL else null
    }

    private companion object {
        const val REFUSAL = "You need to complete Dragon Slayer I to wear this."
    }
}
