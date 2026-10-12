package org.rsmod.content.areas.zeah.arceuus.npcs

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.spells.autocast.MagicSpellbookManager
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Tyss
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:kourend_and_kebos] ash sanctifier after the hard diary
// TODO(content): [verify:in_game] the Spellbook op is assumed to swap books without dialogue
class Tyss @Inject constructor(private val spellbooks: MagicSpellbookManager) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TYSS) { startDialogue(it.npc) { tyss() } }
        onOpNpc3(TYSS) { player.toggleSpellbook() }
    }

    private fun Player.onArceuus(): Boolean =
        spellbooks.activeSpellbook(this) == Spellbook.Arceuus

    private fun Player.toggleSpellbook() {
        val book = if (onArceuus()) Spellbook.Standard else Spellbook.Arceuus
        spellbooks.setSpellbook(this, book)
    }

    private suspend fun Dialogue.tyss() {
        chatNpc(
            neutral,
            "Tread carefully in this hallowed place, traveller. The magicks of the Dark Altar " +
                "are not to be treated lightly.",
        )
        while (true) {
            val spellbookOption =
                if (player.onArceuus()) {
                    "I'd like to stop using your spellbook now."
                } else {
                    "Can I try the magicks myself?"
                }
            val option =
                choice5(
                    "What is this place?",
                    1,
                    spellbookOption,
                    2,
                    "Do you sell souvenirs?",
                    3,
                    "Can I have an ash sanctifier, please?",
                    4,
                    "Can I sacrifice trophy heads on the altar?",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "The Dark Altar is the treasure of Arceuus. Our forebears spent their " +
                            "lives - literally, in some cases - learning to unlock its secrets " +
                            "and wield its power.",
                    )
                    chatNpc(
                        neutral,
                        "The power of the Dark Altar, reflected in these great crystals, can " +
                            "twist dimensions, warp minds, and even bridge the gulf between life " +
                            "and death.",
                    )
                }
                2 -> {
                    spellbook()
                    return
                }
                3 -> {
                    souvenir()
                    return
                }
                4 -> {
                    chatPlayer(quiz, "Can I have an ash sanctifier, please?")
                    chatNpc(
                        neutral,
                        "No. You need to complete all of the hard tasks in your Kourend & Kebos " +
                            "Achievement Diary.",
                    )
                    return
                }
                else -> {
                    chatPlayer(quiz, "Can I sacrifice my trophy heads on the altar?")
                    chatNpc(
                        neutral,
                        "Yes, I believe the Dark Altar may even reward you for such a sacrifice.",
                    )
                    chatPlayer(happy, "Thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.spellbook() {
        if (player.onArceuus()) {
            chatPlayer(neutral, "I'd like to stop using your spellbook now.")
            spellbooks.setSpellbook(player, Spellbook.Standard)
            chatNpc(
                neutral,
                "Very well, the magicks of the Dark Altar have been lifted from your mind.",
            )
            return
        }
        chatPlayer(quiz, "Can I try the magicks myself?")
        spellbooks.setSpellbook(player, Spellbook.Arceuus)
        chatNpc(
            neutral,
            "Very well. If you find an ensouled cadaver in your travels, you may attempted to " +
                "reanimate it with the magicks I have placed in your mind.",
        )
        chatPlayer(happy, "Thanks.")
    }

    private suspend fun Dialogue.souvenir() {
        chatPlayer(quiz, "Do you sell souvenirs?")
        if (!player.invAdd(player.inv, SIGNET).success) {
            chatNpc(
                neutral,
                "I suppose you are a friend to us. Free up some space and I will give you " +
                    "something.",
            )
            return
        }
        chatNpc(neutral, "I suppose you are a friend to us. Here, you may have this.")
        objbox(SIGNET, "Tyss gives you a signet in the shape of a crystal from the Dark Altar.")
    }

    private companion object {
        const val TYSS = "npc.arceuus_darkguardian"
        const val SIGNET = "obj.poh_arceuus_signet"
    }
}
