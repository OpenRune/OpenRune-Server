package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onApNpc4
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ZebakScript : PluginScript() {
    override fun ScriptContext.startup() {
        ZebakJugs.registerAttackOp()
        for (name in listOf(ZebakNpcs.ZEBAK, ZebakNpcs.ZEBAK_ENRAGED)) {
            val type = npcType(name)
            onNpcHit(type) { ZebakEncounter.onZebakHit(npc, hit) }
            onNpcQueue(type, "queue.death") { ZebakEncounter.onZebakDeath(npc) }
        }

        for (name in listOf(ZebakNpcs.BLOOD_CLOUD, ZebakNpcs.BLOOD_CLOUD_SMALL)) {
            onAiTimer(name) { ZebakEncounter.onCloudTick(npc) }
            onNpcQueue(npcType(name), "queue.death") { ZebakEncounter.onCloudDeath(npc) }
        }

        onApNpc1(ZebakNpcs.JUG) {
            if (isWithinApRange(it.npc, 1)) ZebakEncounter.onJugMoved(player, it.npc, push = true)
        }
        onApNpc3(ZebakNpcs.JUG) {
            if (isWithinApRange(it.npc, 1)) ZebakEncounter.onJugMoved(player, it.npc, push = false)
        }
        onApNpc4(ZebakNpcs.JUG) { opNpc2(it.npc) }
        for (name in listOf(ZebakNpcs.JUG, ZebakNpcs.JUG_ROLLING)) {
            val type = npcType(name)
            onAiTimer(name) { ZebakEncounter.onJugTick(npc) }
            onNpcHit(type) { ZebakEncounter.onJugBroken(npc) }
            onNpcQueue(type, "queue.death") { ZebakEncounter.onJugBroken(npc) }
        }

        onNpcQueue(npcType(ZebakNpcs.BOULDER), "queue.death") { ZebakEncounter.onBoulderDeath(npc) }

        onAiTimer(ZebakNpcs.WAVE) { ZebakEncounter.onWaveTick(npc) }
        onAiTimer(ZebakNpcs.WAVE_BLOODY) { ZebakEncounter.onWaveTick(npc) }
        onAiTimer(ZebakNpcs.WATER_CROC) { ZebakEncounter.onCrocTick(npc) }
        onOpLoc1(ZebakLocs.CLIMBING_ROCK) {
            ZebakEncounter.onClimbRock(player, it.loc.coords, it.loc.angle.id)
        }
    }
}
