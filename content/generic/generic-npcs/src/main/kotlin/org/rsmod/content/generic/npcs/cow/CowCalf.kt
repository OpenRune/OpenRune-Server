package org.rsmod.content.generic.npcs.cow

import jakarta.inject.Inject
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiContentTimer
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onOpContentNpcU
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CowCalf
@Inject
constructor(private val worldRepo: WorldRepository, private val random: GameRandom) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onAiContentTimer("content.cow_calf") { npc.calfTimer() }
        onOpContentNpcU("content.cow_calf") { mes("The calf doesn't want that.") }
        onOpContentNpcU("content.cow_calf", "obj.bucket_empty") { mes("Calves are too young to be milked.") }
    }

    private fun Npc.calfTimer() {
        val next = random.of(15..34)
        aiTimer(next)

        if (random.randomBoolean(4)) {
            sayFlavourText()
        }
    }

    private fun Npc.sayFlavourText() {
        worldRepo.soundArea(coords, "synth.cow_atmospheric", radius = 10)
        say("Moo")
        anim("seq.cow_update_graze")
    }
}
