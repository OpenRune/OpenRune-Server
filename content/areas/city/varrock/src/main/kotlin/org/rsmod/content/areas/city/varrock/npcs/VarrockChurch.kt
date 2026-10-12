package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VarrockChurch : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ELSIE) { startDialogue(it.npc) { elsie() } }
        onOpNpcU(ELSIE) {
            if (it.objType.internalName == TEA) {
                startDialogue(it.npc) { elsieSeesTea() }
            } else {
                startDialogue(it.npc) {
                    chatNpc(neutral, "No thanks, dearie, I don't want that just now.")
                }
            }
        }
        onOpNpc1("npc.father_lawrence") { startDialogue(it.npc) { fatherLawrence() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elsie
    private suspend fun Dialogue.elsie() {
        if (TEA in player.inv) {
            elsieSeesTea()
            return
        }
        chatNpc(happy, "Hello dearie! What can old Elsie do for you?")
        elsieOptions()
    }

    private suspend fun Dialogue.elsieSeesTea() {
        chatNpc(happy, "Ooh - that looks like a lovely cup of tea, dearie. Is it for me?")
        if (elsieAsksForTea()) {
            return
        }
        elsieOptions()
    }

    private suspend fun Dialogue.elsieOptions() {
        while (true) {
            val option =
                choice3(
                    "What are you making?",
                    1,
                    "Can you tell me a story?",
                    2,
                    "Can you tell me how to get rich?",
                    3,
                    title = SAY,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What are you making?")
                    chatNpc(
                        happy,
                        "I'm knitting a new stole for Father Lawrence downstairs. He could do " +
                            "with something to keep his neck warm, standing in that draughty " +
                            "old church all day.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Can you tell me a story?")
                    if (TEA in player.inv) {
                        if (elsieAsksForTea()) {
                            return
                        }
                    } else {
                        chatNpc(
                            happy,
                            "Maybe I could tell you a story if you'd fetch me a nice cup of tea.",
                        )
                        chatPlayer(neutral, "I'll think about it.")
                        return
                    }
                }
                3 -> {
                    chatPlayer(quiz, "Can you tell me how to get rich?")
                    chatNpc(
                        neutral,
                        "Well, dearie, I'm probably not the best person to ask about money, but " +
                            "I think the best thing would be for you to get a good trade. If " +
                            "you've got a trade you can earn your way, that's what my old " +
                            "father told me,",
                    )
                    chatNpc(
                        neutral,
                        "Saradomin rest his soul. I hear people try to get rich by fighting in " +
                            "the Wilderness north of here, but that's no way for honest folks to " +
                            "earn a living! So get yourself a good trade, and keep",
                    )
                    chatNpc(
                        neutral,
                        "working at it. There's always folks wanting to buy ore and food " +
                            "around here.",
                    )
                    chatPlayer(happy, "Thanks, old woman.")
                }
            }
        }
    }

    private suspend fun Dialogue.elsieAsksForTea(): Boolean {
        chatNpc(
            happy,
            "Well, maybe I could tell you a story if you'd give me that lovely cup of tea " +
                "you've got there.",
        )
        val give =
            choice2(
                "Yes, you can have it.",
                true,
                "No, keep your hands off my tea.",
                false,
                title = SAY,
            )
        if (!give) {
            chatPlayer(angry, "No, keep your hands off my tea.")
            chatNpc(sad, "Aww. Maybe another time. Anyway, what can old Elsie do for you?")
            return false
        }
        chatPlayer(happy, "Yes, you can have it.")
        // TODO(content): [verify:in_game] no empty cup comes back (the transcript doesn't mention one)
        if (access.invDel(player.inv, TEA).failure) {
            return true
        }
        elsieStory()
        return true
    }

    private suspend fun Dialogue.elsieStory() {
        chatNpc(
            happy,
            "Ahh, there's nothing like a nice cuppa tea. I know what, I'll tell you a story to " +
                "thank you for the lovely tea...",
        )
        chatNpc(
            happy,
            "A long time ago, when I was a little girl, there was a handsome young man living " +
                "in Varrock. I saw him here in the church quite often. Everyone said he was " +
                "going to become a priest, and we girls were so sad about that.",
        )
        chatNpc(
            happy,
            "But young Dissy - that was the young man's nickname - he was a wild young thing. " +
                "One night he gathered some lads together, and after the evening prayer- " +
                "meeting they all put on masks and sneaked down to the",
        )
        chatNpc(
            happy,
            "evil temple in the south of the city, the evil one. The next day, there was quite " +
                "a hubbub. The guards told us that someone had painted 'Saradomin pwns' on the " +
                "wall of the Zamorakian temple!",
        )
        chatNpc(
            happy,
            "Now, we'd always been taught to keep well away from that dreadful place, but it " +
                "really did us all good to see someone wasn't afraid of the scum who live at " +
                "that end of town. Old Father Packett was furious, but Dissy just",
        )
        chatNpc(laugh, "laughed it off.")
        chatNpc(
            sad,
            "Dissy left town after that, saying he wanted to see the world. It was such a " +
                "shame, he had the most handsome shoulders...",
        )
        chatNpc(
            neutral,
            "A young man came here looking for stories about Dissy - of course, that's not his " +
                "proper name, but his friends called him Dissy - and I told him that one. He " +
                "said Dissy had become a really famous man and there was",
        )
        chatNpc(
            sad,
            "going to be a book about him. Well, that's all good, but I do wish Dissy had just " +
                "come back to Varrock. I did miss him so much... well, until I met my Freddie " +
                "and we got married, but that's another story.",
        )
        chatPlayer(happy, "Thank you. I'll leave you to your knitting now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Father_Lawrence (Romeo & Juliet not started)
    // TODO(content): [quest:romeo_and_juliet] quest dialogue and the drunk lines after the potion
    // TODO(content): [system:wise_old_man_tasks] delivering the Wise Old Man's letter
    private suspend fun Dialogue.fatherLawrence() {
        chatNpc(happy, "Hello adventurer, do you seek a quest?")
        while (true) {
            val option =
                choice4(
                    "I am always looking for a quest.",
                    1,
                    "No, I prefer just to kill things.",
                    2,
                    "Can you recommend a good bar?",
                    3,
                    "Ok, thanks",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "I am always looking for a quest.")
                    chatNpc(
                        neutral,
                        "Well, I see poor Romeo wandering around the square. I think he may " +
                            "need help.",
                    )
                    chatNpc(
                        sad,
                        "I was helping him and Juliet to meet, but it became impossible.",
                    )
                    chatNpc(neutral, "I am sure he can use some help.")
                }
                2 -> {
                    chatPlayer(neutral, "No, I prefer just to kill things.")
                    chatNpc(
                        neutral,
                        "That's a fine career in these lands. There is more that needs killing " +
                            "every day.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Can you recommend a good bar?")
                    chatNpc(angry, "Drinking will be the death of you.")
                    chatNpc(neutral, "But the Blue Moon in the city is cheap enough.")
                    chatNpc(
                        neutral,
                        "And providing you buy one drink an hour they let you stay all night.",
                    )
                }
                4 -> {
                    chatPlayer(happy, "Ok, thanks")
                    return
                }
            }
        }
    }

    private companion object {
        const val ELSIE = "npc.varrock_granny_2"
        const val TEA = "obj.cup_of_tea"
        const val SAY = "What would you like to say?"
    }
}
