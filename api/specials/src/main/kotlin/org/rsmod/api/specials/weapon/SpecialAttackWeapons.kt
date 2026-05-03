package org.rsmod.api.specials.weapon

import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.SaEnums.sa_descriptions
import org.rsmod.api.enums.SaEnums.sa_energy_requirements
import org.rsmod.api.specials.energy.SpecialAttackEnergy

public class SpecialAttackWeapons() {
    private lateinit var energyRequirements: Map<Int, Int>
    private lateinit var descriptions: Map<Int, String>

    /**
     * Returns the special attack energy requirement for [objType] from the `energy_requirements`
     * enum.
     *
     * @return the special attack energy requirement for [objType] in the range of `1` to
     *   [MAX_ENERGY] (`1000`), or `null` if [objType] does not have an associated special attack.
     * @see [loadEnergyRequirements]
     */
    public fun getSpecialEnergy(objType: Int): Int? = energyRequirements[objType]

    public fun getSpecialDescription(objType: Int): String? = descriptions[objType]

    internal fun startup() {
        val energyRequirements = loadEnergyRequirements()
        this.energyRequirements = energyRequirements

        val descriptions = loadDescriptions()
        this.descriptions = descriptions
    }

    private fun loadEnergyRequirements(): Map<Int, Int> {
        val requirements = mutableMapOf<Int, Int>()

        val enum = sa_energy_requirements.filterValuesNotNull()
        for ((obj, energy) in enum) {
            check(energy in 0..MAX_ENERGY) {
                "Expected `energy` values to be within range of [0..$MAX_ENERGY]: actual=$energy"
            }
            requirements[obj.id] = energy
        }

        return requirements
    }

    private fun loadDescriptions(): Map<Int, String> {
        val descriptions = mutableMapOf<Int, String>()

        val enum = sa_descriptions.filterValuesNotNull()
        for ((obj, description) in enum) {
            descriptions[obj.id] = description
        }

        return descriptions
    }

    private companion object {
        private const val MAX_ENERGY = SpecialAttackEnergy.MAX_ENERGY
    }
}
