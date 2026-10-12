package org.rsmod.content.areas.zeah.arceuus.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server invs for Filamina's Wares, Regath's Wares and Thyria's
//  Wares (Trade ops)
class ArceuusShops : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.arceuus_staffshop") { startDialogue(it.npc) { filamina() } }
        onOpNpc1("npc.arceuus_generalstore") { startDialogue(it.npc) { regath() } }
        onOpNpc1("npc.arceuus_runeshop") { startDialogue(it.npc) { thyria() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Filamina (favour removed, so always served)
    private suspend fun Dialogue.filamina() {
        chatNpc(neutral, "What do you require, traveller? I deal in staves.")
        seeWares()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thyria (favour removed, so always served)
    private suspend fun Dialogue.thyria() {
        chatNpc(neutral, "What do you require, traveller? I deal in runes.")
        seeWares()
    }

    private suspend fun Dialogue.seeWares() {
        val see = choice2("Let's see what you've got.", true, "I don't need anything.", false)
        if (see) {
            chatPlayer(happy, "Let's see what you've got.")
            // TODO(content): [system:shops] opens the shop
        } else {
            chatPlayer(neutral, "I don't need anything.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Regath
    // TODO(content): [system:treasure_trails] elite challenge scroll
    private suspend fun Dialogue.regath() {
        chatNpc(neutral, "What do you require, traveller? I deal in many things.")
        while (true) {
            val option =
                choice3(
                    "Let's see what you've got.",
                    1,
                    "Who are you?",
                    2,
                    "I don't need anything.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Let's see what you've got.")
                    // TODO(content): [system:shops] opens Regath's Wares
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(neutral, "My name is Regath, traveller.")
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "This place is a union of the mystical and the mundane, where the " +
                            "magical realm reaches out to touch the humdrum world of commerce.",
                    )
                    chatNpc(
                        happy,
                        "Or, in terms that you may understand more easily, this place is a shop " +
                            "that sells magic stuff for money. Though I also trade in various " +
                            "other items.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I don't need anything.")
                    return
                }
            }
        }
    }
}
