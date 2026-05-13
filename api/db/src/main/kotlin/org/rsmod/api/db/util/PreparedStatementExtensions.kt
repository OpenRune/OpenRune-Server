package org.rsmod.api.db.util

import java.sql.PreparedStatement
import java.sql.Types

public fun PreparedStatement.setNullableString(index: Int, value: String?) {
    if (value != null) {
        setString(index, value)
    } else {
        setNull(index, Types.VARCHAR)
    }
}

public fun PreparedStatement.setNullableInt(index: Int, value: Int?) {
    if (value != null) {
        setInt(index, value)
    } else {
        setNull(index, Types.INTEGER)
    }
}
