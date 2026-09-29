package org.rsmod.content.other.pets.dogs

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld5
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.minutesAsText
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpIfPresent
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.content.other.pets.ticksToMinutes
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DogScript
@Inject
constructor(private val followers: PetFollowers, private val care: DogCare) : PluginScript(), PlayerPostTickHook {
    override fun ScriptContext.startup() {
        for (dog in Dogs.all) {
            onOpHeld5(dog.obj) { drop(it.slot, dog) }
            onPetOp(dog.npc, PICK_UP_OP) { pickUp(it, dog) }
            onPetOpIfPresent(dog.npc, PET_OP) { pet(it, dog) }
            onPetOpIfPresent(dog.npc, INTERACT_OP) { interact(it, dog) }
            onPetOpIfPresent(dog.npc, DIG_OP) { dig(it) }
            onPetOpU(dog.npc) { useItem(it.npc, dog, it.objType) }
        }
    }

    override fun onPostTick(player: Player) {
        if (!player.loggingOut) {
            care.tick(player)
        }
    }

    private fun ProtectedAccess.drop(slot: Int, dog: DogForm) {
        if (followers.hasFollower(player)) {
            mes("You already have a follower.")
            return
        }
        if (!invDel(inv, dog.obj, count = 1, slot = slot).success) {
            return
        }
        followers.spawn(player, dog.form)
    }

    private fun ProtectedAccess.pickUp(npc: Npc, dog: DogForm) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space to pick up your follower.")
            return
        }
        followers.dismiss(player)
        invAdd(inv, dog.obj, 1)
    }

    private fun ProtectedAccess.pet(npc: Npc, dog: DogForm) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        npc.say(if (dog.puppy) "Yip!" else "Woof!")
        mes("You pet your ${dog.breed.name.lowercase()}${if (dog.puppy) " puppy" else ""}.")
    }

    private suspend fun ProtectedAccess.interact(npc: Npc, dog: DogForm) {
        if (!followers.requireOwned(this, npc) || !dog.puppy) {
            return
        }
        var option = 0
        startDialogue(npc) { option = choice2("Guess age", 1, "Cancel", 2, title = "Interact with puppy") }
        if (option == 1) {
            guessAge(npc)
        }
    }

    private suspend fun ProtectedAccess.guessAge(npc: Npc) {
        val age = care.ageTicks(player).ticksToMinutes()
        val left = care.ticksUntilAdult(player).ticksToMinutes()
        val paused = if (care.isGrowthPaused(player)) " It isn't growing at the moment because it's hungry." else ""
        startDialogue(npc) {
            mesbox(
                "Your puppy has been growing for ${age.minutesAsText()}. Approximate time until fully grown: " +
                    "${left.minutesAsText()}, assuming you keep it fed.$paused",
            )
        }
    }

    private fun ProtectedAccess.dig(npc: Npc) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        mes("Nothing interesting happens.")
    }

    private suspend fun ProtectedAccess.useItem(npc: Npc, dog: DogForm, obj: ItemServerType) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        val name = if (dog.puppy) "puppy" else "dog"
        when {
            care.isUpsetting(obj) -> mes("Feeding the ${obj.name.lowercase()} to your $name would give them an upset stomach.")
            care.isPotion(obj) -> mes("Magical potions are not famous for their nutritional substance. Try some proper food.")
            care.isFood(obj) -> {
                invDel(inv, obj.internalName, 1)
                care.feed(player)
                mes("You feed the ${obj.name.lowercase()} to your $name.")
            }
            else -> mes("Nothing interesting happens.")
        }
    }

    private companion object {
        const val PICK_UP_OP = "Pick-up"
        const val PET_OP = "Pet"
        const val INTERACT_OP = "Interact"
        const val DIG_OP = "Dig"
    }
}
