package org.alter.game.combat

import org.alter.game.model.combat.NpcCombatDef

object NpcCombatDefRegistry {
    private val defs = mutableMapOf<Int, NpcCombatDef>()

    fun get(npcId: Int): NpcCombatDef? = defs[npcId]

    fun register(npcId: Int, def: NpcCombatDef) {
        defs[npcId] = def
    }

    fun getAll(): Map<Int, NpcCombatDef> = defs.toMap()

    fun clear() { defs.clear() }
}
