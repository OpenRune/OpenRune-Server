package org.rsmod.content.other.containers

import org.rsmod.api.attr.AttributeKey

/** Round supplies are temporary. All five collector levels share the player's bag contents. */
internal object CollectionBags {
    val contents = AttributeKey<MutableMap<String, Int>>(resetOnDeath = true)
    const val FAMILY = "collection_bag"
}
