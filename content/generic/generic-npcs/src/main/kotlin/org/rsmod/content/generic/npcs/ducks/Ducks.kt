package org.rsmod.content.generic.npcs.ducks

import dev.openrune.types.NpcMode
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiContentTimer
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class Ducks
@Inject
constructor(
    private val random: GameRandom,
    private val death: NpcDeath,
    private val search: NpcSearch,
    private val worldRepo: WorldRepository,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onAiContentTimer("content.duck") { npc.duckTimer() }
        onAiContentTimer("content.duckling") { npc.ducklingTimer() }
        onNpcQueue("content.duck", "queue.death") { duckDeath() }
        onNpcQueue("content.duck", "queue.generic_queue1") { duckSay() }
        onNpcQueue("content.duckling", "queue.generic_queue1") { ducklingSay() }
        onNpcQueue("content.duckling", "queue.generic_queue2") { ducklingMourn() }
    }

    private fun Npc.duckTimer() {
        setNextTimer()
        queue("queue.generic_queue1", 1)
    }

    private fun StandardNpcAccess.duckSay() {
        say("Quack!")
        worldRepo.soundArea(coords, "synth.quack")
    }

    private suspend fun StandardNpcAccess.duckDeath() {
        val ducklings = search.findAll(coords, "npc.duck_update_ducklings", 1, HuntVis.LineOfSight)
        for (duckling in ducklings) {
            duckling.queue("queue.generic_queue2", 2)
        }
        val duck = search.find(coords, "npc.duck", 1, HuntVis.LineOfSight)
        duck?.queue("queue.generic_queue1", 2)
        death.deathNoDrops(this)
    }

    private fun Npc.ducklingTimer() {
        setNextTimer()
        val duck = search.find(coords, "npc.duck_female", 10, HuntVis.LineOfSight) ?: return
        duck.say("Quack?")
        worldRepo.soundArea(coords, "synth.quack")
        queue("queue.generic_queue1", 2)
    }

    private fun StandardNpcAccess.ducklingSay() {
        say("Eep!")
        worldRepo.soundArea(coords, "synth.quack")
    }

    private fun StandardNpcAccess.ducklingMourn() {
        say("Cheep cheep!")
        if (npc.mode != NpcMode.None) {
            return
        }
        val coord = mapFindSquareNone(coords, minRadius = 1, maxRadius = 9, collision)
        coord?.let(::walk)
        queue("queue.generic_queue1", 2)
    }

    private fun Npc.setNextTimer() {
        val next = random.of(50..100)
        aiTimer(next)
    }
}
