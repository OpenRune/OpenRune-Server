package org.rsmod.content.travel.canoe.configs

import dev.openrune.varBit

typealias canoe_varbits = CanoeVarBits

object CanoeVarBits {
    val current_station = varBit("canoe_startfrom")
    val lumbridge_state = varBit("canoestation_state_lumbridge")
    val champs_guild_state = varBit("canoestation_state_championsguild")
    val barb_village_state = varBit("canoestation_state_barbarianvillage")
    val edgeville_state = varBit("canoestation_state_edgeville")
    val ferox_enclave_state = varBit("canoestation_state_sanctuary")

    val canoe_type = varBit("canoe_type")
    val canoe_avoid_if = varBit("canoe_avoid_if")

    val disable_wild_pond_warning = varBit("wildy_canoe_warning")
}
