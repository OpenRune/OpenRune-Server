@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.player.dialogue

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.MesAnimType

public typealias mesanims = BaseMesAnims

public object BaseMesAnims {

    public fun mesanim(internal: String): MesAnimType {
        val type = ServerCacheManager.getMesAnim(internal.asRSCM()) ?: error("Error Loading MesAnim")
        return type
    }

    public val quiz: MesAnimType
        get() = mesanim("mesanim.quiz")

    public val bored: MesAnimType
        get() = mesanim("mesanim.bored")

    public val short: MesAnimType
        get() = mesanim("mesanim.short")

    public val happy: MesAnimType
        get() = mesanim("mesanim.happy")

    public val shocked: MesAnimType
        get() = mesanim("mesanim.shocked")

    public val confused: MesAnimType
        get() = mesanim("mesanim.confused")

    public val silent: MesAnimType
        get() = mesanim("mesanim.silent")

    public val neutral: MesAnimType
        get() = mesanim("mesanim.neutral")

    public val shifty: MesAnimType
        get() = mesanim("mesanim.shifty")

    public val worried: MesAnimType
        get() = mesanim("mesanim.worried")

    public val drunk: MesAnimType
        get() = mesanim("mesanim.drunk")

    public val verymad: MesAnimType
        get() = mesanim("mesanim.very_mad")

    public val laugh: MesAnimType
        get() = mesanim("mesanim.laugh")

    public val madlaugh: MesAnimType
        get() = mesanim("mesanim.mad_laugh")

    public val sad: MesAnimType
        get() = mesanim("mesanim.sad")

    public val angry: MesAnimType
        get() = mesanim("mesanim.angry")

}
