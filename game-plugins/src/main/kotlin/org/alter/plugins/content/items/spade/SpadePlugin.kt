package org.alter.plugins.content.items.spade

import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.plugin.*
import org.alter.rscm.RSCM.getRSCM

class SpadePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onItemOption(item = "items.spade", "dig") {
            player.animate("sequences.human_dig")
            if (player.tile.x == 3229 && player.tile.z == 3209 && player.inventory.contains(getRSCM("items.cluequest_clue1"))) {
                player.queue {
                    player.inventory.remove(23067, 1)
                    player.inventory.add(23068, 1)
                    player.setVarp(2111, 2)
                    itemMessageBox(player, "You dig up a Treasure Scroll.", item = "items.cluequest_clue2")
                }
            } else if (player.tile.x == 3202 && player.tile.z == 3211 && player.inventory.contains(getRSCM("items.cluequest_clue2"))) {
                player.queue {
                    player.inventory.remove(23068, 1)
                    player.inventory.add(23069, 1)
                    player.setVarp(2111, 3)
                    itemMessageBox(player, "You dig up a Mysterious Orb.", item = "items.cluequest_clue3")
                }
            } else if (player.tile.x == 3108 && player.tile.z == 3264 && player.inventory.contains(getRSCM("items.cluequest_clue3"))) {
                player.queue {
                    player.inventory.remove(23069, 1)
                    player.inventory.add(23070, 1)
                    player.setVarp(2111, 4)
                    itemMessageBox(player, "You dig up a Treasure Scroll.", item = "items.cluequest_clue4")
                }
            } else if (player.tile.x == 3077 && player.tile.z == 3260 && player.inventory.contains(getRSCM("items.cluequest_clue4"))) {
                player.queue {
                    player.inventory.remove(23070, 1)
                    player.inventory.add(23071, 1)
                    player.setVarp(2111, 5)
                    itemMessageBox(player,
                        "You dig up an Ancient Casket. As you do, you hear a<br>faint whispering. You can't make out what it says<br>though...",
                        item = "items.cluequest_casket",
                    )
                    chatPlayer(player, "Hmmmm... Must have been the wind.")
                    chatPlayer(player,
                        "Anyway, this must be the treasure that Veos is after. I<br>should take it to him. If I remember right, he's docked<br>at the northernmost pier in Port Sarim.",
                    )
                }
            } else {
                player.message("Nothing interesting happens.")
            }
        }
    }
}
