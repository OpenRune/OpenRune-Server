package org.alter.game.combat.ai

import org.alter.game.model.entity.Npc

interface AiState {
    fun onEnter(npc: Npc)
    fun tick(npc: Npc): AiState?
    fun onExit(npc: Npc)
}
