package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.table.QuetzalRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The quetzal network in Varlamore, read straight from Jagex's `dbtable.quetzal`: each row is a
 * landing site with its name and landing tile.
 *
 * `quetzal_menu_draw_locations` draws one icon per row, in table order, as dynamic children of
 * `quetzal_menu:icons`, so an icon's subcomponent is its row's index. Each site's quetzal is a
 * multinpc on `varbit.quetzal_<site>`, invisible until its landing site is built; every site is
 * marked built on login (see docs/quirks.md).
 */
class QuetzalScript : PluginScript() {
    private val sites: List<QuetzalRow> by lazy { QuetzalRow.all() }

    override fun ScriptContext.startup() {
        onPlayerLogin { player.buildLandingSites() }
        for (bird in BIRDS) {
            onOpNpc1(bird) { openMenu() }
            onOpNpc3(bird) { lastDestination() }
        }
        onIfModalButton(ICONS) { fly(it.comsub) }
    }

    private fun Player.buildLandingSites() {
        for (site in sites) {
            val varbit = site.varbit()
            val built = if (varbit == FORTIS) FORTIS_BUILT else BUILT
            if (vars[varbit] < built) {
                VarPlayerIntMapSetter.set(this, varbit.varbitType(), built)
            }
        }
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
        if (actionDelay > mapClock) {
            return
        }
        ifClose()
        actionDelay = mapClock + FLIGHT_TICKS + 2
        mes("You climb onto the quetzal and fly to ${site.name}.")
        fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        delay(FLIGHT_TICKS)
        telejump(site.coord)
        vars[LAST_DESTINATION] = site.id
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        closeFadeOverlay(cycles = 2)
    }

    private fun QuetzalRow.varbit(): String =
        RSCM.getReverseMapping(RSCMType.DBROW, rowId).replace("dbrow.", "varbit.")

    private fun String.varbitType() =
        checkNotNull(ServerCacheManager.getVarbit(RSCM.getRSCM(this))) { this }

    private companion object {
        private const val MENU = "interface.quetzal_menu"
        private const val ICONS = "component.quetzal_menu:icons"
        private const val LAST_DESTINATION = "varbit.quetzal_last_destination"
        private const val FORTIS = "varbit.quetzal_fortis"
        private const val FORTIS_BUILT = 2
        private const val BUILT = 1
        private const val NEARBY = 10
        private const val FLIGHT_TICKS = 4
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
