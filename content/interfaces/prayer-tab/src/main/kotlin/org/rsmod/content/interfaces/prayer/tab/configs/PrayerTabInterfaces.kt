package org.rsmod.content.interfaces.prayer.tab.configs

import dev.openrune.component
import dev.openrune.inter

internal typealias prayer_components = PrayerTabComponents

internal typealias prayer_interfaces = PrayerTabInterfaces

object PrayerTabComponents {
    val quick_prayers_orb = component("orbs:prayerbutton")
    val quick_prayers_close = component("quickprayer:close")
    val quick_prayers_setup = component("quickprayer:buttons")
    val filters = component("prayerbook:filtermenu")
}

object PrayerTabInterfaces {
    val quickprayer = inter("quickprayer")
}
