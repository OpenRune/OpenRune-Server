package org.rsmod.api.type.refs.hitmark

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.hitmark.HashedHitmarkType
import org.rsmod.game.type.hitmark.HitmarkType

public abstract class HitmarkReferences : TypeReferences<HitmarkType>() {
    public fun hitmark(internal: String): HitmarkType {
        val type = HashedHitmarkType(null, internal)
        cache += type
        return type
    }
}
