package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.messaging.MessagePrivate
import net.rsprot.protocol.game.outgoing.social.MessagePrivateEcho
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.net.central.CentralSocialResult
import org.rsmod.api.net.central.CentralSocialService
import org.rsmod.api.social.writeSocialMessage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

private const val PRIVATE_MESSAGE_MAX_CHARS = 255

class MessagePrivateHandler
@Inject
constructor(
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val social: CentralSocialService,
) : MessageHandler<MessagePrivate> {
    override fun handle(player: Player, message: MessagePrivate) {
        val uid = player.uid
        val targetName = message.name
        val text = message.message.trim().take(PRIVATE_MESSAGE_MAX_CHARS)
        val characterId = player.characterId
        val senderDisplayName = player.displayName.ifBlank { player.username }
        val senderCrown = player.modLevel.clientCode

        if (characterId <= 0) {
            player.writeSocialMessage("Private messaging is not available right now.")
            return
        }

        db.request(
            request = {
                social.sendPrivateMessage(
                    fromCharacterId = characterId,
                    targetName = targetName,
                    senderDisplayName = senderDisplayName,
                    senderCrown = senderCrown,
                    message = text,
                )
            },
            response = { result ->
                val current = uid.resolve(playerList) ?: return@request

                result.fold(
                    onOk = { socialResult ->
                        when (socialResult) {
                            CentralSocialResult.Ok -> {
                                current.client.write(
                                    MessagePrivateEcho(
                                        recipient = targetName,
                                        message = text.trim(),
                                    )
                                )
                            }

                            CentralSocialResult.Ignored -> Unit

                            is CentralSocialResult.Failed -> {
                                current.writeSocialMessage(socialResult.message)
                            }
                        }
                    },
                    onErr = {
                        current.writeSocialMessage("Unable to send private message right now.")
                    },
                )
            },
        )
    }
}
