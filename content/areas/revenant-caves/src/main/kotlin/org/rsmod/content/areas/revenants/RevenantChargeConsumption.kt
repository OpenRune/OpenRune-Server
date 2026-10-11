package org.rsmod.content.areas.revenants

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.area.checker.isInWildernessBasic
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Consume once per actual attack, including a splash/miss and multi-hit attacks. */
internal class RevenantChargeConsumption @Inject constructor(private val players: PlayerList) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcHitEvents.Queued> {
            if (hit.isFromPlayer) hit.resolvePlayerSource(players)?.consume(npc.coords.isInWildernessBasic(), hit.righthandType()?.id)
        }
        onEvent<PlayerHitEvents.Modify> {
            if (hit.isFromPlayer) hit.sourceUid?.let { PlayerUid(it).resolve(players) }?.consume(false, hit.righthandType()?.id)
        }
    }
    private fun Player.consume(wildernessNpc: Boolean, weaponId: Int?) {
        val slot = Wearpos.RightHand.slot
        val item = worn[slot] ?: return
        val kind = RevenantCharges.kind(item) ?: return
        if (kind == RevenantCharges.bracelet || item.id != weaponId || attr[LAST_ATTACK] == currentMapClock) return
        val bow = kind.charged.contains("bow_") || kind.charged.contains("webweaver_")
        val powered = (kind.charged.contains("sceptre_") || kind.charged.contains("accursed_")) && !kind.charged.endsWith("_recol")
        if (!wildernessNpc && !bow && !powered) return
        val count = RevenantCharges.count(item)
        if (count > kind.deposit) { worn[slot] = RevenantCharges.withCount(item, count - 1); attr[LAST_ATTACK] = currentMapClock }
    }
    private companion object { val LAST_ATTACK = AttributeKey<Int>(resetOnDeath = true, temp = true) }
}
