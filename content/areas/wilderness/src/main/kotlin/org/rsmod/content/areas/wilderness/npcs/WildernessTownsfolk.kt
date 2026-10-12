package org.rsmod.content.areas.wilderness.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WildernessTownsfolk @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.rcu_zammy_mage1_edgeb") { startDialogue(it.npc) { mageOfZamorak(it.npc) } }
        onOpNpc1("npc.wilderness_resource_guardian") { startDialogue(it.npc) { mandrith() } }
        onOpNpc1("npc.wilderness_resource_certer") { startDialogue(it.npc) { piles() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mage_of_Zamorak (Wilderness)
    // TODO(content): [miniquest:enter_the_abyss] the Abyss teleport ("Veniens! Sallakar!
    //  Rinnesset!") and the darkruneshop_uber stock after the miniquest
    // TODO(content): [system:god_items] the replies to players wearing Saradominist or
    //  Guthixian items
    // TODO(content): [system:treasure_trails] master clue step
    private suspend fun Dialogue.mageOfZamorak(npc: Npc) {
        chatNpc(
            neutral,
            "This isn't the place to talk. Visit me in Varrock's Chaos Temple if you have " +
                "something to discuss. Unless you're here to teleport or buy something?",
        )
        val option =
            choice3(
                "Let's see what you're selling.",
                1,
                "Could you teleport me to the Abyss?",
                2,
                "Alright, I'll go.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "Let's see what you're selling.")
                shops.open(player, npc, "Battle Runes", "inv.darkruneshop_crap")
            }
            2 -> chatPlayer(quiz, "Could you teleport me to the Abyss?")
            3 -> chatPlayer(neutral, "Alright, I'll go.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mandrith
    // TODO(content): [system:treasure_trails] elite and master clue steps
    private suspend fun Dialogue.mandrith() {
        chatPlayer(quiz, "Who are you and what is this place?")
        chatNpc(neutral, "My name is Mandrith.")
        chatNpc(
            shifty,
            "I collect valuable resources and pawn off access to them to foolish adventurers, " +
                "like yourself.",
        )
        chatNpc(
            neutral,
            "You should take a look inside my arena. There's an abundance of valuable resources " +
                "inside.",
        )
        chatPlayer(quiz, "And I can take whatever I want?")
        chatNpc(neutral, "It's all yours. All I ask is you pay the upfront fee.")
        chatPlayer(worried, "Will others be able to kill me inside?")
        chatNpc(neutral, "Yes. These walls will only hold them back for so long.")
        chatPlayer(worried, "You'll stop them though, right?")
        chatNpc(
            laugh,
            "Haha! For the right price, I won't deny any one access to my arena. Even if their " +
                "intention is to kill you.",
        )
        chatPlayer(confused, "Right...")
        chatNpc(
            neutral,
            "My arena holds many treasures that I've acquired at great expense, adventurer. " +
                "Their bounty can come at a price.",
        )
        chatNpc(
            neutral,
            "One day, adventurer, I will boast ownership of a much larger, much more dangerous " +
                "arena than this. Take advantage of this offer while it lasts.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Piles
    // TODO(content): [system:item_noting] noting items used on Piles for 50 coins each
    // TODO(content): [system:treasure_trails] master clue step
    private suspend fun Dialogue.piles() {
        chatNpc(
            neutral,
            "Hello. I can convert items to banknotes, for 50 coins per item. Just hand me the " +
                "items you'd like me to convert.",
        )
        when (choice2("Who are you?", 1, "Thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I'm Piles. I lived in Draynor Village when I was young, where I saw three " +
                        "men working in the market, converting items to banknotes.",
                )
                chatNpc(
                    happy,
                    "Their names were Niles, Miles and Giles. I'm trying to be like them, so " +
                        "I've changed my name and started this business here.",
                )
                chatPlayer(happy, "Thanks.")
            }
            2 -> chatPlayer(happy, "Thanks.")
        }
    }
}
