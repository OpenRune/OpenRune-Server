package org.rsmod.api.account.autosave

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import dev.openrune.types.InvScope
import org.rsmod.api.account.AccountManager
import org.rsmod.api.account.saver.request.AccountSaveResponse
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.attr.AttributeMap
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerPersistenceHints

/**
 * Queues background character saves (same pipeline as logout) on a timer and when important state
 * changes. Coalesces overlapping requests per player while a save is already in flight.
 */
@Singleton
public class PlayerAutosaveOrchestrator
@Inject
constructor(
    private val accountManager: AccountManager,
) {
    private val logger = InlineLogger()

    private val queuedSave: MutableSet<Player> =
        ConcurrentHashMap.newKeySet()

    private var periodicCounter: Int = PERIODIC_INTERVAL_CYCLES

    init {
        PlayerPersistenceHints.bind(this::onPersistenceRelevantChange)
        AttributeMap.persistenceMutationSink = { key ->
            onPersistentAttrMutated(key)
        }
    }

    /**
     * Run once per game tick after inventory updates (while [InvScope.Perm] inventories may still
     * report [org.rsmod.game.inv.Inventory.hasModifiedSlots]).
     */
    public fun processEndOfTick(players: Iterable<Player>) {
        for (player in players) {
            if (!player.canProcess) {
                continue
            }
            if (player.invMap.values.any { it.type.scope == InvScope.Perm && it.hasModifiedSlots() }) {
                onPersistenceRelevantChange(player)
            }
        }
        periodicCounter--
        if (periodicCounter <= 0) {
            periodicCounter = PERIODIC_INTERVAL_CYCLES
            for (player in players) {
                if (player.canProcess && player.persistenceSaveEligible()) {
                    requestBackgroundSave(player)
                }
            }
        }
    }

    private fun onPersistentAttrMutated(@Suppress("UNUSED_PARAMETER") key: AttributeKey<*>) {
        val player = PlayerPersistenceHints.activeOrNull() ?: return
        onPersistenceRelevantChange(player)
    }

    private fun onPersistenceRelevantChange(player: Player) {
        if (!player.persistenceSaveEligible()) {
            return
        }
        requestBackgroundSave(player)
    }

    private fun requestBackgroundSave(player: Player) {
        if (!player.persistenceSaveEligible()) {
            return
        }
        if (!queuedSave.add(player)) {
            return
        }
        accountManager.save(player) { response ->
            queuedSave.remove(player)
            when (response) {
                is AccountSaveResponse.Success -> {}
                is AccountSaveResponse.ExcessiveRetries ->
                    logger.error { "Background autosave failed after retries for ${player.username}" }
                is AccountSaveResponse.InternalShutdownError ->
                    logger.error { "Background autosave shutdown error for ${player.username}" }
            }
        }
    }

    private companion object {
        private const val PERIODIC_INTERVAL_CYCLES: Int = 500
    }
}

private fun Player.persistenceSaveEligible(): Boolean =
    accountId > 0 && characterId > 0 && processedMapClock > 0
