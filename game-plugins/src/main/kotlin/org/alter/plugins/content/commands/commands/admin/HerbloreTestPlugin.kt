package org.alter.plugins.content.commands.commands.admin

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.columnOptional
import org.alter.game.util.multiColumnOptional
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType

/**
 * Admin command to populate bank with all herblore items for testing.
 * Adds all herbs, vials, secondary ingredients, unfinished potions, and finished potions.
 */
class HerbloreTestPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("herbloretest", Privilege.ADMIN_POWER, description = "Populate bank with all herblore items for testing") {
            var addedCount = 0

            // Add vial of water
            player.bank.add("items.vial_water", 1000, true)
            addedCount++

            // Add all grimy herbs from unfinished potions table
            table("tables.herblore_unfinished").forEach { row ->
                val herbItem = row.column("columns.herblore_unfinished:herb_item", ObjType)
                val unfinishedPotion = row.column("columns.herblore_unfinished:unfinished_potion", ObjType)

                player.bank.add(herbItem, 100, true)
                addedCount++

                player.bank.add(unfinishedPotion, 100, true)
                addedCount++
            }

            // Collect unique secondary ingredients and add finished potions
            val secondaryItems = mutableSetOf<Int>()

            table("tables.herblore_finished").forEach { row ->
                val secondaries = row.columnOptional("columns.herblore_finished:secondaries", ObjType)
                    ?.let { listOf(it) } ?: row.multiColumnOptional("columns.herblore_finished:secondaries", ObjType).filterNotNull()
                val finishedPotion = row.columnOptional("columns.herblore_finished:finished_potion", ObjType)

                secondaryItems.addAll(secondaries)

                // Add finished potion if it exists
                finishedPotion?.let {
                    player.bank.add(it, 100, true)
                    addedCount++
                }
            }

            // Add all unique secondary ingredients
            secondaryItems.forEach { secondaryItem ->
                player.bank.add(secondaryItem, 100, true)
                addedCount++
            }

            // Add cleaning herbs
            table("tables.herblore_cleaning").forEach { row ->
                val grimyHerb = row.column("columns.herblore_cleaning:grimy_herb", ObjType)
                val cleanHerb = row.column("columns.herblore_cleaning:clean_herb", ObjType)

                player.bank.add(grimyHerb, 100, true)
                addedCount++
                player.bank.add(cleanHerb, 100, true)
                addedCount++
            }

            // Add barbarian mix ingredients and mixes
            player.bank.add("items.brut_roe", 100, true)
            addedCount++
            player.bank.add("items.brut_caviar", 100, true)
            addedCount++

            table("tables.herblore_barbarian_mixes").forEach { row ->
                val twoDosePotion = row.column("columns.herblore_barbarian_mixes:two_dose_potion", ObjType)
                val barbarianMix = row.column("columns.herblore_barbarian_mixes:barbarian_mix", ObjType)

                player.bank.add(twoDosePotion, 100, true)
                addedCount++
                player.bank.add(barbarianMix, 100, true)
                addedCount++
            }

            // Add swamp tar and finished tars
            player.bank.add("items.swamp_tar", 1000, true)
            addedCount++

            table("tables.herblore_swamp_tar").forEach { row ->
                val herb = row.column("columns.herblore_swamp_tar:herb", ObjType)
                val finishedTar = row.column("columns.herblore_swamp_tar:finished_tar", ObjType)

                player.bank.add(herb, 100, true)
                addedCount++
                player.bank.add(finishedTar, 100, true)
                addedCount++
            }

            player.message("Added $addedCount different herblore items to your bank for testing.")
        }
    }
}

