package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Smoggy
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Smoggy : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lovakengj_charcoal_keeper") { startDialogue(it.npc) { smoggy() } }
    }

    private suspend fun Dialogue.smoggy() {
        chatNpc(quiz, "Are you here to make juniper charcoal?")
        while (true) {
            val option =
                choice5(
                    "Tell me about juniper charcoal.",
                    1,
                    "Tell me about yourself.",
                    2,
                    "What can you do for me?",
                    3,
                    "Have you got a spare tinderbox?",
                    4,
                    "I'll be off.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about juniper charcoal.")
                    chatNpc(
                        neutral,
                        "They use it in the blast mine, out near Lovakengj. It's powerful stuff, " +
                            "more volatile than the regular charcoal you get elsewhere.",
                    )
                    chatNpc(
                        neutral,
                        "To make juniper charcoal, put some juniper logs in my furnaces, then " +
                            "set them alight. I'll keep an eye on it for a fee of 600 coins.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(
                        happy,
                        "Juniper charcoal is my trade, always has been, ever since my mum taught " +
                            "me. She's retired now, and returned to Lovakengj - that's where " +
                            "we're from.",
                    )
                    chatNpc(
                        neutral,
                        "I just live out here in the Hosidius farmlands because it's handy for " +
                            "the juniper trees.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What can you do for me?")
                    chatNpc(
                        happy,
                        "For a small fee, I watch the charcoal furnace so that you can make " +
                            "charcoal here. Also, I do a great back- rub, if you're ever in the " +
                            "mood.",
                    )
                }
                4 -> spareTinderbox()
                else -> {
                    chatPlayer(neutral, "I'll be off.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.spareTinderbox() {
        chatPlayer(quiz, "Have you got a spare tinderbox?")
        if (player.inv.count(TINDERBOX) > 0) {
            chatNpc(neutral, "Yup, but I'll keep it for someone who needs it.")
            return
        }
        if (player.inv.isFull()) {
            chatNpc(neutral, "Yup, but you can't hold it.")
            return
        }
        chatNpc(happy, "Sure, you can have it.")
        player.invAdd(player.inv, TINDERBOX)
    }

    private companion object {
        const val TINDERBOX = "obj.tinderbox"
    }
}
