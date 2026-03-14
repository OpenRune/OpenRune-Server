package org.rsmod.content.interfaces.equipment.configs

import org.rsmod.api.type.refs.obj.ObjReferences

internal typealias equip_objs = EquipmentTabObjs

object EquipmentTabObjs : ObjReferences() {
    val keep_downgraded_without_orn_kit = obj("hundred_pirate_mudskipper_hide")
    val keep_downgraded = obj("burntfish2")
    val deleted = obj("burntfish1")
    val keep = obj("burntfish1")
    val gravestone_downgraded = obj("burntfish5")
    val gravestone = obj("burntfish4")
    val turn_to_coins = obj("hundred_pirate_burned_fishcake")
    val lost_to_killer = obj("jug_bad_wine")
}
