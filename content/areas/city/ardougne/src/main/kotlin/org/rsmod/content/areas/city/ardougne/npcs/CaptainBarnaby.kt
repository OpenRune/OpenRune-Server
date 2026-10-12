package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Captain_Barnaby (East Ardougne dock)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:ship_travel] taking 30 coins and sailing to Rimmington or Brimhaven
// TODO(content): [system:ship_travel] Brimhaven and Rimmington quick-travel ops
// TODO(content): [diary:karamja] free travel with Karamja gloves
// TODO(content): [quest:ghosts_ahoy] free-travel "[Charm]" option with the ring of charos (a)
class CaptainBarnaby : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BARNABY) { startDialogue(it.npc) { barnaby() } }
    }

    private suspend fun Dialogue.barnaby() {
        chatNpc(
            happy,
            "Hello there. Would you like to travel somewhere? I can take you to Rimmington or " +
                "Brimhaven for 30 coins.",
        )
        val destination =
            choice3(
                "I'd like to go to Rimmington.",
                "Rimmington",
                "I'd like to go to Brimhaven.",
                "Brimhaven",
                "I'm good thanks.",
                null,
            )
        if (destination == null) {
            chatPlayer(neutral, "I'm good thanks.")
            return
        }
        chatPlayer(happy, "I'd like to go to $destination.")
        if (access.invTotal(player.inv, "obj.coins") < FARE) {
            chatNpc(neutral, "Come back when you've got 30 coins for me.")
        }
    }

    private companion object {
        const val BARNABY = "npc.captain_barnaby"
        const val FARE = 30
    }
}
