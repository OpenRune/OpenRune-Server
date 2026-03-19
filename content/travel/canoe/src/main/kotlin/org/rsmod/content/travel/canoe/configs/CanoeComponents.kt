package org.rsmod.content.travel.canoe.configs

import dev.openrune.component

typealias canoe_components = CanoeComponents

object CanoeComponents {
    val shape_log = component("canoeing:log")
    val shape_dugout = component("canoeing:dugout")
    val shape_stable_dugout = component("canoeing:stable_dugout")
    val shape_waka = component("canoeing:waka")
    val shape_close = component("canoeing:close")

    val destination_edgeville = component("canoe_map:edgeville")
    val destination_lumbridge = component("canoe_map:lumbridge")
    val destination_champs_guild = component("canoe_map:champions")
    val destination_barb_village = component("canoe_map:barbarian")
    val destination_wild_pond = component("canoe_map:wilderness")
    val destination_ferox_enclave = component("canoe_map:sanctuary")
}
