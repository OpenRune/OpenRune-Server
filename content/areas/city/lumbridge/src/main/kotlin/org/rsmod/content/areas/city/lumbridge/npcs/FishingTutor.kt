package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseFishingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FishingTutor : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aide_tutor_fishing") { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { tutorMenu() }
    }

    private suspend fun Dialogue.tutorMenu() {
        val choice =
            choice4(
                adviceOption(),
                1,
                "Tell me about different fish.",
                2,
                "Where and what should I fish?",
                3,
                "Goodbye.",
                4,
            )
        when (choice) {
            1 -> levelAdvice()
            2 -> differentFish()
            3 -> whereAndWhat()
            4 -> chatPlayer(neutral, "Goodbye.")
        }
    }

    private fun Dialogue.adviceOption(): String =
        when {
            player.baseFishingLvl >= ADVANCED -> "Any advice for an advanced fisher?"
            player.baseFishingLvl >= INTERMEDIATE ->
                "I already know about the basics of fishing, got any tips?"
            else -> "Can you teach me the basics of fishing please?"
        }

    private suspend fun Dialogue.levelAdvice() {
        chatPlayer(quiz, adviceOption())
        giveNetIfRequired()
        when {
            player.baseFishingLvl >= ADVANCED -> advancedAdvice()
            player.baseFishingLvl >= INTERMEDIATE -> intermediateAdvice()
            else -> basicAdvice()
        }
        tutorMenu()
    }

    private suspend fun Dialogue.giveNetIfRequired() {
        if (SMALL_NET in player.inv) {
            chatNpc(happy, "I see you already have a net.")
            return
        }
        if (player.invAdd(player.inv, SMALL_NET).success) {
            chatNpc(happy, "Ah, you've lost your net have you? Have another!")
        }
    }

    private suspend fun Dialogue.basicAdvice() {
        objbox(
            "obj.fishing_spot_icon_dummy",
            "Look for this icon on the minimap to find fishing spots, they " +
                "will move as the fish swim around so you can't be lazy.",
        )
        chatNpc(
            neutral,
            "Ahoy, to fish, you click on the shrimp fishin' spot while ye'r carrying a net.",
        )
        chatPlayer(confused, "I see... is that it?")
        chatNpc(
            happy,
            "There's far more as you progress - not just shrimps. You get " +
                "more equipment, bigger fish and other things too...",
        )
        chatNpc(
            happy,
            "When you have a full inventory, you can cook it or take it to " +
                "the bank. You can find a bank on the roof of the castle in " +
                "Lumbridge and a cookin' range in the castle kitchen.",
        )
    }

    private suspend fun Dialogue.intermediateAdvice() {
        chatNpc(
            neutral,
            "Arrr, chose carefully where and what you fish, you can get " +
                "different fish in different places throughout the land.",
        )
        chatNpc(
            neutral,
            "Make sure you hang on to your fish, don't throw them away as " +
                "you can get valuable cooking experience from them, gar! " +
                "Look out for other things too...",
        )
        chatNpc(neutral, "Then of course there is always the trawler...")
        chatNpc(
            neutral,
            "If you venture to Port Khazard north of Yanille and speaks to " +
                "me cabin boy Murphy, he can let you and a bunch of friends " +
                "use his trawler to fish with... Just make sure you take a " +
                "bailing bucket or two.",
        )
    }

    private suspend fun Dialogue.advancedAdvice() {
        chatNpc(
            happy,
            "Aye, as you get better and better you'll find that you can fish " +
                "things like Tuna and Swordfish. These be very good for combat.",
        )
        chatNpc(
            happy,
            "Of course, after that you will get Shark, and if you venture out " +
                "onto the Trawler from Port Khazard, you can then get Manta Ray " +
                "or Sea Turtle, very good eatin' on one of them.",
        )
        chatNpc(
            happy,
            "You can also use a fishing potion if you can get your hands on " +
                "one and just need that little edge.. ask someone who is " +
                "familiar with herblore.",
        )
        chatNpc(neutral, "Now.. quests...")
        chatNpc(
            neutral,
            "Can't rightly say's there's any I know of that you could handle " +
                "at the moment, but come back when you're a bit more " +
                "experienced and I'm sure I can give you a clue.",
        )
    }

    private suspend fun Dialogue.differentFish() {
        chatPlayer(quiz, "Tell me about different fish.")
        fishMenu()
    }

    /**
     * The chatbox tops out at five options, so the small net entry drops away from level 35 rather
     * than 39 to make room for the harpoon entry.
     */
    private suspend fun Dialogue.fishMenu() {
        val fishingLevel = player.baseFishingLvl
        when {
            fishingLevel >= HARPOON_LEVEL ->
                when (
                    choice5(
                        "Big Net Fish",
                        1,
                        "Rod and Fly Fishing",
                        2,
                        "Lobster pot",
                        3,
                        "Harpoon Fish",
                        4,
                        "Tell me about...",
                        5,
                    )
                ) {
                    1 -> bigNetFish()
                    2 -> rodAndFlyFish()
                    3 -> lobsterPotFish()
                    4 -> harpoonFish()
                    5 -> tutorMenu()
                }
            fishingLevel >= LOBSTER_LEVEL ->
                when (
                    choice5(
                        "Small Net Fish",
                        1,
                        "Big Net Fish",
                        2,
                        "Rod and Fly Fishing",
                        3,
                        "Lobster pot",
                        4,
                        "Tell me about...",
                        5,
                    )
                ) {
                    1 -> smallNetFish()
                    2 -> bigNetFish()
                    3 -> rodAndFlyFish()
                    4 -> lobsterPotFish()
                    5 -> tutorMenu()
                }
            else ->
                when (
                    choice4(
                        "Small Net Fish",
                        1,
                        "Big Net Fish",
                        2,
                        "Rod and Fly Fishing",
                        3,
                        "Tell me about...",
                        4,
                    )
                ) {
                    1 -> smallNetFish()
                    2 -> bigNetFish()
                    3 -> rodAndFlyFish()
                    4 -> tutorMenu()
                }
        }
    }

    private suspend fun Dialogue.smallNetFish() {
        objbox(
            SMALL_NET,
            "Ahoy, small net fishin' you can do just south of Draynor " +
                "Village and in these very spots here. Aye.",
        )
        objbox(
            "obj.shrimp",
            "Shrimp and anchovies can be caught with your small fishin' net.",
        )
        fishMenu()
    }

    private suspend fun Dialogue.bigNetFish() {
        objbox(
            "obj.big_net",
            "Aye, you can net yourself some big fish in Catherby, which is " +
                "a good place to fish for most things, Gar!",
        )
        objbox(
            "obj.mackerel",
            "Mackrel and Cod will form the backbone of your catch when " +
                "big net fishin'.. except for the added extras...",
        )
        chatNpc(
            neutral,
            "Some rich rewards for big net fishin', make sure you be using " +
                "a big net fishing spot though...",
        )
        fishMenu()
    }

    private suspend fun Dialogue.rodAndFlyFish() {
        objbox(
            "obj.fishing_rod",
            "Aye, rod fishin' can be practiced here at these spots, as well " +
                "as south of Draynor Village and in the Lumbridge river, " +
                "depending upon your experience. You can get bait at any " +
                "fishin' shop, there be one in Port Sarim.",
        )
        objbox(
            "obj.pike",
            "With a rod you can catch pike, sardines and herring. Good eating on them.",
        )
        objbox(
            "obj.fly_fishing_rod",
            "The art of fly fishin' can be done in rivers, so the Lumbridge river here would suffice.",
        )
        objbox("obj.salmon", "Aye, you can catch yourself a delicious trout or salmon.")
        fishMenu()
    }

    private suspend fun Dialogue.lobsterPotFish() {
        objbox(
            "obj.lobster_pot",
            "Arrr, Lobster pots can be used from the pier on the island of Karamja.",
        )
        objbox(
            "obj.lobster",
            "In Lobster pots you can catch lobsters. They're tasty after thems cooked.",
        )
        chatPlayer(quiz, "Is that all?")
        fishMenu()
    }

    private suspend fun Dialogue.harpoonFish() {
        objbox(
            "obj.harpoon",
            "Arrr, you can also use your harpoon from the pier on the island of Karamja.",
        )
        doubleobjbox("obj.tuna", "obj.swordfish", "With it you can catch Tuna and Swordfish.")
        objbox("obj.shark", "... and shark.")
        chatNpc(
            neutral,
            "Ahoy, there's also the fishin' guild if ya skilled enough, you " +
                "can find it south west of Seers' Village.",
        )
        fishMenu()
    }

    private suspend fun Dialogue.whereAndWhat() {
        chatPlayer(quiz, "Where and what should I fish?")
        when (player.baseFishingLvl) {
            in 1..9 -> parrotAdvice()
            in 10..15 ->
                chatNpc(
                    neutral,
                    "Herrin' can be fished from Catherby and some other places when you reach level 10.",
                )
            in 16..22 ->
                chatNpc(
                    neutral,
                    "You can use a big net to catch Mackerel from Catherby when you reach level 16.",
                )
            in 23..27 ->
                chatNpc(
                    neutral,
                    "Cod can be fished from Catherby and some other places once you reach level 23.",
                )
            in 28..37 ->
                chatNpc(
                    neutral,
                    "You can use your fishin' rod and some bait to catch Slimy Eel in the swamps at level 28.",
                )
            in 38..45 ->
                chatNpc(
                    neutral,
                    "You can use your fishin' rod and some bait to catch Cave " +
                        "Eel in the caves below Lumbridge Swamp at level 38.",
                )
            in 46..49 ->
                chatNpc(
                    neutral,
                    "Bass can be caught at level 46 in your big net if you wander along to Catherby.",
                )
            else -> {
                chatNpc(
                    neutral,
                    "Tuna and Swordfish can be harpooned - if you're good " +
                        "enough - from the thrivin' fishing village of Catherby, " +
                        "or if you can get in try the Fishin' Guild. Level 35 for " +
                        "Tuna and 50 for Swordfish.",
                )
                chatNpc(
                    neutral,
                    "You can also go talk to Murphy and see if you can use his " +
                        "trawler to catch Manta Ray and Sea Turtle if you're really good... Gar!",
                )
            }
        }
        tutorMenu()
    }

    private suspend fun Dialogue.parrotAdvice() {
        chatNpc(
            neutral,
            "A young and enthusiastic fisher. Try south of Draynor Village " +
                "or the pier on the island of Karamja to fish for Shrimp at " +
                "level 1 or Sardines at level 5. Aye, me parrot concurs.",
        )
        chatPlayer(confused, "What parrot?")
        chatNpc(happy, "Me parrot Percy on me shoulder thar! Arr!")
        chatPlayer(confused, "Oookay...sure, a parrot, on your shoulder.")
        chatNpc(happy, "Arrr!")
    }

    private companion object {
        private const val SMALL_NET = "obj.net"
        private const val INTERMEDIATE = 29
        private const val ADVANCED = 39
        private const val LOBSTER_LEVEL = 25
        private const val HARPOON_LEVEL = 35
    }
}
