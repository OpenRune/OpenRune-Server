package org.rsmod.content.areas.city.ardougne.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ArdougneZoo @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zoo_keeper") { startDialogue(it.npc) { zooKeeper() } }
        onOpNpc1("npc.eadgar_zoo_keeper_aviary") { startDialogue(it.npc) { parrotyPete() } }
        onOpNpc1("npc.eaglepeak_zookeeper_charlie") { startDialogue(it.npc) { charlie() } }
        onOpNpc1("npc.peng_larry_zoo") { startDialogue(it.npc) { larry() } }
        onOpNpc1("npc.mm_monkey_minder") { startDialogue(it.npc) { monkeyMinder() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zoo_keeper
    // TODO(content): [system:treasure_trails] medium clue challenge scroll
    private suspend fun Dialogue.zooKeeper() {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, "Welcome to the Ardougne Zoo! How can I help you?")
        val quests =
            choice2(
                "Do you have any quests for me?",
                true,
                "Where did you get the animals from?",
                false,
            )
        if (!quests) {
            whereAnimalsFrom()
            return
        }
        chatPlayer(quiz, "Do you have any quests for me?")
        chatNpc(
            neutral,
            "Not at the moment. The explorers that come back from far away lands tell such " +
                "amazing tales. Make sure you keep eyes and ears open as you may find new places " +
                "to explore!",
        )
        chatPlayer(happy, "Ooh, I will!")
    }

    private suspend fun Dialogue.whereAnimalsFrom() {
        chatPlayer(quiz, "Where did you get the animals from?")
        chatNpc(
            happy,
            "We get them from all over the place! The most exotic creatures have been brought " +
                "back by explorers and sold to us.",
        )
        chatPlayer(shocked, "Where on Gielinor did you get that scary looking Cyclops?!")
        chatNpc(happy, "Yes he is scary looking!")
        chatNpc(
            neutral,
            "He's from a very far away land, we couldn't find out more as the explorer who " +
                "brought him back died shortly afterwards!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Parroty_Pete
    private suspend fun Dialogue.parrotyPete() {
        chatNpc(happy, "Good day, good day. Come to admire the new parrot aviary have we?")
        val option =
            choice3(
                "It's very nice.",
                1,
                "When did you add it?",
                2,
                "What do you feed them?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "It's very nice.")
                chatNpc(happy, "Isn't it just?")
            }
            2 -> {
                chatPlayer(quiz, "When did you add it?")
                chatNpc(
                    neutral,
                    "Just recently. It would have been sooner, but some wretch thought it would " +
                        "be amusing to replace their drinking water with vodka. The vet had to " +
                        "nurse them back to health for weeks!",
                )
            }
            3 -> {
                chatPlayer(quiz, "What do you feed them?")
                chatNpc(
                    happy,
                    "Well, fruit and grain mostly. I try to give them a balanced diet, but their " +
                        "favourite treat is pineapple chunks.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Charlie
    // TODO(content): [quest:eagles_peak] start offer and quest stages
    // TODO(content): [verify:in_game] Charlie's "Where did you get the animals from?" lines
    private suspend fun Dialogue.charlie() {
        if (QuestRequirements.hasCompleted(player, EAGLES_PEAK)) {
            chatPlayer(quiz, "Hi, how are you getting on with that ferret?")
            chatNpc(
                happy,
                "Well he seems to be just as vicious as the one we lost, but we're still very " +
                    "grateful for your help in acquiring him.",
            )
            return
        }
        chatPlayer(happy, "Hi!")
        chatNpc(
            neutral,
            "Yes, er, welcome to the zoo! I'm afraid I'm kind of busy right now; what was it you " +
                "wanted?",
        )
        val quest =
            choice2(
                "Ah, you sound like someone who needs a quest doing!",
                true,
                "Where did you get the animals from?",
                false,
            )
        if (!quest) {
            whereAnimalsFrom()
            return
        }
        chatPlayer(happy, "Ah, you sound like someone who needs a quest doing!")
        if (player.statBase("stat.hunter") < EAGLES_PEAK_HUNTER) {
            chatNpc(neutral, "Well... maybe. How much do you know about hunting?")
            chatPlayer(neutral, "Well I know a little bit...")
            chatNpc(
                sad,
                "No, I could do with some help, but I'm afraid I need somebody with a little " +
                    "more experience.",
            )
            return
        }
        chatNpc(neutral, "Actually, now that you come to mention it, you might be able to help me.")
        chatNpc(
            neutral,
            "A few weeks ago we had a delivery of a northern ferret. They're funny little " +
                "creatures. Turns out they're tricksy little blighters too.",
        )
        chatNpc(
            neutral,
            "Whilst we were unloading him from the cart he managed to escape his cage. What's " +
                "more he gave the driver a nasty nip on the way out too.",
        )
        chatNpc(
            sad,
            "This was obviously a bit of a blow, you can't make a new attraction without any " +
                "animals.",
        )
        chatNpc(
            neutral,
            "We got in contact with one of our associates, a huntsman called Nickolaus, who " +
                "agreed to capture us another.",
        )
        chatNpc(
            worried,
            "Funny thing is we haven't heard from him for some time now. I don't suppose you'd " +
                "mind having a look for him?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Larry (Ardougne Zoo)
    // TODO(content): [quest:cold_war] start offer and quest stages
    private suspend fun Dialogue.larry() {
        if (QuestRequirements.hasCompleted(player, COLD_WAR)) {
            larryAfterColdWar()
            return
        }
        chatPlayer(quiz, "Hello. Do you need any help? I'm looking for a quest.")
        if (player.statBase("stat.construction") < COLD_WAR_CONSTRUCTION) {
            chatNpc(
                neutral,
                "I'm looking for an experienced adventurer and you're not it. Come back when you " +
                    "are more capable.",
            )
            mesbox("You do not meet all of the requirements to start the Cold War quest.")
            return
        }
        chatNpc(shifty, "Are you working for them?")
        chatPlayer(confused, "Uh, I'm not sure. Who are they?")
        chatNpc(angry, "EXACTLY!")
        chatPlayer(confused, "Seriously, what are you talking about?")
        chatNpc(shifty, "I think you can be trusted. What do you know about...")
        chatNpc(shifty, "penguins?")
        chatPlayer(confused, "Uh...they live in the cold? That's about all.")
        chatNpc(
            neutral,
            "Would you like to learn more about them? I'm conducting a, uh, research project, " +
                "to, uh, learn about their social habits in their natural environment.",
        )
        chatPlayer(quiz, "What do you need my help for?")
        chatNpc(
            neutral,
            "Well, I need to build a shelter to hide from view, and it's always useful to have " +
                "an extra pair of eyes, you know, for, uh, observing...Penguins.",
        )
        chatNpc(quiz, "So, are you up for it?")
    }

    private suspend fun Dialogue.larryAfterColdWar() {
        val lostBook = !player.ownsClockworkBook()
        val myArm = QuestRequirements.hasCompleted(player, MAKING_FRIENDS_WITH_MY_ARM)
        val option =
            when {
                lostBook && myArm ->
                    choice3(
                        "What are we going to do about the penguins?",
                        1,
                        "I've lost the clockwork book, could I have another?",
                        2,
                        "Thanks for helping My Arm earlier.",
                        3,
                    )
                lostBook ->
                    choice2(
                        "What are we going to do about the penguins?",
                        1,
                        "I've lost the clockwork book, could I have another?",
                        2,
                    )
                myArm ->
                    choice2(
                        "What are we going to do about the penguins?",
                        1,
                        "Thanks for helping My Arm earlier.",
                        3,
                    )
                else -> 1
            }
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are we going to do about the penguins?")
                chatNpc(
                    neutral,
                    "I can't speak right now. I have too much to do. I will contact you when the " +
                        "time is right.",
                )
            }
            2 -> {
                chatPlayer(sad, "I've lost the clockwork book, could I have another?")
                chatNpc(
                    neutral,
                    "Well, lucky for you I did make another copy. But be more careful!",
                )
                player.invAddOrDrop(objRepo, CLOCKWORK_BOOK)
            }
            3 -> {
                chatPlayer(happy, "Thanks for helping My Arm earlier.")
                chatNpc(angry, "Never speak to me about that again.")
            }
        }
    }

    private fun Player.ownsClockworkBook(): Boolean {
        if (CLOCKWORK_BOOK in inv) {
            return true
        }
        val bank = invMap["inv.bank"]
        return bank != null && CLOCKWORK_BOOK in bank
    }

    // https://oldschool.runescape.wiki/w/Transcript:Monkey_minder
    // TODO(content): [system:met_flags] skip the introduction after the first conversation
    // TODO(content): [quest:monkey_madness_i] quest options and the monkey greegree dialogue
    private suspend fun Dialogue.monkeyMinder() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello, sir.")
        chatPlayer(quiz, "I haven't seen Monkey Minders here before...")
        chatNpc(neutral, "Yes, you wouldn't have. We have newly been posted here.")
        chatPlayer(quiz, "But why do we need Monkey Minders?")
        chatNpc(
            neutral,
            "It's entirely necessary: just take a look at the monkeys. They've been multiplying " +
                "out of control!",
        )
        chatNpc(
            worried,
            "Every time we turn our backs another one of them tries to break free. It's almost " +
                "as if they're plotting to escape!",
        )
        val multiplying =
            choice2(
                "Why have the monkeys been multiplying?",
                true,
                "What are you going to do about the monkeys?",
                false,
            )
        if (multiplying) {
            chatPlayer(quiz, "Why have the monkeys been multiplying?")
            chatNpc(
                confused,
                "We just don't know. Perhaps it's the weather. Perhaps it's something in the " +
                    "food. Perhaps it's just that time of year.",
            )
            chatNpc(neutral, "Your guess is as good as ours.")
            return
        }
        chatPlayer(quiz, "What are you going to do about the monkeys?")
        chatNpc(
            neutral,
            "We're going to post enough men here to be able to contain the monkeys in case they " +
                "manage to escape.",
        )
        chatPlayer(quiz, "Is that very likely?")
        chatNpc(neutral, "Stranger things have happened in Ardougne.")
    }

    private companion object {
        const val EAGLES_PEAK_HUNTER = 27
        const val COLD_WAR_CONSTRUCTION = 34

        const val EAGLES_PEAK = "quest_eaglespeak"
        const val COLD_WAR = "quest_coldwar"
        const val MAKING_FRIENDS_WITH_MY_ARM = "quest_makingfriendswithmyarm"
        const val CLOCKWORK_BOOK = "obj.peng_book"
    }
}
