package org.rsmod.content.areas.city.canifis.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Fidelio
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(FIDELIO) { startDialogue(it.npc) { fidelio(it.npc) } }
        onOpNpc3(FIDELIO) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "General Store", "inv.werewolfgeneralstore")
    }

    private suspend fun Dialogue.fidelio(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            worried,
            "H-hello. You l-look like a s-stranger to these p-parts. Would you l-like to buy " +
                "something? I h-have some s- special offers at the m-minute...some s-sample " +
                "bottles for s-storing s-snail slime.",
        )
        val option =
            choice3("Yes, please.", 1, "No thanks.", 2, "Why are your prices so high?", 3)
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                player.openShop(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(
                    sad,
                    "(sigh) Th-that's okay. Nobody ever w-wants to buy my wares. Oh, s-sure, if " +
                        "it was food or c-clothes they would though!",
                )
            }
            3 -> {
                chatPlayer(quiz, "Why are your prices so high?")
                chatNpc(
                    worried,
                    "As you p-probably know, the h-humans hate our kind and k-keep us trapped " +
                        "here. To get my s-stocks I have to sneak into the human lands in " +
                        "s-secret!",
                )
                chatNpc(
                    worried,
                    "All the s-secrecy and d-danger has played h-havoc with my nerves!",
                )
                chatPlayer(angry, "But all your stuff is overpriced junk!")
                chatNpc(
                    shocked,
                    "N-not at all! They are v-valuable human artefacts smuggled here at g-great " +
                        "personal risk!",
                )
            }
        }
    }

    private companion object {
        const val FIDELIO = "npc.werewolfshopkeeper3"
    }
}
