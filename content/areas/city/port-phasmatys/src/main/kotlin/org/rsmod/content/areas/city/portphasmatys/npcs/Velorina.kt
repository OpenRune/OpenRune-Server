package org.rsmod.content.areas.city.portphasmatys.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ghosts_Ahoy (starting off)
// https://oldschool.runescape.wiki/w/Transcript:Velorina
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:ghosts_ahoy] quest stages
class Velorina @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ahoy_velorina") { startDialogue(it.npc) { velorina() } }
    }

    private suspend fun Dialogue.velorina() {
        if (!player.understandsGhosts()) {
            return wail()
        }
        if (QuestRequirements.hasCompleted(player, GHOSTS_AHOY)) {
            return afterQuest()
        }
        chatNpc(
            sad,
            "Take pity on me, please - eternity stretches out before me and I am helpless in its " +
                "grasp.",
        )
        val option =
            choice2(
                "Why, what is the matter?",
                1,
                "Sorry, I'm scared of ghosts.",
                2,
                title = "Select an option",
            )
        if (option == 2) {
            chatPlayer(worried, "Leave me alone - I'm scared of ghosts!!!")
            return
        }
        chatPlayer(quiz, "Why, what is the matter?")
        chatNpc(sad, "Oh, I'm sorry - I was just wailing out loud. I didn't mean to scare you.")
        chatPlayer(
            neutral,
            "No, that's okay - it takes more than a ghost to scare me. What is wrong?",
        )
        chatNpc(quiz, "Do you know the history of our town?")
        val history =
            choice2(
                "Yes, I do. It is a very sad story.",
                1,
                "No - could you tell me?",
                2,
                title = "Select an option",
            )
        if (history == 1) {
            chatPlayer(sad, "Yes, I do. It is a very sad story.")
        } else {
            chatPlayer(quiz, "No - could you tell me?")
            phasmatysHistory()
            chatPlayer(sad, "That's a very sad story.")
        }
        chatNpc(quiz, "Would you help us obtain our release into the next world?")
        // TODO(content): [quest:ghosts_ahoy] "Start the Ghosts Ahoy quest?" Yes/No choice
    }

    private suspend fun Dialogue.afterQuest() {
        val option =
            choice2(
                "I thought you were going to pass over to the next world.",
                1,
                "Can I have another Ectophial?",
                2,
            )
        if (option == 1) {
            chatPlayer(quiz, "I thought you were going to pass over to the next world.")
            chatNpc(
                happy,
                "All in good time, ${player.displayName}. We stand forever in your debt, and " +
                    "will certainly put in a good word for you when we pass over.",
            )
            return
        }
        chatPlayer(quiz, "Can I have another Ectophial?")
        // TODO(content): [verify:in_game] whether an Ectophial in the bank counts as having one
        if (ECTOPHIAL in player.inv || ECTOPHIAL_EMPTY in player.inv) {
            chatNpc(quiz, "Really? You've already got an Ectophial.")
            val again =
                choice2(
                    "I know, but can I have another one anyway?",
                    1,
                    "Have I? Okay, forget it.",
                    2,
                )
            if (again == 2) {
                chatPlayer(confused, "Have I? Okay, forget it.")
                return
            }
            chatPlayer(neutral, "I know, but can I have another one anyway?")
        }
        chatNpc(
            happy,
            "Of course you can, you have helped us more than we could ever have hoped.",
        )
        player.invAddOrDrop(objRepo, ECTOPHIAL)
    }

    private companion object {
        const val GHOSTS_AHOY = "quest_ghostsahoy"
        const val ECTOPHIAL = "obj.ectophial"
        const val ECTOPHIAL_EMPTY = "obj.ectophial_empty"
    }
}
