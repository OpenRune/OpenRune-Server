package org.rsmod.content.interfaces.prayer.tab.configs

import dev.openrune.ParamReferences.param
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.aconverted.SynthType

internal typealias prayer_params = PrayerTabParams

object PrayerTabParams {
    val id = param<Int>("prayer_id")
    val component = param<ComponentType>("prayer_component")
    val name = param<String>("prayer_name")
    val level = param<Int>("prayer_levelreq")
    val sound = param<SynthType>("prayer_sound")
    val varbit = param<VarBitType>("prayer_varbit")
    val overhead = param<Int>("prayer_overhead")
    val unlock_varbit = param<VarBitType>("prayer_unlock_varbit")
    val unlock_state = param<Int>("prayer_unlock_state")
    val locked_message = param<String>("prayer_locked_message")
    val drain_effect = param<Int>("prayer_drain_effect")
}
