package org.rsmod.content.areas.misc.lobbies.troublebrewing.npcs

import dev.openrune.util.Wearpos
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:trouble_brewing] joining a crew once the hat and inventory checks pass
//  (team balance, cat and follower checks, team hat and the waiting room teleport), the
//  Join-Crew option and the recruiting overheads
// TODO(content): [verify:in_game] when the under-40 Cooking reply replaces the join dialogue
class BrewingCaptains : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.san_fan") { startDialogue(it.npc) { sanFan() } }
        onOpNpc1("npc.fancy_dan") { startDialogue(it.npc) { fancyDan() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:San_Fan
    private suspend fun Dialogue.sanFan() {
        chatNpc(
            neutral,
            "Hey you, you here for a reason or do you just like hanging around?",
        )
        if (joinOrAsk()) {
            sanFanJoin()
            return
        }
        chatPlayer(quiz, "Who are you, and what is this place?")
        chatNpc(laugh, "Well, well, looks like someone is fresh off the boat and no mistake!")
        chatNpc(
            neutral,
            "A little while ago we had some real problem shipping 'rum' in the island. The " +
                "Braindeath crew said it had something to do with zombies and the like.",
        )
        chatNpc(
            neutral,
            "All the pirates on the island got together and decided to make our own still, but " +
                "of course things got a little out of hand, and now we have two.",
        )
        chatPlayer(quiz, "So with two stills you have plenty of 'rum' right?")
        chatNpc(
            angry,
            "Well we would do, if that scurvy, whelk-eating foppish dog wouldn't keep trying to " +
                "burn our still to the ground!",
        )
        chatPlayer(shocked, "I say!")
        chatNpc(
            neutral,
            "Neither of us can really corner the 'rum' market at the moment, but we are giving " +
                "it our best shot.",
        )
        chatNpc(
            neutral,
            "The code of the island says that we can't attack each other, but after consulting " +
                "with our fearsome pirate lawyers, we have discovered that destroying each " +
                "other's property is fine.",
        )
        chatNpc(
            angry,
            "Hence, that sniveling, hat-wearing cur keeps trying to burn down my still.",
        )
        chatNpc(
            neutral,
            "Of course he claims I started this whole thing, but who would trust him anyway?",
        )
        canIHelpOut()
    }

    private suspend fun Dialogue.sanFanJoin() {
        if (player.cookingLvl < COOKING_LEVEL) {
            chatNpc(neutral, "Sorry, I don't take anyone that can't cook.")
            chatPlayer(quiz, "Why not?")
            chatNpc(
                neutral,
                "Aside from anything else, if you have less than 40 Cooking, then you don't have " +
                    "much of an idea of health and safety when it comes to food.",
            )
            chatPlayer(
                laugh,
                "Oh yeah, Saradomin forbid that the 'rum' made from whatever is lying around " +
                    "gets a bit of hair in it.",
            )
            return
        }
        chatNpc(neutral, "Well you say that now, but I wonder if you can measure up.")
        if (player.worn[Wearpos.Hat.slot] != null) {
            chatNpc(
                neutral,
                "Well, for a start off, come and see me when you don't have that thing on your " +
                    "head.",
            )
            chatPlayer(quiz, "Why can't I help out while wearing this?")
            chatNpc(
                neutral,
                "I need to know you aren't a spy for the other crew, so you'll need to wear a " +
                    "blue bandana.",
            )
            chatNpc(
                neutral,
                "If you still want to join my crew then go deposit it in the deposit box and " +
                    "come back without anything on your head.",
            )
            return
        }
        if (!player.inv.isEmpty()) {
            chatNpc(laugh, "Hah, think that I'm going to assign you the easy tasks eh?")
            chatPlayer(quiz, "What do you mean?")
            chatNpc(
                neutral,
                "Well, I can see you have some items in your inventory. If you want to join my " +
                    "crew you will need all of it free so you are able to work as hard as " +
                    "everyone else.",
            )
            chatNpc(
                neutral,
                "There is a bank deposit box next to that greasy oaf Jimmy's stall, go and use " +
                    "it and then come back.",
            )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fancy_Dan
    private suspend fun Dialogue.fancyDan() {
        chatNpc(happy, "Arr!")
        chatNpc(happy, "Is there something I can help ye with?")
        if (joinOrAsk()) {
            fancyDanJoin()
            return
        }
        chatPlayer(quiz, "Who are you, and what is this place?")
        chatNpc(laugh, "Arrharrharr!")
        chatNpc(
            happy,
            "Do you not recognise me? I am the famous dandy pirate 'Fancy' Dan!",
        )
        chatPlayer(neutral, "Yes, of course, any pirate knows who you are.")
        chatNpc(happy, "But of course.")
        chatNpc(
            happy,
            "As for what this place is, it is my glorious still, producing the best quality " +
                "'rum' this side of Braindeath Island.",
        )
        chatNpc(
            neutral,
            "Oh, and the run-down swill-producing ditch that San Fan over there calls a still.",
        )
        chatPlayer(quiz, "So with two stills you have plenty of 'rum' right?")
        chatNpc(
            angry,
            "Well we would do, if that hatchet-faced harridan wouldn't keep trying to torch the " +
                "place!",
        )
        chatPlayer(shocked, "I say!")
        chatNpc(
            neutral,
            "A little while ago we had some real problems getting rum to the island, hence the " +
                "two stills.",
        )
        chatNpc(
            neutral,
            "However, after a little misunderstanding involving San's still and a barrel of " +
                "gunpowder our relationship seems to have become a little strained.",
        )
        chatNpc(
            neutral,
            "Apparently it is perfectly within the pirate code to burn each others stills, so " +
                "long as nobody directly attacks the other person's crew, so rum production has " +
                "taken a slight dip of late.",
        )
        canIHelpOut()
    }

    private suspend fun Dialogue.fancyDanJoin() {
        if (player.cookingLvl < COOKING_LEVEL) {
            chatNpc(quiz, "So, how well can you cook?")
            chatPlayer(worried, "Well, I can, err, cook a bit...")
            chatNpc(
                neutral,
                "Well 'a bit' doesn't cut it in here. Come back when you have a cooking level of " +
                    "40.",
            )
            return
        }
        chatNpc(happy, "Of course you would!")
        if (player.worn[Wearpos.Hat.slot] != null) {
            chatNpc(
                neutral,
                "However, I don't let anyone on the team if they are wearing a hat or a helmet.",
            )
            chatNpc(neutral, "You can go and store it in the deposit box next to Jimmy's stall.")
            chatPlayer(quiz, "Why not?")
            chatNpc(
                neutral,
                "Well, in order to tell my crew from San Fan's I will need to give you a hat " +
                    "like mine.",
            )
            chatNpc(
                neutral,
                "If you are wearing a hat it might get lost when I give you my crew's hat.",
            )
            chatNpc(neutral, "You wouldn't want that would you?")
            chatPlayer(neutral, "No, that sounds very reasonable in fact.")
            return
        }
        if (!player.inv.isEmpty()) {
            chatNpc(
                neutral,
                "Well, I'll be glad to have you aboard, but you will need to go and store all of " +
                    "your gear first.",
            )
            chatPlayer(quiz, "What do you mean?")
            chatNpc(
                neutral,
                "Well, you will need all of your inventory space for the ingredients, and all of " +
                    "the other things I'll need you to haul about.",
            )
            chatNpc(
                neutral,
                "There is a bank deposit box next to Jimmy's stall if you need it. Come and see " +
                    "me when you have a fully empty inventory.",
            )
        }
    }

    private suspend fun Dialogue.joinOrAsk(): Boolean {
        val join =
            choice2(
                "I would like to join your crew.",
                true,
                "Who are you, and what is this place?",
                false,
            )
        if (join) {
            chatPlayer(happy, "I would like to join your crew.")
        }
        return join
    }

    private suspend fun Dialogue.canIHelpOut() {
        chatPlayer(quiz, "Can I help out?")
        chatNpc(
            neutral,
            "Yes, probably. I have a bunch of jobs that need doing, if you feel up to it.",
        )
        chatNpc(neutral, "What would you like to know about?")
        while (true) {
            when (menu(listOf(BREWING, REPAIRING, SABOTAGING, PAY, HEARD_ENOUGH))) {
                BREWING -> brewing()
                REPAIRING -> if (!repairing()) return
                SABOTAGING -> sabotaging()
                PAY -> pay()
                HEARD_ENOUGH -> {
                    heardEnough()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.brewing() {
        chatPlayer(quiz, "How do I help you brew 'Rum'?")
        chatNpc(
            neutral,
            "Well the stills we have set up are based on the ones on Braindeath island... well " +
                "roughly at any rate.",
        )
        chatNpc(
            neutral,
            "All you have to do is help put the ingredients into the hoppers and keep the " +
                "boilers burning.",
        )
        chatNpc(
            neutral,
            "If you add enough ingredients to the hoppers then you have the ability to pick up a " +
                "single bottle of rum when it brews.",
        )
        chatNpc(
            neutral,
            "So, if ye add 5 buckets of water to the hoppers, then ye get access to one bottle. " +
                "If you add 3 bowls of coloured water you get access to one bottle, and if you " +
                "add one of anything else you get access to one bottle.",
        )
        chatNpc(
            neutral,
            "This is cumulative, meaning that if you add (for example) 10 buckets of water, 3 " +
                "sweetgrubs and 6 bowls of coloured water, then you have a chance to collect 7 " +
                "bottles of rum for the team.",
        )
        chatNpc(
            neutral,
            "The boilers are easy as pie to sort out. You just put some logs in, light them with " +
                "a tinderbox, and then keep putting logs in until they are good and hot.",
        )
        chatNpc(
            neutral,
            "Three logs should do it, but it's best to keep heaping them in just in case.",
        )
        chatNpc(
            neutral,
            "The ingredients are; Bitternuts, Scrapey Tree Bark, Sweetgrubs, Buckets of Water " +
                "and some Coloured Water.",
        )
        var last: String? = null
        while (true) {
            val options =
                listOfNotNull(last?.let { ASK_ELSE }) + BREWING_TOPICS.filter { it != last }
            val topic = menu(options)
            when (topic) {
                ASK_ELSE -> {
                    askSomethingElse()
                    return
                }
                BITTERNUTS -> bitternuts()
                SCRAPEY_BARK -> scrapeyBark()
                SWEETGRUBS -> sweetgrubs()
                BUCKETS -> buckets()
                COLOURED_WATER -> colouredWater()
            }
            last = topic
        }
    }

    private suspend fun Dialogue.bitternuts() {
        chatPlayer(quiz, "Where can I get Bitternuts?")
        chatNpc(
            neutral,
            "There are plenty of Bitternut trees inside this walled off area. The problem is " +
                "that the trees are too tricky for a human to climb.",
        )
        chatNpc(
            neutral,
            "You will need to catch a monkey and send it up the tree to get the nut " +
                "for you.",
        )
        chatPlayer(confused, "Send it up the tree? How does that work?")
        chatNpc(
            neutral,
            "Well, if you were a monkey, and some random person grabbed you, dumped some dye in " +
                "your fur and shoved you up a tree, you would hurl the closest heavy object at " +
                "them too.",
        )
        chatPlayer(confused, "Wait a second, dye?")
        chatNpc(
            neutral,
            "Yes, you'll need to get a bowl of coloured water and pour it on the monkey. " +
                "Otherwise you won't be able to tell which team the monkey belongs too.",
        )
        chatPlayer(confused, "That makes sense...I think...")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.scrapeyBark() {
        chatPlayer(
            worried,
            "Where can I get Scrapey tree bark...and why do I get the feeling I won't like the " +
                "answer?",
        )
        chatNpc(
            neutral,
            "Inside this walled area there are a few Scrapey trees. They are the big, ill " +
                "looking ones, you can't miss them.",
        )
        chatNpc(neutral, "Basically, you cut them down and then carve the bark off with a knife.")
        chatNpc(
            neutral,
            "It's pretty simple, except the sap tends so make people feel kind off ill if they " +
                "get any on themselves.",
        )
        chatPlayer(worried, "How ill?")
        chatNpc(neutral, "Don't worry, we aren't talking deadly ill or anything.")
        chatNpc(neutral, "It's more like you'll feel quite dizzy and weak for a bit.")
        chatPlayer(sad, "Joy...")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.sweetgrubs() {
        chatPlayer(confused, "Sweetgrubs? What are they?")
        chatNpc(
            neutral,
            "Well, lacking any sluglings in the area, we add these little grubs called " +
                "Sweetgrubs. If you ignore the gag- inducing aftertaste, they are quite sweet.",
        )
        chatPlayer(shocked, "How... how can you drink this stuff?")
        chatNpc(laugh, "Quite easily!")
        chatNpc(happy, "Pirates are renowned as being tough for a reason.")
        chatPlayer(quiz, "So where do I get these grubs from anyway?")
        chatNpc(
            neutral,
            "Well, there are a few sweetgrub mounds inside there. We have a handful of rat meat " +
                "that you can use on the mounds to draw the grubs to the surface.",
        )
        chatNpc(neutral, "Mind your fingers though, they are voracious little things.")
        chatPlayer(
            sad,
            "And here was me thinking that nothing could be more disgusting than Braindeath " +
                "'Rum'.",
        )
        val title = if (access.isBodyTypeA()) "lad" else "lass"
        chatNpcSpecific(
            BRAINDEATH_TITLE,
            BRAINDEATH,
            happy,
            "Arr, that's where ye'd be wrong $title, 'cos new, improved Braindeath 'Rum' Classic " +
                "is now 200% more disgustin' than any leadin' competitor!",
        )
        chatNpcSpecific(
            BRAINDEATH_TITLE,
            BRAINDEATH,
            neutral,
            "Legal disclaimer; the dangerous amount of disgustin' in new Braindeath 'Rum' " +
                "Classic be too much for pregnant women, donkeys and anyone with more than seven " +
                "letters in their last name.",
        )
        chatPlayer(confused, "What was that?")
        chatNpc(confused, "What was what?")
        chatNpc(neutral, "Is there anything else?")
    }

    private suspend fun Dialogue.buckets() {
        chatPlayer(quiz, "How do I get...")
        chatPlayer(
            worried,
            "Wait, no, that question makes me sound like I haven't a clue what I'm doing.",
        )
        chatPlayer(quiz, "Where can I get buckets in there?")
        chatNpc(neutral, "There are a few lying about. Help yourself.")
        chatNpc(
            neutral,
            "There is a pump in the still, so long as it's working you should have almost " +
                "unlimited water.",
        )
        chatPlayer(
            worried,
            "Hold on, do I have to add anything disgusting and/or potential lethal to the water " +
                "before I pour it in?",
        )
        chatNpc(shocked, "Perish the thought!")
        chatPlayer(neutral, "Ok, just checking.")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.colouredWater() {
        chatPlayer(quiz, "What sort of coloured water are we talking about here?")
        chatNpc(
            neutral,
            "Well, the other still has an area of waste ground with a flower growing on it. If " +
                "you go and grab one of the flowers and jam it in the kettle, the water will " +
                "boil out the colour.",
        )
        chatNpc(neutral, "We use the colour to keep our rum easily identifiable.")
        chatPlayer(quiz, "So I just empty the kettle into the hopper, right?")
        chatNpc(neutral, "No, no, you'll need to take an empty bowl and fill it from the kettle.")
        chatNpc(neutral, "That way you can use the stuff on a monkey if you need to.")
        chatPlayer(confused, "Use it on a monkey...of course.")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.repairing(): Boolean {
        chatPlayer(quiz, "How do I repair the still?")
        chatNpc(
            neutral,
            "Well, assuming those scurvy dogs have managed to set any of it on fire, then you'll " +
                "need to tip a bucket of water on it before you can repair it.",
        )
        chatNpc(neutral, "Then, it all comes down to what part of it was damaged.")
        var last: String? = null
        while (true) {
            val options =
                listOfNotNull(last?.let { ASK_ELSE }) +
                    REPAIR_TOPICS.filter { it != last } +
                    HEARD_ENOUGH
            val topic = menu(options)
            when (topic) {
                ASK_ELSE -> {
                    askSomethingElse()
                    return true
                }
                HOPPERS -> hoppers()
                PIPES -> pipes()
                BRIDGES -> bridges()
                REPAIR_ITEMS -> {
                    repairItems()
                    return true
                }
                HEARD_ENOUGH -> {
                    heardEnough()
                    return false
                }
            }
            last = topic
        }
    }

    private suspend fun Dialogue.hoppers() {
        chatPlayer(quiz, "How can I repair hoppers?")
        chatNpc(
            neutral,
            "Well, assuming that they are not on fire, all you need to do is use the heavy " +
                "lumber patches. They are big enough to cover most holes.",
        )
        chatPlayer(quiz, "Will I need a hammer?")
        chatNpc(neutral, "No, just shove it into place and whack it until it lines up.")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.pipes() {
        chatPlayer(quiz, "How do I repair pipes and the pump?")
        chatNpc(
            neutral,
            "Well, if you use a pipe section on the damaged part you should be able to replace " +
                "it easily.",
        )
        chatPlayer(quiz, "Will I need a hammer?")
        chatNpc(
            neutral,
            "No, likely you'll just need to whack the old one out and slot the new one in with " +
                "your hands.",
        )
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.bridges() {
        chatPlayer(quiz, "How do I repair bridges?")
        chatNpc(
            neutral,
            "Well, we have these handy bridge sections you can use to repair the " +
                "bridges.",
        )
        chatNpc(
            neutral,
            "The main problem is, while a still will keep working if a bit of it is only " +
                "damaged, the bridges are completely out of commission if they are even slightly " +
                "damaged.",
        )
        chatPlayer(quiz, "Is that going to be a problem? I mean, I can jump across I think.")
        chatNpc(
            shocked,
            "Don't try that! Our lawyers tell us we aren't covered for injuries sustained while " +
                "jumping over burned bridges.",
        )
        chatNpc(neutral, "Seriously, just keep off the bridges until they are fixed.")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.repairItems() {
        chatPlayer(quiz, "Where do I get all this stuff anyway?")
        chatNpc(
            neutral,
            "We have a load of items already on a table inside the still. You can also make new " +
                "ones if you grab some logs.",
        )
        chatPlayer(quiz, "How does that work?")
        chatNpc(
            neutral,
            "We have a work table set up in the still. If you use the logs in it you can craft " +
                "up a handy replacement for part of the still.",
        )
        chatNpc(
            neutral,
            "Don't worry about them fitting, the things are held together by nails, replacement " +
                "parts and soot by now.",
        )
        chatPlayer(worried, "Well...how sanitary...")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.sabotaging() {
        chatPlayer(quiz, "So...how would I go about sabotaging the enemy still?")
        chatNpc(neutral, "Ok, here's what you need to do.")
        chatNpc(
            neutral,
            "The wood around here is really dry and flammable, so take one of the torches, light " +
                "it up, and then go and attack the enemy still with it.",
        )
        chatPlayer(quiz, "That's it?")
        chatNpc(
            neutral,
            "Most of their still is soaked in 'alcohol' and made from tinder dry " +
                "wood.",
        )
        chatNpc(
            neutral,
            "Essentially, if it isn't ours, try using a torch on it and see what " +
                "happens.",
        )
        chatPlayer(neutral, "Ok, I get you.")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.pay() {
        chatPlayer(quiz, "So what will you give me for helping out?")
        chatNpc(laugh, "Hahahaha! What a mercenary attitude! You'll do fine here.")
        chatNpc(
            neutral,
            "Well, for each bottle that your team produces, I'll give you one piece of eight.",
        )
        chatNpc(
            neutral,
            "If you manage to set something on fire then I'll toss in another, same for " +
                "repairing the still.",
        )
        chatPlayer(quiz, "What can I use these pieces of eight for?")
        chatNpc(neutral, "See Honest Jimmy there? He'll trade you some pretty nice stuff for them.")
        chatPlayer(happy, "Great!")
        chatNpc(neutral, "Anything else?")
    }

    private suspend fun Dialogue.askSomethingElse() {
        chatPlayer(quiz, "Can I ask about something else?")
        chatNpc(neutral, "Ok, what else?")
    }

    private suspend fun Dialogue.heardEnough() {
        chatPlayer(neutral, "Ok, I think I've heard enough.")
        chatNpc(neutral, "Fine. Glad I could help.")
    }

    private suspend fun Dialogue.menu(options: List<String>): String =
        when (options.size) {
            2 -> choice2(options[0], options[0], options[1], options[1])
            3 -> choice3(options[0], options[0], options[1], options[1], options[2], options[2])
            4 ->
                choice4(
                    options[0],
                    options[0],
                    options[1],
                    options[1],
                    options[2],
                    options[2],
                    options[3],
                    options[3],
                )
            5 ->
                choice5(
                    options[0],
                    options[0],
                    options[1],
                    options[1],
                    options[2],
                    options[2],
                    options[3],
                    options[3],
                    options[4],
                    options[4],
                )
            else -> error("Unsupported option count: ${options.size}")
        }

    private companion object {
        const val COOKING_LEVEL = 40
        const val BRAINDEATH = "npc.deal_captian_braindeath"
        const val BRAINDEATH_TITLE = "Captain Braindeath"

        const val BREWING = "Brewing 'Rum'"
        const val REPAIRING = "Repairing the Still"
        const val SABOTAGING = "Sabotaging the Still"
        const val PAY = "What will you pay me?"
        const val HEARD_ENOUGH = "Ok, I think I've heard enough"
        const val ASK_ELSE = "Can I ask about something else?"

        const val BITTERNUTS = "Where can I get Bitternuts?"
        const val SCRAPEY_BARK = "Where can I get Scrapey Tree Bark?"
        const val SWEETGRUBS = "Where can I get Sweetgrubs?"
        const val BUCKETS = "Where can I get buckets of water?"
        const val COLOURED_WATER = "Where can I get coloured water?"
        val BREWING_TOPICS = listOf(BITTERNUTS, SCRAPEY_BARK, SWEETGRUBS, BUCKETS, COLOURED_WATER)

        const val HOPPERS = "How do I repair Hoppers?"
        const val PIPES = "How do I repair Pipes and the pump?"
        const val BRIDGES = "How do I repair Bridges?"
        const val REPAIR_ITEMS = "How do I get repair items?"
        val REPAIR_TOPICS = listOf(HOPPERS, PIPES, BRIDGES, REPAIR_ITEMS)
    }
}
