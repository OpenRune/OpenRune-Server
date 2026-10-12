package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VarrockMuseum : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.vm_art_critic") { startDialogue(it.npc) { artCriticJacques() } }
        onOpNpc1("npc.vm_archaeologist1_npc_dummy") { startDialogue(it.npc) { archaeologist() } }
        onOpNpc1("npc.vm_archaeologist2_npc_dummy") { startDialogue(it.npc) { archaeologist() } }
        onOpNpc1("npc.vm_archaeologist3_npc_dummy") { startDialogue(it.npc) { archaeologist() } }
        onOpNpc1("npc.vm_archaeologist4_npc_dummy") { startDialogue(it.npc) { archaeologist() } }
        onOpNpc1("npc.vm_archaeologist5_npc_dummy") { startDialogue(it.npc) { archaeologist() } }
        onOpNpc1("npc.vm_archaeologist6_npc_dummy") { startDialogue(it.npc) { archaeologist() } }
        onOpNpc1("npc.vm_collector") { startDialogue(it.npc) { collector() } }
        onOpNpc1("npc.curator") { startDialogue(it.npc) { curator() } }
        onOpNpc1("npc.vm_fossil_expert") { startDialogue(it.npc) { david() } }
        onOpNpc1("npc.vm_timeline_historian") { startDialogue(it.npc) { historianMinas() } }
        onOpNpc1("npc.vm_info_booth_lady") { startDialogue(it.npc) { informationClerk() } }
        onOpNpc1("npc.vm_advent_01") { startDialogue(it.npc) { museumVisitor() } }
        onOpNpc1("npc.vm_advent_02") { startDialogue(it.npc) { museumVisitor() } }
        onOpNpc1("npc.vm_museum_guard") { startDialogue(it.npc) { cleaningAreaGuard() } }
        onOpNpc1("npc.vm_museum_guard2") { startDialogue(it.npc) { workmansGateGuard() } }
        onOpNpc1("npc.vm_museum_guard3") { startDialogue(it.npc) { workmansEntranceGuard() } }
        onOpNpc1("npc.vm_natural_historian_north") {
            startDialogue(it.npc) { naturalHistorianNorth() }
        }
        onOpNpc1("npc.vm_natural_historian_east") {
            startDialogue(it.npc) { naturalHistorianEast() }
        }
        onOpNpc1("npc.vm_natural_historian_south") {
            startDialogue(it.npc) { naturalHistorianSouth() }
        }
        onOpNpc1("npc.vm_natural_historian_west") {
            startDialogue(it.npc) { naturalHistorianWest() }
        }
        onOpNpc1("npc.vm_nathis_apprentice") { startDialogue(it.npc) { orlandoSmith() } }
        onOpNpc1("npc.vm_school_girl_blonde") { startDialogue(it.npc) { schoolchild() } }
        onOpNpc1("npc.vm_school_girl_brunette") { startDialogue(it.npc) { schoolchild() } }
        onOpNpc1("npc.vm_school_boy_blonde") { startDialogue(it.npc) { schoolchild() } }
        onOpNpc1("npc.vm_school_boy_brunnete") { startDialogue(it.npc) { schoolchild() } }
        onOpNpc1("npc.vm_teacher_girl_multi") {
            startDialogue(it.npc) { teacherAndPupil(FEMALE_TEACHER, GIRL_PUPIL) }
        }
        onOpNpc1("npc.vm_school_teacher_male_and_boy") {
            startDialogue(it.npc) { teacherAndPupil(MALE_TEACHER, BOY_PUPIL) }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Art_Critic_Jacques
    private suspend fun Dialogue.artCriticJacques() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Ah, many greetings, and welcome to the Museum!")
        when (choice2("Who are you?", 1, "What do you do here?", 2)) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(happy, "My name is Jacques and I am the Museum's finest art critic!")
                chatPlayer(neutral, "Looks to me like you're the only one.")
                chatNpc(
                    happy,
                    "This is true. However, in the future, I will have my own entire floor of " +
                        "the Museum with paintings galore!",
                )
                chatPlayer(confused, "Er... has that been approved, or are you just dreaming?")
                chatNpc(laugh, "Ah, I see you're being witty, eh?")
                chatPlayer(confused, "I am?")
                chatNpc(
                    angry,
                    "Yes! You're poking fun at me! I shall have my gallery. You will see " +
                        "eventually. Then I shall be the one with the last laugh!",
                )
            }
            2 -> {
                chatPlayer(quiz, "What do you do here?")
                chatNpc(
                    happy,
                    "I critique the art! See the beautiful colours and the masterful use of the " +
                        "brush to capture perfectly the royal presence...",
                )
                when (choice2("Umm...it's a painting.", 1, "Yes, I see!", 2)) {
                    1 -> {
                        chatPlayer(confused, "Er... it's a painting.")
                        chatNpc(
                            angry,
                            "Just a painting? How could you say such a thing? When you learn how " +
                                "to appreciate the wonderful works of art, you may talk to me.",
                        )
                    }
                    2 -> {
                        chatPlayer(happy, "Yes, I see!")
                        chatPlayer(
                            happy,
                            "And look at the use of shade and contrast - isn't it just " +
                                "magnificent?",
                        )
                        chatNpc(
                            happy,
                            "So it is. So perfect, it almost makes me weep with joy! The " +
                                "grandiloquent strokes of the brush convey a kind of mystic " +
                                "quality... It is almost as if he were here with us.",
                        )
                        chatNpc(
                            happy,
                            "No words can truly describe my feelings, so let us continue to " +
                                "admire this work in silence.",
                        )
                    }
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barnabus_Hurma (the same for all six)
    private suspend fun Dialogue.archaeologist() {
        when (access.random.of(3)) {
            0 -> chatNpc(happy, "Hello there; I see you're qualified. Come to help us out?")
            1 ->
                chatNpc(
                    happy,
                    "Hello! You're that student who recently finished all three Earth Sciences " +
                        "exams, aren't you? Come to help us out?",
                )
            else -> {
                val title = if (access.isBodyTypeA()) "sir" else "madam"
                chatNpc(happy, "Greetings, my dear $title. Have you come to give us a hand?")
            }
        }
        val option =
            choice3(
                "Yes, how can I help out?",
                1,
                "I found something interesting.",
                2,
                "No thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Yes, how can I help out?")
                if (!hasCleaningEquipment()) {
                    chatNpc(
                        neutral,
                        "Well, you'll need to get your equipment first - it's all there on the " +
                            "tool rack. Use what you learned from your Earth Sciences exams. " +
                            "You'll need to be wearing your leather gloves and boots as well as " +
                            "have access to",
                    )
                    chatNpc(neutral, "your trowel, rock pick and specimen brush.")
                    return
                }
                chatNpc(happy, "I see you have your equipment - well done.")
                chatNpc(
                    happy,
                    "To really give us a hand, you could clean some finds for us. They're mostly " +
                        "done - just some fine work to finish off. Take a find from the pile " +
                        "that the workmen are bringing in, then there are spare tables over there",
                )
                chatNpc(
                    happy,
                    "to clean them on. If you find anything interesting, let us know and we'll " +
                        "see if it's good enough to go on display.",
                )
            }
            2 -> {
                chatPlayer(happy, "I found something interesting.")
                chatNpc(quiz, "Oh? Let's take a look...")
                // TODO(content): [system:museum_finds] clean necklace, old/ancient coin,
                //  old/ancient symbol, pottery, arrowheads, jewellery and vase hand-ins (also
                //  item-on-npc)
                chatPlayer(sad, "Err... I seem to have lost it. Sorry.")
                val lad = if (access.isBodyTypeA()) "lad" else "lass"
                chatNpc(laugh, "Sounds like you never had it, $lad!")
            }
            3 -> chatPlayer(neutral, "No, just here to watch.")
        }
    }

    private fun Dialogue.hasCleaningEquipment(): Boolean =
        "obj.trowel" in access.inv &&
            "obj.rockpick" in access.inv &&
            "obj.specimen_brush" in access.inv &&
            "obj.leather_boots" in access.worn &&
            "obj.leather_gloves" in access.worn

    // https://oldschool.runescape.wiki/w/Transcript:The_Collector
    // TODO(content): [system:collection_log] collection log offer when the player has none,
    //  Get-log and Claim-log-rewards ops
    // TODO(content): [event:varlamore_tokens] red token dialogue
    private suspend fun Dialogue.collector() {
        chatNpc(happy, "It's beautiful, isn't it?")
        chatPlayer(confused, "What is?")
        chatNpc(
            happy,
            "Everything! The wonders right here in this museum, collected from all corners of " +
                "the land.",
        )
        chatPlayer(neutral, "I guess it is, you're right.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Curator_Haig_Halen
    // TODO(content): [quest:bone_voyage] "Talk about Fossil Island." and after-quest news
    // TODO(content): [quest:below_ice_mountain] Camdozaal relic hand-in
    // TODO(content): [system:shipwreck_salvaging] salvaging relic hand-ins
    // TODO(content): [system:museum_digsite_certificates] showing digsite certificates
    private suspend fun Dialogue.curator() {
        chatNpc(happy, "Welcome to the museum of Varrock.")
        val option =
            choice3(
                "Have you any interesting news?",
                1,
                "Do you know where I could find any treasure?",
                2,
                "How do I get a medallion?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Have you any interesting news?")
                chatNpc(
                    happy,
                    "Yes, we found a rather interesting island to the North of Morytania. We " +
                        "believe that it may be of archaeological significance.",
                )
                chatPlayer(happy, "Oh? That sounds interesting.")
                chatNpc(
                    neutral,
                    "Indeed. We are making the final preparations for our voyage, in fact, you " +
                        "may be just the person we are looking for... Assuming you've got the " +
                        "necessary qualifications, that is.",
                )
                chatPlayer(quiz, "Would I qualify then?")
                // TODO(content): [quest:bone_voyage] continues into the quest start
            }
            2 -> {
                chatPlayer(quiz, "Do you know where I could find any treasure?")
                chatNpc(happy, "Look around you! This museum is full of treasures!")
                chatPlayer(angry, "No, I meant treasures for ME.")
                chatNpc(
                    neutral,
                    "Any treasures this museum knows about it goes to great lengths to acquire.",
                )
            }
            3 -> {
                // TODO(content): [system:museum_kudos] digsite pendant discovered: medallion
                //  requirements, ruby part payments and the medallion exchange
                chatNpc(
                    neutral,
                    "Well, firstly you'll need to do a bit of work around the museum, then maybe " +
                        "we can talk.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:David_(Varrock_Museum)
    private suspend fun Dialogue.david() {
        chatNpc(
            happy,
            "Hello, young adventurer. What brings you to the Museum's paleontology floor?",
        )
        while (true) {
            val option =
                choice5(
                    "What's paleontology?",
                    1,
                    "Where do these fossils come from?",
                    2,
                    "What can I do with these fossils?",
                    3,
                    "What can you tell me about some of the fossil displays?",
                    4,
                    "Nevermind",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's paleontology?")
                    chatNpc(
                        happy,
                        "I'm glad you asked! Paleontology is the study of prehistoric lifeforms " +
                            "in Gielinor through the examination of fossils.",
                    )
                    chatNpc(
                        happy,
                        "We use fossils to try determine several things: information about what " +
                            "fossil belonged to, what the environment was like, and try to " +
                            "uncover secrets of Gielinor's past.",
                    )
                    chatNpc(quiz, "What else would you like to know?")
                }
                2 -> {
                    chatPlayer(quiz, "Where do these fossils come from?")
                    chatNpc(
                        happy,
                        "Ah, brilliant question. Fossils of creatures are found in sedimentary " +
                            "rock, we use specialised tools to carefully remove the unwanted " +
                            "rock only leaving only the fossil behind.",
                    )
                    chatNpc(
                        neutral,
                        "If you're looking for fossils yourself, I hear there is a voyage to a " +
                            "mysterious island to discover new types of fossil.",
                    )
                    chatNpc(
                        neutral,
                        "You should speak to the Curator as he knows more about it than I.",
                    )
                    chatNpc(quiz, "Is there anything else you need help with?")
                }
                3 -> {
                    chatPlayer(quiz, "What can I do with these fossils?")
                    chatNpc(
                        neutral,
                        "Any fossils you find can be added to the surrounding display cases.",
                    )
                    chatNpc(
                        neutral,
                        "If you manage to complete display case, the Museum will ensure you are " +
                            "rewarded for your efforts.",
                    )
                    chatNpc(
                        neutral,
                        "Also, if you find any rare fossils on your travels, make sure you hold " +
                            "onto them. Something tells me they will be very valuable in the " +
                            "future...",
                    )
                    chatNpc(quiz, "Do you have any other questions?")
                }
                4 -> {
                    chatPlayer(quiz, "What can you tell me about some of the fossil displays?")
                    // TODO(content): [system:museum_fossil_displays] completed display dialogue
                    chatNpc(
                        neutral,
                        "It looks like the Museum doesn't have any complete fossil displays yet. " +
                            "I could only speculate on what they will contain.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Nevermind.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Historian_Minas
    private suspend fun Dialogue.historianMinas() {
        chatPlayer(happy, "Hello!")
        chatNpc(happy, "Hello there, welcome to Varrock Museum! Can I help you?")
        val option =
            choice3(
                "Tell me about the Museum.",
                1,
                "I have some information which might be of use in your displays.",
                2,
                "Tell me about other sections of the Museum.",
                3,
            )
        when (option) {
            1 -> minasMuseum()
            2 -> minasInformation()
            3 -> minasOtherSections()
        }
    }

    private suspend fun Dialogue.minasMuseum() {
        chatPlayer(quiz, "Tell me about the Museum.")
        chatNpc(
            happy,
            "Well, as you can see we have recently expanded a great deal to cope with the influx " +
                "of finds from the Dig Site. Also, of course, to prepare for the new dig we're " +
                "opening soon.",
        )
        while (true) {
            val option =
                choice3(
                    "Tell me about the new dig.",
                    1,
                    "I have some information which might be of use in your displays.",
                    2,
                    "Tell me about other sections of the Museum.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the new dig.")
                    chatNpc(
                        happy,
                        "Well, we recently had a large chunk of funds donated to us, so we sent " +
                            "out all sorts of explorers - I'm sure you'll meet them as you go " +
                            "around the world. People like Simon down in the desert near the " +
                            "pyramid as well",
                    )
                    chatNpc(
                        happy,
                        "as Aristarchus in Pollnivneach, Varmen and, of course, Anna Jones. " +
                            "Anyway, we think we have discovered an island that has massive " +
                            "archaeological significance. We can't say much beyond that at this " +
                            "point, as we haven't",
                    )
                    chatNpc(
                        happy,
                        "researched it. If you have the qualifications and help us out around " +
                            "the museum, I'm sure we could offer you a place on the dig when " +
                            "we're ready to go.",
                    )
                    chatPlayer(happy, "That would be very nice.")
                }
                2 -> return minasInformation()
                else -> return minasOtherSections()
            }
        }
    }

    // TODO(content): [system:museum_timeline] quest reports (Demon Slayer, Shield of Arrav,
    //  Rune Mysteries, Observatory Quest, Merlin's Crystal, The Grand Tree, Priest in Peril,
    //  In Aid of the Myreque, A Tail of Two Cats, Making History, Temple of Ikov, Curse of the
    //  Empty Lord, Hazeel Cult, What Lies Below, Defender of Varrock), kudos and old lamps
    private suspend fun Dialogue.minasInformation() {
        chatPlayer(happy, "I have some information that might be of use in your displays.")
        chatNpc(
            happy,
            "That's grand! We're always glad to have more facts. Now, tell me what you know...",
        )
        chatPlayer(neutral, "On second thoughts, you seem to have everything I know covered here!")
        chatNpc(
            neutral,
            "No matter. I'm sure you'll find more. Look around the Museum, explore the city and " +
                "maybe do some quests, and I'm sure you'll find out something worth telling.",
        )
        chatPlayer(neutral, "Yes, I'll go do some quests or something...")
    }

    private suspend fun Dialogue.minasOtherSections() {
        chatPlayer(quiz, "Tell me about other sections of the Museum.")
        chatNpc(happy, "I'd be delighted to; which areas are you interested in?")
        while (true) {
            val option =
                choice4(
                    "Natural History exhibit",
                    1,
                    "Dig Site exhibit",
                    2,
                    "Dig Site find cleaning area",
                    3,
                    "Timeline exhibit",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the Natural History exhibit.")
                    chatNpc(
                        happy,
                        "Ahh, our newest expansion. The Natural History exhibit in the basement " +
                            "was set up to inform our citizens of the weird and wonderful " +
                            "diversity in our environment today. Indeed, I am sure you will " +
                            "recognise some of",
                    )
                    chatNpc(
                        happy,
                        "the creatures down there. Hopefully, our lectures will teach you some " +
                            "things you didn't know about them!",
                    )
                    chatPlayer(confused, "So where does the 'history' bit come in?")
                    chatNpc(
                        neutral,
                        "Ahh, I see. The 'history' refers to that of the species - it doesn't " +
                            "necessarily mean it's died out. After all, we have human history " +
                            "and we're still here!",
                    )
                    chatPlayer(neutral, "I guess that makes sense.")
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about the Dig Site exhibit.")
                    chatNpc(
                        neutral,
                        "Ahh, those. We're slowly filling them up with the help of people who " +
                            "have their Earth Sciences exams completed at the Dig Site. If you " +
                            "have, feel free to give us a hand. We'd appreciate it.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about the Dig Site find cleaning area.")
                    chatNpc(
                        neutral,
                        "The cleaning area, yes. Well, we have some really good archaeologists " +
                            "on site right now. They're cleaning up the finds that are shipped " +
                            "over from the Dig Site to the east of here. If you have the right " +
                            "qualifications, you can help them clean the finds.",
                    )
                    chatPlayer(quiz, "What qualifications?")
                    chatNpc(
                        neutral,
                        "Level 3 Earth Science exams. You can take them over at the Exam Centre " +
                            "on the Dig Site.",
                    )
                    // TODO(content): [quest:the_dig_site] working towards / passed the exams
                    chatPlayer(sad, "Oh, I don't have those.")
                    chatNpc(
                        happy,
                        "I'm sure you can get them easily enough. Just pop over to the Exam " +
                            "Centre on the Dig Site and speak to one of the examiners there.",
                    )
                }
                else -> {
                    chatPlayer(quiz, "Tell me about the Timeline exhibit.")
                    chatNpc(
                        happy,
                        "Ahh, my speciality. Being a historian, matters of the past are my " +
                            "passion. Collated on this floor and the one above are just part of " +
                            "my life's work. I am still working on updating and finding out more " +
                            "everyday, so if you find",
                    )
                    chatNpc(
                        happy,
                        "any information on your travels that may be useful, please do let me " +
                            "know.",
                    )
                    chatNpc(
                        happy,
                        "You can follow the timeline of the history of Gielinor from the " +
                            "creation of the world to the present day, simply by walking around " +
                            "this display. A wondrous journey, I think you'll agree!",
                    )
                    chatPlayer(
                        happy,
                        "Wow, that's a lot of information. I'm sure in my travels and quests " +
                            "I'll pick up a few bits of information for you.",
                    )
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Information_clerk
    // TODO(content): [system:museum_kudos] kudos skill rewards greeting, exhibit progress
    //  lines and "Would I qualify, then?" with enough kudos
    // TODO(content): [miniquest:crack_the_clue] Crack the Clue III dialogue
    private suspend fun Dialogue.informationClerk() {
        chatNpc(happy, "Welcome to Varrock Museum. How can I help you today?")
        while (true) {
            val option =
                choice5(
                    "Take a map of the Museum",
                    1,
                    "Find out about the Dig Site exhibit.",
                    2,
                    "Find out about the Timeline exhibit.",
                    3,
                    "Find out about the Natural History exhibit.",
                    4,
                    "Find out about Kudos.",
                    5,
                )
            when (option) {
                1 -> {
                    // TODO(content): [system:museum_map] "You reach and take a map of the
                    //  Museum." and receive obj museum map
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Could you tell me about the Dig Site exhibit please?")
                    chatNpc(
                        happy,
                        "Of course. The Dig Site exhibit has several display cases of finds " +
                            "discovered on the Dig Site to the east of Varrock.",
                    )
                    // TODO(content): [quest:the_dig_site] passed exams / exhibit progress lines
                    chatNpc(
                        neutral,
                        "Unfortunately, you haven't passed your Earth Science exams, so you " +
                            "aren't qualified to help us on the dig. If you're interested, visit " +
                            "the Education Centre on the Dig Site to the east and talk to the " +
                            "examiners there.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Could you tell me about the Timeline exhibit please?")
                    chatNpc(
                        happy,
                        "Why, yes. The Timeline exhibit has lots of display cases showing things " +
                            "from the beginning of time right up to the present day.",
                    )
                    // TODO(content): [system:museum_timeline] contributed / completed lines
                    chatNpc(
                        happy,
                        "You could help out in the Timeline exhibit upstairs. By providing " +
                            "information you may have gathered on your travels around the " +
                            "world, you can help to make our displays more comprehensive and " +
                            "earn yourself Kudos",
                    )
                    chatNpc(happy, "too.")
                    chatPlayer(
                        quiz,
                        "Okay, thanks. One more question: why are the display numbers all out " +
                            "of sequence?",
                    )
                    chatNpc(
                        neutral,
                        "Ahh, that's due to the numbering being done as we were constructing " +
                            "the cases and putting the displays in them, then shuffling them " +
                            "into the right places. We thought rather than renumbering them all " +
                            "- such a",
                    )
                    chatNpc(
                        neutral,
                        "boring job, writing labels - we'd leave it. They all have unique " +
                            "numbers and future displays would mess up the consecutive " +
                            "numbering anyway.",
                    )
                    chatPlayer(neutral, "Ahhh, I see.")
                }
                4 -> {
                    chatPlayer(quiz, "Could you tell me about the Natural History exhibit please?")
                    chatNpc(
                        happy,
                        "Why, yes. The Natural History exhibit has displays of various creatures " +
                            "you can find around Gielinor.",
                    )
                    // TODO(content): [system:museum_quiz] corrected some / all display cases
                    chatNpc(
                        happy,
                        "Downstairs, in the Natural History exhibit, you can answer quizzes " +
                            "about animals to show us your knowledge and earn Kudos at the same " +
                            "time. Talk to Orlando, I'm sure he'd appreciate your knowledge.",
                    )
                    val sub =
                        choice3(
                            "But what's Natural History got to do with existing animals?",
                            1,
                            "Ask about something else.",
                            2,
                            "Bye",
                            3,
                        )
                    when (sub) {
                        2 -> continue
                        3 -> return clerkBye()
                    }
                    chatPlayer(
                        confused,
                        "But what's natural history got to do with existing animals?",
                    )
                    chatNpc(
                        neutral,
                        "The study of natural history is simply the study of the history of " +
                            "the species. The species doesn't necessarily need to be an extinct " +
                            "one.",
                    )
                }
                else -> {
                    chatPlayer(quiz, "What is Kudos?")
                    chatNpc(
                        neutral,
                        "Kudos is a measure of how much you've assisted the Museum. The more " +
                            "information you give us, Dig Site finds that you clean and quizzes " +
                            "you solve, the higher your Kudos.",
                    )
                    chatPlayer(quiz, "But what's it for?")
                    chatNpc(
                        neutral,
                        "Well, recently we found a rather interesting island to the north of " +
                            "Morytania. We believe that it may be of archaeological " +
                            "significance. I suspect we'll be looking for qualified " +
                            "archaeologists once we have constructed our",
                    )
                    chatNpc(
                        neutral,
                        "canal and barge. So, we're using Kudos as a measure of who is willing " +
                            "and able to help us here at the Museum, so they can then be invited " +
                            "on our dig on the new island.",
                    )
                    chatPlayer(quiz, "Would I qualify, then?")
                    chatNpc(
                        neutral,
                        "Unfortunately, you haven't earned enough Kudos yet, so you aren't " +
                            "qualified to help us on the dig. If you're interested in helping us " +
                            "out and getting that Kudos, simply help out around the Museum.",
                    )
                    chatPlayer(neutral, "Okay, thanks.")
                }
            }
            if (choice2("Ask about something else.", 1, "Bye", 2) == 2) {
                return clerkBye()
            }
        }
    }

    private suspend fun Dialogue.clerkBye() {
        chatPlayer(happy, "Bye.")
        chatNpc(happy, "Have a good day!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Torrcs (Marfet shares these dialogues:
    // https://oldschool.runescape.wiki/w/Transcript:Marfet)
    private suspend fun Dialogue.museumVisitor() {
        when (access.random.of(5)) {
            0 -> {
                chatPlayer(happy, "Hello.")
                chatNpc(happy, "Hi there. So much to see here!")
                chatPlayer(happy, "Yep, could be looking all day!")
                chatPlayer(quiz, "Updated any displays yet?")
                chatNpc(
                    happy,
                    "Yes, I just learned more about some priestly warriors and a staff of the " +
                        "gods.",
                )
                chatPlayer(quiz, "Sounds fun, how did you do that?")
                chatNpc(
                    happy,
                    "Ahh, I'm big on quests me, love questing. So when I've done one, I check " +
                        "here with 'Historian' Minas to see if I can help the Museum out with " +
                        "some information. Sometimes they already know it, or don't want to know,",
                )
                chatNpc(happy, "and sometimes it earns me a reward or two.")
                chatPlayer(happy, "That sounds really good. Perhaps I should do the same!")
                chatNpc(
                    happy,
                    "You should, as well as everything else there is here in the Museum. Did you " +
                        "know they have a really fun- looking display down in the basement? I " +
                        "love monkeys.",
                )
                chatPlayer(happy, "I'll check it out. Got to get going now, nice talking to you.")
                chatNpc(happy, "Have a good day!")
            }
            1 -> {
                chatNpc(happy, "Hi there!")
                chatPlayer(quiz, "Hello, what are you doing here?")
                chatNpc(
                    happy,
                    "Oh, just taking a look at these displays. I'm a bit of a history buff.",
                )
                chatPlayer(happy, "Really? Tell me something I didn't know!")
                chatNpc(
                    shifty,
                    "Well, did you know that Bob the Cat is... no, no... you'd never believe me " +
                        "anyway.",
                )
                chatPlayer(quiz, "He's what? Tell me!")
                chatNpc(shifty, "No, no. Couldn't possibly ruin the surprise.")
                chatPlayer(angry, "ARG! Tell me!")
                chatNpc(
                    neutral,
                    "If you really want to know, go talk to Unferth in his house in Burthorpe.",
                )
            }
            2 -> {
                chatPlayer(happy, "Hi!")
                chatNpc(neutral, "Oh, hello. I was just looking at this display here.")
                chatPlayer(quiz, "Anything interesting?")
                chatNpc(
                    happy,
                    "Yes, one I just updated by talking to 'Historian' Minas. I'm a bit of a " +
                        "quester you see.",
                )
                chatPlayer(neutral, "Okay, I'll let you get on then.")
                chatNpc(
                    neutral,
                    "Hmm, yes. Okay. Don't forget to check out the Natural History exhibit - you " +
                        "could learn a lot.",
                )
                chatPlayer(happy, "Right, bye!")
            }
            3 -> {
                chatNpc(happy, "Hello!")
                chatPlayer(quiz, "Hi, have you seen the Dig Site displays?")
                chatNpc(happy, "Yes, good aren't they! Have you done the quest?")
                // TODO(content): [quest:the_dig_site] "Yes, I did it all." after the quest
                chatPlayer(neutral, "Not yet.")
                chatNpc(
                    happy,
                    "Oh you should, it lets you get into that bit where they clean the finds, " +
                        "and you can join in!",
                )
                chatPlayer(happy, "Sounds fun, I should probably check it out.")
            }
            else -> {
                chatPlayer(happy, "Hi!")
                chatNpc(confused, "Hmm... 3rd Age.... No, no...maybe 4th...")
                chatPlayer(quiz, "Hello?")
                chatNpc(
                    confused,
                    "Yes, that's it, just like that. Mustn't forget the map either....hmm...what " +
                        "else?",
                )
                chatPlayer(angry, "HELLO! Are you there?")
                chatNpc(
                    shocked,
                    "Oh, hello there. Sorry, was a bit engrosed in updating this display. I've " +
                        "just been out questing and thought I'd stop off here you see. It's " +
                        "quite rewarding too.",
                )
                chatPlayer(angry, "Yes, well, no need to ignore me.")
                chatNpc(
                    neutral,
                    "No offence intended, my dear chap. Just very interested in this history " +
                        "stuff, that's all.",
                )
                chatPlayer(quiz, "Okay. Have you seen the Natural History exhibit in the basement?")
                chatNpc(
                    happy,
                    "Oh, yes, marvellous isn't it! I particularly like the wyvern, very " +
                        "interesting considering they've never found a live specimen. And to " +
                        "think they could possibly have been flapping around at one time!",
                )
                chatPlayer(
                    laugh,
                    "A bit like the crazy gnomes and their gliders. Have you seen the model they " +
                        "have upstairs?",
                )
                chatNpc(
                    neutral,
                    "Yes, very intricate isn't it? Worrying to think some people risk their " +
                        "lives in them! You wouldn't catch me up in one. Had a chat with Jacques " +
                        "while I was up there, the art critic...he's a bit funny in the head.",
                )
                chatPlayer(angry, "That's not very nice!")
                chatNpc(
                    neutral,
                    "Well, he thinks he's going to get an entire portrait gallery to himself!",
                )
                chatPlayer(
                    neutral,
                    "Who knows, the Museum just got really big and appears to have cash. They " +
                        "might entertain him with his very own floor - we'll see.",
                )
                chatNpc(
                    happy,
                    "I guess so. Wonder what poetry he'll come up with then! Still, I must get " +
                        "on with this display. See you around I hope!",
                )
                chatPlayer(happy, "Bye!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Museum_guard (museum gate, cleaning area)
    // TODO(content): [quest:the_dig_site] gate prompt into the cleaning area after the quest
    private suspend fun Dialogue.cleaningAreaGuard() {
        chatNpc(neutral, "You're not permitted in this area.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Museum_guard (Dig Site gate)
    // TODO(content): [system:museum_kudos] "As a friend of the museum..." with 153 kudos
    private suspend fun Dialogue.workmansGateGuard() {
        chatNpc(
            neutral,
            "Hello there! Sorry, I can't stop to talk. I'm guarding this workman's gate. I'm " +
                "afraid you can't come through here - you'll need to find another way around.",
        )
        mesbox("You need 153 kudos to pass through here.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Museum_guard (museum doors)
    // TODO(content): [quest:the_dig_site] after-quest "you can go through if you want" lines
    private suspend fun Dialogue.workmansEntranceGuard() {
        chatNpc(happy, "Hello there. Come to see the new museum?")
        chatPlayer(quiz, "Yes, how do I get in?")
        chatNpc(
            happy,
            "Well, the main entrance is 'round the front. Just head west then north slightly, " +
                "you can't miss it!",
        )
        chatPlayer(quiz, "What about these doors?")
        chatNpc(
            neutral,
            "They're for the workmen bringing finds from the Dig Site; sorry, but you can't go " +
                "through.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Natural_historian
    // TODO(content): [system:museum_natural_history] first-talk introduction ("Hello there and
    //  welcome to the Natural History exhibit...")
    private suspend fun Dialogue.naturalHistorianGreeting() {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(happy, "Hello again, $title, how can I help you on this fine day?")
        chatPlayer(quiz, "I was hoping you could tell me about something.")
    }

    // TODO(content): [system:museum_lectures] the lecture cutscene at the display case, and
    //  "I don't allow pets in my lectures" with a follower
    private suspend fun Dialogue.lecture(question: String, intro: String) {
        chatPlayer(quiz, question)
        chatNpc(happy, intro)
        chatNpc(happy, "If you just follow me to the display case I shall explain all about them.")
    }

    private suspend fun Dialogue.enoughEducation() {
        chatPlayer(neutral, "That's enough education for one day.")
        chatNpc(laugh, "Nonsense! There's always room for more.")
        chatNpc(happy, "And remember, science isn't dull!")
    }

    private suspend fun Dialogue.naturalHistorianNorth() {
        naturalHistorianGreeting()
        val option =
            choice5(
                "Tell me about lizards.",
                1,
                "Tell me about tortoises.",
                2,
                "Tell me about dragons.",
                3,
                "Tell me about wyverns.",
                4,
                "That's enough education for one day.",
                5,
            )
        when (option) {
            1 -> lecture("Tell me about lizards.", "Ahh lizards, the scaly carnivores.")
            2 -> lecture("Tell me about the tortoises.", "Ahh tortoises, the armoured ancients.")
            3 -> lecture("Tell me about dragons.", "Ahh dragons, the mighty hunters of the sky.")
            4 -> lecture("Tell me about the wyverns.", "Ahh, wyverns. The extinct lizards.")
            else -> enoughEducation()
        }
    }

    private suspend fun Dialogue.naturalHistorianEast() {
        naturalHistorianGreeting()
        val option =
            choice5(
                "Tell me about snails.",
                1,
                "Tell me about monkeys.",
                2,
                "Tell me about sea slugs.",
                3,
                "Tell me about snakes.",
                4,
                "That's enough education for one day.",
                5,
            )
        when (option) {
            1 -> lecture("Tell me about snails.", "Ahh snails, the gelatinous gastropods.")
            2 -> lecture("Tell me about monkeys.", "Ahh monkeys, the simian collective.")
            3 -> lecture("Tell me about the sea slugs.", "Ahh sea slugs, the cute crustaceans.")
            4 -> lecture("Tell me about snakes.", "Ahh snakes, the slithering squamata.")
            else -> enoughEducation()
        }
    }

    private suspend fun Dialogue.naturalHistorianSouth() {
        naturalHistorianGreeting()
        val option =
            choice4(
                "Tell me about natural history.",
                1,
                "Tell me about terrorbirds.",
                2,
                "Tell me about the Kalphite Queen.",
                3,
                "That's enough education for one day.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Tell me about natural history.")
                chatNpc(
                    happy,
                    "Well, the field of natural history covers a wide range of sciences.",
                )
                chatNpc(
                    happy,
                    "So we use biology, the study of living things, botany, the study of plants " +
                        "and zoology, the study of animals.",
                )
                chatNpc(
                    happy,
                    "Though the field is growing all the time and we're also using techniques " +
                        "from magic, astrology and numerology.",
                )
                chatNpc(happy, "A person interested in natural history is known as a naturalist.")
            }
            2 ->
                lecture(
                    "Tell me about the terrorbirds.",
                    "Ahh terrorbirds, the fastest bird on two legs.",
                )
            3 ->
                lecture(
                    "Tell me about the Kalphite Queen.",
                    "Ahh kalphites, the insectoid eating machines.",
                )
            else -> enoughEducation()
        }
    }

    private suspend fun Dialogue.naturalHistorianWest() {
        naturalHistorianGreeting()
        val option =
            choice5(
                "Tell me about camels.",
                1,
                "Tell me about leeches.",
                2,
                "Tell me about moles.",
                3,
                "Tell me about penguins.",
                4,
                "That's enough education for one day.",
                5,
            )
        when (option) {
            1 -> lecture("Tell me about camels.", "Ahh camels, the ships of the desert.")
            2 -> lecture("Tell me about leeches.", "Ahh leeches, the haemophagic parasites.")
            3 -> lecture("Tell me about moles.", "Ahh moles, the mammalian mountain makers.")
            4 -> lecture("Tell me about penguins.", "Ahh penguins, the cunning birds of the sea.")
            else -> enoughEducation()
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Orlando_Smith (before the quiz)
    // TODO(content): [system:museum_quiz] accepting starts the natural history quiz; progress,
    //  completion reward (Slayer and Hunter xp) and after-quiz lines
    private suspend fun Dialogue.orlandoSmith() {
        chatNpc(happy, "G'day there, mate.")
        chatPlayer(quiz, "Good day. Are you alright? You look a little lost.")
        chatNpc(
            sad,
            "Well, mate, to tell you the truth, I think I've come a gutser with these displays.",
        )
        chatPlayer(confused, "Come a what?")
        chatNpc(
            sad,
            "Gutser and no mistake. Me boss asked me to put together a quiz for the visitors.",
        )
        chatNpc(
            sad,
            "But to be deadset with you, I wasn't paying much attention to me boss over there " +
                "and I've done a bit of a rush job.",
        )
        chatPlayer(quiz, "You mean the natural historian?")
        chatNpc(neutral, "Yep, that's the bloke. Say, mate, you do me a favour?")
        chatPlayer(quiz, "Perhaps. What do you need?")
        chatNpc(
            happy,
            "Well, you look like a pretty smart cobber. Could you take a look at the display " +
                "plaques and give 'em a runthrough?",
        )
        when (choice2("Sure thing.", 1, "No thanks I'm too busy.", 2)) {
            1 -> chatPlayer(happy, "Sure thing.")
            2 -> {
                chatPlayer(neutral, "No thanks. I'm too busy.")
                chatNpc(neutral, "Fair dinkum mate. I'm sure I'll get someone else to help me.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Schoolgirl
    // https://oldschool.runescape.wiki/w/Transcript:Schoolboy
    private suspend fun Dialogue.schoolchild() {
        val line =
            when (access.random.of(9)) {
                0 -> "*sniff* They won't let me take an arrowhead as a souvenir."
                1 -> "I wonder what they're doing behind that rope."
                2 -> "Can you find my teacher? I need the toilet!"
                3 -> "*cough* It's so dusty in here."
                4 -> "Teacher! Can we go to the Natural History exhibit now?"
                5 -> "Maz...Zar...Za-mor-ak is bestest!"
                6 -> "I wanna be an archaeologist when I grow up!"
                7 -> "Yaaay! A day off school."
                else -> "Sada...Sram...Sa-ra-do-min is bestest!"
            }
        chatNpc(happy, line)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Teacher_and_pupil
    private suspend fun Dialogue.teacherAndPupil(teacher: String, pupil: String) {
        if (player.coords.level == 2) {
            chatNpcSpecific(PUPIL, pupil, happy, "That man over there talks funny, miss.")
            chatNpcSpecific(
                TEACHER,
                teacher,
                neutral,
                "That's because he's an art critic, dear. They have some very funny ideas.",
            )
            return
        }
        chatNpcSpecific(
            TEACHER,
            teacher,
            neutral,
            "Stop pulling, we've plenty of time to see everything.",
        )
        chatNpcSpecific(PUPIL, pupil, happy, "Aww, but miss, it's sooo exciting.")
    }

    private companion object {
        const val TEACHER = "Teacher"
        const val PUPIL = "Pupil"
        const val FEMALE_TEACHER = "npc.vm_female_school_teacher_dummy"
        const val GIRL_PUPIL = "npc.vm_female_school_teacher_girl_dummy"
        const val MALE_TEACHER = "npc.vm_male_school_teacher_chathead_dummy"
        const val BOY_PUPIL = "npc.vm_male_school_teacher_boy_chathead_dummy"
    }
}
