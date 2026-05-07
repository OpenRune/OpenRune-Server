package org.rsmod.api.social

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player
//TODO: NEED TO CREATE OWN SQL TABLE
private val socialDataAttr: AttributeKey<SocialData> = AttributeKey(temp = true)

private val socialPersistenceAttr: AttributeKey<Map<String, Any>> =
    AttributeKey(persistenceKey = "social")

public val Player.social: SocialData
    get() {
        val existing = attr[socialDataAttr]
        if (existing != null) {
            return existing
        }

        val persisted = attr[socialPersistenceAttr] ?: emptyMap()
        val loaded = SocialData.fromPersistentMap(persisted)

        attr[socialDataAttr] = loaded
        return loaded
    }

public fun Player.persistSocial(): Unit {
    attr[socialPersistenceAttr] = social.toPersistentMap()
}
