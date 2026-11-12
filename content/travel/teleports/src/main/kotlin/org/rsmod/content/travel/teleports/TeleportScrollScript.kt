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
