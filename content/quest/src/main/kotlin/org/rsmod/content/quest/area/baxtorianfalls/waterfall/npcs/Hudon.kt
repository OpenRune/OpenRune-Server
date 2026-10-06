package org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallCoords
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.EnteredFalls
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.EnteredTomb
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.HudonNpc
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MetHudon
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ReadBook
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Started
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Hudon stands on a rock in the river, cut off from the island the raft runs aground on, so he
 * is talked to across the water. From the riverbank the falls drown out the conversation.
 */
class Hudon @Inject constructor(private val waterfall: WaterfallQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onApNpc1(HudonNpc) { apTalk(it.npc) }
        onOpNpc1(HudonNpc) { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.apTalk(npc: Npc) {
        if (WaterfallCoords.onHudonIsland(player.coords) && !isWithinApRange(npc, TalkRange)) {
            return
        }
        talk(npc)
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        if (!WaterfallCoords.onHudonIsland(player.coords)) {
            mesbox(
                "Hudon can't hear a word over the roar of the waterfall. You might have more " +
                    "luck from the island he's standing beside."
            )
            return
        }
        startDialogue(npc) { hudon(waterfall) }
    }

    private companion object {
        const val TalkRange = 5
    }
}

internal suspend fun Dialogue.hudon(waterfall: WaterfallQuest) {
    val stage = waterfall.stage(player)
    when {
        waterfall.isComplete(player) -> {
            chatPlayer(happy, "Hello again.")
            chatNpc(angry, "You took my treasure! I saw you do it!")
            chatPlayer(happy, "Don't worry, I'll put it to good use.")
            chatNpc(angry, "Hmph!")
        }
        stage == 0 -> {
            chatPlayer(happy, "Hello there.")
            chatNpc(angry, "Go away, I'm busy.")
        }
        stage == Started -> hudonFirstMeeting(waterfall)
        stage == MetHudon -> {
            chatPlayer(quiz, "Still out here then?")
            chatNpc(happy, "I'll find that treasure any day now, you'll see.")
        }
        stage == ReadBook -> {
            chatPlayer(happy, "Hello Hudon.")
            chatNpc(angry, "You again. Still after my treasure?")
            chatPlayer(confused, "I didn't realise it was yours.")
            chatNpc(
                angry,
                "It will be once I find it. I just need to get inside that stupid waterfall. " +
                    "It's already swept me downstream three times.",
            )
        }
        stage == EnteredTomb -> {
            chatPlayer(happy, "Hello again.")
            chatNpc(angry, "Haven't you given up yet?")
            chatPlayer(laugh, "And miss out on all the fun?")
            chatNpc(angry, "Anything you find, you have to share with me.")
            chatPlayer(quiz, "And why is that?")
            chatNpc(angry, "Because I'm the one who told you about it!")
            chatPlayer(neutral, "I wouldn't get your hopes up.")
            chatNpc(sad, "That's not fair.")
            chatPlayer(neutral, "Life isn't fair, kid.")
        }
        stage >= EnteredFalls -> {
            chatPlayer(quiz, "How's it going, Hudon?")
            chatNpc(sad, "Nothing yet.")
            chatPlayer(happy, "Me neither, but I'm not one to give up.")
        }
    }
}

/** The first conversation, which the raft crash also leads straight into. */
internal suspend fun Dialogue.hudonFirstMeeting(waterfall: WaterfallQuest) {
    chatPlayer(worried, "Are you alright, lad? Do you need a hand?")
    chatNpc(laugh, "Looks to me like you're the one who needs a hand.")
    chatPlayer(neutral, "Your mother asked me to come and find you.")
    chatNpc(angry, "Don't pretend to be nice. You're after the treasure as well, I can tell.")
    chatPlayer(quiz, "What treasure is that?")
    chatNpc(angry, "I might be small, but I'm not stupid! If I told you, you'd keep it all.")
    chatPlayer(happy, "Perhaps I could help you look.")
    chatNpc(angry, "I don't need any help.")
    chatPlayer(confused, "Hmm... I wonder what this treasure could be.")
    waterfall.advanceTo(access, MetHudon)
}
