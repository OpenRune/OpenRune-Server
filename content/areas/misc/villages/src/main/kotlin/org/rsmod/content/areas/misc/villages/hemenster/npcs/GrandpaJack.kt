package org.rsmod.content.areas.misc.villages.hemenster.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Grandpa_Jack
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:fishing_contest] "Are you entering the fishing competition?"
class GrandpaJack @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.grandpa_jack") { startDialogue(it.npc) { grandpaJack() } }
    }

    private suspend fun Dialogue.grandpaJack() {
        chatNpc(
            happy,
            "Hello young ${youngOne()}!  Come to visit old Grandpa Jack? I can tell ye stories " +
                "for sure. I used to be the best fisherman these parts have seen!",
        )
        val option =
            choice4(
                "Tell me a story then.",
                1,
                "Sorry, I don't have time now.",
                2,
                "Can I buy one of your fishing rods?",
                3,
                "I've forgotten how to fish, can you remind me?",
                4,
            )
        when (option) {
            1 -> story()
            2 -> {
                chatPlayer(neutral, "Sorry, I don't have time now.")
                chatNpc(sad, "Sigh...")
                chatNpc(sad, "Young people - always in such a rush.")
            }
            3 -> buyRod()
            4 -> remindMe()
        }
    }

    private fun Dialogue.youngOne(): String =
        when (player.appearance.subjectPronoun()) {
            "He" -> "man"
            "She" -> "lady"
            else -> "one"
        }

    private suspend fun Dialogue.story() {
        chatPlayer(happy, "Tell me a story then.")
        chatNpc(
            happy,
            "Well, when I were a young man we used to take fishing trips over to Catherby; The " +
                "fishing over there - now that was something!",
        )
        chatNpc(
            neutral,
            "Anyway, we decided to do a bit of fishing with our nets, I wasn't having the best of " +
                "days turning up nothing but old boots and bits of seaweed. Then my net suddenly " +
                "got really heavy!",
        )
        chatNpc(neutral, "I pulled it up...")
        chatNpc(shocked, "To my amazement I'd caught this little chest thing!")
        chatNpc(
            happy,
            "Even more amazing was when I opened it it contained a diamond the size of a radish! " +
                "That's the best catch I've ever had!",
        )
    }

    private suspend fun Dialogue.buyRod() {
        chatPlayer(quiz, "Can I buy one of your fishing rods?")
        if (FISHING_ROD in player.inv) {
            chatNpc(
                neutral,
                "But young man, you have your own. I'll not sell you mine when you already have " +
                    "one!",
            )
            return
        }
        chatNpc(
            happy,
            "Of course you can young man. Let's see now...I think 5 gold is a fair price for a " +
                "rod which has won the Fishing contest before eh?",
        )
        if (player.inv.count(COINS) < PRICE) {
            chatPlayer(sad, "I don't have enough money for that, I'll go get some and come back.")
            chatNpc(neutral, "Right you are. I'll be here.")
            return
        }
        val option =
            choice2(
                "Very fair, I'll buy that rod!",
                1,
                "That's too rich for me, I'll go to Catherby.",
                2,
            )
        if (option == 2) {
            chatPlayer(neutral, "That's too rich for me, I'll go to Catherby.")
            chatNpc(
                sad,
                "If you're sure.... passing up an opportunity of a lifetime you are.",
            )
            return
        }
        chatPlayer(happy, "Very fair, I'll buy that rod!")
        chatNpc(happy, "Excellent choice!")
        if (player.inv.isFull()) {
            chatPlayer(
                sad,
                "Only problem is, I don't have any space to carry one right now. Let me get back " +
                    "to you on that.",
            )
            chatNpc(neutral, "Right you are. I'll be here.")
            return
        }
        if (!player.invTakeFee(PRICE)) {
            return
        }
        player.invAddOrDrop(objRepo, FISHING_ROD)
        doubleobjbox(FISHING_ROD, COINS_ICON, "You hand over the money and receive a fishing rod.")
        chatPlayer(happy, "Thanks!")
        chatNpc(happy, "You're welcome young 'un.")
    }

    private suspend fun Dialogue.remindMe() {
        chatPlayer(quiz, "I've forgotten how to fish, can you remind me?")
        chatNpc(
            happy,
            "Of course! Let me see now...You'll need a rod and bait. You can fish with a net too, " +
                "but not in the competition.",
        )
        chatPlayer(neutral, "Ok... I think I can get those at Catherby.")
        chatNpc(
            neutral,
            "Then simply find yourself a fishing spot, either in the competition near here, or " +
                "wherever you can - I recommend net fishing in Catherby. Net or Lure the fish in " +
                "the fishing spot by clicking on it and then be",
        )
        chatNpc(neutral, "patient... ")
        chatPlayer(quiz, "It's that simple?")
        chatNpc(happy, "Yep! Go get em tiger.")
    }

    private companion object {
        const val COINS = "obj.coins"
        const val COINS_ICON = "obj.coins_250"
        const val FISHING_ROD = "obj.fishing_rod"
        const val PRICE = 5
    }
}
