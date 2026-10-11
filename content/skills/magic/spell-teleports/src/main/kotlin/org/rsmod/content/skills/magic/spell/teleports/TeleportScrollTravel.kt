package org.rsmod.content.skills.magic.spell.teleports

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

class TeleportScrollTravel @Inject constructor(private val teleports: PlayerTeleportValidator, private val areas: AreaChecker, private val collision: CollisionFlagMap) {
    suspend fun travel(access: ProtectedAccess, scroll: TeleportScroll, valid: () -> Boolean, consume: () -> Boolean) = with(access) {
        if (actionDelay > mapClock || !valid() || !allowed(scroll)) return
        val destination = if (scroll == TeleportScroll.Revenants) {
            val selected = choice4(
                "Northern entrance", scroll.destination,
                "Middle entrance", TeleportScrollScript.REVENANT_MIDDLE,
                "Southern entrance", TeleportScrollScript.REVENANT_SOUTH,
                "Cancel", null,
            )
            ifCloseChat()
            selected ?: return
        } else scroll.destination
        if (scroll == TeleportScroll.Revenants && player.vars["varp.revenant_scroll_nowarn"] == 0) {
            mesbox("This teleport takes you into the Wilderness, where other players can attack you.")
            val confirmed = choice2("Teleport", true, "Cancel", false)
            ifCloseChat()
            if (!confirmed || !allowed(scroll, destination)) return
        }
        beginTeleport()
        val origin = coords
        actionDelay = mapClock + 3
        resetAnim()
        resetSpotanim()
        anim("seq.teleport_scroll_open")
        spotanim("spotanim.telescroll_teleport")
        try {
            delay(2)
            if (coords != origin || !valid() || !allowed(scroll, destination)) return
            val arrive = {
                // Keep the scroll until the cave delay actually activates, including late blocks.
                if (coords == origin && valid() && allowed(scroll, destination) &&
                    consume()) {
                    PathingEntityCommon.telejump(player, collision, destination)
                    resetAnim(); resetSpotanim()
                }
            }
            if (!teleports.defer(player, TeleportType.Standard, areas, arrive)) arrive()
        } finally {
            resetSpotanim()
            if (player.hitpoints > 0) resetAnim()
        }
    }

    private fun ProtectedAccess.allowed(scroll: TeleportScroll, destination: CoordGrid = scroll.destination): Boolean {
        if (player.pendingLogout || player.loggingOut || player.hitpoints == 0) return false
        if (scroll == TeleportScroll.GuthixianTemple && !QuestRequirements.hasCompleted(player, "quest.whileguthixsleeps")) {
            mes("You must complete While Guthix Sleeps to enter the Ancient Guthixian Temple.")
            return false
        }
        val denial = teleports.validate(player, TeleportType.Standard, areas)
        if (denial != null) {
            mes(denial)
            return false
        }
        if (!collision.isZoneValid(destination)) {
            mes("That teleport destination is unavailable.")
            return false
        }
        return true
    }
}
