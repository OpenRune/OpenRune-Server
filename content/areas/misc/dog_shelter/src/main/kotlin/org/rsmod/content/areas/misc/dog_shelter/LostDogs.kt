package org.rsmod.content.areas.misc.dog_shelter

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.other.pets.dogs.DogBreed
import org.rsmod.content.other.pets.dogs.Dogs
import org.rsmod.content.other.pets.onPetOpIfPresent
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LostDogs : PluginScript() {
    override fun ScriptContext.startup() {
        for (breed in Dogs.breeds) {
            if (breed.unlock == null) {
                continue
            }
            onPetOpIfPresent("npc.${breed.key}_wander", RESCUE_OP) { rescue(it, breed) }
        }
    }

    private suspend fun ProtectedAccess.rescue(npc: Npc, breed: DogBreed) {
        if (!QuestRequirements.hasCompleted(player, QUEST)) {
            mes("You have nowhere to send this lost dog...")
            return
        }
        startDialogue(npc) {
            chatPlayer(happy, "Hey there, I can take you somewhere safe if you'd like?")
            chatNpc(happy, "Arf arf!")
            mesbox("The ${breed.name} heads to the dog shelter.")
        }
        VarPlayerIntMapSetter.set(player, breed.unlock!!, 1)
    }

    private companion object {
        const val RESCUE_OP = "Rescue"
        const val QUEST = "quest_ruffsituation"
    }
}
