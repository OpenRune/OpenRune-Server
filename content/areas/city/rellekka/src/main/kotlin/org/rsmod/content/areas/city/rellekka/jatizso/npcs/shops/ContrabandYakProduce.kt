package org.rsmod.content.areas.city.rellekka.jatizso.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Vanligga_Gastfrihet (after The Fremennik Isles)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_isles] in-quest dialogue, window tax, and the store only
// opening if her tax was paid for her or repaid after the quest
// TODO(content): [system:met_flags] "Wanna buy some yak?" once the player has talked to her
class ContrabandYakProduce @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.frisd_izso_landlady") { startDialogue(it.npc) { vanligga(it.npc) } }
    }

    private suspend fun Dialogue.vanligga(npc: Npc) {
        chatPlayer(happy, "Hello, Vanligga!")
        chatNpc(happy, "Oh, hello ${fremennikName()}. Always good to see a friend.")
        chatNpc(
            shifty,
            "Hmmm, I suppose I can trust you, since you have done me a favour in the past. Can " +
                "you keep a secret?",
        )
        chatPlayer(happy, "Of course.")
        chatNpc(
            shifty,
            "Well as you probably already know, all yak meat and yak products are banned on " +
                "this island - orders of the King...",
        )
        chatPlayer(neutral, "That King has some very strange attitudes.")
        chatNpc(
            shifty,
            "Well, I have a contact on the other island. Are you interested in obtaining some " +
                "produce?",
        )
        when (choice2("Yes, please.", 1, "No, thanks.", 2)) {
            1 -> shops.open(player, npc, "Contraband yak produce.", "inv.frisd_yak_contraband")
            2 -> {
                chatPlayer(neutral, "No, thanks.")
                chatNpc(
                    shifty,
                    "Well, any time you need black market yak produce, you know where to come.",
                )
            }
        }
    }
}
