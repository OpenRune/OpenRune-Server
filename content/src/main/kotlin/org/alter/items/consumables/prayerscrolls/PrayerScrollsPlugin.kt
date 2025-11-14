package org.alter.items.consumables.prayerscrolls

import org.alter.api.ClientScript
import org.alter.api.ext.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemOption
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM

class PrayerScrollsPlugin : PluginEvent() {

    companion object {
        private const val RIGOUR_UNLOCK_VARBIT = "varbits.prayer_rigour_unlocked"
        private const val AUGURY_UNLOCK_VARBIT = "varbits.prayer_augury_unlocked"
        private const val PRESERVE_UNLOCK_VARBIT = "varbits.prayer_preserve_unlocked"
    }

    override fun init() {
        onItemOption("items.raids_prayerscroll", "read") {
            player.queue {
                if (player.getVarbit(RIGOUR_UNLOCK_VARBIT) == 1) {
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
                            player.setVarbit(id = RIGOUR_UNLOCK_VARBIT, value = 1)
                            // Run client script to update prayer interface
                            player.runClientScript(ClientScript(id = 2158))
                            player.animate(RSCM.NONE)
                            itemMessageBox(player,
                                "You study the scroll and learn a new prayer: <col=8B0000>Rigour</col>",
                                item = "items.raids_prayerscroll",
                            )
                        }
                    }
                    2 -> {
                        player.animate(RSCM.NONE)
                    }
                }
            }
        }

        onItemOption("items.raids_prayerscroll_augury", "read") {
            player.queue {
                if (player.getVarbit(AUGURY_UNLOCK_VARBIT) == 1) {
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
                            player.setVarbit(id = AUGURY_UNLOCK_VARBIT, value = 1)
                            // Run client script to update prayer interface
                            player.runClientScript(ClientScript(id = 2158))
                            player.animate(RSCM.NONE)
                            itemMessageBox(player,
                                "You study the scroll and learn a new prayer: <col=8B0000>Augury</col>",
                                item = "items.raids_prayerscroll_augury",
                            )
                        }
                    }
                    2 -> {
                        player.animate(RSCM.NONE)
                    }
                }
            }
        }

        onItemOption("items.raids_prayerscroll_preserve", "read") {
            player.queue {
                if (player.getVarbit(PRESERVE_UNLOCK_VARBIT) == 1) {
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
                            player.setVarbit(id = PRESERVE_UNLOCK_VARBIT, value = 1)
                            // Run client script to update prayer interface
                            player.runClientScript(ClientScript(id = 2158))
                            player.animate(RSCM.NONE)
                            itemMessageBox(player,
                                "You study the scroll and learn a new prayer: <col=8B0000>Preserve</col>",
                                item = "items.raids_prayerscroll_preserve",
                            )
                        }
                    }
                    2 -> {
                        player.animate(RSCM.NONE)
                    }
                }
            }
        }
    }
}
