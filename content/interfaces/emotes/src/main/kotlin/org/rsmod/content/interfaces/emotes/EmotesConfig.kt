package org.rsmod.content.interfaces.emotes

import dev.openrune.component
import dev.openrune.types.SequenceServerType
import dev.openrune.types.StatType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.enums.enum
import dev.openrune.varBit
import dev.openrune.varp
import dev.openrune.walkTrigger

typealias emote_components = EmoteComponents

internal typealias emote_varps = EmoteVarps

internal typealias emote_varbits = EmoteVarBits

internal typealias emote_walktriggers = EmoteWalkTriggers

object EmoteComponents {
    val emote_list = component("emote:contents")
}

internal object EmoteVarps {
    val emote_counters = varp("emote_counters")
    val emote_clock_premier_shield = varp("emote_clock_premier_shield")
}

internal object EmoteVarBits {
    val emote_counters_crazy_dance = varBit("emote_counters_crazy_dance")
    val emote_counters_premier_shield = varBit("emote_counters_premier_shield")
}

internal object EmoteWalkTriggers {
    val cancelanim = walkTrigger("emote_cancelanim")
}
