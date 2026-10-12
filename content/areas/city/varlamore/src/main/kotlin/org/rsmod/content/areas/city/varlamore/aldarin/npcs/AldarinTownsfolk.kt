package org.rsmod.content.areas.city.varlamore.aldarin.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class AldarinTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in CITIZENS) onOpNpc1(npc) { startDialogue(it.npc) { citizen() } }
        for (npc in VINE_WORKERS) onOpNpc1(npc) { startDialogue(it.npc) { vineyardWorker() } }
        for (npc in MISTROCK_CITIZENS) {
            onOpNpc1(npc) { startDialogue(it.npc) { mistrockCitizen() } }
        }
        for (npc in MISTROCK_CHILDREN) onOpNpc1(npc) { startDialogue(it.npc) { mistrockChild() } }
        for (npc in MISTROCK_WORKERS) onOpNpc1(npc) { startDialogue(it.npc) { mistrockWorker() } }
        for (npc in MISTROCK_GUARDS) onOpNpc1(npc) { startDialogue(it.npc) { mistrockGuard() } }
        onOpNpc1("npc.aldarin_not_hitman") { startDialogue(it.npc) { wellDressedMan() } }
        onOpNpc1("npc.mistrock_overseer") { startDialogue(it.npc) { overseerKhougo() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Citizen_(Aldarin)
    private suspend fun Dialogue.citizen() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(6)) {
            0 -> chatNpc(happy, "I'm very well, thank you.")
            1 -> chatNpc(neutral, "Sorry, but I don't want to buy anything.")
            2 -> chatNpc(neutral, "Not too bad thanks.")
            3 -> chatNpc(happy, "Hello!")
            4 -> chatNpc(happy, "Nilsal! I'm pretty good, thank you.")
            else -> chatNpc(happy, "Nilsal!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vineyard_worker
    private suspend fun Dialogue.vineyardWorker() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(happy, "Lovely day for some wine!")
            1 -> chatNpc(sad, "I wish these grapes could pick themselves.")
            2 -> chatNpc(happy, "The grapes are in good shape for this harvest.")
            3 -> chatNpc(neutral, "I'm sorry, but the vineyards need tending to.")
            else -> chatNpc(happy, "Have you tried the wine? You won't regret it if you do!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Well-dressed_man
    private suspend fun Dialogue.wellDressedMan() {
        val variant = access.random.of(3)
        chatNpc(shifty, "Three stories... Multiple entries... Numerous butlers...")
        chatPlayer(quiz, "Hello! What're you doing up here?")
        when (variant) {
            0 ->
                chatNpc(
                    shifty,
                    "I'm here on business. I'd suggest you not worry about it. My line of work " +
                        "can be rather... cutthroat.",
                )
            1 ->
                chatNpc(
                    shifty,
                    "I'm just watching and observing. I find that doing so offers plenty of... " +
                        "opportunities.",
                )
            else ->
                chatNpc(
                    shifty,
                    "I'm just enjoying the view. I find a place like this good when you want " +
                        "to... elimitate distractions.",
                )
        }
        if (QuestRequirements.hasCompleted(player, DEATH_ON_THE_ISLE)) {
            chatPlayer(
                neutral,
                "I'm not sure what you're up to, but if it's got anything to do with that villa, " +
                    "then I think someone might've beaten you to it...",
            )
            chatNpc(shifty, "Then it seems I now have a gap in my schedule...")
            chatPlayer(happy, "Well you enjoy it!")
        } else {
            chatPlayer(happy, "Okay! I'll be going then!")
        }
        if (variant != 0) {
            chatPlayer(happy, "Well you enjoy it!")
        }
        chatPlayer(happy, "What a nice man.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Overseer_Khougo
    // TODO(content): [system:met_flags] the "You seem to have returned." greeting on later visits
    private suspend fun Dialogue.overseerKhougo() {
        chatNpc(
            angry,
            "Human. You seem to have found your way into Mistrock. What are you doing here?",
        )
        chatPlayer(quiz, "Am I not allowed to be here?")
        chatNpc(
            angry,
            "Your kind have managed to establish dominion over the vast majority of these lands. " +
                "Will you not even let us have our small corner in peace?",
        )
        chatPlayer(quiz, "Is that a no?")
        chatNpc(
            angry,
            "Hmph! I can't stop you being here, but I don't have to enjoy your presence.",
        )
        if (!QuestRequirements.hasCompleted(player, PERILOUS_MOONS)) {
            chatPlayer(quiz, "Is there anything that I can do to change that?")
            chatNpc(angry, "No. Now if this conversation is over, I'm going to have a drink.")
            return
        }
        chatPlayer(
            quiz,
            "Is there anything that I can do to change that? I've already been quite helpful " +
                "over in Cam Torum.",
        )
        chatNpc(angry, "One act of charity will not excuse years of subjugation.")
        chatPlayer(confused, "Subjugation? That seems like a bit of an over-exaggeration.")
        chatNpc(
            angry,
            "Call it what you will. The end result is the same. Your kind prosper at the expense " +
                "of ours, and you act like it's not even happening.",
        )
        chatPlayer(neutral, "I've seen Cam Torum. You don't seem to be doing too badly.")
        chatNpc(
            angry,
            "And here you prove my point. One city! One! You see that one city and consider it " +
                "somehow equal to your many.",
        )
        chatPlayer(quiz, "Well what about this place?")
        chatNpc(
            neutral,
            "A mere mining outpost. It hardly compares to those villas elsewhere on the island.",
        )
        chatPlayer(
            neutral,
            "Not all humans are the same though. It's not just Cam Torum. I've actually worked " +
                "with your kind a few times.",
        )
        chatNpc(quiz, "Such as?")
        chatPlayer(
            neutral,
            "Well there was this one time when I helped the dwarves of Keldagrim with a bit of a " +
                "statue issue. Another time I worked with one of the last surviving Imcando " +
                "dwarves to create a blurite sword.",
        )
        chatNpc(shocked, "Imcando?")
        chatPlayer(happy, "Yes! You know about them?")
        chatNpc(
            angry,
            "Of course I know about them! The Imcando were our ancestors, but I thought they " +
                "were long dead.",
        )
        chatPlayer(neutral, "Not quite.")
        chatNpc(angry, "Hmph! It's still going to take more than that to get me to like you.")
        chatPlayer(quiz, "Well if liking is out of the question, how about tolerate?")
        chatNpc(neutral, "Fine. Consider yourself tolerated. Now, did you actually want something?")
        khougoOptions()
    }

    // TODO(content): [verify:in_game] whether the options come back after the first two answers
    private suspend fun Dialogue.khougoOptions() {
        val option =
            choice3(
                "What do you do here?",
                1,
                "You said the Imcando were your ancestors?",
                2,
                "Not really.",
                3,
                title = "Select an Option",
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What do you do here?")
                chatNpc(
                    neutral,
                    "I'm the overseer for Mistrock. I make sure everything around here is " +
                        "running as it should be.",
                )
                chatPlayer(quiz, "And you do that from the pub?")
                chatNpc(angry, "Am I not allowed a break from time to time?")
                chatPlayer(neutral, "I suppose.")
                chatNpc(angry, "Hmph!")
            }
            2 -> {
                chatPlayer(quiz, "You said the Imcando were your ancestors?")
                chatNpc(
                    neutral,
                    "That's right. The Lovakengj dwarves were born from the Imcando and we were " +
                        "born from the Lovakengj. If you want to know more, find a historian.",
                )
            }
            3 -> {
                chatPlayer(neutral, "Not really.")
                chatNpc(angry, "Then be on your way so that I can have a drink.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Citizen_(Mistrock)
    private suspend fun Dialogue.mistrockCitizen() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(6)) {
            0 -> chatNpc(angry, "I don't talk to you lot.")
            1 -> chatNpc(angry, "Hmph!")
            2 -> chatNpc(angry, "Why are you here? This isn't a place for humans.")
            3 -> chatNpc(angry, "A human? Why don't you go back to where you came from?")
            4 -> chatNpc(angry, "I don't talk to you lot.")
            else -> chatNpc(angry, "Not interested!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Child_(Mistrock)
    private suspend fun Dialogue.mistrockChild() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(worried, "I'm not allowed to talk to your kind.")
            1 -> chatNpc(angry, "I don't want to talk to you.")
            2 -> chatNpc(worried, "I shouldn't speak with humans.")
            3 -> chatNpc(worried, "Mummy said I should stay away from humans.")
            else -> chatNpc(neutral, "I don't know you.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Worker_(Mistrock)
    private suspend fun Dialogue.mistrockWorker() {
        chatPlayer(happy, "Hello there.")
        when (access.random.of(5)) {
            0 -> chatNpc(angry, "Leave me alone. I have work to do.")
            1 -> chatNpc(angry, "Whatever you want, the answer's no.")
            2 -> chatNpc(angry, "I don't have time to talk to you.")
            3 -> chatNpc(neutral, "Please don't disturb me.")
            else -> chatNpc(neutral, "I'm busy right now.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Mistrock)
    private suspend fun Dialogue.mistrockGuard() {
        when (access.random.of(5)) {
            0 -> chatNpc(angry, "Be on your way.")
            1 -> chatNpc(angry, "You don't want to mess with me, human.")
            2 ->
                chatNpc(
                    angry,
                    "If you're thinking of trying anything, you'll quickly find yourself " +
                        "regretting it.",
                )
            3 -> chatNpc(angry, "I'm watching you, human.")
            else -> chatNpc(angry, "Move along.")
        }
    }

    private companion object {
        const val DEATH_ON_THE_ISLE = "quest_deathontheisle"
        const val PERILOUS_MOONS = "quest_perilousmoons"

        val CITIZENS =
            listOf(
                "npc.aldarin_citizen_poor_m_1",
                "npc.aldarin_citizen_poor_m_2",
                "npc.aldarin_citizen_poor_m_3",
                "npc.aldarin_citizen_poor_f_1",
                "npc.aldarin_citizen_poor_f_2",
                "npc.aldarin_citizen_poor_f_3",
                "npc.aldarin_citizen_normal_m_1",
                "npc.aldarin_citizen_normal_m_2",
                "npc.aldarin_citizen_normal_m_3",
                "npc.aldarin_citizen_normal_m_4",
                "npc.aldarin_citizen_normal_f_1",
                "npc.aldarin_citizen_normal_f_2",
                "npc.aldarin_citizen_normal_f_3",
                "npc.aldarin_citizen_normal_f_4",
                "npc.aldarin_citizen_normal_f_5",
                "npc.aldarin_citizen_rich_m_1",
                "npc.aldarin_citizen_rich_m_2",
                "npc.aldarin_citizen_rich_f_1",
                "npc.aldarin_citizen_rich_f_2",
            )

        val VINE_WORKERS =
            listOf(
                "npc.aldarin_vine_worker_1",
                "npc.aldarin_vine_worker_2",
                "npc.aldarin_vine_worker_3",
                "npc.aldarin_vine_worker_4",
            )

        val MISTROCK_CITIZENS =
            listOf(
                "npc.mistrock_citizen_m_1",
                "npc.mistrock_citizen_m_2",
                "npc.mistrock_citizen_m_3",
                "npc.mistrock_citizen_m_4",
                "npc.mistrock_citizen_f_1",
                "npc.mistrock_citizen_f_2",
                "npc.mistrock_citizen_f_3",
                "npc.mistrock_citizen_f_4",
            )

        val MISTROCK_CHILDREN =
            listOf(
                "npc.mistrock_child_m_1",
                "npc.mistrock_child_m_2",
                "npc.mistrock_child_f_1",
                "npc.mistrock_child_f_2",
            )

        val MISTROCK_WORKERS =
            listOf(
                "npc.mistrock_worker_m_1",
                "npc.mistrock_worker_m_2",
                "npc.mistrock_worker_f_1",
                "npc.mistrock_worker_f_2",
                "npc.mistrock_worker_m_1_pickaxe",
                "npc.mistrock_worker_m_2_pickaxe",
                "npc.mistrock_worker_f_1_pickaxe",
                "npc.mistrock_worker_f_2_pickaxe",
            )

        val MISTROCK_GUARDS =
            listOf("npc.mistrock_guard_1", "npc.mistrock_guard_2", "npc.mistrock_guard_3")
    }
}
