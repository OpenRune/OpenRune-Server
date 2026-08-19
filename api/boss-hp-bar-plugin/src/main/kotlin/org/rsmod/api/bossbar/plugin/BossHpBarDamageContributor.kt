package org.rsmod.api.bossbar.plugin

import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import org.rsmod.api.bossbar.BossHpBarMode
import org.rsmod.api.config.refs.params
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.npc.hit.NpcDamageContributor
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

@Singleton
public class BossHpBarDamageContributor
@Inject
constructor(
    private val scriptProvider: Provider<BossHpBarScript>,
    private val instances: InstanceManager,
    private val playerList: PlayerList,
    private val worldClock: MapClock,
) : NpcDamageContributor {

    private data class AttackEntry(val player: Player, val npc: Npc, var lastTick: Int)

    private val tracked = HashMap<Long, AttackEntry>()

    private val script: BossHpBarScript
        get() = scriptProvider.get()

    override fun onPlayerDamageNpc(npc: Npc, source: Player, damage: Int) {
        expireStale()

        when (npcBarMode(npc)) {
            BossHpBarMode.ON_ATTACK -> handleOnAttack(source, npc)
            BossHpBarMode.ON_ENTER -> handleOnEnter(npc)
            BossHpBarMode.NEVER -> Unit
        }
    }

    private fun handleOnAttack(player: Player, npc: Npc) {
        val key = player.uuid ?: return
        val existing = tracked[key]

        if (existing == null || existing.npc !== npc) {
            if (existing != null) script.onClose(existing.player, existing.npc)
            tracked[key] = AttackEntry(player, npc, worldClock.cycle)
            script.onOpen(player, npc)
            if (existing == null) {
                player.softTimer(COMBAT_CHECK_TIMER, ATTACK_TIMEOUT_TICKS)
            }
        } else {
            existing.lastTick = worldClock.cycle
        }

        if (npc.hitpoints == 0) {
            script.onClose(player, npc)
            tracked.remove(key)
            player.clearSoftTimer(COMBAT_CHECK_TIMER)
        } else {
            script.onUpdate(player, npc)
        }
    }

    public fun checkAndExpirePlayer(player: Player) {
        val key = player.uuid ?: return
        val entry = tracked[key]
        if (entry == null) {
            player.clearSoftTimer(COMBAT_CHECK_TIMER)
            return
        }
        val expireBefore = worldClock.cycle - ATTACK_TIMEOUT_TICKS
        if (entry.lastTick < expireBefore) {
            script.onClose(entry.player, entry.npc)
            tracked.remove(key)
            player.clearSoftTimer(COMBAT_CHECK_TIMER)
        }
    }

    private fun handleOnEnter(npc: Npc) {
        val instanceId = instances.instanceForNpc(npc) ?: return
        val players = playersInInstance(instanceId)

        if (npc.hitpoints == 0) {
            for (player in players) script.onClose(player, npc)
        } else {
            for (player in players) script.onUpdate(player, npc)
        }
    }

    public fun removeTrackedForNpc(npc: Npc): List<Player> {
        val affected = mutableListOf<Player>()
        val iter = tracked.iterator()
        while (iter.hasNext()) {
            val (_, entry) = iter.next()
            if (entry.npc === npc) {
                affected += entry.player
                iter.remove()
            }
        }
        return affected
    }

    private fun expireStale() {
        val expireBefore = worldClock.cycle - ATTACK_TIMEOUT_TICKS
        val iter = tracked.iterator()
        while (iter.hasNext()) {
            val (_, entry) = iter.next()
            if (entry.lastTick < expireBefore) {
                script.onClose(entry.player, entry.npc)
                iter.remove()
            }
        }
    }

    internal fun playersInInstance(instanceId: InstanceId): List<Player> {
        val occupants = instances.sessionForId(instanceId)?.occupants ?: return emptyList()
        return playerList.filter { it.uuid in occupants }
    }

    private fun npcBarMode(npc: Npc): BossHpBarMode =
        BossHpBarMode.fromId(npc.visType.paramOrNull(params.boss_hp_bar_mode) ?: 0)

    internal companion object {
        internal const val COMBAT_CHECK_TIMER = "timer.boss_hp_bar_check"
        internal const val ATTACK_TIMEOUT_TICKS = 10
    }
}
