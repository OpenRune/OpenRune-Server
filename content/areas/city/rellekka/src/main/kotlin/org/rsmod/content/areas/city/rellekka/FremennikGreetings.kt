package org.rsmod.content.areas.city.rellekka

import org.rsmod.api.player.dialogue.Dialogue

// https://oldschool.runescape.wiki/w/Transcript:Borrokar (after The Fremennik Trials; shared by
// the Rellekka citizens and the council members Agnar, Inga and Pontak)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
internal suspend fun Dialogue.fremennikGreeting() {
    val name = fremennikName()
    val sibling = brotherOrSister()
    chatPlayer(happy, "Hello.")
    when (access.random.of(8)) {
        0 -> {
            chatNpc(
                happy,
                "Hello again $sibling $name. What need you of me this day? You can always rely " +
                    "on my assistance!",
            )
            chatPlayer(happy, "Nothing thanks. I just fancied a chat.")
            chatNpc(happy, "My door is always open should you ever need my help $name.")
        }
        1 -> chatNpc(happy, "$name... It has been too long since last we met!")
        2 -> chatNpc(happy, "Welcome back $sibling $name. It is always a pleasure to see you here!")
        3 -> {
            chatNpc(
                neutral,
                "My apologies $sibling $name! I have not time to tarry here with you!",
            )
            chatPlayer(neutral, "That's okay, I didn't really want anything.")
        }
        4 -> {
            chatNpc(happy, "How are you $name? I hope all fares well with you this day!")
            chatPlayer(happy, "I'm fine, thanks, how are you?")
            chatNpc(happy, "All is well with me also!")
        }
        5 -> chatNpc(happy, "Hello to you, too!")
        6 -> {
            chatNpc(quiz, "Hello. Did you want something $name?")
            chatPlayer(neutral, "No, not really.")
            chatNpc(neutral, "Well... okay then!")
        }
        else -> chatNpc(happy, "Always good to see you $sibling $name.")
    }
}
