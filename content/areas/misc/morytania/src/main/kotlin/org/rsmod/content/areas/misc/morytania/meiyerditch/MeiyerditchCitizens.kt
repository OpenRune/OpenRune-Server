package org.rsmod.content.areas.misc.morytania.meiyerditch

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] "Oh... Sorry, we're not meant to talk to the nobles."
//  while wearing vyre noble clothing
// TODO(content): [system:meiyerditch_sectors] "I'm lost..." answers with the sector the npc
//  stands in; no sector data exists yet, so the option is left out
class MeiyerditchCitizens : PluginScript() {
    override fun ScriptContext.startup() {
        for (citizen in tithedCitizens()) {
            onOpNpc1(citizen) { startDialogue(it.npc) { citizen() } }
        }
        for (citizen in freedCitizens()) {
            onOpNpc1(citizen) { startDialogue(it.npc) { freedCitizen() } }
        }
        for (child in CHILDREN) {
            onOpNpc1(child) { startDialogue(it.npc) { child() } }
        }
    }

    private fun tithedCitizens(): List<String> =
        (1..MALE_CITIZENS).map { "npc.myreque_pt3_male_citizen${it}_vis" } +
            (1..FEMALE_CITIZENS).map { "npc.myreque_pt3_female_citizen${it}_vis" } +
            (1..MALE_HUDDLED).map { "npc.myreque_pt3_male_huddled${it}_vis" } +
            (1..FEMALE_HUDDLED).map { "npc.myreque_pt3_female_huddled${it}_vis" } +
            (1..LOOKING_OUT).map { "npc.myreque_pt3_male_looking_out${it}_vis" } +
            (1..OLD_MEN).map { "npc.myq3_citizen_male_old_${it}_vis" }

    private fun freedCitizens(): List<String> =
        (1..MALE_CITIZENS).map { "npc.myreque_pt3_male_citizen${it}_post_myq6" } +
            (1..FEMALE_CITIZENS).map { "npc.myreque_pt3_female_citizen${it}_post_myq6" } +
            (1..OLD_MEN).map { "npc.myq3_citizen_male_old_${it}_post_myq6" }

    // https://oldschool.runescape.wiki/w/Transcript:Meiyerditch_citizen (after Darkness of
    // Hallowvale)
    // TODO(content): [quest:darkness_of_hallowvale] the in-quest dialogue and the "improperly
    //  accesses Meiyerditch" line
    private suspend fun Dialogue.citizen() {
        when (access.random.of(4)) {
            0 -> {
                chatNpc(worried, "What? What's wrong?")
                chatPlayer(neutral, "Nothing, I just need to ask a couple of questions.")
            }
            1 -> {
                chatNpc(neutral, "Oh, hello. How's everything?")
                chatPlayer(neutral, "Not too bad thanks, but I wanted to ask a question.")
            }
            2 -> {
                chatNpc(confused, "Hello?")
                chatPlayer(neutral, "Hello, I wonder if I could ask some questions.")
            }
            else -> {
                chatNpc(shocked, "Hey, don't sneak up on me like that!")
                chatPlayer(
                    worried,
                    "Sorry, didn't mean to scare you. I just wanted to ask a question.",
                )
                chatNpc(neutral, "Well, I guess so.")
            }
        }
        var menu = 0
        while (true) {
            menu =
                when (menu) {
                    0 -> initialQuestions() ?: return
                    1 -> moreQuestions()
                    else -> lastQuestions() ?: return
                }
        }
    }

    private suspend fun Dialogue.initialQuestions(): Int? {
        val option =
            choice3(
                "What do you do here?",
                1,
                "Do you know about the Myreque?",
                2,
                "- More questions -",
                3,
                title = "Select an Option",
            )
        when (option) {
            1 -> whatDoYouDo()
            2 -> myreque()
            3 -> return 1
        }
        return 0
    }

    private suspend fun Dialogue.moreQuestions(): Int {
        val option =
            choice4(
                "Can you tell me about the Sanguinesti region?",
                1,
                "What do you know about the vampyres?",
                2,
                "What do you know about Drakan?",
                3,
                "- More questions -",
                4,
                title = "Select an Option",
            )
        when (option) {
            1 -> sanguinesti()
            2 -> vampyres()
            3 -> drakan()
            4 -> return 2
        }
        return 1
    }

    private suspend fun Dialogue.lastQuestions(): Int? {
        val option =
            choice4(
                "What is there to do around here?",
                1,
                "Is there anything I can do to help?",
                2,
                "Okay, thanks.",
                3,
                "- Initial questions -",
                4,
                title = "Select an Option",
            )
        when (option) {
            1 -> thingsToDo()
            2 -> help()
            3 -> {
                chatPlayer(neutral, "Okay, thanks.")
                return null
            }
            4 -> return 0
        }
        return 2
    }

    private suspend fun Dialogue.whatDoYouDo() {
        chatPlayer(quiz, "What do you do here?")
        when (access.random.of(6)) {
            0 -> chatNpc(sad, "I'm barely surviving...")
            1 ->
                chatNpc(
                    sad,
                    "When I'm not being drained of blood, the vyrewatch force me to repair the " +
                        "walls.",
                )
            2 -> chatNpc(sad, "When I'm not being drained of blood I'm fixing up the ghetto.")
            3 ->
                chatNpc(
                    sad,
                    "I just try to make a living the best way I can. You haven't got some food " +
                        "have you?",
                )
            4 ->
                chatNpc(
                    sad,
                    "Like everyone else, I just survive here. Just try to keep alive till after " +
                        "the next blood tithe. That's when they feed us.",
                )
            else -> {
                chatNpc(shifty, "Why do you wanna know?")
                chatPlayer(neutral, "Just curious, that's all.")
                chatNpc(shifty, "I dunno. I don't know if I can trust you...")
            }
        }
    }

    private suspend fun Dialogue.myreque() {
        chatPlayer(quiz, "Do you know about the Myreque?")
        if (QuestRequirements.hasCompleted(player, SINS_OF_THE_FATHER)) {
            chatNpc(sad, "The Myreque are gone. Do not speak of them, it will do you no good.")
            return
        }
        chatNpc(worried, "It's probably best not to talk about that, friend.")
        chatPlayer(
            neutral,
            "I need to make contact with them. Please don't worry - I'm on their side.",
        )
        chatNpc(
            worried,
            "As are we all, but I really can't talk about them. It's not a subject easily " +
                "broached here.",
        )
    }

    private suspend fun Dialogue.sanguinesti() {
        chatPlayer(quiz, "Can you tell me about the Sanguinesti region?")
        chatNpc(
            neutral,
            "Yep, that's where we are at the moment. This place, Meiyerditch, is just one large " +
                "ghetto.",
        )
        chatNpc(
            neutral,
            "The city is sectioned off so that the vampyres can collect blood tithes on a " +
                "rotational basis. That way they don't kill us all off in one go.",
        )
    }

    private suspend fun Dialogue.vampyres() {
        chatPlayer(quiz, "What do you know about the vampyres?")
        chatNpc(
            angry,
            "They're monsters, I know that much. I've never known anything else, but I have a " +
                "feeling it might be nice to not have to live in fear the whole time. Worrying " +
                "if I'm going to make the next blood tithe quota.",
        )
        chatNpc(
            neutral,
            "I know that the juveniles are the lowest form of the vampyres. Apparently they were " +
                "human once, converted by the vampyres.",
        )
        chatNpc(
            neutral,
            "Then you have juvinates. They're initiated juveniles who've had some training and " +
                "are starting to get some powers.",
        )
        chatNpc(worried, "After that you have the vyres. They're nasty... really nasty.")
    }

    // TODO(content): [verify:in_game] whether the pre-A Taste of Hope answer returns to the
    //  options ("Continues below" on the transcript)
    private suspend fun Dialogue.drakan() {
        chatPlayer(quiz, "What do you know about Drakan?")
        if (QuestRequirements.hasCompleted(player, SINS_OF_THE_FATHER)) {
            chatNpc(
                worried,
                "No one had seen anything of him for years, they say he's back now though. We " +
                    "shouldn't talk of him any more though, the vyrewatch might not like it.",
            )
            return
        }
        val tasteOfHope = QuestRequirements.hasCompleted(player, A_TASTE_OF_HOPE)
        chatNpc(quiz, if (tasteOfHope) "Vanescula?" else "Drakan? Which one: Ranis or Vanescula?")
        chatPlayer(
            neutral,
            "Well, I was referring to the main Drakan, you know, the overlord of Morytania.",
        )
        val siblings = if (tasteOfHope) "Vanescula" else "Ranis or Vanescula"
        chatNpc(
            neutral,
            "Hmm, nothing has been seen of him for many years now. Some people say they " +
                "occasionally see $siblings flying about, but it's not too often.",
        )
    }

    private suspend fun Dialogue.thingsToDo() {
        chatPlayer(quiz, "What is there to do around here?")
        chatNpc(
            angry,
            "To do? Around here? Survive! Eke out an existence and just live for another day, " +
                "that's it really.",
        )
        chatNpc(
            neutral,
            "The juvinates organise work crews to mine the daeyalt, fix the walls, the castles " +
                "and so on. If we refuse to do the work, they put us on double tithes and you " +
                "can't do that for long before you dry up.",
        )
        chatNpc(
            sad,
            "If you want to escape, there's only one way. Anyone who survives in the theatre " +
                "gets to leave. It's pointless though. No one survives.",
        )
    }

    private suspend fun Dialogue.help() {
        chatPlayer(quiz, "Is there anything I can do to help?")
        chatNpc(
            sad,
            "It's very nice of you to offer, my friend, but I shouldn't expect that you could do " +
                "anything. The vampyres are just too powerful to overcome.",
        )
        chatNpc(
            sad,
            "They say this place once had a Saradominist Queen. Even she could not withstand the " +
                "vampyres. Anyone who has challenged their power in the past has come undone.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meiyerditch_citizen (after The Blood Moon
    // Rises)
    private suspend fun Dialogue.freedCitizen() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello there.")
        while (true) {
            val option =
                choice3(
                    "What do you do here?",
                    1,
                    "How are things here?",
                    2,
                    "I'd best be off.",
                    3,
                    title = "Select an option",
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do here?")
                    val line =
                        access.random.pick(
                            "I've been working on getting some things fixed up around here. " +
                                "Hopefully we can eventually make a proper home out of this city.",
                            "Just trying to make a living really. Things are a lot better now " +
                                "the vampyres aren't taking our blood, but it's still going to " +
                                "be a long time before we get this place in a good state.",
                        )
                    chatNpc(neutral, line)
                }
                2 -> {
                    chatPlayer(quiz, "How are things here?")
                    chatNpc(
                        neutral,
                        "Well, the vampyres aren't taking our blood anymore, and they've " +
                            "promised to help us rebuild the city. Still, it's going to take a " +
                            "long time to fix this place up. Most people have chosen to leave " +
                            "instead.",
                    )
                }
                3 -> {
                    chatPlayer(neutral, "I'd best be off.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Child_(Meiyerditch)
    private suspend fun Dialogue.child() {
        if (QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) {
            return freedChild()
        }
        when (access.random.of(4)) {
            0 -> {
                chatNpc(worried, "He-he-hello?")
                chatPlayer(happy, "Hello, I wondered if I could have a chat.")
            }
            1 -> {
                chatNpc(shocked, "Argghghhh!")
                chatPlayer(neutral, "Hey, don't be scared, I just want to talk to you.")
            }
            2 -> {
                chatNpc(angry, "Leave me alone!")
                chatPlayer(neutral, "Sorry but I need to ask a couple of questions.")
            }
            else -> {
                chatNpc(shocked, "Get off me! I haven't got anything!")
                chatPlayer(
                    neutral,
                    "Hey, calm down, I'm not going to hurt you. I just want to ask a few " +
                        "questions.",
                )
            }
        }
        chatNpc(neutral, "Well, I guess so.")
        while (true) {
            val option =
                choice4(
                    "What do you do here?",
                    1,
                    "Do you know about the Myreque?",
                    2,
                    "(whisper) Do you know about the Myreque?",
                    3,
                    "Okay, thanks.",
                    4,
                    title = "Select an Option",
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do here?")
                    val line =
                        access.random.pick(
                            "I'm not supposed to talk to strangers, and you sure are strange...",
                            "Nuffin'! I wasn't doin' nuffin'!",
                            "I 'elp wiv building fings an' da wall. But mainly I is hiding from " +
                                "da vyresies. I don't like dem vyresies.",
                            "I was playin', but den da flapsies came so I was hidin'. Don't want " +
                                "da flapsies to bit me.",
                        )
                    chatNpc(neutral, line)
                }
                2 -> {
                    chatPlayer(quiz, "Do you know about the Myreque?")
                    chatNpc(
                        neutral,
                        "Err, me dad says I'm not allowed to talk about that. Do you have any " +
                            "sweets?",
                    )
                    chatPlayer(neutral, "No, sorry, but please can you answer my questions?")
                }
                3 -> {
                    chatPlayer(shifty, "(whisper) Do you know about the Myreque?")
                    chatNpc(
                        shifty,
                        "Err, no! I'm not allowed to say any thingies about dem; but they're the " +
                            "good 'uns what can help us.",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "Okay, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.freedChild() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Oh... hello.")
        while (true) {
            val option =
                choice3(
                    "What do you do here?",
                    1,
                    "How are things here?",
                    2,
                    "I'd best be off.",
                    3,
                    title = "Select an option",
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do here?")
                    val line =
                        access.random.pick(
                            "I just like to go around playin'. I get to play lots now da " +
                                "vyresies aren't all mean, but I still don't like dem very much.",
                            "I's been helpin' to make da buildin's all good again!",
                        )
                    chatNpc(happy, line)
                }
                2 -> {
                    chatPlayer(quiz, "How are things here?")
                    chatNpc(
                        neutral,
                        "The vyresies don't go takin' our blood no more, but now we gotta " +
                            "rebuild everything. They say it's gonna take years! Sounds a bit " +
                            "borin' to me.",
                    )
                }
                3 -> {
                    chatPlayer(neutral, "I'd best be off.")
                    return
                }
            }
        }
    }

    private companion object {
        const val SINS_OF_THE_FATHER = "quest_sinsofthefather"
        const val A_TASTE_OF_HOPE = "quest_tasteofhope"
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val MALE_CITIZENS = 8
        const val FEMALE_CITIZENS = 10
        const val MALE_HUDDLED = 3
        const val FEMALE_HUDDLED = 2
        const val LOOKING_OUT = 2
        const val OLD_MEN = 2

        val CHILDREN =
            listOf(
                "npc.myq3_child_female_1",
                "npc.myq3_child_female_2",
                "npc.myq3_child_female_3",
                "npc.myq3_child_female_4",
                "npc.myq3_child_male_1",
                "npc.myq3_child_male_2",
                "npc.myq3_child_male_3",
                "npc.myq3_child_male_4",
                "npc.myq3_child_male_5",
                "npc.myq3_child_male_6",
            )
    }
}
