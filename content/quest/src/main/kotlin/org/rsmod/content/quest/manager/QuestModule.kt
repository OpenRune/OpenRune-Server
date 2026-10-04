package org.rsmod.content.quest.manager

import org.rsmod.api.combat.commons.magic.SpellQuestRequirement
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest
import org.rsmod.content.quest.area.lumbridge.XMarksTheSpot
import org.rsmod.plugin.module.PluginModule

public class QuestModule : PluginModule() {
    override fun bind() {
        bindInstance<QuestRequirementResolver>()
        bindInstance<RuneMysteriesQuest>()
        bindInstance<GertrudesCatQuest>()
        bindInstance<XMarksTheSpot>()
        addSetBinding<SpellQuestRequirement>(PolicySpellQuestRequirement::class.java)
        addSetBinding<SpadeDigHook>(XMarksTheSpot::class.java)
    }
}
