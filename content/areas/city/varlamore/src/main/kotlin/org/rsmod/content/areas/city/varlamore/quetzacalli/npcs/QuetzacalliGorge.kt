package org.rsmod.content.areas.city.varlamore.quetzacalli.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class QuetzacalliGorge @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BARTENDER) { startDialogue(it.npc) { bartender(it.npc) } }
        onOpNpc3(BARTENDER) { openPub(it.npc) }
        onOpNpc1(SHOPKEEPER) { startDialogue(it.npc) { shopkeeper(it.npc) } }
        onOpNpc3(SHOPKEEPER) { openGeneralStore(it.npc) }
        onOpNpc1(WORKER) { startDialogue(it.npc) { worker() } }
    }

    private fun ProtectedAccess.openPub(npc: Npc) {
        shops.open(player, npc, "The Windbreaker", "inv.pub_quetzacalli")
    }

    private fun ProtectedAccess.openGeneralStore(npc: Npc) {
        shops.open(player, npc, "Quetzacalli Gorge General Store", "inv.quetzacalli_general_store")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(The_Windbreaker)
    private suspend fun Dialogue.bartender(npc: Npc) {
        chatNpc(happy, "Welcome to the Windbreaker. We've got the best drinks around.")
        while (true) {
            val option =
                choice3(
                    "Let's see what you have.",
                    1,
                    "What's with those big gates out there?",
                    2,
                    "Nothing for me, thanks.",
                    3,
                    title = "Select an option",
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Let's see what you have.")
                    return access.openPub(npc)
                }
                2 -> {
                    chatPlayer(quiz, "What's with those big gates out there?")
                    chatNpc(
                        neutral,
                        "I couldn't tell you to be honest. They've been here for a very long " +
                            "time. Apparently they came in quite useful during the war, and " +
                            "these days they do help stop any undesirable beasts wandering into " +
                            "town.",
                    )
                    chatNpc(happy, "Anyway, did you want to buy a drink?")
                }
                else -> {
                    chatPlayer(neutral, "Nothing for me, thanks.")
                    chatNpc(neutral, "As you wish.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shopkeeper_(Quetzacalli_Gorge)
    // TODO(content): [quest:the_heart_of_darkness] "I hear you can offer help to those in need."
    //  and "About the Emissaries..." during the quest
    private suspend fun Dialogue.shopkeeper(npc: Npc) {
        chatNpc(happy, "Hello there. Looking for some supplies?")
        when (choice2("Yes please.", 1, "No thanks.", 2, title = "Select an option")) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                access.openGeneralStore(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Very well.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Worker_(The_Windbreaker) (first time talking)
    // TODO(content): [system:met_flags] "Hello again. Still on break?" on later visits
    // TODO(content): [boss:the_hueycoatl] the reply for players who have fought the Hueycoatl
    private suspend fun Dialogue.worker() {
        chatPlayer(happy, "Hi there.")
        chatNpc(neutral, "Can I help you?")
        chatPlayer(quiz, "I don't know. Can you?")
        chatNpc(neutral, "Probably not. I'm on break.")
        chatPlayer(quiz, "Break from what?")
        chatNpc(
            neutral,
            "See that big mountain outside? That's the Darkfrost. There's a nice beastie up " +
                "there called the Hueycoatl.",
        )
        chatNpc(
            angry,
            "You humans don't seem to care that much, so as usual it's up to us to stop it " +
                "making a big mess of the entire area.",
        )
        chatPlayer(happy, "Well, if you're in need of help, I'm open to contributing.")
        chatNpc(
            neutral,
            "You are? Oh... Fair enough then. In that case, head on up the mountain and get " +
                "stuck in.",
        )
    }

    private companion object {
        const val BARTENDER = "npc.quetzacalli_bartender"
        const val SHOPKEEPER = "npc.quetzacalli_general_store"
        const val WORKER = "npc.quetzacalli_dwarf_worker"
    }
}
