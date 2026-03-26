package org.alter.game.combat

import org.alter.game.model.World

class NpcAiSystem(private val world: World) {

    fun processTick() {
        world.npcs.forEach { npc ->
            npc.aiStateMachine?.tick(npc)
        }
    }
}
