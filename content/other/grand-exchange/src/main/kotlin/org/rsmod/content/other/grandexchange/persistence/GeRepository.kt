package org.rsmod.content.other.grandexchange.persistence

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import java.sql.ResultSet
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.content.other.grandexchange.market.GeAccount
import org.rsmod.content.other.grandexchange.market.GeHistoryEntry
import org.rsmod.content.other.grandexchange.market.GeOffer
import org.rsmod.content.other.grandexchange.market.GeOfferType
import org.rsmod.content.other.grandexchange.market.GeOwner

/** A detached copy of one slot, safe to hand to a database thread. */
data class GeRow(
    val owner: GeOwner,
    val slot: Int,
    val version: Long,
    val offer: Snapshot?,
) {
    data class Snapshot(
        val type: GeOfferType,
        val obj: Int,
        val price: Long,
        val count: Int,
        val seq: Long,
        val completed: Int,
        val gold: Long,
        val escrowCoins: Long,
        val aborted: Boolean,
        val collectItems: Int,
        val collectCoins: Long,
    ) {
        fun toOffer(owner: GeOwner, slot: Int): GeOffer =
            GeOffer(owner, slot, type, obj, price, count, seq).also {
                it.completed = completed
                it.gold = gold
                it.escrowCoins = escrowCoins
                it.aborted = aborted
                it.collectItems = collectItems
                it.collectCoins = collectCoins
            }
    }

    companion object {
        fun of(account: GeAccount, slot: Int): GeRow {
            val offer = account.slots[slot]
            val snapshot =
                offer?.let {
                    Snapshot(
                        it.type,
                        it.obj,
                        it.price,
                        it.count,
                        it.seq,
                        it.completed,
                        it.gold,
                        it.escrowCoins,
                        it.aborted,
                        it.collectItems,
                        it.collectCoins,
                    )
                }
            return GeRow(account.owner, slot, account.versions[slot], snapshot)
        }
    }
}

data class GeStoredBook(
    val rows: List<GeRow>,
    val history: Map<GeOwner, List<GeHistoryEntry>>,
)

/**
 * SQL for the `ge_offers` and `ge_history` tables. They sit in `public`, keyed by world type, so
 * every gameplay mode's character saves can reach them whatever their schema search path.
 * Offer rows carry a version and an update only lands if it is newer than the stored one, which
 * keeps a late write from undoing a newer one regardless of which thread commits first.
 */
object GeRepository {
    fun createTables(connection: DatabaseConnection) {
        connection.prepareStatement(CREATE_OFFERS).use { it.executeUpdate() }
        connection.prepareStatement(CREATE_HISTORY).use { it.executeUpdate() }
    }

    fun load(connection: DatabaseConnection): GeStoredBook {
        createTables(connection)
        val rows = ArrayList<GeRow>()
        connection.prepareStatement(SELECT_OFFERS).use { statement ->
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    rows += readRow(rs) ?: continue
                }
            }
        }
        val history = LinkedHashMap<GeOwner, MutableList<GeHistoryEntry>>()
        connection.prepareStatement(SELECT_HISTORY).use { statement ->
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    val owner = GeOwner(rs.getString("world_type"), rs.getInt("character_id"))
                    val obj = decodeObj(rs.getString("obj")) ?: continue
                    history.getOrPut(owner) { ArrayList() } +=
                        GeHistoryEntry(
                            GeOfferType.entries[rs.getInt("type")],
                            obj,
                            rs.getInt("count"),
                            rs.getLong("gold"),
                        )
                }
            }
        }
        return GeStoredBook(rows, history)
    }

    fun upsert(connection: DatabaseConnection, rows: Collection<GeRow>) {
        if (rows.isEmpty()) {
            return
        }
        connection.prepareStatement(UPSERT_OFFER).use { statement ->
            for (row in rows) {
                val offer = row.offer
                statement.setString(1, row.owner.worldType)
                statement.setInt(2, row.owner.characterId)
                statement.setInt(3, row.slot)
                statement.setLong(4, row.version)
                statement.setBoolean(5, offer != null)
                statement.setInt(6, offer?.type?.ordinal ?: 0)
                statement.setString(7, offer?.let { RSCM.getReverseMapping(RSCMType.OBJ, it.obj) })
                statement.setLong(8, offer?.price ?: 0)
                statement.setInt(9, offer?.count ?: 0)
                statement.setLong(10, offer?.seq ?: 0)
                statement.setInt(11, offer?.completed ?: 0)
                statement.setLong(12, offer?.gold ?: 0)
                statement.setLong(13, offer?.escrowCoins ?: 0)
                statement.setBoolean(14, offer?.aborted ?: false)
                statement.setInt(15, offer?.collectItems ?: 0)
                statement.setLong(16, offer?.collectCoins ?: 0)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    fun replaceHistory(
        connection: DatabaseConnection,
        owner: GeOwner,
        history: List<GeHistoryEntry>,
    ) {
        connection.prepareStatement(DELETE_HISTORY).use {
            it.setString(1, owner.worldType)
            it.setInt(2, owner.characterId)
            it.executeUpdate()
        }
        if (history.isEmpty()) {
            return
        }
        connection.prepareStatement(INSERT_HISTORY).use { statement ->
            history.forEachIndexed { index, entry ->
                statement.setString(1, owner.worldType)
                statement.setInt(2, owner.characterId)
                statement.setInt(3, index)
                statement.setInt(4, entry.type.ordinal)
                statement.setString(5, RSCM.getReverseMapping(RSCMType.OBJ, entry.obj))
                statement.setInt(6, entry.count)
                statement.setLong(7, entry.gold)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun readRow(rs: ResultSet): GeRow? {
        val owner = GeOwner(rs.getString("world_type"), rs.getInt("character_id"))
        val slot = rs.getInt("slot")
        val version = rs.getLong("version")
        if (!rs.getBoolean("occupied")) {
            return GeRow(owner, slot, version, null)
        }
        val obj = decodeObj(rs.getString("obj")) ?: return null
        val snapshot =
            GeRow.Snapshot(
                type = GeOfferType.entries[rs.getInt("type")],
                obj = obj,
                price = rs.getLong("price"),
                count = rs.getInt("count"),
                seq = rs.getLong("seq"),
                completed = rs.getInt("completed"),
                gold = rs.getLong("gold"),
                escrowCoins = rs.getLong("escrow_coins"),
                aborted = rs.getBoolean("aborted"),
                collectItems = rs.getInt("collect_items"),
                collectCoins = rs.getLong("collect_coins"),
            )
        return GeRow(owner, slot, version, snapshot)
    }

    private fun decodeObj(key: String?): Int? {
        if (key.isNullOrBlank()) {
            return null
        }
        return key.toIntOrNull() ?: RSCM.getRSCMOrNull(key, RSCMType.OBJ)
    }

    private const val CREATE_OFFERS =
        "CREATE TABLE IF NOT EXISTS public.ge_offers (" +
            "world_type TEXT NOT NULL, " +
            "character_id INT NOT NULL, " +
            "slot INT NOT NULL, " +
            "version BIGINT NOT NULL, " +
            "occupied BOOLEAN NOT NULL, " +
            "type INT NOT NULL DEFAULT 0, " +
            "obj TEXT, " +
            "price BIGINT NOT NULL DEFAULT 0, " +
            "count INT NOT NULL DEFAULT 0, " +
            "seq BIGINT NOT NULL DEFAULT 0, " +
            "completed INT NOT NULL DEFAULT 0, " +
            "gold BIGINT NOT NULL DEFAULT 0, " +
            "escrow_coins BIGINT NOT NULL DEFAULT 0, " +
            "aborted BOOLEAN NOT NULL DEFAULT FALSE, " +
            "collect_items INT NOT NULL DEFAULT 0, " +
            "collect_coins BIGINT NOT NULL DEFAULT 0, " +
            "PRIMARY KEY (world_type, character_id, slot))"

    private const val CREATE_HISTORY =
        "CREATE TABLE IF NOT EXISTS public.ge_history (" +
            "world_type TEXT NOT NULL, " +
            "character_id INT NOT NULL, " +
            "idx INT NOT NULL, " +
            "type INT NOT NULL, " +
            "obj TEXT NOT NULL, " +
            "count INT NOT NULL, " +
            "gold BIGINT NOT NULL, " +
            "PRIMARY KEY (world_type, character_id, idx))"

    private const val SELECT_OFFERS = "SELECT * FROM public.ge_offers"

    private const val SELECT_HISTORY =
        "SELECT * FROM public.ge_history ORDER BY world_type, character_id, idx"

    private const val UPSERT_OFFER =
        "INSERT INTO public.ge_offers AS o (world_type, character_id, slot, version, occupied, " +
            "type, obj, price, count, seq, completed, gold, escrow_coins, aborted, " +
            "collect_items, collect_coins) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (world_type, character_id, slot) DO UPDATE SET " +
            "version = EXCLUDED.version, occupied = EXCLUDED.occupied, type = EXCLUDED.type, " +
            "obj = EXCLUDED.obj, price = EXCLUDED.price, count = EXCLUDED.count, " +
            "seq = EXCLUDED.seq, completed = EXCLUDED.completed, gold = EXCLUDED.gold, " +
            "escrow_coins = EXCLUDED.escrow_coins, aborted = EXCLUDED.aborted, " +
            "collect_items = EXCLUDED.collect_items, collect_coins = EXCLUDED.collect_coins " +
            "WHERE o.version < EXCLUDED.version"

    private const val DELETE_HISTORY =
        "DELETE FROM public.ge_history WHERE world_type = ? AND character_id = ?"

    private const val INSERT_HISTORY =
        "INSERT INTO public.ge_history (world_type, character_id, idx, type, obj, count, gold) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)"
}
