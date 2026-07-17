package org.rsmod.api.net.central

import jakarta.inject.Inject
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.net.central.WorldLinkFrameSpecs.PRIVATE_MESSAGE_MAX_CHARS

class CentralSocialService
@Inject
constructor(
    private val central: OpenRuneCentralWorldLink,
) {
    fun addFriend(
        characterId: Int,
        name: String,
    ): GameDbResult<CentralSocialResult> {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social is not available right now."),
            )
        }

        return GameDbResult.Ok(
            central.addFriend(
                characterId = characterId,
                targetName = cleaned,
            ),
        )
    }

    fun deleteFriend(
        characterId: Int,
        name: String,
    ): GameDbResult<CentralSocialResult> {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social is not available right now."),
            )
        }

        return GameDbResult.Ok(
            central.deleteFriend(
                characterId = characterId,
                targetName = cleaned,
            ),
        )
    }

    fun socialSnapshot(
        characterId: Int,
    ): GameDbResult<OpenRuneCentralWorldLink.CentralSocialSnapshotResult> {
        if (characterId <= 0) {
            return GameDbResult.Ok(
                OpenRuneCentralWorldLink.CentralSocialSnapshotResult.Failed("Social is not available right now."),
            )
        }

        return GameDbResult.Ok(
            central.socialSnapshot(
                characterId = characterId,
            ),
        )
    }

    fun addIgnore(
        characterId: Int,
        name: String,
    ): GameDbResult<CentralSocialResult> {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social is not available right now."),
            )
        }

        return GameDbResult.Ok(
            central.addIgnore(
                characterId = characterId,
                targetName = cleaned,
            ),
        )
    }

    fun deleteIgnore(
        characterId: Int,
        name: String,
    ): GameDbResult<CentralSocialResult> {
        val cleaned = name.trim()
        if (cleaned.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social is not available right now."),
            )
        }

        return GameDbResult.Ok(
            central.deleteIgnore(
                characterId = characterId,
                targetName = cleaned,
            ),
        )
    }

    fun sendPrivateMessage(
        fromCharacterId: Int,
        targetName: String,
        senderDisplayName: String,
        senderCrown: Int,
        message: String,
    ): GameDbResult<CentralSocialResult> {
        val cleanedTarget = targetName.trim()
        val cleanedMessage = message.trim().take(PRIVATE_MESSAGE_MAX_CHARS)

        if (cleanedTarget.isBlank() || cleanedMessage.isBlank()) {
            return GameDbResult.Ok(CentralSocialResult.Ignored)
        }

        if (fromCharacterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Private messaging is not available right now."),
            )
        }

        return GameDbResult.Ok(
            central.sendPrivateMessage(
                fromCharacterId = fromCharacterId,
                targetName = cleanedTarget,
                senderDisplayName = senderDisplayName,
                senderCrown = senderCrown,
                message = cleanedMessage,
            ),
        )
    }

    fun setChatFilters(
        characterId: Int,
        publicChat: Int,
        privateChat: Int,
        tradeChat: Int,
    ): GameDbResult<CentralSocialResult> {
        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social settings are not available right now."),
            )
        }

        return GameDbResult.Ok(
            central.setChatFilters(
                characterId = characterId,
                publicChat = publicChat,
                privateChat = privateChat,
                tradeChat = tradeChat,
            ),
        )
    }

    fun setPrivateChatFilter(
        characterId: Int,
        privateChatFilter: Int,
    ): GameDbResult<CentralSocialResult> {
        if (characterId <= 0) {
            return GameDbResult.Ok(
                CentralSocialResult.Failed("Social settings are not available right now."),
            )
        }

        return GameDbResult.Ok(
            central.setPrivateChatFilter(
                characterId = characterId,
                privateChatFilter = privateChatFilter,
            ),
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
