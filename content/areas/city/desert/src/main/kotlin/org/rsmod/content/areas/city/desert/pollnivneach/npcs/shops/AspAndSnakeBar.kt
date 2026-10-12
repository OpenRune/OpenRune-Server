package org.rsmod.content.areas.city.desert.pollnivneach.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ali_the_Barman
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_feud] "I'm looking for Traitorous Ali." and "I'm looking for Ali from
//  Pollnivneach." during the quest
class AspAndSnakeBar @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BARMAN) { startDialogue(it.npc) { barman(it.npc) } }
        onOpNpc3(BARMAN) { player.openBar(it.npc) }
    }

    private fun Player.openBar(npc: Npc) {
        shops.open(this, npc, "The Asp & Snake Bar.", "inv.feud_alispub")
    }

    private suspend fun Dialogue.barman(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Good day. Can I help you with anything?")
        while (true) {
            val option =
                choice3(
                    "Yes, I'd like a drink please.",
                    1,
                    "What's going on in town?",
                    2,
                    "The 'Asp and Snake'? What a strange name for a bar.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Yes, I'd like a drink please.")
                    player.openBar(npc)
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What's going on in town?")
                    chatNpc(
                        neutral,
                        "Nothing much really. About what you'd expect from a sleepy little town " +
                            "like ours.",
                    )
                    chatPlayer(happy, "What about those two gangs? That seems exciting.")
                    chatNpc(
                        shifty,
                        "They're paying customers, so I won't rock the boat if you don't mind.",
                    )
                }
                3 -> {
                    chatPlayer(confused, "The 'Asp and Snake'? What a strange name for a bar.")
                    chatNpc(
                        neutral,
                        "Maybe around these parts. Were you expecting it to have 'Ali' in the name?",
                    )
                    chatNpc(
                        neutral,
                        "I did think about calling it 'Ali's', but then it'd get confused with " +
                            "the Camel Shop, the Kebab House, the Mayor's residence...",
                    )
                    chatPlayer(happy, "Yes, I get your point. It's a perfectly fine name.")
                }
            }
            chatNpc(happy, "Is there anything else I can help you with?")
            val more =
                choice2(
                    "No thanks, I'm ok.",
                    false,
                    "Yes, I'd like to talk about something else.",
                    true,
                )
            if (!more) {
                chatPlayer(happy, "No thanks, I'm ok. Thanks for your time.")
                return
            }
        }
    }

    private companion object {
        const val BARMAN = "npc.feud_ali_the_barman_multi"
    }
}
