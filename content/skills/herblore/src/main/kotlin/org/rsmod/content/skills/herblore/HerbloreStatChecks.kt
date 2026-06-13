package org.rsmod.content.skills.herblore

import dev.openrune.types.StatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.Tuple2

internal fun ProtectedAccess.hasStatReqs(reqs: List<Tuple2<StatType, Int>>): Boolean =
    reqs.all { statBase(it.t0.internalName) >= it.t1 }

internal suspend fun ProtectedAccess.meetsStatReqs(reqs: List<Tuple2<StatType, Int>>): Boolean {
    for (req in reqs) {
        val stat = req.t0
        val level = req.t1
        if (statBase(stat.internalName) < level) {
            val name = stat.displayName.ifEmpty { stat.internalName.removePrefix("stat.") }
            mesbox("You need a $name level of $level.")
            return false
        }
    }
    return true
}

internal fun List<Tuple2<StatType, Int>>.primaryLevel(): Int = first().t1
