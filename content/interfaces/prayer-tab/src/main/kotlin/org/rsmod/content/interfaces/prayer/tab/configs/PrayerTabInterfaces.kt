package org.rsmod.content.interfaces.prayer.tab.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences

internal typealias prayer_components = PrayerTabComponents

internal typealias prayer_interfaces = PrayerTabInterfaces

object PrayerTabComponents : ComponentReferences() {
    val quick_prayers_orb = component("orbs:prayerbutton")
    val quick_prayers_close = component("quickprayer:close")
    val quick_prayers_setup = component("quickprayer:buttons")
    val filters = component("prayerbook:filtermenu")
}

object PrayerTabInterfaces : InterfaceReferences() {
    val quickprayer = inter("quickprayer")
}
