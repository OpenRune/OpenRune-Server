package org.rsmod.api.bossbar.plugin

import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bossbar.BossHpBarMode
import org.rsmod.api.config.refs.params
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.npc.hit.NpcDamageContributor
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.queue.WorldQueueList

@Singleton
public class BossHpBarDamageContributor
@Inject
constructor(
    private val scriptProvider: Provider<BossHpBarScript>,
    private val instances: InstanceManager,
    private val playerList: PlayerList,
    private val worldClock: MapClock,
    private val worldQueues: WorldQueueList,
) : NpcDamageContributor {

    private data class AttackEntry(val player: Player, val npc: Npc, var lastTick: Int)

    private val tracked = HashMap<Long, AttackEntry>()

    private val closingAfterDeath: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap())

    private val script: BossHpBarScript get() = scriptProvider.get()

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

        script.onUpdate(player, npc)
        if (npc.hitpoints == 0) closeAfterDeath(npc)
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

        for (player in players) script.onUpdate(player, npc)
        if (npc.hitpoints == 0) closeAfterDeath(npc)
    }

    @OptIn(InternalApi::class)
    private fun closeAfterDeath(npc: Npc) {
        if (!closingAfterDeath.add(npc)) return
        worldQueues.add(DEATH_FADE_DELAY) {
            closingAfterDeath.remove(npc)
            if (npc.isSlotAssigned && !npc.hidden && npc.hitpoints > 0) return@add
            val players = barPlayers(npc)
            for (player in players) script.fadeOut(player)
            worldQueues.add(DEATH_HIDE_DELAY) { players.forEach(script::onDeathClose) }
        }
    }

    private fun barPlayers(npc: Npc): List<Player> =
        when (npcBarMode(npc)) {
            BossHpBarMode.ON_ATTACK ->
                removeTrackedForNpc(npc).onEach { it.clearSoftTimer(COMBAT_CHECK_TIMER) }
            BossHpBarMode.ON_ENTER ->
                instances.instanceForNpc(npc)?.let(::playersInInstance) ?: emptyList()
            BossHpBarMode.NEVER -> emptyList()
        }

    public fun isClosingAfterDeath(npc: Npc): Boolean = npc in closingAfterDeath

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
        BossHpBarMode.fromId(
            npc.visType.paramOrNull(params.boss_hp_bar_mode)
                ?: npc.type.paramOrNull(params.boss_hp_bar_mode)
                ?: 0,
        )

    internal companion object {
        internal const val COMBAT_CHECK_TIMER = "timer.boss_hp_bar_check"
        internal const val ATTACK_TIMEOUT_TICKS = 10
        private const val DEATH_FADE_DELAY = 10
        private const val DEATH_HIDE_DELAY = 4
    }
}
