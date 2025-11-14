package org.alter.plugins.content.mechanics.prayer

import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.plugin.*

class PrayersPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onPlayerDeath {
            Prayers.deactivateAll(player)
        }

        /**
         * Deactivate all prayers on log out.
         */
        onLogout {
            Prayers.deactivateAll(player)
        }

        /**
         * Activate prayers.
         */
        Prayer.values.forEach { prayer ->
            onButton(interfaceId = 541, component = prayer.child) {
                player.queue {
                    Prayers.toggle(player, this, prayer)
                }
            }
        }

        /**
         * Prayer drain.
         */
        onLogin {
            player.timers[Prayers.PRAYER_DRAIN] = 1
            // Sync quick prayer selections on login (varps are already loaded, just sync the interface state)
            Prayers.syncQuickPrayerInterface(player)
            // Sync prayer unlocks to update the interface
            Prayers.syncPrayerUnlocks(player)
        }

        onTimer(Prayers.PRAYER_DRAIN) {
            player.timers[Prayers.PRAYER_DRAIN] = 1
            Prayers.drainPrayer(player)
        }

        /**
         * Toggle quick-prayers (component 19).
         */
        onButton(interfaceId = 160, component = 19) {
            val opt = player.getInteractingOption()
            Prayers.toggleQuickPrayers(player, opt)
        }

        /**
         * Quick prayer buttons (component 20).
         * Option 1: Toggle quick prayers on/off
         * Option 2: Open quick prayer setup
         */
        onButton(interfaceId = 160, component = 20) {
            val opt = player.getInteractingOption()
            Prayers.toggleQuickPrayers(player, opt)
        }

        /**
         * Select quick-prayer.
         * Maps the slot from interface 77 component 4 to the actual prayer using quickPrayerSlot.
         * Note: If multiple prayers share the same slot, prefer Smite over Piety since Piety isn't implemented yet.
         */
        onButton(interfaceId = 77, component = 4) {
            val slot = player.getInteractingSlot()
            // Find all prayers with this slot
            val prayers = Prayer.values.filter { it.quickPrayerSlot == slot }
            if (prayers.isEmpty()) {
                return@onButton
            }
            // If multiple prayers share the slot, prefer Smite since Piety isn't implemented yet
            val prayer = prayers.firstOrNull { it != Prayer.PIETY } ?: prayers.firstOrNull() ?: return@onButton
            Prayers.selectQuickPrayer(this, prayer)
        }

        /**
         * Accept selected quick-prayer.
         */
        onButton(interfaceId = 77, component = 5) {
            player.openInterface(InterfaceDestination.PRAYER)
        }
    }
}
