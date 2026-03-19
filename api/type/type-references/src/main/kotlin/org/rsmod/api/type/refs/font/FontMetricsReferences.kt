package org.rsmod.api.type.refs.font

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.font.FontMetricsType
import org.rsmod.game.type.font.HashedFontMetricsType

public abstract class FontMetricsReferences :
    TypeReferences<FontMetricsType>() {
    public fun fontMetrics(internal: String): FontMetricsType {
        val type = HashedFontMetricsType(null, internal)
        cache += type
        return type
    }
}
