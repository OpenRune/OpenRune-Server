package org.rsmod.content.areas.city.barbarianvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Checkal : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bim_checkal") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            if (npc.type.isType("npc.bim_checkal_postquest")) postQuest() else preQuest()
        }

    private suspend fun Dialogue.preQuest() {
        val title = if (access.isBodyTypeA()) "sir" else "miss"
        mesbox("Checkal stares at you intensely.")
        chatNpc(angry, "You best not be here to start any trouble $title.")
        chatPlayer(neutral, "Wouldn't dream of it.")
    }

    private suspend fun Dialogue.postQuest() {
        chatNpc(happy, "${player.displayName}, I am so glad to see you are safe.")
        chatPlayer(quiz, "Hello, Checkal. Why didn't you tell me about Willow?")
        chatPlayer(sad, "I thought we were friends...")
        chatNpc(
            sad,
            "I am sorry ${player.displayName}. I do respect you and your love for the muscle. " +
                "But sadly I needed the money. These muscles don't pay the bills on their own.",
        )
        chatPlayer(neutral, "Oh, I understand...")
        chatNpc(
            happy,
            "Please do not let this incident divert you from your path of becoming a strongman! " +
                "I truly believe in you.",
        )
    }
}
