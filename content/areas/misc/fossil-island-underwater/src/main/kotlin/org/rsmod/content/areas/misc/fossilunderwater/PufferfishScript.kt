package org.rsmod.content.areas.misc.fossilunderwater

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The sea floor's pufferfish: caught into the pack and squeezed for a breath of air. */
class PufferfishScript @Inject constructor(private val npcRepo: NpcRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        // TODO(content): charging the merfolk trident with pufferfish, up to 10 charges.
        onOpNpc1(PUFFERFISH_NPC) { catch(it.npc) }
        onOpHeld1(PUFFERFISH) { depuff(it.slot) }
    }

    private fun ProtectedAccess.catch(npc: Npc) {
        if (!npc.isSlotAssigned) {
            return
        }
        if (invAdd(inv, PUFFERFISH, 1).failure) {
            // UNVERIFIED: live's answer to a full pack.
            mes("You don't have enough inventory space.")
            return
        }
        spam("You catch the pufferfish and stuff it in your pack.")
        // UNVERIFIED: how long a caught pufferfish takes to come back.
        npcRepo.del(npc, RESPAWN_TICKS)
    }

    private fun ProtectedAccess.depuff(slot: Int) {
        if (invDel(inv, PUFFERFISH, 1, slot).failure) {
            return
        }
        player.oxygen = Breath.restore(player.oxygen, Breath.PUFFERFISH)
        mes("You depuff the pufferfish, restoring some of your puff.")
        anim(INHALE)
        // UNVERIFIED: how long the emptied pufferfish swims about.
        npcRepo.add(Npc(DEPUFFED, coords), DEPUFFED_TICKS)
    }

    private companion object {
        const val PUFFERFISH_NPC = "npc.fossil_puffer_fish_model"
        const val PUFFERFISH = "obj.fossil_puffer_fish"
        const val DEPUFFED = "npc.fossil_puffer_fish_depuffed"
        const val INHALE = "seq.human_inhale_pufferfish"
        const val RESPAWN_TICKS = 50
        const val DEPUFFED_TICKS = 20
    }
}
