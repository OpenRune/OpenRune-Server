package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.Pets
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GullDialogue @Inject constructor(private val followers: PetFollowers) : PluginScript() {
    private var cachedRawFish: Set<String>? = null

    override fun ScriptContext.startup() {
        onPetOp(GULL, "Talk-to") { talkGull(it) }
        onPetOp(GULLIVER, "Talk-to") { talkGulliver(it) }
        onPetOpU(GULL) { feedGull(it.npc, it.objType) }
        onPetOpU(GULLIVER) { feedGulliver(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talkGull(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(happy, "What a pretty bird you are!")
            chatNpc(happy, "Chirp chirp!")
        }

    private suspend fun ProtectedAccess.talkGulliver(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(
                        quiz,
                        "So what exactly are you? I can't tell if you're a bird or some kind of cat... thing.",
                    )
                    chatNpc(angry, "What kind of question is that? I'm a gryphon.")
                    chatPlayer(neutral, "You remind me of a seagull, except much bigger and with claws.")
                    chatNpc(
                        neutral,
                        "You remind me of a monkey, except well dressed and somehow even more annoying.",
                    )
                    chatPlayer(
                        neutral,
                        "Well, they say that comparison is the thief of joy, " +
                            "so how about we stop this line of thought.",
                    )
                    chatNpc(neutral, "Fine by me. Caw!")
                }
                else -> {
                    chatPlayer(happy, "What a pretty bird you are!")
                    chatNpc(angry, "If you talk to me like that again, you'll promptly find yourself down one finger.")
                    chatPlayer(shocked, "Wow, you can talk! Can all gryphons talk? Are you like parrots?")
                    chatNpc(neutral, "I haven't met every gryphon. You took me from the nest remember?")
                    chatPlayer(neutral, "Oh, right.")
                    chatNpc(happy, "I'd rather enjoy the opportunity to meet a parrot. It sounds tasty.")
                }
            }
        }

    private suspend fun ProtectedAccess.feedGull(npc: Npc, obj: ItemServerType) {
        if (obj.internalName !in rawFish()) {
            startDialogue(npc) {
                mesbox("Gull refuses the item.")
                chatNpc(angry, "Caw!")
            }
            return
        }
        invDel(inv, obj.internalName, 1)
        val growing = followers.isFollowerOf(npc, player) && player.vars[GULLIVER_UNLOCKED] == 0
        val fed = if (growing) player.vars[FISH_FED] + 1 else 0
        if (fed in 1 until FISH_TO_GROW) {
            VarPlayerIntMapSetter.set(player, FISH_FED, fed)
        }
        startDialogue(npc) {
            mesbox("Gull gobbles up the ${obj.name.lowercase()}.")
            chatNpc(happy, "Chirp! chirp!")
            if (fed >= FISH_TO_GROW) {
                mesbox("Your pet grows up before your very eyes!")
                chatNpc(happy, "Caw! That hit the spot.")
            }
        }
        if (fed >= FISH_TO_GROW) {
            VarPlayerIntMapSetter.set(player, GULLIVER_UNLOCKED, 1)
            followers.spawn(player, Pets.forObj(GULLIVER_OBJ)!!.second)
        }
    }

    private suspend fun ProtectedAccess.feedGulliver(npc: Npc, obj: ItemServerType) {
        if (obj.internalName !in rawFish()) {
            startDialogue(npc) { chatNpc(angry, "Caw!") }
            return
        }
        invDel(inv, obj.internalName, 1)
        startDialogue(npc) {
            mesbox("Gulliver gobbles up the ${obj.name.lowercase()}.")
            when (access.random.of(2)) {
                0 -> chatNpc(happy, "How delightful.")
                else -> chatNpc(happy, "Mine!")
            }
        }
    }

    private fun rawFish(): Set<String> =
        cachedRawFish ?: FishingSpotRow.all()
            .mapTo(hashSetOf(RAW_KARAMBWAN)) { it.fish.internalName }.also { cachedRawFish = it }

    private companion object {
        const val GULL = "npc.gryphonboss_pet"
        const val GULLIVER = "npc.gryphonboss_pet_adult"
        const val RAW_KARAMBWAN = "obj.tbwt_raw_karambwan"
        const val GULLIVER_OBJ = "obj.gryphonbosspet_adult"
        const val FISH_FED = "varbit.pet_gull_fish_fed"
        const val GULLIVER_UNLOCKED = "varbit.pet_gull_gulliver"
        const val FISH_TO_GROW = 50
    }
}
