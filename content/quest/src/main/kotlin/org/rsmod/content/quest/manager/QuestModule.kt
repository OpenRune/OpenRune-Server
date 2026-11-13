package org.rsmod.content.quest.manager

import org.rsmod.api.combat.commons.magic.SpellQuestRequirement
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.content.generic.locs.bookcases.BookcaseSearchHook
import org.rsmod.content.quest.area.asgarnia.icemountain.BelowIceMountain
import org.rsmod.content.quest.area.asgarnia.icemountain.BimGuardian
import org.rsmod.content.quest.area.asgarnia.icemountain.BimGuardianHook
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.TouristCentreBookcases
import org.rsmod.content.quest.area.falador.doricsquest.DoricsQuest
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.XMarksTheSpotQuest
import org.rsmod.content.quest.area.rimmington.WitchsPotionRatTail
import org.rsmod.content.quest.area.varrock.daddyshome.DaddysHomeQuest
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest
import org.rsmod.plugin.module.PluginModule

public class QuestModule : PluginModule() {
    override fun bind() {
        bindInstance<QuestRequirementResolver>()
        bindInstance<BelowIceMountain>()
        bindInstance<BimGuardian>()
        bindInstance<RuneMysteriesQuest>()
        bindInstance<GertrudesCatQuest>()
        bindInstance<DaddysHomeQuest>()
        bindInstance<XMarksTheSpotQuest>()
        bindInstance<DoricsQuest>()
        bindInstance<IdesOfMilkQuest>()
        bindInstance<KnightsSwordQuest>()
        addSetBinding<SpellQuestRequirement>(PolicySpellQuestRequirement::class.java)
        addSetBinding<BookcaseSearchHook>(TouristCentreBookcases::class.java)
        addSetBinding<SpadeDigHook>(XMarksTheSpotQuest::class.java)
        addSetBinding<NpcDeathKillHook>(WitchsPotionRatTail::class.java)
        addSetBinding<NpcDeathKillHook>(BimGuardianHook::class.java)
    }
}
