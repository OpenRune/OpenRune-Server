package org.rsmod.content.areas.city.varlamore.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VarlamoreFolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in CITIZENS) onOpNpc1(npc) { startDialogue(it.npc) { citizen() } }
        for (npc in CHILDREN) onOpNpc1(npc) { startDialogue(it.npc) { child() } }
        for (npc in FISHERS) onOpNpc1(npc) { startDialogue(it.npc) { fisher() } }
        for (npc in FARMERS) onOpNpc1(npc) { startDialogue(it.npc) { farmer() } }
        for (npc in WORKERS) onOpNpc1(npc) { startDialogue(it.npc) { worker() } }
        for (npc in TOURISTS) onOpNpc1(npc) { startDialogue(it.npc) { tourist() } }
        for (npc in SAILORS) onOpNpc1(npc) { startDialogue(it.npc) { sailor() } }
        for (npc in SQUIRES) onOpNpc1(npc) { startDialogue(it.npc) { squire() } }
        for (npc in NELTAS_OF_RALOS) {
            onOpNpc1(npc) { startDialogue(it.npc) { neltaOfRalos(it.npc) } }
        }
        for (npc in NELTAS_OF_RANUL) onOpNpc1(npc) { startDialogue(it.npc) { neltaOfRanul() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Citizen_(Civitas_illa_Fortis)
    private suspend fun Dialogue.citizen() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(10)) {
            0 -> chatNpc(happy, "Hello!")
            1 -> chatNpc(happy, "Hello there! Nice weather we've been having.")
            2 -> {
                chatNpc(happy, "I'm fine, how are you?")
                chatPlayer(happy, "Very well, thank you.")
            }
            3 -> chatNpc(happy, "I'm very well, thank you.")
            4 -> chatNpc(happy, "Nilsal!")
            5 -> chatNpc(happy, "Nilsal! I'm pretty good, thank you.")
            6 -> chatNpc(neutral, "Not too bad thanks.")
            7 -> chatNpc(neutral, "Sorry, but I don't want to buy anything.")
            8 -> chatNpc(neutral, "Sorry, but I'm busy right now.")
            else -> {
                chatNpc(quiz, "Who are you?")
                chatPlayer(happy, "I'm a bold adventurer.")
                chatNpc(happy, "Ah, a very noble profession.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Child_(Civitas_illa_Fortis)
    // https://oldschool.runescape.wiki/w/Transcript:Child_(Aldarin)
    private suspend fun Dialogue.child() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(neutral, "I don't know you.")
            1 -> chatNpc(angry, "I don't want to talk to you.")
            2 -> chatNpc(worried, "I shouldn't be talking to you.")
            3 -> chatNpc(worried, "I shouldn't speak with strangers.")
            else -> chatNpc(worried, "I'm not allowed to talk to strangers.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fisher
    private suspend fun Dialogue.fisher() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(happy, "Hello! Nice day for fishing!")
            1 ->
                chatNpc(
                    happy,
                    "Well given how good today's catch was, I'd say it's going pretty well!",
                )
            2 -> chatNpc(happy, "Nilsal! It's going well, thanks.")
            3 -> {
                chatNpc(happy, "I'm fine, how are you?")
                chatPlayer(happy, "Very well, thank you.")
            }
            else -> chatNpc(neutral, "Not too bad thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farmer_(Varlamore)
    private suspend fun Dialogue.farmer() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(happy, "Well the work is going well, so I'm very good, thank you.")
            1 -> chatNpc(neutral, "Not too bad thanks.")
            2 -> {
                chatNpc(happy, "I'm fine, how are you?")
                chatPlayer(happy, "Very well, thank you.")
            }
            3 ->
                chatNpc(
                    happy,
                    "Nilsal! I'm pretty good, thank you. It's always nice to be out in the sun.",
                )
            else ->
                chatNpc(
                    happy,
                    "Hello there! Nice weather we've been having. Perfect for a day outdoors!",
                )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Worker_(Varlamore)
    private suspend fun Dialogue.worker() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(neutral, "Sorry, but I have lots of work to do.")
            1 -> chatNpc(neutral, "Not too bad, but the work is definitely tiring.")
            2 -> chatNpc(neutral, "Well I could do with a good rest, but fine otherwise.")
            3 -> chatNpc(neutral, "Sorry, but I'm busy right now.")
            else -> chatNpc(neutral, "Busy, busy, busy.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tourist
    private suspend fun Dialogue.tourist() {
        chatPlayer(happy, "Hey there.")
        when (access.random.of(7)) {
            0 -> chatNpc(happy, "Hello!")
            1 -> chatNpc(happy, "Hello there! The weather here is wonderful, isn't it?")
            2 -> chatNpc(happy, "Hello there, or should I say nilsal?")
            3 -> chatNpc(happy, "I can't wait to visit the rest of the kingdom!")
            4 -> chatNpc(happy, "Isn't this place wonderful?")
            5 -> chatNpc(happy, "Hi there. Hope you're enjoying your time in Varlamore.")
            else -> {
                chatNpc(quiz, "Who are you?")
                chatPlayer(happy, "I'm a bold adventurer.")
                chatNpc(happy, "Ah, a very noble profession.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sailor_(Varlamore)
    private suspend fun Dialogue.sailor() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(neutral, "Sorry, but I'm busy right now.")
            1 -> chatNpc(neutral, "Okay, but it will be better once we're back on board.")
            2 -> chatNpc(neutral, "Not too bad, but there's still a lot that needs doing today.")
            3 ->
                chatNpc(
                    neutral,
                    "Sorry, but I have lots of work to do before we head back out to sea.",
                )
            else -> chatNpc(neutral, "Well I could do with a good rest, but fine otherwise.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Squire_(Civitas_illa_Fortis)
    private suspend fun Dialogue.squire() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(neutral, "Busy, busy, busy.")
            1 -> chatNpc(neutral, "Sorry, but I'm busy right now.")
            2 -> chatNpc(neutral, "Sorry, but I have lots of work to do.")
            3 -> chatNpc(neutral, "Not too bad, but the work is definitely tiring.")
            else -> chatNpc(neutral, "Well I could do with a good rest, but fine otherwise.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nelta_of_Ralos
    // TODO(content): [verify:in_game] the bounds of the Teomat for "What is this place?"
    private suspend fun Dialogue.neltaOfRalos(npc: Npc) {
        chatNpc(happy, "Nilsal, traveller.")
        while (true) {
            val option =
                if (npc.inTeomat()) {
                    choice4(
                        "Can you heal me? I'm injured.",
                        1,
                        "So what do you do?",
                        2,
                        "What is this place?",
                        3,
                        "I'd best get going.",
                        4,
                    )
                } else {
                    choice3(
                        "Can you heal me? I'm injured.",
                        1,
                        "So what do you do?",
                        2,
                        "I'd best get going.",
                        4,
                    )
                }
            when (option) {
                1 -> {
                    chatPlayer(sad, "Can you heal me? I'm injured.")
                    if (!isWounded()) {
                        chatNpc(
                            neutral,
                            "I could, but you don't appear to actually need healing right now.",
                        )
                    } else {
                        chatNpc(happy, "Of course.")
                        ralosHeal()
                        mesbox("The nelta whispers a prayer and you suddenly feel a bit better.")
                    }
                }
                2 -> {
                    chatPlayer(quiz, "So what do you do?")
                    chatNpc(happy, "I am a nelta in service to Ralos.")
                    chatPlayer(quiz, "What's a nelta?")
                    chatNpc(
                        happy,
                        "We are the faithful servants of Ralos. We serve in a variety of roles " +
                            "across the Church of Ralos.",
                    )
                    chatPlayer(quiz, "So kind of like a priest?")
                    chatNpc(
                        neutral,
                        "Yes, I believe that would be the closest comparison to the Church of " +
                            "Saradomin, though I am no expert on their practices.",
                    )
                    chatNpc(
                        neutral,
                        "Above us mere neltas are the teoki, leaders of the key temples across " +
                            "the kingdom. And of course, above them you have the Teokan himself.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "This is the Teomat, home to both the Church of Ralos and the Sect of " +
                            "Ranul. The first and only time that Ralos appeared to us, he did so " +
                            "here on Ralos' Rise. The Teomat was built here as a result.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'd best get going.")
                    chatNpc(happy, "May the sun light your way.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nelta_of_Ranul
    private suspend fun Dialogue.neltaOfRanul() {
        chatNpc(neutral, "Adventurer.")
        while (true) {
            val option =
                choice4(
                    "Can you heal me? I'm injured.",
                    1,
                    "So what do you do?",
                    2,
                    "What is this place?",
                    3,
                    "I'd best get going.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(sad, "Can you heal me? I'm injured.")
                    chatNpc(
                        neutral,
                        "I would suggest you seek out a member of the Church of Ralos for that " +
                            "sort of thing. We are more concerned with the dead than the living.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "So what do you do?")
                    chatNpc(neutral, "I am a nelta of the Sect of Ranul.")
                    chatPlayer(quiz, "What's a nelta?")
                    chatNpc(
                        neutral,
                        "Both the Church of Ralos and the Sect of Ranul has neltas. For those of " +
                            "us dedicated to Ranul, our primary duty is to care for the dead and " +
                            "prepare them for their journey through the Gates of Mictl.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "This is the Teomat. Both the Church of Ralos and the Sect of Ranul are " +
                            "based here, though given us being atop Ralos' Rise, the location " +
                            "holds far more importance to the Church than it does to us.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'd best get going.")
                    chatNpc(neutral, "Farewell.")
                    return
                }
            }
        }
    }

    private fun Npc.inTeomat(): Boolean =
        coords.x in TEOMAT_MIN_X..TEOMAT_MAX_X && coords.z in TEOMAT_MIN_Z..TEOMAT_MAX_Z

    private companion object {
        const val TEOMAT_MIN_X = 1408
        const val TEOMAT_MAX_X = 1487
        const val TEOMAT_MIN_Z = 3136
        const val TEOMAT_MAX_Z = 3215

        val CITIZENS =
            listOf(
                "npc.varlamore_citizen_poor_m_1",
                "npc.varlamore_citizen_poor_m_2",
                "npc.varlamore_citizen_poor_m_3",
                "npc.varlamore_citizen_poor_m_4",
                "npc.varlamore_citizen_poor_m_5",
                "npc.varlamore_citizen_poor_f_1",
                "npc.varlamore_citizen_poor_f_2",
                "npc.varlamore_citizen_poor_f_3",
                "npc.varlamore_citizen_poor_f_4",
                "npc.varlamore_citizen_poor_f_5",
                "npc.varlamore_citizen_normal_m_1",
                "npc.varlamore_citizen_normal_m_2",
                "npc.varlamore_citizen_normal_m_3",
                "npc.varlamore_citizen_normal_m_4",
                "npc.varlamore_citizen_normal_m_5",
                "npc.varlamore_citizen_normal_f_1",
                "npc.varlamore_citizen_normal_f_2",
                "npc.varlamore_citizen_normal_f_3",
                "npc.varlamore_citizen_normal_f_4",
                "npc.varlamore_citizen_normal_f_5",
                "npc.varlamore_citizen_rich_m_1",
                "npc.varlamore_citizen_rich_m_2",
                "npc.varlamore_citizen_rich_m_3",
                "npc.varlamore_citizen_rich_m_4",
                "npc.varlamore_citizen_rich_m_5",
                "npc.varlamore_citizen_rich_f_1",
                "npc.varlamore_citizen_rich_f_2",
                "npc.varlamore_citizen_rich_f_3",
                "npc.varlamore_citizen_rich_f_4",
                "npc.varlamore_citizen_rich_f_5",
                "npc.varlamore_citizen_with_baby_m_1",
                "npc.varlamore_citizen_with_baby_m_2",
                "npc.varlamore_citizen_with_baby_f_1",
                "npc.varlamore_citizen_with_baby_f_2",
            )

        val CHILDREN =
            listOf(
                "npc.varlamore_child_poor_m_1",
                "npc.varlamore_child_poor_m_2",
                "npc.varlamore_child_poor_f_1",
                "npc.varlamore_child_poor_f_2",
                "npc.varlamore_child_normal_m_1",
                "npc.varlamore_child_normal_m_2",
                "npc.varlamore_child_normal_f_1",
                "npc.varlamore_child_normal_f_2",
                "npc.aldarin_child_f_1",
                "npc.aldarin_child_m_1",
            )

        val FISHERS =
            listOf(
                "npc.varlamore_fisher_m_1",
                "npc.varlamore_fisher_m_2",
                "npc.varlamore_fisher_f_1",
                "npc.varlamore_fisher_f_2",
                "npc.kastori_fisher_m_1",
            )

        val FARMERS =
            listOf(
                "npc.varlamore_farmer_m_1",
                "npc.varlamore_farmer_m_2",
                "npc.varlamore_farmer_m_3",
                "npc.varlamore_farmer_m_4",
                "npc.varlamore_farmer_f_1",
                "npc.varlamore_farmer_f_2",
                "npc.varlamore_farmer_f_3",
                "npc.varlamore_farmer_f_4",
            )

        val WORKERS =
            listOf(
                "npc.avium_savannah_worker_m_1",
                "npc.avium_savannah_worker_m_2",
                "npc.avium_savannah_worker_m_3",
                "npc.avium_savannah_worker_m_4",
                "npc.avium_savannah_worker_f_1",
                "npc.avium_savannah_worker_f_2",
                "npc.avium_savannah_worker_f_3",
                "npc.avium_savannah_worker_f_4",
                "npc.varlamore_worker_m_1",
                "npc.varlamore_worker_m_2",
                "npc.varlamore_worker_m_3",
                "npc.varlamore_worker_f_1",
                "npc.varlamore_worker_f_2",
                "npc.varlamore_worker_f_3",
            )

        val TOURISTS =
            listOf(
                "npc.varlamore_tourist_m_1",
                "npc.varlamore_tourist_m_2",
                "npc.varlamore_tourist_m_3",
                "npc.varlamore_tourist_f_1",
                "npc.varlamore_tourist_f_2",
                "npc.varlamore_tourist_f_3",
            )

        val SAILORS =
            listOf(
                "npc.varlamore_sailor_m_1",
                "npc.varlamore_sailor_m_2",
                "npc.varlamore_sailor_f_1",
                "npc.varlamore_sailor_f_2",
            )

        val SQUIRES =
            listOf(
                "npc.varlamore_squire_m_1",
                "npc.varlamore_squire_m_2",
                "npc.varlamore_squire_f_1",
                "npc.varlamore_squire_f_2",
            )

        val NELTAS_OF_RALOS =
            listOf(
                "npc.varlamore_nelta_ralos_m_1",
                "npc.varlamore_nelta_ralos_m_2",
                "npc.varlamore_nelta_ralos_m_3",
                "npc.varlamore_nelta_ralos_m_4",
                "npc.varlamore_nelta_ralos_m_5",
                "npc.varlamore_nelta_ralos_f_1",
                "npc.varlamore_nelta_ralos_f_2",
                "npc.varlamore_nelta_ralos_f_3",
                "npc.varlamore_nelta_ralos_f_4",
                "npc.varlamore_nelta_ralos_f_5",
                "npc.fortis_nelta_ralos_m_1",
                "npc.fortis_nelta_ralos_m_2",
            )

        val NELTAS_OF_RANUL =
            listOf(
                "npc.varlamore_nelta_ranul_m_1",
                "npc.varlamore_nelta_ranul_m_2",
                "npc.varlamore_nelta_ranul_m_3",
                "npc.varlamore_nelta_ranul_m_4",
                "npc.varlamore_nelta_ranul_m_5",
                "npc.varlamore_nelta_ranul_f_1",
                "npc.varlamore_nelta_ranul_f_2",
                "npc.varlamore_nelta_ranul_f_3",
                "npc.varlamore_nelta_ranul_f_4",
                "npc.varlamore_nelta_ranul_f_5",
                "npc.fortis_nelta_ranul_f_1",
            )
    }
}
