package org.rsmod.content.areas.city.portphasmatys.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GreenGhost @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(INNKEEPER) { startDialogue(it.npc) { innkeeper() } }
        onOpNpc1(ROBIN) { startDialogue(it.npc) { robin() } }
        onOpNpc3(ROBIN) { startDialogue(it.npc) { robinClaimSlime() } }
        onOpNpc1(METARIALUS) { startDialogue(it.npc) { metarialus() } }
        onOpNpc1(BILL_TEACH) { startDialogue(it.npc) { billTeach() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_innkeeper
    // TODO(content): [verify:in_game] his reply without a ghostspeak amulet, and whether
    //  "Do you have any jobs I can do?" is offered before Ghosts Ahoy
    private suspend fun Dialogue.innkeeper() {
        if (!player.understandsGhosts()) {
            return wail()
        }
        chatPlayer(happy, "Hello there!")
        if (QuestRequirements.hasCompleted(player, GHOSTS_AHOY)) {
            chatNpc(
                happy,
                "Welcome, my friend! Be welcome in 'The Green Ghost' for as long as you live!",
            )
        } else {
            chatNpc(
                neutral,
                "Greetings, traveller. Welcome to 'The Green Ghost' Tavern. What can I do you for?",
            )
        }
        val option =
            choice5(
                "Can I buy a beer?",
                1,
                "Can I hear some gossip?",
                2,
                "What happened to the folk of this town?",
                3,
                "Do you have any jobs I can do?",
                4,
                "Nothing, thanks.",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can I buy a beer?")
                chatNpc(
                    sad,
                    "Sorry, but our pumps dried up over half a century ago. We of this village " +
                        "do not have much of a thirst these days.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Can I hear some gossip?")
                chatNpc(shifty, "I suppose, as long as you keep it to yourself...")
                chatNpc(
                    shifty,
                    "You see Gravingas out there in the marketplace? He speaks for the silent " +
                        "majority of Port Phasmatys, for those of us who would prefer to pass " +
                        "over into the next world.",
                )
            }
            3 -> {
                chatPlayer(quiz, "What happened to the folk of this town?")
                chatNpc(
                    neutral,
                    "That's a long story, my friend, but you look like you have the time...",
                )
                phasmatysHistory()
            }
            4 -> jobs()
            5 -> chatPlayer(neutral, "Nothing, thanks.")
        }
    }

    private suspend fun Dialogue.jobs() {
        chatPlayer(quiz, "Do you have any jobs I can do?")
        if (BEDSHEET in player.inv) {
            chatNpc(neutral, "Well, you could take that bedsheet through to Robin like I asked.")
            return
        }
        chatNpc(
            happy,
            "Yes, actually, I do. We have a very famous Master Bowman named Robin staying with " +
                "us at the moment. Could you take him some clean bed linen for me?",
        )
        val option =
            choice2("Yes, I'd be delighted.", 1, "No, I didn't mean a job like that.", 2)
        if (option == 2) {
            chatPlayer(neutral, "No, I didn't mean a job like that.")
            return
        }
        chatPlayer(happy, "Yes, I'd be delighted.")
        chatNpc(
            happy,
            "Oh, thank you. Be careful with that Robin, though - he's far too full of himself, " +
                "that one.",
        )
        player.invAddOrDrop(objRepo, BEDSHEET)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Robin
    // TODO(content): [quest:ghosts_ahoy] signing the oak longbow and the Runedraw game
    // TODO(content): [system:treasure_trails] master clue step reward
    private suspend fun Dialogue.robin() {
        if (QuestRequirements.hasCompleted(player, GHOSTS_AHOY)) {
            chatPlayer(neutral, "So, still here then?")
            chatNpc(quiz, "Do you want another game of Runedraw?")
            chatPlayer(angry, "No!")
            return
        }
        if (BEDSHEET in player.inv) {
            chatPlayer(happy, "I've brought you a clean bedsheet, Robin.")
            chatNpc(neutral, "Well it's about time. Run along now, I need some Robin time.")
            return
        }
        chatPlayer(happy, "It's nice to see another human face around here.")
        chatNpc(bored, "Leave me be, peasant - I am relaxing.")
        chatPlayer(angry, "Well, that's nice!")
        chatNpc(quiz, "Do you know who I am?")
        chatPlayer(neutral, "I'm sorry, I haven't had the privilege.")
        chatNpc(happy, "I, peasant, am Robin, Master Bowman. I am very famous you know.")
        chatPlayer(neutral, "Oh, Robin, Master Bowman, I see.")
        chatNpc(quiz, "So have you heard of me?")
        chatPlayer(neutral, "No.")
        chatNpc(
            neutral,
            "Would you do me a favour? I appear to have run out of bed linen. Can you run along " +
                "to the innkeeper and get me a clean bedsheet?",
        )
        val option =
            choice2(
                "Anything for a famous person such as yourself.",
                1,
                "Go and get it yourself.",
                2,
                title = "Select an option",
            )
        if (option == 1) {
            chatPlayer(happy, "Anything for a famous person such as yourself.")
            chatNpc(happy, "Now that sounds more like it. Run along now, get me my sheet.")
        } else {
            chatPlayer(angry, "Go and get it yourself.")
            chatNpc(angry, "Oh charming. You just can't get the staff these days.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Robin (Claim-slime)
    // TODO(content): [diary:morytania] bones to slime and bonemeal with the medium diary or above
    private suspend fun Dialogue.robinClaimSlime() {
        chatNpc(
            neutral,
            "Sorry, my orders are that I can only give slime to those who have completed the " +
                "Morytania medium, hard or elite diaries.",
        )
        chatNpc(neutral, "There's a diary master in Canifis, go and speak to her.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Metarialus
    private suspend fun Dialogue.metarialus() {
        chatPlayer(quiz, "Hello, I wonder if you could help me on this whole brewing thing...")
        if (!player.understandsGhosts()) {
            return wail(text = "Woooo wooo wooooo woooo.", cannotUnderstand = false)
        }
        chatNpc(happy, "I might be able to - what do you need to know?")
        while (true) {
            val option =
                choice5(
                    "How do I brew ales?",
                    1,
                    "How do I brew cider?",
                    2,
                    "What do I do once my ale has matured?",
                    3,
                    "Do you have any spare ale yeast?",
                    4,
                    "That's all I need to know, thanks.",
                    5,
                    title = "Select an option",
                )
            when (option) {
                1 -> brewAles()
                2 -> brewCider()
                3 -> {
                    chatPlayer(quiz, "What do I do once my ale has matured?")
                    chatNpc(
                        neutral,
                        "Well, once you've got a full vat of the good stuff, just turn the valve " +
                            "and your barrel will fill up with eight pints of whatever your " +
                            "chosen tipple is. Mind it's an empty barrel, though.",
                    )
                }
                4 -> aleYeast()
                5 -> {
                    chatPlayer(happy, "That's all I need to know, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.brewAles() {
        chatPlayer(quiz, "How do I brew ales?")
        chatNpc(
            neutral,
            "Well first off you need to fill the vat with water - two bucketfuls should do the " +
                "trick. Then you'll need to put in two handfuls of barley malt - that's roasted " +
                "barley by the way.",
        )
        chatNpc(
            neutral,
            "After that you'll be putting your main ingredient in - this will decide which ale " +
                "it is you're brewing. There should be some good guides around with recipes in.",
        )
        chatNpc(
            neutral,
            "Lastly you pour a pot full of ale yeast in, which'll start it off fermenting. Then " +
                "all you have to do is wait for the good stuff.",
        )
    }

    private suspend fun Dialogue.brewCider() {
        chatPlayer(quiz, "How do I brew cider?")
        chatNpc(
            neutral,
            "First you'll need some apples. You'll need to crush them using this cider-press - " +
                "four apples should make a full bucket of apple-mush. Take four buckets of mush " +
                "and fill up the vat.",
        )
        chatNpc(
            neutral,
            "After that you pour a pot full of ale yeast in, which'll start it off fermenting. " +
                "Then all you have to do is wait for the good stuff.",
        )
    }

    // TODO(content): [verify:in_game] the wiki is missing the dialogue for buying the yeast
    private suspend fun Dialogue.aleYeast() {
        chatPlayer(quiz, "Do you have any spare ale yeast?")
        chatNpc(
            neutral,
            "Well, as a matter of fact I do, although I wouldn't describe it as spare. This ale " +
                "yeast I've got is the best you can get - if you've got a pot I'll fill it for " +
                "you for 5 ectotokens. Coins are of no use to me.",
        )
        if (!buyYeast()) {
            chatPlayer(sad, "I don't have an empty pot, or enough ectotokens I'm afraid.")
        }
    }

    private fun Dialogue.buyYeast(): Boolean {
        val bought =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete {
                    from = inventory
                    obj = POT.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                delete {
                    from = inventory
                    obj = ECTOTOKEN.asRSCM(RSCMType.OBJ)
                    strictCount = YEAST_PRICE
                }
                insert {
                    into = inventory
                    obj = ALE_YEAST.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return bought.success
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bill_Teach
    private suspend fun Dialogue.billTeach() {
        if (QuestRequirements.hasCompleted(player, CABIN_FEVER)) {
            // The lift itself is offered by Bill Teach on board The Adventurous.
            chatNpc(
                neutral,
                "If yer after a lift go to the ship and I'll take ye where ye want to go.",
            )
            return
        }
        chatNpc(angry, "Mumblelandlubbermumblemumble...")
        chatPlayer(happy, "Hello!")
        chatNpc(angry, "What right have ye to be so happy, landlubber?")
        chatPlayer(happy, "Well it is quite a nice day.")
        chatNpc(neutral, "Aye, that it is.")
        chatNpc(sad, "Will ye forgive an old sailor his temper $lad? I'm in dire straits indeed.")
        chatPlayer(quiz, "What's the problem? I may be able to help.")
        chatNpc(
            sad,
            "I dunno, $lad, I need a pirate to help me with my problem, but none will sail with " +
                "me.",
        )
        chatNpc(
            quiz,
            "Well, ye look like ye might have the right stuff, tell me would ye like te be a " +
                "pirate?",
        )
        val option =
            choice2(
                "Yes, I've always wanted to be a pirate!",
                1,
                "No thanks, it sounds quite dangerous.",
                2,
            )
        if (option == 1) {
            // TODO(content): [quest:cabin_fever] quest start
            return
        }
        chatPlayer(worried, "No thanks, it sounds quite dangerous.")
        chatNpc(neutral, "Aye, that it be. Come back when yer liver ain't so lily-like, $lad.")
    }

    private val Dialogue.lad: String
        get() = if (access.isBodyTypeA()) "lad" else "lass"

    private companion object {
        const val INNKEEPER = "npc.ahoy_ghost_innkeeper"
        const val ROBIN = "npc.ahoy_robin"
        const val METARIALUS = "npc.metarialus"
        const val BILL_TEACH = "npc.fever_teach"
        const val BEDSHEET = "obj.ahoy_bedsheet"
        const val POT = "obj.pot_empty"
        const val ECTOTOKEN = "obj.ectotoken"
        const val ALE_YEAST = "obj.ale_yeast"
        const val YEAST_PRICE = 5
        const val GHOSTS_AHOY = "quest_ghostsahoy"
        const val CABIN_FEVER = "quest_cabinfever"
    }
}
