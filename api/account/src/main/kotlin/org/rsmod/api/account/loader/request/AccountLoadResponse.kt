package org.rsmod.api.account.loader.request

import dev.or2.central.account.AccountData
import kotlin.contracts.contract
import org.rsmod.api.account.character.CharacterDataTransformer
import org.rsmod.game.world.WorldType

public sealed class AccountLoadResponse {
    public sealed class Ok : AccountLoadResponse() {
        public abstract val auth: AccountLoadAuth
        public abstract val account: AccountData
        public abstract val transforms: List<CharacterDataTransformer<*>>

        /** The mode this login resolved to, and whose save was loaded. */
        public abstract val worldType: WorldType

        /**
         * `true` when this character had no save for [worldType] and one was just created. An
         * existing character entering a world type for the first time starts from spawn, like a new
         * account would, even though the account and name are not new.
         */
        public abstract val firstVisitToWorldType: Boolean

        public data class NewAccount(
            override val auth: AccountLoadAuth,
            override val account: AccountData,
            override val transforms: List<CharacterDataTransformer<*>>,
            override val worldType: WorldType,
            override val firstVisitToWorldType: Boolean = true,
        ) : Ok()

        public data class LoadAccount(
            override val auth: AccountLoadAuth,
            override val account: AccountData,
            override val transforms: List<CharacterDataTransformer<*>>,
            override val worldType: WorldType,
            override val firstVisitToWorldType: Boolean = false,
        ) : Ok()
    }

    public sealed class Err : AccountLoadResponse() {
        public data object AccountNotFound : Err()

        public data object ShutdownInProgress : Err()

        public data object InternalServiceError : Err()

        public data object Timeout : Err()

        public data class Exception(val reason: Throwable) : Err()
    }
}

public fun AccountLoadResponse.Ok.isNewAccount(): Boolean {
    contract { returns(true) implies (this@isNewAccount is AccountLoadResponse.Ok.NewAccount) }
    return this is AccountLoadResponse.Ok.NewAccount
}
