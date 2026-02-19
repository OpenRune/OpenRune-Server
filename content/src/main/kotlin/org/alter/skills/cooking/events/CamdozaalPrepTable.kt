package org.alter.skills.cooking.events

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.Skills
import org.alter.api.computeSkillingSuccess
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.message
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.skills.cooking.events.CookingUtils.isStillAtStation
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import kotlin.random.Random/**
 * Handles cooking at the Camdozaal Preparation Table (knife-based prep, not heat).
 */
object CamdozaalPrepTable {

    private data class PrepFishDef(
        val raw: String,
        val prepared: String,
        val ruined: String,
        val level: Int,
        val xp: Int
    )

    val prepTableId: Int = "objects.camdozaal_preparation_table".asRSCM()

    private val knifeId: Int? = runCatching { "items.knife".asRSCM() }.getOrNull()

    private val prepFishByRaw: Map<Int, PrepFishDef> = listOf(
        PrepFishDef(
            raw = "items.raw_guppy",
            prepared = "items.guppy",
            ruined = "items.ruined_guppy",
            level = 7,
            xp = 12
        ),
        PrepFishDef(
            raw = "items.raw_cavefish",
            prepared = "items.cavefish",
            ruined = "items.ruined_cavefish",
            level = 20,
            xp = 23
        ),
        PrepFishDef(
            raw = "items.raw_tetra",
            prepared = "items.tetra",
            ruined = "items.ruined_tetra",
            level = 33,
            xp = 31
        ),
        PrepFishDef(
            raw = "items.raw_catfish",
            prepared = "items.catfish",
            ruined = "items.ruined_catfish",
            level = 46,
            xp = 43
        )
    ).mapNotNull { def ->
        val rawId = runCatching { def.raw.asRSCM() }.getOrNull() ?: return@mapNotNull null
        rawId to def
    }.toMap()

    /**
     * Checks if the given item ID is a raw hunter fish for the prep table.
     */
    fun isRawPrepFish(itemId: Int): Boolean = prepFishByRaw.containsKey(itemId)

    /**
     * Handles using a raw fish on the preparation table.
     */
    suspend fun QueueTask.prepareFish(
        player: Player,
        gameObject: GameObject,
        rawItemId: Int
    ) {
        val def = prepFishByRaw[rawItemId] ?: return
        val knife = knifeId
        if (knife == null || player.inventory.getItemCount(knife) <= 0) {
            player.message("You need a knife to prepare this.")
            return
        }

        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)
        if (cookingLevel < def.level) {
            player.filterableMessage("You need a Cooking level of ${def.level} to prepare this.")
            return
        }

        val objectTile = gameObject.tile
        val objectId = gameObject.internalID
        val preparedId = runCatching { def.prepared.asRSCM() }.getOrNull() ?: return
        val ruinedId = runCatching { def.ruined.asRSCM() }.getOrNull() ?: return

        var prepared = 0
        player.filterableMessage("You begin preparing the fish...")

        repeatWhile(delay = 3, immediate = true, canRepeat = {
            player.inventory.getItemCount(rawItemId) > 0 &&
                player.inventory.getItemCount(knife) > 0 &&
                isStillAtStation(player, objectTile, objectId)
        }) {
            runCatching { player.animate("sequences.human_cutting", interruptable = true) }
                .onFailure { player.animate(RSCM.NONE) }

            val removed = player.inventory.remove(rawItemId, 1)
            if (!removed.hasSucceeded()) {
                stop()
                return@repeatWhile
            }

            val successChance = computeSkillingSuccess(
                low = 64,
                high = 256,
                level = (cookingLevel - def.level + 1).coerceIn(1, 99)
            ).coerceIn(0.0, 1.0)

            val success = Random.nextDouble() < successChance

            if (success) {
                if (!player.inventory.add(preparedId, 1).hasSucceeded()) {
                    player.inventory.add(rawItemId, 1)
                    player.filterableMessage("You don't have enough inventory space.")
                    stop()
                    return@repeatWhile
                }
                player.addXp(Skills.COOKING, def.xp.toDouble())
                val name = getItem(preparedId)?.name?.lowercase() ?: "fish"
                player.filterableMessage("You prepare the $name.")
            } else {
                if (!player.inventory.add(ruinedId, 1).hasSucceeded()) {
                    player.inventory.add(rawItemId, 1)
                    player.filterableMessage("You don't have enough inventory space.")
                    stop()
                    return@repeatWhile
                }
                val name = getItem(ruinedId)?.name?.lowercase() ?: "fish"
                player.filterableMessage("You ruin the $name.")
            }

            prepared++
            wait(1)
        }

        player.animate(RSCM.NONE)
    }
}
