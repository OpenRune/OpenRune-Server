package org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LighthouseGapNpcs @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.horror_girlfriend_prequest") { startDialogue(it.npc) { larrissa() } }
        onOpNpc1("npc.horror_lighthousekeeeper_well") { startDialogue(it.npc) { jossik(it.npc) } }
        onOpNpc3("npc.horror_lighthousekeeeper_well") { player.openLighthouseStore(it.npc) }
    }

    private fun Player.openLighthouseStore(npc: Npc) {
        shops.open(this, npc, "The Lighthouse Store", "inv.lighthouseshop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Larrissa (before Horror from the Deep)
    // TODO(content): [quest:horror_from_the_deep] the quest start offer and the quest dialogue
    private suspend fun Dialogue.larrissa() {
        chatNpc(worried, "Oh, thank Armadyl! I am in such a worry... please help me!")
        when (choice2("With what?", 1, "Sorry, just passing through", 2)) {
            1 -> withWhat()
            2 -> passingThrough()
        }
    }

    private suspend fun Dialogue.withWhat() {
        chatPlayer(quiz, "With what?")
        chatNpc(
            sad,
            "Oh... it is terrible... horrible... My boyfriend lives here in this lighthouse, but " +
                "I haven't seen him the last few days! I think something terrible has happened!",
        )
        chatNpc(
            worried,
            "Look, you can see for yourself that the light has gone out, and the front door is " +
                "locked up tight! He would NEVER do that!",
        )
        chatNpc(
            worried,
            "With the light off this coastline is terribly dangerous to ships! And to lock the " +
                "front door so that nobody can turn the light back on?",
        )
        chatPlayer(
            neutral,
            "Maybe he just went on holiday or something? Must be pretty boring living in a " +
                "lighthouse.",
        )
        chatNpc(
            angry,
            "That is terribly irresponsible! He is far too thoughtful for that! He would never " +
                "leave it unattended! He would also never leave without telling me!",
        )
        chatNpc(
            sad,
            "Please, I know something terrible has happened to him... I can sense it! Please... " +
                "please help me adventurer!",
        )
        when (choice2("But how can I help?", 1, "Sorry, just passing through", 2)) {
            1 -> {
                chatPlayer(quiz, "But how can I help?")
                chatNpc(
                    worried,
                    "Well, we have to do something to get the lighthouse working again! Also, " +
                        "as you may have noticed, the storm that knocked the bridge out",
                )
                chatNpc(
                    worried,
                    "has trapped me on this causeway! You seem to have got here okay somehow, so " +
                        "if you could go and visit my cousin and get the spare key I left him,",
                )
                chatNpc(
                    worried,
                    "as well as fix the bridge enough so that I can go and speak to my family in " +
                        "Rellekka and tell them whats happened, I will be eternally grateful!",
                )
            }
            2 -> passingThrough()
        }
    }

    private suspend fun Dialogue.passingThrough() {
        chatPlayer(neutral, "Sorry, just passing through.")
        chatNpc(
            sad,
            "Oh... my poor darling Jossik... I just know something terrible has happened...",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jossik
    // TODO(content): [quest:horror_from_the_deep] the rusty casket dialogue on the first talk
    // after the quest
    // TODO(content): [system:prayer_books] Jossik's salvaged prayerbooks
    private suspend fun Dialogue.jossik(npc: Npc) {
        chatNpc(happy, "Hello again, adventurer. What brings you this way?")
        val option =
            choice3(
                "Can I see your wares?",
                1,
                "Have you found any more prayerbooks?",
                2,
                "Nothing, really. Good bye.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can I see your wares?")
                chatNpc(happy, "Sure thing! I think you'll find my prices are remarkable!")
                player.openLighthouseStore(npc)
            }
            2 -> {
                chatPlayer(quiz, "Have you found any more prayerbooks?")
                chatNpc(
                    happy,
                    "It's amazing how things like that keep washing up on the shore in caskets. " +
                        "Take a look at what I've got...",
                )
            }
            3 -> chatPlayer(neutral, "Nothing, really. Good bye.")
        }
    }
}
