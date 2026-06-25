package org.rsmod.api.net.central

import jakarta.inject.Inject
import org.rsmod.api.db.gateway.model.GameDbResult

class CentralSocialService
@Inject
constructor(
    private val central: OpenRuneCentralWorldLink,
) {
    fun addFriend(
        sessionToken: ByteArray,
        characterId: Int,
        name: String,
    ): GameDbResult<CentralSocialResult> {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social is not available right now.")
            )
        }

        return GameDbResult.Ok(
            central.addFriend(
                sessionToken = sessionToken,
                characterId = characterId,
                targetName = cleaned,
            )
        )
    }

    fun deleteFriend(
        sessionToken: ByteArray,
        characterId: Int,
        name: String,
    ): GameDbResult<CentralSocialResult> {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social is not available right now.")
            )
        }

        return GameDbResult.Ok(
            central.deleteFriend(
                sessionToken = sessionToken,
                characterId = characterId,
                targetName = cleaned,
            )
        )
    }

    fun socialSnapshot(
        sessionToken: ByteArray,
        characterId: Int,
    ): GameDbResult<OpenRuneCentralWorldLink.CentralSocialSnapshotResult> {
        if (characterId <= 0) {
            return GameDbResult.Ok(
                OpenRuneCentralWorldLink.CentralSocialSnapshotResult.Failed("Social is not available right now.")
            )
        }

        return GameDbResult.Ok(
            central.socialSnapshot(
                sessionToken = sessionToken,
                characterId = characterId,
            )
        )
    }

    fun addIgnore(
        sessionToken: ByteArray,
        characterId: Int,
        name: String,
    ): GameDbResult<CentralSocialResult> {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social is not available right now.")
            )
        }

        return GameDbResult.Ok(
            central.addIgnore(
                sessionToken = sessionToken,
                characterId = characterId,
                targetName = cleaned,
            )
        )
    }

    fun deleteIgnore(
        sessionToken: ByteArray,
        characterId: Int,
        name: String,
    ): GameDbResult<CentralSocialResult> {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social is not available right now.")
            )
        }

        return GameDbResult.Ok(
            central.deleteIgnore(
                sessionToken = sessionToken,
                characterId = characterId,
                targetName = cleaned,
            )
        )
    }

    fun sendPrivateMessage(
        sessionToken: ByteArray,
        fromCharacterId: Int,
        targetName: String,
        senderDisplayName: String,
        senderCrown: Int,
        message: String,
    ): GameDbResult<CentralSocialResult> {
        val cleanedTarget = targetName.trim()
        val cleanedMessage = message.trim()

        if (cleanedTarget.isBlank() || cleanedMessage.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (fromCharacterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Private messaging is not available right now.")
            )
        }

        return GameDbResult.Ok(
            central.sendPrivateMessage(
                sessionToken = sessionToken,
                fromCharacterId = fromCharacterId,
                targetName = cleanedTarget,
                senderDisplayName = senderDisplayName,
                senderCrown = senderCrown,
                message = cleanedMessage,
            )
        )
    }

    fun setPrivateChatFilter(
        sessionToken: ByteArray,
        characterId: Int,
        privateChatFilter: Int,
    ): GameDbResult<CentralSocialResult> {
        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social settings are not available right now.")
            )
        }

        return GameDbResult.Ok(
            central.setPrivateChatFilter(
                sessionToken = sessionToken,
                characterId = characterId,
                privateChatFilter = privateChatFilter,
            )
        )
    }
}

    sealed class CentralSocialResult {
        data object Ok : CentralSocialResult()
        data object Ignored : CentralSocialResult()

        data class Failed(
            val message: String,
    ) : CentralSocialResult()
}
