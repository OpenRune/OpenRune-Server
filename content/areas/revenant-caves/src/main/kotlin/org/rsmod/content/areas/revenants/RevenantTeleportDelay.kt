package org.rsmod.content.areas.revenants

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.hook.PlayerTeleportDelayHook
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.isInPvnCombat
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
internal class RevenantTeleportDelay @Inject constructor() : PlayerTeleportDelayHook {
    override fun begin(player: Player, type: TeleportType, areas: AreaChecker) { if (eligible(player, type, areas)) player.attr[START] = player.currentMapClock }
    override fun remaining(player: Player, type: TeleportType, areas: AreaChecker): Int {
        if (!eligible(player, type, areas)) return 0
        val start = player.attr[START] ?: player.currentMapClock.also { player.attr[START] = it }
        return (maxOf(start, player.attr[ATTACK] ?: start) + 2 - player.currentMapClock).coerceAtLeast(0)
    }
    override fun clear(player: Player) { player.attr.remove(START) }
    private fun eligible(player: Player, type: TeleportType, areas: AreaChecker) = type != TeleportType.Exempt && areas.inArea("area.revenant_caves", player.coords) && player.vars["varbit.wilderness_diary_hard_complete"] == 0 && player.isInPvnCombat() && !player.isInPvpCombat()
    internal companion object {
        val START = AttributeKey<Int>(resetOnDeath = true, temp = true)
        val ATTACK = AttributeKey<Int>(resetOnDeath = true, temp = true)
    }
}
internal class RevenantTeleportDelayScript @Inject constructor(private val areas: AreaChecker, private val teleports: PlayerTeleportValidator) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<PlayerHitEvents.Modify> { if (hit.isFromNpc && areas.inArea("area.revenant_caves", player.coords)) player.attr[RevenantTeleportDelay.ATTACK] = player.currentMapClock }
        onPlayerLogout { teleports.cancel(player) }
    }
}
