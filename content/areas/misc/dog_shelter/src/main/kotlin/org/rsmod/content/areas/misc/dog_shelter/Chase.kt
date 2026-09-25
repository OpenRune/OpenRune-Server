package org.rsmod.content.areas.misc.dog_shelter

import jakarta.inject.Inject
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.other.pets.dogs.DogCare
import org.rsmod.content.other.pets.dogs.DogForm
import org.rsmod.content.other.pets.dogs.Dogs
import org.rsmod.content.other.pets.onPetOpIfPresent
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Chase @Inject constructor(private val care: DogCare) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CHASE) { talk(it.npc) }
        onPetOpIfPresent(CHASE, ADOPT_OP) { adopt(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            if (CAT_EARS in player.worn) {
                val show = choice2("Yes.", true, "No.", false, title = "Show Chase your cat ears?")
                if (show) {
                    chatPlayer(happy, "Meow-llo. Do you adopt cats?")
                    chatNpc(laugh, "Haha, afraid not, adventurer.")
                    return@startDialogue
                }
            }
            if (!questComplete(player)) {
                chatPlayer(quiz, "Do you have any dogs up for adoption?")
                chatNpc(neutral, "Best to speak with Talia.")
                return@startDialogue
            }
            chatPlayer(quiz, "Hello there, Chase. Could I adopt a puppy?")
            chatNpc(neutral, "If you want to.")
            adoption()
        }

    private suspend fun ProtectedAccess.adopt(npc: Npc) {
        if (!questComplete(player)) {
            startDialogue(npc) {
                chatPlayer(quiz, "Do you have any dogs up for adoption?")
                chatNpc(neutral, "Best to speak with Talia.")
            }
            return
        }
        startDialogue(npc) { adoption() }
    }

    private suspend fun Dialogue.adoption() {
        if (care.ownsDog(player, puppy = true)) {
            mesbox("You can only look after one puppy at a time.")
            return
        }
        if (care.ownsDog(player, puppy = false)) {
            mesbox("You'll need to find a home for your grown dog before you can adopt another puppy.")
            return
        }
        val breeds = Dogs.breeds.filter { it.unlocked(player) }
        val breedIndex = access.menu(ADOPTION_TITLE, *breeds.map { it.name }.toTypedArray())
        val breed = breeds.getOrNull(breedIndex) ?: return
        val colours = Dogs.colours(breed)
        val colourIndex = access.menu(ADOPTION_TITLE, *colours.toTypedArray())
        val colour = colours.getOrNull(colourIndex) ?: return
        val puppy = Dogs.of(breed, colour, puppy = true)
        chatNpc(neutral, "That'll be $PRICE coins.")
        if (access.invTotal(access.inv, COINS) < PRICE) {
            chatPlayer(sad, "I don't have $PRICE coins...")
            chatNpc(neutral, "Come back when you do then.")
            return
        }
        objbox(puppy.obj, "Pay $PRICE Coins?")
        val pay = choice2("No.", false, "Yes.", true)
        if (!pay) {
            return
        }
        if (access.inv.isFull()) {
            access.mes("You don't have enough inventory space.")
            return
        }
        if (!player.invTakeFee(PRICE)) {
            return
        }
        give(puppy)
        objbox(puppy.obj, "Chase hands you the ${breed.name} puppy.")
    }

    private fun Dialogue.give(puppy: DogForm) {
        access.invAdd(access.inv, puppy.obj, 1)
        care.resetPuppy(player)
    }

    private fun questComplete(player: Player): Boolean = QuestRequirements.hasCompleted(player, QUEST)

    private companion object {
        const val CHASE = "npc.dogq_chase_post"
        const val ADOPT_OP = "Adopt"
        const val QUEST = "quest_ruffsituation"
        const val CAT_EARS = "obj.osb7_cat_ears"
        const val COINS = "obj.coins"
        const val PRICE = 200
        const val ADOPTION_TITLE = "Puppy Adoption"
    }
}
