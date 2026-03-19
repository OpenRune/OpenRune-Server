package org.rsmod.api.invtx

import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// @see [docs/quirks.md] for details on why this is done.
internal lateinit var cachedInventoryTransactions: InvTransactions

public class InvTransactionsScript : PluginScript() {
    public lateinit var transactions: InvTransactions

    override fun ScriptContext.startup() {
        val create = InvTransactions.from()
        transactions = create
        cachedInventoryTransactions = create
    }
}
