package org.rsmod.content.quest.area.alkharid

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.quest.manager.menu

/** Ned and Aggie branches that only show while Prince Ali Rescue is in progress. */
object PrinceAliHelpers {
    private const val WOOL = "obj.ball_of_wool"
    private const val WIG_WOOL = 3
    private const val WATER = "obj.bucket_water"
    private const val JUG_WATER = "obj.jug_water"
    private val PASTE_BASE = listOf("obj.ashes", "obj.pot_flour", "obj.redberries")

    suspend fun Dialogue.nedOtherThings() {
        chatPlayer(quiz, "Could you make other things apart from rope?")
        chatNpc(happy, "I'm sure I can. What are you thinking of?")
        when (
            menu(
                "Could you knit me a sweater?" to 1,
                "How about some sort of wig?" to 2,
                "Could you repair the arrow holes in the back of my shirt?" to 3,
                "Actually, I don't need anything." to 4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Could you knit me a sweater?")
                chatNpc(
                    angry,
                    "Do I look like a member of a sewing circle? Be off wi' you. I have fought " +
                        "monsters that would turn your hair blue.",
                )
                chatNpc(angry, "I don't need to be laughed at just 'cos I'm getting a bit old.")
            }
            2 -> nedWig()
            3 -> {
                chatPlayer(quiz, "Could you repair the arrow holes in the back of my shirt?")
                chatNpc(
                    neutral,
                    "Ah yes, it's a tough world these days. There's a few brave enough to attack from " +
                        "ten metres away.",
                )
                mesbox("Ned pulls out a needle and attacks your shirt.")
                chatNpc(happy, "There you go, good as new.")
                chatPlayer(happy, "Thanks Ned. Maybe next time they will attack me face to face.")
            }
            else -> chatPlayer(neutral, "Actually, I don't need anything.")
        }
    }

    private suspend fun Dialogue.nedWig() {
        chatPlayer(quiz, "How about some sort of wig?")
        chatNpc(
            neutral,
            "Well... that's an interesting thought. Yes, I think I could do something. Give me three " +
                "balls of wool and I might be able to do it.",
        )
        if (access.inv.count(WOOL) < WIG_WOOL) {
            chatPlayer(neutral, "Great, I will get some. I think a wig would be useful.")
            return
        }
        val make =
            menu(
                "I have them here. Please make me a wig." to true,
                "Actually, I don't need one right now." to false,
            )
        if (!make) {
            chatPlayer(neutral, "Actually, I don't need one right now.")
            chatNpc(neutral, "Fair enough.")
            return
        }
        chatPlayer(neutral, "I have them here. Please make me a wig.")
        chatNpc(neutral, "Okay, I'll have a go.")
        access.invDel(access.inv, WOOL, WIG_WOOL)
        access.invAdd(access.inv, PrinceAliRescue.WIG)
        objbox(PrinceAliRescue.WIG, "Ned gives you a pretty good wig.")
        chatNpc(happy, "Here you go. How's that for a quick effort? Not bad I think!")
        chatPlayer(happy, "Thanks Ned. There's more to you than meets the eye.")
    }

    private fun Dialogue.waterSource(): String? =
        listOf(WATER, JUG_WATER).firstOrNull { access.inv.count(it) > 0 }

    suspend fun Dialogue.aggieSkinPaste() {
        chatPlayer(quiz, "Can you make skin paste?")
        val water = waterSource()
        if (water == null || PASTE_BASE.any { access.inv.count(it) == 0 }) {
            chatNpc(
                happy,
                "Why, it's one of my most popular potions! Lots of people around here like to pretty " +
                    "their faces up a bit. I can make it for you if you get me what's needed.",
            )
            chatPlayer(quiz, "What do you need?")
            chatNpc(
                neutral,
                "Well dearie, you need a base for the paste. That's a mix of ash, flour and water. " +
                    "Then you need redberries to colour it as you want. Bring me those four items and I " +
                    "will make you some.",
            )
            return
        }
        chatNpc(
            happy,
            "Yes, I can. I see you already have the ingredients - the water, flour, ashes and " +
                "redberries. Would you like me to mix some for you now?",
        )
        val mix =
            menu(
                "Yes please. Mix me some skin paste." to true,
                "No thank you. I don't need any skin paste right now." to false,
            )
        if (!mix) {
            chatPlayer(neutral, "No thank you. I don't need any skin paste right now.")
            chatNpc(neutral, "Okay dearie, that's always your choice.")
            return
        }
        chatPlayer(happy, "Yes please. Mix me some skin paste.")
        chatNpc(happy, "That should be simple. Hand the things to Aggie then.")
        access.invDel(access.inv, water)
        for (item in PASTE_BASE) access.invDel(access.inv, item)
        doubleobjbox(
            "obj.redberries",
            "obj.pot_flour",
            "You hand the ash, flour, water and redberries to Aggie. She tips the ingredients into a " +
                "cauldron and mutters some words.",
        )
        chatNpc(confused, "Tourniquet, Fenderbaum, Tottenham, Marshmallow, Marblearch.")
        access.invAdd(access.inv, PrinceAliRescue.PASTE)
        objbox(PrinceAliRescue.PASTE, "Aggie hands you the skin paste.")
        chatNpc(happy, "There you go dearie. That will make you look good at the Varrock dances.")
    }
}
