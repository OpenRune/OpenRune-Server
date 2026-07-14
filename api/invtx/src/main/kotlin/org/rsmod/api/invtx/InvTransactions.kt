package org.rsmod.api.invtx

import dev.openrune.ServerCacheManager
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
            val certLookup = Int2ObjectOpenHashMap<TransactionObjTemplate>()
            val transformLookup = Int2ObjectOpenHashMap<TransactionObjTemplate>()
            val placeholderLookup = Int2ObjectOpenHashMap<TransactionObjTemplate>()
            val stackableLookup = IntOpenHashSet()
            val dummyitemLookup = IntOpenHashSet()
            for (item in ServerCacheManager.getItemTypes()) {
                if (item.certlink != 0) {
                    certLookup[item.id] = TransactionObjTemplate(item.certlink, item.certtemplate)
                }
                if (item.transformlink != 0) {
                    transformLookup[item.id] =
                        TransactionObjTemplate(item.transformlink, item.transformtemplate)
                }
                if (item.placeholderLink != 0) {
                    placeholderLookup[item.id] =
                        TransactionObjTemplate(item.placeholderLink, item.placeholderTemplate)
                }
                if (item.stackable) {
                    stackableLookup.add(item.id)
                }
                if (item.resolvedDummyitem == Dummyitem.GraphicOnly) {
                    dummyitemLookup.add(item.id)
                }
            }
            return InvTransactions(
                certLookup = certLookup,
                transformLookup = transformLookup,
                placeholderLookup = placeholderLookup,
                stackableLookup = stackableLookup,
                dummyitemLookup = dummyitemLookup,
            )
        }
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
