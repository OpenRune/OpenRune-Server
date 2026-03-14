package org.rsmod.content.travel.canoe.configs

import org.rsmod.api.type.refs.synth.SynthReferences

typealias canoe_synths = CanoeSynths

object CanoeSynths : SynthReferences() {
    val canoe_pushed = synth("canoe_pushed")
    val canoe_paddle = synth("canoe_paddle")
    val canoe_sink = synth("canoe_sink")
}
