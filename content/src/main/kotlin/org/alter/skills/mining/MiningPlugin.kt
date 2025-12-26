package org.alter.skills.mining

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.INTERACTING_OBJ_ATTR
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.ReturnableEventListener
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.getRSCM
import org.alter.rscm.RSCMType
import org.alter.skills.mining.MiningDefinitions.getDepletionRange
import org.alter.skills.mining.MiningDefinitions.isInfiniteResource
import org.alter.skills.mining.MiningDefinitions.pickaxeData
import org.alter.skills.woodcutting.WoodcuttingDefinitions.axeData
import org.generated.tables.mining.MiningEnhancersRow
import org.generated.tables.mining.MiningPickaxesRow
import org.generated.tables.mining.MiningRocksRow
import kotlin.random.Random

class MiningPlugin : PluginEvent() {

    val amuletOfGlorys = listOf(
        "items.amulet_of_glory_1",
        "items.amulet_of_glory_2",
        "items.amulet_of_glory_3",
        "items.amulet_of_glory_4"
    )

    private val miningGloves = listOf(
        "items.mguild_gloves" to 1,
        "items.mguild_gloves_expert" to 2,
        "items.mguild_gloves_superior" to 3
    )

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Sounds
         */
        const val ORE_OBTAINED_SOUND = 3600

        /**
         * Attribute key for tracking how many ores have been mined from a rock before depletion.
         */
        val MINED_ORE_COUNT_ATTR = AttributeKey<Int>()

        /**
         * Attribute key for tracking how many ores have been mined from a rock before depletion.
         */
        val DEPLETE_GLOVE_COUNT_ATTR = AttributeKey<Int>()

        /**
         * Attribute key for the randomly selected threshold at which a rock depletes for mechanic 2.
         */
        val DEPLETION_THRESHOLD_ATTR = AttributeKey<Int>()

        private const val RANDOM_GEM_CHANCE = 1.0 / 256

        private val GEM_ROCK_DROP_TABLE: Map<Int, Double> = mapOf(
            getRSCM("items.uncut_opal") to 1.0 / 2.133,
            getRSCM("items.uncut_jade") to 1.0 / 4.267,
            getRSCM("items.uncut_red_topaz") to 1.0 / 8.533,
            getRSCM("items.uncut_sapphire") to 1.0 / 14.22,
            getRSCM("items.uncut_emerald") to 1.0 / 25.6,
            getRSCM("items.uncut_ruby") to 1.0 / 25.6,
            getRSCM("items.uncut_diamond") to 1.0 / 32.0,
        )
        private val RANDOM_GEM_DROP_TABLE: Map<Int, Double> = mapOf(
            getRSCM("items.uncut_sapphire") to 1.0 / 14.22,
            getRSCM("items.uncut_emerald") to 1.0 / 25.6,
            getRSCM("items.uncut_ruby") to 1.0 / 25.6,
            getRSCM("items.uncut_diamond") to 1.0 / 32.0,
        )

        private fun rollGem(dropTable: Map<Int, Double>): Int {
            val totalWeight = dropTable.values.sum()
            val roll = Random.nextDouble(totalWeight)

            var cumulative = 0.0
            for ((itemId, weight) in dropTable) {
                cumulative += weight
                if (roll <= cumulative) {
                    return itemId
                }
            }

            return dropTable.keys.last()
        }

        fun calculateMiningXP(baseXP: Double, hasVarrockDiary: Boolean = false, player: Player): Double {
            val xpBoosts = mapOf(
                "items.motherlode_reward_hat" to 0.004,
                "items.fossil_motherlode_reward_hat" to 0.004,
                "items.motherlode_reward_top" to 0.008,
                "items.fossil_motherlode_reward_top" to 0.008,
                "items.motherlode_reward_legs" to 0.006,
                "items.fossil_motherlode_reward_legs" to 0.006,
                "items.motherlode_reward_boots" to 0.002
            )

            val setBonus = 0.025

            val equippedItems = xpBoosts.keys.filter { player.equipment.contains(it) }

            val totalBoost = equippedItems.sumOf { xpBoosts[it] ?: 0.0 }

            val requiredSlots = listOf(
                listOf("items.motherlode_reward_hat", "items.fossil_motherlode_reward_hat"),
                listOf("items.motherlode_reward_top", "items.fossil_motherlode_reward_top").let { tops ->
                    if (hasVarrockDiary) tops + listOf("items.varrock_armour_elite") else tops
                },
                listOf("items.motherlode_reward_legs", "items.fossil_motherlode_reward_legs"),
                listOf("items.motherlode_reward_boots", "items.fossil_motherlode_reward_boots")
            )

            val hasFullSet = requiredSlots.all { slotOptions -> slotOptions.any { player.equipment.contains(it) } }

            return baseXP * (1 + totalBoost + if (hasFullSet) setBonus else 0.0)
        }
    }

    /**
     * Registers a rock deplete handler for a specific rock type.
     */
    fun onDeplete(rockTypeId: String, handler: RockDepleteHandler) {
        require(handler.rockType == rockTypeId) { "Handler rock type (${handler.rockType}) must match provided rock type ($rockTypeId)" }
        ReturnableEventListener.on<RockDepleteEvent, Boolean> {
            where { rockType == handler.rockType }
            then {
                return@then handler.handleDeplete(player, rockObject, world)
            }
        }
    }

    override fun init() {
        registerDepleteHandlers()

        MiningDefinitions.miningRocks.forEach { rock ->
            rock.rockObject.forEach { rockId ->
                try {
                    onObjectOption(rockId, "mine", "Mine") {
                        player.queue { mineRock(player, rock) }
                    }
                } catch (e: Exception) {
                    logger.warn { "Rock object '$rockId' not found in cache or option not available, skipping registration: ${e.message}" }
                }
            }
        }
    }

    private fun registerDepleteHandlers() {
        // Placeholder for custom rock-specific handlers when required.
    }

    private fun getDepletedRock(rockData: MiningRocksRow): Int? =
        rockData.emptyRockObject

    private fun hasAnyPickaxe(player: Player): Boolean {
        player.equipment[EquipmentType.WEAPON.id]?.let { weapon ->
            if (pickaxeData.any { it.item == weapon.id }) return true
        }

        return player.inventory.asSequence()
            .filterNotNull()
            .any { axeData.any { axeData -> axeData.item == it.id } }
    }

    private fun getBestPickaxe(player: Player): MiningPickaxesRow? {
        val miningLevel = player.getSkills().getBaseLevel(Skills.MINING)

        player.equipment[EquipmentType.WEAPON.id]?.let { weapon ->
            pickaxeData.find { it.item == weapon.id }?.takeIf { miningLevel >= it.level }?.let { return it }
        }

        return player.inventory.asSequence()
            .filterNotNull()
            .mapNotNull { invItem -> pickaxeData.find { it.item == invItem.id } }
            .filter { miningLevel >= it.level }
            .maxByOrNull { it.level }
    }

    private fun isDepletedRock(obj: GameObject, depletedId: Int, player: Player): Boolean {
        return obj.getTransform(player) == depletedId
    }

    private fun handleOreObtained(
        player: Player,
        rockData: MiningRocksRow
    ): Int? {
        var oreItem = when {
            rockData.type == "gemrock" -> rollGem(GEM_ROCK_DROP_TABLE)
            shouldRollRandomGem(rockData,player.equipment.contains(*amuletOfGlorys.toTypedArray())) -> rollGem(RANDOM_GEM_DROP_TABLE)
            else -> resolveOreItem(player, rockData) ?: return null
        }

        var total = 1

        if (player.equipment.contains("items.jewl_bracelet_of_clay")) {
            oreItem = "items.softclay".asRSCM()
            if (rockData.rockObject.any { it in listOf("objects.softclayrock1".asRSCM(), "objects.softclayrock2".asRSCM()) }) {
                total = 2
            }
        }


        if (player.inventory.add(oreItem, total).hasSucceeded()) {
            player.addXp(Skills.MINING, calculateMiningXP(rockData.xp.toDouble(),false,player))
            try {
                val oreName = Item(oreItem).getName().lowercase()
                player.message("You manage to mine some $oreName.")
            } catch (e: Exception) {
                player.message("You manage to mine some ore.")
            }
            player.playSound(ORE_OBTAINED_SOUND, volume = 1, delay = 0)
            return oreItem
        } else {
            player.message("Your inventory is too full to hold any more ore.")
            player.animate(RSCM.NONE)
            return null
        }
    }

    private fun depleteRock(
        player: Player,
        obj: GameObject,
        rockData: MiningRocksRow,
    ): Boolean {
        val specialRock = EventManager.postWithResult(
            RockDepleteEvent(
                player = player,
                rockObject = obj,
                rockType = rockData.type,
                world = world,
            ),
        )

        if (specialRock) {
            return true
        }

        getDepletedRock(rockData)?.let { depleted ->
            obj.replaceWith(world, depleted, rockData.respawnCycles, restoreOriginal = true)
        }

        player.animate(RSCM.NONE)

        return false
    }

    private fun resolveAnimationId(pickaxe: MiningPickaxesRow, rockType: String): Int {
        if (rockType == "wall") {
            pickaxe.wallAnimation.let { return it }
        }
        return pickaxe.animation
    }

    private fun shouldRollRandomGem(rockData: MiningRocksRow, hasAmuletOfGlory: Boolean): Boolean {
        if (rockData.type == "gemrock") return false
        if (rockData.oreItem == null) return false

        val chance = if (hasAmuletOfGlory) 1.0 / 86 else RANDOM_GEM_CHANCE
        return Random.nextDouble() < chance
    }

    private fun resolveOreItem(player: Player, rockData: MiningRocksRow): Int? {
        val oreItem = rockData.oreItem ?: return null

        if (oreItem == getRSCM("items.blankrune") && player.getSkills().getBaseLevel(Skills.MINING) >= 30) {
            return getRSCM("items.blankrune_high")
        }

        return oreItem
    }

    suspend fun QueueTask.mineRock(player: Player, rockData: MiningRocksRow) {
        val miningLevel = player.getSkills().getBaseLevel(Skills.MINING)

        if (miningLevel < rockData.level) {
            player.message("You need a Mining level of ${rockData.level} to mine this rock.")
            return
        }

        if (!hasAnyPickaxe(player)) {
            player.message("You need a pickaxe to mine this rock.")
            return
        }

        val pickaxe = getBestPickaxe(player)
        if (pickaxe == null) {
            player.message("You do not have a pickaxe which you have the Mining level to use.")
            return
        }

        val obj = player.attr[INTERACTING_OBJ_ATTR]?.get() ?: return

        if (!obj.isSpawned(world)) {
            return
        }

        val depletedId = getDepletedRock(rockData)

        val nearestTile = obj.findNearestTile(player.tile)
        player.faceTile(nearestTile)
        player.message("You swing your pickaxe at the rock.")

        val tickDelay = pickaxe.delay
        val (low, high) = rockData.successRateLow to rockData.successRateHigh
        val animationId = resolveAnimationId(pickaxe, rockData.type)
        val miningAnimation = RSCM.getReverseMapping(RSCMType.SEQTYPES, animationId) ?: return

        player.loopAnim(miningAnimation)

        repeatWhile(
            delay = tickDelay,
            immediate = false,
            canRepeat = {
                val currentNearestTile = obj.findNearestTile(player.tile)
                val notDepleted = depletedId == null || !isDepletedRock(obj, depletedId, player)
                player.tile.isWithinRadius(currentNearestTile, 1) && !player.inventory.isFull() &&
                        obj.isSpawned(world) &&
                        notDepleted
            }
        ) {

            val success = success(low, high, miningLevel)

            if (success) {

                val oreId = handleOreObtained(player, rockData) ?: return@repeatWhile

                RockOreObtainedEvent(player, obj, rockData, resourceId = oreId).post()

                val shouldDeplete = when {
                    rockData.isInfiniteResource() -> false
                    rockData.depleteMechanic == 2 -> {
                        val depletionRange = rockData.getDepletionRange()
                        val depletionThreshold = obj.attr.getOrPut(DEPLETION_THRESHOLD_ATTR) {
                            Random.nextInt(depletionRange.first, depletionRange.last + 1)
                        }

                        val newCount = obj.attr.getOrPut(MINED_ORE_COUNT_ATTR) { 0 } + 1
                        obj.attr[MINED_ORE_COUNT_ATTR] = newCount

                        newCount >= depletionThreshold
                    }

                    else -> handleNormalDeplete(player, obj, rockData)
                }


                if (shouldDeplete) {
                    obj.attr.remove(MINED_ORE_COUNT_ATTR)
                    obj.attr.remove(DEPLETION_THRESHOLD_ATTR)
                    depleteRock(player, obj, rockData)
                    return@repeatWhile
                }
            }
        }

        player.stopLoopAnim()
    }


    private fun handleNormalDeplete(player: Player, obj: GameObject, rockData: MiningRocksRow): Boolean {
        val gloveInfo = rockData.miningEnhancers
            ?.let { MiningEnhancersRow.getRow(it).miningGloves }
            ?.let { mapGlove(it) }

        val currentCount = obj.attr[DEPLETE_GLOVE_COUNT_ATTR] ?: 0

        return if (gloveInfo != null && player.equipment.contains(gloveInfo.first)) {
            if (currentCount != gloveInfo.second) {
                obj.attr.increment(DEPLETE_GLOVE_COUNT_ATTR, 1)
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    private fun mapGlove(type: String): Pair<String, Int> = when (type) {
        "standard" -> miningGloves[0]
        "expert" -> miningGloves[1]
        "superior" -> miningGloves[2]
        else -> miningGloves[1]
    }

}

