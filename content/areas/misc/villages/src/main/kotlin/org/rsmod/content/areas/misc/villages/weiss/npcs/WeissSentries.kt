package org.rsmod.content.areas.misc.villages.weiss.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Driftwood
// https://oldschool.runescape.wiki/w/Transcript:Icicle
// https://oldschool.runescape.wiki/w/Transcript:Root
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:making_friends_with_my_arm] the sentries' lines during the quest
class WeissSentries : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.my2arm_sentry_driftwood_background") {
            startDialogue(it.npc) { sentry(rootIntro = false) }
        }
        onOpNpc1("npc.my2arm_sentry_icicle_background") {
            startDialogue(it.npc) { sentry(rootIntro = false) }
        }
        onOpNpc1("npc.my2arm_sentry_root_background") {
            startDialogue(it.npc) { sentry(rootIntro = true) }
        }
    }

    private suspend fun Dialogue.sentry(rootIntro: Boolean) {
        chatNpc(happy, "Hey, friend of Snowflake!")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "Tell me about this place.",
                    2,
                    "Do you like it here?",
                    3,
                    "I'll be off.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    if (rootIntro) {
                        chatNpc(neutral, "I am Root.")
                        chatPlayer(quiz, "Is that all you have to say about yourself?")
                        chatNpc(neutral, "I am Root.")
                        chatPlayer(neutral, "Oh well, I suppose that's enough for some people.")
                    } else {
                        chatNpc(
                            neutral,
                            "Just a guard. If sneaky human come in here without permission, I " +
                                "chuck rocks at 'em, knock 'em out and chuck 'em out of Weiss.",
                        )
                        chatPlayer(neutral, "I'm familiar with the idea, yeah.")
                    }
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about this place.")
                    chatNpc(
                        neutral,
                        "This here is Weiss. Trolls built it years ago, after we got kicked out " +
                            "from caves further south. Now Weiss is our home.",
                    )
                    chatNpc(
                        happy,
                        "It great here, now Snowflake an' her husband troll taught us goutweed " +
                            "farming. Food so much better now.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Do you like it here?")
                    chatNpc(
                        neutral,
                        "Eh, it kinda rough, but it getting better. Snowflake, she taught us " +
                            "goutweed farming. Goat, wolf, human all taste so much nicer with " +
                            "goutweed in.",
                    )
                    chatNpc(
                        happy,
                        "So yeah, Weiss is good place to live, now Snowflake in charge. All " +
                            "trolls like Snowflake. Her husband troll kinda weird, but that not " +
                            "our problem.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'll be off.")
                    return
                }
            }
        }
    }
}
