package org.rsmod.content.areas.misc.goblinvillage.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Wartface and Bentnoze bicker with each other regardless of which one was spoken to, so every line
 * names its own speaker rather than relying on the clicked npc.
 */
class GoblinGenerals : PluginScript() {
    override fun ScriptContext.startup() {
        for (general in GENERALS) {
            onOpNpc1(general) { startDialogue(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        val pick = random.of(maxExclusive = CONVERSATIONS)
        startDialogue(npc) {
            when (pick) {
                0 -> shutUp()
                1 -> armourColour()
                2 -> betterWeapons()
                else -> dinner()
            }
        }
    }

    private suspend fun Dialogue.shutUp() {
        bentnoze(angry, "Shut up Grubfoot!")
        grubfoot(sad, "But me not say anything!")
        wartface(angry, "Shut up Grubfoot!")
        bentnoze(angry, "Shut up Wartface!")
        wartface(angry, "Shut up Bentnoze!")
        grubfoot(angry, "Shut up generals!")
        bentnoze(angry, "Shut up Grubfoot!")
        wartface(angry, "Shut up Grubfoot!")
        chatPlayer(angry, "Shut up goblins!")
    }

    private suspend fun Dialogue.armourColour() {
        bentnoze(quiz, "What we do now Wartface?")
        wartface(confused, "Me dunno... maybe we change armour colour again.")
        bentnoze(quiz, "Human, what colour you think best?")
        chatPlayer(quiz, "What about brown?")
        wartface(neutral, "Yeah brown is nice.")
        bentnoze(happy, "Thanks human.")
    }

    private suspend fun Dialogue.betterWeapons() {
        bentnoze(neutral, "Our soldiers need better weapons. We give them all warhammers!")
        wartface(angry, "No no give them battleaxes!")
        bentnoze(angry, "Axes stupid! They things you cut wood with!")
        wartface(angry, "But hammers things you put nails in wood with! At least axes sharp!")
        bentnoze(angry, "You not need sharp when you have strong goblin swinging big lump of iron!")
        chatPlayer(neutral, "Do you want me to...")
        wartface(angry, "Shut up human, we sort this one out ourselves!")
    }

    private suspend fun Dialogue.dinner() {
        wartface(neutral, "Me tell Mudknuckles make worm stew for dinner.")
        bentnoze(neutral, "Me not like worm stew! Tell him make beetle pie!")
        wartface(happy, "Beetle pie give you wind! You smell bad enough already!")
    }

    private suspend fun Dialogue.wartface(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("General Wartface", WARTFACE, mesanim, text)

    private suspend fun Dialogue.bentnoze(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("General Bentnoze", BENTNOZE, mesanim, text)

    private suspend fun Dialogue.grubfoot(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Grubfoot", GRUBFOOT, mesanim, text)

    private companion object {
        private const val CONVERSATIONS = 4
        private const val WARTFACE = "npc.general_wartface_green"
        private const val BENTNOZE = "npc.general_bentnoze_red"
        private const val GRUBFOOT = "npc.catwalk_goblin_brown"

        private val GENERALS = listOf(BENTNOZE, WARTFACE)
    }
}
