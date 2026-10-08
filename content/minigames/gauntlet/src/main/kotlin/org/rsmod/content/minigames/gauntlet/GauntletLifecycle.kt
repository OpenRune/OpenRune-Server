package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.instances.events.InstancePlayerLeaveEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletLifecycle
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val bossEntry: GauntletBossEntry,
    private val bossHpBar: BossHpBarScript,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftTimer(GauntletRuns.TIME_LIMIT_TIMER) {
            player.clearSoftTimer(GauntletRuns.TIME_LIMIT_TIMER)
            protectedAccess.launch(player) { with(bossEntry) { begin() } }
        }
        onEvent<InstancePlayerLeaveEvent>(instanceEventId(GauntletRuns.KEY)) {
            player.clearSoftTimer(GauntletRuns.TIME_LIMIT_TIMER)
            GauntletHolding.restore(player)
            player.ifCloseOverlay(GauntletRuns.OVERLAY, eventBus)
            if (player.gauntletBossStarted) bossHpBar.onDeathClose(player)
            player.inGauntlet = false
            player.gauntletCorrupted = false
            player.gauntletStart = 0
            player.gauntletBossStarted = false
        }
    }
}
