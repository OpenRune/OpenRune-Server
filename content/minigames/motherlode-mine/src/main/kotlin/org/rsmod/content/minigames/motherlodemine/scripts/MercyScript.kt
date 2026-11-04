package org.rsmod.content.minigames.motherlodemine.scripts

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeUpperFloorUnlocked
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MercyScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(MotherlodeConstants.NPC_MERCY) { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { greeting() }

    private suspend fun Dialogue.greeting() {
        if (!player.motherlodeUpperFloorUnlocked) {
            chatNpc(neutral, "You shouldn't be up here!")
            return
        }
        chatNpc(
            neutral,
            "Why, hello there, young'un. Did my boy Percy say you could come up here?",
        )
        chatPlayer(neutral, "Yes, I had to pay him to get access to this area.")
        chatNpc(happy, "Ah, that's my Percy alright. I raised him well.")
    }

    companion object {
        suspend fun ProtectedAccess.mercyWarnsAboutHopper() =
            startDialogue {
                chatNpcSpecific(
                    "Mercy",
                    MotherlodeConstants.NPC_MERCY,
                    neutral,
                    "I don't think you should be using that without speaking to Percy first.",
                )
            }
    }
}
