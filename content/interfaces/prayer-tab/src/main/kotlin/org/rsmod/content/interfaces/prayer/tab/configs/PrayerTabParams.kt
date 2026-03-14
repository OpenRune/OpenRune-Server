package org.rsmod.content.interfaces.prayer.tab.configs

import org.rsmod.api.type.builders.param.ParamBuilder
import org.rsmod.api.type.refs.param.ParamReferences
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.synth.SynthType
import org.rsmod.game.type.varbit.VarBitType

internal typealias prayer_params = PrayerTabParams

object PrayerTabParams : ParamReferences() {
    val id = param<Int>("prayer_id", 88673346945)
    val component = param<ComponentType>("prayer_component", 61646266434)
    val name = param<String>("prayer_name", 9222941699801316537)
    val level = param<Int>("prayer_levelreq", 29733805813656036)
    val sound = param<SynthType>("prayer_sound", 67558440545)
    val varbit = param<VarBitType>("prayer_varbit")
    val overhead = param<Int>("prayer_overhead")
    val unlock_varbit = param<VarBitType>("prayer_unlock_varbit")
    val unlock_state = param<Int>("prayer_unlock_state")
    val locked_message = param<String>("prayer_locked_message")
    val drain_effect = param<Int>("prayer_drain_effect")
}

internal object PrayerTabParamBuilder : ParamBuilder() {
    init {
        build<Int>("prayer_overhead")
        build<VarBitType>("prayer_varbit")
        build<VarBitType>("prayer_unlock_varbit")
        build<Int>("prayer_unlock_state") { default = 1 }
        build<String>("prayer_locked_message")
        build<Int>("prayer_drain_effect")
    }
}
