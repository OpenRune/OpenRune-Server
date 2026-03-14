package org.rsmod.content.generic.locs.doors

import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.synths
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences

internal typealias door_locs = DoorLocs

internal object DoorConstants {
    /** The (cycle) duration that a door remains changed before reverting to its original state. */
    const val DURATION = 500
}

internal object DoorLocs : LocReferences() {
    val door_opened = loc("poordooropen")
    val door_closed = loc("poordoor")
    val poordoor_closed = loc("poordoor_m")
    val poordoor_opened = loc("poordooropen_m")
    val poshdoor_closed = loc("poshdoor")
    val poshdoor_opened = loc("poshdooropen")
    val door_left_closed = loc("castledoubledoorl")
    val door_left_opened = loc("opencastledoubledoorl")
    val door_right_closed = loc("castledoubledoorr")
    val door_right_opened = loc("opencastledoubledoorr")
}

internal object DoorLocEdits : LocEditor() {
    init {
        edit(door_locs.door_opened) {
            param[params.next_loc_stage] = door_locs.door_closed
            contentGroup = content.opened_single_door
        }

        edit(door_locs.door_closed) {
            param[params.next_loc_stage] = door_locs.door_opened
            contentGroup = content.closed_single_door
        }

        edit(door_locs.poordoor_opened) {
            param[params.next_loc_stage] = door_locs.poordoor_closed
            contentGroup = content.opened_single_door
        }

        edit(door_locs.poordoor_closed) {
            param[params.next_loc_stage] = door_locs.poordoor_opened
            contentGroup = content.closed_single_door
        }

        edit(door_locs.poshdoor_opened) {
            param[params.next_loc_stage] = door_locs.poshdoor_closed
            param[params.closesound] = synths.nicedoor_close
            contentGroup = content.opened_single_door
        }

        edit(door_locs.poshdoor_closed) {
            param[params.next_loc_stage] = door_locs.poshdoor_opened
            param[params.opensound] = synths.nicedoor_open
            contentGroup = content.closed_single_door
        }

        edit(door_locs.door_left_closed) {
            param[params.next_loc_stage] = door_locs.door_left_opened
            contentGroup = content.closed_left_door
        }

        edit(door_locs.door_left_opened) {
            param[params.next_loc_stage] = door_locs.door_left_closed
            contentGroup = content.opened_left_door
        }

        edit(door_locs.door_right_closed) {
            param[params.next_loc_stage] = door_locs.door_right_opened
            contentGroup = content.closed_right_door
        }

        edit(door_locs.door_right_opened) {
            param[params.next_loc_stage] = door_locs.door_right_closed
            contentGroup = content.opened_right_door
        }
    }
}
