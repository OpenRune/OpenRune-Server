package org.rsmod.api.db.util

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

public fun ResultSet.getLocalDateTime(columnLabel: String): LocalDateTime? {
    val time = getString(columnLabel)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Timestamp.valueOf(time).toLocalDateTime()
}

public fun ResultSet.getStringOrNull(columnLabel: String): String? {
    return getString(columnLabel).takeUnless { wasNull() }
}

public fun ResultSet.getIntOrNull(columnLabel: String): Int? {
    return getInt(columnLabel).takeUnless { wasNull() }
}
