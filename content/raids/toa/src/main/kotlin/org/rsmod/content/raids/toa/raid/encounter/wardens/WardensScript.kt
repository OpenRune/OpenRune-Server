package org.rsmod.content.raids.toa.raid.encounter.wardens

import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcHit
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WardensScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(WardensEncounter.OSMUMTEN) {
            mes("Osmumten is ready. Select 'Begin' to start the challenge.")
        }
        onOpNpc3(WardensEncounter.OSMUMTEN) {
            ToaRooms.of<WardensEncounter>(player)?.beginChallenge(player)
        }

        onModifyNpcHit(npcType(WardensEncounter.OBELISK_P1)) {
            ToaRooms.of<WardensEncounter>(npc)?.modifyObeliskHit(npc, hit)
        }
        onModifyNpcHit(npcType(WardensEncounter.OBELISK_P2)) {
            ToaRooms.of<WardensEncounter>(npc)?.modifyObeliskHit(npc, hit)
        }
        onRoomNpcHit<WardensEncounter>(WardensEncounter.OBELISK_P1) { npc, hit ->
            obeliskHit(npc, hit)
        }

        for (god in WardenGod.entries) {
            for (form in god.phaseTwoForms) {
                onRoomNpcHit<WardensEncounter>(form) { npc, hit -> wardenHit(npc, hit) }
            }
            onRoomNpcHit<WardensSecondEncounter>(god.phaseThree) { npc, hit -> wardenHit(npc, hit) }
            onModifyNpcHit(npcType(god.core)) { if (hit.damage == 0) hit.damage = 1 }
            onRoomNpcHit<WardensEncounter>(god.core) { npc, hit -> core.mirror(npc, hit) }
        }
    }
}
