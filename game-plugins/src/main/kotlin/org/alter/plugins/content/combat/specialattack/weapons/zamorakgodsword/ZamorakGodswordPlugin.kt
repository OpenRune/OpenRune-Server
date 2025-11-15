package org.alter.plugins.content.combat.specialattack.weapons.zamorakgodsword

import org.alter.api.ext.freeze
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

class ZamorakGodswordPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        val SPECIAL_REQUIREMENT = 50

        SpecialAttacks.register("items.zgs", SPECIAL_REQUIREMENT) {
            player.animate(id = "sequences.zgs_special_player")
            player.graphic(id = "spotanims.dh_sword_update_zamorak_special_spotanim")
            world.spawn(AreaSound(tile = player.tile, id = 3869, radius = 10, volume = 1))

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = 1.1)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = 1.0)
            val landHit = accuracy >= world.randomDouble()
            val delay = 1
            val pawnHit = player.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = delay)

            // Freeze target for 20 seconds if hit lands (PvP only, or certain NPCs)
            if (pawnHit.landed) {
                val isPvP = target is Player
                val isFreezableNpc = target is Npc // TODO: Add specific NPC checks if needed

                if (isPvP || isFreezableNpc) {
                    // 20 seconds = 20 / 0.6 = 33.33 ticks, use 33 ticks
                    val freezeTarget = target
                    freezeTarget.freeze(cycles = 33) {
                        if (freezeTarget is Player) {
                            freezeTarget.message("You have been frozen.")
                        }
                    }
                }
            }
        }
    }
}

