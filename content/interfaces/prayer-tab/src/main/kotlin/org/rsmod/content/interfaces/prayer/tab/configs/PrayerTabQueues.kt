package org.rsmod.content.interfaces.prayer.tab.configs

import dev.openrune.queue

internal typealias prayer_queues = PrayerTabQueues

object PrayerTabQueues {
    val toggle = queue("prayer_toggle")
    val quick_prayer = queue("quick_prayer_toggle")
}
