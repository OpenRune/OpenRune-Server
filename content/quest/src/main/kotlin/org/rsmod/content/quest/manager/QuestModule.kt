package org.rsmod.content.quest.manager

import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.plugin.module.PluginModule

public class QuestModule : PluginModule() {
    override fun bind() {
        bindInstance<QuestRequirementResolver>()
        bindInstance<RuneMysteriesQuest>()
    }
}

