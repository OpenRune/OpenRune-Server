package org.rsmod.content.other.home

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class HomeTeleporterScript @Inject constructor(
    private val npcs: NpcRepository,
    private val menu: HomeTeleportMenu,
    private val travel: HomeTravel,
    private val access: ProtectedAccessLauncher,
) : PluginScript() {
    private var wizard: Npc? = null
    override fun ScriptContext.startup() {
        onGameStartup {
            val type = checkNotNull(ServerCacheManager.getNpc(TeleportCatalogue.WIZARD_TYPE.asRSCM()))
            wizard = Npc(type, TeleportCatalogue.WIZARD).also { npcs.add(it, Int.MAX_VALUE) }
        }
        onOpNpc1(TeleportCatalogue.WIZARD_TYPE) { menu.open(this) }
        onCommand("home") {
            desc = "Teleport to Edgeville home"
            cheat { access.launch(player) { travel.travel(this, TeleportCatalogue.all.single { it.key == "edgeville" }) } }
        }
        onCommand("teleports") {
            desc = "Open the home teleport catalogue"
            cheat { access.launch(player) { menu.open(this) } }
        }
    }
    override fun ScriptContext.shutdown() {
        wizard?.takeIf { it.isSlotAssigned }?.let { npcs.del(it, Int.MAX_VALUE) }
        wizard = null
    }
}
