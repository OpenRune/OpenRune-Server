package org.rsmod.api.account

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.loader.AccountLoaderService
import org.rsmod.api.account.loader.request.AccountLoadAuth
import org.rsmod.api.account.loader.request.AccountLoadCallback
import org.rsmod.api.account.loader.request.AccountLoadRequest
import org.rsmod.api.account.saver.AccountSavingService
import org.rsmod.api.account.saver.request.AccountSaveCallback
import org.rsmod.api.account.saver.request.AccountSaveRequest
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType

@Singleton
public class AccountManager
@Inject
constructor(
    private val loader: AccountLoaderService,
    private val saver: AccountSavingService,
    private val repository: CharacterAccountRepository,
    private val pipelines: Set<CharacterDataStage.Pipeline>,
) {
    private val pendingSaves = ArrayList<Pair<Player, AccountSaveCallback>>()

    public fun save(player: Player, callback: AccountSaveCallback) {
        pendingSaves += player to callback
    }

    /** Called on the game thread after gameplay mutations have finished for this tick. */
    public fun capturePendingSaves() {
        for ((player, callback) in pendingSaves) {
            val request =
                AccountSaveRequest(
                    accountId = player.accountId,
                    characterId = player.characterId,
                    player = player,
                    worldType = player.worldType,
                    account = repository.capture(player),
                    segments = pipelines.map { it.capture(player) },
                    callback = callback,
                )
            saver.queue(request)
        }
        pendingSaves.clear()
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
