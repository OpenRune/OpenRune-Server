package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Talking to a camel is always an insult, and the camel always takes it badly. */
class AlKharidCamels : PluginScript() {
    override fun ScriptContext.startup() {
        for (camel in CAMELS) {
            onOpNpc1(camel) { insult(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.insult(npc: Npc) {
        val remark = INSULTS[random.of(maxExclusive = INSULTS.size)]
        val reaction = REACTIONS[random.of(maxExclusive = REACTIONS.size)]
        startDialogue(npc) { chatPlayer(neutral, remark) }
        mes(reaction)
    }

    private companion object {
        private val CAMELS =
            listOf(
                "npc.camel",
                "npc.al_the_camel",
                "npc.cam_the_camel",
                "npc.elly_the_camel",
                "npc.alice_the_camel_multinpc",
            )

        private val INSULTS =
            listOf(
                "Mmm... looks like that camel would make a nice kebab.",
                "If I go near that camel, it'll probably bite my hand off.",
                "I wonder if that camel has fleas...",
            )

        private val REACTIONS =
            listOf(
                "The camel tries to stamp on your foot, but you pull it back quickly.",
                "The camel turns its head and glares at you.",
                "The camel spits at you, and you jump back hurriedly.",
            )
    }
}
