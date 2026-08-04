package org.rsmod.content.skills.mining.configs

import dev.openrune.ParamReferences.param
import dev.openrune.types.SequenceServerType

object MiningParams {
    val skill_action_delay = param<Int>("skill_action_delay")
    val skill_wall_anim = param<SequenceServerType>("skill_wall_anim")
}
