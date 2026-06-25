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
        val token = player.openRuneCentralSessionToken
        val characterId = player.characterId

        if (token == null || characterId <= 0) {
            player.writeSocialMessage("Social settings are not available right now.")
            return
        }

        db.request(
            request = {
                social.setPrivateChatFilter(
                    sessionToken = token,
                    characterId = characterId,
                    privateChatFilter = message.privateChatFilter,
                )
            },
            response = { result ->
                val current = uid.resolve(playerList) ?: return@request

                result.fold(
                    onOk = { socialResult ->
                        when (socialResult) {
                            CentralSocialResult.Ok,
                            CentralSocialResult.Ignored -> Unit

                            is CentralSocialResult.Failed -> {
                                current.writeSocialMessage(socialResult.message)
                            }
                        }
                    },
                    onErr = {
                        current.writeSocialMessage("Unable to update private chat filter right now.")
                    },
                )
            },
        )
    }
}
