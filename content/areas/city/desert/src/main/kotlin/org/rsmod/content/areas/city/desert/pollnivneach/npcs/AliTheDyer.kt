package org.rsmod.content.areas.city.desert.pollnivneach.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ali_the_dyer
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_feud] Rana's name after the quest
class AliTheDyer @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(DYER) { startDialogue(it.npc) { dyer() } }
    }

    private suspend fun Dialogue.dyer() {
        chatPlayer(happy, "Wow! look at this place, it's so colourful.")
        chatNpc(happy, "Why thank you. Can I help you with anything?")
        chatPlayer(
            quiz,
            "I take it by the clothes you are wearing and this room that you are in the dye trade?",
        )
        chatNpc(neutral, "How observant of you.")
        val makeDye =
            choice2(
                "Could you make some dye for me please?",
                true,
                "How did you get into the colour business?",
                false,
            )
        if (makeDye) {
            dyeMenu()
            return
        }
        chatPlayer(quiz, "How did you get into the colour business?")
        chatNpc(
            neutral,
            "Well, it's quite simple really my mother was a drysalter and I learnt the trade from " +
                "her.",
        )
        chatPlayer(confused, "Umm, what's a drysalter, it isn't a medical condition is it?")
        chatNpc(
            laugh,
            "No silly, it's someone who deals with preservatives and dyes. I didn't really enjoy " +
                "the preservative side of things so I've chosen to deal solely in dyeing.",
        )
        chatPlayer(quiz, "So how's business then?")
        chatNpc(
            laugh,
            "It's a dyeing trade! Oh gosh, I'm sorry, that was a truly dreadful joke.",
        )
        chatPlayer(neutral, "Hmm yes, about the worst I've heard today at least.")
        chatNpc(
            neutral,
            "Thanks. But really, it's not the best around here, nearly everyone in this town " +
                "wears only white, which means I don't exactly do a roaring trade. Now is there " +
                "anything I can get you?",
        )
        val dyes =
            choice2(
                "Could you make some dye for me please?",
                true,
                "No thanks, I'm just having a look around.",
                false,
            )
        if (dyes) {
            dyeMenu()
        } else {
            chatPlayer(neutral, "No thanks, I'm just having a look around.")
        }
    }

    private suspend fun Dialogue.dyeMenu() {
        chatPlayer(quiz, "Can you make dyes for me please?")
        chatNpc(
            neutral,
            "What colour dye do you want? I can make you blue, red or yellow dyes for you. If you " +
                "bring me the correct raw materials.",
        )
        val dye =
            choice4(
                "What do you need to make red dye?",
                Dye.Red,
                "What do you need to make yellow dye?",
                Dye.Yellow,
                "What do you need to make blue dye?",
                Dye.Blue,
                "On second thoughts I don't really need any dye.",
                null,
            )
        if (dye == null) {
            chatPlayer(neutral, "On second thoughts, I don't think that I need some dye right now.")
            return
        }
        chatPlayer(quiz, "What do you need to make ${dye.colour} dye?")
        for (line in dye.recipe) {
            chatNpc(neutral, line)
        }
        val choice =
            choice4(
                "Okay, make me some ${dye.colour} dye please.",
                1,
                "I don't think I have all the ingredients yet.",
                2,
                "I can do without dye at that price.",
                3,
                dye.whereOption,
                4,
            )
        when (choice) {
            1 -> makeDye(dye)
            2 -> {
                chatPlayer(neutral, "I don't think I have all the ingredients yet.")
                chatNpc(
                    neutral,
                    "You know what you need to get, now come back when you have them. Goodbye " +
                        "for now.",
                )
            }
            3 -> {
                chatPlayer(neutral, "I can do without dye at that price.")
                chatNpc(
                    neutral,
                    "Fair enough, I still think that the price is fairly reasonable considering " +
                        "that I'm the only source of dye this side of the Shantay pass.",
                )
            }
            4 -> where(dye)
        }
    }

    private suspend fun Dialogue.where(dye: Dye) {
        chatPlayer(quiz, dye.whereOption)
        when (dye) {
            Dye.Red -> {
                chatNpc(neutral, "Well you could try growing them. They shouldn't be too hard to grow.")
                chatPlayer(quiz, "If I didn't care to grow my own, where else could I get some?")
                chatNpc(
                    neutral,
                    "Well I know for sure that the desert climate wouldn't suit them. I have mine " +
                        "picked for me in the woods south of Varrock.",
                )
            }
            Dye.Yellow -> {
                chatNpc(laugh, "Generally you would find onions in vegetable gardens, silly.")
                chatPlayer(
                    neutral,
                    "Ok you have me there but is there anywhere around here where I could get my " +
                        "hands on some onions?",
                )
                chatNpc(
                    neutral,
                    "I really don't know. A friend of mine grows hers on a farms near Draynor " +
                        "village. But that's a little far from here to be useful.",
                )
            }
            Dye.Blue ->
                chatNpc(
                    neutral,
                    "Woad leaves are fairly rare. My other customers tell me there's a gardener in " +
                        "Falador who grows them.",
                )
        }
    }

    private suspend fun Dialogue.makeDye(dye: Dye) {
        val inv = access.inv
        if (inv.count(dye.ingredient) < dye.ingredientCount) {
            mesbox("You don't have enough ${dye.shortName} to make the ${dye.colour} dye.")
            return
        }
        if (inv.count(COINS) < DYE_PRICE) {
            mesbox("You don't have enough coins to pay for the dye.")
            return
        }
        chatPlayer(happy, "Ok make me some ${dye.colour} dye please.")
        if (!player.invTakeFee(DYE_PRICE)) return
        if (access.invDel(inv, dye.ingredient, dye.ingredientCount).failure) {
            player.invAddOrDrop(objRepo, COINS, DYE_PRICE)
            return
        }
        player.invAddOrDrop(objRepo, dye.obj)
        mesbox(dye.handOver)
    }

    private enum class Dye(
        val colour: String,
        val obj: String,
        val ingredient: String,
        val ingredientCount: Int,
        val shortName: String,
        val recipe: List<String>,
        val whereOption: String,
        val handOver: String,
    ) {
        Red(
            colour = "red",
            obj = "obj.reddye",
            ingredient = "obj.redberries",
            ingredientCount = 3,
            shortName = "berries",
            recipe =
                listOf(
                    "Hmm.... 3 lots of redberries should do the trick and 25 coins should cover " +
                        "any other costs."
                ),
            whereOption = "Where would I find a redberry bush?",
            handOver =
                "You hand the redberries and some coins to Rana, who then rummages around some " +
                    "shelves, eventually finding a red bottle, which she then hands to you.",
        ),
        Yellow(
            colour = "yellow",
            obj = "obj.yellowdye",
            ingredient = "obj.onion",
            ingredientCount = 2,
            shortName = "onions",
            recipe =
                listOf(
                    "Yellow is an awkward colour to recreate, I guess if you got me 2 onions I " +
                        "could use their skins to make a warm yellow dye for you. Oh yes and lest " +
                        "I forget I'll need 25 coins to cover the cost of the glass ware and",
                    "my wages.",
                ),
            whereOption = "Where could I find some onions?",
            handOver =
                "You hand two onions to Rana, who then rummages around some shelves, eventually " +
                    "finding a yellow bottle, which she then hands to you.",
        ),
        Blue(
            colour = "blue",
            obj = "obj.bluedye",
            ingredient = "obj.woadleaf",
            ingredientCount = 2,
            shortName = "woad leaves",
            recipe = listOf("To make a bottle of blue dye I'll need 2 woad leaves and 25 coins from you."),
            whereOption = "Where can I get some woad leaves?",
            handOver =
                "You hand the woad leaves and some coins to Rana, who then rummages around some " +
                    "shelves, eventually finding a blue bottle, which she then hands to you.",
        ),
    }

    private companion object {
        const val DYER = "npc.feud_dye_lady_multi"
        const val COINS = "obj.coins"
        const val DYE_PRICE = 25
    }
}
