package org.rsmod.content.areas.misc.outskirts.digsite.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DigsiteNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        for (examiner in EXAMINERS) {
            onOpNpc1(examiner) { startDialogue(it.npc) { examiner() } }
        }
        onOpNpc1("npc.qip_digsite_researcher") { startDialogue(it.npc) { researcher() } }
        onOpNpc1("npc.archaeological_expert") { startDialogue(it.npc) { terryBalando() } }
        onOpNpc1("npc.panning_guide") { startDialogue(it.npc) { panningGuide() } }
        for (workman in DIGSITE_WORKMEN) {
            onOpNpc1(workman) { startDialogue(it.npc) { digsiteWorkman() } }
        }
        onOpNpc1("npc.golem_elissa") { startDialogue(it.npc) { elissa() } }
        onOpNpc3("npc.pilot_digsite") { startDialogue(it.npc) { captainErrdo() } }
        onOpNpc1("npc.vm_canal_barge_foremen_talking") { startDialogue(it.npc) { bargeForeman() } }
        onOpNpc1("npc.fossilquest_barge_guard_port") { startDialogue(it.npc) { bargeGuard() } }
        onOpNpc1("npc.vm_woodcutter") { startDialogue(it.npc) { edWood() } }
        for (workman in BARGE_WORKMEN) {
            onOpNpc1(workman) { it.npc.say(BUSY_LINES[random.of(BUSY_LINES.size)]) }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Dig_Site (starting the quest)
    // TODO(content): [quest:the_dig_site] quest start, exams and the after-quest dialogue
    // (https://oldschool.runescape.wiki/w/Transcript:Examiner)
    // TODO(content): [system:treasure_trails] hard clue puzzle box dialogue
    private suspend fun Dialogue.examiner() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Ah hello there! I am the resident lecturer on antiquities and artefacts. I also set " +
                "the Earth Sciences exams.",
        )
        chatPlayer(quiz, "Earth Sciences?")
        chatNpc(
            happy,
            "That is right dear, the world of Gielinor holds many wonders beneath its surface. " +
                "Students come to me to take exams so that they may join in on the archaeological " +
                "dig going on just north of here.",
        )
        chatPlayer(quiz, "So if they don't pass the exams they can't dig at all?")
        chatNpc(
            neutral,
            "That's right! We have to make sure that students know enough to be able to dig " +
                "safely and not damage the artefacts.",
        )
        when (choice2("Can I take an exam?", 1, "Interesting...", 2)) {
            1 -> {
                chatPlayer(quiz, "Can I take an exam?")
                chatNpc(neutral, "Are you sure you want to? Our exams are serious business.")
            }
            2 -> {
                chatPlayer(neutral, "Interesting...")
                chatNpc(happy, "You could gain much with an understanding of the world below.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Researcher
    // TODO(content): [quest:the_dig_site] after-quest greeting and the tools options
    private suspend fun Dialogue.researcher() {
        chatNpc(quiz, "Hello there. What are you doing here?")
        chatPlayer(neutral, "Just looking around at the moment.")
        chatNpc(
            happy,
            "Well, feel free to talk to me should you come across anything you can't figure out.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Terry_Balando
    // TODO(content): [quest:the_dig_site] after-quest dialogue and identifying used items
    // TODO(content): [quest:desert_treasure_i] after-quest translation line and the ancient staff
    // dialogue
    private suspend fun Dialogue.terryBalando() {
        chatPlayer(quiz, "Hello. Who are you?")
        chatNpc(
            happy,
            "Good day to you. My name is Terry Balando; I am an expert on digsite finds.",
        )
        chatNpc(
            neutral,
            "I am employed by the museum in Varrock to oversee all finds at this digsite. " +
                "Anything you find must be reported to me.",
        )
        chatPlayer(neutral, "Oh, ok. If I find anything of interest I will bring it here.")
        chatNpc(quiz, "Can I help you at all?")
        val option =
            choice4(
                "I have something I need checking out.",
                1,
                "No thanks.",
                2,
                "Can you tell me anything about the digsite?",
                3,
                "Can you tell me more about the tools an archaeologist uses?",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I have something I need checking out.")
                chatNpc(happy, "Okay, give it to me and I'll have a look for you.")
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(happy, "Good, let me know if you find anything unusual.")
            }
            3 -> {
                chatPlayer(quiz, "Can you tell me anything about the digsite?")
                chatNpc(
                    happy,
                    "Yes, indeed; I am studying the lives of the settlers. During the end of the " +
                        "third age there used to be a great city at the site.",
                )
                chatNpc(
                    neutral,
                    "Its inhabitants were humans, supporters of the god Saradomin. It's not " +
                        "recorded what happened to the community here.",
                )
                chatNpc(neutral, "I suspect nobody has lived here for over a millennium!")
                val next =
                    choice2(
                        "Can you tell me more about the tools an archaeologist uses?",
                        1,
                        "Thank you!",
                        2,
                    )
                when (next) {
                    1 -> archaeologistTools(anythingElse = false)
                    2 -> chatPlayer(happy, "Thank you!")
                }
            }
            4 -> archaeologistTools(anythingElse = false)
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Panning_guide
    // TODO(content): [quest:the_dig_site] panning permission and the cup of tea
    private suspend fun Dialogue.panningGuide() {
        chatPlayer(quiz, "Hello, who are you?")
        chatNpc(happy, "Hello, I am the panning guide. I'm here to teach you how to pan for gold.")
        chatPlayer(happy, "Excellent!")
        chatNpc(
            neutral,
            "Let me explain how panning works. First you need a panning tray. Use the tray in the " +
                "panning points in the water and then search your tray.",
        )
        chatNpc(
            neutral,
            "If you find any gold, take it to the archaeological expert up in the museum storage " +
                "facility. He will calculate its value for you.",
        )
        val option =
            choice2(
                "Can you tell me more about the tools an archaeologist uses?",
                1,
                "Thank you!",
                2,
            )
        when (option) {
            1 -> archaeologistTools(anythingElse = true)
            2 -> chatPlayer(happy, "Thank you!")
        }
    }

    private suspend fun Dialogue.archaeologistTools(anythingElse: Boolean) {
        chatPlayer(quiz, "Can you tell me more about the tools an archaeologist uses?")
        chatNpc(
            happy,
            "Of course! Let's see now... Rock picks are for splitting rocks or scraping away soil; " +
                "you can get one from a cupboard in the Education Centre.",
        )
        chatPlayer(quiz, "What about sample jars?")
        chatNpc(
            neutral,
            "I think you'll find them scattered about pretty much everywhere, but I know you can " +
                "get one from a cupboard somewhere in the Education Centre, just like the rock " +
                "pick!",
        )
        chatPlayer(quiz, "Okay, what about a specimen brush?")
        chatNpc(
            neutral,
            "We have a bit of a shortage of those at the moment. You could try borrowing one from " +
                "a workman on the site... but I don't think they'd give it willingly.",
        )
        chatPlayer(
            shifty,
            "Sounds like I'll need to be sneaky to get one of those, then... Okay - trowel?",
        )
        chatNpc(
            neutral,
            "Ahh... that you must earn by passing your exams! The examiner holds those.",
        )
        if (anythingElse) {
            chatPlayer(quiz, "Anything else?")
            chatNpc(
                neutral,
                "If you need something identified or are not sure about something, give it to " +
                    "Terry; he's the archaeological expert in the next room.",
            )
        }
        chatPlayer(happy, "Ahh, ok thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Digsite_workman
    // TODO(content): [quest:the_dig_site] after-quest greeting
    // TODO(content): [skill:thieving] Steal-from and the caught dialogue
    private suspend fun Dialogue.digsiteWorkman() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Good day; what can I do for you?")
        val option =
            choice3(
                "What do you do here?",
                1,
                "I'm not sure...",
                2,
                "Can I dig around here?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What do you do here?")
                chatNpc(
                    neutral,
                    "I am involved in various stages of the dig, from the initial investigation to " +
                        "the installation of the mine shafts.",
                )
                chatPlayer(neutral, "Oh, okay, thanks.")
            }
            2 -> {
                chatPlayer(confused, "I'm not sure...")
                chatNpc(neutral, "Well, let me know when you are.")
            }
            3 -> {
                chatPlayer(quiz, "Can I dig around here?")
                chatNpc(
                    neutral,
                    "You can only use a site you have the appropriate exam level for.",
                )
                val next =
                    choice2(
                        "Appropriate exam level?",
                        1,
                        "I am already skilled in digging.",
                        2,
                    )
                when (next) {
                    1 -> {
                        chatPlayer(quiz, "Appropriate exam level?")
                        chatNpc(
                            neutral,
                            "Yes, only persons with the correct Earth Sciences certificate can " +
                                "dig here. A level 1 certificate will let you dig in a level 1 " +
                                "site and so on.",
                        )
                        chatPlayer(neutral, "Ah, yes; I understand.")
                    }
                    2 -> {
                        chatPlayer(neutral, "I am already skilled in digging.")
                        chatNpc(
                            neutral,
                            "Well that's nice for you. You can't dig around here without a " +
                                "certificate though.",
                        )
                    }
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elissa
    // TODO(content): [quest:the_golem] quest dialogue
    private suspend fun Dialogue.elissa() {
        chatNpc(happy, "Hello there.")
        val option =
            choice3(
                "What do you do here?",
                1,
                "What is this place?",
                2,
                "What happened to the city?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What do you do here?")
                chatNpc(happy, "I'm helping with the dig. I'm an expert on Third Age architecture.")
            }
            2 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    happy,
                    "In the Third Age, this was a great city. Look at these giant walls! They put " +
                        "Varrock to shame!",
                )
                // TODO(content): [verify:in_game] whether "I don't know, Varrock is pretty
                // impressive." is a one-option menu
                chatPlayer(neutral, "I don't know, Varrock palace is impressive.")
                chatNpc(
                    angry,
                    "Hmph. I don't think it will look this good when it's been buried in the " +
                        "ground for three thousand years!",
                )
            }
            3 -> {
                chatPlayer(quiz, "What happened to the city?")
                chatNpc(neutral, "No one knows for sure.")
                chatNpc(
                    sad,
                    "But the Third Age was a time of destruction, when the gods were violently at " +
                        "war.",
                )
                chatNpc(sad, "Many great civilizations were destroyed then.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Errdo (east of Varrock)
    private suspend fun Dialogue.captainErrdo() {
        chatNpc(sad, "Ah, how embarrassing.")
        chatPlayer(quiz, "What happened?")
        chatNpc(
            sad,
            "A bit of a technical hitch with the landing gear. I won't be able to fly you " +
                "anywhere, sorry.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barge_foreman
    // TODO(content): [quest:bone_voyage] after-quest line
    private suspend fun Dialogue.bargeForeman() {
        chatPlayer(quiz, "What's going on here?")
        chatNpc(
            neutral,
            "We're making the final preparations for our voyage to the distant lands to the " +
                "North.",
        )
        when (choice2("Is there anything I can do to help?", 1, "Good luck, bye.", 2)) {
            1 -> {
                chatPlayer(quiz, "Is there anything I can do to help?")
                chatNpc(
                    neutral,
                    "We could do with an extra pair of hands, but you're better off speaking to " +
                        "Curator Haig Halen at the Varrock museum if you'd like to get involved.",
                )
                chatPlayer(happy, "Okay, thanks.")
            }
            2 -> chatPlayer(happy, "Good luck, bye.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barge_guard (before Bone Voyage)
    // TODO(content): [quest:bone_voyage] boarding the barge and Quick-Travel to Fossil Island
    private suspend fun Dialogue.bargeGuard() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello there, can I help you?")
        when (choice2("Can I go onto the barge?", 1, "No, thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "Can I go onto the barge?")
                chatNpc(
                    neutral,
                    "I'm afraid the team are still in the final stages of construction, you won't " +
                        "be able to go on board until they have finished. The foreman will know " +
                        "more if you have any questions.",
                )
            }
            2 -> chatPlayer(neutral, "No, thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ed_Wood
    private suspend fun Dialogue.edWood() {
        chatNpc(angry, ED_WOOD_LINES[access.random.of(ED_WOOD_LINES.size)])
    }

    private companion object {
        val EXAMINERS =
            listOf("npc.examiner", "npc.qip_digsite_examiner_02", "npc.qip_digsite_examiner_03")
        val DIGSITE_WORKMEN =
            listOf(
                "npc.digworkman1",
                "npc.qip_digsite_digworkman_03",
                "npc.qip_digsite_digworkman_04",
            )
        val BARGE_WORKMEN =
            listOf(
                "npc.vm_canal_barge_workman_hammering",
                "npc.vm_canal_barge_workman_sawing",
                "npc.vm_canal_barge_workman_sledgehammering",
                "npc.vm_canal_barge_workman_painting_tar",
            )
        val ED_WOOD_LINES =
            listOf(
                "Can't stop. Too busy.",
                "Wonder when I'll get paid.",
                "Is it lunch break yet?",
                "Hey I'm working here. I'm working here.",
                "This work isn't going to do itself.",
                "Ouch! That was my finger!",
            )
        val BUSY_LINES =
            listOf(
                "This work isn't going to do itself.",
                "Is it lunch break yet?",
                "Ouch! That was my finger!",
                "Hey I'm working here, I'm working here.",
                "Wonder when I'll get paid.",
                "Can't stop. Too busy.",
            )
    }
}
