package org.rsmod.content.interfaces.skill.guides.configs

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.enums.enum

typealias guide_enums = SkillGuideEnums

object SkillGuideEnums {
    val open_buttons = enum<ComponentType, Int>("skill_guide_button_vars")
    val subsection_buttons = enum<ComponentType, Int>("skill_guide_section_vars")
}
