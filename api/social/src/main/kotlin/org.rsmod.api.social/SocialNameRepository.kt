package org.rsmod.api.social

import jakarta.inject.Inject
import org.rsmod.api.db.DatabaseConnection

public class SocialNameRepository @Inject constructor() {

    public fun selectByAnyName(
        connection: DatabaseConnection,
        name: String,
    ): SocialNameRecord? {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return null
        }

        val select =
            connection.prepareStatement(
                """
                    SELECT
                    login_username,
                    display_name,
                    previous_display_name
                    FROM accounts
                    WHERE login_username = ? COLLATE NOCASE
                    OR display_name = ? COLLATE NOCASE
                    OR previous_display_name = ? COLLATE NOCASE
                    LIMIT 1
                    """
                    .trimIndent()
            )

        select.use {
            it.setString(1, cleaned)
            it.setString(2, cleaned)
            it.setString(3, cleaned)

            it.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    return null
                }

                val loginName = resultSet.getString("login_username")
                val displayName = resultSet.getString("display_name")
                val previousDisplayName = resultSet.getString("previous_display_name")

                val current = displayName?.takeIf(String::isNotBlank) ?: loginName

                return SocialNameRecord(
                    canonicalName = loginName.lowercase(),
                    currentName = current,
                    previousName = previousDisplayName?.takeIf(String::isNotBlank),
                )
            }
        }
    }
}
