package org.rsmod.content.areas.misc.lobbies.tempoross.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Spirit_Angler_(Reward_pool)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server inv for Flakes 'n' Flotsam (spirit flake currency):
//  Trade op and "Do you have any of those useful trinkets you mentioned?"
// TODO(content): [system:tempoross] Take-net option (gives the net without dialogue)
class RewardPoolAngler @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tempoross_lobby_pool_watcher") { startDialogue(it.npc) { angler() } }
    }

    private suspend fun Dialogue.angler() {
        chatPlayer(happy, "Hello there.")
        chatNpc(silent, "...")
        while (true) {
            val option =
                choice4(
                    "Uh... What are you doing?",
                    1,
                    "Do you have a spare fishing net I could have?",
                    2,
                    "Do you have any of those useful trinkets you mentioned?",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> whatAreYouDoing()
                2 -> spareNet()
                3 -> {
                    chatPlayer(quiz, "Do you have any of those useful trinkets you mentioned?")
                    chatNpc(
                        neutral,
                        "I might do... But it's going to cost you a hefty amount of spirit " +
                            "flakes, here take a look.",
                    )
                    return
                }
                4 -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whatAreYouDoing() {
        chatPlayer(confused, "Uh... What are you doing?")
        chatNpc(shocked, "Oh sorry, I was somewhere else entirely!")
        chatNpc(neutral, "I'm just watching the debris wash up at the shore here.")
        chatNpc(neutral, "It's sort of mesmerising...")
        chatPlayer(quiz, "Does anything useful wash up?")
        chatNpc(silent, "...")
        chatNpc(
            happy,
            "Oh, yes! All sorts of things get washed up by the storms. Aside from the usual " +
                "materials, I've seen some interesting trinkets including odd looking books and " +
                "fish preservation barrels.",
        )
        chatNpc(
            neutral,
            "If you help in the fight against Tempoross, the Captain's said you can help " +
                "yourself to anything that washes up.",
        )
        chatPlayer(happy, "I'll keep an eye out, thank you!")
        chatNpc(neutral, "Any time...")
        mesbox("His voice trails off as he stares back out to sea.")
    }

    private suspend fun Dialogue.spareNet() {
        chatPlayer(quiz, "Do you have a spare fishing net I could have?")
        chatNpc(silent, "...")
        chatPlayer(confused, "...")
        if (access.invTotal(player.inv, NET) > 0) {
            mesbox(
                "Without turning his head, the man shakes his finger at you. Maybe because you " +
                    "already have a net."
            )
            return
        }
        mesbox("Without turning his head, the man holds up a fishing net.")
        player.invAddOrDrop(objRepo, NET)
        chatPlayer(neutral, "Thanks...")
    }

    private companion object {
        const val NET = "obj.net"
    }
}
