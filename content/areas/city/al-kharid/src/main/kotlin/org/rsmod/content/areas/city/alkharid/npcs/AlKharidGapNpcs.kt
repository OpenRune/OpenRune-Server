package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class AlKharidGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.borderguard2") { startDialogue(it.npc) { borderGuard() } }
        onOpNpc1("npc.tempoross_pass_guard") { startDialogue(it.npc) { shantayGateGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Border_Guard (before Prince Ali Rescue)
    // TODO(content): [system:toll_gate] paying 10 coins and walking through the gate
    // TODO(content): [quest:prince_ali_rescue] free passage after the quest
    private suspend fun Dialogue.borderGuard() {
        chatPlayer(quiz, "Can I come through this gate?")
        chatNpc(neutral, "You must pay a toll of 10 gold coins to pass.")
        val option =
            choice3(
                "No thank you, I'll walk around.",
                1,
                "Who does my money go to?",
                2,
                "Yes, ok.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "No, thank you. I'll walk around.")
                chatNpc(neutral, "Ok suit yourself.")
            }
            2 -> {
                chatPlayer(quiz, "Who does my money go to?")
                chatNpc(neutral, "The money goes to the city of Al-Kharid.")
            }
            3 -> {
                chatPlayer(neutral, "Yes, ok.")
                if (access.invTotal(player.inv, "obj.coins") < TOLL) {
                    chatPlayer(sad, "Oh dear, I don't actually seem to have enough money.")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shantay_Guard (guard at the gate)
    // TODO(content): [verify:in_game] Trade and Buy-pass ops on the gate guard
    private suspend fun Dialogue.shantayGateGuard() {
        chatNpc(happy, "Hello there! What can I do for you?")
        val desert =
            choice2("I'd like to go into the desert please.", true, "Nothing thanks.", false)
        if (desert) {
            chatPlayer(quiz, "I'd like to go into the desert please.")
            chatNpc(
                neutral,
                "You'll need a Shantay pass to go through the gate into the desert. See Shantay, " +
                    "he'll sell you one for a very reasonable price.",
            )
            return
        }
        chatPlayer(neutral, "Nothing thanks.")
        chatNpc(happy, "Okay then, have a nice day.")
    }

    private companion object {
        const val TOLL = 10
    }
}
