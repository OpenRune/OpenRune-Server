package org.rsmod.content.interfaces.skill.guides.configs

import dev.openrune.component
import dev.openrune.inter

typealias guide_interfaces = SkillGuideInterfaces

typealias guide_components = SkillGuideComponents

object SkillGuideInterfaces {
    val skill_guide = inter("skill_guide")
}

object SkillGuideComponents {
    val attack = component("stats:attack")
    val strength = component("stats:strength")
    val defence = component("stats:defence")
    val ranged = component("stats:ranged")
    val prayer = component("stats:prayer")
    val magic = component("stats:magic")
    val runecraft = component("stats:runecraft")
    val construction = component("stats:construction")
    val hitpoints = component("stats:hitpoints")
    val agility = component("stats:agility")
    val herblore = component("stats:herblore")
    val thieving = component("stats:thieving")
    val crafting = component("stats:crafting")
    val fletching = component("stats:fletching")
    val slayer = component("stats:slayer")
    val hunter = component("stats:hunter")
    val mining = component("stats:mining")
    val smithing = component("stats:smithing")
    val fishing = component("stats:fishing")
    val cooking = component("stats:cooking")
    val firemaking = component("stats:firemaking")
    val woodcutting = component("stats:woodcutting")
    val farming = component("stats:farming")

    val subsection_1 = component("skill_guide:00")
    val subsection_2 = component("skill_guide:01")
    val subsection_3 = component("skill_guide:02")
    val subsection_4 = component("skill_guide:03")
    val subsection_5 = component("skill_guide:04")
    val subsection_6 = component("skill_guide:05")
    val subsection_7 = component("skill_guide:06")
    val subsection_8 = component("skill_guide:07")
    val subsection_9 = component("skill_guide:08")
    val subsection_10 = component("skill_guide:09")
    val subsection_11 = component("skill_guide:10")
    val subsection_12 = component("skill_guide:11")
    val subsection_13 = component("skill_guide:12")
    val subsection_14 = component("skill_guide:13")
    val subsection_entry_list = component("skill_guide:icons")
    val close_button = component("skill_guide:close")
}
