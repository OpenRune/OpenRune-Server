package org.rsmod.content.skills.agility

import org.rsmod.plugin.module.PluginModule

class AgilityModule : PluginModule() {
    override fun bind() {
        addSetBinding<AgilityCourseHook>(WyrmTermites::class.java)
        addSetBinding<AgilityCourseHook>(PrifddinasPortals::class.java)
        addSetBinding<AgilityCourseHook>(WildernessLaps::class.java)
    }
}
