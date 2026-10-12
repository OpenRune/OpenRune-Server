package org.rsmod.content.areas.misc.guilds.legends

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LegendsGuild @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.radimus_erkle") { startDialogue(it.npc) { radimus() } }
        onOpNpc1("npc.legends_guild_guard1") { startDialogue(it.npc) { legendsGuard() } }
        onOpNpc1("npc.legends_guild_guard2") { startDialogue(it.npc) { legendsGuard() } }
        onOpNpc1("npc.fionella") { startDialogue(it.npc) { fionella(it.npc) } }
        onOpNpc3("npc.fionella") { player.openGeneralStore(it.npc) }
        onOpNpc1("npc.siegfried_erkle") { startDialogue(it.npc) { siegfried(it.npc) } }
        onOpNpc3("npc.siegfried_erkle") { player.openUsefulItems(it.npc) }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Legends Guild General Store.", "inv.generallegends")
    }

    private fun Player.openUsefulItems(npc: Npc) {
        shops.open(this, npc, "Legends Guild Shop of Useful Items.", "inv.legendsguildshop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Legends'_Quest (not started)
    // TODO(content): [quest:legends_quest] quest start and progress
    private suspend fun Dialogue.radimus() {
        if (QuestRequirements.hasCompleted(player, LEGENDS_QUEST)) {
            radimusAfterQuest()
            return
        }
        chatNpc(
            happy,
            "Good day to you, adventurer! No doubt you are keen to become a member of the " +
                "Legends' Guild?",
        )
        val option =
            choice3(
                "Yes actually, what's involved?",
                1,
                "Maybe some other time.",
                2,
                "Who are you?",
                3,
            )
        when (option) {
            1 -> radimusQuestOffer()
            2 -> radimusLater()
            3 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "My name is Radimus Erkle. I am the Grand Vizier of the Legends' Guild. Are " +
                        "you interested in becoming a member?",
                )
                val involved =
                    choice2("Yes actually, what's involved?", true, "Maybe some other time.", false)
                if (involved) radimusQuestOffer() else radimusLater()
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Radimus_Erkle
    // TODO(content): [system:item_reclaim] the antique lamps he hands out to players who haven't
    //  claimed them all
    private suspend fun Dialogue.radimusAfterQuest() {
        chatNpc(happy, "Hello there! How are you enjoying the Legends Guild?")
        val option =
            choice4(
                "It's legendary.",
                1,
                "It's okay.",
                2,
                "It's lame.",
                3,
                "Have you got anything for me?",
                4,
            )
        when (option) {
            1 -> chatPlayer(happy, "It's legendary.")
            2 -> chatPlayer(neutral, "It's okay.")
            3 -> chatPlayer(bored, "It's lame.")
            else -> {
                chatPlayer(quiz, "Have you got anything for me?")
                chatNpc(
                    neutral,
                    "One of our other members visited the Kharazi Jungle, following in your " +
                        "illustrious footsteps, and they found some weird daggers. Would you " +
                        "like one?",
                )
                mesbox("Radimus shows you a familiar-looking dagger.")
                // TODO(content): [system:dark_dagger_collection] the Dark Dagger collection
                //  interface and the daggers it hands out
                return
            }
        }
        chatNpc(
            neutral,
            "That's nice. Excuse me now, I have other matters to attend to. Please feel free to " +
                "explore the rest of the building.",
        )
    }

    private suspend fun Dialogue.radimusQuestOffer() {
        chatPlayer(quiz, "Yes actually, what's involved?")
        chatNpc(
            neutral,
            "Well, you'll need to complete a quest for us as proof that you've got what it takes " +
                "to be a Legend. How do you fancy mapping the Kharazi Jungle? It's the " +
                "unexplored southern part of Karamja Island.",
        )
        chatNpc(
            neutral,
            "We'd also like you to befriend a Kharazi tribesman, and bring back a token of the " +
                "tribe's friendship. The Legends' Guild has allies in all corners of the map, " +
                "and we could always use more.",
        )
        chatNpc(quiz, "What do you say? Are you interested in this quest?")
    }

    private suspend fun Dialogue.radimusLater() {
        chatPlayer(neutral, "Maybe some other time.")
        chatNpc(neutral, "As you wish...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Legends'_Quest (not started)
    // TODO(content): [quest:legends_quest] the eligibility check (quests and quest points) and
    //  the in-quest greeting
    // TODO(content): [verify:in_game] which options each menu shows; the transcript only lists
    //  them as "(Same as above)"
    private suspend fun Dialogue.legendsGuard() {
        if (QuestRequirements.hasCompleted(player, LEGENDS_QUEST)) {
            legendsGuardMember()
            return
        }
        mesbox("You approach a nearby guard...")
        chatNpc(neutral, "Yes, how can I help you?")
        val option =
            choice3(
                "What is this place?",
                1,
                "How do I get in here?",
                2,
                "Can I speak to someone in charge?",
                3,
            )
        when (option) {
            1 -> guardWhatIsThisPlace()
            2 -> guardHowDoIGetIn()
            3 -> guardSomeoneInCharge()
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Legends'_Guard
    // TODO(content): [verify:in_game] whether the repeated lines come from the other guard
    private suspend fun Dialogue.legendsGuardMember() {
        chatNpc(neutral, "! ! ! Attention ! ! !")
        chatNpc(neutral, "! ! ! Attention ! ! !")
        chatNpc(neutral, "Legends' Guild Member Approaching!")
        chatNpc(neutral, "Legends' Guild Member Approaching!")
        val welcome = if (access.isBodyTypeA()) "Welcome Sir" else "Welcome Madam"
        chatNpc(happy, "$welcome! I hope you enjoy your time in the Legends' Guild.")
    }

    private suspend fun Dialogue.guardWhatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(happy, "This is the Legends' Guild!")
        chatNpc(
            neutral,
            "Legendary citizens of Gielinor are invited on a quest in order to become members of " +
                "the guild.",
        )
        val option =
            choice2("Can I go on the quest?", 1, "What kind of quest is it?", 2)
        when (option) {
            1 -> guardCanIGoOnTheQuest()
            2 -> {
                chatPlayer(quiz, "What kind of quest is it?")
                chatNpc(
                    neutral,
                    "Well, to be honest, I'm not really sure. You'll need to talk to Grand " +
                        "Vizier Erkle to find that out.",
                )
                when (choice2("Can I go on the quest?", 1, "Thanks for your help.", 2)) {
                    1 -> guardCanIGoOnTheQuest()
                    2 -> {
                        chatPlayer(happy, "Thanks for your help.")
                        chatNpc(happy, "You're welcome...")
                        mesbox("The Guard marches off on patrol again.")
                    }
                }
            }
        }
    }

    private suspend fun Dialogue.guardHowDoIGetIn() {
        chatPlayer(quiz, "How do I get in here?")
        chatNpc(neutral, "Well, you'll need to be a legendary citizen of Gielinor.")
        chatNpc(
            neutral,
            "If you want to enter the Legends' Guild, you'll be invited to complete a quest. " +
                "Once you have completed that quest, you'll be a fully fledged member of the " +
                "Guild.",
        )
        val option =
            choice2("What is this place?", 1, "Can I speak to someone in charge?", 2)
        when (option) {
            1 -> guardWhatIsThisPlace()
            2 -> guardSomeoneInCharge()
        }
    }

    private suspend fun Dialogue.guardSomeoneInCharge() {
        chatPlayer(quiz, "Can I speak to someone in charge?")
        chatNpc(
            neutral,
            "Well, Radimus Erkle is the Grand Vizier of the Legends Guild. He's a very busy man. " +
                "And he'll only talk to those people eligible for the quest.",
        )
        when (choice2("Can I go on the quest?", 1, "It's okay, thanks.", 2)) {
            1 -> guardCanIGoOnTheQuest()
            2 -> {
                chatPlayer(neutral, "It's ok thanks.")
                chatNpc(neutral, "Very well!")
            }
        }
    }

    private suspend fun Dialogue.guardCanIGoOnTheQuest() {
        chatPlayer(quiz, "Can I go on the quest?")
        mesbox("The guard gets out a scroll of paper and starts looking through it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fionella
    private suspend fun Dialogue.fionella(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "Yes please. What are you selling?")
                chatNpc(happy, "Take a look.")
                player.openGeneralStore(npc)
            }
            2 -> chatPlayer(neutral, "No thanks")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Siegfried_Erkle
    private suspend fun Dialogue.siegfried(npc: Npc) {
        chatNpc(
            happy,
            "Hello there and welcome to the shop of useful items. Can I help you at all?",
        )
        while (true) {
            val option =
                choice3("What are you selling?", 1, "Do you sell Silverlight?", 2, "No thanks.", 3)
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What are you selling?")
                    chatNpc(happy, "Take a look.")
                    player.openUsefulItems(npc)
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Do you sell Silverlight?")
                    chatNpc(
                        neutral,
                        "Silverlight? No, Sir Prysin of Varrock explained that that was a unique " +
                            "sword and told us to stop selling it. If you want Silverlight, but " +
                            "don't have it, you should speak to him.",
                    )
                    chatNpc(quiz, "So would you like to see what else I have?")
                }
                3 -> {
                    chatPlayer(neutral, "No thanks.")
                    chatNpc(neutral, "Okay. Well, if you change your mind, do pop back.")
                    return
                }
            }
        }
    }

    private companion object {
        const val LEGENDS_QUEST = "quest_legends"
    }
}
