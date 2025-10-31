package org.alter.plugins.content.items.consumables.prayerscrolls

import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.plugin.*
import org.alter.plugins.content.mechanics.prayer.Prayers
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM

class PrayerScrollsPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onItemOption("items.raids_prayerscroll", "read") {
            player.queue {
                if (player.getVarbit(Prayers.RIGOUR_UNLOCK_VARBIT) == 1) {
                    messageBox(player,
                        "You can make out some faded words on the ancient parchment. It appears to be an archaic invocation of the gods. However there's nothing more for you to learn.",
                    )
                    return@queue
                }
                player.animate(id = "sequences.qip_watchtower_read_scroll_and_stop")
                itemMessageBox(player,
                    "You can make out some faded words on the ancient parchment. It appears to be an archaic invocation of the gods! Would you like to absorb its power? <br>(Warning: This will consume the scroll.)</b>",
                    item = "items.raids_prayerscroll",
                )
                when (options(player, "Learn Rigour", "Cancel", title = "This will consume the scroll")) {
                    1 -> {
                        if (player.inventory.contains(getRSCM("items.raids_prayerscroll"))) {
                            player.inventory.remove(item = "items.raids_prayerscroll")
                            player.setVarbit(id = Prayers.RIGOUR_UNLOCK_VARBIT, value = 1)
                            player.animate(id = RSCM.NONE)
                            itemMessageBox(player,
                                "You study the scroll and learn a new prayer: <col=8B0000>Rigour</col>",
                                item = "items.raids_prayerscroll",
                            )
                        }
                    }
                    2 -> {
                        player.animate(id = RSCM.NONE)
                    }
                }
            }
        }

        onItemOption("items.raids_prayerscroll_augury", "read") {
            player.queue {
                if (player.getVarbit(Prayers.AUGURY_UNLOCK_VARBIT) == 1) {
                    messageBox(player,
                        "You can make out some faded words on the ancient parchment. It appears to be an archaic invocation of the gods. However there's nothing more for you to learn.",
                    )
                    return@queue
                }
                player.animate(id = "sequences.qip_watchtower_read_scroll_and_stop")
                itemMessageBox(player,
                    "You can make out some faded words on the ancient parchment. It appears to be an archaic invocation of the gods! Would you like to absorb its power? <br>(Warning: This will consume the scroll.)</b>",
                    item = "items.raids_prayerscroll_augury",
                )
                when (options(player, "Learn Augury", "Cancel", title = "This will consume the scroll")) {
                    1 -> {
                        if (player.inventory.contains("items.raids_prayerscroll_augury")) {
                            player.inventory.remove(item = "items.raids_prayerscroll_augury")
                            player.setVarbit(id = Prayers.AUGURY_UNLOCK_VARBIT, value = 1)
                            player.animate(id = RSCM.NONE)
                            itemMessageBox(player,
                                "You study the scroll and learn a new prayer: <col=8B0000>Augury</col>",
                                item = "items.raids_prayerscroll_augury",
                            )
                        }
                    }
                    2 -> {
                        player.animate(id = RSCM.NONE)
                    }
                }
            }
        }

        onItemOption("items.raids_prayerscroll_preserve", "read") {
            player.queue {
                if (player.getVarbit(Prayers.PRESERVE_UNLOCK_VARBIT) == 1) {
                    messageBox(player,
                        "You can make out some faded words on the ancient parchment. It appears to be an archaic invocation of the gods. However there's nothing more for you to learn.",
                    )
                    return@queue
                }
                player.animate(id = "sequences.qip_watchtower_read_scroll_and_stop")
                itemMessageBox(player,
                    "You can make out some faded words on the ancient parchment. It appears to be an archaic invocation of the gods! Would you like to absorb its power? <br>(Warning: This will consume the scroll.)</b>",
                    item = "items.raids_prayerscroll_preserve",
                )
                when (options(player, "Learn Preserve", "Cancel", title = "This will consume the scroll")) {
                    1 -> {
                        if (player.inventory.contains("items.raids_prayerscroll_preserve")) {
                            player.inventory.remove(item = "items.raids_prayerscroll_preserve")
                            player.setVarbit(id = Prayers.PRESERVE_UNLOCK_VARBIT, value = 1)
                            player.animate(id = RSCM.NONE)
                            itemMessageBox(player,
                                "You study the scroll and learn a new prayer: <col=8B0000>Preserve</col>",
                                item = "items.raids_prayerscroll_preserve",
                            )
                        }
                    }
                    2 -> {
                        player.animate(id = RSCM.NONE)
                    }
                }
            }
        }
    }
}
