package org.rsmod.content.areas.city.portphasmatys.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ghosts_Ahoy (starting off)
// https://oldschool.runescape.wiki/w/Transcript:Velorina
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:ghosts_ahoy] quest stages and the after-quest Ectophial
class Velorina : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ahoy_velorina") { startDialogue(it.npc) { velorina() } }
    }

    private suspend fun Dialogue.velorina() {
        if (!player.understandsGhosts()) {
            return wail()
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
}
