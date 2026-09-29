package org.rsmod.content.other.pets

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetEmoteScript @Inject constructor(private val followers: PetFollowers) : PluginScript() {
    override fun ScriptContext.startup() {
        for (form in Pets.all.flatMap { it.forms }.filter { it.emotes.isNotEmpty() }) {
            onPetOpIfPresent(form.npc, EMOTE_OP) { emote(it, form.emotes) }
        }
        onPetOpIfPresent(BEEF, INTERACT_OP) { interactBeef(it) }
    }

    private fun ProtectedAccess.emote(npc: Npc, seqs: List<String>) {
        if (!ready(npc)) {
            return
        }
        val index = player.emoteIndex % seqs.size
        npc.anim(seqs[index])
        player.emoteIndex = (index + 1) % seqs.size
    }

    private suspend fun ProtectedAccess.interactBeef(npc: Npc) {
        var option = 0
        startDialogue(npc) { option = choice2("Moo.", 1, "Emote", 2) }
        when (option) {
            1 -> npc.say("Moo.")
            2 -> {
                if (!ready(npc)) {
                    return
                }
                npc.anim(if (random.of(BEEF_WHIRLWIND_CHANCE) == 0) BEEF_WHIRLWIND else BEEF_SNIFF)
            }
        }
    }

    private fun ProtectedAccess.ready(npc: Npc): Boolean {
        if (!followers.requireOwned(this, npc)) {
            return false
        }
        if (mapClock < player.emoteCooldown) {
            mes("You must wait a moment before your pet can do another emote.")
            return false
        }
        player.emoteCooldown = mapClock + COOLDOWN_CYCLES
        return true
    }

    private companion object {
        const val EMOTE_OP = "Emote"
        const val INTERACT_OP = "Interact"
        const val COOLDOWN_CYCLES = 5
        const val BEEF = "npc.cowboss_pet"
        const val BEEF_SNIFF = "seq.cowboss_pet_emote"
        const val BEEF_WHIRLWIND = "seq.cowboss_pet_emote_charge"
        const val BEEF_WHIRLWIND_CHANCE = 20
    }
}

private var Player.emoteCooldown: Int by intVarp("varp.pet_emote_cooldown")
private var Player.emoteIndex: Int by intVarBit("varbit.pet_emote_index")
