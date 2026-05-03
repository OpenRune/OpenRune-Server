package org.rsmod.content.generic.npcs.sheep

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiContentTimer
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpContentNpc1
import org.rsmod.api.script.onOpContentNpcU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Sheep
@Inject
constructor(
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onAiContentTimer("content.sheep") { npc.sheepTimer() }
        onOpContentNpc1("content.sheep") { shearSheep(it.npc) }
        onOpContentNpcU("content.sheep", "obj.shears") { shearSheep(it.npc) }

        onAiContentTimer("content.sheared_sheep") { npc.sheepTimer() }
        onNpcQueue("content.sheared_sheep", "queue.generic_queue1") { queueTransmogReset() }
    }

    private fun Npc.sheepTimer() {
        val next = random.of(15..34)
        aiTimer(next)

        if (random.randomBoolean(4)) {
            sayFlavourText()
        }
    }

    private fun Npc.sayFlavourText() {
        worldRepo.soundArea(coords, "synth.sheep_atmospheric1")
        say("Baa!")
    }

    private suspend fun ProtectedAccess.shearSheep(npc: Npc) {
        if ("obj.shears" !in inv) {
            mes("You need a set of shears to do this.")
            return
        }
        val sheared = npcParam(npc, params.next_npc_stage)
        anim("seq.human_shearing")
        soundSynth("synth.shear_sheep", delay = 10)
        faceEntitySquare(npc)
        delay(1)
        npcPlayerFaceClose(npc)
        delay(1)
        npcChangeType(npc, sheared, duration = 50)
        mes("You get some wool.")
        invAddOrDrop(objRepo, "obj.wool")
        npc.queue("queue.generic_queue1", cycles = 1)
    }

    private fun StandardNpcAccess.queueTransmogReset() {
        resetMode()
        npc.sayFlavourText()
    }
}
