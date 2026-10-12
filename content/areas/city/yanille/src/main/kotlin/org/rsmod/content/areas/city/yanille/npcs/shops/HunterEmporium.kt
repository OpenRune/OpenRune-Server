package org.rsmod.content.areas.city.yanille.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HunterEmporium @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ALECK) { startDialogue(it.npc) { aleck(it.npc) } }
        onOpNpc5(ALECK) { player.openHunterEmporium(it.npc) }
        onOpNpc1(LEON) { startDialogue(it.npc) { leon() } }
    }

    private fun Player.openHunterEmporium(npc: Npc) {
        shops.open(this, npc, "Aleck's Hunter Emporium.", "inv.hunting_shop_yanille")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aleck
    private suspend fun Dialogue.aleck(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Hello, hello, and a most warm welcome to my Hunter Emporium. We have everything the " +
                "discerning Hunter could need.",
        )
        chatNpc(
            happy,
            "Would you like me to show you our range of equipment? Or was there something " +
                "specific you were after?",
        )
        val option =
            choice3(
                "Ok, let's see what you've got!",
                1,
                "I'm not interested, thanks.",
                2,
                "Who's that guy over there?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Ok, let's see what you've got!")
                player.openHunterEmporium(npc)
            }
            2 -> {
                chatPlayer(neutral, "I'm not interested, thanks.")
                chatNpc(
                    neutral,
                    "Well, if you do ever find yourself in need of the finest Hunter equipment " +
                        "available, then you know where to come.",
                )
            }
            3 -> {
                chatPlayer(quiz, "Who's that guy over there?")
                chatNpc(
                    shifty,
                    "Him? I think he might be crazy. Either that or he's seeking attention.",
                )
                chatNpc(
                    neutral,
                    "He keeps trying to sell me these barmy looking weapons he's invented. I " +
                        "can't see them working, personally.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Leon
    // TODO(content): [system:shops] no server inv for Leon's Prototype Crossbow (Trade op)
    // TODO(content): [skill:hunter] kebbit bolt make-x interface and the Ammo op
    private suspend fun Dialogue.leon() {
        if (HUNTERS_CROSSBOW in player.inv || HUNTERS_CROSSBOW in player.worn) {
            chatNpc(happy, "Oh, hey, you have one of my crossbows! How's it working for you?")
            val option =
                choice5(
                    "It's okay, thanks.",
                    0,
                    "What is this place?",
                    1,
                    "Can you tell me about your crossbow?",
                    2,
                    "Tell me about the ammo for your crossbow.",
                    3,
                    "I'll be off now, excuse me.",
                    4,
                )
            if (option == 0) {
                chatPlayer(happy, "It's okay, thanks.")
                leonTopics(leonMenu())
                return
            }
            leonTopics(option)
            return
        }
        mesbox("Leon is gazing intently at the crossbow in his hands.")
        leonTopics(leonMenu())
    }

    private suspend fun Dialogue.leonMenu(): Int =
        choice4(
            "What is this place?",
            1,
            "Can you tell me about your crossbow?",
            2,
            "Tell me about the ammo for your crossbow.",
            3,
            "I'll be off now, excuse me.",
            4,
        )

    private suspend fun Dialogue.leonTopics(first: Int) {
        var option = first
        while (true) {
            when (option) {
                1 -> whatIsThisPlace()
                2 -> if (!crossbow()) return
                3 -> if (!ammo()) return
                else -> {
                    leaveLeon()
                    return
                }
            }
            option =
                choice3(
                    "Can you tell me about your crossbow?",
                    2,
                    "Tell me about the ammo for your crossbow.",
                    3,
                    "I'll be off now, excuse me.",
                    4,
                )
        }
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is Aleck's Hunter Emporium. Basically, it's just a shop with a fancy name; you " +
                "can buy various weapons and traps here.",
        )
        chatPlayer(quiz, "Can I buy some equipment from the shop then?")
        chatNpc(
            neutral,
            "Oh, this isn't my shop; the owner is Aleck over there behind the counter.",
        )
        chatNpc(
            neutral,
            "I experiment with weapon designs. I'm here because I've been trying to convince " +
                "people to back my research and maybe sell some of my products - like this one " +
                "I'm holding - in their shops.",
        )
        chatNpc(sad, "Aleck doesn't seem to be interested, though.")
    }

    private suspend fun Dialogue.crossbow(): Boolean {
        chatPlayer(quiz, "Can you tell me about your crossbow?")
        chatNpc(
            happy,
            "It's good, isn't it? I designed it to incorporate the bones of various animals in " +
                "its construction.",
        )
        chatNpc(
            happy,
            "It's a fair bit faster than an ordinary crossbow too; it'll take you far less time " +
                "to reload between shots.",
        )
        val crazy =
            choice2(
                "That's crazy, it'll never work!",
                true,
                "That sounds good. Let's trade.",
                false,
            )
        if (!crazy) {
            chatPlayer(happy, "That sounds good. Let's trade.")
            return false
        }
        chatPlayer(laugh, "That's crazy, it'll never work!")
        chatNpc(neutral, "That's what they said about my wind-powered mouse traps, too.")
        chatPlayer(quiz, "And did they work?")
        chatNpc(
            sad,
            "Well, they only ran into problems because people kept insisting on trying to use " +
                "them indoors.",
        )
        chatNpc(happy, "Anyway, I think my crossbow invention is showing a lot more promise.")
        return true
    }

    private suspend fun Dialogue.ammo(): Boolean {
        chatPlayer(quiz, "Tell me about the ammo for your crossbow.")
        chatNpc(
            neutral,
            "Ah, I admit as a result of its... er... unique construction, it won't take just any " +
                "old bolts.",
        )
        chatNpc(
            neutral,
            "If you can supply the materials and a token fee, I'd be happy to make some for you.",
        )
        chatNpc(
            neutral,
            "I need kebbit spikes, lots of 'em. Not all kebbits have spikes, mind you. You'll be " +
                "wanting prickly kebbits or, even better, razor-backed kebbits to get material " +
                "hard enough.",
        )
        val ownAmmo =
            choice2(
                "Can't I just make my own?",
                true,
                "Okay, can you make ammo for me?",
                false,
            )
        if (!ownAmmo) {
            chatPlayer(quiz, "Okay, can you make ammo for me?")
            return false
        }
        chatPlayer(quiz, "Can't I just make my own?")
        chatNpc(
            neutral,
            "Yes, I suppose you could, although you'll need a steady hand with a knife and a " +
                "chisel.",
        )
        chatNpc(
            neutral,
            "The bolts have an unusual diameter, but basically you'll just need to be able to " +
                "carve kebbit spikes into straight shafts.",
        )
        chatNpc(
            neutral,
            "Meanwhile, since you're here, I can make some for you if you have the materials.",
        )
        return true
    }

    private suspend fun Dialogue.leaveLeon() {
        chatPlayer(neutral, "I'll be off now, excuse me.")
        chatNpc(
            happy,
            "Well, if you ever find yourself in need of that innovative edge, you can always " +
                "find me here.",
        )
        chatPlayer(confused, "... thanks.")
    }

    private companion object {
        const val ALECK = "npc.hunting_shop_owner_yanille"
        const val LEON = "npc.hunting_weapon_designer"
        const val HUNTERS_CROSSBOW = "obj.hunting_crossbow"
    }
}
