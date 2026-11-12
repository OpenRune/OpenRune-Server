package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.content.other.pets.PetRewards

/** The giant squirrel roll a rewarded lap makes at its course's `pet_chance`. */
class AgilityPets @Inject constructor(private val pets: PetRewards) {
    internal fun roll(access: ProtectedAccess, course: AgilityCoursesRow) {
        val chance = course.petChance ?: return
        pets.rollSkillingPet(access.player, GIANT_SQUIRREL, AGILITY, chance)
    }

    private companion object {
        const val GIANT_SQUIRREL = "obj.skillpetagility"
        const val AGILITY = "stat.agility"
    }
}
