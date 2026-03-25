package org.alter.combat

import org.alter.api.WeaponType
import org.alter.api.ext.hasWeaponType
import org.alter.combat.strategy.NewMagicCombatStrategy
import org.alter.combat.strategy.NewMeleeCombatStrategy
import org.alter.combat.strategy.NewRangedCombatStrategy
import org.alter.game.combat.CombatSystem
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.impl.WorldTickEvent

/**
 * Wires [CombatSystem.processTick] into the world tick loop and registers
 * the new melee/ranged/magic strategy implementations.
 *
 * Only runs when `combat.useNewSystem: true` is set in game.yml.
 * When false, the legacy CombatPlugin handles all combat and this
 * bootstrap is a no-op, preventing double-processing.
 */
class CombatSystemBootstrap : PluginEvent() {

    override fun init() {
        NpcCombatDefLoader.load()

        val combatSystem = CombatSystem(EventManager)

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
            if (world.gameContext.useNewCombatSystem) {
                combatSystem.processTick()
            }
        }
    }
}
