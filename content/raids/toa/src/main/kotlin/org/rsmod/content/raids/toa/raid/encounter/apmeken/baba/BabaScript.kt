package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcHit
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BabaScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (name in listOf(BabaNpcs.BABA, BabaNpcs.DIGGING, BabaNpcs.COFFIN)) {
            onRoomNpcHit<BabaEncounter>(name) { npc, hit -> babaHit(npc, hit) }
            onRoomNpcQueue<BabaEncounter>(name, DEATH) { babaDeath(it) }
        }
        onModifyNpcHit(npcType(BabaNpcs.BOULDER_WEAK)) {
            ToaRooms.of<BabaEncounter>(npc)?.crackedHit(npc, hit)
        }
        for (name in listOf(BabaNpcs.BOULDER, BabaNpcs.BOULDER_WEAK)) {
            onRoomNpcQueue<BabaEncounter>(name, DEATH) { boulders.died(it) }
        }
        onRoomNpcQueue<BabaEncounter>(BabaNpcs.RUBBLE, DEATH) { rockfall.died(it) }
        onRoomNpcQueue<BabaEncounter>(BabaNpcs.BABOON, DEATH) { baboons.killed(it) }
        onRoomNpcQueue<BabaEncounter>(BabaNpcs.SARCOPHAGUS, DEATH) { baboons.sarcophagusDied(it) }
    }

    private companion object {
        const val DEATH = "queue.death"
    }
}
