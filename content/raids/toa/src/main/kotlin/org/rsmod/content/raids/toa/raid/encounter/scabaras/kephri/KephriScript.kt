package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcHit
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KephriScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (name in listOf(KephriNpcs.SHIELDED, KephriNpcs.WEAK, KephriNpcs.ENRAGE)) {
            onRoomNpcHit<KephriEncounter>(name) { npc, _ -> kephriHit(npc) }
            onRoomNpcQueue<KephriEncounter>(name, DEATH) { kephriDeath(it) }
        }

        onModifyNpcHit(npcType(KephriNpcs.SHIELD_SCARAB)) {
            if (hit.isFromPlayer) hit.damage = npc.hitpoints
        }
        for (egg in listOf(KephriNpcs.EGG_EXPLODE, KephriNpcs.EGG_HATCH)) {
            onModifyNpcHit(npcType(egg)) { if (hit.isFromPlayer) hit.damage = npc.hitpoints }
            onRoomNpcQueue<KephriEncounter>(egg, DEATH) { eggs.killed(it) }
        }
        onRoomNpcHit<KephriEncounter>(KephriNpcs.GUARDIAN_MAGE) { npc, hit ->
            guardians.arcaneHit(npc, hit)
        }
    }

    private companion object {
        const val DEATH = "queue.death"
    }
}
