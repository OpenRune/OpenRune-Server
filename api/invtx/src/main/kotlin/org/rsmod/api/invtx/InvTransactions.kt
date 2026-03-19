package org.rsmod.api.invtx

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import dev.openrune.types.util.UncheckedType
import dev.openrune.util.Dummyitem
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.rsmod.game.inv.InvObj
import org.rsmod.objtx.Transaction
import org.rsmod.objtx.TransactionCancellation
import org.rsmod.objtx.TransactionObj
import org.rsmod.objtx.TransactionObjTemplate
import org.rsmod.objtx.TransactionResultList

public class InvTransactions(
    public val certLookup: Map<Int, TransactionObjTemplate>,
    public val transformLookup: Map<Int, TransactionObjTemplate>,
    public val placeholderLookup: Map<Int, TransactionObjTemplate>,
    public val stackableLookup: Set<Int>,
    public val dummyitemLookup: Set<Int>,
) {
    public fun transaction(
        autoCommit: Boolean,
        init: Transaction<InvObj>.() -> Unit,
    ): TransactionResultList<InvObj> {
        contract { callsInPlace(init, InvocationKind.AT_MOST_ONCE) }
        val transaction =
            Transaction(input = InvObj?::toTransactionObj, output = TransactionObj?::toObj)
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
            filter { it.certlink != 0 }
                .associate { it.id to TransactionObjTemplate(it.certlink, it.certtemplate) }

        private fun Iterable<ItemServerType>.toTransformLookup(): Map<Int, TransactionObjTemplate> =
            filter { it.transformlink != 0 }
                .associate {
                    it.id to TransactionObjTemplate(it.transformlink, it.transformtemplate)
                }

        private fun Iterable<ItemServerType>.toPlaceholderLookup():
            Map<Int, TransactionObjTemplate> =
            filter { it.placeholderLink != 0 }
                .associate {
                    it.id to TransactionObjTemplate(it.placeholderLink, it.placeholderTemplate)
                }

        private fun Iterable<ItemServerType>.toStackableLookup(): List<Int> =
            filter(ItemServerType::stackable).map(ItemServerType::id)

        private fun Iterable<ItemServerType>.toDummyitemLookup(): List<Int> =
            filter { it.resolvedDummyitem == Dummyitem.GraphicOnly }.map(ItemServerType::id)
    }
}

private fun InvObj?.toTransactionObj(): TransactionObj? =
    if (this != null) {
        TransactionObj(id, count, vars)
    } else {
        null
    }

@OptIn(UncheckedType::class)
private fun TransactionObj?.toObj(): InvObj? =
    if (this != null) {
        InvObj(id, count, vars)
    } else {
        null
    }
