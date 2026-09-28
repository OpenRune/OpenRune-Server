package org.rsmod.content.other.pets

import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.followerObj: Int by intVarp("varp.follower_obj")

internal object PetLocation {
    const val NONE = 0
    const val BANK = 1
    const val INVENTORY = 2
    const val FOLLOWER = 3
}

/** True when any of [objs] is held in the backpack or the bank. */
fun Player.storesAnyObj(objs: Iterable<String>): Boolean {
    if (objs.any { it in inv }) {
        return true
    }
    val bank = invMap["inv.bank"] ?: return false
    return objs.any { it in bank }
}

internal fun Player.petLocation(pet: Pet): Int {
    val following = followerObj
    if (pet.forms.any { it.objId == following }) {
        return PetLocation.FOLLOWER
    }
    if (pet.forms.any { it.obj in inv }) {
        return PetLocation.INVENTORY
    }
    val bank = invMap["inv.bank"]
    if (bank != null && pet.forms.any { it.obj in bank }) {
        return PetLocation.BANK
    }
    return PetLocation.NONE
}

internal fun Player.ownsPet(pet: Pet): Boolean = petLocation(pet) != PetLocation.NONE
