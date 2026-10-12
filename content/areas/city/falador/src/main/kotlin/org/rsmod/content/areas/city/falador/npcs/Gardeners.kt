package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Gardeners : PluginScript() {
    override fun ScriptContext.startup() {
        for (gardener in GARDENERS) {
            onOpNpc1(gardener) { startDialogue(it.npc) { gardener() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gardener
    private suspend fun Dialogue.gardener() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            angry,
            "Oi'm busy. If tha' wants owt, tha' can go find Wyson. He's ta boss 'round here. And " +
                "KEEP YER TRAMPIN' FEET ORFF MA' FLOWERS!",
        )
        chatPlayer(confused, "Right...")
    }

    private companion object {
        val GARDENERS =
            listOf("npc.falador_gardener", "npc.falador_gardener2", "npc.falador_gardener_diary")
    }
}
