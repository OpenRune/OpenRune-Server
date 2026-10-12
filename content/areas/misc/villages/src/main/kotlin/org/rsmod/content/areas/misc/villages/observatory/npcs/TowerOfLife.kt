package org.rsmod.content.areas.misc.villages.observatory.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TowerOfLife @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tol_npc_efergy01_multi") { startDialogue(it.npc) { effigy() } }
        onOpNpc1("npc.tol_npc_barry01") { startDialogue(it.npc) { bonafido() } }
        onOpNpc1("npc.tol_npc_builder01") { startDialogue(it.npc) { blackEye() } }
        onOpNpc1("npc.tol_npc_builder02") { startDialogue(it.npc) { noFingers() } }
        onOpNpc1("npc.tol_npc_builder03") { startDialogue(it.npc) { gummy() } }
        onOpNpc1("npc.tol_npc_builder04") { startDialogue(it.npc) { theGuns() } }
        onOpNpc1("npc.tol_npc_mage01_multi") { startDialogue(it.npc) { transmute() } }
        onOpNpc1("npc.tol_npc_mage02_multi") { startDialogue(it.npc) { currency() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Effigy (starting the quest)
    // TODO(content): [quest:tower_of_life] quest start, quest stages and after-quest dialogue
    //  (the transcript has only the start)
    private suspend fun Dialogue.effigy() {
        chatPlayer(happy, "Hi. Interesting building you have here.")
        chatPlayer(quiz, "What's it for?")
        chatNpc(
            happy,
            "You like it? It's simply fabulous, isn't it? A real marvel of modern design made " +
                "just for my fellow alchemists and I!",
        )
        chatPlayer(
            neutral,
            "I see some builders over there. So I assume it's not yet finished.",
        )
        chatNpc(
            happy,
            "What a keen eye this chap has! But imagine this: a gargantuan cylinder of expert " +
                "design, incorporating an inventive inner spiral walkway to multiple planes of " +
                "wondrous inhabitance!",
        )
        chatPlayer(neutral, "So, it's a tower.")
        chatNpc(angry, "Oh, no, no, no, no.")
        chatNpc(happy, "It's going to have a use that will change this world!")
        chatNpc(sad, "Or at least it would if we could ever get it finished.")
        chatNpc(neutral, "See those builders?")
        chatPlayer(neutral, "They don't seem to be building much.")
        chatNpc(angry, "Exactly.")
        chatNpc(
            angry,
            "They've gone on strike for some petty reason about the tower being too weird. I " +
                "really can't understand those men.",
        )
        chatPlayer(sad, "Shame.")
        chatNpc(happy, "Hey! Maybe you could talk to them?")
        chatNpc(happy, "You look like a sturdy chap.")
        chatNpc(happy, "Maybe you could finish the work?")
        chatPlayer(confused, "Me?")
        chatNpc(happy, "Come on! This is the chance of a lifetime!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bonafido
    private suspend fun Dialogue.bonafido() {
        if (QuestRequirements.hasCompleted(player, TOWER_OF_LIFE)) {
            chatPlayer(quiz, "You still hangin' around the tower?")
            chatNpc(
                neutral,
                "Yeah, we need another job. But til then, we figured we may as well wait ta see " +
                    "what else would turns up.",
            )
            chatPlayer(neutral, "Fair enough.")
            return
        }
        chatPlayer(happy, "Hi there.")
        chatNpc(happy, "A'up, mate, how's it gowin'?")
        chatPlayer(happy, "Not bad, thank you. ")
        chatPlayer(quiz, "What are you building here?")
        chatNpc(neutral, "Nowt at moment. We're on strike!")
        chatPlayer(shocked, "You're on strike? Whatever for?")
        chatNpc(
            shifty,
            "Those strange alchemists are up ta somefin', best go ta one ov 'em.",
        )
        chatPlayer(neutral, "Oh, okay.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Black-eye%27
    private suspend fun Dialogue.blackEye() {
        if (QuestRequirements.hasCompleted(player, TOWER_OF_LIFE)) {
            blackEyeHelmet()
            return
        }
        chatPlayer(quiz, "What's going on?")
        chatNpc(happy, "Ah, we be building this 'ere tower. Lookin' good ain't it?")
        chatPlayer(
            quiz,
            "It does look pretty impressive, but what are you doing hanging around?",
        )
        chatNpc(happy, "Tea break, naturally.")
        chatPlayer(neutral, "Naturally. Not sure why I asked.")
        chatNpc(happy, "Fancy some?")
        chatPlayer(happy, "Tea? Sure.")
        chatNpc(happy, "You'll find a kettle in the box to the-")
        chatPlayer(worried, "Erm...yeah...no...sorry...gotta go.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Black-eye%27 (after Tower of Life)
    // TODO(content): [verify:in_game] order of the quiz options
    private suspend fun Dialogue.blackEyeHelmet() {
        chatPlayer(happy, "Say, that's a nice helmet.")
        if (HARD_HAT in player.inv) {
            chatNpc(happy, "Yes - just as nice as the one you're carrying I'd say.")
            return
        }
        val bank = player.invMap["inv.bank"]
        if (bank != null && HARD_HAT in bank) {
            chatNpc(happy, "Thank you - just as nice as the one in your bank!")
            return
        }
        chatNpc(happy, "Why, thanks.")
        chatPlayer(quiz, "I was just wondering if you might be able to help me out with one?")
        chatNpc(
            happy,
            "Always glad to help a budding builder. But first, prove your Construction knowledge " +
                "by answering some questions.",
        )
        chatNpc(quiz, "How many nails does it take to make a rocking chair?")
        when (choice3("One", 1, "Five", 2, "Three", 3)) {
            1 -> {
                chatPlayer(quiz, "One?")
                chatNpc(laugh, "One? How do you suppose you'd use a single nail?")
                return
            }
            2 -> {
                chatPlayer(quiz, "Five?")
                chatNpc(neutral, "Nope. Keep guessing!")
                return
            }
        }
        chatPlayer(quiz, "Three?")
        chatNpc(
            quiz,
            "Bingo! Okay, now what takes 3 planks, 3 cloths and 3 nails to make, and helps " +
                "remove light from a room?",
        )
        when (choice3("Torn curtains", 1, "A brown rug", 2, "Opulent curtains", 3)) {
            2 -> {
                chatPlayer(quiz, "A brown rug?")
                chatNpc(
                    laugh,
                    "Of course, because that's what everyone needs - a rug up at their window.",
                )
                return
            }
            3 -> {
                chatPlayer(quiz, "Opulent curtains?")
                chatNpc(
                    neutral,
                    "Those curtains are not going to be in any way opulent, matey.",
                )
                return
            }
        }
        chatPlayer(quiz, "Torn curtains?")
        chatNpc(
            quiz,
            "Nice one. Last question: I like fish and I want to put some in my garden, but I " +
                "need a special water feature. What materials would I require?",
        )
        val answer =
            choice3(
                "8 clay pieces and 3 limestone bricks",
                1,
                "10 clay pieces",
                2,
                "10 limestone bricks",
                3,
            )
        when (answer) {
            1 -> {
                chatPlayer(quiz, "8 clay pieces and 3 limestone bricks?")
                chatNpc(sad, "Oh, and you were so close!")
                return
            }
            3 -> {
                chatPlayer(quiz, "10 limestone bricks?")
                chatNpc(neutral, "Guess again.")
                return
            }
        }
        chatPlayer(quiz, "10 clay pieces?")
        chatNpc(
            happy,
            "That's it! You seem to know your stuff. I got a spare helmet from a builder that " +
                "died on... I mean, err, had to leave the job for greener pastures.",
        )
        chatPlayer(confused, "Greener pastures?")
        chatPlayer(neutral, "Fair enough.")
        player.invAddOrDrop(objRepo, HARD_HAT)
        chatPlayer(shocked, "Wait a minute! Is there blood on this helmet?")
        chatNpc(shifty, "Erm, no, that's just, erm, paint. Yes, paint! That's what it is!")
        chatPlayer(happy, "Oh, okay. Thanks. Bye.")
        chatNpc(laugh, "Ha, gullible fool.")
        chatPlayer(confused, "What?")
        chatNpc(neutral, "Nothing.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27No_fingers%27
    private suspend fun Dialogue.noFingers() {
        if (QuestRequirements.hasCompleted(player, TOWER_OF_LIFE)) {
            chatPlayer(happy, "Hi.")
            chatNpc(
                quiz,
                "Don't suppose you've seen any boots around here? Size nine, steel toed, brown " +
                    "leather...",
            )
            chatPlayer(shifty, "Nope, not at all. I'd best be going. Have a nice day. Bye!")
            return
        }
        chatPlayer(neutral, "You seem to like playing with danger, there.")
        chatNpc(laugh, "They don't call me 'No-fingers' for nothing!")
        chatPlayer(
            confused,
            "Well, correct me if I'm wrong, but you seem to have all your fingers.",
        )
        chatNpc(
            neutral,
            "It may seem that way mate, but I think you'll find I don't have any.",
        )
        chatPlayer(confused, "What?")
        chatNpc(neutral, "These not be fingers. These 'ere be wooden fingers.")
        chatPlayer(confused, "Wooden fingers?")
        chatNpc(neutral, "Yep.")
        chatPlayer(quiz, "That's not possible, how could you move them?")
        chatNpc(neutral, "Magic wood.")
        chatPlayer(worried, "Yes, well I think I'd better be going now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Gummy%27
    private suspend fun Dialogue.gummy() {
        if (QuestRequirements.hasCompleted(player, TOWER_OF_LIFE)) {
            chatPlayer(quiz, "You winning?")
            chatNpc(
                neutral,
                "It's a draw at the moment. First it was best of three, then it was best of five.",
            )
            chatPlayer(quiz, "And now?")
            chatNpc(happy, "Best of a hundred and one.")
            return
        }
        chatPlayer(quiz, "What are you up to?")
        chatNpc(neutral, "Rock, Paper, Scissors. Can't you tell?")
        chatPlayer(neutral, "It was more of a rhetorical question.")
        chatNpc(confused, "Rhetora-what?")
        chatPlayer(quiz, "Shouldn't you be working?")
        chatNpc(happy, "We are. It's team building.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27The_Guns%27
    // TODO(content): [system:npc_overhead] 'The Guns' overhead counting
    private suspend fun Dialogue.theGuns() {
        if (QuestRequirements.hasCompleted(player, TOWER_OF_LIFE)) {
            chatPlayer(quiz, "What do you do when you're not lifting logs above your head?")
            chatNpc(quiz, "In my free time?")
            chatPlayer(neutral, "Yes.")
            chatNpc(
                happy,
                "All sorts. I like...hummff...a good kebab and a cold beer down...urrghh...the " +
                    "pub. And for those quiet evenings...hurrr...",
            )
            chatNpc(happy, "A bit of needlepoint is always welcome.")
            chatPlayer(neutral, "I can honestly say you're a very original guy.")
            return
        }
        chatPlayer(shocked, "Wow, that looks pretty heavy.")
        chatNpc(worried, "Umpf! Yeah, mate...argghh...this is...umpf...hard work...")
        chatPlayer(shifty, "What would happen if I were to say...")
        chatPlayer(shifty, "135")
        chatPlayer(shifty, "25")
        chatPlayer(shifty, "234")
        chatPlayer(shifty, "2351...")
        chatNpc(angry, "Stop! Stop! Humpf...I'll beat you to...urghh...a pulp!")
        chatPlayer(laugh, "Okay, okay! Don't cry!")
        chatNpc(angry, "Why, I oughta...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Transmute%27_The_Alchemist
    private suspend fun Dialogue.transmute() {
        chatPlayer(quiz, "Hi there. What is this place?")
        chatNpc(happy, "Ah, this be the Tower of Life! Beautiful, isn't it?")
        chatPlayer(neutral, "I prefer ordinary houses.")
        chatNpc(angry, "Pah, you have no taste.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Currency%27_The_Alchemist
    private suspend fun Dialogue.currency() {
        chatPlayer(happy, "This is quite the tower.")
        chatNpc(happy, "Yeah, it cost a fair bit.")
        chatPlayer(quiz, "How much?")
        chatNpc(
            shifty,
            "Well, I had to 'alch' a fair few belongings of my fellow alchemists to afford it.",
        )
        chatPlayer(neutral, "I bet they love you for that.")
        chatNpc(happy, "Everyone loves someone that can create money.")
        chatPlayer(angry, "I don't.")
    }

    private companion object {
        const val TOWER_OF_LIFE = "quest_toweroflife"
        const val HARD_HAT = "obj.tol_player_construction_hardhat"
    }
}
