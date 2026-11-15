package org.rsmod.content.travel.teleports

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.table.QuetzalRow
import org.rsmod.content.travel.teleports.TravelRequirements.checkQuest
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The quetzal network in Varlamore, read straight from Jagex's `dbtable.quetzal`: each row is a
 * landing site with its name and landing tile.
 *
 * `quetzal_menu_draw_locations` draws one icon per row, in table order, as dynamic children of
 * `quetzal_menu:icons`, so an icon's subcomponent is its row's index. Each site's quetzal is a
 * multinpc on `varbit.quetzal_<site>`, which Twilight's Promise and building the site set. A site
 * can be flown to once it is `auto_unlocked` or its bit of `varp.quetzals_unlocked` is set, the
 * same test the menu uses to draw its lock. The quetzals sit in their nests, so they answer from
 * [AP_RANGE] tiles away rather than making the player path beside them, and a flight follows a
 * live capture: the screen fades as the menu closes, the minimap goes a tick later, the player
 * lands on the fourth tick and the fade lifts on the fifth.
 */
class QuetzalScript : PluginScript() {
    private val sites: List<QuetzalRow> by lazy { QuetzalRow.all() }

    override fun ScriptContext.startup() {
        for (bird in BIRDS) {
            onApNpc1(bird) { if (isWithinApRange(it.npc, AP_RANGE)) openMenu() }
            onApNpc3(bird) { if (isWithinApRange(it.npc, AP_RANGE)) lastDestination() }
        }
        onIfModalButton(ICONS) { fly(it.comsub) }
    }

    private fun ProtectedAccess.openMenu() {
        ifOpenMainModal(MENU)
        ifSetEvents(ICONS, sites.indices, IfEvent.Op1)
    }

    private suspend fun ProtectedAccess.lastDestination() {
        val last = sites.indexOfFirst { it.id == vars[LAST_DESTINATION] }
        if (last == -1) {
            mes("You have not flown anywhere by quetzal yet.")
            return
        }
        fly(last)
    }

    private suspend fun ProtectedAccess.fly(index: Int) {
        val site = sites.getOrNull(index) ?: return
        if (coords.chebyshevDistance(site.coord) <= NEARBY) {
            mes("You are already at ${site.name}.")
            return
        }
        if (!checkQuest(TWILIGHTS_PROMISE)) {
            return
        }
        if (!isUnlocked(site)) {
            mes("You have not built a landing site at ${site.name} yet.")
            return
        }
        if (actionDelay > mapClock) {
            return
        }
        ifClose()
        actionDelay = mapClock + MINIMAP_TICKS + FLIGHT_TICKS + LANDED_TICKS
        vars[LAST_DESTINATION] = site.id
        fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        delay(MINIMAP_TICKS)
        minimapHideMap()
        delay(FLIGHT_TICKS)
        telejump(site.coord)
        delay(1)
        minimapReset()
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        closeFadeOverlay(cycles = LANDED_TICKS)
    }

    private fun ProtectedAccess.isUnlocked(site: QuetzalRow): Boolean =
        site.autoUnlocked ||
            (site.id <= MAX_UNLOCK_BIT && vars[UNLOCKED_SITES] and (1 shl site.id) != 0)

    private companion object {
        private const val MENU = "interface.quetzal_menu"
        private const val ICONS = "component.quetzal_menu:icons"
        private const val LAST_DESTINATION = "varbit.quetzal_last_destination"
        private const val UNLOCKED_SITES = "varp.quetzals_unlocked"
        private const val MAX_UNLOCK_BIT = 30
        private const val TWILIGHTS_PROMISE = "quest_twilightspromise"
        private const val NEARBY = 10
        private const val AP_RANGE = 2
        private const val MINIMAP_TICKS = 1
        private const val FLIGHT_TICKS = 3
        private const val LANDED_TICKS = 2
        private const val FADE_CYCLES = 50

        /** The rideable quetzal forms; each site's multinpc shows one once its site is built. */
        private val BIRDS =
            listOf(
                "npc.quetzal_child_green",
                "npc.quetzal_child_orange",
                "npc.quetzal_child_blue",
                "npc.quetzal_child_cyan",
                "npc.quetzal_child_green_orange",
            )
    }
}
