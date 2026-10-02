package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.script.onApNpc4
import org.rsmod.content.raids.toa.raid.encounter.onRoomAiTimer
import org.rsmod.content.raids.toa.raid.encounter.onRoomApNpc1
import org.rsmod.content.raids.toa.raid.encounter.onRoomApNpc3
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcHit
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcQueue
import org.rsmod.content.raids.toa.raid.encounter.onRoomOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ZebakScript : PluginScript() {
    override fun ScriptContext.startup() {
        ZebakJugs.registerAttackOp()
        for (name in listOf(ZebakNpcs.ZEBAK, ZebakNpcs.ZEBAK_ENRAGED)) {
            onRoomNpcHit<ZebakEncounter>(name) { npc, hit -> zebakHit(npc, hit) }
            onRoomNpcQueue<ZebakEncounter>(name, DEATH) { if (it === zebak) complete() }
        }

        for (name in listOf(ZebakNpcs.BLOOD_CLOUD, ZebakNpcs.BLOOD_CLOUD_SMALL)) {
            onRoomAiTimer<ZebakEncounter>(name) { bloodMagic.cloudTick(it) }
            onRoomNpcQueue<ZebakEncounter>(name, DEATH) { bloodMagic.removeCloud(it) }
        }

        onRoomApNpc1<ZebakEncounter>(ZebakNpcs.JUG) { room, jug ->
            if (isWithinApRange(jug, 1)) room.jugs.move(player, jug, push = true)
        }
        onRoomApNpc3<ZebakEncounter>(ZebakNpcs.JUG) { room, jug ->
            if (isWithinApRange(jug, 1)) room.jugs.move(player, jug, push = false)
        }
        onApNpc4(ZebakNpcs.JUG) { opNpc2(it.npc) }
        for (name in listOf(ZebakNpcs.JUG, ZebakNpcs.JUG_ROLLING)) {
            onRoomAiTimer<ZebakEncounter>(name) { jugs.tick(it) }
            onRoomNpcHit<ZebakEncounter>(name) { jug, _ -> jugs.hit(jug) }
            onRoomNpcQueue<ZebakEncounter>(name, DEATH) { jugs.hit(it) }
        }

        onRoomNpcQueue<ZebakEncounter>(ZebakNpcs.BOULDER, DEATH) { boulders.remove(it) }

        onRoomAiTimer<ZebakEncounter>(ZebakNpcs.WAVE) { waves.tick(it) }
        onRoomAiTimer<ZebakEncounter>(ZebakNpcs.WAVE_BLOODY) { waves.tick(it) }
        onRoomAiTimer<ZebakEncounter>(ZebakNpcs.WATER_CROC) { water.crocodileTick(it) }
        onRoomOpLoc1<ZebakEncounter>(ZebakLocs.CLIMBING_ROCK) { room, rock ->
            room.water.climbOut(player, rock.coords, rock.angle.id)
        }
    }

    private companion object {
        const val DEATH = "queue.death"
    }
}
