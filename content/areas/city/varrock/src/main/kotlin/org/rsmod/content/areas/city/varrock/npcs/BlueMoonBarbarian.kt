package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] when the transcript's "person" form of address is used
class BlueMoonBarbarian : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.barbarianwoman_varrock") { startDialogue(it.npc) { barbarian(it.npc) } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barbarian (Blue Moon Inn Barbarian)
    private suspend fun Dialogue.barbarian(npc: Npc) {
        val address = if (access.isBodyTypeA()) "man" else "woman"
        val name = player.displayName
        chatPlayer(happy, "Hello.")
        chatNpc(laugh, "Hah! What you want with me, little city $address?")
        val option = choice3("Can I buy you a drink?", 1, "Can I fight you?", 2, "Um...", 3)
        when (option) {
            1 -> {
                chatNpc(laugh, "Ha ha ha! Little $name wants to buy me a drink, how sweet.")
                chatPlayer(quiz, "Well... how about it?")
                chatNpc(
                    neutral,
                    "You not talk like barbarian $address. You certainly not smell like " +
                        "barbarian $address! But I think I give you test anyway.",
                )
                chatPlayer(quiz, "What's the test?")
                chatNpc(
                    neutral,
                    "Back in village to the west, we barbarians scream our defiance as we fight. " +
                        "We scream 'YYEEEEEAAARRRRGGHHHH!!!' Can you do this?",
                )
                repeat(2) {
                    chatNpc(angry, BARBARIAN_YELL)
                    npc.say(BARBARIAN_YELL)
                    chatPlayer(angry, PLAYER_YELL)
                    access.say(PLAYER_YELL)
                }
                chatNpc(
                    laugh,
                    "Ha ha ha! Little $name cannot yell like barbarian. Also, I strong barbarian " +
                        "woman; I get my own drink. Run away now, little $name.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Can I fight you?")
                chatNpc(laugh, "Ha ha ha! We fight anytime you like.")
            }
            else -> {
                chatPlayer(confused, "Um...")
                chatNpc(
                    laugh,
                    "Ha ha ha! I not interested in talking to soft city $address who not know " +
                        "what he want.",
                )
            }
        }
    }

    private companion object {
        const val BARBARIAN_YELL = "YYEEEEEAAARRRRGGHHHH!!!"
        const val PLAYER_YELL = "Yyeeeeeaaarrrrgghhhh!!!"
    }
}
