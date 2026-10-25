package org.rsmod.api.account

import jakarta.inject.Inject
import org.rsmod.api.account.loader.AccountLoaderService
import org.rsmod.api.account.loader.request.AccountLoadAuth
import org.rsmod.api.account.loader.request.AccountLoadCallback
import org.rsmod.api.account.loader.request.AccountLoadRequest
import org.rsmod.api.account.saver.AccountSavingService
import org.rsmod.api.account.saver.request.AccountSaveCallback
import org.rsmod.api.account.saver.request.AccountSaveRequest
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType

public class AccountManager
@Inject
constructor(private val loader: AccountLoaderService, private val saver: AccountSavingService) {
    public fun save(player: Player, callback: AccountSaveCallback) {
        val request =
            AccountSaveRequest(
                accountId = player.accountId,
                characterId = player.characterId,
                player = player,
                callback = callback,
            )
        saver.queue(request)
    }

    public fun load(
        auth: AccountLoadAuth,
        accountName: String,
        callback: AccountLoadCallback,
    ): Boolean {
        val loadRequest =
            AccountLoadRequest.StrictSearch(auth, accountName, WorldType.DEFAULT, callback)
        return loader.queue(loadRequest)
    }

    public fun loadOrCreate(
        auth: AccountLoadAuth,
        accountName: String,
        hashedPassword: () -> String,
        callback: AccountLoadCallback,
    ): Boolean {
        val loadRequest =
            AccountLoadRequest.SearchOrCreateWithPassword(
                hashedPassword,
                auth,
                accountName,
                WorldType.DEFAULT,
                callback,
            )
        return loader.queue(loadRequest)
    }

    /** Loads [worldType]'s save, pinning the mode instead of resolving it from the preference. */
    public fun loadWorldTypeSwitch(
        accountName: String,
        worldType: WorldType,
        callback: AccountLoadCallback,
    ): Boolean {
        val loadRequest =
            AccountLoadRequest.WorldTypeSwitch(
                auth = AccountLoadAuth.UnknownDevice,
                accountName = accountName,
                worldType = worldType,
                callback = callback,
            )
        return loader.queue(loadRequest)
    }

    public fun isLoaderShuttingDown(): Boolean = loader.isShuttingDown()

    public fun isLoaderRejectingRequests(): Boolean = loader.isTemporarilyRejectingRequests()
}
