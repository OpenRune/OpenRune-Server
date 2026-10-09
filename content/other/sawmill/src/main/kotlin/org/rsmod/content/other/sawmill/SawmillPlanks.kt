package org.rsmod.content.other.sawmill

import org.rsmod.api.table.SawmillPlanksRow

internal object SawmillPlanks {
    val all: List<SawmillPlanksRow>
        get() = SawmillPlanksRow.all()

    fun forPlank(plank: String): SawmillPlanksRow = all.first { it.plank.internalName == plank }
}
