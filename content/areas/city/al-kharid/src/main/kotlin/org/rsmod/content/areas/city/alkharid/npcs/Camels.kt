package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:enakhras_lament] camulet dialogue and ugthanki dung for every camel
class Camels : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.camel") { startDialogue(it.npc) { camel() } }
        for (camel in NAMED_CAMELS) {
            onOpNpc1(camel) { startDialogue(it.npc) { namedCamel() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Camel
    private suspend fun Dialogue.camel() {
        if (!askCamel()) {
            return
        }
        val reaction =
            access.random.pick(
                "The camel tries to stamp on your foot, but you pull it back quickly.",
                "The camel turns its head and glares at you.",
                "The camel spits at you, and you jump back hurriedly.",
            )
        mesbox(reaction)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Al_the_Camel
    // https://oldschool.runescape.wiki/w/Transcript:Alice_the_Camel
    // https://oldschool.runescape.wiki/w/Transcript:Cam_the_Camel
    // https://oldschool.runescape.wiki/w/Transcript:Elly_the_Camel
    // https://oldschool.runescape.wiki/w/Transcript:Ollie_the_Camel
    private suspend fun Dialogue.namedCamel() {
        askCamel()
    }

    private suspend fun Dialogue.askCamel(): Boolean {
        val option =
            choice3(
                "Ask the camel about its dung.",
                1,
                "Say something unpleasant.",
                2,
                "Neither - I'm a polite person.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    quiz,
                    "I'm sorry to bother you, but could you spare me a little dung?",
                )
                mesbox("The camel didn't seem to appreciate that question.")
            }
            2 -> {
                val insult =
                    access.random.pick(
                        "Mmm... looks like that camel would make a nice kebab.",
                        "If I go near that camel, it'll probably bite my hand off.",
                        "I wonder if that camel has fleas...",
                    )
                chatPlayer(shifty, insult)
                return true
            }
        }
        return false
    }

    private companion object {
        val NAMED_CAMELS =
            listOf(
                "npc.al_the_camel",
                "npc.alice_the_camel",
                "npc.cam_the_camel",
                "npc.elly_the_camel",
                "npc.ollie_the_camel",
            )
    }
}
