package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.social.FriendListAdd
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.social.SocialNameRepository
import org.rsmod.api.social.addSocialFriend
import org.rsmod.api.social.writeSocialMessage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class FriendListAddHandler
@Inject
constructor(
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val names: SocialNameRepository,
) : MessageHandler<FriendListAdd> {
    override fun handle(player: Player, message: FriendListAdd) {
        val uid = player.uid
        val requestedName = message.name

        db.request(
            request = { connection ->
                GameDbResult.Ok(names.selectByAnyName(connection, requestedName))
            },
            response = { result ->
                val current = uid.resolve(playerList)
                if (current != null) {
                    result.fold(
                        onOk = { record ->
                            if (record == null) {
                                current.writeSocialMessage("Unable to add friend - unknown player.")
                            } else {
                                current.addSocialFriend(requestedName, playerList, record)
                            }
                        },
                        onErr = {
                            current.writeSocialMessage("Unable to add player right now.")
                        },
                    )
                }
            },
        )
    }
}
