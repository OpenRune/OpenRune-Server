package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.social.IgnoreListAdd
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.social.SocialNameRepository
import org.rsmod.api.social.addSocialIgnore
import org.rsmod.api.social.writeSocialMessage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class IgnoreListAddHandler
@Inject
constructor(
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val names: SocialNameRepository,
) : MessageHandler<IgnoreListAdd> {
    override fun handle(player: Player, message: IgnoreListAdd) {
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
                                current.writeSocialMessage("Unable to ignore player; user with this username doesn't exist.")
                            } else {
                                current.addSocialIgnore(requestedName, playerList, record)
                            }
                        },
                        onErr = {
                            current.writeSocialMessage("Unable to ignore player right now.")
                        },
                    )
                }
            },
        )
    }
}
