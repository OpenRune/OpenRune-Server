package org.alter.game.model.inv.invtx



internal val cachedInventoryTransactions: InvTransactions by lazy {
    InvTransactionsScript.transactionss
}

object InvTransactionsScript {
    val transactionss: InvTransactions by lazy {
        InvTransactions.from()
    }
}