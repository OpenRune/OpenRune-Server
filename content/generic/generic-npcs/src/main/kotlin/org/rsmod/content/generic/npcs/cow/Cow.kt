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

class Cow
@Inject
constructor(private val worldRepo: WorldRepository, private val random: GameRandom) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onAiContentTimer("content.cow") { npc.cowTimer() }
        onOpContentNpcU("content.cow") { mes("The cow doesn't want that.") }
        onOpContentNpcU("content.cow", "obj.bucket_empty") {
            mes("Only dairy cows are suitable for milking.")
        }
    }

    private fun Npc.cowTimer() {
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
