package org.rsmod.content.bosses.doom

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DoomSimCommand
@Inject
internal constructor(
    private val delves: DoomDelves,
    private val loot: DoomLoot,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("doomsim") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Simulate clearing delves 1..N and open the end-level loot interface"
            invalidArgs = "Use as ::doomsim <levels> [startLevel] (ex: ::doomsim 12, ::doomsim 5 8)"
            cheat { simulate() }
        }
    }

    private fun Cheat.simulate() {
        val last = args.getOrNull(0)?.toIntOrNull()?.takeIf { it > 0 }
        val first = args.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: 1
        if (last == null || first > last) {
            player.mes("Use as ::doomsim <levels> [startLevel] (ex: ::doomsim 12, ::doomsim 5 8)")
            return
        }
        val type = ServerCacheManager.getNpc(DoomNpcs.BOSS.asRSCM(RSCMType.NPC)) ?: return
        val dummy = Npc(type, player.coords)
        loot.reset(player)
        for (level in first..last) {
            delves.setLevel(player, level)
            loot.rollFor(player, dummy, recordStats = false)
        }
        delves.setLevel(player, last)
        protectedAccess.launch(player) {
            with(loot) { openEndLevel() }
            delves.resetLevel(player)
        }
    }
}
