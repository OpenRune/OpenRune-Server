package org.alter.skills.firemaking

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.produceItemBox
import org.alter.game.model.EntityType
import org.alter.game.model.TileGraphic
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.INTERACTING_OBJ_ATTR
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginConfig
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.InterruptActionEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType
import org.alter.settings.FiremakingSettings
import org.alter.skills.firemaking.BurnLogEvents.Companion.logs
import org.alter.skills.firemaking.ColoredLogs.Companion.CAMPFIRE_OBJECTS
import org.generated.tables.FiremakingLogsRow

@PluginConfig("firemaking.toml")
class CampfireEvents : PluginEvent() {

    companion object {
        private val CAMPFIRE_ROTATIONS = listOf(
            0 to "spotanims.forestry_campfire_smoke_01",
            90 to "spotanims.forestry_campfire_smoke_02",
            180 to "spotanims.forestry_campfire_smoke_03",
            270 to "spotanims.forestry_campfire_smoke_04"
        )

        private val CAMPFIRE_TIMER = TimerKey()

        // Attribute for tracking number of players tending the fire
        val PLAYERS_COUNT_ATTR = AttributeKey<Int>()
    }

    private val VALID_FIRES = ColoredLogs.COLOURED_LOGS.values.map { it.second } + "objects.fire"

    val firemakingSettings: FiremakingSettings
        get() = getSetting<FiremakingSettings>()

    override fun init() {

        on<ObjectClickEvent> {
            where { ColoredLogs.CAMPFIRE_OBJECTS.containsValue(gameObject.id) }
            then {
                when (op) {
                    MenuOption.OP1 -> tendFireAction(player, gameObject)
                    MenuOption.OP2 -> showFireStatus(player, gameObject)
                    MenuOption.OP3 -> player.animate("sequences.forestry_sitting_tea_loop")
                    else -> {}
                }
            }
        }

        on<InterruptActionEvent> {
            where { ColoredLogs.CAMPFIRE_OBJECTS.containsValue(player.attr[INTERACTING_OBJ_ATTR]?.get()?.id) }
            then { removePlayerFromFire(player.attr[INTERACTING_OBJ_ATTR]?.get() ?: return@then) }
        }

        logs.forEach { log ->
            on<ItemOnObject> {
                where { item.id == log.item && log.foresterAnimation != -1 }
                then {
                    if (VALID_FIRES.contains(gameObject.id)) {
                        lightFire(player, gameObject, log.item, log.foresterLogTicks?:0)
                    } else if (ColoredLogs.CAMPFIRE_OBJECTS.containsValue(gameObject.id)) {
                        tendFireAction(player, gameObject)
                    }
                }
            }
        }
    }

    private fun tendFireAction(player: Player, gameObject: GameObject) {
        val validLogs = logs.filter { log ->
            player.inventory.contains(log.item) &&
                    log.foresterLogTicks != -1 &&
                    player.getSkills().getCurrentLevel(Skills.FIREMAKING) >= log.level
        }

        if (validLogs.isEmpty()) return

        val maxQuantity = validLogs.maxOf { log -> player.inventory.getItemCount(log.item) }

        player.queue {
            produceItemBox(player, *validLogs.map { it.item }.toIntArray(), maxProducable = maxQuantity) { itemId, amt ->
                val logData = validLogs.first { it.item == itemId }
                tendFire(player, gameObject, logData, amt)
            }
        }
    }

    private fun showFireStatus(player: Player, gameObject: GameObject) {
        player.animate(RSCM.NONE)
        val ticksLeft = gameObject.getTimeLeft(CAMPFIRE_TIMER)
        val message = when (ticksLeft) {
            in 0..58 -> "The embers glow softly."
            in 59..118 -> "The flames flicker gently."
            in 119..178 -> "The fire burns steadily."
            in 179..238 -> "The fire burns brightly."
            else -> "The roaring fire crackles invitingly."
        }
        player.filterableMessage(message)
    }

    private fun lightFire(player: Player, gameObject: GameObject, log: Int, initialTicks: Int) {
        val entities = player.world.chunks.get(player.tile)!!.getEntities<GameObject>(EntityType.DYNAMIC_OBJECT)

        val isFireNearby = entities.any { fire ->
            val isCampfire = CAMPFIRE_OBJECTS.containsValue(fire.id)
            val distance = fire.tile.getDistance(player.tile)
            isCampfire && distance <= 5
        }

        if (isFireNearby) {
            player.filterableMessage("There's a Forester's Campfire nearby, help tend to that one or move further away.")
            return
        }

        if (!player.inventory.contains("items.tinderbox")) {
            stopTending(player, gameObject, "You need a Tinderbox to do this.")
            return
        }

        val world = player.world
        val fireObject = ColoredLogs.CAMPFIRE_OBJECTS[VALID_FIRES.indexOf(gameObject.id)] ?: "objects.forestry_fire"
        val replacement = gameObject.replaceWith(world, fireObject, CAMPFIRE_TIMER)

        world.queue {
            val graphicId = CAMPFIRE_ROTATIONS[replacement.rot].second
            repeatWhile(delay = 3, immediate = true, canRepeat = { replacement.isSpawned(world) }) {
                world.spawn(TileGraphic(tile = replacement.tile, id = graphicId))
            }
        }

        player.inventory.remove(log)
        replacement.setTimer(CAMPFIRE_TIMER, initialTicks)
    }

    private fun tendFire(player: Player, gameObject: GameObject, logData: FiremakingLogsRow, logsToAdd: Int) {
        addPlayerToFire(gameObject)

        val ticks = firemakingSettings.campFireDelayTicks

        player.queue {
            var logsAdded = 0

            repeatWhile(delay = ticks, immediate = true, canRepeat = {
                canAddLog(player, gameObject, logData) && logsAdded < logsToAdd
            }) {
                player.animate(RSCM.getReverseMapping(RSCMType.SEQTYPES, logData.foresterAnimation?: -1)?: RSCM.NONE)
                val playersUsingFire = gameObject.attr[PLAYERS_COUNT_ATTR] ?: 1

                val currentTime = gameObject.getTimeLeft(CAMPFIRE_TIMER)
                val newTime = (currentTime + (logData.foresterLogTicks?: 4)).coerceAtMost(300)
                gameObject.increaseTimer(CAMPFIRE_TIMER, newTime)

                player.addXp(Skills.FIREMAKING, calculateXp(logData.xp, playersUsingFire))
                player.inventory.remove(logData.item)

                logsAdded++
                wait(3)
                player.world.spawn(TileGraphic(tile = gameObject.tile, id = "spotanims.forestry_campfire_burning_spotanim"))
            }

            removePlayerFromFire(gameObject)
            stopTending(player, gameObject)
        }
    }

    private fun calculateXp(xp: Int, playersUsingFire: Int): Double {
        val bonfire = firemakingSettings.rs2Bonfire?.takeIf { it.enabled } ?: return xp.toDouble()

        val xpBoostMap: Map<Int, Double> = bonfire.xpBoostPlayers.mapKeys { it.key.toInt() }
            .mapValues { it.value / 100.0 }

        val applicableBoost = xpBoostMap
            .filterKeys { it <= (playersUsingFire - 1) }
            .maxByOrNull { it.key }
            ?.value ?: 0.0

        return (xp * (1 + applicableBoost))
    }

    private fun canAddLog(player: Player, gameObject: GameObject, logData: FiremakingLogsRow): Boolean {
        if (!player.inventory.contains("items.tinderbox")) {
            stopTending(player, gameObject, "You need a Tinderbox to do this.")
            return false
        }

        if (player.getSkills().getCurrentLevel(Skills.FIREMAKING) < logData.level) {
            stopTending(
                player,
                gameObject,
                "You need a Firemaking level of ${logData.level} to burn ${getItem(logData.item)} logs."
            )
            return false
        }

        if (!gameObject.isSpawned(player.world) || !player.inventory.contains(logData.item)) {
            stopTending(player, gameObject)
            return false
        }

        return true
    }

    private fun stopTending(player: Player, gameObject: GameObject, message: String? = null) {
        player.animate(RSCM.NONE)
        message?.let(player::filterableMessage)
    }

    private fun addPlayerToFire(gameObject: GameObject) {
        val count = gameObject.attr.getOrPut(PLAYERS_COUNT_ATTR) { 0 }
        gameObject.attr[PLAYERS_COUNT_ATTR] = count + 1
    }

    private fun removePlayerFromFire(gameObject: GameObject) {
        val count = gameObject.attr.getOrPut(PLAYERS_COUNT_ATTR) { 0 }
        gameObject.attr[PLAYERS_COUNT_ATTR] = (count - 1).coerceAtLeast(0)
    }
}