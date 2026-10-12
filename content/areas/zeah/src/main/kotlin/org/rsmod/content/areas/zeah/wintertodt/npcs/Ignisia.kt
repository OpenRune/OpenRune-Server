package org.rsmod.content.areas.zeah.wintertodt.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ignisia
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] "Welcome back, [brother/sister]. How may I be of
//  assistance?" on later visits
// TODO(content): [quest:the_forsaken_tower] "The Wintertodt is no longer growing in strength..."
//  after the quest
// TODO(content): [boss:wintertodt] the Exchange op
// TODO(content): [verify:in_game] when Ignisia says "my child" instead of brother or sister
class Ignisia : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wint_master_pyromancer") { startDialogue(it.npc) { ignisia() } }
    }

    private suspend fun Dialogue.ignisia() {
        val sibling = if (access.isBodyTypeA()) "brother" else "sister"
        chatNpc(neutral, "Welcome, $sibling.")
        chatPlayer(confused, "Er... Are we related?")
        chatNpc(
            neutral,
            "We are all related, $sibling, for we are all children of the flame.",
        )
        chatPlayer(confused, "Okayyy...")
        chatPlayer(quiz, "So what is this place?")
        chatNpc(
            neutral,
            "You stand before the Doors of Dinh. Behind them lies the prison of the Wintertodt.",
        )
        chatPlayer(quiz, "What's a Wintertodt?")
        chatNpc(
            worried,
            "We do not know what it is. All we know is that it threatens to snuff out every " +
                "flame in this land and plunge the world into eternal darkness.",
        )
        chatPlayer(worried, "Oh dear... We should probably do something about that.")
        chatNpc(
            neutral,
            "That is why we are here, $sibling. Ever since the Wintertodt was imprisoned, " +
                "members of the Order of the Sacred Flame, or Pyromancers as we're often known, " +
                "have stood vigil over this place.",
        )
        chatNpc(
            sad,
            "But alas, despite our best efforts, the Wintertodt is growing in power again.",
        )
        chatPlayer(quiz, "Well that's just no good. Is there anything I can do to help?")
        chatNpc(
            neutral,
            "Our order searches tirelessly for a way to defeat the Wintertodt. Until we discover " +
                "it, the Wintertodt must be held at bay.",
        )
        if (player.statBase("stat.firemaking") < WINTERTODT_FIREMAKING) {
            chatNpc(
                neutral,
                "However, I fear your abilities are not great enough to withstand the cold of " +
                    "the Wintertodt. Learn the ways of the sacred flame and then return to me.",
            )
            mesbox("You require at least 50 Firemaking to take on the Wintertodt.")
        } else {
            chatNpc(
                neutral,
                "Pass through the Doors of Dinh to the prison within. You will find the finest " +
                    "of our order channeling the power of the sacred flame into the Wintertodt.",
            )
            chatNpc(
                neutral,
                "Help them keep the holy fires lit so that they can keep the Wintertodt subdued.",
            )
            sacredFlame()
        }
        assistance(sibling)
    }

    private suspend fun Dialogue.assistance(sibling: String) {
        while (true) {
            chatNpc(neutral, "How else may I be of assistance?")
            val option =
                choice5(
                    "Tell me about the Wintertodt.",
                    1,
                    "Tell me about the Pyromancers.",
                    2,
                    "Tell me about yourself.",
                    3,
                    "What can I do to help?",
                    4,
                    "I'm fine thanks.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the Wintertodt.")
                    chatNpc(
                        neutral,
                        "Do you feel the winds of winter as they lick across the land? This is " +
                            "the power of the Wintertodt. We know not what it is nor from where " +
                            "it came. Only that it wields the power of Winter itself.",
                    )
                    chatNpc(
                        sad,
                        "The ground on which we stand was once full of light, full of life. But " +
                            "then the Wintertodt came. Now the fires of these lands burn no more.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about the Pyromancers.")
                    chatNpc(
                        neutral,
                        "We are the Order of the Sacred Flame, formed over a thousand years ago " +
                            "when the Wintertodt first threatened Great Kourend.",
                    )
                    chatNpc(
                        neutral,
                        "We fought against the Wintertodt until we managed to lure it behind " +
                            "Dinh's great Doors. Our task did not end there though, for we " +
                            "always knew that the Wintertodt would rise again.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(
                        neutral,
                        "I am Ignisia, Grand Master of the Order of the Sacred Flame. I have " +
                            "served our Order faithfully since I passed the Trials of Fire " +
                            "before the Ascent of Arceuus.",
                    )
                }
                4 -> help()
                else -> {
                    chatPlayer(neutral, "I'm fine thanks.")
                    chatNpc(neutral, "May the sacred flame guide you, $sibling.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.help() {
        chatPlayer(quiz, "What can I do to help?")
        if (player.statBase("stat.firemaking") < WINTERTODT_FIREMAKING) {
            chatNpc(
                neutral,
                "You are not currently strong enough to face the Wintertodt. Learn the ways of " +
                    "the sacred flame and then return here.",
            )
            mesbox("You require at least 50 Firemaking to take on the Wintertodt.")
            return
        }
        chatNpc(
            neutral,
            "We must hold back the Wintertodt until we can find a way to defeat it. Head into " +
                "the prison and keep the holy fires lit so that our order may keep the evil at " +
                "bay.",
        )
        sacredFlame()
    }

    private suspend fun Dialogue.sacredFlame() {
        chatPlayer(quiz, "How can I do that?")
        chatNpc(
            neutral,
            "You will find the roots of the great Bruma tree have crept into the prison. Chop " +
                "the roots and use them to keep the sacred flame alive.",
        )
        chatPlayer(quiz, "Anything else?")
        chatNpc(
            neutral,
            "You can use the herbs of the Bruma tree to make Rejuvenation potions. Give these to " +
                "any of our order who fall to the cold of the Wintertodt before they succumb to " +
                "the darkness.",
        )
    }

    private companion object {
        const val WINTERTODT_FIREMAKING = 50
    }
}
