package org.rsmod.content.drops

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.constants
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDropReceiveHook
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.events.LootItem
import org.rsmod.api.player.events.LootSource
import org.rsmod.api.player.events.PlayerLootEvents
import org.rsmod.api.player.events.ReceivedLoot
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

/**
 * Hands rolled loot to a player and publishes [PlayerLootEvents] around it. Pets from any source go
 * to [PetRewards]. Npc sources also pass through the receive and consume hooks (bonecrusher, Gauntlet
 * shards) and the loot tracker.
 *
 * [collectionLog] false leaves the log to the caller, for loot that can still be lost (Doom's pile).
 * Pets are logged either way, since they are given on the spot.
 */
@Singleton
public class LootDelivery
@Inject
constructor(
    private val areaChecker: AreaChecker,
    private val objRepo: ObjRepository,
    private val random: GameRandom,
    private val pets: PetRewards,
    // Providers, because hook implementations can depend on LootDelivery (Gauntlet's shard hook).
    private val deathDropHooks: Provider<Set<NpcDeathDropHook>>,
    private val receiveHooks: Provider<Set<NpcDropReceiveHook>>,
    private val eventBus: EventBus,
) {
    public fun give(
        player: Player,
        source: LootSource,
        drops: List<DropRollItem>,
        destination: Inventory? = player.inv,
        coords: CoordGrid = player.coords,
        lootTrackerEventId: Int? = null,
        collectionLog: Boolean = true,
    ): List<ReceivedLoot> =
        give(
            player,
            source,
            resolve(player, source, drops, destination),
            coords,
            lootTrackerEventId,
            collectionLog,
        )

    /** Gives [items] from [resolve], for callers that adjust the items before anyone else sees them. */
    public fun give(
        player: Player,
        source: LootSource,
        items: MutableList<LootItem>,
        coords: CoordGrid = player.coords,
        lootTrackerEventId: Int? = null,
        collectionLog: Boolean = true,
    ): List<ReceivedLoot> {
        if (items.isEmpty()) {
            return emptyList()
        }
        eventBus.publish(PlayerLootEvents.Prepare(player, source, items))
        val duration = player.lootDropDuration ?: constants.lootdrop_duration
        val received =
            items
                .filter { it.count > 0 }
                .map {
                    deliver(player, source, it, coords, duration, lootTrackerEventId, collectionLog)
                }
        if (
            received.isNotEmpty() &&
                eventBus.unbound[PlayerLootEvents.Received::class.java] != null
        ) {
            eventBus.publish(PlayerLootEvents.Received(player, source, received))
        }
        return received
    }

    public fun resolve(
        player: Player,
        source: LootSource,
        drops: List<DropRollItem>,
        destination: Inventory? = player.inv,
    ): MutableList<LootItem> {
        val npc = (source as? LootSource.Npc)?.npc
        val items = ArrayList<LootItem>()
        fun add(drop: DropRollItem) {
            if (drop.isNothing || !drop.condition(player)) {
                return
            }
            val killCondition = drop.killCondition
            if (npc != null && killCondition != null && !killCondition(player, npc, areaChecker)) {
                return
            }
            val item = drop.transformObj(player) ?: drop.obj
            items += LootItem(item, drop.rollCount(random), destination)
            drop.bonusDrops.forEach(::add)
        }
        drops.forEach(::add)
        return items
    }

    private fun deliver(
        player: Player,
        source: LootSource,
        loot: LootItem,
        coords: CoordGrid,
        duration: Int,
        lootTrackerEventId: Int?,
        collectionLog: Boolean,
    ): ReceivedLoot {
        val obj = loot.item
        val count = loot.count
        if (pets.give(player, obj)) {
            CollectionLog.grant(player, obj, count)
            return ReceivedLoot(obj, count, null)
        }
        if (collectionLog) {
            CollectionLog.grant(player, obj, count)
        }
        val npc = (source as? LootSource.Npc)?.npc
        if (npc != null && takenByHook(player, obj, count, coords, duration)) {
            return ReceivedLoot(obj, count, null)
        }
        val inventory = loot.destination?.takeIf { player.add(it, obj, count) }
        if (inventory == null) {
            objRepo.add(obj, coords, duration, player, count)
        }
        if (npc != null && lootTrackerEventId != null) {
            trackLoot(player, npc, lootTrackerEventId, obj, count)
        }
        return ReceivedLoot(obj, count, inventory)
    }

    private fun Player.add(inv: Inventory, obj: String, count: Int): Boolean =
        invAdd(inv, obj, count, ignoreVirtualStorage = true).success

    private fun trackLoot(player: Player, npc: Npc, eventId: Int, obj: String, count: Int) {
        ClientScripts.lootTrackerAddLoot(player, npc.id, eventId, obj.asRSCM(RSCMType.OBJ), count)
    }

    private fun takenByHook(
        player: Player,
        obj: String,
        count: Int,
        coords: CoordGrid,
        duration: Int,
    ): Boolean {
        if (receiveHooks.get().any { it.tryReceive(player, obj, count) }) {
            return true
        }
        if (count != 1) {
            return false
        }
        val type = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)) ?: return false
        val context = NpcDeathDropContext(player, type, coords, duration, objRepo)
        return deathDropHooks.get().any { it.tryConsume(context) }
    }
}
