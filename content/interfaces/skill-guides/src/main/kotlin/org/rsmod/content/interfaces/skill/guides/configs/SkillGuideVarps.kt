package org.rsmod.content.interfaces.skill.guides.configs

import dev.openrune.varBit

typealias guide_varbits = SkillGuideVarBits

object SkillGuideVarBits {
    val selected_skill = varBit("skill_guide_skill")
    val selected_subsection = varBit("skill_guide_subsection")
}
