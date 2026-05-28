package org.rsmod.content.slayer.core

import org.rsmod.api.table.slayer.SlayerMasterTaskRow

sealed class AssignmentRoll {
    data class Boss(val masterTask: SlayerMasterTaskRow) : AssignmentRoll()
    data class Regular(val masterTask: SlayerMasterTaskRow, val amount: Int) : AssignmentRoll()
}
