package org.alter.combat

import org.alter.api.WeaponType
import org.alter.api.ext.hasWeaponType
import org.alter.combat.strategy.NewMagicCombatStrategy
import org.alter.combat.strategy.NewMeleeCombatStrategy
import org.alter.combat.strategy.NewRangedCombatStrategy
import org.alter.game.combat.CombatSystem
import org.alter.game.combat.NpcAiSystem
import org.alter.game.combat.ai.AiStateMachine
import org.alter.game.combat.ai.IdleState
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.impl.WorldTickEvent

/**
 * Wires [CombatSystem.processTick] into the world tick loop and registers
 * the new melee/ranged/magic strategy implementations.
 */
class CombatSystemBootstrap : PluginEvent() {

    override fun init() {
        NpcCombatDefLoader.load()

        val combatSystem = CombatSystem(EventManager)
        CombatSystem.instance = combatSystem

        val aiSystem = NpcAiSystem(world)

        val meleeStrategy = NewMeleeCombatStrategy()
        val rangedStrategy = NewRangedCombatStrategy()
        val magicStrategy = NewMagicCombatStrategy()

        // Wire strategy resolver: NPC uses its combatClass field; Player uses weapon type.
        // NOTE: Player magic detection (CASTING_SPELL attr) requires a key defined in
        // game-plugins which is inaccessible here. Until that key is moved to a shared
        // module, players with an active spell will fall through to the melee default.
        // TODO: Add magic detection once Combat.CASTING_SPELL is moved to game-server/game-api.
        combatSystem.strategyResolver = { attacker ->
            when (attacker) {
                is Npc -> when (attacker.combatClass) {
                    CombatClass.RANGED -> rangedStrategy
                    CombatClass.MAGIC -> magicStrategy
                    else -> meleeStrategy
                }
                is Player -> when {
                    attacker.hasWeaponType(
                        WeaponType.BOW,
                        WeaponType.CROSSBOW,
                        WeaponType.THROWN,
                        WeaponType.CHINCHOMPA,
                    ) -> rangedStrategy
                    else -> meleeStrategy
                }
                else -> meleeStrategy
            }
        }

        // Wire combat-style resolver: NPC uses its combatStyle field; Player uses weapon type.
        // Full weapon-style-to-CombatStyle mapping lives in CombatConfigs (game-plugins).
        // Here we cover the most common cases; edge cases fall back to CRUSH (unarmed).
        // TODO: Port full CombatConfigs.getCombatStyle mapping once game-plugins dependency
        //       is either removed or the content module is allowed to depend on it.
        combatSystem.combatStyleResolver = { attacker ->
            when (attacker) {
                is Npc -> attacker.combatStyle
                is Player -> when {
                    attacker.hasWeaponType(
                        WeaponType.BOW,
                        WeaponType.CROSSBOW,
                        WeaponType.THROWN,
                        WeaponType.CHINCHOMPA,
                    ) -> CombatStyle.RANGED
                    else -> CombatStyle.CRUSH
                }
                else -> CombatStyle.CRUSH
            }
        }

        onEvent<WorldTickEvent> {
            aiSystem.processTick()    // AI state transitions first
            combatSystem.processTick() // Then combat pipeline
        }

        // Assign an AiStateMachine to every NPC that has combat stats on spawn.
        // The legacy globalNpcSpawn hook fires after setNpcDefaults (which sets combatDef),
        // so combatDef is guaranteed to be initialised when this lambda runs.
        world.plugins.bindGlobalNpcSpawn {
            val npc = ctx as Npc
            val combatDef = npc.combatDef
            if (combatDef.hitpoints > 0 && combatDef != NpcCombatDef.DEFAULT) {
                val machine = AiStateMachine(IdleState())
                npc.aiStateMachine = machine
                machine.start(npc)
            }
        }
    }
}
