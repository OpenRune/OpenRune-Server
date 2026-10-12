package org.rsmod.content.areas.misc.villages.ghorrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GhorrockDungeonSoldiers : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sotn_khazard_head_soldier_vis") { startDialogue(it.npc) { malleta() } }
        onOpNpc1("npc.sotn_khazard_muspah_soldier_vis") { startDialogue(it.npc) { raymond() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Malleta
    // TODO(content): [system:met_flags] "You again?" after the first talk
    // TODO(content): [quest:desert_treasure_ii] "I already told you to stop bothering us." after
    // the quest
    private suspend fun Dialogue.malleta() {
        chatNpc(angry, "Adventurer. The General warned us about you.")
        chatPlayer(quiz, "Warned?")
        chatNpc(
            angry,
            "Yes. He said you might show up here again. We're not to harm you, but make no " +
                "mistake, we're keeping a close eye on you. You're not to be trusted.",
        )
        chatPlayer(
            neutral,
            "Well I guess that's the best I can hope for. So how are things going here?",
        )
        chatNpc(angry, "We are not here to make small talk. Leave us to our work.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sergeant_Raymond
    // TODO(content): [system:met_flags] "Be on your guard, adventurer." after the first talk
    private suspend fun Dialogue.raymond() {
        chatNpc(
            worried,
            "Adventurer, if you're looking to head back in there, I'd suggest you reconsider.",
        )
        chatPlayer(quiz, "Why?")
        chatNpc(
            worried,
            "There is a horror within that has already claimed the lives of some of our soldiers.",
        )
        chatPlayer(quiz, "Horror? What horror?")
        chatNpc(
            worried,
            "A nightmare that should not exist. A phantom of the beast that Jhallan took the " +
                "form of. General Khazard called it a muspah.",
        )
        chatPlayer(quiz, "A phantom? So it's not actually real?")
        chatNpc(
            neutral,
            "It is, and it isn't. Truth be told, I don't fully understand it myself. General " +
                "Khazard called it a remnant of dark energy. Raw magic released from the prison " +
                "that has taken form.",
        )
        chatNpc(
            worried,
            "All I know is that whatever it is, real or otherwise, it's very dangerous.",
        )
        chatPlayer(quiz, "So I can't go in?")
        chatNpc(
            neutral,
            "I won't stop you, but if it all goes wrong, don't expect me to come and save you " +
                "either.",
        )
        chatPlayer(neutral, "I'll keep that in mind. Thanks for the information.")
    }
}
