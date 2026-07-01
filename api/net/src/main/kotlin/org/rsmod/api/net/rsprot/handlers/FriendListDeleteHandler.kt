package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.social.FriendListDel
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.net.central.CentralSocialResult
import org.rsmod.api.net.central.CentralSocialService
import org.rsmod.api.social.writeSocialMessage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class FriendListDeleteHandler
@Inject
constructor(
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val social: CentralSocialService,
) : MessageHandler<FriendListDel> {
    override fun handle(player: Player, message: FriendListDel) {
        val uid = player.uid
        val requestedName = message.name
        val characterId = player.characterId

        if (characterId <= 0) {
            player.writeSocialMessage("Social is not available right now.")
            return
        }

        db.request(
            request = {
                social.deleteFriend(
                    characterId = characterId,
                    name = requestedName,
                )
            },
            response = { result ->
                val current = uid.resolve(playerList) ?: return@request

                result.fold(
                    onOk = { socialResult ->
                        when (socialResult) {
                            CentralSocialResult.Ok -> Unit

                            CentralSocialResult.Ignored -> Unit

                            is CentralSocialResult.Failed -> {
                                current.writeSocialMessage(socialResult.message)
                            }
                        }
                    },
                    onErr = {
                        current.writeSocialMessage("Unable to delete friend right now.")
                    },
                )
            },
        )
    }
}
