package org.rsmod.content.skills.agility

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase

/**
 * The agility pet rolls once per completed lap at 1 in `base - level * 25`, where the base is the
 * course's own and the level is unboosted.
 */
object SquirrelPet {
    const val PET_OBJ: String = "obj.skillpetagility"

    fun ProtectedAccess.rollSquirrel(course: Course) {
        if (course.petBase <= 0) {
            return
        }
        val chance = (course.petBase - player.statBase(STAT_AGILITY) * LEVEL_WEIGHT).coerceAtLeast(1)
        if (random.of(chance) != 0 || inv.isFull()) {
            return
        }
        invAdd(inv, PET_OBJ, 1)
        spam("You have a funny feeling like you're being followed.")
    }

    private const val LEVEL_WEIGHT = 25
}
