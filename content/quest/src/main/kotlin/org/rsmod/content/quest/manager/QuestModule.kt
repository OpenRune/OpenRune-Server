package org.rsmod.content.quest.manager

import org.rsmod.api.combat.commons.magic.SpellQuestRequirement
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest
import org.rsmod.content.quest.area.varrock.daddyshome.DaddysHomeQuest
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest
import org.rsmod.plugin.module.PluginModule

public class QuestModule : PluginModule() {
    override fun bind() {
        bindInstance<QuestRequirementResolver>()
        bindInstance<RuneMysteriesQuest>()
        bindInstance<GertrudesCatQuest>()
        bindInstance<DaddysHomeQuest>()
        bindInstance<XMarksTheSpotQuest>()
        addSetBinding<SpellQuestRequirement>(PolicySpellQuestRequirement::class.java)
        addSetBinding<SpadeDigHook>(XMarksTheSpotQuest::class.java)
    }
}
