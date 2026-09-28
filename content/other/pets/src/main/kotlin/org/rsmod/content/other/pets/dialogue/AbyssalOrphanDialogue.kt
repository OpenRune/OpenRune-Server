package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AbyssalOrphanDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatNpc(angry, "You killed my father.")
            when (choice2("Yeah, don't take it personally.", 1, "No, I am your father.", 2)) {
                1 -> {
                    chatPlayer(neutral, "Yeah, don't take it personally.")
                    chatNpc(
                        neutral,
                        "In his dying moment, my father poured his last ounce of strength " +
                            "into my creation. My being is formed from his remains.",
                    )
                    chatNpc(
                        neutral,
                        "When your own body is consumed to nourish the Nexus, and an army of " +
                            "scions arises from your corpse, I trust you will not take it personally either.",
                    )
                }
                2 -> {
                    chatPlayer(happy, "No, I am your father.")
                    chatNpc(neutral, "No, you are not.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.abyssalsire_pet"
    }
}
