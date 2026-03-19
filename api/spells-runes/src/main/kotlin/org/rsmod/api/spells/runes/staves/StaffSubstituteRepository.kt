package org.rsmod.api.spells.runes.staves

import dev.openrune.types.ItemServerType
import org.rsmod.api.spells.runes.staves.configs.staff_enums
import org.rsmod.game.inv.InvObj

public class StaffSubstituteRepository {
    private lateinit var subs: Map<Int, Set<Int>>

    public fun isValidSubstitute(baseStaff: ItemServerType, otherStaff: InvObj): Boolean {
        val substitutes = subs[baseStaff.id] ?: return false
        return otherStaff.id in substitutes
    }

    internal fun init() {
        val subs = loadStaffSubstitutes()
        this.subs = subs
    }

    private fun loadStaffSubstitutes(): Map<Int, Set<Int>> {
        val mapped = hashMapOf<Int, Set<Int>>()

        val staffList = staff_enums.staves.filterValuesNotNull()
        for ((staff, subEnum) in staffList) {
            val subList = subEnum.filterValuesNotNull()
            mapped[staff.id] = subList.map { it.value.id }.toHashSet()
        }

        return mapped
    }
}
