package org.rsmod.content.interfaces.prayer.tab.configs

import dev.openrune.synth

internal typealias prayer_sounds = PrayerTabSounds

object PrayerTabSounds {
    val disable = synth("prayer_disable")
    val drain = synth("prayer_drain")
}
