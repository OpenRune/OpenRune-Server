package org.rsmod.content.slayer.core

import org.rsmod.api.table.slayer.SlayerMasterTaskRow
import org.rsmod.api.table.slayer.SlayerTaskSublistRow

sealed class AssignmentRoll {
    data class Boss(val masterTask: SlayerMasterTaskRow, val boss: SlayerTaskSublistRow) :
        AssignmentRoll()

    data class Regular(val masterTask: SlayerMasterTaskRow, val amount: Int) : AssignmentRoll()
}
