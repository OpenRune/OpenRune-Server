package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcHit
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AkkhaScript : PluginScript() {
    override fun ScriptContext.startup() {
        val forms = listOf(AkkhaNpcs.MELEE, AkkhaNpcs.RANGE, AkkhaNpcs.MAGE)
        for (name in forms) {
            onRoomNpcHit<AkkhaEncounter>(name) { npc, hit -> akkhaHit(npc, hit) }
            onRoomNpcQueue<AkkhaEncounter>(name, DEATH) { akkhaDeath(it) }
        }

        val finalForms = listOf(AkkhaNpcs.ENRAGE_SPAWN, AkkhaNpcs.ENRAGE_INITIAL, AkkhaNpcs.ENRAGE)
        for (name in finalForms) {
            onModifyNpcHit(npcType(name)) {
                ToaRooms.of<AkkhaEncounter>(npc)?.modifyFinalHit(npc, hit)
            }
        }
        onRoomNpcHit<AkkhaEncounter>(AkkhaNpcs.ENRAGE) { npc, hit -> finalHit(npc, hit) }
        onRoomNpcQueue<AkkhaEncounter>(AkkhaNpcs.ENRAGE, DEATH) { akkhaDeath(it) }

        onModifyNpcHit(npcType(AkkhaNpcs.SHADOW_ENRAGE)) { hit.damage = 0 }
        onModifyNpcHit(npcType(AkkhaNpcs.SHADOW)) {
            ToaRooms.of<AkkhaEncounter>(npc)?.modifyShadowHit(npc, hit)
        }
        onRoomNpcQueue<AkkhaEncounter>(AkkhaNpcs.SHADOW, DEATH) { shadowDeath(it) }
    }

    private companion object {
        const val DEATH = "queue.death"
    }
}
