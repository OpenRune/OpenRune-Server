package org.rsmod.content.areas.city.portsarim.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RustyAnchorScript @Inject constructor(private val dragonSlayer: DragonSlayerQuest) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sarim_pub_sitting_patron") { startDialogue(it.npc) { ahab() } }
        onOpNpc1("npc.rustyanchor_bartender") { startDialogue(it.npc) { bartender() } }
        onOpNpc1(JACK_SEAGULL) { startDialogue(it.npc) { pirateDrinker(JACK_SEAGULL_NAME) } }
        onOpNpc1(LONGBOW_BEN) { startDialogue(it.npc) { pirateDrinker(LONGBOW_BEN_NAME) } }
    }

    private suspend fun Dialogue.ahab() {
        chatNpc(drunk, "Arrr, matey!")
        while (true) {
            val options = buildList {
                if (!foundRedbeard()) add("I'm looking for Redbeard Frank." to AhabTopic.Redbeard)
                add("Arrr!" to AhabTopic.Arrr)
                if (dragonSlayer.isSeekingCrandor(player)) {
                    add("Can you help me get to Crandor Island?" to AhabTopic.Crandor)
                }
                add("Are you going to sit there all day?" to AhabTopic.SitAllDay)
                add("Do you want to trade?" to AhabTopic.Trade)
            }
            when (menu(options)) {
                AhabTopic.Redbeard -> return lookingForRedbeard()
                AhabTopic.Arrr -> {
                    chatPlayer(happy, "Arrr!")
                    chatNpc(drunk, "Arrr, matey!")
                }
                AhabTopic.SitAllDay -> return ahabsShip()
                AhabTopic.Crandor -> return ahabCrandor()
                AhabTopic.Trade -> {
                    chatPlayer(quiz, "Do you have anything for trade?")
                    chatNpc(neutral, CUSTOMS_REPLY)
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.ahabCrandor() {
        chatPlayer(quiz, "Can you help me get to Crandor Island?")
        chatNpc(drunk, "Arrr, matey, ye canna be serious! That be the home of Elvarg the Dragon!")
        chatPlayer(
            neutral,
            "Yes, that's right. I 've got to go there and kill the dragon for this quest I'm doing.",
        )
        chatNpc(
            sad,
            "I canna help ye. And right sorry I be to see a fine young ${laddie()} like yerself go " +
                "off to yer doom.",
        )
        chatPlayer(happy, "Thanks, but I'll be fine!")
        chatNpc(drunk, "Arrr...")
    }

    private suspend fun Dialogue.ahabsShip() {
        chatPlayer(quiz, "Are you going to sit there all day?")
        chatNpc(angry, "Aye, I am. I canna walk, ye see.")
        chatPlayer(quiz, "What's stopping you from walking?")
        chatNpc(sad, "Arrr, I 'ave only the one leg! I lost its twin when my last ship went down.")
        chatPlayer(confused, "But I can see both your legs!")
        chatNpc(
            sad,
            "Nay, young ${laddie()}, this be a false leg. For years I had me a sturdy wooden " +
                "peg-leg, but now I wear this dainty little feller.",
        )
        chatNpc(neutral, "Yon peg-leg kept getting stuck in the floorboards.")
        chatPlayer(confused, "Right...")
        val youngOne = if (isLad()) "bright young 'un" else "bonnie young lassie"
        chatNpc(
            quiz,
            "Perhaps a $youngOne like yerself would like to help me? I be needing another ship " +
                "to go a-hunting my enemy.",
        )
        if (dragonSlayer.isComplete(player)) {
            chatPlayer(happy, "Well, I do have a ship that I'm not using. It's the Lady Lumbridge.")
            chatNpc(happy, "Arrr! That ship be known to me, and a fine lass she is.")
            chatPlayer(shifty, "I suppose she might be...")
            chatNpc(quiz, "So would ye be kind enough to let me take her out to sea?")
            chatPlayer(quiz, "I had to pay 2000 gp for that ship. Have you got that much?")
        } else {
            chatPlayer(quiz, "Hmmm. And can you afford another ship?")
        }
        chatNpc(
            sad,
            "Nay, I have nary a penny to my name. All my worldly goods went down with me old ship.",
        )
        chatPlayer(quiz, "So you're actually asking me to give you a free ship.")
        chatNpc(happy, "Arrr! Would ye be so kind?")
        chatPlayer(angry, "No I jolly well wouldn't!")
        chatNpc(sad, "Arrr.")
    }

    private suspend fun Dialogue.bartender() {
        if (choice2("Could I buy a beer please?", true, "Have you heard any rumours here?", false)) {
            chatPlayer(happy, "Could I buy a beer please?")
            chatNpc(happy, "Sure, that will be $BEER_PRICE gold coins please.")
            val inv = access.inv
            if (inv.count("obj.coins") < BEER_PRICE) {
                chatPlayer(sad, "I don't have enough coins.")
                return
            }
            chatPlayer(happy, "Ok, here you go.")
            if (access.invDel(inv, "obj.coins", BEER_PRICE).failure) {
                return
            }
            access.invAdd(inv, "obj.beer")
            mesbox("You buy a pint of beer!")
            return
        }
        chatPlayer(neutral, "Have you heard any rumours here?")
        if (QuestRequirements.hasCompleted(player, GOBLIN_DIPLOMACY)) {
            chatNpc(neutral, "No, it hasn't been very busy lately.")
            return
        }
        chatNpc(
            neutral,
            "Well, there was a guy in here earlier saying the goblins up by the mountain are " +
                "arguing again, about the colour of their armour of all things.",
        )
        chatNpc(
            neutral,
            "Knowing the goblins it could easily turn into a full blown war, which wouldn't be " +
                "good. Goblin wars make such a mess of the countryside.",
        )
        chatPlayer(
            neutral,
            "Well if I have the time I'll go and see if I can knock some sense into them.",
        )
    }

    private suspend fun Dialogue.pirateDrinker(name: String) {
        if (access.random.of(2) == 0) {
            return piratesBicker()
        }
        chatNpc(drunk, "Arrr, matey!")
        val options = buildList {
            if (!foundRedbeard()) add("I'm looking for Redbeard Frank." to DrinkerTopic.Redbeard)
            if (name == LONGBOW_BEN_NAME) {
                add("Why are you called Longbow Ben?" to DrinkerTopic.Name)
            } else {
                add("What are you doing here?" to DrinkerTopic.Doing)
            }
            add("Have you got any quests I could do?" to DrinkerTopic.Quests)
            if (dragonSlayer.isSeekingCrandor(player)) {
                add("Can you help me get to Crandor Island?" to DrinkerTopic.Crandor)
            }
        }
        when (menu(options)) {
            DrinkerTopic.Redbeard -> lookingForRedbeard()
            DrinkerTopic.Doing -> {
                chatPlayer(quiz, "What are you doing here?")
                chatNpc(drunk, "Drinking.")
                chatPlayer(neutral, "Fair enough.")
            }
            DrinkerTopic.Name -> longbowBenStory()
            DrinkerTopic.Crandor -> drinkerCrandor(name)
            DrinkerTopic.Quests -> {
                chatPlayer(happy, "Have you got any quests I could do?")
                chatNpc(sad, questHint())
                chatPlayer(happy, "Thanks.")
            }
        }
    }

    private suspend fun Dialogue.drinkerCrandor(name: String) {
        chatPlayer(quiz, "Can you help me get to Crandor Island?")
        if (name == LONGBOW_BEN_NAME) {
            chatNpc(
                drunk,
                "Arrr, ye may be wanting to go to Crandor Island now, but if ye gets there, ye'll be " +
                    "a-wantin' to get off it again quickly enough!",
            )
            chatPlayer(quiz, "So that's a 'No', then?")
            chatNpc(drunk, "Aye, ${laddie()}, that it is.")
            chatPlayer(quiz, "Do you know anyone who would take me there?")
            chatNpc(
                drunk,
                "There's only one sailor alive daft enough to go there. Old Ned, the ropemaker in " +
                    "Draynor Village. I can't think of any other fools just now. Except for old " +
                    "Jack Seagull there.",
            )
            chatNpcSpecific(JACK_SEAGULL_NAME, JACK_SEAGULL, angry, "Arrr, who are ye calling old?")
        } else {
            chatNpc(drunk, "Crandor Island, ye say?")
            chatNpc(angry, "Arrr, keep yeself away from that accursed place if ye values yer skin!")
            chatPlayer(quiz, "So that's a 'No', then?")
            chatNpc(drunk, "Aye, that it be.")
            chatPlayer(quiz, "Do you know anyone who would take me there?")
            chatNpc(drunk, "No, I don't know many idiots. Except for young Ben there.")
            chatNpcSpecific(
                LONGBOW_BEN_NAME,
                LONGBOW_BEN,
                angry,
                "Arrr, get away with ye, ye scurvy scallywag!",
            )
        }
        chatPlayer(neutral, "Okay, thanks anyway.")
    }

    private suspend fun Dialogue.longbowBenStory() {
        chatPlayer(quiz, "Why are you called Longbow Ben?")
        chatNpc(neutral, "Arrr, that's a strange yarn.")
        chatNpc(
            neutral,
            "I was to be marooned, ye see. A scurvy troublemaker had taken my ship, and he put " +
                "me ashore on a little island.",
        )
        chatPlayer(quiz, "Gosh, how did you escape?")
        chatNpc(
            happy,
            "Arrr, ye see, he made one mistake! Before he sailed away, he gave me a bow and one " +
                "arrow so that I wouldn't have to die slowly.",
        )
        chatNpc(laugh, "So I shot him and took my ship back.")
        chatPlayer(confused, "Right...")
    }

    private suspend fun Dialogue.piratesBicker() {
        chatNpcSpecific(JACK_SEAGULL_NAME, JACK_SEAGULL, drunk, "Arrr, matey!")
        chatNpcSpecific(LONGBOW_BEN_NAME, LONGBOW_BEN, happy, "Yo ho ho!")
        chatPlayer(happy, "So are you pirates?")
        chatNpcSpecific(LONGBOW_BEN_NAME, LONGBOW_BEN, happy, "Aye, ${laddie()}, that we are.")
        chatNpcSpecific(JACK_SEAGULL_NAME, JACK_SEAGULL, happy, "Aye, that we be.")
        chatNpcSpecific(
            LONGBOW_BEN_NAME,
            LONGBOW_BEN,
            angry,
            "Nay, always ye say it wrong! Tis 'we are', not 'we be'.",
        )
        chatNpcSpecific(
            JACK_SEAGULL_NAME,
            JACK_SEAGULL,
            angry,
            "I be a pirate, not a scurvy schoolmaster!",
        )
        chatNpcSpecific(
            LONGBOW_BEN_NAME,
            LONGBOW_BEN,
            angry,
            "Ye be a fool, and a disgrace to piracy.",
        )
        chatNpcSpecific(JACK_SEAGULL_NAME, JACK_SEAGULL, laugh, "Now ye be saying 'be' too!")
        chatNpcSpecific(LONGBOW_BEN_NAME, LONGBOW_BEN, angry, "Arrr! Tis thy fault.")
        chatPlayer(happy, "I think I'll leave you two to sort it out.")
    }

    private suspend fun Dialogue.lookingForRedbeard() {
        chatPlayer(quiz, "I'm looking for Redbeard Frank.")
        chatNpc(
            neutral,
            "Redbeard Frank ye say? He be outside. Says he likes the feel of the wind on his " +
                "cheeks.",
        )
        chatPlayer(happy, "Thanks.")
    }

    private fun Dialogue.questHint(): String {
        val treasure = QuestRequirements.hasCompleted(player, PIRATES_TREASURE)
        val goblins = QuestRequirements.hasCompleted(player, GOBLIN_DIPLOMACY)
        return when {
            !treasure && !goblins ->
                "Nay, but the barkeep hears most of the news around here. Or Redbeard Frank, " +
                    "he's often spoken of buried treasure. Perhaps ye should be asking them for " +
                    "quests."
            !treasure ->
                "Nay, but Redbeard Frank has often spoken of buried treasure. Perhaps ye should " +
                    "be asking him for a quest."
            !goblins ->
                "Nay, but the barkeep hears most of the news around here. Perhaps ye should be " +
                    "asking him for a quest."
            else ->
                "Nay, I've nothing for ye to do. But I hear there's an old landlubber in Draynor " +
                    "Village who's always a-looking for a lively ${if (isLad()) "lad" else "lass"}" +
                    " to do him a favour."
        }
    }

    private fun Dialogue.foundRedbeard(): Boolean =
        QuestRequirements.hasCompleted(player, PIRATES_TREASURE)

    private fun Dialogue.isLad(): Boolean = player.appearance.bodyType == Constants.bodytype_a

    private fun Dialogue.laddie(): String = if (isLad()) "laddie" else "lassie"

    private enum class AhabTopic {
        Redbeard,
        Arrr,
        SitAllDay,
        Trade,
        Crandor,
    }

    private enum class DrinkerTopic {
        Redbeard,
        Doing,
        Name,
        Quests,
        Crandor,
    }

    private companion object {
        const val JACK_SEAGULL = "npc.sarim_pub_drinker_1"
        const val JACK_SEAGULL_NAME = "Jack Seagull"
        const val LONGBOW_BEN = "npc.sarim_pub_drinker_2"
        const val LONGBOW_BEN_NAME = "Longbow Ben"
        const val BEER_PRICE = 2
        const val PIRATES_TREASURE = "quest_piratestreasure"
        const val GOBLIN_DIPLOMACY = "quest_goblindiplomacy"
        const val CUSTOMS_REPLY =
            "Nothin' at the moment, but then again the Customs Agents are on the warpath right now."
    }
}
