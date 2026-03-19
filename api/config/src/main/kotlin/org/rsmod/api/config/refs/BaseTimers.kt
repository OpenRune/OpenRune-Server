@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import dev.openrune.timer

typealias timers = BaseTimers

object BaseTimers {
    val toxins = timer("toxins")
    val stat_regen = timer("stat_regen")
    val stat_boost_restore = timer("stat_boost_restore")
    val health_regen = timer("health_regen")
    val rapidrestore_regen = timer("rapidrestore_regen")
    val spec_regen = timer("spec_regen")
    val prayer_drain = timer("prayer_drain")
}
