package org.rsmod.content.generic.locs.gate

import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.synths
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences

internal typealias gate_locs = GateLocs

internal object GateConstants {
    /** The (cycle) duration that gates remains changed before reverting to their original state. */
    const val DURATION = 500
}

internal object GateLocs : LocReferences() {
    val picketgate_left_closed = loc("fencegate_l")
    val picketgate_right_closed = loc("fencegate_r")
    val picketgate_left_opened = loc("openfencegate_l")
    val picketgate_right_opened = loc("openfencegate_r")
    val nicepicketgate_left_closed = loc("rustic_fencegate_l")
    val nicepicketgate_right_closed = loc("rustic_fencegate_r")
    val nicepicketgate_left_opened = loc("rustic_openfencegate_l")
    val nicepicketgate_right_opened = loc("rustic_openfencegate_r")
    val farmerfred_gate_left_closed = loc("qip_sheep_shearer_fencegate_l")
    val farmerfred_gate_right_closed = loc("qip_sheep_shearer_fencegate_r")
    val farmerfred_gate_left_opened = loc("qip_sheep_shearer_openfencegate_l")
    val farmerfred_gate_right_opened = loc("qip_sheep_shearer_openfencegate_r")
}

internal object GateLocEditor : LocEditor() {
    init {
        edit(gate_locs.picketgate_left_closed) {
            param[params.opensound] = synths.picketgate_open
            param[params.next_loc_stage] = gate_locs.picketgate_left_opened
            contentGroup = content.closed_left_picketgate
        }

        edit(gate_locs.picketgate_right_closed) {
            param[params.opensound] = synths.picketgate_open
            param[params.next_loc_stage] = gate_locs.picketgate_right_opened
            contentGroup = content.closed_right_picketgate
        }

        edit(gate_locs.picketgate_left_opened) {
            param[params.closesound] = synths.picketgate_close
            param[params.next_loc_stage] = gate_locs.picketgate_left_closed
            contentGroup = content.opened_left_picketgate
        }

        edit(gate_locs.picketgate_right_opened) {
            param[params.closesound] = synths.picketgate_close
            param[params.next_loc_stage] = gate_locs.picketgate_right_closed
            contentGroup = content.opened_right_picketgate
        }

        edit(gate_locs.nicepicketgate_left_closed) {
            param[params.opensound] = synths.picketgate_open
            param[params.next_loc_stage] = gate_locs.nicepicketgate_left_opened
            contentGroup = content.closed_left_picketgate
        }

        edit(gate_locs.nicepicketgate_right_closed) {
            param[params.opensound] = synths.picketgate_open
            param[params.next_loc_stage] = gate_locs.nicepicketgate_right_opened
            contentGroup = content.closed_right_picketgate
        }

        edit(gate_locs.nicepicketgate_left_opened) {
            param[params.closesound] = synths.door_close
            param[params.next_loc_stage] = gate_locs.nicepicketgate_left_closed
            contentGroup = content.opened_left_picketgate
        }

        edit(gate_locs.nicepicketgate_right_opened) {
            param[params.closesound] = synths.door_close
            param[params.next_loc_stage] = gate_locs.nicepicketgate_right_closed
            contentGroup = content.opened_right_picketgate
        }

        edit(gate_locs.farmerfred_gate_left_closed) {
            param[params.opensound] = synths.picketgate_open
            param[params.next_loc_stage] = gate_locs.farmerfred_gate_left_opened
            contentGroup = content.closed_left_picketgate
        }

        edit(gate_locs.farmerfred_gate_right_closed) {
            param[params.opensound] = synths.picketgate_open
            param[params.next_loc_stage] = gate_locs.farmerfred_gate_right_opened
            contentGroup = content.closed_right_picketgate
        }

        edit(gate_locs.farmerfred_gate_left_opened) {
            param[params.closesound] = synths.door_close
            param[params.next_loc_stage] = gate_locs.farmerfred_gate_left_closed
            contentGroup = content.opened_left_picketgate
        }

        edit(gate_locs.farmerfred_gate_right_opened) {
            param[params.closesound] = synths.door_close
            param[params.next_loc_stage] = gate_locs.farmerfred_gate_right_closed
            contentGroup = content.opened_right_picketgate
        }
    }
}
