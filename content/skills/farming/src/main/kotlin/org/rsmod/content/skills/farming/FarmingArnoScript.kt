package org.rsmod.content.skills.farming

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.boolVarp
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingArnoScript : PluginScript() {
    private var Player.seedOptOut by boolVarp("varp.farming_hespori_seed_opt_out")

    override fun ScriptContext.startup() {
        onOpNpc1(ARNO) {
            startDialogue(it.npc) {
                chatNpc(neutral, "Hello there.")
                options()
            }
        }
        onOpNpc3(ARNO) {
            startDialogue(it.npc) {
                chatNpc(
                    neutral,
                    "I'm afraid I don't have anything for you. If I did have any of your items, " +
                        "but you died before collecting them, they'll now be lost.",
                )
            }
        }
    }

    private suspend fun Dialogue.options() {
        while (true) {
            val optOut = !player.seedOptOut
            val seedOption =
                if (optOut) "I don't want to find Hespori seeds when farming."
                else "I want to find Hespori seeds when farming."
            when (
                choice4(
                    "What's in this cave?", 1,
                    "Do you have any of my items?", 2,
                    seedOption, 3,
                    "Goodbye.", 4,
                )
            ) {
                1 -> cave()
                2 -> {
                    chatPlayer(neutral, "Do you have any of my items?")
                    chatNpc(
                        neutral,
                        "I'm afraid not. If I did have any of your items, but you died before " +
                            "collecting them, they'll now be lost.",
                    )
                    return
                }
                3 -> {
                    chatPlayer(neutral, seedOption)
                    player.seedOptOut = optOut
                    chatNpc(
                        neutral,
                        if (optOut) "Very well. You'll no longer find Hespori seeds when farming."
                        else "Very well. You'll now have a chance of finding Hespori seeds when farming.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
            chatNpc(neutral, "Do you need anything else?")
        }
    }

    private suspend fun Dialogue.cave() {
        chatPlayer(neutral, "What's in this cave?")
        chatNpc(
            neutral,
            "A unique farming patch. This cave is the only place we've succesfully been able " +
                "to grow a Hespori.",
        )
        chatPlayer(neutral, "What's a Hespori?")
        chatNpc(
            neutral,
            "A rare carnivorous plant originally from Morytania. When grown, the plant becomes " +
                "conscious and highly aggressive. They are truly fascinating.",
        )
        chatPlayer(neutral, "Can I grow one?")
        chatNpc(
            neutral,
            "Of course. Just plant some Hespori seeds in the cave below and it will grow like " +
                "any other plant. A Hespori is pretty tough so you don't need to worry about it " +
                "getting diseased.",
        )
        chatNpc(neutral, "Do be careful though. A fully grown Hespori can be quite troublesome to deal with.")
    }

    private companion object {
        const val ARNO = "npc.farming_guild_hespori_farmer"
    }
}
