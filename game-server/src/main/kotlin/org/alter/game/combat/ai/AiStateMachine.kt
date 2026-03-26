package org.alter.game.combat.ai

import org.alter.game.model.entity.Npc

class AiStateMachine(initialState: AiState) {
    var currentState: AiState = initialState
        private set

    fun start(npc: Npc) {
        currentState.onEnter(npc)
    }

    fun tick(npc: Npc) {
        val nextState = currentState.tick(npc)
        if (nextState != null) {
            currentState.onExit(npc)
            currentState = nextState
            currentState.onEnter(npc)
        }
    }
}
