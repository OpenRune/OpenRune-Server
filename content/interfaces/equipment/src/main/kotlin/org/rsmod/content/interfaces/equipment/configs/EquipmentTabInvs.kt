package org.rsmod.content.interfaces.equipment.configs

import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.game.type.inv.InvStackType

internal typealias equip_invs = EquipmentTabInvs

object EquipmentTabInvs : InvReferences() {
    // TODO: Fix incorrect inv types.
    val death_data = inv("diango_hols_sack")
    val death = inv("deathkeep_items")
    val kept = inv("skill_guide_hunting_tracking")
}

internal object EquipmentTabInvEdit : InvEditor() {
    init {
        edit(equip_invs.kept) { stack = InvStackType.Never }
    }
}
