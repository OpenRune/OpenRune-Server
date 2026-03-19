package org.rsmod.api.spells.autocast.configs

import dev.openrune.ParamReferences.param
import org.rsmod.api.config.aliases.ParamObj

internal typealias autocast_params = AutocastParams

internal object AutocastParams {
    val additional_spell_autocast1: ParamObj = param("additional_spell_autocast1")
    val additional_spell_autocast2: ParamObj = param("additional_spell_autocast2")
    val additional_spell_autocast3: ParamObj = param("additional_spell_autocast3")
}
