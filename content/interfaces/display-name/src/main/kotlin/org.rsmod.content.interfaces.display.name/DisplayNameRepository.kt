package org.rsmod.content.interfaces.display.name

import jakarta.inject.Inject
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.db.util.getLocalDateTime

data class DisplayNameLookupResult(
    val requestedName: String,
    val available: Boolean,
    val message: String,
)

data class DisplayNameChangeResult(
    val requestedName: String,
    val success: Boolean,
    val message: String,
)

class DisplayNameRepository @Inject constructor() {
    fun checkName(
        connection: DatabaseConnection,
        accountId: Int,
        requestedName: String,
    ): GameDbResult<DisplayNameLookupResult> {
        val cleaned = cleanName(requestedName)
        val validation = validate(cleaned)
        if (validation != null) {
            return GameDbResult.Ok(
                DisplayNameLookupResult(
                    requestedName = cleaned,
                    available = false,
                    message = validation,
                )
            )
        }

        val now = LocalDateTime.now()
        val heldAfter = now.minus(35, ChronoUnit.DAYS)

        val select =
            connection.prepareStatement(
                """
                      SELECT id, display_name, previous_display_name, display_name_changed_at
                      FROM accounts
                      WHERE display_name = ? COLLATE NOCASE
                      OR (
                      previous_display_name = ? COLLATE NOCASE
                      AND display_name_changed_at IS NOT NULL
                      AND display_name_changed_at >= ?
                      )
                      LIMIT 1
                      """.trimIndent()
            )

        select.use {
            it.setString(1, cleaned)
            it.setString(2, cleaned)
            it.setString(3, heldAfter.toString())

            it.executeQuery().use { results ->
                if (!results.next()) {
                    return GameDbResult.Ok(
                        DisplayNameLookupResult(
                            requestedName = cleaned,
                            available = true,
                            message = "That display name is available.",
                        )
                    )
                }

                val foundAccountId = results.getInt("id")
                val displayName = results.getString("display_name")
                val previousDisplayName = results.getString("previous_display_name")
                val changedAt = results.getLocalDateTime("display_name_changed_at")

                val sameAccountCurrent =
                    foundAccountId == accountId && displayName.equals(cleaned, ignoreCase = true)
                if (sameAccountCurrent) {
                    return GameDbResult.Ok(
                        DisplayNameLookupResult(
                            requestedName = cleaned,
                            available = false,
                            message = "That is already your current display name.",
                        )
                    )
                }

                val heldPrevious =
                    previousDisplayName.equals(cleaned, ignoreCase = true) &&
                        changedAt != null &&
                        changedAt.isAfter(heldAfter)

                val message =
                    if (heldPrevious) {
                        "That display name is currently reserved."
                    } else {
                        "That display name is already taken."
                    }

                return GameDbResult.Ok(
                    DisplayNameLookupResult(
                        requestedName = cleaned,
                        available = false,
                        message = message,
                    )
                )
            }
        }
    }

    fun changeName(
        connection: DatabaseConnection,
        accountId: Int,
        currentDisplayName: String,
        requestedName: String,
    ): GameDbResult<DisplayNameChangeResult> {
        val lookup = checkName(connection, accountId, requestedName)
        if (lookup is GameDbResult.Err) {
            return lookup
        }

        val result = (lookup as GameDbResult.Ok).value
        if (!result.available) {
            return GameDbResult.Ok(
                DisplayNameChangeResult(
                    requestedName = result.requestedName,
                    success = false,
                    message = result.message,
                )
            )
        }

        val oldName = currentDisplayName.takeIf(String::isNotBlank)

        val update =
            connection.prepareStatement(
                """
                    UPDATE accounts
                    SET previous_display_name = ?,
                    display_name = ?,
                    display_name_changed_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                """.trimIndent()
            )

        update.use {
            it.setString(1, oldName ?: "")
            it.setString(2, result.requestedName)
            it.setInt(3, accountId)

            val updated = it.executeUpdate()
            if (updated == 0) {
                return GameDbResult.Ok(
                    DisplayNameChangeResult(
                        requestedName = result.requestedName,
                        success = false,
                        message = "Unable to change your display name right now.",
                    )
                )
            }
        }

        return GameDbResult.Ok(
            DisplayNameChangeResult(
                requestedName = result.requestedName,
                success = true,
                message = "Your display name has been changed.",
            )
        )
    }

    private fun validate(name: String): String? {
        if (name.isBlank()) {
            return "You did not enter a display name."
        }

        if (name.length > 12) {
            return "Display names cannot exceed 12 characters."
        }

        if (name.contains("mod", ignoreCase = true)) {
            return "Display names cannot contain 'mod'."
        }

        if (!name.any(Char::isLetterOrDigit)) {
            return "Display names must contain at least one letter or number."
        }

        return null
    }

    private fun cleanName(input: String): String {
        val cleaned =
            input
                .trim()
                .filter { it.isLetterOrDigit() || it == ' ' || it == '_' || it == '-' }
                .replace('_', ' ')
                .replace(Regex("\\s+"), " ")
                .take(12)
                .trim()

        return formatDisplayName(cleaned)
    }

    private fun formatDisplayName(name: String): String {
        if (name.isBlank()) {
            return name
        }

        val result = StringBuilder(name.length)
        var capitalizeNext = true

        for (char in name.lowercase(java.util.Locale.ROOT)) {
            val formatted =
                if (capitalizeNext && char.isLetter()) {
                    char.uppercaseChar()
                } else {
                    char
                }

            result.append(formatted)

            capitalizeNext = char == ' ' || char == '-' || char == '_'
        }

        return result.toString()
    }
}
