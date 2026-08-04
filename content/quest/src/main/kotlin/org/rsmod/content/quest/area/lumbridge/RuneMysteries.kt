package org.rsmod.content.quest.area.lumbridge

import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class RuneMysteriesQuest : QuestScript(
    "quest_runemysteries", "varp.runemysteries", rewards {
    extra("Access to the Rune Essence Mine") },
    ItemRewardDisplay(AIR_TALISMAN)
) {

    val doubtedIdentity = quest.attribute(name = "DOUBTED_IDENTITY", default = false)

    override fun ScriptContext.init() {

    }

    override fun subTitle(): String =
        "talking to <col=800000>Duke Horacio</col> on the <col=800000>1<sup>st</sup> floor of Lumbridge Castle</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            objective(
                "I spoke to Duke Horacio in Lumbridge Castle. He told me that he'd found a " +
                    "<red>Strange Talisman</red> in the Castle which might be of use to the Order of " +
                    "the Wizards at the Wizards' Tower. He asked me to take it there and give it to " +
                    "a wizard called <red>Sedridor</red>.",
            ) {
                stageAtLeast(
                    STAGE_PACKAGE,
                    "I delivered the Strange Talisman to Sedridor in the basement of the " +
                        "Wizards' Tower. He believes it might be key to discovering a Teleportation " +
                        "Incantation to the lost Rune Essence Mine. He asked me to help confirm this " +
                        "by delivering a <red>Package</red> to Aubury, an expert on Runecrafting. I " +
                        "can find him in his Rune Shop in south east Varrock.",
                ).finalise()
            }

            objective("If I lose the Package, I'll need to ask Sedridor for another.") {
                visibleWhen {
                    quest.getQuestStage(access.player) == STAGE_PACKAGE &&
                        !access.inv.contains(RESEARCH_PACKAGE)
                }
            }

            objective("I delivered the Package to Aubury at his Rune Shop in south east Varrock. " + "I should see what he can tell me about the Teleportation Incantation.") {
                visibleWhen { quest.getQuestStage(access.player) >= STAGE_AWAITING_NOTES }
                stageAtLeast(
                    STAGE_NOTES,
                    "I delivered the Package to Aubury at his Rune Shop in south east Varrock. " +
                        "He confirmed Sedridor's suspicions and asked me to take some " +
                        "<red>Research Notes</red> back to him. I can find Sedridor in the basement " +
                        "of the Wizards' Tower.",
                ).finalise()
            }

            objective(
                "I delivered the Package to Aubury at his Rune Shop in south east Varrock. " +
                    "He confirmed Sedridor's suspicions and asked me to take some Research Notes " +
                    "back to him, which I did. I should see what Sedridor has learnt from them.",
            ) {
                visibleWhen {
                    quest.getQuestStage(access.player) >= STAGE_NOTES &&
                        !access.inv.contains(RESEARCH_NOTES) &&
                        quest.questState(access.player) != QuestProgressState.FINISHED
                }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "I spoke to Duke Horacio in Lumbridge Castle. He told me that he'd found a " +
                    "Strange Talisman in the Castle which might be of use to the Order of the " +
                    "Wizards at the Wizards' Tower. He asked me to take it there and give it to a " +
                    "wizard called Sedridor.",
            )
            line(
                "I delivered the Strange Talisman to Sedridor in the Wizards' Tower. He believed " +
                    "it might be key to discovering a Teleportation Incantation to the lost Rune " +
                    "Essence Mine. He asked me to help confirm this by delivering a Package to " +
                    "Aubury, an expert on Runecrafting.",
            )
            line(
                "I delivered the Package to Aubury in Varrock. He confirmed Sedridor's suspicions " +
                    "and asked me to take some Research Notes back to him. I did so, and Sedridor " +
                    "used them to discover the Teleportation Incantation to the lost Rune Essence " +
                    "Mine. As a thank you for my help, he granted me permission to use the Rune " +
                    "Essence Mine whenever I please.",
            )
        }

    companion object {

        const val STAGE_TALISMAN = 1
        const val STAGE_TALISMAN_GIVEN = 2
        const val STAGE_PACKAGE = 3
        const val STAGE_AWAITING_NOTES = 4
        const val STAGE_NOTES = 5
        const val STAGE_COMPLETE = 6

        const val AIR_TALISMAN = "obj.air_talisman"
        const val RESEARCH_PACKAGE = "obj.research_package"
        const val RESEARCH_NOTES = "obj.research_notes"
    }
}
