package org.rsmod.content.travel.teleports

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.enums.NamedEnums.teleport_scroll_destinations
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TeleportScrollScript
@Inject
constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((scroll, dest) in teleport_scroll_destinations.backing.entries) {
            dest ?: continue
            val name = RSCM.getReverseMapping(RSCMType.OBJ, scroll.id)
            onOpHeld1(name) { read(name, it.slot, dest) }
        }
        onOpHeld1(REVENANT_SCROLL) { readRevenant(it.slot) }
    }

    /** The Revenant cave scroll lets the player pick one of the caves' three entrances. */
    private suspend fun ProtectedAccess.readRevenant(slot: Int) {
        val entrances = TeleportNetworks.of(REVENANT_NETWORK)
        val labels = entrances.map { it.name } + CANCEL
        val entrance = entrances.getOrNull(menu(REVENANT_TITLE, hotkeys = true, choices = labels))
        if (entrance == null) {
            return
        }
        val confirmed =
            choice2(
                "Yes, teleport me into the Wilderness.",
                true,
                "No, I will stay here.",
                false,
                title = "The ${entrance.name} is in the Wilderness. Teleport anyway?",
            )
        if (confirmed) {
            read(REVENANT_SCROLL, slot, entrance.coords)
        }
    }

    private suspend fun ProtectedAccess.read(scroll: String, slot: Int, dest: CoordGrid) {
        if (actionDelay > mapClock) {
            return
        }
        val denial = teleportValidator.validate(player, TeleportType.Standard, areaChecker)
        if (denial != null) {
            mes(denial, ChatType.Engine)
            return
        }
        if (!TeleportScrolls.takeOne(this, scroll, slot)) {
            return
        }
        ifClose()
        actionDelay = mapClock + CAST_TICKS + 1
        anim(READ_ANIM)
        spotanim(READ_SPOTANIM)
        delay(CAST_TICKS)
        telejump(dest, TeleportType.Exempt)
        resetAnim()
    }

    private companion object {
        private const val READ_ANIM = "seq.teleport_scroll_open"
        private const val READ_SPOTANIM = "spotanim.telescroll_teleport"
        private const val CAST_TICKS = 3
        private const val REVENANT_SCROLL = "obj.teleportscroll_revenants"
        private const val REVENANT_NETWORK = "revenant_cave"
        private const val REVENANT_TITLE = "Which entrance of the Revenant Caves?"
        private const val CANCEL = "Cancel"
    }
}

object TeleportScrolls {
    /** Takes one [scroll] from inventory [slot]; `false`, taking nothing, if it is not there. */
    fun takeOne(access: ProtectedAccess, scroll: String, slot: Int): Boolean {
        val obj = access.inv[slot] ?: return false
        if (RSCM.getReverseMapping(RSCMType.OBJ, obj.id) != scroll) {
            return false
        }
        return access.invDel(access.inv, scroll, count = 1, slot = slot).success
    }
}
