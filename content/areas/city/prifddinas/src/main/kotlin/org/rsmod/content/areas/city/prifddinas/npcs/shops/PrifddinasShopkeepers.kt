package org.rsmod.content.areas.city.prifddinas.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no inv with these shops' stock exists in the cache data, so
// Trade and the "opens the shop" branches do nothing yet
class PrifddinasShopkeepers : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.prif_runestore") { startDialogue(it.npc) { efa() } }
        onOpNpc1("npc.prif_armourstore") { startDialogue(it.npc) { aneirin() } }
        onOpNpc1("npc.prif_farmingstore") { startDialogue(it.npc) { branwen() } }
        onOpNpc1("npc.prif_staffstore") { startDialogue(it.npc) { elgan() } }
        onOpNpc1("npc.prif_dyestore") { startDialogue(it.npc) { guinevere() } }
        onOpNpc1("npc.prif_miningstore") { startDialogue(it.npc) { gwyn() } }
        onOpNpc1("npc.prif_lliann") { startDialogue(it.npc) { lliann() } }
        onOpNpc1("npc.prif_generalstore") { startDialogue(it.npc) { gwallter() } }
        onOpNpc1("npc.prif_herbstore") { startDialogue(it.npc) { llio() } }
        onOpNpc1("npc.prif_seamstress") { startDialogue(it.npc) { nia() } }
        onOpNpc1("npc.prif_rangestore") { startDialogue(it.npc) { sian() } }
        onOpNpc1("npc.prif_spice") { startDialogue(it.npc) { caerwyn() } }
        onOpNpc1("npc.prif_gem") { startDialogue(it.npc) { glenda() } }
        onOpNpc1("npc.prif_silver") { startDialogue(it.npc) { osian() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Efa
    private suspend fun Dialogue.efa() {
        chatNpc(happy, "Welcome to my store. Are you in search of any runes?")
        when (choice2("Can I see your wares?", 1, "No thanks.", 2)) {
            1 -> chatPlayer(quiz, "Can I see your wares?")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(happy, "Come back any time. I've always got something for anybody.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aneirin
    // TODO(content): [system:item_repair] Repair option and using items on Aneirin
    private suspend fun Dialogue.aneirin() {
        chatNpc(happy, "You look like somebody who could use a bit of armour. What do you say?")
        when (choice2("I'll take a look.", 1, "No thanks.", 2)) {
            1 -> chatPlayer(happy, "I'll take a look.")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(
                    neutral,
                    "Maybe I overestimated you. I suppose such fine equipment isn't for everyone.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Branwen
    private suspend fun Dialogue.branwen() {
        chatNpc(happy, "Can I help you with anything today?")
        when (choice2("What are you selling?", 1, "No thanks.", 2)) {
            1 -> chatPlayer(quiz, "What are you selling?")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(happy, "Okay, well let me know if I can.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elgan
    private suspend fun Dialogue.elgan() {
        chatNpc(happy, "Welcome, adventurer. Are you in need of a staff?")
        when (choice2("What do you have?", 1, "No thanks.", 2)) {
            1 -> chatPlayer(quiz, "What do you have?")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(happy, "Don't be a stranger. Come on by any time you need anything.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guinevere
    private suspend fun Dialogue.guinevere() {
        chatNpc(
            happy,
            "You look like you could use a little colour in your life! Can I interest you in " +
                "any dyes?",
        )
        when (choice2("I could take a look.", 1, "No thanks.", 2)) {
            1 -> chatPlayer(happy, "I could take a look.")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(happy, "Well, don't be afraid to come back any time!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gwyn
    private suspend fun Dialogue.gwyn() {
        chatNpc(
            happy,
            "Looking for a new pick? I've got the best you'll find around these parts!",
        )
        when (choice2("Sure.", 1, "I'm okay, thanks.", 2)) {
            1 -> chatPlayer(happy, "Sure.")
            2 -> {
                chatPlayer(neutral, "I'm okay, thanks.")
                chatNpc(happy, "Well, let everyone know! You can always find me here.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lliann
    private suspend fun Dialogue.lliann() {
        chatNpc(
            happy,
            "You look like someone that could use an overly expensive crown. Finest clothes " +
                "around! Want to take a look?",
        )
        when (choice2("Yes, please.", 1, "No thanks.", 2)) {
            1 -> chatPlayer(happy, "Yes, please.")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Your loss, I suppose.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gwallter
    private suspend fun Dialogue.gwallter() {
        chatNpc(happy, "Hi, can I help you with anything today?")
        when (choice2("Yes, please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> chatPlayer(quiz, "Yes, please. What are you selling?")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(happy, "Have a nice day.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Llio
    private suspend fun Dialogue.llio() {
        chatNpc(
            happy,
            "Can I interest you in any herbal equipment? Any supplies, perhaps?",
        )
        when (choice2("Let's see what you've got.", 1, "No thanks.", 2)) {
            1 -> chatPlayer(happy, "Let's see what you've got.")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(
                    happy,
                    "Well whenever you're looking to mix a potion or two, I've got what you need!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nia
    // TODO(content): [system:fur_clothing] Fur Clothing option and "Could you make something for
    // me?"
    private suspend fun Dialogue.nia() {
        chatNpc(happy, "You look like someone who knows how to dress up! How can I help ya?")
        val option =
            choice3(
                "Can I take a look at your stocks?",
                1,
                "Do you make any custom orders?",
                2,
                "I think I'm okay.",
                3,
            )
        when (option) {
            1 -> chatPlayer(quiz, "Can I take a look at your stocks?")
            2 -> {
                chatPlayer(quiz, "Do you make any custom orders?")
                chatNpc(
                    happy,
                    "I do! I'm always keeping an eye out for interesting materials to work with.",
                )
                when (choice2("Could you make something for me?", 1, "Maybe later.", 2)) {
                    1 -> chatPlayer(quiz, "Could you make something for me?")
                    2 -> chatPlayer(neutral, "Maybe later.")
                }
            }
            3 -> chatPlayer(neutral, "I think I'm okay.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sian
    private suspend fun Dialogue.sian() {
        chatNpc(
            happy,
            "Hey! You look like quite the sharp shooter. Can I interest you in a new bow?",
        )
        when (choice2("Let's see what you've got.", 1, "No thanks.", 2)) {
            1 -> chatPlayer(happy, "Let's see what you've got.")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(happy, "Well, if ya change your mind, I'll be right here.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Caerwyn
    // TODO(content): [skill:thieving] "You're the one who stole something from me!" after stealing
    private suspend fun Dialogue.caerwyn() {
        chatNpc(happy, "Are you interested in buying or selling spice?")
        if (choice2("Yes please.", 1, "No thanks.", 2) == 2) {
            chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Glenda
    // TODO(content): [skill:thieving] "You're the one who stole something from me!" after stealing
    private suspend fun Dialogue.glenda() {
        chatNpc(happy, "Here, look at my lovely gems.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Osian
    // TODO(content): [skill:thieving] "You stole from me. Talk to me when you've considered your
    // actions!" after stealing
    private suspend fun Dialogue.osian() {
        chatNpc(happy, "Silver! Silver! Best prices for buying and selling in all Prifddinas!")
        if (choice2("Yes please.", 1, "No thank you.", 2) == 2) {
            chatPlayer(neutral, "No thank you.")
        }
    }
}
