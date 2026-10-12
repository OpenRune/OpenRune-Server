package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TunnelOfChaos : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.surok_monk1") { startDialogue(it.npc) { mishkalunDorn() } }
        onOpNpc1("npc.surok_monk2") { startDialogue(it.npc) { dakhthoulanAegis() } }
        onOpNpc1("npc.surok_monk3") { startDialogue(it.npc) { silasDahcsnu() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mishkal%27un_Dorn
    private suspend fun Dialogue.mishkalunDorn() {
        val name = player.displayName
        chatNpc(neutral, "You are excused. And you are welcome.")
        chatPlayer(confused, "Excuse me...er...thanks.")
        chatNpc(neutral, "We are the Order of the Dagon'hai.")
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "Through my magic, I can see a short way into the future.")
        chatPlayer(confused, "How do you seem to know what I'm going to say? ...Er...oh.")
        chatNpc(neutral, "These are the Tunnels of Chaos.")
        chatPlayer(happy, "What is...uh...aha! I'm not going to ask that. So you got it wrong!")
        chatNpc(neutral, "Indeed. You are very clever.")
        chatPlayer(happy, "So I won!")
        chatNpc(neutral, "Yes.")
        chatPlayer(quiz, "So, what is this place?")
        chatPlayer(angry, "I mean...Argh! How do you do that?")
        chatNpc(
            neutral,
            "I can tell that your mind is not suited to the paradoxicalities of precognition.",
        )
        chatPlayer(confused, "My what does what now?")
        chatNpc(neutral, "You get confused very easily.")
        chatPlayer(neutral, "I knew that.")
        chatNpc(
            neutral,
            "Of course you did. Speak to one of my order here. They will be able to explain in a " +
                "manner more suited to your understanding.",
        )
        chatNpc(neutral, "You are welcome, $name. There is a bed around here if you wish.")
        chatPlayer(
            confused,
            "Thanks. My name's $name, by the w...uh...okay, I think I need to lie down.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dakh%27thoulan_Aegis
    private suspend fun Dialogue.dakhthoulanAegis() {
        chatPlayer(happy, "Hi there.")
        chatNpc(happy, "Hello, young one. What brings you to our humble dwelling?")
        chatPlayer(quiz, "I was wondering what this place was?")
        chatNpc(
            neutral,
            "These are the Tunnels of Chaos. They radiate with the energy of chaos magic. At the " +
                "far end of the tunnel, you will find a portal to the Chaos Altar itself where " +
                "chaos runes are crafted!",
        )
        var viewed = false
        while (true) {
            val option =
                if (viewed) {
                    choice5(
                        "Why is the tunnel so short?",
                        1,
                        "Why are there only three of you?",
                        2,
                        "Is the Wilderness above us?",
                        3,
                        "You seem quite nice for a Zamorakian.",
                        4,
                        "Thanks for your time.",
                        5,
                    )
                } else {
                    choice4(
                        "Why is the tunnel so short?",
                        1,
                        "Why are there only three of you?",
                        2,
                        "Is the Wilderness above us?",
                        3,
                        "Thanks for your time.",
                        5,
                    )
                }
            viewed = true
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Why is the tunnel so short?")
                    chatNpc(
                        neutral,
                        "The nature of chaos magic itself is mysterious and hard to understand.",
                    )
                    chatPlayer(quiz, "What do you mean?")
                    chatNpc(
                        neutral,
                        "The tunnel is here, and yet it is not here. We also are here, where we " +
                            "should be, yet we are somewhere else. And so, the tunnel goes from " +
                            "one place to another, yet touches neither.",
                    )
                    chatPlayer(confused, "I really don't understand.")
                    chatNpc(
                        neutral,
                        "I told you you wouldn't. The best way to think of it is that the tunnel " +
                            "is like a gateway that allows you to travel from one place in the " +
                            "world to another in a more safe and convenient way.",
                    )
                    chatPlayer(quiz, "Oh, I think I see. So it is like a magical alternate route?")
                    chatNpc(
                        happy,
                        "Exactly! Well done! And at the end, there is a portal which will " +
                            "transport you to the Chaos Altar.",
                    )
                    chatPlayer(neutral, "Oh, ok.")
                }
                2 -> {
                    chatPlayer(quiz, "Why are there only three of you?")
                    chatNpc(
                        neutral,
                        "Our order is a secret one and those of us that remain and survive " +
                            "through the ages choose to hide ourselves away.",
                    )
                    chatPlayer(quiz, "Where are the rest of you now?")
                    chatNpc(
                        neutral,
                        "We have built or found many tunnels such as these. This is not the only " +
                            "one. Rest assured, there are many more of us around.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Is the Wilderness above us?")
                    chatNpc(
                        neutral,
                        "Yes, I can understand you would expect it to be since the tunnel leads " +
                            "to the Chaos Altar. And yet, the tunnel does not enter the " +
                            "Wilderness, no.",
                    )
                    chatNpc(
                        neutral,
                        "Rest assured, you are safe here and should not come to harm.",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "You seem quite nice for a Zamorakian.")
                    chatNpc(neutral, "History is written by the strong and the influential.")
                    chatPlayer(quiz, "What does that mean?")
                    chatNpc(
                        neutral,
                        "It means we cannot always believe what we are told to believe. You " +
                            "would do well to remember that.",
                    )
                    chatPlayer(happy, "I will! Thank you!")
                }
                else -> {
                    chatPlayer(neutral, "Thanks for your time.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sil%27as_Dahcsnu
    // TODO(content): [system:met_flags] "Oh, it's you again. What are you doing here?" after the
    // first talk
    private suspend fun Dialogue.silasDahcsnu() {
        chatPlayer(happy, "Hello there.")
        chatNpc(angry, "Can't you see that I'm busy here?")
        chatPlayer(neutral, "Oh. Sorry, you don't look very busy.")
        chatNpc(angry, "Don't look busy? I've got a lot of important work to do here.")
        chatPlayer(quiz, "Really? What do you do?")
        chatNpc(angry, "That doesn't concern you. What are you doing here anyway?")
        val option =
            choice5(
                "I'm not actually sure.",
                1,
                "I'm on an important quest!",
                2,
                "None of your business!",
                3,
                "I am looking for something.",
                4,
                "I'm the leader of the Dagon'hai. Bow before me!",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(confused, "I'm not actually sure.")
                chatNpc(angry, "Hmph! Well I can tell you what you ARE doing.")
                chatPlayer(quiz, "What's that?")
                chatNpc(
                    angry,
                    "Wasting my time! Now go away! If you must speak to someone, speak to one of " +
                        "the other monks around here!",
                )
            }
            2 -> {
                chatPlayer(happy, "I'm on an important quest!")
                chatNpc(bored, "Really...")
                chatPlayer(happy, "Yes! Do you want to know what my quest is?")
                chatNpc(
                    bored,
                    "Let me see...you helped out a trader who in turn asked you to deliver " +
                        "something for him, and then you had to journey to some palace to see " +
                        "someone and they said thanks by sending you somewhere else.",
                )
                chatPlayer(confused, "Uhh...")
                chatNpc(angry, "So, now you've ended up down here, bothering me?")
                chatPlayer(neutral, "Uh, yes, that's about right.")
                chatNpc(bored, "Lucky me...")
                chatPlayer(sad, "Sorry about that. I'll leave you alone, then.")
            }
            3 -> {
                chatPlayer(angry, "None of your business!")
                chatNpc(
                    angry,
                    "Of course it isn't, but you were the one who came here bothering me! So, " +
                        "unless you want something, be off with you!",
                )
            }
            4 -> {
                chatPlayer(neutral, "I am looking for something.")
                chatNpc(quiz, "And what are you looking for exactly?")
                chatPlayer(neutral, "The Chaos Altar, I think.")
                chatNpc(
                    angry,
                    "Hmph! Well, it's just a bit further down. Now, if you don't mind, I'm very " +
                        "busy here!",
                )
            }
            5 -> leaderOfTheDagonhai()
        }
    }

    // TODO(content): [verify:in_game] the spell's dance, headbang or jump emote and overhead
    // text, and whether level 80 itself deflects it
    private suspend fun Dialogue.leaderOfTheDagonhai() {
        chatPlayer(angry, "I'm the leader of the Dagon'hai. Bow before me!")
        chatNpc(
            laugh,
            "You? Leader of the Dagon'hai? Hah! I've never heard of such rubbish! You call " +
                "yourself a mage?",
        )
        when (choice2("I'm a very powerful mage!", 1, "No, you're right. I'm not a mage.", 2)) {
            1 -> {
                chatPlayer(happy, "I'm a very powerful mage!")
                chatNpc(quiz, "Really?")
                chatPlayer(madlaugh, "Yes! Be afraid! Be very afraid! Ha ha ha!")
                chatNpc(angry, "I see. Well then, let's see how you handle this!")
                chatPlayer(shocked, "What...?!?!")
                if (access.statBase("stat.magic") < DEFLECT_LEVEL) {
                    chatNpc(
                        laugh,
                        "Hah! I knew you were no mage! Let that be a lesson to you! Now, please " +
                            "leave.",
                    )
                    return
                }
                chatNpc(shocked, "So! You are a good mage! You deflected my spell.")
                chatPlayer(happy, "Hah! Yes. I told you so! Now, I have work to be done.")
                chatNpc(neutral, "Of course. Goodbye!")
            }
            2 -> {
                chatPlayer(sad, "No, you're right. I'm not a mage.")
                chatNpc(angry, "I didn't think so! Now go away. As I said, I'm very busy!")
            }
        }
    }

    private companion object {
        const val DEFLECT_LEVEL = 80
    }
}
