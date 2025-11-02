package org.alter.skills.firemaking

import dev.openrune.ServerCacheManager.getItem
import net.rsprot.protocol.game.outgoing.misc.client.HintArrow
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.message
import org.alter.api.ext.produceItemBox
import org.alter.game.model.EntityType
import org.alter.game.model.TileGraphic
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.rscm.RSCM
import org.alter.skills.firemaking.ColoredLogs.Companion.CAMPFIRE_OBJECTS

class CampfireEvents : PluginEvent() {

    companion object {
        private val CAMPFIRE_ROTATIONS = listOf(
            0 to "spotanims.forestry_campfire_smoke_01",
            90 to "spotanims.forestry_campfire_smoke_02",
            180 to "spotanims.forestry_campfire_smoke_03",
            270 to "spotanims.forestry_campfire_smoke_04"
        )

        private val CAMPFIRE_TIMER = TimerKey()
        val PLAYERS_ON_FIRE = AttributeKey<Int>()
    }

    private val VALID_FIRES = ColoredLogs.COLOURED_LOGS.values.map { it.second } + "objects.fire"

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

        Logs.logs.forEach { log ->
            on<ItemOnObject> {
                where { item.id == log.logItem && log.animation != RSCM.NONE }
                then {
                    if (VALID_FIRES.contains(gameObject.id)) {
                        lightFire(player, gameObject, log.logItem, log.initialTicks)
                    } else if (ColoredLogs.CAMPFIRE_OBJECTS.containsValue(gameObject.id)) {
                        tendFireAction(player, gameObject)
                    }
                }
            }
        }
    }

    private fun tendFireAction(player: Player, gameObject: GameObject) {
        val validLogs = Logs.logs.filter { log ->
            player.inventory.contains(log.logItem) &&
            log.animation != RSCM.NONE &&
            player.getSkills().getCurrentLevel(Skills.FIREMAKING) >= log.level
        }

        if (validLogs.isEmpty()) return

        val maxQuantity = validLogs.maxOf { log -> player.inventory.getItemCount(log.logItem) }

        player.queue {
            produceItemBox(player, *validLogs.map { it.logItem }.toIntArray(), maxProducable = maxQuantity) { itemId, amt ->
                val logData = validLogs.first { it.logItem == itemId }
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
            repeatUntil(delay = 3, immediate = true, predicate = { !replacement.isSpawned(world) }) {
                world.spawn(TileGraphic(tile = replacement.tile, id = graphicId))
            }
        }

        player.inventory.remove(log)
        replacement.attr[PLAYERS_ON_FIRE] = 0
        replacement.setTimer(CAMPFIRE_TIMER, initialTicks)
    }

    private fun tendFire(player: Player, gameObject: GameObject, logData: Logs.LogData, logsToAdd: Int) {


        gameObject.attr[PLAYERS_ON_FIRE] = (gameObject.attr[PLAYERS_ON_FIRE] ?: 0) + 1

        player.queue {
            var logsAdded = 0

            repeatUntil(delay = 9, true, {
                !canAddLog(player, gameObject, logData) || logsAdded >= logsToAdd
            }) {
                player.animate(logData.animation)
                player.addXp(Skills.FIREMAKING, logData.xp)
                player.inventory.remove(logData.logItem)

                logsAdded++

                wait(3)
                player.world.spawn(TileGraphic(tile = gameObject.tile, id = "spotanims.forestry_campfire_burning_spotanim"))
            }
            stopTending(player, gameObject)
        }
    }

    private fun canAddLog(player: Player, gameObject: GameObject, logData: Logs.LogData): Boolean {
        if (!player.inventory.contains("items.tinderbox")) {
            stopTending(player, gameObject, "You need a Tinderbox to do this.")
            return false
        }

        if (player.getSkills().getCurrentLevel(Skills.FIREMAKING) < logData.level) {
            stopTending(
                player,
                gameObject,
                "You need a Firemaking level of ${logData.level} to burn ${getItem(logData.logItem)} logs."
            )
            return false
        }

        if (!gameObject.isSpawned(player.world) || !player.inventory.contains(logData.logItem)) {
            stopTending(player, gameObject)
            return false
        }

        return true
    }

    private fun stopTending(player: Player, gameObject: GameObject, message: String? = null) {
        player.animate(RSCM.NONE)
        gameObject.attr[PLAYERS_ON_FIRE] = (gameObject.attr[PLAYERS_ON_FIRE] ?: 0) - 1
        message?.let(player::filterableMessage)
    }
}