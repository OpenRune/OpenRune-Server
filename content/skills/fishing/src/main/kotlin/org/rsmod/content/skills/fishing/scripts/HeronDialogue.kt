package org.rsmod.content.skills.fishing.scripts

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.content.skills.fishing.HeronPet
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HeronDialogue @Inject constructor() : PluginScript() {
    private var cachedCookedFish: Set<String>? = null
    private var cachedRawFish: Set<String>? = null

    override fun ScriptContext.startup() {
        onOpNpc1(HERON_NPC) { talkToHeron(it.npc) }
        onOpNpc1(GREAT_BLUE_NPC) { talkToGreatBlue(it.npc) }
        onOpNpc3(HERON_NPC) { becomeGreatBlue(it.npc) }
        onOpNpc3(GREAT_BLUE_NPC) { becomeHeron(it.npc) }
        onOpNpcU(GREAT_BLUE_NPC) { refuseOffering(it.npc, it.objType) }
    }

    /** The great blue heron only eats raw fish, and is vocal about everything else. */
    private suspend fun ProtectedAccess.refuseOffering(npc: Npc, offering: ItemServerType) {
        val name = offering.internalName
        when {
            name == EEL_SUSHI ->
                startDialogue(npc) {
                    chatNpc(angry, "Sushi? Do I look like a commoner to you?")
                    chatPlayer(neutral, "In Dorgeshuun culture, this is considered a delicacy.")
                    chatNpc(angry, "In bird culture, this is considered an insult. Get it out of my face at once!")
                    chatPlayer(sad, "How could a member of my own house say something so horrible?")
                }
            offering.name.contains("burnt") ->
                startDialogue(npc) {
                    chatNpc(angry, "What is wrong with you?")
                    chatPlayer(neutral, "I'm giving you some fish.")
                    chatNpc(angry, "And you think that I can do anything with something charred to the bone?")
                    chatPlayer(neutral, "In my culture, these well-cooked fish are considered pretty valuable.")
                    chatNpc(angry, "It's burnt, you dancing donkey!")
                    chatPlayer(quiz, "Dancing donkey...?")
                }
            name in cookedFish() ->
                startDialogue(npc) {
                    chatNpc(angry, "Are you trying to annoy me?")
                    chatPlayer(neutral, "I put in a lot of effort cooking this for you.")
                    chatNpc(angry, "Exactly, you've cooked it! You've ruined a fine delicacy, now go and uncook it!")
                    chatPlayer(quiz, "Uncook? That's not even possible!")
                    chatNpc(
                        neutral,
                        "I once knew a cat who mastered uncooking. Nice guy, actually. " +
                            "Enough talk, go and get me some more raw fish!",
                    )
                }
            name in rawFish() -> becomeHeron(npc)
        }
    }

    private fun cookedFish(): Set<String> =
        cachedCookedFish
            ?: CookingFoodsRow.all()
                .mapTo(HashSet()) { it.output.internalName }
                .also { cachedCookedFish = it }

    private suspend fun ProtectedAccess.talkToHeron(npc: Npc): Unit =
        startDialogue(npc) {
            chatNpc(neutral, "Hop inside my mouth if you want to live!")
            chatPlayer(neutral, "I'm not falling for that... I'm not a fish! I've got more foresight than that.")
        }

    private suspend fun ProtectedAccess.talkToGreatBlue(npc: Npc): Unit =
        startDialogue(npc) {
            chatNpc(quiz, "Got any raw fish?")
            chatPlayer(quiz, "Haven't you already eaten enough Spirit flakes?")
            chatNpc(neutral, "Yes, I have. That's why I'm asking you for raw fish.")
            chatPlayer(neutral, "Sometimes I wonder whether getting stuck with you was my good luck or yours.")
        }

    private suspend fun ProtectedAccess.becomeGreatBlue(npc: Npc) {
        val flakes = invTotal(inv, SPIRIT_FLAKES)
        when {
            flakes == 0 ->
                startDialogue(npc) {
                    chatNpc(
                        angry,
                        "What's the big idea? If you're going to offer me a " +
                            "Spirit flake, at least have one to hand!",
                    )
                }
            flakes < FLAKES_REQUIRED ->
                startDialogue(npc) {
                    chatNpc(
                        neutral,
                        "Those look tasty. But I'm looking for a proper feast. " +
                            "Come back to me with $FLAKES_REQUIRED of them.",
                    )
                    chatPlayer(shocked, "$FLAKES_REQUIRED!? Are you insane?!")
                    chatNpc(neutral, "Not really, I'm just a little bit peckish.")
                    chatPlayer(happy, "Good thing you have a beak then!")
                    chatNpc(neutral, "Touché.")
                }
            else -> {
                invDel(inv, SPIRIT_FLAKES, FLAKES_REQUIRED)
                invReplace(inv, HeronPet.PET_OBJ, 1, HeronPet.GREAT_BLUE_OBJ)
                startDialogue(npc) {
                    chatNpc(
                        happy,
                        "That really hit the spot! Though I feel a little blue " +
                            "now that there are none left.",
                    )
                }
            }
        }
    }

    private suspend fun ProtectedAccess.becomeHeron(npc: Npc) {
        val fish = rawFishInInventory()
        if (fish == null) {
            if (invTotal(inv, SPIRIT_FLAKES) > 0) {
                startDialogue(npc) {
                    chatPlayer(
                        quiz,
                        "Oops! That was a mistake, you're already blue. " +
                            "If you give it back, I'll give you a raw fish?",
                    )
                    chatNpc(neutral, "Sorry, no can do. They're just too tasty. You should try one sometime!")
                    chatPlayer(neutral, "I'll pass.")
                }
                return
            }
            startDialogue(npc) { chatNpc(angry, "Where's the fish? You don't have any!") }
            return
        }
        invDel(inv, fish, 1)
        invReplace(inv, HeronPet.GREAT_BLUE_OBJ, 1, HeronPet.PET_OBJ)
        startDialogue(npc) { chatNpc(happy, "Delicious. Back to white for me, then.") }
    }

    private fun ProtectedAccess.rawFishInInventory(): String? =
        rawFish().firstOrNull { invTotal(inv, it) > 0 }

    private fun rawFish(): Set<String> =
        cachedRawFish ?: FishingSpotRow.all()
            .mapTo(hashSetOf(RAW_KARAMBWAN)) { it.fish.internalName }.also { cachedRawFish = it }

    private companion object {
        private const val HERON_NPC = "npc.skillpet_fish"
        private const val GREAT_BLUE_NPC = "npc.skillpet_fish_tempoross"
        private const val SPIRIT_FLAKES = "obj.spirit_flakes"
        private const val FLAKES_REQUIRED = 3000
        private const val EEL_SUSHI = "obj.dorgesh_cave_eel_sushi"
        private const val RAW_KARAMBWAN = "obj.tbwt_raw_karambwan"
    }
}
