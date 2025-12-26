package org.alter.game.model.inv.invtx

import dev.openrune.ServerCacheManager
import dev.openrune.types.Dummyitem
import dev.openrune.types.ItemServerType
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.alter.game.model.inv.objtx.Transaction
import org.alter.game.model.inv.objtx.TransactionCancellation
import org.alter.game.model.inv.objtx.TransactionObj
import org.alter.game.model.inv.objtx.TransactionObjTemplate
import org.alter.game.model.inv.objtx.TransactionResultList
import org.alter.game.model.item.Item
import kotlin.contracts.ExperimentalContracts

public class InvTransactions(
    public val certLookup: Map<Int, TransactionObjTemplate>,
    public val transformLookup: Map<Int, TransactionObjTemplate>,
    public val placeholderLookup: Map<Int, TransactionObjTemplate>,
    public val stackableLookup: Set<Int>,
    public val dummyitemLookup: Set<Int>,
) {
    @OptIn(ExperimentalContracts::class)
    public fun transaction(
        autoCommit: Boolean,
        init: Transaction<Item>.() -> Unit,
    ): TransactionResultList<Item> {
        contract { callsInPlace(init, InvocationKind.AT_MOST_ONCE) }
        val transaction =
            Transaction(input = Item?::toTransactionObj, output = TransactionObj?::toObj)
        transaction.autoCommit = autoCommit
        transaction.certLookup = certLookup
        transaction.transformLookup = transformLookup
        transaction.placeholderLookup = placeholderLookup
        transaction.stackableLookup = stackableLookup
        transaction.dummyitemLookup = dummyitemLookup
        try {
            transaction.apply(init)
        } catch (_: TransactionCancellation) {
            /* cancellation is normal */
        }
        val results = transaction.results()
        if (results.success && transaction.autoCommit) {
            results.commitAll()
        }
        return results
    }

    public companion object {
        public fun from(): InvTransactions {

            val certLookup = ServerCacheManager.getItems().values.toCertLookup()
            val transformLookup = ServerCacheManager.getItems().values.toTransformLookup()
            val placeholderLookup = ServerCacheManager.getItems().values.toPlaceholderLookup()
            val stackableLookup = ServerCacheManager.getItems().values.toStackableLookup()
            val dummyitemLookup = ServerCacheManager.getItems().values.toDummyitemLookup()
            return InvTransactions(
                certLookup = Int2ObjectOpenHashMap(certLookup),
                transformLookup = Int2ObjectOpenHashMap(transformLookup),
                placeholderLookup = Int2ObjectOpenHashMap(placeholderLookup),
                stackableLookup = IntOpenHashSet(stackableLookup),
                dummyitemLookup = IntOpenHashSet(dummyitemLookup),
            )
        }

        private fun Iterable<ItemServerType>.toCertLookup(): Map<Int, TransactionObjTemplate> =
            filter { it.noteLinkId != -1 }
                .associate { it.id to TransactionObjTemplate(it.noteLinkId, it.noteTemplateId) }

        private fun Iterable<ItemServerType>.toTransformLookup():
            Map<Int, TransactionObjTemplate> =
            filter { it.transformlink != -1 }
                .associate {
                    it.id to TransactionObjTemplate(it.transformlink, it.transformtemplate)
                }

        private fun Iterable<ItemServerType>.toPlaceholderLookup():
            Map<Int, TransactionObjTemplate> =
            filter { it.placeholderLink != -1 }
                .associate {
                    it.id to TransactionObjTemplate(it.placeholderLink, it.placeholderTemplate)
                }

        private fun Iterable<ItemServerType>.toStackableLookup(): List<Int> =
            filter(ItemServerType::stackable).map(ItemServerType::id)

        private fun Iterable<ItemServerType>.toDummyitemLookup(): List<Int> =
            filter { it.resolvedDummyitem == Dummyitem.GraphicOnly }.map(ItemServerType::id)
    }
}

private fun Item?.toTransactionObj(): TransactionObj? =
    if (this != null) {
        TransactionObj(id, amount, vars)
    } else {
        null
    }

private fun TransactionObj?.toObj(): Item? =
    if (this != null) {
        Item(id, count, vars)
    } else {
        null
    }
