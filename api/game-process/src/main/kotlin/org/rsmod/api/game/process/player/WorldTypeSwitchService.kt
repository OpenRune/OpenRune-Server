package org.rsmod.api.game.process.player

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.types.InvScope
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import org.rsmod.api.account.AccountManager
import org.rsmod.api.account.loader.request.AccountLoadResponse
import org.rsmod.api.account.saver.request.AccountSaveResponse
import org.rsmod.api.player.isLogoutBlocked
import org.rsmod.api.player.logoutBlockedMessage
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.realm.Realm
import org.rsmod.api.registry.account.AccountRegistry
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.entity.player.WorldTypeChangedEvent
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Swaps an online player's save for another world type's without dropping their connection: write
 * out the current mode, read in the target mode, then replace the player's mode-scoped state with
 * what was read and re-run the login sequence against it.
 *
 * Each step hops threads - the save runs on the saver service, the load on the loader service, and
 * the swap itself back on the game thread through [AccountRegistry].
 *
 * See `docs/world-types.md` for why the swap does each of the things it does; several of the steps
 * are load-bearing in ways that are not obvious from reading them.
 */
@Singleton
public class WorldTypeSwitchService
@Inject
constructor(
    private val accountManager: AccountManager,
    private val accountRegistry: AccountRegistry,
    private val eventBus: EventBus,
    private val collision: CollisionFlagMap,
    private val realm: Realm,
) {
    private val logger = InlineLogger()

    private val inFlight: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val pendingFailures = ConcurrentLinkedQueue<FailedSwitch>()

    private data class FailedSwitch(val player: Player, val message: String)

    private var Player.newAccount: Boolean by boolVarBit("varbit.new_player_account")

    public sealed class Result {
        public data object Started : Result()

        public data object AlreadyOnWorldType : Result()

        public data object NotServed : Result()

        public data object AlreadySwitching : Result()

        public data class Busy(val message: String) : Result()
    }

    public fun isSwitching(player: Player): Boolean = inFlight.contains(player.characterId)

    /**
     * Begins a switch to [target], or explains why it cannot start.
     *
     * [served] is the set this world serves. [firstVisitSpawn] is where to put the player when they
     * have never played [target], since there is no saved position to restore; `null` keeps whatever
     * the load defaulted to.
     */
    public fun switch(
        player: Player,
        target: WorldType,
        served: Collection<WorldType>,
        firstVisitSpawn: CoordGrid? = null,
    ): Result {
        if (player.worldType == target) {
            return Result.AlreadyOnWorldType
        }
        if (target !in served) {
            return Result.NotServed
        }
        if (player.characterId <= 0 || !inFlight.add(player.characterId)) {
            return Result.AlreadySwitching
        }
        if (!canSwitchNow(player)) {
            inFlight.remove(player.characterId)
            return Result.Busy(player.logoutBlockedMessage())
        }
        accountManager.save(player) { response ->
            onCurrentModeSaved(player, target, firstVisitSpawn, response)
        }
        return Result.Started
    }

    /** Shared with the logout path so the two cannot drift; a switch discards the same state. */
    private fun canSwitchNow(player: Player): Boolean {
        if (player.loggingOut || player.pendingLogout || player.forceDisconnect) {
            return false
        }
        return !player.isLogoutBlocked()
    }

    /** Saver thread. */
    private fun onCurrentModeSaved(
        player: Player,
        target: WorldType,
        firstVisitSpawn: CoordGrid?,
        response: AccountSaveResponse,
    ) {
        if (response !is AccountSaveResponse.Success) {
            abort(player, target, "save failed (${response::class.simpleName})")
            return
        }
        val queued =
            accountManager.loadWorldTypeSwitch(player.username, target) { loadResponse ->
                onTargetModeLoaded(player, target, firstVisitSpawn, loadResponse)
            }
        if (!queued) {
            abort(player, target, "loader rejected the request")
        }
    }

    /** Loader thread. */
    private fun onTargetModeLoaded(
        player: Player,
        target: WorldType,
        firstVisitSpawn: CoordGrid?,
        response: AccountLoadResponse,
    ) {
        if (response !is AccountLoadResponse.Ok) {
            abort(player, target, "load failed ($response)")
            return
        }
        accountRegistry.queueLogin(player, response) { queued, ok ->
            applySwitch(queued, ok, firstVisitSpawn)
        }
    }

    /** Game thread, via [AccountRegistry.handleLogins]. */
    private fun applySwitch(
        player: Player,
        response: AccountLoadResponse.Ok,
        firstVisitSpawn: CoordGrid?,
    ) {
        val target = response.worldType
        // The save and load take cycles, so the guard is re-checked against the player as they are
        // now rather than as they were when they asked.
        if (!canSwitchNow(player)) {
            inFlight.remove(player.characterId)
            player.mes(player.logoutBlockedMessage())
            return
        }
        try {
            val origin = player.coords
            val from = player.worldType
            val before = snapshot(player)

            player.persistenceSuspended = true

            clearWorldTypeScopedState(player)
            for (transform in response.transforms) {
                transform.apply(player)
            }
            player.worldType = target
            if (response.firstVisitToWorldType) {
                player.newAccount = true
            }
            // The applier reloads `modLevel` from the account row, so a dev realm has to re-grant.
            if (realm.config.devMode) {
                player.modLevel = Rights.ADMINISTRATOR
            }
            val after = snapshot(player)

            // The appliers assign `coords` outright, which skips the zone bookkeeping a registered
            // player needs; put them back and move through the engine instead.
            val loaded = player.coords
            val destination =
                if (response.firstVisitToWorldType) firstVisitSpawn ?: loaded else loaded
            player.coords = origin
            telejumpTo(player, destination)

            // A switch usually lands on the same tile, and the scene is otherwise only resent when
            // the build area changes.
            player.clientCycle.forceSceneRebuild()

            val prepareLogin = eventBus.publish(SessionStateEvent.PrepareLogin(player))
            val initialize = eventBus.publish(SessionStateEvent.Initialize(player))
            val login = eventBus.publish(SessionStateEvent.Login(player))
            val engineLogin = eventBus.publish(SessionStateEvent.EngineLogin(player))

            eventBus.publish(WorldTypeChangedEvent(player, from = from, to = target))

            player.mes("You are now playing ${target.label}.")
            logger.info {
                "World type switched: player=${player.username} from='${from.key}' " +
                    "to='${target.key}' firstVisit=${response.firstVisitToWorldType} " +
                    "coords=$destination transforms=${response.transforms.size} " +
                    "events(prepare/init/login/engine)=" +
                    "$prepareLogin/$initialize/$login/$engineLogin " +
                    "state[$before -> $after]"
            }
        } catch (e: Exception) {
            // A half-swapped player holds a mix of two saves; the outgoing one is already written.
            player.forceDisconnect = true
            logger.error(e) {
                "Could not switch world type, disconnecting: player=${player.username} " +
                    "target='${target.key}'"
            }
        } finally {
            player.persistenceSuspended = false
            inFlight.remove(player.characterId)
        }
    }

    private fun telejumpTo(player: Player, destination: CoordGrid) {
        if (!collision.isZoneValid(destination)) {
            logger.warn {
                "World-type switch destination is not a valid zone, keeping current coords: " +
                    "player=${player.username} dest=$destination"
            }
            return
        }
        PathingEntityCommon.telejump(player, collision, destination)
    }

    private fun snapshot(player: Player): String {
        var objs = 0
        for (inventory in player.invMap.values) {
            if (inventory.type.scope != InvScope.Perm) {
                continue
            }
            for (slot in inventory.indices) {
                if (inventory[slot] != null) {
                    objs++
                }
            }
        }
        return "objs=$objs varps=${player.vars.backing.size} coords=${player.coords}"
    }

    /**
     * The appliers merge rather than replace, so anything the incoming save has no row for has to be
     * dropped first - for inventories that would otherwise mean items crossing between modes.
     *
     * Only `Perm` scope inventories are touched, matching what the save pipeline persists.
     */
    private fun clearWorldTypeScopedState(player: Player) {
        for (inventory in player.invMap.values) {
            if (inventory.type.scope != InvScope.Perm) {
                continue
            }
            for (slot in inventory.indices) {
                inventory[slot] = null
            }
        }
        player.statMap.clear()
        player.vars.backing.clear()
        player.attr.clear()
    }

    /** Game thread. Delivers what the save and load callbacks could not say from their own. */
    public fun processPendingFailures() {
        while (true) {
            val failed = pendingFailures.poll() ?: break
            failed.player.mes(failed.message)
        }
    }

    private fun abort(player: Player, target: WorldType, reason: String) {
        inFlight.remove(player.characterId)
        logger.warn {
            "Could not switch world type: player=${player.username} " +
                "from='${player.worldType.key}' target='${target.key}' ($reason)"
        }
        pendingFailures += FailedSwitch(player, "Could not switch world type right now.")
    }
}
