package org.rsmod.content.areas.misc.lobbies.tempoross.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Gita_Prymes
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:tempoross] converting Angler pieces to Spirit Angler pieces for spirit
//  flakes and back (needs the paid-conversion flag), and using items on her
class GitaPrymes : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tempoross_lobby_outfit_vendor") { startDialogue(it.npc) { gita() } }
    }

    private suspend fun Dialogue.gita() {
        chatNpc(happy, "Have ye come for an outfit like mine? Or else whatcha want?")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "Let's talk about outfits.",
                    2,
                    "I don't think I need anything from you.",
                    3,
                )
            when (option) {
                1 -> whoAreYou()
                2 -> {
                    outfits()
                    return
                }
                3 -> {
                    chatPlayer(neutral, "I don't think I need anything from you.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            happy,
            "Don't ye recognise the greatest angler in all the land? Never mind Grandpa Jack an' " +
                "Big Dave, or that jack-of-all-trades Mac - I'm Gita Prymes!",
        )
        val competition =
            choice2(
                "Have you won the Hemenster Fishing Competition?",
                true,
                "Okay, you're Gita Prymes.",
                false,
            )
        if (competition) {
            chatPlayer(quiz, "Have you won the Hemenster Fishing Competition?")
            chatNpc(confused, "Um...")
            chatNpc(
                angry,
                "Keep yer triflin' talk of poxy contests - Gita Prymes has bigger fish to catch! " +
                    "And I am Gita Prymes.",
            )
        }
        chatPlayer(neutral, "Okay, you're Gita Prymes.")
        chatNpc(happy, "That I be! Now, are ye here for an outfit like mine?")
    }

    private suspend fun Dialogue.outfits() {
        chatPlayer(happy, "Let's talk about outfits.")
        chatNpc(
            neutral,
            "Aye. See, a lot of anglers wear ugly green stuff. It 'elps them wi' the fishing, to " +
                "be sure, but they look so rotten in it. So Gita Prymes invented something " +
                "better- looking. The Spirit Angler outfit gives the same Fishing XP bonus as " +
                "the standard Angler kit.",
        )
        chatNpc(
            neutral,
            "It'll cost ye some spirit flakes but Gita Prymes can change yer Spirit Angler back " +
                "to normal Angler for free if ye want it. Which ever suits ye.",
        )
        val toSpirit =
            choice2(
                "Ask about the Spirit Angler outfit.",
                true,
                "Ask about the regular Angler outfit.",
                false,
            )
        if (toSpirit) {
            chatPlayer(quiz, "Can you turn my Angler outfit into the Spirit Angler outfit?")
            chatNpc(
                neutral,
                "Sure, hand me a piece of the regular Angler kit. I'll want 1,200 spirit flakes " +
                    "for a hat, the top for 1,200 spirit flakes, the leggings for 1,200 spirit " +
                    "flakes and the footwear for 1,200 spirit flakes.",
            )
            if (!carriesAny(ANGLER_KIT)) {
                chatPlayer(
                    sad,
                    "Actually, I don't have any Angler kit on me at the moment, sorry.",
                )
            }
            return
        }
        chatPlayer(
            quiz,
            "Can you turn my Spirit Angler outfit back into the regular Angler outfit?",
        )
        chatNpc(
            neutral,
            "Sure, hand me a piece of the Spirit Angler kit an' I'll convert it back for free.",
        )
        if (!carriesAny(SPIRIT_ANGLER_KIT)) {
            chatPlayer(
                sad,
                "Actually, I don't have any Spirit Angler kit on me at the moment, sorry.",
            )
        }
    }

    private fun Dialogue.carriesAny(objs: List<String>): Boolean =
        objs.any { access.invTotal(player.inv, it) > 0 }

    private companion object {
        val ANGLER_KIT =
            listOf(
                "obj.trawler_reward_hat",
                "obj.trawler_reward_top",
                "obj.trawler_reward_legs",
                "obj.trawler_reward_boots",
            )
        val SPIRIT_ANGLER_KIT =
            listOf(
                "obj.spirit_angler_hat",
                "obj.spirit_angler_top",
                "obj.spirit_angler_legs",
                "obj.spirit_angler_boots",
            )
    }
}
