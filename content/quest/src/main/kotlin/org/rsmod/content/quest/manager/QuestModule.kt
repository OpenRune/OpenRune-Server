package org.rsmod.content.quest.manager

import org.rsmod.api.combat.commons.magic.SpellQuestRequirement
import org.rsmod.content.quest.area.alkharid.princealirescue.DisguiseMakers
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest
import org.rsmod.plugin.module.PluginModule

public class QuestModule : PluginModule() {
    override fun bind() {
        bindInstance<QuestRequirementResolver>()
        bindInstance<RuneMysteriesQuest>()
        bindInstance<GertrudesCatQuest>()
        bindInstance<PrinceAliRescueQuest>()
        bindInstance<DisguiseMakers>()
        addSetBinding<SpellQuestRequirement>(PolicySpellQuestRequirement::class.java)
    }
}
