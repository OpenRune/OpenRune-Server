package org.rsmod.content.areas.misc.ver_sinhaza

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.dogs.DogCare
import org.rsmod.content.other.pets.dogs.DogForm
import org.rsmod.content.other.pets.dogs.Dogs
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MysteriousStranger
@Inject
constructor(private val care: DogCare, private val followers: PetFollowers) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(STRANGER) { talk(it.npc) }
        onOpNpcU(STRANGER) { useItem(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            val bloodMoon = QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)
            if (!player.strangerIntro) {
                player.strangerIntro = true
                chatNpc(neutral, "Greetings, adventurer. How can I be of service?")
                chatPlayer(quiz, "I'm not sure. What type of services do you provide?")
                if (bloodMoon) {
                    chatNpc(neutral, "Father Sugadinti is in need of... specimens. I'll happily take any grown dogs off your hands.")
                    chatPlayer(quiz, "What does he use them for?")
                } else {
                    chatNpc(neutral, "I have a client in need of... specimens. I'll happily take any grown dogs off your hands.")
                    chatPlayer(quiz, "What does your client use them for?")
                }
                chatNpc(neutral, "That is not information I am privy to.")
                chatPlayer(quiz, "What's in it for me?")
                chatNpc(neutral, "I can compensate you. A modest sum of blood runes, in exchange for each dog you bring me.")
                chatNpc(quiz, "Would you like to make an exchange?")
            } else {
                chatNpc(quiz, "You've come once again, adventurer. Would you like to make an exchange?")
            }
            val dog = dogWithPlayer(player)
            if (dog == null) {
                chatPlayer(sad, "I'm afraid I don't have a dog to offer you.")
                if (bloodMoon) {
                    chatNpc(neutral, "Please, return when you do. Father Sugadinti is always in need of more specimens.")
                } else {
                    chatNpc(neutral, "Please, return when you do. My client is always in need of more specimens.")
                }
                return@startDialogue
            }
            chatPlayer(happy, "I have a dog if you're interested.")
            offer(dog)
        }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        val dog = Dogs.forObj(obj.id)
        startDialogue(npc) {
            when {
                dog != null && !dog.puppy -> offer(dog)
                dog != null -> chatNpc(neutral, "I'm afraid I cannot take them so young. They need to be fully grown for Father Sugadinti.")
                obj.internalName == HELLPUPPY ->
                    chatNpc(neutral, "That fiery one would be more effort than it is worth, although I appreciate the offer.")
                obj.internalName == BLOODHOUND ->
                    chatNpc(
                        neutral,
                        "That is a rare breed. For the deal to be fair, you would need to be compensated more blood " +
                            "runes than I can offer.",
                    )
                else -> chatNpc(neutral, "I'm afraid my client is only interested in canines, although I appreciate the offer.")
            }
        }
    }

    private suspend fun Dialogue.offer(dog: DogForm) {
        val name = dog.breed.name.lowercase()
        val runes = if (player.morytaniaMediumDiary) DIARY_RUNES else RUNES
        objbox(dog.obj, "You show the stranger your $name.")
        chatNpc(happy, "An excellent specimen. I can offer you $runes blood runes in exchange for your $name?")
        objbox(dog.obj, "Exchange your $name for $runes blood runes?")
        val accept = choice2("No.", false, "Yes.", true)
        if (!accept) {
            chatPlayer(neutral, "No, thank you. I won't be selling any dogs today.")
            chatNpc(neutral, "The offer is always open.")
            return
        }
        chatPlayer(happy, "I'll accept your offer.")
        if (!access.takeDog(dog)) {
            return
        }
        access.invAdd(access.inv, BLOOD_RUNE, runes)
        objbox(dog.obj, "You exchange your $name for $runes blood runes.")
        chatNpc(
            happy,
            "Pleasure doing business, adventurer. Please, return if you have more to offer. Father Sugadinti will " +
                "always accept such fine specimens.",
        )
    }

    private fun ProtectedAccess.takeDog(dog: DogForm): Boolean {
        if (dog.obj in inv) {
            return invDel(inv, dog.obj, 1).success
        }
        if (care.following(player) === dog) {
            followers.dismiss(player)
            return true
        }
        return false
    }

    private fun dogWithPlayer(player: Player): DogForm? {
        val following = care.following(player)
        if (following != null && !following.puppy) {
            return following
        }
        return Dogs.all.firstOrNull { !it.puppy && it.obj in player.inv }
    }

    private companion object {
        const val STRANGER = "npc.tob_dog_stranger"
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val BLOOD_RUNE = "obj.bloodrune"
        const val HELLPUPPY = "obj.hell_pet"
        const val BLOODHOUND = "obj.bloodhound_pet"
        const val RUNES = 100
        const val DIARY_RUNES = 200

        var Player.strangerIntro: Boolean by boolVarBit("varbit.dogq_stranger_intro")
        val Player.morytaniaMediumDiary: Boolean by boolVarBit("varbit.morytania_diary_medium_complete")
    }
}
