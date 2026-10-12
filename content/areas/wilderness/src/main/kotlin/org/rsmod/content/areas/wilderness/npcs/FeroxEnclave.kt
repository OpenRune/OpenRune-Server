package org.rsmod.content.areas.wilderness.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FeroxEnclave @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wildy_hub_chief") { startDialogue(it.npc) { ferox() } }
        onOpNpc1("npc.wildy_hub_guard") { startDialogue(it.npc) { sigismund() } }
        onOpNpc1("npc.clanwars_nun") { startDialogue(it.npc) { sisterScarophia() } }
        onOpNpc1("npc.wildy_hub_pub_owner") { startDialogue(it.npc) { camarst() } }
        for (acolyte in ACOLYTES) {
            onOpNpc1(acolyte) { startDialogue(it.npc) { zamorakianAcolyte() } }
        }
        onOpNpc1("npc.canoeing_marten") { startDialogue(it.npc) { marten() } }
        onOpNpc1("npc.br_host_main") { startDialogue(it.npc) { lisa() } }
        onOpNpc1("npc.br_host_tutorial") { startDialogue(it.npc) { lisa() } }
        onOpNpc1("npc.br_reward_shopkeeper") { startDialogue(it.npc) { justine() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ferox (never paid for the respawn point)
    // TODO(content): [system:respawn_point] take 5,000,000 coins and set the Ferox Enclave
    //  respawn point on "Okay, switch my respawn to Ferox Enclave.", and the "Ask about your
    //  respawn point." dialogue once paid
    private suspend fun Dialogue.ferox() {
        chatNpc(happy, "Welcome wanderer.")
        val option =
            choice3(
                "What is this place?",
                1,
                "What can you do for me?",
                2,
                "Nevermind.",
                3,
            )
        when (option) {
            1 -> feroxWhatIsThisPlace()
            2 -> feroxWhatCanYouDo()
            3 -> {
                chatPlayer(neutral, "Never mind, I'm just looking around.")
                chatNpc(neutral, "So be it.")
            }
        }
    }

    private suspend fun Dialogue.feroxWhatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(neutral, "You're surrounded by a relic of a long lost kingdom.")
        chatPlayer(quiz, "What happened here?")
        chatNpc(neutral, "Oh, I couldn't really tell you that.")
        chatNpc(
            neutral,
            "This place has sat in ruin for an age, slowly destroyed by the harsh climate of the " +
                "Wilderness.",
        )
        chatPlayer(confused, "If it's a ruin, what's the point in being here?")
        chatNpc(
            neutral,
            "I search for knowledge. I want to know what happened to this part of the world.",
        )
        chatNpc(
            happy,
            "Whilst looking for answers, I slowly turned this broken town into a safe haven for " +
                "travellers. Welcome to Ferox Enclave.",
        )
        chatPlayer(worried, "Quite a dangerous place for a town, don't you think?")
        chatNpc(
            neutral,
            "It is, but that's why it's needed! Look around, you can see how many others share " +
                "my opinion.",
        )
        chatNpc(
            neutral,
            "You'll find yourself quite safe inside my enclave - the protective barriers should " +
                "keep any troublemakers out.",
        )
        chatNpc(
            neutral,
            "I will warn you though, if you've had Teleblock cast upon you, I will not allow you " +
                "back into the Enclave. You'd be a danger to the others gathered here.",
        )
        when (choice2("What can you do for me?", 1, "I'll be heading off then.", 2)) {
            1 -> feroxWhatCanYouDo()
            2 -> {
                chatPlayer(happy, "Thanks for the information! I'll take a look around.")
                chatNpc(neutral, "Tread carefully my friend.")
            }
        }
    }

    private suspend fun Dialogue.feroxWhatCanYouDo() {
        chatPlayer(quiz, "What can you do for me?")
        chatNpc(
            neutral,
            "Well, if you'd like, I can make it so you respawn in our Enclave. It'll put you " +
                "right in the heart of all the chaos that surrounds us.",
        )
        if (choice2("Tell me more.", 1, "That doesn't interest me.", 2) == 2) {
            chatPlayer(neutral, "That doesn't interest me.")
            chatNpc(neutral, "Come find me if you change your mind.")
            return
        }
        chatPlayer(quiz, "Tell me more.")
        chatNpc(
            neutral,
            "This is how it'll work: When you die, you'll respawn right here in Ferox Enclave, " +
                "just outside the pub we've set up. You will lose your items as normal of course.",
        )
        chatNpc(
            neutral,
            "I won't be able to do anything if you die somewhere like Castle Wars or the Emir's " +
                "Arena; they have their own magic to keep you safe.",
        )
        chatNpc(
            neutral,
            "It'll cost you though! Transporting you here is no easy task. 5 million coins " +
                "should do it, that'd be a one time fee of course.",
        )
        if (access.invTotal(access.inv, COINS) < RESPAWN_FEE) {
            chatPlayer(sad, "I don't have that much money on me.")
            val person = if (access.isBodyTypeA()) "man" else "woman"
            chatNpc(
                neutral,
                "Understandable, you'd have to be a brave $person to be carrying that much gold " +
                    "out here.",
            )
            return
        }
        val option =
            choice2(
                "Okay, switch my respawn to Ferox Enclave.",
                1,
                "I'm not interested.",
                2,
            )
        when (option) {
            1 -> chatPlayer(happy, "Okay, switch my respawn to Ferox Enclave.")
            2 -> {
                chatPlayer(neutral, "I'm not interested.")
                chatNpc(neutral, "Can't say I blame you, this wouldn't be my choice either.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sigismund
    // TODO(content): [system:treasure_trails] Crack the Clue III clue step
    private suspend fun Dialogue.sigismund() {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "Can I help you?")
        chatPlayer(quiz, "I was just curious as to what you do around here.")
        chatNpc(neutral, "I protect other travellers from monsters, both animal and human kind.")
        chatPlayer(
            confused,
            "Human? Can't say I've seen many human shaped monsters in the Wilderness.",
        )
        chatNpc(
            neutral,
            "When you've been in the Wilderness as long as I have, you learn that not all " +
                "monsters show themselves on the outside.",
        )
        chatPlayer(confused, "Uhm... alright then, I'll leave you to your duties.")
        chatNpc(
            neutral,
            "Watch yourself out there, it can be hard to tell friend from foe in these lands.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sister_Scarophia
    private suspend fun Dialogue.sisterScarophia() {
        chatNpc(happy, "Welcome to my little chapel. Can I help you?")
        while (true) {
            val option =
                choice3(
                    "Why are you here?",
                    1,
                    "So whose sister are you?",
                    2,
                    "I'm fine, thanks.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Why are you here?")
                    chatNpc(
                        neutral,
                        "I built this chapel here so that fighters could come and pray before " +
                            "battle. A fighter who ignores their spiritual needs may as well " +
                            "have stabbed themselves in the foot.",
                    )
                    chatNpc(
                        sad,
                        "People keep claiming they're not interested in religion, but as soon as " +
                            "they're under attack they're yelling for 'Protect from Melee'. " +
                            "Sad, really.",
                    )
                    chatNpc(neutral, "So is there something I can do for you?")
                }
                2 -> {
                    chatPlayer(laugh, "So whose sister are you?")
                    chatNpc(
                        laugh,
                        "Ahahahahahaha, very funny! With jokes like that, you should become a " +
                            "banker.",
                    )
                    chatNpc(
                        neutral,
                        "But no I am a sister to the broken and down trodden, I heard talk of " +
                            "this enclave being built and knew that my skills could be put to " +
                            "good use.",
                    )
                    chatNpc(
                        happy,
                        "I've found a worthy cause here running my little chapel, so I'm happy " +
                            "enough. Now, can I help you with something?",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'm fine, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Camarst
    // TODO(content): [system:treasure_trails] Crack the Clue III clue step
    private suspend fun Dialogue.camarst() {
        chatNpc(happy, "Welcome my friend to the Old Nite! What can I do for you?")
        val option =
            choice3(
                "Got anything strong to drink?",
                1,
                "Your pub looks a bit rough.",
                2,
                "Just browsing...",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Got anything strong to drink?")
                chatNpc(
                    happy,
                    "Of course I do! I can offer you a big tasty beer, yours for only two coins.",
                )
                if (access.invTotal(access.inv, COINS) < BEER_PRICE) {
                    chatPlayer(sad, "Oh dear, I don't seem to have enough money.")
                    chatNpc(
                        neutral,
                        "No worries, there's a bank just around the corner if you want to grab " +
                            "some coin.",
                    )
                    return
                }
                chatPlayer(happy, "Sure, I'll take a beer.")
                if (player.invTakeFee(BEER_PRICE)) {
                    player.invAddOrDrop(objRepo, BEER)
                }
            }
            2 -> {
                chatPlayer(neutral, "Your pub looks a bit rough.")
                chatNpc(
                    happy,
                    "It's all part of the aesthetic! Adventurers who've just returned from the " +
                        "fight of their lives don't want a fancy la-de-da establishment like " +
                        "you'd find down south.",
                )
                chatNpc(happy, "They want somewhere to relax and have some hearty ale.")
                chatPlayer(
                    confused,
                    "And that's what you offer with your broken tables and glass covered floors?",
                )
                chatNpc(
                    laugh,
                    "Aye it is, because here at the Old Nite, your safety is our lowest priority!",
                )
            }
            3 -> chatPlayer(neutral, "Just browsing...")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zamorakian_Acolyte
    private suspend fun Dialogue.zamorakianAcolyte() {
        when (access.random.of(3)) {
            0 -> {
                chatPlayer(quiz, "What brings you all the way out into the Wilderness?")
                chatNpc(neutral, "The hunt.")
                chatPlayer(worried, "Oh... you partake in that?")
                chatNpc(angry, "Not the hunt for other beings! The hunt for knowledge.")
                chatPlayer(
                    confused,
                    "Knowledge? Can't say I've seen much of that around here. In case you " +
                        "didn't notice, it's all ruins.",
                )
                chatNpc(
                    neutral,
                    "Maybe for someone like you, but to us followers of Zamorak, there's relics " +
                        "of his mighty power everywhere.",
                )
                chatPlayer(
                    worried,
                    "Isn't it a little dangerous to be looking for knowledge out here?",
                )
                chatNpc(
                    neutral,
                    "That doesn't bother us. Our lord will protect us, we hunt in service of him.",
                )
                chatPlayer(neutral, "I'll leave you to it then.")
            }
            1 -> {
                chatPlayer(
                    neutral,
                    "You're a lot calmer than the other Zamorakian followers I've come across " +
                        "in the Wilderness.",
                )
                chatNpc(neutral, "We only search for answers.")
                chatPlayer(neutral, "Don't think you'll find much in these ruins.")
                chatNpc(
                    neutral,
                    "Be that as it may, it is our duty to honour our god and hunt for knowledge.",
                )
                chatPlayer(neutral, "I'll leave you to it then.")
            }
            else -> {
                chatNpc(neutral, "All power to the almighty Zamorak.")
                chatPlayer(neutral, "I take it you're with the others dressed in red.")
                chatNpc(neutral, "We are here in service of the only true god.")
                chatPlayer(neutral, "I'll leave you to it then.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Marten
    // TODO(content): [system:canoe_axe_storage] the Store-axe option
    private suspend fun Dialogue.marten() {
        chatPlayer(happy, "Hello there.")
        chatNpc(angry, "Aye? Who's that, what do you want?")
        when (choice2("Who are you?", 1, "Can you teach me about canoes?", 2)) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(neutral, "Marten.")
                chatPlayer(confused, "Oh, straight to the point.")
                chatNpc(angry, "I got betta' things to do than waste my time talkin' to you.")
                chatPlayer(angry, "You're rather rude aren't you?")
                chatNpc(angry, "Fine, wanna know more 'bout canoes?")
                if (choice2("Yes", 1, "No", 2) == 1) {
                    martenCanoes()
                } else {
                    chatPlayer(angry, "No thanks, not from you at least.")
                    chatNpc(angry, "Then get out of here and stop botherin' me.")
                }
            }
            2 -> martenCanoes()
        }
    }

    private suspend fun Dialogue.martenCanoes() {
        chatPlayer(quiz, "Could you teach me about canoes?")
        val level = player.woodcuttingLvl
        if (level < 12) {
            chatNpc(
                laugh,
                "Hahaha! Sorry mate, you'll not be able to make any canoes outta that tree " +
                    "there. You'll need to get a bit more skilled at the craft of Woodcuttin'.",
            )
            chatNpc(
                neutral,
                "But once you get a bit betta', you'll be able to make a canoe and ride the river.",
            )
            return
        }
        chatNpc(neutral, "It's not 'ard mate, you just gotta chop that tree o'er there down.")
        chatNpc(neutral, "Then cut it down into a canoe. So simple a gobbo could do it.")
        when {
            level < 27 -> {
                chatNpc(laugh, "But you got no chance of turning it into a beautiful canoe.")
                chatPlayer(quiz, "Why's that?")
                chatNpc(
                    laugh,
                    "I can tell by lookin' at ya! You ain't a woodcutter, most you'd be able to " +
                        "do is sail to the next stop on a log.",
                )
            }
            level < 42 -> {
                chatNpc(
                    neutral,
                    "Hmm, s'pose you could make a decent canoe with your skills. you'll be able " +
                        "to make a dugout, I reckons.",
                )
                chatPlayer(quiz, "How far will I be able to go in a dugout canoe?")
                chatNpc(neutral, "Err, probably about two stops before it sinks on ya.")
            }
            level < 57 -> {
                chatPlayer(quiz, "Could that take me back to Lumbridge?")
                chatNpc(neutral, "Pah, I don't think so. You'll be lucky to make it past Varrock.")
            }
            else -> {
                chatNpc(
                    happy,
                    "Well bless me axe, you're almost as good a woodchopper as meself! You'll " +
                        "have no issue craftin' the best of the best, the waka canoe.",
                )
                chatNpc(
                    happy,
                    "One of those beauties can take you up further in the Wilderness. Beautiful " +
                        "place that.",
                )
                chatPlayer(happy, "Great, thank you.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lisa (not on an official LMS world)
    // TODO(content): [system:last_man_standing] the game lobby, the first-talk explanation and
    //  the "I'd like to play." mode choice
    private suspend fun Dialogue.lisa() {
        chatNpc(neutral, "I'm afraid you can't take part in Last Man Standing on this world.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Justine
    // TODO(content): [system:last_man_standing] Justine's stuff for the Last Shopper Standing on
    //  "What have you got?" and the Trade option
    private suspend fun Dialogue.justine() {
        chatNpc(
            happy,
            "Hey, I'm Justine. I've got some rewards here for fighters who've done well in " +
                "Lisa's Last Man Standing game. Wanna see?",
        )
        when (choice2("What have you got?", 1, "No thanks.", 2)) {
            1 -> chatPlayer(quiz, "What have you got?")
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val BEER = "obj.beer"
        const val BEER_PRICE = 2
        const val RESPAWN_FEE = 5_000_000
        val ACOLYTES =
            listOf("npc.wildy_hub_acolyte1", "npc.wildy_hub_acolyte2", "npc.wildy_hub_acolyte3")
    }
}
