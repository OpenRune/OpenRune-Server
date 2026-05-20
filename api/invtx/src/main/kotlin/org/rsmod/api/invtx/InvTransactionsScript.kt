package org.rsmod.api.invtx

import jakarta.inject.Inject
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// @see [docs/quirks.md] for details on why this is done.
internal lateinit var cachedInventoryTransactions: InvTransactions

public class InvTransactionsScript
@Inject
constructor(private val itemStorage: PlayerItemStorage) : PluginScript() {
    public lateinit var transactions: InvTransactions

    override fun ScriptContext.startup() {
        val create = InvTransactions.from()
        transactions = create
        cachedInventoryTransactions = create
        cachedPlayerItemStorage = itemStorage
        InvVirtualStorageHolder.instance = itemStorage
    }
}
