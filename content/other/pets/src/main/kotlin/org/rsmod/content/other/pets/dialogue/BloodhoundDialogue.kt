package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BloodhoundDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOpU(NPC) { useItem(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(5)) {
                0 -> {
                    chatPlayer(quiz, "How come I can talk to you without an amulet?")
                    chatNpc(happy, "*Woof woof bark!* Elementary, it's due to the influence of the -SQUIRREL-!")
                }
                1 -> {
                    chatPlayer(happy, "Walkies!")
                    chatNpc(neutral, "...")
                }
                2 -> {
                    chatPlayer(quiz, "Can you help me with this clue?")
                    chatNpc(happy, "*Woof! Bark yip woof!* Sure! Eliminate the impossible first.")
                    chatPlayer(quiz, "And then?")
                    chatNpc(neutral, "*Bark! Woof bark bark.* Whatever is left, however improbable, must be the answer.")
                    chatPlayer(neutral, "So helpful.")
                }
                3 -> {
                    chatPlayer(shifty, "I wonder if I could sell you to a vampyre to track down dinner.")
                    chatNpc(angry, "*Woof bark bark woof* I have teeth too you know, that joke was not funny.")
                }
                else -> {
                    chatPlayer(happy, "Hey boy, what's up?")
                    chatNpc(neutral, "*Woof! Bark bark woof!* You smell funny.")
                    chatPlayer(confused, "Err... funny strange or funny ha ha?")
                    chatNpc(neutral, "*Bark bark woof!* You aren't funny.")
                }
            }
        }

    private fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        if (obj.name.contains("bones", ignoreCase = true)) {
            mes("You give the dog the bone - it eats it.")
        }
    }

    private companion object {
        const val NPC = "npc.bloodhoundpet"
    }
}
