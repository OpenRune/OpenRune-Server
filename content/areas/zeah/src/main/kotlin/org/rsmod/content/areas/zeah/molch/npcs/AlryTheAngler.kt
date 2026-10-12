package org.rsmod.content.areas.zeah.molch.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Alry_the_Angler
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server inv for Alry the Angler's Angling Accessories (Trade op
//  and "Let's take a look.")
// TODO(content): [skill:aerial_fishing] lending and taking back the cormorant's glove, the
//  "Hello again." dialogue while wearing it, and paying 20 pearls to feed whole fish
class AlryTheAngler : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ALRY) { startDialogue(it.npc) { alry() } }
        onOpNpc4(ALRY) { startDialogue(it.npc) { getBird(watching = true) } }
    }

    private suspend fun Dialogue.alry() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "What brings you to these parts, stranger?")
        val hasTench = GOLDEN_TENCH in player.inv
        val topic =
            if (hasTench) {
                choice5(
                    "What is this place?",
                    AlryTopic.Place,
                    "Could I have a go with your bird?",
                    AlryTopic.Bird,
                    "What's in the sack?",
                    AlryTopic.Sack,
                    "I found this big fish...",
                    AlryTopic.Tench,
                    "About feeding the bird...",
                    AlryTopic.Feeding,
                )
            } else {
                choice4(
                    "What is this place?",
                    AlryTopic.Place,
                    "Could I have a go with your bird?",
                    AlryTopic.Bird,
                    "What's in the sack?",
                    AlryTopic.Sack,
                    "About feeding the bird...",
                    AlryTopic.Feeding,
                )
            }
        when (topic) {
            AlryTopic.Place -> place()
            AlryTopic.Bird -> getBird(watching = false)
            AlryTopic.Sack -> sack()
            AlryTopic.Tench -> tench()
            AlryTopic.Feeding -> feeding()
        }
    }

    private suspend fun Dialogue.place() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "I suppose it is a little different to the places your type are used to...",
        )
        chatNpc(
            happy,
            "This is my humble little hideaway! Plenty of fish around to sustain a man like me, " +
                "with the help of my trusty cormorant.",
        )
        chatPlayer(quiz, "A bird helps you to fish?")
        chatNpc(
            happy,
            "Sure she does! I haven't used a rod in years, just send her out to the waters and " +
                "she takes care of it for me!",
        )
        val haveAGo =
            choice2(
                "Could I have a go with your bird?",
                true,
                "I don't think that's for me.",
                false,
            )
        if (haveAGo) {
            getBird(watching = true)
        } else {
            chatPlayer(neutral, "I don't think that's for me... I'll leave that to you.")
            chatNpc(laugh, "No surprises there! Your type never have been cut out for it.")
        }
    }

    private suspend fun Dialogue.getBird(watching: Boolean) {
        chatPlayer(quiz, "Could I have a go with your bird?")
        val skilled =
            player.statBase("stat.fishing") >= AERIAL_FISHING_LEVEL &&
                player.statBase("stat.hunter") >= AERIAL_HUNTER_LEVEL
        if (!skilled) {
            chatNpc(
                neutral,
                "These birds can be a lot of work... I'm not sure you could handle them, let " +
                    "alone to catch a fish!",
            )
            mesbox("You need 43 Fishing and 35 Hunter to go aerial fishing.")
            return
        }
        chatNpc(neutral, "I don't know, I doubt that your type's up to the task...")
        chatNpc(laugh, "But it would be quite the amusing sight. Go on!")
        val handsFree =
            player.worn[Wearpos.Hands.slot] == null && player.worn[Wearpos.RightHand.slot] == null
        if (!handsFree) {
            chatNpc(neutral, "You're going to need to free up your hands first, though.")
            mesbox("Nothing in or on your hands.")
            return
        }
        if (watching) {
            chatNpc(
                happy,
                "I'll keep an eye on you and make sure you don't have too much trouble with her!",
            )
        }
    }

    private suspend fun Dialogue.sack() {
        chatPlayer(quiz, "What's in the sack?")
        chatNpc(happy, "Oh, this? This is my trusty old fish sack!")
        chatPlayer(confused, "Fish sack?")
        chatNpc(
            happy,
            "Yeah, fish sack! I might sell you one if you've found any of those pearls around " +
                "here.",
        )
        if (!choice2("Let's take a look.", true, "No thanks.", false)) {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.tench() {
        chatPlayer(happy, "I found this big fish...")
        chatNpc(
            shocked,
            "Now that is a biggun! How she managed to wrangle one like that I'll never know.",
        )
        chatNpc(
            neutral,
            "If you don't want it, I could take it off your hands for a few of those pearls!",
        )
        chatPlayer(quiz, "I'm interested... How many pearls are we talking?")
        chatNpc(neutral, "I'll give you a hundred pearls for it. What do you say?")
        val trade =
            choice2(
                "It's a deal.",
                true,
                "No thanks.",
                false,
                title = "Trade in your golden tench for 100 molch pearls?",
            )
        if (!trade) {
            chatPlayer(neutral, "No thanks.")
            chatNpc(neutral, "Well, let me know if you change your mind.")
            return
        }
        val sure =
            choice2(
                "No, keep the tench.",
                false,
                "Yes, exchange the golden tench.",
                true,
                title = "Are you REALLY sure?",
            )
        if (!sure) {
            chatPlayer(neutral, "On seconds thoughts, I'd rather keep it.")
            chatNpc(neutral, "As you wish.")
            return
        }
        if (!exchangeTench()) {
            return
        }
        chatPlayer(happy, "It's a deal.")
        chatNpc(happy, "Pleasure doing business with you!")
    }

    private fun Dialogue.exchangeTench(): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = GOLDEN_TENCH.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = inv
                    obj = MOLCH_PEARL.asRSCM(RSCMType.OBJ)
                    strictCount = TENCH_PEARLS
                }
            }
        return result.success
    }

    private suspend fun Dialogue.feeding() {
        chatPlayer(quiz, "About feeding the bird... Do I have to cut up the fish for it?")
        chatNpc(neutral, "Yes, she likes her food chopped up.")
        chatPlayer(quiz, "But she could eat them whole in a pinch?")
        chatNpc(
            neutral,
            "I suppose, but you wouldn't want me to catch you feeding her that way.",
        )
        if (player.inv.count(MOLCH_PEARL) < FEEDING_PEARLS) {
            chatPlayer(shifty, "Of course, I'd never do such a thing.")
            mesbox("You could bargain better if you had some pearls.")
            return
        }
        chatPlayer(shifty, "What if I were to... Sweeten the deal.")
        mesbox("You flash a handful of pearls.")
        chatNpc(happy, "I do like pearls...")
        chatNpc(neutral, "I'll do it for 20 of them.")
    }

    private enum class AlryTopic {
        Place,
        Bird,
        Sack,
        Tench,
        Feeding,
    }

    private companion object {
        const val ALRY = "npc.fishing_npc_angler"
        const val GOLDEN_TENCH = "obj.golden_tench"
        const val MOLCH_PEARL = "obj.aerial_fishing_pearl"
        const val TENCH_PEARLS = 100
        const val FEEDING_PEARLS = 20
        const val AERIAL_FISHING_LEVEL = 43
        const val AERIAL_HUNTER_LEVEL = 35
    }
}
