package org.rsmod.content.areas.misc.villages.observatory.npcs

import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Observatory @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(PROFESSOR) { startDialogue(it.npc) { professor() } }
        onOpNpc1(ASSISTANT) { startDialogue(it.npc) { assistant() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Observatory_professor
    // TODO(content): [system:met_flags] "I've lost my chart." menu after claiming a chart once
    private suspend fun Dialogue.professor() {
        val completed = QuestRequirements.hasCompleted(player, OBSERVATORY_QUEST)
        chatNpc(happy, if (completed) "Welcome back." else "Hello.")
        when (choice2("Talk about the Observatory quest.", 1, "Talk about Treasure Trails.", 2)) {
            1 -> if (completed) observatoryQuestDone() else observatoryQuest()
            2 -> treasureTrails()
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Observatory_professor (after the quest)
    // TODO(content): [system:met_flags] "Is it true your name is Mambo-duna-roona?" once the
    //  assistant has told the player his name
    private suspend fun Dialogue.observatoryQuestDone() {
        chatNpc(happy, "Thanks for all your help with the telescope. What can I do for you?")
        val moreHelp =
            choice2(
                "Do you need any more help with the telescope?",
                true,
                "Nothing, thanks.",
                false,
            )
        if (moreHelp) {
            chatNpc(neutral, "Not right now,")
            chatNpc(neutral, "but the stars may hold a secret for you.")
        } else {
            chatPlayer(neutral, "Nothing, thanks.")
            chatNpc(happy, "Okay, no problem. See you again.")
        }
    }

    private suspend fun Dialogue.observatoryQuest() {
        chatPlayer(happy, "Hi, I was...")
        chatNpc(
            happy,
            "Welcome to the magnificent wonder of the Observatory, where wonder is all around " +
                "you, where the stars can be clutched from the heavens!",
        )
        chatPlayer(happy, "Wow, nice intro.")
        chatNpc(happy, "Why, thanks! How might I help you?")
        val option =
            choice3(
                "I'm totally lost.",
                1,
                "An Observatory?",
                2,
                "I'm just passing through.",
                3,
            )
        when (option) {
            1 -> {
                chatNpc(
                    neutral,
                    "Lost? It must have been those pesky goblins that led you astray.",
                )
                chatNpc(neutral, "Head north-east to find the city of Ardougne.")
                chatPlayer(happy, "I'm sure I'll find the way.")
                chatPlayer(happy, "Thanks for all your help.")
                chatNpc(happy, "No problem at all. Come and visit again!")
                mesbox("The professor carries on with his studies.")
            }
            2 -> anObservatory()
            3 -> {
                chatPlayer(neutral, "I'm just passing through.")
                chatNpc(
                    neutral,
                    "Fair enough. Not everyone is interested in this place, I suppose.",
                )
            }
        }
    }

    private suspend fun Dialogue.anObservatory() {
        chatPlayer(quiz, "An Observatory?")
        chatNpc(
            happy,
            "Of course. We have a superb telescope up in the Observatory, on the hill.",
        )
        chatNpc(
            happy,
            "A truly marvellous invention, the likes of which you'll never behold again.",
        )
        assistantSays(neutral, "Well, it would be if it worked.")
        chatNpc(angry, "Don't interrupt!")
        chatPlayer(shocked, "What? It doesn't work?")
        chatNpc(
            worried,
            "Oh, no, no, no. Don't listen to him, he's joking. Aren't you, my FAITHFUL assistant?",
        )
        assistantSays(neutral, "Nope, dead serious. Hasn't been working for a long time.")
        chatNpc(angry, "Arghhh! Get back to work and stop sticking your nose in!")
        chatPlayer(quiz, "So, it's broken. How come?")
        chatNpc(
            sad,
            "Oh, I suppose there's no use keeping it secret. Did you see those houses outside?",
        )
        chatPlayer(neutral, "Up on the hill? Yes, I've seen them.")
        chatNpc(angry, "It's a horde of goblins.")
        chatNpc(angry, "Since they moved here they have caused nothing but trouble.")
        chatNpc(sad, "Last week, my telescope was tampered with.")
        chatNpc(
            sad,
            "Now, parts need replacing before it can be used again. They've even been messing " +
                "around in the dungeons under this area. Something needs to be done.",
        )
        assistantSays(neutral, "Strikes me that this visitor could help us.")
        chatNpc(angry, "Stop being so rude.")
        chatNpc(neutral, "...")
        chatNpc(neutral, "Although, he has a point. What do you say?")
        chatPlayer(confused, "What, me?")
        // TODO(content): [quest:observatory_quest] quest start and quest stages
    }

    private suspend fun Dialogue.assistantSays(
        mesanim: MesAnimType,
        text: String,
    ) {
        chatNpcSpecific("Observatory assistant", ASSISTANT, mesanim, text)
    }

    private suspend fun Dialogue.treasureTrails() {
        chatNpc(happy, "Welcome back! How can I help you today?")
        chatPlayer(quiz, "Can you teach me to solve treasure trail clues?")
        chatNpc(
            happy,
            "Ah, I get asked about treasure trails all the time! Listen carefully and I shall " +
                "tell you what I know...",
        )
        chatNpc(
            neutral,
            "Lots of clues have <col=800000>degrees</col> and <col=800000>minutes</col> written " +
                "on them. These are the coordinates of the place where the treasure is buried.",
        )
        chatNpc(
            neutral,
            "You have to walk to the correct spot, so that your coordinates are exactly the same " +
                "as the values written on the clue scroll.",
        )
        chatNpc(
            neutral,
            "To do this, you must use a <col=800000>sextant</col>, a <col=800000>watch</col> and " +
                "a <col=800000>chart</col> to find your own coordinates.",
        )
        chatNpc(
            neutral,
            "Once you know the coordinates of the place where you are, you know which way you " +
                "have to walk to get to the place where the treasure is!",
        )
        chatPlayer(confused, "Riiight. So where do I get those items from?")
        chatNpc(
            neutral,
            "I think Murphy, the owner of the fishing trawler moored south of Ardougne, might be " +
                "able to spare you a sextant. Then the nearest Clock Tower is south of Ardougne " +
                "- you could probably get a watch there. I've",
        )
        chatNpc(happy, "got plenty of charts myself, here have one.")
        if (player.inv.isFull()) {
            chatNpc(
                neutral,
                "You don't have enough space for the chart. Come back to me when you do.",
            )
            return
        }
        player.invAddOrDrop(objRepo, CHART)
        objbox(CHART, "The professor has given you a navigation chart.")
        chatPlayer(happy, "Thanks, I'll see you later.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Observatory_assistant (before the quest)
    // TODO(content): [quest:observatory_quest] after-quest dialogue: the wine gift on the first
    //  talk and "How was the wine?" afterwards need a wine-claimed varbit
    // TODO(content): [system:shooting_stars] board reply for a player who has found a star
    private suspend fun Dialogue.assistant() {
        chatPlayer(quiz, "Hi, are you busy?")
        chatNpc(neutral, "Me? I'm always busy.")
        chatNpc(
            neutral,
            "See that man there? That's the professor. If he had his way, I think he'd never let " +
                "me sleep!",
        )
        chatNpc(neutral, "Anyway, how might I help you?")
        val option =
            choice3(
                "I was wondering what you do here.",
                WONDERING,
                "Just looking around, thanks.",
                LOOKING,
                "Can I have a look through that telescope?",
                TELESCOPE,
            )
        if (option == LOOKING) {
            chatPlayer(neutral, "Just looking around, thanks.")
            chatNpc(
                neutral,
                "Okay, just don't break anything. If you need any help, let me know.",
            )
            return
        }
        var next = option
        while (true) {
            next =
                when (next) {
                    WONDERING -> wondering()
                    BOARD -> board()
                    TELESCOPE -> telescope()
                    else -> {
                        chatPlayer(neutral, "I'd better be off.")
                        return
                    }
                }
        }
    }

    private suspend fun Dialogue.wondering(): Int {
        chatPlayer(quiz, "I was wondering what you do here.")
        chatNpc(happy, "Glad you ask. This is the Observatory reception.")
        chatNpc(
            neutral,
            "Up on the cliff is the Observatory dome, from which you can view the heavens.",
        )
        chatNpc(neutral, "Usually...")
        chatPlayer(quiz, "What do you mean, 'usually'?")
        chatNpcSpecific(
            "Observatory professor",
            PROFESSOR_HEAD,
            angry,
            "*Ahem*. Back to work, please.",
        )
        chatNpc(neutral, "I'd speak with the professor. He'll explain.")
        return choice3(
            "What's that board for?",
            BOARD,
            "Can I have a look through that telescope?",
            TELESCOPE,
            "I'd better be off.",
            OFF,
        )
    }

    private suspend fun Dialogue.board(): Int {
        chatPlayer(quiz, "What's that board for?")
        chatNpc(
            neutral,
            "The one behind me? We use it to keep track of fallen stars and those that find them.",
        )
        chatPlayer(quiz, "Fallen stars?")
        chatNpc(
            neutral,
            "That's right. From time to time, stars fall from the sky and land somewhere in " +
                "Gielinor.",
        )
        chatNpc(
            neutral,
            "Whenever one does, we make a note on the board. If someone finds it, we note their " +
                "name as well.",
        )
        chatPlayer(quiz, "I see. And how do people find them?")
        chatNpc(
            neutral,
            "There's a lot of amateur astronomers out there. Many of them have small telescopes " +
                "in their homes. They use them to work out when a star is going to fall.",
        )
        chatPlayer(quiz, "Could I do that?")
        chatNpc(
            neutral,
            "I imagine so. Just get yourself a telescope and look out for stars.",
        )
        chatPlayer(happy, "Awesome. I might just give it a go.")
        chatNpc(
            happy,
            "Good luck. If you're interested, I hear there's a dwarf at the Mining Guild who's " +
                "keen on gathering stardust. If you find some, he might be willing to trade with " +
                "you.",
        )
        chatPlayer(happy, "Good to know. Thanks.")
        return choice3(
            "I was wondering what you do here.",
            WONDERING,
            "Can I have a look through that telescope?",
            TELESCOPE,
            "I'd better be off.",
            OFF,
        )
    }

    private suspend fun Dialogue.telescope(): Int {
        chatPlayer(quiz, "Can I have a look through that telescope?")
        chatNpc(neutral, "You can. You won't see much though.")
        chatPlayer(quiz, "And that's because?")
        chatNpc(neutral, "Just talk to the professor. He'll fill you in.")
        return choice3(
            "I was wondering what you do here.",
            WONDERING,
            "What's that board for?",
            BOARD,
            "I'd better be off.",
            OFF,
        )
    }

    private companion object {
        const val PROFESSOR = "npc.observatory_professor_multi"
        const val PROFESSOR_HEAD = "npc.observatory_professor"
        const val ASSISTANT = "npc.qip_obs_proffesors_assistant"
        const val CHART = "obj.trail_chart"
        const val OBSERVATORY_QUEST = "quest_observatory"
        const val WONDERING = 1
        const val LOOKING = 2
        const val TELESCOPE = 3
        const val BOARD = 4
        const val OFF = 5
    }
}
