package org.rsmod.api.delivery

import org.rsmod.api.attr.AttributeKey

public object DeliveryAttributes {
    /**
     * `character_attrs.attr` value every player's delivery document is stored under.
     *
     * Reserved for `api/delivery`. Nothing else may define an [AttributeKey] with this persistence
     * key: [AttributeKey] equality is by persistence key, so a second definition would fight
     * [DOCUMENT] for the same row.
     */
    public const val DOCUMENT_PERSISTENCE_KEY: String = "timed_deliveries"

    /**
     * Pending parcels, stored goods and account stats commit together. Replace the JSON
     * document wholesale so the saver cannot read a half-mutated object.
     */
    internal val DOCUMENT: AttributeKey<MutableMap<String, Any>> =
        AttributeKey(persistenceKey = DOCUMENT_PERSISTENCE_KEY)

    /**
     * The live, typed state, rebuilt from [DOCUMENT] at login.
     *
     * Marked `temp` and given **no persistence key** on purpose, exactly as
     * `FarmingAttributes.PATCHES` is: handing the live object to Jackson during the account save
     * would store a shape nothing can read back.
     */
    internal val LIVE: AttributeKey<DeliveryState> = AttributeKey(temp = true)
}
