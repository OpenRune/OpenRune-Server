package org.rsmod.content.interfaces.skill.guides.configs

import org.rsmod.api.type.refs.varbit.VarBitReferences

typealias guide_varbits = SkillGuideVarBits

object SkillGuideVarBits : VarBitReferences() {
    val selected_skill = varBit("skill_guide_skill")
    val selected_subsection = varBit("skill_guide_subsection")
}
