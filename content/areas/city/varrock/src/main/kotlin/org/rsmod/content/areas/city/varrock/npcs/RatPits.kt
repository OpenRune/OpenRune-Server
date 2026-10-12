package org.rsmod.content.areas.city.varrock.npcs

import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RatPits @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.vc_gambler_rich3") { startDialogue(it.npc) { barman() } }
        onOpNpc1("npc.vc_gambler_poor1") { startDialogue(it.npc) { leatherGamer() } }
        onOpNpc1("npc.vc_gambler_poor2") { startDialogue(it.npc) { shirtlessGamer(it.npc) } }
        onOpNpc1("npc.vc_gambler_rich1") { startDialogue(it.npc) { topHatGamer(it.npc) } }
        onOpNpc1("npc.vc_gambler_rich4") { startDialogue(it.npc) { featherGamer(it.npc) } }
        onOpNpc1("npc.vc_gambler_woman1") { startDialogue(it.npc) { greenGamer(it.npc) } }
        onOpNpc1("npc.vc_gambler_woman2") { startDialogue(it.npc) { greyGamer(it.npc) } }
        onOpNpc1("npc.vc_gambler_woman3") { startDialogue(it.npc) { pinkGamer(it.npc) } }
        onOpNpc1(GRIMESQUIT) { startDialogue(it.npc) { sistersGrime(GRIMESQUIT) } }
        onOpNpc1(PHINGSPET) { startDialogue(it.npc) { sistersGrime(PHINGSPET) } }
        onOpNpc1("npc.rat_boss_biologist_op1") { startDialogue(it.npc) { historianAldo() } }
        onOpNpc1("npc.rat_boss_biologist_op2") { startDialogue(it.npc) { historianAldo() } }
        onOpNpc3("npc.rat_boss_biologist_op2") { startDialogue(it.npc) { aldoTrade() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barman_(Ratpit_bar)
    private suspend fun Dialogue.barman() {
        chatNpc(happy, "Welcome traveller, can I get you a drink or some food?")
        val option =
            choice3(
                "I'd like a beer please.",
                1,
                "I'd like some food please.",
                2,
                "I have to go",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I'd like a beer please.")
                chatNpc(happy, "That'll be 2 gold coins.")
                barOrder(BEER, BEER_PRICE)
            }
            2 -> {
                chatPlayer(happy, "I'd like some food please.")
                chatNpc(happy, "I can make you a stew for 20 gold coins.")
                barOrder(STEW, STEW_PRICE)
            }
            3 -> chatPlayer(neutral, "I have to go.")
        }
    }

    private suspend fun Dialogue.barOrder(obj: String, price: Int) {
        if (choice2("Pay.", 1, "Don't pay.", 2) == 2) {
            chatPlayer(neutral, "Sorry, I changed my mind.")
            return
        }
        if (!player.invTakeFee(price)) {
            chatPlayer(sad, "Sorry, I don't have $price coins on me.")
            return
        }
        player.invAddOrDrop(objRepo, obj)
        chatNpc(happy, "Thanks for your custom.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gamer (man in leather body)
    private suspend fun Dialogue.leatherGamer() {
        chatPlayer(happy, "Good day.")
        chatNpc(angry, "What's so good about it?")
        chatPlayer(quiz, "What's eating you.")
        chatNpc(sad, "I'm ruined, all because of that stupid cat! It has ruined me.")
        chatNpc(sad, "Why did it have to get itself killed? Why?")
        chatPlayer(
            worried,
            "*Aside* I think I should leave him alone. He seems a little upset right now.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gamer (shirtless man)
    private suspend fun Dialogue.shirtlessGamer(npc: Npc) {
        chatPlayer(happy, "Hello!")
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(sad, "Trying to win my shirt back. I lost it last night in a silly bet.")
        chatPlayer(neutral, "Well I hope your luck improves. I doubt it can get any worse.")
        chatNpc(sad, "Well I've little left to lose, maybe some more hair and my trousers.")
        chatPlayer(worried, "I really think you should try hold on to them.")
        ratPitsExplained(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gamer (man with top hat)
    private suspend fun Dialogue.topHatGamer(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "What do you want?")
        chatPlayer(quiz, "I was just wondering what you were doing here?")
        chatNpc(neutral, "I ask myself the same question often.")
        chatNpc(
            angry,
            "What am I doing down here in these pits along with these filthy dregs of society?",
        )
        chatPlayer(quiz, "And do you ever come up with an answer?")
        chatNpc(neutral, "Always.")
        chatNpc(shifty, "I'm here taking their money, that's what.")
        ratPitsExplained(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gamer (man with feathered hat)
    private suspend fun Dialogue.featherGamer(npc: Npc) {
        chatPlayer(quiz, "Hello! What's going on here?")
        chatNpc(
            happy,
            "Just a bit of drinking, a bit of fighting and a few people trying to win big.",
        )
        chatPlayer(happy, "Nothing but good clean family fun.")
        ratPitsExplained(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gamer (woman in green)
    private suspend fun Dialogue.greenGamer(npc: Npc) {
        chatPlayer(happy, "Hello!")
        chatPlayer(quiz, "What are you doing?")
        chatNpc(
            neutral,
            "Nothing much. Just keeping myself occupied, placing a couple of wagers here and " +
                "there.",
        )
        chatPlayer(quiz, "Are you winning?")
        chatNpc(
            neutral,
            "Yes in the sense that I'm not as bored as I was and no in terms of the number of " +
                "coins in my purse.",
        )
        ratPitsExplained(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gamer (woman in grey)
    private suspend fun Dialogue.greyGamer(npc: Npc) {
        chatPlayer(happy, "Hello!")
        chatPlayer(quiz, "What are you up to?")
        chatNpc(neutral, "Trying to win back some money.")
        chatPlayer(quiz, "Is it going well?")
        chatNpc(
            sad,
            "In every bet there is a fool and a thief and I'm beginning to think that I'm not " +
                "much of a thief.",
        )
        ratPitsExplained(npc)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gamer (woman with pink beret)
    private suspend fun Dialogue.pinkGamer(npc: Npc) {
        chatPlayer(happy, "Hello!")
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(
            happy,
            "Having fun. It's such a blast I've already won 5 thousand gold coins today, my cat " +
                "just keeps winning.",
        )
        ratPitsExplained(npc)
    }

    // TODO(content): [system:rat_pits] betting on cat fights against other players
    private suspend fun Dialogue.ratPitsExplained(npc: Npc) {
        chatPlayer(quiz, "So how do these rat pits work?")
        val ratsToWin =
            when (npc.coords.mx to npc.coords.mz) {
                ARDOUGNE_PIT -> {
                    chatNpc(neutral, "Things work simply in the Ardougne ratpits.")
                    chatNpc(
                        neutral,
                        "If you have a kitten and at least 100 coins you can challenge someone " +
                            "else who also has a kitten and some spare change.",
                    )
                    chatPlayer(neutral, "That sounds simple enough so far.")
                    5
                }
                VARROCK_PIT -> {
                    chatNpc(neutral, "It's simple.")
                    chatNpc(
                        neutral,
                        "In Varrock if you have a grown cat and a couple of coins you can " +
                            "challenge someone else who also has a grown cat and some spare " +
                            "change.",
                    )
                    chatPlayer(neutral, "That sounds simple enough so far.")
                    7
                }
                PORT_SARIM_PIT -> {
                    chatNpc(
                        neutral,
                        "The Port Sarim pits are quite fussy, no lazy cats allowed in here.",
                    )
                    chatPlayer(confused, "Lazy cats?")
                    chatNpc(
                        neutral,
                        "You know if you neglect a wily cat, it gets fat and lazy? Well, " +
                            "they're not allowed to fight in here, only wily cats.",
                    )
                    chatPlayer(neutral, "That sounds simple enough so far.")
                    11
                }
                else -> error("Gamer outside a known rat pit: ${npc.coords}")
            }
        chatNpc(
            neutral,
            "You then agree on a wager. It's rumoured that some ratcatchers can talk to their " +
                "own cats and give them tactics to aid them.",
        )
        chatPlayer(quiz, "So then how do you win the challenge?")
        chatNpc(
            neutral,
            "I was just getting to that. Both the challengers' cats are placed inside the " +
                "arena, and whichever kills $ratsToWin rats first wins.",
        )
        ratPitsQuestions()
    }

    private suspend fun Dialogue.ratPitsQuestions() {
        while (true) {
            val option =
                choice5(
                    "That sounds vile and cruel.",
                    1,
                    "It sounds a little dangerous for my cat.",
                    2,
                    "Wow that sounds like fun.",
                    3,
                    "Is there a limit to how much you can bet?",
                    4,
                    "Can I challenge you and your cat?",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(angry, "That sounds vile and cruel.")
                    chatNpc(neutral, "It's a cruel world and the pits are no exception.")
                    chatNpc(neutral, "Each to their own, I say.")
                }
                2 -> {
                    chatPlayer(worried, "It sounds a little dangerous for my cat.")
                    chatNpc(neutral, "I agree. Cats have been known to be killed in fights.")
                    chatPlayer(
                        shocked,
                        "Oh how terrible. Is there nothing you can do to save your cat?",
                    )
                    chatNpc(
                        neutral,
                        "Of course there is, you can instruct it to be cautious, and it will " +
                            "leave once it feels as if it is in danger.",
                    )
                    chatPlayer(happy, "Oh that sounds quite wise.")
                    chatNpc(neutral, "That depends on how you value your cat.")
                    chatPlayer(confused, "Sorry, I don't follow.")
                    chatNpc(neutral, "Well if your cat dies or it runs away you lose.")
                    chatNpc(
                        neutral,
                        "So the longer it stays in the fight the better chance you have of " +
                            "winning.",
                    )
                }
                3 -> {
                    chatPlayer(happy, "Wow that sounds like fun!")
                    chatNpc(neutral, "I don't know about that.")
                    chatNpc(
                        neutral,
                        "One thing I know though is that it's a quick way to make and lose money.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "Is there a limit to how much you can bet?")
                    chatNpc(
                        neutral,
                        "Hmmm... let me think. I don't think that's ever arisen but I think it's " +
                            "20,000.",
                    )
                    chatPlayer(
                        shocked,
                        "Hmm, that sounds like a silly amount of money to bet on a cat fight.",
                    )
                    chatNpc(
                        neutral,
                        "You'd be surprised what some people would be willing to bet.",
                    )
                    chatNpc(neutral, "There is a minimum bet of 100 coins too.")
                }
                5 -> {
                    chatPlayer(quiz, "Can I challenge you and your cat?")
                    chatNpc(neutral, "No, not now, we don't really bet with outsiders.")
                    chatPlayer(
                        confused,
                        "But I'm not an outsider, am I not inside in the pits now?",
                    )
                    chatNpc(
                        neutral,
                        "I don't know, maybe once you've done something to win over my trust.",
                    )
                    chatPlayer(quiz, "Can it be bought?")
                    chatNpc(confused, "What?")
                    chatPlayer(neutral, "Your trust.")
                    chatNpc(
                        angry,
                        "No. Regular townsfolk don't like us, which leads us to distrust them " +
                            "and anyone else.",
                    )
                }
            }
            if (choice2("One more thing...", 1, "Thanks for your help.", 2) == 2) {
                chatPlayer(happy, "Thanks for your help.")
                return
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grimesquit (before Ratcatchers)
    // TODO(content): [quest:ratcatchers] after-quest dialogue and rat pole replacement
    private suspend fun Dialogue.sistersGrime(talkedTo: String) {
        sister(talkedTo, GRIMESQUIT, happy, "'Ello 'Ello!")
        sister(talkedTo, PHINGSPET, confused, "Who's this then?")
        sister(talkedTo, GRIMESQUIT, confused, "I don't know any more than you do!")
        sister(talkedTo, PHINGSPET, neutral, "Well ask them!")
        sister(talkedTo, GRIMESQUIT, neutral, "You ask them!")
        sister(talkedTo, PHINGSPET, angry, "No! I'm not talking to some stranger in a sewer.")
        sister(talkedTo, GRIMESQUIT, angry, "Go away, weirdo! We're busy!")
    }

    private suspend fun Dialogue.sister(
        talkedTo: String,
        speaker: String,
        mesanim: MesAnimType,
        text: String,
    ) {
        if (speaker == talkedTo) {
            chatNpc(mesanim, text)
        } else {
            val title = if (speaker == GRIMESQUIT) "Grimesquit" else "Phingspet"
            chatNpcSpecific(title, speaker, mesanim, text)
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Historian_Aldo
    // TODO(content): [boss:scurrius] Scurrius' spine used on Aldo
    private suspend fun Dialogue.historianAldo() {
        chatNpc(happy, "Hello there.")
        while (true) {
            val option =
                choice4(
                    "What's through those broken bars?",
                    1,
                    "Can you tell me about Scurrius' spine?",
                    2,
                    "Can you make a bone weapon for me please?",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> aldoBrokenBars()
                2 -> aldoSpine()
                3 -> {
                    chatPlayer(quiz, "Can you make a bone weapon for me please?")
                    chatNpc(
                        neutral,
                        "You'll need to bring a Scurrius spine and either a Rune mace, a Yew " +
                            "shortbow, or a Battlestaff. An additional 1000 chaos runes are " +
                            "needed if you want to create a Bone staff.",
                    )
                    chatNpc(neutral, "I'll also charge 50,000 coins to craft the weapon.")
                    if (hasBoneWeaponMaterials()) {
                        // TODO(content): [boss:scurrius] Aldo crafting a bone weapon
                        return
                    }
                    chatNpc(neutral, "Come back when you have all the necessary materials.")
                }
                4 -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.aldoBrokenBars() {
        chatPlayer(quiz, "What's through those broken bars?")
        chatNpc(
            happy,
            "Well, my intrepid friend, through those broken bars lies a most remarkable " +
                "discovery! I named it Scurrius, The Rat King!",
        )
        chatNpc(
            happy,
            "This is no ordinary rat. It is a magnificent creature of evolution, forged by the " +
                "unwitting contributions of adventurers who entered its cave.",
        )
        chatNpc(
            happy,
            "It absorbed their combat abilities, transforming itself into an obscene monster. " +
                "Nature's response to intrusion, you might say! How utterly fascinating!",
        )
        chatNpc(
            shifty,
            "Ahem, anyway you'll be absolutely fine, of course! Your victory awaits, I'm sure. " +
                "I shall observe your progress with interest... for science, you know.",
        )
        chatNpc(
            happy,
            "Oh, and if you happen to come across Scurrius' spine, please bring it to me.",
        )
    }

    private suspend fun Dialogue.aldoSpine() {
        chatPlayer(quiz, "Excuse me, can you tell me about Scurrius' spine?")
        chatNpc(happy, "Of course! Scurrius spines are invaluable for my research.")
        chatNpc(
            neutral,
            "I can use them to craft a potent bone weapon which will aid you when battling the " +
                "Rat King.",
        )
        chatNpc(
            neutral,
            "Although you can create the weapons yourself, provided you have the required " +
                "expertise and appropriate materials.",
        )
        chatNpc(
            neutral,
            "If you don't want a bone weapon, I can also trade you a lamp I found during my " +
                "research expedition - one for every Scurrius spine you bring to me.",
        )
    }

    private fun Dialogue.hasBoneWeaponMaterials(): Boolean {
        val inv = access.inv
        if (!access.invContains(inv, SPINE) || access.invTotal(inv, COINS) < BONE_WEAPON_FEE) {
            return false
        }
        return access.invContains(inv, "obj.rune_mace") ||
            access.invContains(inv, "obj.yew_shortbow") ||
            (access.invContains(inv, "obj.battlestaff") &&
                access.invTotal(inv, "obj.chaosrune") >= BONE_STAFF_CHAOS_RUNES)
    }

    // TODO(content): [boss:scurrius] trading Scurrius' spines for antique lamps or bone weapons
    private suspend fun Dialogue.aldoTrade() {
        if (access.invContains(access.inv, SPINE)) {
            return
        }
        chatNpc(
            neutral,
            "Ah, I'll need one of Scurrius' spines to do that. Perhaps you could fetch me one?",
        )
    }

    private companion object {
        const val GRIMESQUIT = "npc.vc_grimesquit"
        const val PHINGSPET = "npc.vc_phingspet"
        const val COINS = "obj.coins"
        const val BEER = "obj.beer"
        const val STEW = "obj.stew"
        const val SPINE = "obj.rat_boss_spine"
        const val BEER_PRICE = 2
        const val STEW_PRICE = 20
        const val BONE_WEAPON_FEE = 50_000
        const val BONE_STAFF_CHAOS_RUNES = 1000
        val ARDOUGNE_PIT = 41 to 150
        val VARROCK_PIT = 45 to 79
        val PORT_SARIM_PIT = 46 to 150
    }
}
