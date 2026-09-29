package org.rsmod.content.quest.area.lumbridge

import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

class TheRestlessGhost
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) :
    QuestScript(
        "quest_restlessghost",
        "varp.prieststart",
        rewards {
            xp("stat.prayer", 1125.0)
            extra("A Ghostspeak Amulet")
        },
        ItemRewardDisplay("obj.ghostskull"),
        questVarbit = "varbit.restless_ghost_progress",
    ) {

    private val cutscene = TheRestlessGhostCutscene(quest, regionRepo, npcRepo, locRepo, worldRepo)
    private val scenery = TheRestlessGhostScenery(quest, locRepo, npcRepo, worldRepo, cutscene, aiPlayerInteractions)
    private val dialogue = TheRestlessGhostDialogue(quest)

    override fun ScriptContext.init() {
        check(quest.maxSteps == TheRestlessGhostStage.Complete) {
            "The Restless Ghost end state is ${quest.maxSteps} in the cache, but " +
                "TheRestlessGhostStage.Complete is ${TheRestlessGhostStage.Complete}."
        }

        assertRestlessGhostSceneCoords()

        with(dialogue) { register() }
        with(scenery) { register() }
        with(cutscene) { register() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Father Aereck<col=000080> in the chapel in " +
            "<col=800000>Lumbridge<col=000080>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val stage = quest.getQuestStage(player.player)

            if (stage >= TheRestlessGhostStage.GhostSpoken) {
                strike(
                    "Father Aereck asked me to help him deal with the Ghost in the graveyard next " +
                        "to the church in Lumbridge."
                )
            } else {
                strike(
                    "Father Aereck asked me to help him deal with the Ghost in the graveyard next " +
                        "to the church."
                )
            }

            if (stage == TheRestlessGhostStage.Started) {
                line(
                    "I should find <red>Father Urhney</red> who is an expert on <red>ghosts</red>."
                )
                line(
                    "He lives in a <red>shack</red> in the far west of " +
                        "<red>Lumbridge Swamp</red>."
                )
            }

            if (stage >= TheRestlessGhostStage.HasAmulet) {
                strike(
                    "I found Father Urhney in the far west of the swamp south of Lumbridge. He " +
                        "gave me an Amulet of Ghostspeak to talk to the ghost."
                )
            }
            if (stage == TheRestlessGhostStage.HasAmulet) {
                line(
                    "I should talk to the <red>Ghost</red> in the graveyard next to the church in " +
                        "Lumbridge to find out why it is haunting the <red>graveyard crypt</red>."
                )
            }

            if (stage >= TheRestlessGhostStage.SkullFound) {
                strike(
                    "I spoke to the Ghost and he told me he could not rest in peace because an " +
                        "evil wizard had stolen his skull."
                )
            }
            if (stage == TheRestlessGhostStage.GhostSpoken) {
                strike(
                    "I spoke to the Ghost and he told me he could not rest in peace because an " +
                        "evil wizard had stolen his skull."
                )
                line(
                    "I should go and search the <red>Wizards' Tower South West of Lumbridge</red> " +
                        "for the <red>Ghost's Skull</red>."
                )
            }

            if (stage == TheRestlessGhostStage.SkullFound) {
                strike(
                    "I found the Ghost's Skull in the basement of the Wizards' Tower. It was " +
                        "guarded by a skeleton, but I took it anyway."
                )
                line(
                    "I should take the <red>Skull</red> back to the <red>Ghost</red> coffin so it " +
                        "can rest."
                )
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "Father Aereck asked me to help him deal with the Ghost in the graveyard next to " +
                    "the church."
            )
            line(
                "I found Father Urhney in the swamp south of Lumbridge. He gave me an Amulet of " +
                    "Ghostspeak to talk to the ghost."
            )
            line(
                "I spoke to the Ghost and he told me he could not rest in peace because an evil " +
                    "wizard had stolen his skull."
            )
            line(
                "I found the Ghost's Skull in the basement of the Wizard's Tower. It was guarded " +
                    "by a skeleton, but I took it anyway."
            )
            line(
                "I placed the Skull in the Ghost's coffin, and allowed it to rest in peace once " +
                    "more, with gratitude for my help."
            )
        }
}
