package org.rsmod.content.raids.toa.raid

import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.script.onEvent
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.specials.events.SpecialAttackEvents
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaInvocationEffectsScript : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<PlayerHitEvents.Modify> {
            if (!hit.isFromNpc) return@onEvent
            val raid = ToaRooms.roomOf(player)?.raid ?: return@onEvent
            if (raid.isActive(QUIET_PRAYERS)) hit.penetration += QUIET_PRAYERS_PENETRATION
        }
        onEvent<PlayerHitEvents.Impact> {
            if (!hit.isFromNpc || hit.damage <= 0) return@onEvent
            val raid = ToaRooms.roomOf(player)?.raid ?: return@onEvent
            if (!raid.isActive(DEADLY_PRAYERS)) return@onEvent
            val drain = hit.damage * DEADLY_PRAYERS_PERCENT / 100
            if (drain > 0) player.statSub("stat.prayer", constant = drain, percent = 0)
        }
        onEvent<SpecialAttackEvents.AdjustCost> {
            val raid = ToaRooms.roomOf(player)?.raid ?: return@onEvent
            if (raid.isActive(OVERLY_DRAINING)) cost = SpecialAttackEnergy.MAX_ENERGY
        }
    }

    private companion object {
        const val QUIET_PRAYERS = "dbrow.toa_invocation_quiet_prayers"
        const val DEADLY_PRAYERS = "dbrow.toa_invocation_deadly_prayers"
        const val OVERLY_DRAINING = "dbrow.toa_invocation_overly_draining"
        const val QUIET_PRAYERS_PENETRATION = 10
        const val DEADLY_PRAYERS_PERCENT = 20
    }
}
