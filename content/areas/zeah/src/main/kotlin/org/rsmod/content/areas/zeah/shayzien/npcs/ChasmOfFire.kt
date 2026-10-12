package org.rsmod.content.areas.zeah.shayzien.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ChasmOfFire : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.disciple_of_yama_cavern") { startDialogue(it.npc) { disciple() } }
        onOpNpc1("npc.voice_of_yama") { startDialogue(it.npc) { voiceOfYama() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Disciple_of_Yama (inside the Chasm of Fire)
    private suspend fun Dialogue.disciple() {
        chatPlayer(quiz, "Are you okay?")
        chatNpc(happy, "Oh yes! Just praying to my new master. It's hot down here, isn't it?")
        chatPlayer(neutral, "I guess so.")
        while (true) {
            val option =
                choice3(
                    "Your new master, that would be...?",
                    1,
                    "What happened to your eyes?",
                    2,
                    "Well, I'll leave you to it.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Your new master, that would be...?")
                    chatNpc(
                        happy,
                        "Yama! I sold my soul you see, well, I guess that Ox-headed guy down " +
                            "there forcibly removed it.",
                    )
                    chatPlayer(worried, "That doesn't sound like a fair trade...")
                    chatNpc(
                        happy,
                        "Oh, don't worry, I'm perfectly fine with the outcome. Sure, the loss of " +
                            "vision is a shame but it could be worse.",
                    )
                    chatPlayer(quiz, "Could it?")
                    chatNpc(
                        neutral,
                        "Underground there are no tall buildings, I'm awfully afraid of heights, " +
                            "you see.",
                    )
                    chatPlayer(neutral, "Perhaps it's a good thing you can't see...")
                    chatNpc(quiz, "What was that?")
                    chatPlayer(neutral, "Oh, nothing...")
                }
                2 -> {
                    chatPlayer(quiz, "What happened to your eyes?")
                    chatNpc(neutral, "The ritual, there were some... adverse effects...")
                    chatPlayer(quiz, "The ritual?")
                    chatNpc(
                        happy,
                        "Oh yes! Pretty basic really, head of an Ox, bruma roots, lots of fire " +
                            "and of course the human sacrifice.",
                    )
                    chatPlayer(quiz, "And at what point did that blind you?")
                    chatNpc(
                        neutral,
                        "The voice demanded that we stare into the fire until we saw " +
                            "enlightenment.",
                    )
                    chatPlayer(neutral, "I guess the fire was pretty bright.")
                    chatNpc(neutral, "Blindingly so.")
                    chatPlayer(shocked, "Wait! Did you say human sacrifice?")
                    chatNpc(happy, "Why, of course! We're Zamorakians, we're evil...")
                    chatPlayer(neutral, "Yeah, I guess that's true.")
                }
                else -> {
                    chatPlayer(neutral, "Well, I'll leave you to it.")
                    chatNpc(sad, "So soon?")
                    chatPlayer(neutral, "Yep, the demons won't slay themselves.")
                    chatNpc(confused, "What demons?")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Voice_of_Yama
    // TODO(content): [quest:a_kingdom_divided] the first and later talks after the quest, and the
    // trip to Yama's domain
    private suspend fun Dialogue.voiceOfYama() {
        chatNpc(angry, "What is it, human? Speak and begone, your presence disgusts me!")
        chatPlayer(worried, "Oh, uh, I'm sorry.")
        chatNpc(angry, "Stop your muttering and get to the point!")
        chatPlayer(worried, "W-Who are you?")
        chatNpc(neutral, "I am the Voice of Yama.")
        chatPlayer(quiz, "Yama?")
        chatNpc(neutral, "Yama is my master, a powerful demon of fire.")
        chatPlayer(quiz, "Why can't Yama speak for themself?")
        chatNpc(
            angry,
            "Currently my master resides below us, building strength. Worry not, human, my " +
                "master will eventually own your soul too.",
        )
        chatPlayer(worried, "I'll be leaving now.")
        chatNpc(angry, "I suspect you should.")
        mesbox("You must complete <col=000080>A Kingdom Divided</col> to access Yama's domain.")
    }
}
