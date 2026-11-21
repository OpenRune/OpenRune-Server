package org.rsmod.content.skills.agility

import org.rsmod.content.skills.agility.arena.ArenaPlankFalls
import org.rsmod.content.skills.agility.werewolf.WerewolfStickThrow
import org.rsmod.plugin.module.PluginModule

class AgilityModule : PluginModule() {
    override fun bind() {
        addSetBinding<AgilityCourseHook>(WyrmTermites::class.java)
        addSetBinding<AgilityCourseHook>(PrifddinasPortals::class.java)
        addSetBinding<AgilityCourseHook>(WildernessLaps::class.java)
        addSetBinding<AgilityCourseHook>(WerewolfStickThrow::class.java)
        addSetBinding<AgilityCourseHook>(DorgeshKaanCourse::class.java)
        addSetBinding<AgilityCourseHook>(ArenaPlankFalls::class.java)
    }
}
