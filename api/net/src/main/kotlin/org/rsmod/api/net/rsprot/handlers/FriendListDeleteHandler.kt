package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.social.FriendListDel
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.social.SocialNameRepository
import org.rsmod.api.social.deleteSocialFriend
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class FriendListDeleteHandler
@Inject
constructor(
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val names: SocialNameRepository,
) : MessageHandler<FriendListDel> {
    override fun handle(player: Player, message: FriendListDel) {
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
                            current.deleteSocialFriend(requestedName, playerList, record)
                        },
                        onErr = {
                            current.deleteSocialFriend(requestedName, playerList)
                        },
                    )
                }
            },
        )
    }
}
