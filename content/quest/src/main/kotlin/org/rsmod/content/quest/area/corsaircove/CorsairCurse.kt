package org.rsmod.content.quest.area.corsaircove

import jakarta.inject.Inject
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class CorsairCurse
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val regionRepo: RegionRepository,
    private val random: GameRandom,
    private val playerHitModifier: PlayerHitModifier,
    private val collision: CollisionFlagMap,
    private val worldRepo: WorldRepository,
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
) :
    QuestScript(
        CorsairCurseRefs.QuestKey,
        CorsairCurseRefs.QuestVarp,
        rewards { extra("Corsair Cove Bank Access") },
        ItemRewardDisplay("obj.poison", zoom = 400, x = 78, y = 128),
        questVarbit = CorsairCurseRefs.StageVarbit,
        completeJingle = CorsairCurseRefs.CompletionJingle,
    ),
    SpadeDigHook {

    private val fight = CorsairCurseFight(quest, npcRepo, random, playerHitModifier, worldRepo, accuracy, maxHits)
    private val cutscene = CorsairCurseCutscene(regionRepo, npcRepo, collision, worldRepo)
    private val dialogue = CorsairCurseDialogue(quest, cutscene)
    private val scenery = CorsairCurseScenery(quest, locRepo, npcRepo, collision, worldRepo, cutscene)
    private val ferry = CorsairCurseFerry(quest, fight, dialogue)
    private val bank = CorsairCurseBank(quest)

    override fun claims(player: Player): Boolean = scenery.claims(player)

    override suspend fun ProtectedAccess.dig() = with(scenery) { dig() }

    override fun ScriptContext.init() {
        check(quest.maxSteps == CorsairCurseStage.Complete) {
            "The Corsair Curse end state is ${quest.maxSteps} in the cache dbrow, but " +
                "CorsairCurseStage.Complete is ${CorsairCurseStage.Complete}."
        }

        assertCorsairCurseSceneCoords()

        with(dialogue) { register() }
        with(scenery) { register() }
        with(ferry) { register() }
        with(bank) { register() }
        with(fight) { register() }
        with(cutscene) { register() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Captain Tock<col=000080> north of " +
            "<col=800000>Port Sarim<col=000080>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val stage = quest.getQuestStage(player.player)

            strike(MetTheCaptain)
            strike(AgreedToSail)

            if (stage >= CorsairCurseStage.TheoriesRuledOut) {
                strike(SailedToTheCove)
            } else {
                line(SailedToTheCove)
            }

            colinBlock(player)
            ithoiBlock(player, stage)
            arsenBlock(player)
            gnocciBlock(player)
            endgameBlock(stage)
        }

    private fun QuestJournalBuilder.colinBlock(player: ProtectedAccess) {
        val counter = player.corscursCabinBoy
        if (counter < CorsairCurseCounts.ColinInterviewed) {
            return
        }
        line("")
        block(counter, CorsairCurseCounts.ColinInterviewed, ColinMermaid)
        block(counter, CorsairCurseCounts.ColinTelescopeSeen, ColinTelescope)
        block(counter, CorsairCurseCounts.ColinReassured, ColinReassured)
    }

    private fun QuestJournalBuilder.ithoiBlock(player: ProtectedAccess, stage: Int) {
        if (player.corscursNavigator < CorsairCurseCounts.IthoiInterviewed) {
            return
        }
        line("")
        if (stage >= CorsairCurseStage.TheoriesRuledOut) {
            strike(IthoiTheory)
        } else {
            line(IthoiTheory)
        }
    }

    private fun QuestJournalBuilder.arsenBlock(player: ProtectedAccess) {
        val counter = player.corscursThief
        if (counter < CorsairCurseCounts.ArsenTellingStory) {
            return
        }
        line("")
        block(counter, CorsairCurseCounts.ArsenTellingStory, ArsenStory)
        block(counter, CorsairCurseCounts.ArsenInterviewed, ArsenAskedForRelic)
        block(counter, CorsairCurseCounts.ArsenRelicTaken, ArsenRelicTaken)
        block(counter, CorsairCurseCounts.ArsenRelicReturned, ArsenTess)
        block(counter, CorsairCurseCounts.ArsenToldOfTess, ArsenToldOfTess)
        block(counter, CorsairCurseCounts.ArsenReassured, ArsenReassured)
    }

    private fun QuestJournalBuilder.gnocciBlock(player: ProtectedAccess) {
        val counter = player.corscursCook
        if (counter < CorsairCurseCounts.GnocciInterviewed) {
            return
        }
        line("")
        block(counter, CorsairCurseCounts.GnocciInterviewed, GnocciDoll)
        block(counter, CorsairCurseCounts.GnocciDollDug, GnocciDug)
        block(counter, CorsairCurseCounts.GnocciReassured, GnocciReassured)
    }

    private fun QuestJournalBuilder.endgameBlock(stage: Int) {
        if (stage < CorsairCurseStage.TheoriesRuledOut) {
            return
        }
        line("")
        stageBlock(stage, CorsairCurseStage.TheoriesRuledOut, RuledOut)
        stageBlock(stage, CorsairCurseStage.TockToldOfDinner, DinnerTiming)
        stageBlock(stage, CorsairCurseStage.GnocciNamedIthoi, IthoiCooked)
        stageBlock(stage, CorsairCurseStage.ArsenNamedMotive, IthoiMotive)
        stageBlock(stage, CorsairCurseStage.IthoiConfessed, IthoiConfession)
        stageBlock(stage, CorsairCurseStage.IthoiChallenged, IthoiChallenge)
        stageBlock(stage, CorsairCurseStage.IthoiExposed, DriftwoodProof)
        stageBlock(stage, CorsairCurseStage.KillOrdered, KillOrdered)
        stageBlock(stage, CorsairCurseStage.IthoiSlain, IthoiKilled)
    }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(MetTheCaptain)
            line(AgreedToSail)
            line(SailedToTheCove)
            line("")
            line(ColinMermaid)
            line(ColinTelescope)
            line(ColinReassured)
            line("")
            line(IthoiTheory)
            line("")
            line(ArsenStory)
            line(ArsenAskedForRelic)
            line(ArsenRelicTaken)
            line(ArsenTess)
            line(ArsenToldOfTess)
            line(ArsenReassured)
            line("")
            line(GnocciDoll)
            line(GnocciDug)
            line(GnocciReassured)
            line("")
            line(RuledOut)
            line(DinnerTiming)
            line(IthoiCooked)
            line(IthoiMotive)
            line(IthoiConfession)
            line(IthoiChallenge)
            line(DriftwoodProof)
            line(KillOrdered)
            line(IthoiKilled)
            line(TockThanked)
        }

    private companion object {
        private fun QuestJournalBuilder.block(counter: Int, required: Int, text: String) {
            when {
                counter > required -> strike(text)
                counter == required -> line(text)
            }
        }

        private fun QuestJournalBuilder.stageBlock(stage: Int, required: Int, text: String) {
            when {
                stage > required -> strike(text)
                stage == required -> line(text)
            }
        }

        private const val MetTheCaptain =
            "I met the Corsair Captain at the crossroads north of Port Sarim. He wanted help with " +
                "a curse that was afflicting his crew."

        private const val AgreedToSail = "I agreed to go to his ship, moored west of Rimmington."

        private const val SailedToTheCove =
            "The Corsair Captain told me that four of his crew had been affected by a curse: " +
                "Gnocci, Ithoi, Arsen and Colin, all of whom were in their beds. He sailed me " +
                "south to Corsair Cove to meet them and investigate."

        private const val ColinMermaid =
            "Cabin Boy Colin thought the curse might have come because he offended a mermaid he " +
                "saw through Ithoi's telescope."

        private const val ColinTelescope =
            "However, I looked through that telescope myself, and saw an ogre rather than a mermaid."

        private const val ColinReassured =
            "I told Colin that there was no mermaid there, to reassure him that the curse was not " +
                "his fault."

        private const val IthoiTheory =
            "Ithoi the Navigator said he knew a lot about curses. He said there were any number of " +
                "things that could cause one."

        private const val ArsenStory =
            "Arsen the Thief told me he had been exploring a cave to the west of the cove. In " +
                "there, he found fearsome ogres. He quickly stole an artefact from them, and fled. " +
                "When the curse began, Arsen thought the ogres had cursed him for stealing a " +
                "sacred relic from them."

        private const val ArsenAskedForRelic =
            "He asked me to retrieve the relic from Captain Tock and return it to the ogre chief " +
                "with his apologies."

        private const val ArsenRelicTaken =
            "I retrieved the ogre artefact from Captain Tock to return it to the ogre chief in " +
                "their cave to the west of the cove."

        private const val ArsenTess =
            "Tess, the ogre chief, said the artefact was a tooth-pick, and she didn't care about " +
                "it being stolen. She said her shamans had not cursed the crew."

        private const val ArsenToldOfTess =
            "I returned to Arsen and began telling him what Tess had told me."

        private const val ArsenReassured =
            "I was able to reassure Arsen that the curse was not his fault."

        private const val GnocciDoll =
            "Gnocci the Cook thought the curse might have come because he awakened a possessed " +
                "doll he found while fishing. He buried the doll in the sand but was afraid that " +
                "its possessor might have cursed them."

        private const val GnocciDug =
            "However, I dug up the doll and found that it was clockwork, and did not appear to be " +
                "possessed. It fell in the sea and was lost again."

        private const val GnocciReassured =
            "I told Gnocci that the doll was not possessed, to reassure him that the curse was not " +
                "his fault."

        private const val RuledOut =
            "I had ruled out all the corsairs' theories about how they had become cursed."

        private const val DinnerTiming =
            "Captain Tock mentioned that the curse had begun straight after the crew's dinner, " +
                "although Ithoi wasn't there to eat that meal."

        private const val IthoiCooked =
            "Gnocci the Cook told me that the Ithoi had prepared the meal that the crew ate " +
                "immediately before becoming sick, though Ithoi himself did not eat that meal."

        private const val IthoiMotive =
            "Arsen the Thief told me that Ithoi might be about to get fired from the crew."

        private const val IthoiConfession =
            "Ithoi the Navigator admitted poisoning the dinner and pretending to be sick so that " +
                "the Corsairs would think they were cursed, and would therefore want him to stay " +
                "on the crew."

        private const val IthoiChallenge =
            "He challenged me to prove he was faking his own illness."

        private const val DriftwoodProof =
            "By igniting the driftwood under Ithoi's hut, I made him run out, proving he'd faked " +
                "his illness. I also saw him cast a magic spell, showing he has some magical " +
                "combat ability."

        private const val KillOrdered =
            "When I told Captain Tock that I'd seen Ithoi running around, thus proving he'd faked " +
                "his illness, the Captain told me to kill Ithoi."

        private const val IthoiKilled = "I fought Ithoi and successfully killed him."

        private const val TockThanked = "Captain Tock thanked me for all my help."
    }
}
