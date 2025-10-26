package org.alter.skills.prayer

import dev.openrune.cache.CacheManager
import org.alter.game.pluginnew.Script
import org.alter.rscm.RSCM.asRSCM

class PrayerBuryEvent : Script() {

    val boneEnum = CacheManager.getEnum("enums.bone_data".asRSCM())

    init {

        boneEnum?.values?.forEach {

        }


    }

}