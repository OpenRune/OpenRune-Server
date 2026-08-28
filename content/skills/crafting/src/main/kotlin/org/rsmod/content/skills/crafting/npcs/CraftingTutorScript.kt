package org.rsmod.content.skills.crafting.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CraftingTutorScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CraftingConstants.CRAFTING_TUTOR) { greet(it.npc) }
    }

    private suspend fun ProtectedAccess.greet(npc: Npc) {
        startDialogue(npc) {
            chatPlayer(happy, "Hello.")
            chatNpc(happy, "Hello there! Are you interested in hearing all about crafting?")
            mainMenu()
        }
    }

    private suspend fun Dialogue.mainMenu() {
        val asked = mutableSetOf<MainTopic>()
        while (true) {
            val remaining = MainTopic.entries.filter { it !in asked }
            val options = remaining.map { it.option to it } + ("No, thank you." to null)
            val pick = chooseFrom(options)
            if (pick == null) {
                farewell()
                return
            }
            asked += pick
            when (pick) {
                MainTopic.Training -> trainingAdvice()
                MainTopic.Craftables -> craftableTopics()
            }
        }
    }

    private suspend fun Dialogue.farewell() {
        chatPlayer(neutral, "No, thank you.")
        chatNpc(happy, "Well, just come back any time you want to know anything!")
    }

    private suspend fun Dialogue.trainingAdvice() {
        chatPlayer(quiz, MainTopic.Training.option)
        when (access.statBase(CraftingConstants.STAT_CRAFTING)) {
            in Int.MIN_VALUE..9 -> beginnerAdvice()
            in 10..19 -> noviceAdvice()
            else -> experiencedAdvice()
        }
        chatPlayer(happy, "Thanks!")
        chatNpc(quiz, "Is there anything else you want to know?")
    }

    private suspend fun Dialogue.beginnerAdvice() {
        chatNpc(
            neutral,
            "To get started, you might like to try your hand at crafting some armour from cow's " +
                "leather. There's a tanner just over in Al Kharid who could tan any hides for you.",
        )
        chatNpc(
            neutral,
            "Once you've managed to get your hand on some kind of leather, you just need a needle " +
                "and some thread to get to work!",
        )
        chatNpc(
            neutral,
            "If that doesn't take your fancy, you could try your hand at spinning wool right here " +
                "on the spinning wheel.",
        )
        chatNpc(
            neutral,
            "Come to think of it... Farmer Fred, just north of here, sounds like he could use a " +
                "hand shearing some of his sheep. Maybe you could help him out, and get some " +
                "practice while you're at it.",
        )
    }

    private suspend fun Dialogue.noviceAdvice() {
        chatNpc(
            neutral,
            "Lots of budding craftsmen seem to spend their time here spinning Flax into bow " +
                "strings on the wheel, if that takes your fancy.",
        )
        chatNpc(
            neutral,
            "Or you might like to try your hand at crafting some armour from cow's leather. " +
                "There's a tanner just over in Al Kharid who could tan any hides for you.",
        )
    }

    private suspend fun Dialogue.experiencedAdvice() {
        if (access.statBase(CraftingConstants.STAT_CRAFTING) >= CraftingConstants.MAX_CRAFTING_LEVEL) {
            chatNpc(
                happy,
                "I can't help but feel like I should be the one asking you! At this point, the " +
                    "world's your oyster!",
            )
        } else {
            chatNpc(happy, "Now that you've got a little experience under your belt, the world's your oyster!")
        }
        chatNpc(
            neutral,
            "If you wanted to try your hand at making some jewellery, you'll want to get your hand " +
                "on some gold and some kind of gem, with a mould to work with and a chisel at hand.",
        )
        chatNpc(neutral, "But if that's not your thing, maybe you'd like to try making some more armour!")
        chatNpc(
            neutral,
            "Cow's leather's a good place to start, but you can try all other kinds of hides, like " +
                "a dragon's!",
        )
        chatNpc(
            neutral,
            "Glass blowing seems to be a popular trade, too. You can forge molten glass from some " +
                "sand and soda ash, then make all kinds of things using a glass blowing pipe.",
        )
    }

    private suspend fun Dialogue.craftableTopics() {
        chatPlayer(quiz, MainTopic.Craftables.option)
        chatNpc(
            happy,
            "All kinds of things, really! You can make armour from leather, make some pottery, try " +
                "your hand at glass blowing, make some jewellery, weapons even...",
        )
        chatNpc(quiz, "What would you like to hear about?")

        val asked = mutableSetOf<CraftTopic>()
        while (true) {
            val remaining = CraftTopic.entries.filter { it !in asked }
            val topics = remaining.map { it.option to it }

            val options: List<Pair<String, CraftTopic?>>
            if (asked.isEmpty()) {
                options = topics
            } else {
                options = topics + ("Not right now." to null)
            }

            val pick = chooseFrom(options)
            if (pick == null) {
                chatPlayer(neutral, "Not right now.")
                chatNpc(quiz, "Is there anything else you want to know?")
                return
            }
            asked += pick
            chatPlayer(quiz, pick.option)
            when (pick) {
                CraftTopic.Armour -> armour()
                CraftTopic.Pottery -> pottery()
                CraftTopic.GlassBlowing -> glassBlowing()
                CraftTopic.Jewellery -> jewellery()
                CraftTopic.Weapons -> weapons()
            }
            chatNpc(quiz, "Would you like to hear about anything else I mentioned?")
        }
    }

    private suspend fun Dialogue.armour() {
        chatNpc(
            neutral,
            "Sure thing! Most armour you can craft just involves taking a needle and some thread " +
                "to whatever material you can get your hands on.",
        )
        chatNpc(
            neutral,
            "You might like to try with different kinds of animal hide, from cows, yaks, snakes, " +
                "dragons... Or any other sort of fabric you can find!",
        )
        chatNpc(neutral, "Some crafty types have started making some leather-covered wooden shields recently, too!")
        chatNpc(
            neutral,
            "You might find some creatures make for a pretty sturdy helmet, too. The best way to " +
                "make something like that's just to take a good old chisel to it!",
        )
        chatPlayer(happy, "That sounds great.")
    }

    private suspend fun Dialogue.pottery() {
        chatNpc(neutral, "Of course! Pottery's all kind of the same, it just takes a lot of getting used to.")
        chatNpc(
            neutral,
            "All that there really is to it is getting your hands on some clay, getting it wet and " +
                "getting to work on a potter's wheel.",
        )
        chatNpc(
            neutral,
            "Once you've got the shape you want out of it, you want to put it in a pottery oven to " +
                "fire it up.",
        )
        chatNpc(
            neutral,
            "The barbarians west of Varrock seem to be pretty keen on their pottery. I'd take a " +
                "look around there, if you wanted to try it out!",
        )
        chatNpc(neutral, "Once you get the hang of it, you can make pots, pie dishes, bowls... You know.")
        chatPlayer(happy, "Neat!")
    }

    private suspend fun Dialogue.glassBlowing() {
        chatNpc(
            neutral,
            "Glass blowing? Well, to get started you'll need to get yourself some molten glass. You " +
                "can make some for yourself by heating sand and soda ash in a furnace.",
        )
        chatNpc(
            neutral,
            "Once you've got a hold of that, you'll want a glass blowing pipe to blow it out in to " +
                "shape, before it cools down.",
        )
        chatNpc(neutral, "It's not the easiest thing to get to grips with, but it shouldn't be too difficult!")
        chatPlayer(happy, "Nifty.")
    }

    private suspend fun Dialogue.jewellery() {
        chatNpc(
            neutral,
            "Sure. All it boils down to is a bar of gold or silver, some kind of cut gem if you're " +
                "feeling fancy, and a mould to help shape the thing.",
        )
        chatNpc(
            neutral,
            "You can use a chisel to make a gem really shine and sparkle. You wouldn't want to use " +
                "an uncut one for any jewellery.",
        )
        chatNpc(
            neutral,
            "Once you've got all of your materials together, you'll want to use a furnace to " +
                "actually craft whatever it is you're trying to make!",
        )
        chatNpc(
            neutral,
            "Wizards seem to like enchanting their jewellery to do all kinds of things, too, but I " +
                "don't know a lot about that.",
        )
        chatPlayer(happy, "Sounds good!")
    }

    private suspend fun Dialogue.weapons() {
        chatNpc(
            neutral,
            "Well, there aren't too many weapons that people tend to craft, but there are a couple " +
                "worth noting!",
        )
        chatNpc(
            neutral,
            "Battlestaves are probably the most common. It boils down to fastening an orb of some " +
                "kind onto the end of a battlestaff.",
        )
        chatNpc(
            neutral,
            "You do need to get the orb in the first place, though... You can make one through " +
                "glass blowing, but it's not much use without being charged.",
        )
        chatNpc(
            neutral,
            "I can't help much with crafting it, though! There's a little more magic involved there " +
                "than I'm familiar with.",
        )
        chatNpc(
            neutral,
            "Aside from battlestaves, there are a couple of weapons that people like to make from " +
                "silver... Sickles and bolts, usually.",
        )
        chatPlayer(happy, "Interesting...")
    }

    private suspend fun <T> Dialogue.chooseFrom(options: List<Pair<String, T>>): T =
        when (options.size) {
            1 -> options[0].second
            2 -> choice2(options[0].first, options[0].second, options[1].first, options[1].second)
            3 -> choice3(
                options[0].first, options[0].second,
                options[1].first, options[1].second,
                options[2].first, options[2].second,
            )
            4 -> choice4(
                options[0].first, options[0].second,
                options[1].first, options[1].second,
                options[2].first, options[2].second,
                options[3].first, options[3].second,
            )
            else -> choice5(
                options[0].first, options[0].second,
                options[1].first, options[1].second,
                options[2].first, options[2].second,
                options[3].first, options[3].second,
                options[4].first, options[4].second,
            )
        }

    private enum class MainTopic(val option: String) {
        Training("How can I train my crafting?"),
        Craftables("What kinds of things can be crafted?"),
    }

    private enum class CraftTopic(val option: String) {
        Armour("Tell me about crafting armour."),
        Pottery("Tell me about pottery."),
        GlassBlowing("Tell me about glass blowing."),
        Jewellery("Tell me about making jewellery."),
        Weapons("Tell me about crafting weapons."),
    }
}
