package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Hirko
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CrossbowShop
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HIRKO) { startDialogue(it.npc) { hirko() } }
        onOpNpc3(HIRKO) { shops.open(player, it.npc, "Crossbow Shop", "inv.xbows_shop_addy") }
    }

    private suspend fun Dialogue.hirko() {
        chatNpc(happy, "Hello there!")
        chatPlayer(quiz, "Hello, what's in those boxes?")
        chatNpc(happy, "Aha, interested in my crossbows are you?")
        chatPlayer(quiz, "Are they any good?")
        chatNpc(
            happy,
            "They're dwarven engineering at its best. If you've got enough skill in crafting, " +
                "smithing and fletching, you can even make them yourself. You can buy some of " +
                "the parts here and make the rest yourself.",
        )
        val option =
            choice3(
                "How do I make one for myself?",
                1,
                "What about ammo?",
                2,
                "Thanks for telling me. Bye!",
                3,
            )
        when (option) {
            1 -> crossbowGuide()
            2 -> ammo()
            else -> farewell()
        }
    }

    private suspend fun Dialogue.crossbowGuide() {
        chatPlayer(quiz, "How do I make one for myself?")
        chatNpc(
            neutral,
            "Well, firstly you'll need to chop yourself some wood, then use a knife on the wood " +
                "to whittle out a nice crossbow stock like these here.",
        )
        chatPlayer(neutral, "Wood fletched into stock... check.")
        chatNpc(
            neutral,
            "Then get yourself some metal and a hammer and smith yourself some limbs for the " +
                "bow, mind that you use the right metals and woods though as some wood is too " +
                "light to use with some metal and vice versa.",
        )
        chatPlayer(quiz, "Which goes with which?")
        chatNpc(
            neutral,
            "Wood and Bronze as they're basic materials, Oak and Blurite, Willow and Iron, " +
                "Steel and Teak, Mithril and Maple, Adamantite and Mahogany and finally Runite " +
                "and Yew.",
        )
        chatPlayer(quiz, "Ok, so I have my stock and a pair of limbs... what now?")
        chatNpc(
            neutral,
            "Simply take a hammer and smack the limbs firmly onto the stock. You'll then need a " +
                "string, only they're not the same as normal bows. You'll need to dry some " +
                "large animal's meat to get sinew, then spin that on a spinning",
        )
        chatNpc(
            neutral,
            "wheel, it's the only thing we've found to be strong enough for a crossbow.",
        )
        if (choice2("What about magic logs?", true, "Thanks for telling me. Bye!", false)) {
            chatPlayer(quiz, "What about magic logs?")
            chatNpc(
                neutral,
                "Well.. I don't rightly know... us dwarves don't work with magic, we prefer gold " +
                    "and rock. Much more stable. I guess you could ask the humans at the " +
                    "rangers guild to see if they can do something but I don't want",
            )
            chatNpc(neutral, "anything to do with it!")
        }
        farewell()
    }

    private suspend fun Dialogue.ammo() {
        chatPlayer(quiz, "What about ammo?")
        chatNpc(
            neutral,
            "You can smith yourself lots of different bolts, don't forget to flight them with " +
                "feathers like you do arrows though. You can poison any untipped bolt but " +
                "there's also the option of tipping them with gems then",
        )
        chatNpc(
            neutral,
            "enchanting them with runes. This can have some pretty powerful effects.",
        )
        chatPlayer(worried, "Oh my poor bank, how will I store all those?!")
        chatNpc(
            happy,
            "Don't you worry, I have just the thing here, a bolt pouch designed specially for " +
                "holding 4 different types of crossbow bolt.",
        )
        chatPlayer(quiz, "Wow, how much?")
        if (playerContainsObj(BOLT_POUCH)) {
            chatNpc(
                neutral,
                "Hmm... didn't I see you down here buying one the other day? You can only have " +
                    "one at a time I'm afraid.",
            )
            return
        }
        chatNpc(
            neutral,
            "Well let's see now... for you? 1500 gold pieces should do the trick.",
        )
        if (!choice2("Yes I'll pay the 1500gp.", true, "Thanks for telling me. Bye!", false)) {
            farewell()
            return
        }
        chatPlayer(happy, "Yes I'll pay the 1500gp.")
        chatNpc(neutral, "Hand it over then.")
        if (!player.invTakeFee(BOLT_POUCH_PRICE)) {
            chatPlayer(sad, "Oh, I don't have that much on me.")
            chatNpc(neutral, "Best go make some money then, shouldn't take you too long.")
            return
        }
        player.invAddOrDrop(objRepo, BOLT_POUCH)
    }

    private suspend fun Dialogue.farewell() {
        chatPlayer(happy, "Thanks for telling me. Bye!")
        chatNpc(happy, "Take care, straight shooting.")
    }

    private companion object {
        const val HIRKO = "npc.xbows_sales_dwarf_keldegrim"
        const val BOLT_POUCH = "obj.xbows_bolt_pouch"
        const val BOLT_POUCH_PRICE = 1500
    }
}
