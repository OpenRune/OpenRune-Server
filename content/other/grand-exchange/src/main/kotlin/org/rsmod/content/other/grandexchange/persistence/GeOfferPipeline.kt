package org.rsmod.content.other.grandexchange.persistence

import jakarta.inject.Inject
import jakarta.inject.Provider
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.content.other.grandexchange.GrandExchange
import org.rsmod.content.other.grandexchange.market.GeAccount
import org.rsmod.game.entity.Player

/**
 * Saves a player's offers and trade history inside their character save, so the coins and items
 * a placement or collection moves out of the inventory are written in the same transaction.
 * Offers load once for the whole world at startup, so login appends nothing. The exchange comes
 * through a provider because it needs the account manager, which needs this pipeline.
 */
class GeOfferPipeline @Inject constructor(private val exchangeProvider: Provider<GrandExchange>) :
    CharacterDataStage.Pipeline {
    private val exchange: GrandExchange by lazy { exchangeProvider.get() }

    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {}

    override fun capture(player: Player): CharacterDataStage.Save {
        if (!exchange.ready) {
            return CharacterDataStage.Save { _, _ -> }
        }
        val account = exchange.book.accountOrNull(exchange.owner(player))
            ?: return CharacterDataStage.Save { _, _ -> }
        val rows = (0 until GeAccount.SLOT_COUNT).map { GeRow.of(account, it) }
        val history = account.history.toList()
        val owner = account.owner
        return CharacterDataStage.Save { connection, _ ->
            GeRepository.upsert(connection, rows)
            GeRepository.replaceHistory(connection, owner, history)
        }
    }
}
