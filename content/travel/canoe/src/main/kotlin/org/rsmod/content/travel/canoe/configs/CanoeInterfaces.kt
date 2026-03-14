package org.rsmod.content.travel.canoe.configs

import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias canoe_interfaces = CanoeInterfaces

object CanoeInterfaces : InterfaceReferences() {
    val shaping = inter("canoeing")
    val destination = inter("canoe_map")
}
