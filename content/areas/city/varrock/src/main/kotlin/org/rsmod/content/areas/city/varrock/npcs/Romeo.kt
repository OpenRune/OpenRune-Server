package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Romeo_%26_Juliet (before starting the quest)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:romeo_and_juliet] start offer, quest stages and after-quest dialogue
// TODO(content): [quest:romeo_and_juliet] "Romeo approaches you..." when the player is nearby
class Romeo : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.romeo") { startDialogue(it.npc) { romeo() } }
    }

    private suspend fun Dialogue.romeo() {
        chatNpc(sad, GREETINGS[access.random.of(GREETINGS.size)])
        val option =
            choice3(
                "Yes, I have seen her actually!",
                1,
                "No sorry, I haven't seen her.",
                2,
                "Perhaps I could help to find her for you?",
                3,
            )
        when (option) {
            1 -> seenHer()
            2 -> notSeenHer()
            3 -> helpFindHer()
        }
    }

    private suspend fun Dialogue.seenHer() {
        chatPlayer(happy, "Yes, I have seen her actually!")
        chatPlayer(neutral, "At least, I think it was her... Blonde? A bit stressed?")
        chatNpc(happy, "Golly...yes, yes...you make her sound very interesting!")
        chatNpc(happy, "And I'll bet she's a bit of a fox!")
        chatPlayer(neutral, "Well, I guess she could be considered attractive...")
        chatNpc(laugh, "I'll bet she is! Wooooooooo!")
        chatNpc(
            confused,
            "Sorry, all that jubilation has made me forget what we were talking about.",
        )
        chatPlayer(quiz, "You were asking me about Juliet? You seemed to know her?")
        chatNpc(happy, "Oh yes, Juliet!")
        chatNpc(
            happy,
            "The fox...could you tell her that she is the love of my long and that I life to be " +
                "with her?",
        )
        chatPlayer(
            confused,
            "What? Surely you mean that she is the love of your life and that you long to be " +
                "with her?",
        )
        loveMessageAgreed()
    }

    private suspend fun Dialogue.notSeenHer() {
        chatPlayer(neutral, "No sorry, I haven't seen her.")
        chatNpc(sad, "Oh...well, that's a shame...I was rather hoping you had.")
        chatPlayer(
            quiz,
            "Why? Is she a fugitive? Does she owe you some money or something?",
        )
        chatNpc(confused, "Hmmm, she might do? Perhaps she does? How do you know?")
        chatPlayer(angry, "I don't know? I was asking 'YOU' how 'YOU' know Juliet!")
        chatNpc(
            happy,
            "Ahh, yes Juliet, she's my one true love. Well, one of my one true loves! If you see " +
                "her, could you tell her that she is the love of my long and that I life to be " +
                "with her?",
        )
        chatPlayer(
            confused,
            "What? Surely you mean that 'she is the love of your life and that you long to be " +
                "with her?'",
        )
        loveMessageAgreed()
    }

    private suspend fun Dialogue.helpFindHer() {
        chatPlayer(quiz, "Perhaps I can help find her for you? What does she look like?")
        chatNpc(happy, "Oh would you? That would be great! She has this sort of hair...")
        chatPlayer(neutral, "Hair...check..")
        chatNpc(happy, "...and she these...great lips...")
        chatPlayer(neutral, "Lips...right.")
        chatNpc(happy, "Oh and she has these lovely shoulders as well..")
        chatPlayer(
            confused,
            "Shoulders...right, so she has hair, lips and shoulders...that should cut it down a " +
                "bit.",
        )
        chatNpc(
            happy,
            "Oh yes, Juliet is very different...please tell her that she is the love of my long " +
                "and that I life to be with her?",
        )
        chatPlayer(
            confused,
            "What? Surely you mean that 'she is the love of your life and that you long to be " +
                "with her?'",
        )
        loveMessageAgreed()
    }

    private suspend fun Dialogue.loveMessageAgreed() {
        chatNpc(
            happy,
            "Oh yeah...what you said...tell her that, it sounds much better! Oh you're so good " +
                "at this!",
        )
    }

    private companion object {
        val GREETINGS =
            listOf(
                "Blub! Blub...where is my Juliet? Have you seen her?",
                "Looking for a blonde girl, goes by the name of Juliet..quite pretty...haven't " +
                    "seen her have you?",
                "Juliet, Juliet, wherefore art thou Juliet? Have you seen my Juliet?",
                "Oh woe is me that I cannot find my Juliet! You haven't seen Juliet have you?",
                "Sadness surrounds me now that Juliet's father forbids us to meet. Have you seen " +
                    "my Juliet?",
                "What is to become of me and my darling Juliet, I cannot find her anywhere, have " +
                    "you seen her?",
            )
    }
}
