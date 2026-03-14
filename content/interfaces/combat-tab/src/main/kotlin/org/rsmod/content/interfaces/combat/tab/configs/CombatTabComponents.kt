package org.rsmod.content.interfaces.combat.tab.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias combat_components = CombatTabComponents

object CombatTabComponents : ComponentReferences() {
    val stance1 = component("combat_interface:0")
    val stance2 = component("combat_interface:1")
    val stance3 = component("combat_interface:2")
    val stance4 = component("combat_interface:3")
    val auto_retaliate = component("combat_interface:retaliate")
    val special_attack = component("combat_interface:special_attack")

    val special_attack_orb = component("orbs:specbutton")
}
