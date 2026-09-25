package org.rsmod.content.other.pets.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * Skilling pets keyed by the skill that rolls them. `chances` is an optional enum of resource obj
 * to base chance, used by skills whose rate varies per resource; `base` is the fallback.
 */
object PetSkillingTable {
    const val SKILL = 0
    const val PET = 1
    const val BASE = 2
    const val CHANCES = 3

    fun skilling() = dbTable("dbtable.pet_skilling", serverOnly = true) {
        column("skill", SKILL, VarType.STAT)
        column("pet", PET, VarType.OBJ)
        column("base", BASE, VarType.INT)
        column("chances", CHANCES, VarType.ENUM)

        fun skill(row: String, stat: String, pet: String, base: Int = 0, chances: String? = null) {
            row(row) {
                columnRSCM(SKILL, stat)
                columnRSCM(PET, pet)
                column(BASE, base)
                if (chances != null) {
                    columnRSCM(CHANCES, chances)
                }
            }
        }

        skill("dbrow.pet_skilling_fishing", "stat.fishing", "obj.skillpetfish", chances = "enum.heron_pet_chance")
        skill("dbrow.pet_skilling_mining", "stat.mining", "obj.skillpetmining", chances = "enum.rock_golem_chance")
        skill("dbrow.pet_skilling_woodcutting", "stat.woodcutting", "obj.skillpetwc", chances = "enum.beaver_chance")
    }
}
