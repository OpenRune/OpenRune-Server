package org.rsmod.content.interfaces.prayer.tab.configs

import org.rsmod.api.type.refs.queue.QueueReferences

internal typealias prayer_queues = PrayerTabQueues

object PrayerTabQueues : QueueReferences() {
    val toggle = queue("prayer_toggle")
    val quick_prayer = queue("quick_prayer_toggle")
}
