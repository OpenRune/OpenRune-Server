@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.MesAnimType

typealias mesanims = BaseMesAnims

object BaseMesAnims {
    public fun mesanim(internal: String): MesAnimType {
        val type =
            ServerCacheManager.getMesAnim("mesanim.${internal}".asRSCM())
                ?: error("Error Loading MesAnim")
        return type
    }

    val quiz = mesanim("quiz")

    val bored = mesanim("bored")

    val short = mesanim("short")

    val happy = mesanim("happy")

    val shocked = mesanim("shocked")

    val confused = mesanim("confused")

    val silent = mesanim("silent")

    val goblin = mesanim("goblin")

    val neutral = mesanim("neutral")

    val shifty = mesanim("shifty")
    val worried = mesanim("worried")

    val drunk = mesanim("drunk")

    val very_mad = mesanim("very_mad")

    val laugh = mesanim("laugh")

    val mad_laugh = mesanim("mad_laugh")

    val sad = mesanim("sad")

    val angry = mesanim("angry")
}
