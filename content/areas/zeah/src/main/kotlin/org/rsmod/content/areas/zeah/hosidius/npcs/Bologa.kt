package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Bologa
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] "Hello again" greetings before and after buying the blessing
// TODO(content): [system:god_items] Zamorakian and other-god equipment checks; until then nobody
//  passes the dress check, so the 75,000 coin blessing purchase (varbit.hosidius_evilgrapes) is
//  never offered
class Bologa : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_evilgrapegirl") { startDialogue(it.npc) { bologa() } }
    }

    private suspend fun Dialogue.bologa() {
        chatNpc(bored, "Yeah?")
        val option =
            choice3("What is this place?", 1, "Tell me about yourself.", 2, "Never mind.", 3)
        when (option) {
            1 -> whatIsThisPlace()
            2 -> aboutYourself()
            else -> chatPlayer(neutral, "Never mind.")
        }
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            bored,
            "Granddad's fruit farm. It's a right dump. This whole corner of the city is a dump.",
        )
        val why =
            choice2(
                "So why are you here if it's a dump?",
                true,
                "Okay, thanks for the info.",
                false,
            )
        if (why) {
            chatPlayer(quiz, "So why are you here if it's a dump?")
            chatNpc(
                angry,
                "My cow of a sister kicked me out of her bar. Said I was getting the place a bad " +
                    "name. So I'm staying here until I'm old enough to get my own place.",
            )
            chatNpc(
                happy,
                "I want to move to Arceuus. It's properly awesome up there. I'll do bartending " +
                    "or something until I can learn to be a necromancer.",
            )
            chatNpc(bored, "Anyway, why are you here? Do you want something?")
        } else {
            chatPlayer(neutral, "Okay, thanks for the info.")
            chatNpc(
                bored,
                "Yeah, you're in the biggest dumpster of Dumpsterville. Now you know. Anyway, do " +
                    "you want something?",
            )
        }
        if (choice2("Tell me about yourself.", true, "Never mind.", false)) {
            aboutYourself()
        } else {
            chatPlayer(neutral, "Never mind.")
        }
    }

    private suspend fun Dialogue.aboutYourself() {
        chatPlayer(quiz, "Tell me about yourself.")
        chatNpc(bored, "Yeah, alright...")
        chatNpc(
            happy,
            "I am Bologa Gricoller, seeker of forbidden wisdom, wanderer of the dark pathways, " +
                "thinker of the unthinkable. I can do bartending too.",
        )
        chatNpc(
            neutral,
            "I used to work at The Golden Field. That's my sister's bar. It's mostly farmhands " +
                "drinking there, but you get some interesting people as well.",
        )
        chatNpc(
            bored,
            "See, most Hosidius people are Saradominists. My sister, she's always listening to " +
                "Father Jean at the church with his goodie-goodie sermons.",
        )
        chatNpc(
            neutral,
            "But some of us in the bar thought we'd give Zamorak a try. Like, you gotta be " +
                "open-minded, right? What did Saradomin ever do for us anyway?",
        )
        chatNpc(
            shifty,
            "So we'd meet in the bar, sharing books that the Piscarilius smugglers would bring " +
                "us, learning what the Saradominists don't want us to know.",
        )
        chatNpc(
            angry,
            "Anyway, my sister - she's a right cow, by the way - she fired me and kicked me out. " +
                "Said I was bringing bad sorts to her bar.",
        )
        chatNpc(
            angry,
            "Bad sorts, she said! Like, morality is totally subjective, right? So who's she to " +
                "call us bad sorts just 'cos we're not afraid to think differently?",
        )
        chatNpc(
            neutral,
            "Anyway, I've learnt all this cool stuff about Zamorak, and being stuck in a fruit " +
                "farm doesn't stop me being his loyal servant, right?",
        )
        chatNpc(
            happy,
            "In fact I've invented a Zamorakian blessing that works on fruit! You can use it to " +
                "make Zamorak's wine out of grapes from the Vinery.",
        )
        val wine =
            choice2(
                "Can you help me to make Zamorak's wine?",
                true,
                "I'll leave you to it.",
                false,
            )
        if (!wine) {
            chatPlayer(neutral, "I'll leave you to it.")
            return
        }
        chatPlayer(quiz, "Can you help me to make Zamorak's wine?")
        chatNpc(
            neutral,
            "I'll let you have access to the Zamorakian blessing I invented, so you can invoke " +
                "Zamorak's unholy power upon the grapes... if I think you're up to it.",
        )
        chatNpc(neutral, "I'll want 75,000 coins too.")
        chatPlayer(quiz, "And do you think I'm up to it?")
        if (player.statBase("stat.prayer") < BLESSING_PRAYER_LEVEL) {
            chatNpc(
                laugh,
                "Nah, I bet you couldn't even bless an unholy symbol - you're not going to get " +
                    "very far with my grape blessing.",
            )
            mesbox("You need a Prayer level of 50 to use Bologa's blessing.")
            return
        }
        chatNpc(
            bored,
            "You don't look much like a Zamorakian to me. Dress Zamorakian if you want my help. " +
                "And I'll still want 75,000 coins.",
        )
    }

    private companion object {
        const val BLESSING_PRAYER_LEVEL = 50
    }
}
