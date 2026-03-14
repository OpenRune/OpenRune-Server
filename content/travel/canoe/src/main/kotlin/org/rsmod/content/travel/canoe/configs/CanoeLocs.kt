package org.rsmod.content.travel.canoe.configs

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences

typealias canoe_locs = CanoeLocs

object CanoeLocs : LocReferences() {
    val station_lumbridge = loc("canoeing_canoestation_lumbridge")
    val station_champs_guild = loc("canoeing_canoestation_championsguild")
    val station_barb_village = loc("canoeing_canoestation_barbarianvillage")
    val station_edgeville = loc("canoeing_canoestation_edgeville")
    val station_ferox_enclave = loc("canoeing_canoestation_sanctuary")
    val ready_to_shape = loc("canoestation_fallen_tree")
    val ready_log = loc("canoestation_log")
    val ready_dugout = loc("canoestation_dugout")
    val ready_stable_dugout = loc("canoestation_stabledugout")
    val ready_waka = loc("canoestation_waka")
    val floating_log = loc("canoeing_log_canoeing_station_in_water")
    val floating_dugout = loc("canoeing_dugout_canoeing_station_in_water")
    val floating_stable_dugout = loc("canoeing_catamaran_canoeing_station_in_water")
    val floating_waka = loc("canoeing_waka_canoeing_station_in_water")
    val sinking_log = loc("canoeing_log_sinking")
    val sinking_dugout = loc("canoeing_dugout_sinking")
    val sinking_stable_dugout = loc("canoeing_catamaran_sinking")
    val sinking_waka = loc("canoeing_waka_sinking")
}

object CanoeLocEditor : LocEditor() {
    init {
        edit(canoe_locs.ready_log) {
            param[params.skill_xp] = 30
            param[params.levelrequire] = 12
            param[params.next_loc_stage] = canoe_locs.sinking_log
        }

        edit(canoe_locs.ready_dugout) {
            param[params.skill_xp] = 60
            param[params.levelrequire] = 27
            param[params.next_loc_stage] = canoe_locs.sinking_dugout
        }

        edit(canoe_locs.ready_stable_dugout) {
            param[params.skill_xp] = 90
            param[params.levelrequire] = 42
            param[params.next_loc_stage] = canoe_locs.sinking_stable_dugout
        }

        edit(canoe_locs.ready_waka) {
            param[params.skill_xp] = 150
            param[params.levelrequire] = 57
            param[params.next_loc_stage] = canoe_locs.sinking_waka
        }
    }
}
