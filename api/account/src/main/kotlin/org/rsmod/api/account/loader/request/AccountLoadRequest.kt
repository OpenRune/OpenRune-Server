package org.rsmod.api.account.loader.request

import org.rsmod.game.world.WorldType

public sealed class AccountLoadRequest {
    public abstract val auth: AccountLoadAuth
    public abstract val accountName: String
    public abstract val callback: AccountLoadCallback

    /** A hint for the mode to load; ignored when this world does not serve it. */
    public abstract val worldType: WorldType

    /**
     * Account requests that only require read access to the database or other account-related
     * storage services.
     *
     * Properly labeling requests as read or write enables systems to optimize operations based on
     * access type. For example, read-only requests can be executed in parallel - even in systems
     * which benefit from concurrent reads but not concurrent writes on a single writer DB.
     */
    public sealed class ReadOnly : AccountLoadRequest()

    public data class StrictSearch(
        override val auth: AccountLoadAuth,
        override val accountName: String,
        override val worldType: WorldType,
        override val callback: AccountLoadCallback,
    ) : ReadOnly()

    /** Loads [worldType] exactly, bypassing the stored preference and the world's default. */
    public data class WorldTypeSwitch(
        override val auth: AccountLoadAuth,
        override val accountName: String,
        override val worldType: WorldType,
        override val callback: AccountLoadCallback,
    ) : ReadOnly()

    /**
     * Account requests that require write access to the database or other account-related storage
     * services.
     *
     * Properly labeling requests as read or write enables systems to optimize operations based on
     * access type. For example, read-only requests can be executed in parallel - even in systems
     * which benefit from concurrent reads but not concurrent writes on a single writer DB.
     */
    public sealed class WriteRequired : AccountLoadRequest()

    public data class SearchOrCreateWithPassword(
        public val hashedPassword: () -> String,
        override val auth: AccountLoadAuth,
        override val accountName: String,
        override val worldType: WorldType,
        override val callback: AccountLoadCallback,
    ) : WriteRequired()
}
