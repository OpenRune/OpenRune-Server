package org.rsmod.content.slayer.dialogue

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.slayer.slayerDuradelNotesReceived
import org.rsmod.content.slayer.dialogue.StandardSlayerDialogue.openMain

/** Task-master and NPC ids for slayer dialogue routing. */
object SlayerMasters {
    /** Quest gates assumed complete until real quest varbits exist. */
    private const val PORCINE_OF_INTEREST_COMPLETE = true
    private const val WHILE_GUTHIX_SLEEPS_ACTIVE = true
    private const val WHILE_GUTHIX_SLEEPS_COMPLETE = true
    private const val AYA_TURAEL_PASSED_AWAY = true
    private const val MONKEY_MADNESS_II_COMPLETE = true
    const val TASK_TURAEL = 1
    const val TASK_MAZCHNA = 2
    const val TASK_VANNAKA = 3
    const val TASK_DURADEL = 5
    const val TASK_NIEVE = 6
    const val TASK_KONAR = 8
    const val TASK_WILDERNESS = 9

    object Npc {
        val turael = "npc.slayer_master_1_tureal".asRSCM()
        val aya = "npc.slayer_master_1_aya".asRSCM()
        val spriaActive = "npc.slayer_master_9_active".asRSCM()
        val spria = "npc.porcine_spria".asRSCM()
        val mazchna = "npc.slayer_master_2_mazchna".asRSCM()
        val achtryn = "npc.slayer_master_2_achtryn_vis".asRSCM()
        val vannaka = "npc.slayer_master_3".asRSCM()
        val chaeldar = "npc.slayer_master_4".asRSCM()
        val kuradal = "npc.slayer_master_5_kuradal".asRSCM()
        val duradel = "npc.slayer_master_5_duradel".asRSCM()
        val nieve = "npc.slayer_master_nieve".asRSCM()
        val steve = "npc.slayer_master_steve".asRSCM()
        val krystilia = "npc.slayer_master_7".asRSCM()
        val konar = "npc.slayer_master_8".asRSCM()
    }

    fun extraMenuOptions(dialogue: Dialogue, npcId: Int): List<Pair<String, SlayerMenuOption>> {
        return buildList {
            when (npcId) {
                Npc.achtryn ->
                    add("So you're filling in for Mazchna?" to { dialogue.achtrynFiller() })
                Npc.aya -> {
                    add("So you're filling in for Turael?" to { dialogue.ayaFiller() })
                    if (AYA_TURAEL_PASSED_AWAY) {
                        add("About Turael..." to { dialogue.ayaAboutTurael() })
                    }
                }
                Npc.spria, Npc.spriaActive -> {
                    if (WHILE_GUTHIX_SLEEPS_COMPLETE) {
                        add("About Turael..." to { dialogue.spriaAboutTurael() })
                    }
                }
                Npc.kuradal -> {
                    if (WHILE_GUTHIX_SLEEPS_ACTIVE) {
                        add("So you're filling in for Duradel?" to { dialogue.kuradalFillingIn() })
                    }
                    if (WHILE_GUTHIX_SLEEPS_COMPLETE) {
                        add("About Duradel..." to { dialogue.aboutDuradel() })
                    }
                }
                Npc.duradel -> {
                    if (WHILE_GUTHIX_SLEEPS_COMPLETE) {
                        add("About Duradel..." to { dialogue.aboutDuradel() })
                    }
                }
                Npc.steve -> {
                    if (MONKEY_MADNESS_II_COMPLETE) {
                        add("I see you're the new Slayer Master here." to { dialogue.steveNewMaster() })
                    }
                }
            }
        }
    }

    fun isSpriaUnlocked(dialogue: Dialogue): Boolean = PORCINE_OF_INTEREST_COMPLETE

    suspend fun Dialogue.spriaStart() {
        if (!isSpriaUnlocked(this)) {
            chatNpc(
                neutral,
                "I've only recently moved in and I'm still getting unpacked. Could you come back later?",
            )
            return
        }
        val npcId =
            if (npc?.id == Npc.spria) Npc.spria else Npc.spriaActive
        openMain(npcId, extras = extraMenuOptions(this, npcId))
    }

    suspend fun Dialogue.steveStart() {
        if (!MONKEY_MADNESS_II_COMPLETE) {
            steveWyvernIntro()
            return
        }
        openMain(Npc.steve, extras = extraMenuOptions(this, Npc.steve))
    }

    private suspend fun Dialogue.steveWyvernIntro() {
        chatNpc(neutral, "Welcome to my private little wyvern area.")
        when (
            choice4(
                "Who are you?",
                1,
                "Why is this wyvern area private?",
                2,
                "So are you a Slayer Master?",
                3,
                "Thanks, goodbye.",
                4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I'm Steve. You may have heard of my big cousin Nieve - she's one of the Slayer Masters. She's the famous one in the family. But Nieve's not here - this area's mine",
                )
                steveWyvernIntro()
            }
            2 -> {
                chatPlayer(quiz, "Why is this wyvern area private?")
                chatNpc(
                    neutral,
                    "Because I say so! My cousin Nieve thinks she's so great because she's got a big cave where she can boss people around. So I carved some steps out of the ice, and made myself a training area here.",
                )
                steveWyvernIntro()
            }
            3 -> {
                chatPlayer(quiz, "So are you a Slayer Master?")
                chatNpc(
                    neutral,
                    "No, I'm not. My cousin Nieve is a Slayer Master. There's nothing special about her, you know. All she does is tell people to kill stuff. That's Slayer Masters for you.",
                )
                steveWyvernIntro()
            }
            4 -> chatPlayer(neutral, "Thanks, goodbye.")
        }
    }

    private suspend fun Dialogue.achtrynFiller() {
        chatPlayer(neutral, "So you're filling in for Mazchna?")
        chatNpc(
            neutral,
            "Indeed I am. It's been a while since he last asked me to step in, but it's good to get out and see the sights, to meet mortals and to generally socialise.",
        )
        chatPlayer(neutral, "So do you know Mazchna well?")
        chatNpc(
            neutral,
            "Oh, yes, he's such a mine of information. He hears what's going on at the pub, listens to the gossip of passing adventurers, and now he's off on a great adventure himself.",
        )
        chatNpc(
            neutral,
            "Though, of course, you know that already. Sorry to babble on. I'm sure you have plenty of other things to be doing.",
        )
    }

    private suspend fun Dialogue.ayaFiller() {
        chatPlayer(neutral, "So you're filling in for Turael?")
        chatNpc(
            neutral,
            "That I am. It's been quite a while since I last did this sort of thing, but my brother wants one last adventure, so here I am.",
        )
        chatPlayer(neutral, "So you've done this before?")
        chatNpc(
            neutral,
            "Oh yes. In fact, I was doing it before my brother was. Back in his adventuring days, I was the one here teaching people the basics.",
        )
        chatNpc(
            neutral,
            "After Spria was born, he retired from his life of thrill- seeking. That's when he took over from me.",
        )
        chatPlayer(neutral, "Interesting... I never knew that.")
        chatNpc(
            neutral,
            "He doesn't talk about his adventures much, but once you get him going, you won't be able to make him stop!",
        )
    }

    private suspend fun Dialogue.ayaAboutTurael() {
        chatPlayer(neutral, "About Turael...")
        chatNpc(
            neutral,
            "You need not share your condolences with me, adventurer. I've been prepared for this for a long time.",
        )
        chatPlayer(quiz, "You have?")
        chatNpc(
            neutral,
            "My brother was a thrill-seeker his whole life. Even after he retired, I always knew something would come and pull him back in.",
        )
        chatPlayer(sad, "And that something was me. I'm sorry...")
        chatNpc(
            neutral,
            "Don't be. He died doing what he loved, and he died a hero. He wouldn't have had it any other way.",
        )
    }

    private suspend fun Dialogue.spriaAboutTurael() {
        chatPlayer(neutral, "About Turael...")
        chatNpc(
            neutral,
            "It's nothing personal, but I'd prefer not to discuss what happened with my father.",
        )
        chatPlayer(neutral, "Of course. Sorry for bringing it up.")
    }

    private suspend fun Dialogue.kuradalFillingIn() {
        chatPlayer(neutral, "So you're filling in for Duradel?")
        chatNpc(
            neutral,
            "I am. The task he has been given is an important one, but this post must always be filled. Who better to fill it than his own daughter.",
        )
        chatPlayer(quiz, "Daughter? So being a Slayer Master runs in the family?")
        chatNpc(
            neutral,
            "Of course. One cannot simply grow up with someone like my father. My childhood consisted of a lot of training. My father has always been among the best, but he wanted me to be even better.",
        )
        chatPlayer(neutral, "Sounds like a lot of hard work.")
        chatNpc(neutral, "Yes, but we wouldn't have it any other way.")
    }

    private suspend fun Dialogue.aboutDuradel() {
        chatPlayer(neutral, "About Duradel...")
        chatNpc(quiz, "Are you here with news on Lucien?")
        chatPlayer(sad, "I'm afraid not.")
        chatNpc(
            neutral,
            "The moment you find him, you must come to me. I will ensure my father is avenged!",
        )
        chatPlayer(neutral, "He wouldn't want you to get yourself killed as well!")
        if (!access.player.slayerDuradelNotesReceived) {
            chatNpc(
                neutral,
                "I will not suffer the same fate as my father. I intend to be prepared. For a start, I have been researching my father's notes to find weaknesses in Lucien's demonic thralls.",
            )
            chatPlayer(neutral, "Those sound interesting. May I see them?")
            chatNpc(neutral, "Of course. If we expect to defeat Lucien, we must work together.")
            access.player.slayerDuradelNotesReceived = true
            mesbox("Kuradal hands you an old notebook.")
            return
        }
        if (access.inv.freeSpace() > 0) {
            chatNpc(
                neutral,
                "I will not suffer the same fate as my father. That fate is reserved for Lucien!",
            )
            chatPlayer(neutral, "I hope you're right...")
        } else {
            chatNpc(
                neutral,
                "I will not suffer the same fate as my father. I intend to be prepared. For a start, I have been researching my father's notes to find weaknesses in Lucien's demonic thralls.",
            )
            chatPlayer(
                neutral,
                "Speaking of those notes, I seem to have misplaced them. Do you happen to have another copy?",
            )
            chatNpc(neutral, "Luckily for you, I made many.")
            chatNpc(neutral, "Return to me when you have space to carry them.")
        }
    }

    private suspend fun Dialogue.steveNewMaster() {
        chatPlayer(neutral, "I see you're the new Slayer Master here.")
        chatNpc(
            neutral,
            "Yes, Nieve used to be the local Slayer Master - she was my cousin. We weren't particularly close, but I came here straight away to take over the job.",
        )
        chatNpc(
            neutral,
            "I'd hoped I'd receive a shield like Nieve's when I took over her job, but I've not been given one so far. I guess the perks of the job aren't what they used to be.",
        )
        chatPlayer(neutral, "How sad for you.")
    }
}
