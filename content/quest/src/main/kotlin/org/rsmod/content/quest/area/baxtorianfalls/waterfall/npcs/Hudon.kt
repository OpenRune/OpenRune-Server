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
        val stage = waterfall.stage(player)
        if (stage != WaterfallQuest.Complete &&
            (stage == 0 || !WaterfallCoords.onHudonIsland(player.coords))
        ) {
            mesbox(
                "Hudon can't hear you because of the noise of the waterfall. Perhaps the " +
                    "acoustics would be better from that island?"
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
            chatNpc(angry, "You stole my treasure. I saw you!")
            chatPlayer(happy, "I'll make sure it goes to a good cause.")
            chatNpc(angry, "Hmmmm!")
        }
        stage == Started -> hudonFirstMeeting(waterfall)
        stage == MetHudon -> {
            chatPlayer(quiz, "So you're still here.")
            chatNpc(happy, "I'll find that treasure soon, just you wait and see.")
        }
        stage == ReadBook -> {
            chatPlayer(happy, "Hello Hudon.")
            chatNpc(angry, "Oh it's you, trying to find my treasure again are you?")
            chatPlayer(confused, "I didn't know it belonged to you.")
            chatNpc(
                angry,
                "It will do when I find it. I just need to get into this blasted waterfall I've " +
                    "been washed downstream three times already.",
            )
        }
        stage == EnteredTomb -> {
            chatPlayer(happy, "Hello again.")
            chatNpc(angry, "Not you still, why don't you give up?")
            chatPlayer(laugh, "And miss all the fun!")
            chatNpc(angry, "You do understand that anything you find you have to share with me.")
            chatPlayer(quiz, "Why's that?")
            chatNpc(angry, "Because I told you about the treasure.")
            chatPlayer(neutral, "Well, I wouldn't count on it.")
            chatNpc(sad, "That's not fair.")
            chatPlayer(neutral, "Neither is life kid.")
        }
        stage >= EnteredFalls -> {
            chatPlayer(quiz, "How are you doing, Hudon?")
            chatNpc(sad, "No luck yet I'm afraid.")
            chatPlayer(happy, "Me neither. but I don't give up easily.")
        }
    }
}

/** The first conversation, which the raft crash also leads straight into. */
internal suspend fun Dialogue.hudonFirstMeeting(waterfall: WaterfallQuest) {
    chatPlayer(worried, "Hello son, are you okay? You need help?")
    chatNpc(laugh, "It looks like you need the help.")
    chatPlayer(neutral, "Your mum sent me to find you.")
    chatNpc(angry, "Don't play nice with me, I know you're looking for the treasure too.")
    chatPlayer(quiz, "Where is this treasure you talk of?")
    chatNpc(
        angry,
        "Just because I'm small doesn't mean I'm dumb! If I told you, you would take it all " +
            "for yourself.",
    )
    chatPlayer(happy, "Maybe I could help.")
    chatNpc(angry, "I'm fine alone.")
    chatPlayer(confused, "Hmm... I wonder what this treasure is.")
    waterfall.advanceTo(access, MetHudon)
}
