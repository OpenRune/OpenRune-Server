package org.rsmod.content.other.grandexchange.persistence

import com.github.michaelbull.logging.InlineLogger
import com.google.inject.ImplementedBy
import jakarta.inject.Inject
import org.rsmod.api.account.AccountManager
import org.rsmod.api.account.saver.request.AccountSaveResponse
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.db.gateway.model.isOk
import org.rsmod.game.entity.Player

/** Where the exchange's state goes to be durable. Callbacks may arrive on any thread. */
@ImplementedBy(DatabaseGePersistence::class)
interface GePersistence {
    fun load(onLoaded: (GeStoredBook) -> Unit)

    fun write(rows: List<GeRow>)

    fun saveCharacter(player: Player, onSaved: (Boolean) -> Unit)
}

class DatabaseGePersistence
@Inject
constructor(private val db: GameDbManager, private val accounts: AccountManager) : GePersistence {
    private val logger = InlineLogger()

    override fun load(onLoaded: (GeStoredBook) -> Unit) {
        db.request(
            request = { connection -> GameDbResult.Ok(GeRepository.load(connection)) },
            response = { result ->
                if (result.isOk()) {
                    onLoaded(result.value)
                } else {
                    logger.error { "Grand Exchange: could not load offers, it stays closed. $result" }
                }
            },
        )
    }

    override fun write(rows: List<GeRow>) {
        db.request(
            request = { connection ->
                GeRepository.upsert(connection, rows)
                GameDbResult.Ok(Unit)
            },
            response = { result ->
                if (!result.isOk()) {
                    logger.error { "Grand Exchange: could not save ${rows.size} offers: $result" }
                }
            },
        )
    }

    override fun saveCharacter(player: Player, onSaved: (Boolean) -> Unit) {
        accounts.save(player) { response -> onSaved(response is AccountSaveResponse.Success) }
    }
}
