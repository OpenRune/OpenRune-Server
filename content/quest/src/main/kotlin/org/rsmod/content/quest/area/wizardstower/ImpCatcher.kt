package org.rsmod.content.quest.area.wizardstower

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.VarpSync
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class ImpCatcher @Inject constructor(private val worldRepo: WorldRepository) :
    QuestScript(
        "quest_impcatcher",
        "varp.imp",
        rewards {
            xp("stat.magic", MagicXpReward)
            extra("An Amulet of Accuracy")
        },
        ItemRewardDisplay(AmuletOfAccuracy, zoom = 400),
        questVarbit = "varbit.imp_catcher_progress",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == QuestCompleteStage) {
            "Imp Catcher end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the hand-in writes $QuestCompleteStage."
        }

        onOpNpc1("npc.wizard_mizgog") { startMizgog(it.npc) }
        onOpNpc3("npc.wizard_mizgog") { purchaseAmulet(it.npc) }
        onOpNpc1("npc.wizard_grayzag") { startDialogue(it.npc) { grayzagDialogue() } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Wizard Mizgog</col> on the top floor of the " +
            "<col=800000>Wizards' Tower</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "<red>Wizard Mizgog</red> has asked me to recover the four magical beads that " +
                    "<red>Wizard Grayzag's</red> imps stole from him."
            )

            objective(
                "I need to bring him a <red>black bead</red>, a <red>red bead</red>, a " +
                    "<red>white bead</red> and a <red>yellow bead</red>. The imps have spread out " +
                    "all over the kingdom."
            ) {
                hasItem("black_bead", "I have found the <red>black bead</red>.")
                    .preserveObjective(strikeObjective = false)
                hasItem("red_bead", "I have found the <red>red bead</red>.")
                    .preserveObjective(strikeObjective = false)
                hasItem("white_bead", "I have found the <red>white bead</red>.")
                    .preserveObjective(strikeObjective = false)
                hasItem("yellow_bead", "I have found the <red>yellow bead</red>.")
                    .preserveObjective(strikeObjective = false)
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "Wizard Grayzag summoned an army of imps to torment Wizard Mizgog, and they made " +
                    "off with his four magical beads."
            )
            line(
                "I tracked down a black, a red, a white and a yellow bead and returned them, and " +
                    "Mizgog enchanted them into an amulet of accuracy for me."
            )
        }

    private suspend fun ProtectedAccess.startMizgog(npc: Npc) {
        startDialogue(npc) { mizgogDialogue(npc) }
    }

    private suspend fun Dialogue.mizgogDialogue(npc: Npc) {
        when {
            quest.isQuestCompleted(player) -> dialogAfterQuest(npc)
            quest.isQuestInProgress(player) -> dialogDuringQuest(npc)
            else -> dialogQuestNotStarted(npc)
        }
    }

    private suspend fun Dialogue.dialogQuestNotStarted(npc: Npc) {
        if (!askForQuest()) {
            return
        }

        explainTheImps()

        chatNpc(
            sad,
            "These imps have now spread out all over the kingdom. Could you get my beads back " +
                "for me?",
        )

        val accept = choice2("Yes.", true, "No.", false, title = "Start the Imp Catcher quest?")
        if (!accept) {
            chatPlayer(neutral, "I've better things to do than chase imps.")
            chatNpc(
                angry,
                "Well if you're not interested in the quests I have to give you, don't waste my " +
                    "time by asking me for them.",
            )
            return
        }

        quest.advanceQuestStage(access)

        if (!hasAllBeads(player)) {
            chatPlayer(neutral, "I'll try.")
            chatNpc(happy, "That's great, thank you.")
            return
        }

        chatPlayer(shocked, "Well this is a surprising turn of events!")
        chatNpc(happy, "What?")
        chatPlayer(happy, "Well I just so happen to have all of those beads on me!")
        chatNpc(
            angry,
            "Are you saying that you stole my beads all this time and I've been blaming these imps!?",
        )
        chatPlayer(
            worried,
            "No, not at all! I just found them throughout my travels and presumed somebody would " +
                "need them at some point.",
        )
        chatNpc(angry, "Bah! Fine.")
        handInBeads(npc)
    }

    private suspend fun Dialogue.dialogDuringQuest(npc: Npc) {
        chatNpc(quiz, "So how are you doing finding my beads?")

        val held = Beads.count { player.inv.count(it) > 0 }

        when (held) {
            0 -> {
                chatPlayer(sad, "I've not found any yet.")
                chatNpc(
                    angry,
                    "Well get on with it. I've lost a white bead, a red bead, a black bead, and a " +
                        "yellow bead. Go kill some imps!",
                )
            }
            Beads.size -> {
                chatPlayer(happy, "I've got all four beads. It was hard work I can tell you.")
                handInBeads(npc)
            }
            else -> {
                chatPlayer(neutral, "I have found some of your beads.")
                chatNpc(
                    neutral,
                    "Come back when you have them all. The colour of the four beads that I need " +
                        "are red, yellow, black, and white. Go chase some imps!",
                )
            }
        }
    }

    private suspend fun Dialogue.grayzagDialogue() {
        when {
            quest.isQuestCompleted(player) -> {
                chatNpc(angry, "So you think finding those beads makes you clever, do you?")
                chatPlayer(happy, "Well yes, actually.")
                chatNpc(
                    angry,
                    "Well you'd better just watch your back, because when you least expect it " +
                        "I'll be there. You shouldn't go sticking your nose into other people's " +
                        "affairs, meddler.",
                )
            }
            quest.isQuestInProgress(player) ->
                chatNpc(
                    laugh,
                    "You're a fool, ${player.displayName}. Do you really think you'll find four " +
                        "imps out of thousands? Good luck. Ha!",
                )
            else -> chatNpc(angry, "Not now, I'm trying to concentrate on a very difficult spell!")
        }
    }

    private suspend fun Dialogue.askForQuest(): Boolean {
        chatPlayer(neutral, "Give me a quest!")
        chatNpc(happy, "Give me a quest what?")

        val choice =
            choice3(
                "Give me a quest please.",
                1,
                "Give me a quest or else!",
                2,
                "Just stop messing around and give me a quest!",
                3,
            )

        when (choice) {
            2 -> {
                chatPlayer(angry, "Give me a quest or else!")
                chatNpc(happy, "Or else what? You'll attack me? ")
                chatNpc(laugh, "Hahaha!")
                return false
            }
            3 -> {
                chatPlayer(angry, "Just stop messing around and give me a quest!")
                chatNpc(happy, "Ah now you're assuming I have one to give.")
                return false
            }
        }

        chatPlayer(neutral, "Give me a quest please.")
        return true
    }

    private suspend fun Dialogue.explainTheImps() {
        chatNpc(happy, "Well seeing as you asked nicely... I could do with some help.")
        chatNpc(
            sad,
            "The wizard Grayzag next door decided he didn't like me so he enlisted an army of " +
                "hundreds of imps.",
        )
        chatNpc(
            sad,
            "These imps stole all sorts of my things. Most of these things I don't really care " +
                "about, just eggs and balls of string and things.",
        )
        chatNpc(
            sad,
            "But they stole my four magical beads. There was a red one, a yellow one, a black " +
                "one, and a white one.",
        )
    }

    private suspend fun Dialogue.handInBeads(npc: Npc) {
        chatNpc(
            happy,
            "Give them here and I'll check that they really are MY beads, before I give you your " +
                "reward. You'll like it, it's an amulet of accuracy.",
        )

        val state = HandIn()
        try {
            mesboxNp("You give four coloured beads to Wizard Mizgog.")
            fadeToBlack()

            delay(1)
            access.midiJingle(CutsceneJingle)
            access.camLookAtV3(CamLookAt, height = 350, rate = 232, rate2 = 100)
            access.camMoveToV3(CamMoveTo, height = 775, rate = 232, rate2 = 100)

            delay(1)
            fadeFromBlack()

            delay(2)
            npc.faceSquare(TableCoord)

            delay(1)
            npc.anim(EnchantSeq)
            access.soundSynth("synth.mizgog_placebeads")

            delay(2)
            if (!deleteBeads()) {
                access.mes("You no longer have all four beads.")
                return
            }
            state.beadsTaken = true
            // Preview the table transform without completing saved progress before the reward.
            val nativeStage = checkNotNull(ServerCacheManager.getVarp("varp.imp".asRSCM()))
            VarpSync.writeVarp(player.client, nativeStage, QuestCompleteStage)

            delay(1)
            access.spotanimMap(worldRepo, EnchantSpotanim, TableCoord, height = 100)
            access.soundSynth("synth.mizgog_beads")

            delay(8)
            fadeToBlack()

            delay(1)
            giveAmulet(state)
            access.camReset()
            fadeFromBlack()

            delay(2)
            access.closeFadeOverlayNow()
            finishQuest(state)
        } catch (cancelled: Throwable) {
            // Cancellation resumes with an exception; non-suspending recovery must finish the committed hand-in.
            access.camReset()
            access.closeFadeOverlayNow()
            if (state.beadsTaken) {
                giveAmulet(state)
                finishQuest(state)
            }
            throw cancelled
        } finally {
            access.camReset()
            access.closeFadeOverlayNow()
            player.resyncVar("varp.imp")
        }
    }

    private class HandIn {
        var beadsTaken: Boolean = false
        var amuletGiven: Boolean = false
        var completed: Boolean = false
    }

    private fun Dialogue.giveAmulet(state: HandIn) {
        if (state.amuletGiven) {
            return
        }
        state.amuletGiven = true
        access.mes("The wizard hands you an amulet.")
        access.invAdd(access.inv, AmuletOfAccuracy)
    }

    private fun Dialogue.finishQuest(state: HandIn) {
        if (state.completed) {
            return
        }
        state.completed = true
        quest.completeQuest(access)
    }

    private suspend fun Dialogue.dialogAfterQuest(npc: Npc) {
        when (
            choice3(
                "Got any more quests?",
                1,
                "Do you know any interesting spells you could teach me?",
                2,
                "Have you got another one of those fancy schmancy amulets?",
                3,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Got any more quests?")
                chatNpc(neutral, "No, everything is good with the world today.")
            }
            2 -> {
                chatPlayer(neutral, "Do you know any interesting spells you could teach me?")
                chatNpc(
                    laugh,
                    "I don't think so, the type of magic I study involves years of meditation " +
                        "and research.",
                )
            }
            3 -> {
                chatPlayer(quiz, "Have you got another one of those fancy schmancy amulets?")
                chatNpc(
                    neutral,
                    "I have a few spare. I'd like one of each coloured bead again in return, " +
                        "though! Black, white, yellow and red.",
                )

                if (!hasAllBeads(player)) {
                    chatPlayer(
                        quiz,
                        "I don't have them all on me at the moment. I'll come back when I have them!",
                    )
                    chatNpc(neutral, "Very well. See you soon!")
                    return
                }

                val hand = choice2("I have them with me!", true, "Maybe later.", false)
                if (!hand) {
                    chatPlayer(neutral, "Maybe later.")
                    chatNpc(neutral, "Perhaps some other time, then.")
                    return
                }

                chatPlayer(happy, "I have them with me! Here you go.")
                exchangeBeads()
                chatPlayer(happy, "Thanks, Mizgog!")
                chatPlayer(quiz, "What are you going to do with all of these extra beads, anyway?")
                chatNpc(shifty, "You don't want to know. Take care!")
            }
        }
    }

    private suspend fun ProtectedAccess.purchaseAmulet(npc: Npc) {
        startDialogue(npc) {
            if (!quest.isQuestCompleted(player)) {
                mizgogDialogue(npc)
                return@startDialogue
            }
            if (!hasAllBeads(player)) {
                chatNpc(
                    quiz,
                    "You don't seem to have one of each coloured bead on you at the moment. " +
                        "Return when you have a black, white, yellow and red in your backpack!",
                )
                return@startDialogue
            }
            exchangeBeads()
        }
    }

    private suspend fun Dialogue.exchangeBeads() {
        if (!deleteBeads()) {
            access.mes("You need one of each coloured bead.")
            return
        }
        access.invAdd(access.inv, AmuletOfAccuracy)
        mesbox(
            "Mizgog removes the beads from your backpack and gives you another Amulet of accuracy."
        )
    }

    private fun hasAllBeads(player: Player): Boolean = Beads.all { player.inv.count(it) > 0 }

    private fun Dialogue.deleteBeads(): Boolean =
        player.invTransaction(access.inv) {
            val inventory = select(access.inv)
            for (bead in Beads) {
                delete {
                    from = inventory
                    obj = bead.asRSCM()
                    strictCount = 1
                }
            }
        }.success

    private fun Dialogue.fadeToBlack() {
        access.fadeOverlay(
            startColour = 0,
            startTransparency = 255,
            endColour = 0,
            endTransparency = 0,
            clientDuration = FadeOutDuration,
        )
    }

    private fun Dialogue.fadeFromBlack() {
        access.fadeOverlay(
            startColour = 0,
            startTransparency = 0,
            endColour = 0,
            endTransparency = 255,
            clientDuration = FadeInDuration,
        )
    }

    private companion object {
        private val Beads =
            listOf("obj.black_bead", "obj.red_bead", "obj.white_bead", "obj.yellow_bead")

        private const val AmuletOfAccuracy = "obj.amulet_of_accuracy"
        private const val EnchantSeq = "seq.qip_imp_catcher_wizard"
        private const val EnchantSpotanim = "spotanim.qip_imp_catcher_spotanim"

        private const val MagicXpReward = 875.0

        private const val QuestCompleteStage = 2

        private val TableCoord = CoordGrid(3102, 3163, 2)
        private val CamLookAt = CoordGrid(3103, 3162, 2)
        private val CamMoveTo = CoordGrid(3103, 3161, 2)

        private const val CutsceneJingle = "jingle.waterlogged"

        private const val FadeOutDuration = 15
        private const val FadeInDuration = 50
    }
}
