package org.rsmod.api.instances.ui

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

@Singleton
public class BossCountdown @Inject constructor(private val eventBus: EventBus) {
    private val displays = WeakHashMap<Player, Display>()

    public fun show(
        player: Player,
        owner: String,
        bossName: String,
        remainingTicks: Int,
        remainingSeconds: Int = ((remainingTicks.toLong() * 600 + 999) / 1000).toInt(),
    ) {
        if (remainingTicks <= 0) {
            clear(player, owner)
            return
        }
        val previous = displays[player]
        if (previous != null && previous.owner != owner) return
        val current = Display(owner, bossName, remainingSeconds.coerceAtLeast(1))
        val opened = player.ui.containsOverlay(INTERFACE)
        if (opened && previous == current) return
        if (!opened) player.ifOpenOverlay(INTERFACE, eventBus)
        player.ifSetText(LABEL, "Next spawn:")
        player.ifSetText(TIMER, "${current.seconds}s")
        player.ifSetText(BOSS_NAME, bossName)
        displays[player] = current
    }

    public fun clear(player: Player, owner: String) {
        if (displays[player]?.owner != owner) return
        displays.remove(player)
        player.ifCloseOverlay(INTERFACE, eventBus)
    }

    private data class Display(val owner: String, val bossName: String, val seconds: Int)

    public companion object {
        public const val INTERFACE: String = "interface.barbassault_timer_overlay"
        private const val LABEL = "component.barbassault_timer_overlay:com_5"
        private const val TIMER =
            "component.barbassault_timer_overlay:barbassault_wave_complete_timer"
        private const val BOSS_NAME = "component.barbassault_timer_overlay:com_6"
    }
}
