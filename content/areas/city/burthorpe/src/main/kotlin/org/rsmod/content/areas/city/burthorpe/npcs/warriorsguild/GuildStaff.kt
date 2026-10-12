package org.rsmod.content.areas.city.burthorpe.npcs.warriorsguild

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.basementAccess by intVarBit("varbit.warguild_basement_access")

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GuildStaff @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.warguild_gamfred") { startDialogue(it.npc) { gamfred() } }
        onOpNpc3("npc.warguild_gamfred") { startDialogue(it.npc) { claimTokens() } }
        onOpNpc4("npc.warguild_gamfred") {
            startDialogue(it.npc) {
                chatPlayer(quiz, SHIELD_REQUEST)
                giveShield()
            }
        }
        onOpNpc1("npc.warguild_shanomi") { startDialogue(it.npc) { shanomi() } }
        onOpNpc3("npc.warguild_shanomi") { startDialogue(it.npc) { shanomiTokens() } }
        onOpNpc1("npc.warguild_kamfreena") { startDialogue(it.npc) { kamfreena() } }
        onOpNpc3("npc.warguild_kamfreena") { startDialogue(it.npc) { claimTokens() } }
        onOpNpc1("npc.warguild_lorelai") { startDialogue(it.npc) { lorelai() } }
        onOpNpc3("npc.warguild_lorelai") { startDialogue(it.npc) { claimTokens() } }
        onOpNpc1("npc.warguild_cyclops_pet") { startDialogue(it.npc) { rory() } }
        for (ref in REFS) {
            onOpNpc1(ref) { startDialogue(it.npc) { ref() } }
            onOpNpc3(ref) { startDialogue(it.npc) { claimTokens() } }
        }
        onOpNpc1("npc.war_strength_drunken_warrior") { startDialogue(it.npc) { jimmy() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gamfred
    // TODO(content): [system:warriors_guild_catapult] the catapult game and its marker warning
    private suspend fun Dialogue.gamfred() {
        chatNpc(
            happy,
            "Ello there. I'm Gamfred, the engineer in this here guild. Have you seen my catapult?",
        )
        val option =
            choice5(
                "That's not a catapult, it's a large crossbow.",
                1,
                "Yes, beautiful piece of engineering.",
                2,
                "No, where is it?",
                3,
                CLAIM_TOKENS,
                4,
                "Bye!",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "That's not a catapult, it's a large crossbow.")
                chatNpc(
                    angry,
                    "WHAT!? I'll have you know that is the finest piece of dwarven engineering " +
                        "for miles around! How DARE you insult my work!",
                )
            }
            2 -> gamfredEngineering()
            3 -> {
                chatPlayer(quiz, "No, where is it?")
                val title = if (isMale()) "lad" else "lass"
                chatNpc(
                    angry,
                    "Are ye blind $title? Tis over there in the next room with me assistant " +
                        "working it!",
                )
            }
            4 -> claimTokens()
            5 -> {
                chatPlayer(happy, "Bye!")
                chatNpc(happy, "Come back soon! My catapult needs more test subjects.")
            }
        }
    }

    private suspend fun Dialogue.gamfredEngineering() {
        chatPlayer(happy, "Yes, beautiful piece of engineering.")
        chatNpc(
            happy,
            "Nice to meet someone who appreciates fine work, have you tried it out yet?",
        )
        if (choice2("Yes.", true, "No, how do I do that?", false)) {
            chatPlayer(happy, "Yes.")
            chatNpc(quiz, "What did you think?")
            val option =
                choice4(
                    "It was ok I guess.",
                    1,
                    "It was fun!",
                    2,
                    "I didn't like it.",
                    3,
                    SHIELD_REQUEST,
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(neutral, "It was ok I guess.")
                    chatNpc(neutral, "Well I guess not everyone will like it.")
                }
                2 -> {
                    chatPlayer(happy, "It was fun!")
                    chatNpc(
                        happy,
                        "Glad ta hear it. Try it again sometime. We have more tests to run.",
                    )
                }
                3 -> {
                    chatPlayer(sad, "I didn't like it.")
                    chatNpc(
                        neutral,
                        "Well I guess not everyone will like it. But give it another chance " +
                            "before you go.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, SHIELD_REQUEST)
                    giveShield()
                }
            }
            return
        }
        chatPlayer(quiz, "No, how do I do that?")
        chatNpc(
            neutral,
            "Well ye take the big defence shield in both hands and watch the catapult. My " +
                "assistant will fire different things at you and you need to defend against them.",
        )
        if (choice2(SHIELD_REQUEST, true, "Sounds boring.", false)) {
            chatPlayer(quiz, SHIELD_REQUEST)
            giveShield()
            return
        }
        chatPlayer(bored, "Sounds boring.")
        chatNpc(neutral, "Your loss...")
    }

    // TODO(content): [verify:in_game] whether a defensive shield in the bank counts as having one
    private suspend fun Dialogue.giveShield() {
        if (playerContainsObj(SHIELD)) {
            chatNpc(laugh, "Silly muffin, you have one already!")
            return
        }
        if (access.inv.isFull()) {
            chatNpc(neutral, "I'd be happy ta give you a shield but you've no space for it.")
            return
        }
        chatNpc(happy, "Of course!")
        player.invAddOrDrop(objRepo, SHIELD)
        mesbox("The dwarf hands you a large shield.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shanomi
    // TODO(content): [system:warriors_guild_animator] "My armour disappeared!" returns despawned
    //  armour, and the animator's wrong-item warning
    private suspend fun Dialogue.shanomi() {
        chatNpc(happy, "Greetings ${player.displayName}. Welcome you are in the test of combat.")
        while (true) {
            val option =
                choice4(
                    "What do I do here?",
                    1,
                    "Where do the machines come from?",
                    2,
                    CLAIM_TOKENS,
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> shanomiMachines()
                2 -> shanomiMagic()
                3 -> {
                    shanomiTokens()
                    return
                }
                4 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, "Health be with you travelling.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.shanomiMachines() {
        chatPlayer(quiz, "What do I do here?")
        chatNpc(
            neutral,
            "A spare suit of plate armour need you will. Full helm, plate leggings and platebody " +
                "yes? Placing it in the centre of the magical machines you will be doing. KA- " +
                "POOF! The armour, it attacks most furiously as if",
        )
        chatNpc(neutral, "alive! Kill it you must, yes.")
        chatPlayer(
            confused,
            "So I use a full set of plate armour on the centre plate of the machines and it will " +
                "animate it? Then I have to kill my own armour... how bizarre!",
        )
        chatNpc(
            neutral,
            "Yes. It is as you are saying. For this earn tokens you will. Also gain experience " +
                "in combat you will. Trained long and hard here have I.",
        )
        chatPlayer(quiz, "You're not from around here are you...?")
        chatNpc(neutral, "It is as you say.")
        chatPlayer(worried, "So will I lose my armour?")
        chatNpc(
            neutral,
            "Lose armour you will if damaged too much it becomes. Rare this is, but still " +
                "possible. If kill you the armour does, also lose armour you will.",
        )
        chatPlayer(
            neutral,
            "So, occasionally I might lose a bit because it's being bashed about and I'll " +
                "obviously lose it if I die... that it?",
        )
        chatNpc(neutral, "It is as you say.")
    }

    private suspend fun Dialogue.shanomiMagic() {
        chatPlayer(quiz, "Where do the machines come from?")
        chatNpc(neutral, "Make them I did, with magics.")
        chatPlayer(confused, "Magic, in the Warrior's Guild?")
        chatNpc(
            neutral,
            "A skilled warrior also am I. Harrallak mistakes does not make. Potential in my " +
                "invention he sees and opportunity grasps.",
        )
        chatPlayer(
            neutral,
            "I see, so you made the magical machines and Harrallak saw how they could be used in " +
                "the guild to train warrior's combat... interesting. Harrallak certainly is an " +
                "intelligent guy.",
        )
        chatNpc(neutral, "It is as you say.")
    }

    private suspend fun Dialogue.shanomiTokens() {
        claimTokens(
            refusal = "No tokens earned have you. In training activities participate you must.",
            reply = "Okay, I'll go see what I can find to do around here to earn some tokens.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kamfreena
    // TODO(content): [system:warriors_guild_cyclopes] defender showing, the cyclops room door and
    //  its token timer
    // TODO(content): [system:treasure_trails] elite clue reward
    private suspend fun Dialogue.kamfreena() {
        chatNpc(
            happy,
            "Why hello there! I'm Kamfreena. Like the look of my pets? I think they're eyeing " +
                "you up.",
        )
        chatPlayer(bored, "That was a really bad pun.")
        chatNpc(
            sad,
            "Sorry... I don't get to see the rest of the guild much, stuck up here. The Cyclopes " +
                "don't talk much you see.",
        )
        chatPlayer(quiz, "Shouldn't that be cyclopses?")
        chatNpc(
            neutral,
            "Nope! Cyclopes is the plural of cyclops. One cyclops, many cyclopes.",
        )
        chatPlayer(neutral, "Oh, right, thanks.")
        while (true) {
            val option =
                choice4(
                    "Where are they from?",
                    1,
                    "How did they get here?",
                    2,
                    "Why are they here?",
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Where are they from?")
                    chatNpc(neutral, "They're from the far east lands.")
                }
                2 -> {
                    chatPlayer(quiz, "How did they get here?")
                    chatNpc(
                        neutral,
                        "Ahh.. our guildmaster, Harrallak, went on an expedition there. He " +
                            "brought them back with him.",
                    )
                }
                3 -> kamfreenaDefenders()
                4 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, "See you back here soon I hope!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.kamfreenaDefenders() {
        chatPlayer(quiz, "Why are they here?")
        chatNpc(
            neutral,
            "For the warriors to train on of course! They also drop a rather nice blade.",
        )
        chatPlayer(quiz, "Oh? What would that be?")
        chatNpc(neutral, "Defenders.")
        chatPlayer(confused, "Err what are they?")
        chatNpc(
            neutral,
            "It's a blade you can defend with using your shield hand, like I have.",
        )
        chatPlayer(shocked, "Wow!")
        chatNpc(
            neutral,
            "For every 10 tokens you collect around the guild, you can spend one minute in with " +
                "my pets. As you get defenders you can show them to me to earn even better " +
                "ones... but remember if you lose them you'll have",
        )
        chatNpc(neutral, "to start at bronze again. I'd advise keeping a spare in your bank.")
        chatPlayer(happy, "Okay!")
        chatNpc(neutral, "Oh, by the way, you'll need to earn 100 tokens before I'll let you in!")
        chatPlayer(happy, "Right, I'd better go play some games then!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lorelai
    // TODO(content): [system:warriors_guild_cyclopes] the basement door, its token timer and
    //  using a rune or dragon defender on Lorelai
    private suspend fun Dialogue.lorelai() {
        chatNpc(happy, "Hello! I'm Lorelai. Welcome to the Warriors' Guild basement!")
        chatPlayer(quiz, "What are you doing down here?")
        chatNpc(
            neutral,
            "I'm training this group of special Cyclopes to be stronger and more powerful for " +
                "Kamfreena.",
        )
        while (true) {
            val option =
                choice4(
                    "What's special about these Cyclopes?",
                    1,
                    "How exactly are you training them?",
                    2,
                    "Can I fight them?",
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's special about these Cyclopes?")
                    chatNpc(
                        neutral,
                        "They have a chance of dropping a new, more powerful defender. Kamfreena " +
                            "doesn't want just anyone getting their hands on one so she moved " +
                            "them down here and tasked me with training them.",
                    )
                }
                2 -> {
                    chatNpc(
                        neutral,
                        "With a punishing routine of combat, fighting and the odd death!",
                    )
                    chatPlayer(
                        confused,
                        "So, you just let them fight each other and hope they get stronger?",
                    )
                    chatNpc(
                        neutral,
                        "Of course not, that would just be cruel! Kamfreena and I only allow the " +
                            "most worthy of adventurers to fight these cyclopes.",
                    )
                    lorelaiFight()
                    return
                }
                3 -> {
                    lorelaiFight()
                    return
                }
                4 -> {
                    lorelaiBye()
                    return
                }
            }
        }
    }

    // TODO(content): [verify:in_game] whether a rune defender in the bank or worn counts
    private suspend fun Dialogue.lorelaiFight() {
        chatPlayer(happy, "I am a mighty adventurer! Can I fight them?")
        if (player.basementAccess != 0) {
            chatNpc(happy, "Yes, you have already proved yourself to me, the door is unlocked.")
            return
        }
        chatNpc(
            neutral,
            "You'll need to display your skills as a Warrior before I can allow you to enter the " +
                "room.",
        )
        chatPlayer(quiz, "How can I do that?")
        chatNpc(
            neutral,
            "Bring me a Rune Defender and I shall unlock the door for you, this shows me you " +
                "have dedicated enough time to fighting the Cyclops upstairs.",
        )
        if (!choice2("I have a Rune Defender here.", true, "Bye!", false)) {
            lorelaiBye()
            return
        }
        chatPlayer(happy, "I have a Rune Defender here, please let me in!")
        if (!playerContainsObj(RUNE_DEFENDER)) {
            chatNpc(
                laugh,
                "And the Wise Old Man is my Dad... come back when you have the Rune Defender!",
            )
            return
        }
        player.basementAccess = 1
        chatNpc(
            happy,
            "Very well, I've unlocked the door but you'll still need 100 tokens from upstairs " +
                "before you can enter as well as 10 for every minute you want to stay inside. " +
                "Have fun in there!",
        )
    }

    private suspend fun Dialogue.lorelaiBye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "See you around.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lorelai
    private suspend fun Dialogue.rory() {
        chatNpcSpecific(
            "Lorelai",
            "npc.warguild_lorelai",
            happy,
            "Ahh I see you've met Rory. As a young Cyclops he's not ready for combat yet so he " +
                "keeps me company.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ref
    // TODO(content): [system:warriors_guild_shot_put] shot put throws and referee calls
    private suspend fun Dialogue.ref() {
        val title = if (isMale()) "Sir" else "Madam"
        chatNpc(neutral, "Greetings $title.")
        val option =
            choice4(
                "Tell me about the Shot Put area.",
                1,
                CLAIM_TOKENS,
                2,
                "Do you have any tips for me?",
                3,
                "Bye!",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Tell me about the Shot Put area.")
                chatNpc(neutral, "Of course $title. There are two different weights of shot...")
                chatPlayer(confused, "Shot?")
                chatNpc(
                    neutral,
                    "Yes $title. Shot. The iron spheres that are propelled by the chemical " +
                        "energy stored in your body.",
                )
                chatPlayer(confused, "The... what?")
                chatNpc(
                    neutral,
                    "Your strength $title. The stronger you are, the further you can throw the " +
                        "shot, but there are other factors of course, like your technique.",
                )
                chatPlayer(quiz, "What's that then?")
                chatNpc(neutral, "The style of the shot $title.")
                chatPlayer(confused, "Iron has style??")
                mesbox("The Referee sighs, rolls his eyes and continues....")
                chatNpc(
                    bored,
                    "$title, the style in which you throw the shot, not the style of the iron.",
                )
                chatPlayer(
                    happy,
                    "Oh! You mean the spinny round thing or the chuck it straight?",
                )
                chatNpc(
                    neutral,
                    "Crudely put $title, but yes. Some are more difficult than others. " +
                        "Experiment and see which you prefer.",
                )
                chatPlayer(happy, "Thanks for the help!")
                chatNpc(neutral, "You are welcome $title.")
            }
            2 -> claimTokens()
            3 -> {
                chatPlayer(quiz, "Do you have any tips for me?")
                chatNpc(quiz, "Tips $title?")
                chatPlayer(neutral, "Yes, like how can I do better than everyone else.")
                chatNpc(
                    neutral,
                    "$title may find that a fine powder applied to the hands may give one an " +
                        "advantage when putting the shot.",
                )
                chatPlayer(
                    quiz,
                    "You mean if I grind something up and put dust on my hands I'll chuck the " +
                        "ball further?",
                )
                chatNpc(neutral, "Yes $title.")
                chatPlayer(happy, "Thanks!")
                chatNpc(neutral, "You are welcome $title.")
            }
            4 -> {
                chatPlayer(happy, "Bye!")
                chatNpc(neutral, "Good luck $title.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jimmy_(Warriors%27_Guild)
    // TODO(content): [system:warriors_guild_kegs] keg balancing and the ledger entry for its tokens
    private suspend fun Dialogue.jimmy() {
        chatNpc(drunk, "'Ello there.")
        while (true) {
            val option =
                choice4(
                    "Tell me about this room.",
                    1,
                    "Tell me how to balance kegs.",
                    2,
                    CLAIM_TOKENS,
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> jimmyRoom()
                2 -> jimmyKegs()
                3 -> {
                    chatPlayer(quiz, CLAIM_TOKENS)
                    chatNpc(drunk, "Wha'? You ain't earnt any mate!")
                    return
                }
                4 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(drunk, "Shure you wouldn't like an ickle drinkie fore yer go?")
                    chatPlayer(
                        neutral,
                        "No thanks, got things to do, people to see, tokens to earn...",
                    )
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.jimmyRoom() {
        chatPlayer(quiz, "Tell me about this room?")
        chatNpc(drunk, "Well... s'like thish...")
        chatNpc(drunk, "Thish here'sh a shtore room right?")
        chatPlayer(confused, "A store room you mean?")
        chatNpc(
            drunk,
            "That'sh what I shaid! *HIC* A shtore room.... Now technic'ly shpeaking, I should be " +
                "outshide guarding it...",
        )
        chatPlayer(quiz, "But you just nipped in to have a quick drink?")
        chatNpc(drunk, "Yep... and to practish.")
        chatPlayer(quiz, "Practish? I mean.. practise what?")
        chatNpc(drunk, "Keg balancin. I'm the besht.")
    }

    private suspend fun Dialogue.jimmyKegs() {
        chatPlayer(quiz, "Tell me how to balance kegs?")
        chatNpc(
            drunk,
            "Yer very very shtrange. But.... you pick the keg up, and balance it on yer head, " +
                "then you pick another keg up and put that on top. S'really very eashy.",
        )
        chatPlayer(confused, "Eashy?")
        chatNpc(drunk, "Yesh. Eashy.")
        chatNpc(drunk, "But you couldn't ever balansh ash many ash meee!")
        chatPlayer(happy, "That sounds like a challenge, I'll show you!")
    }

    private companion object {
        const val SHIELD = "obj.warguild_shield"
        const val RUNE_DEFENDER = "obj.rune_parryingdagger"
        const val SHIELD_REQUEST = "May I have a shield please?"
        val REFS = listOf("npc.strength_referree1", "npc.strength_referree2")
    }
}
