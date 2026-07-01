package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.misc.user.SetChatFilterSettings
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.net.central.CentralSocialResult
import org.rsmod.api.net.central.CentralSocialService
import org.rsmod.api.social.writeSocialMessage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class SetChatFilterSettingsHandler
@Inject
constructor(
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val social: CentralSocialService,
) : MessageHandler<SetChatFilterSettings> {
    override fun handle(player: Player, message: SetChatFilterSettings) {
        val uid = player.uid
        val characterId = player.characterId

        if (characterId <= 0) {
            player.writeSocialMessage("Social settings are not available right now.")
            return
        }

        db.request(
            request = {
                social.setChatFilters(
                    characterId = characterId,
                    publicChat = message.publicChatFilter,
                    privateChat = message.privateChatFilter,
                    tradeChat = message.tradeChatFilter,
                )
            },
            response = { result ->
                val current = uid.resolve(playerList) ?: return@request

                result.fold(
                    onOk = { socialResult ->
                        when (socialResult) {
                            CentralSocialResult.Ok -> {
                                current.client.write(
                                    net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings(
                                        message.publicChatFilter,
                                        message.tradeChatFilter,
                                    ),
                                )
                                current.client.write(
                                    net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettingsPrivateChat(
                                        message.privateChatFilter,
                                    ),
                                )
                            }

                            CentralSocialResult.Ignored -> Unit

                            is CentralSocialResult.Failed -> {
                                current.writeSocialMessage(socialResult.message)
                            }
                        }
                    },
                    onErr = {
                        current.writeSocialMessage("Unable to update chat settings right now.")
                    },
                )
            },
        )
    }
}
