package org.alter.items.teleports

import org.alter.api.ext.message
import org.alter.api.ext.prepareForTeleport
import org.alter.game.model.LockState
import org.alter.game.model.Tile
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemClickEvent
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.columnOptional
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType
import org.alter.rscm.RSCM.asRSCM

class TeleportTabEvents : PluginEvent() {

    override fun init() {
        table("tables.teleport_tablets").forEach { tablet ->
            val itemId = tablet.column("columns.teleport_tablets:item", ObjType)
            val location = tablet.columnOptional("columns.teleport_tablets:location", IntType)

            on<ItemClickEvent> {
                where { item == itemId }
                then {
                    when (option) {
                        MenuOption.OP2 -> location?.let {
                            teleport(player, item, it)
                        } ?: teleportSpecial(player, item, MenuOption.OP3)

                        MenuOption.OP3,MenuOption.OP4 -> teleportSpecial(player, item, option)
                        else -> player.message("Unhandled teleport tablet option: ${option.name.lowercase()} ($item)")
                    }
                }
            }
        }
    }

    private fun teleportSpecial(player: Player, item: Int, option: MenuOption) {
        val targetLocation = when {
            item == "items.poh_tablet_varrockteleport".asRSCM() && option == MenuOption.OP3 -> Tile(3213, 3424)
            item == "items.poh_tablet_watchtowerteleport".asRSCM() && option == MenuOption.OP3 -> Tile(2549, 3112, 2)
            item == "items.poh_tablet_varrockteleport".asRSCM() && option == MenuOption.OP4 -> Tile(3165, 3479)
            item == "items.poh_tablet_watchtowerteleport".asRSCM() && option == MenuOption.OP4 -> Tile(2584, 3097)
            else -> null
        }

        targetLocation?.let { teleport(player, item, it.as30BitInteger) }
    }

    private fun teleport(player: Player, item: Int, location: Int) {
        player.queue {
            if (!player.inventory.contains(item)) return@queue

            player.inventory.remove(item)
            player.prepareForTeleport()
            player.lock = LockState.FULL_WITH_DAMAGE_IMMUNITY

            player.animate("sequences.poh_smash_magic_tablet".asRSCM(), delay = 16)
            player.playSound("jingles.artistry".asRSCM(), volume = 1, delay = 15)

            wait(cycles = 3)
            player.graphic("spotanims.poh_absorb_tablet_magic".asRSCM())
            player.animate("sequences.poh_absorb_tablet_teleport".asRSCM())

            wait(cycles = 2)
            player.animate(-1)
            player.unlock()

            val destination = Tile.from30BitHash(location).radius(2).random()
            player.moveTo(destination)
        }
    }
}
