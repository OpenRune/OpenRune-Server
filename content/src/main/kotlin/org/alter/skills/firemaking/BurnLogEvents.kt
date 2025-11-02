package org.alter.skills.firemaking

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.message
import org.alter.api.success
import org.alter.game.model.Direction
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.model.move.walkTo
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.PluginConfig
import org.alter.game.pluginnew.event.impl.GroundItemClickEvent
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.settings.FiremakingSettings


@PluginConfig("firemaking.yml")
class BurnLogEvents : PluginEvent() {

    companion object {
        private val WALK_DIRECTIONS = listOf(Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH)
    }

    override fun init() {

        val setting = getSetting<FiremakingSettings>()

        println("MAX FIRES: "+ setting.maxFires)


        Logs.logs.forEach { log ->
            onItemOnItem("items.tinderbox", log.logItem) {
                burnLog(player, log.logItem, null, log.xp, log.level)
            }

            on<GroundItemClickEvent> {
                where {
                    groundItem.item == log.logItem &&
                            option == MenuOption.OP4 &&
                            player.inventory.contains("items.tinderbox")
                }
                then { burnLog(player, log.logItem, groundItem, log.xp, log.level) }
            }
        }
    }

    private fun burnLog(player: Player, log: Int, groundItem: GroundItem?, xp: Int, level: Int) {
        val logDrop = groundItem ?: GroundItem(log, 1, player.tile, player)
        val isGroundBurning = groundItem != null

        if (!canBurn(player, isGroundBurning, log, logDrop, level)) {
            player.animate(RSCM.NONE)
            return
        }

        if (groundItem == null) {
            player.inventory.remove(log)
            player.world.spawn(logDrop)
        }

        player.filterableMessage("You attempt to light the logs.")

        player.queue {
            repeatUntil(delay = 3, immediate = true, predicate = { false }) {
                if (!player.world.isSpawned(logDrop)) {
                    player.animate(RSCM.NONE)
                    return@repeatUntil
                }

                player.animate("sequences.human_createfire")

                if (!canBurn(player, isGroundBurning, log, logDrop, level)) {
                    player.animate(RSCM.NONE)
                    return@repeatUntil
                }

                val firemakingLevel = player.getSkills().getCurrentLevel(Skills.FIREMAKING)
                val success = ColoredLogs.isColoredLog(log) || success(64, 512, 1, firemakingLevel)
                if (success) {
                    handleFireSuccess(player, logDrop, xp)
                    return@repeatUntil
                }
            }
        }
    }

    private fun handleFireSuccess(player: Player, logDrop: GroundItem, xp: Int) {
        val world = player.world
        world.queue {
            val fireId = ColoredLogs.COLOURED_LOGS[logDrop.item]?.second ?: "objects.fire"
            val fire = DynamicObject(fireId, 10, 0, logDrop.tile)
            val burnTicks = (150..300).random()

            world.remove(logDrop)
            player.filterableMessage("The fire catches and the logs begin to burn.")
            player.addXp(Skills.FIREMAKING, xp)
            world.spawn(fire)

            wait(burnTicks)
            world.remove(fire)
            world.spawn(GroundItem("items.ashes".asRSCM(), 1, fire.tile))
        }

        player.animate(RSCM.NONE)
        movePlayerAwayFromFire(player)
        player.queue {
            wait(2)
            player.faceTile(logDrop.tile)
            player.unlock()
        }
    }

    private fun movePlayerAwayFromFire(player: Player) {
        for (dir in WALK_DIRECTIONS) {
            val world = player.world
            if (world.canTraverse(player.tile, dir, player)) {
                player.walkTo(player.tile.step(dir, 1))
                player.lock()
                break
            }
        }
    }

    private fun canBurn(player: Player, groundBurning: Boolean, log: Int, groundItem: GroundItem, level: Int): Boolean {
        if (groundBurning && !player.world.isSpawned(groundItem)) return false

        if (!player.inventory.contains("items.tinderbox")) {
            player.filterableMessage("You do not have any fire source to light this.")
            return false
        }

        if (player.getSkills().getCurrentLevel(Skills.FIREMAKING) < level) {
            player.filterableMessage("You need a Firemaking level of $level to burn ${getItem(log)} logs.")
            return false
        }

        val tile = player.tile
        val world = player.world
        val blocked = world.getObject(tile, type = 10) != null ||
                world.getObject(tile, type = 11) != null ||
                isWithinBankArea(player)

        if (blocked) {
            player.message("You can't light a fire here.")
            return false
        }

        return true
    }

    private fun isWithinBankArea(player: Player): Boolean {
        val (x, z) = player.tile
        return BANK_AREAS.any { (xRange, zRange) ->
            x in xRange.first..xRange.second && z in zRange.first..zRange.second
        }
    }

    private val BANK_AREAS = listOf(
        (3091 to 3098) to (3488 to 3499),  // Edgeville
        (3179 to 3194) to (3432 to 3446),  // Varrock west
        (3250 to 3257) to (3416 to 3423),  // Varrock east
        (3265 to 3272) to (3161 to 3173),  // Al Kharid
        (3088 to 3097) to (3240 to 3246),  // Draynor
        (3009 to 3018) to (3355 to 3358),  // Falador east
        (3009 to 3921) to (3353 to 3356),
        (2943 to 2949) to (3368 to 3369),  // Falador west
        (2943 to 2947) to (3370 to 3373),
        (2806 to 2812) to (3438 to 3445),  // Catherby
        (2724 to 2727) to (3487 to 3489),  // Seers' Village
        (2721 to 2730) to (3490 to 3493),
        (2612 to 2621) to (3332 to 3335),  // Ardougne north
        (2649 to 2658) to (3280 to 3287),  // Ardougne south
        (2609 to 2616) to (3088 to 3097),  // Yanille
        (3146 to 3151) to (3476 to 3481),  // Grand Exchange corners
        (3178 to 3183) to (3476 to 3481),
        (3178 to 3183) to (3502 to 3507),
        (3146 to 3151) to (3502 to 3507)
    )
}
