package org.alter.skills.mining

import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.game.pluginnew.PluginEvent
import org.alter.rscm.RSCM.asRSCM
import org.generated.tables.mining.MiningEnhancersRow
import kotlin.random.Random

class MiningEnhancers : PluginEvent() {

    private val varrockArmour = mapOf(
        0 to "items.varrock_armour_easy",
        1 to "items.varrock_armour_medium",
        2 to "items.varrock_armour_hard",
        3 to "items.varrock_armour_elite"
    )

    override fun init() {
        // Bonus for mining cape
        on<RockOreObtainedEvent> {
            where {
                val enhancers = rockData.miningEnhancers?.let { MiningEnhancersRow.getRow(it) }
                rockData.oreItem != null &&
                        enhancers?.miningCape == true &&
                        player.equipment.contains("items.skillcape_mining", "items.skillcape_mining_trimmed")
            }
            then {
                if (Random.nextInt(100) < 5) {
                    rockData.oreItem?.let { player.inventory.add(it, 1) }
                }
            }
        }

        // Bonus for Varrock armour
        on<RockOreObtainedEvent> {
            where {
                val level = rockData.miningEnhancers?.let { MiningEnhancersRow.getRow(it).varrockArmourLevel }
                rockData.oreItem != null &&
                        level != null &&
                        player.equipment.contains(varrockArmour[level] ?: "")
            }
            then {
                if (Random.nextInt(100) < 10) {
                    rockData.oreItem?.let {
                        player.inventory.add(it, 1)
                        player.addXp(Skills.MINING, MiningPlugin.calculateMiningXP(rockData.xp.toDouble(), false, player))
                    }
                }
            }
        }

        // Bonus for celestial ring
        on<RockOreObtainedEvent> {
            where {
                val enhancers = rockData.miningEnhancers?.let { MiningEnhancersRow.getRow(it) }
                rockData.oreItem != null &&
                        enhancers?.ringOrSignet == true &&
                        player.equipment.contains("items.celestial_ring", "items.celestial_signet")
            }
            then {
                if (Random.nextInt(100) < 10) {
                    rockData.oreItem?.let { player.inventory.add(it, 1) }
                }
            }
        }

        //Trahaearn mine
        on<RockOreObtainedEvent> {
            where { player.tile.regionId == 13250 }
            then {
                if (Random.nextInt(65) == 0) {
                    player.inventory.add("items.prif_crystal_shard")
                    player.message("You have obtained a crystal shard!")
                }
            }
        }

    }
}