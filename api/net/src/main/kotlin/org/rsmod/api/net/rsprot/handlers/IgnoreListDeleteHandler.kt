package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.social.IgnoreListDel
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.net.central.CentralSocialResult
import org.rsmod.api.net.central.CentralSocialService
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.net.central.writeCentralSocialSnapshot
import org.rsmod.api.social.writeSocialMessage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class IgnoreListDeleteHandler
@Inject
constructor(
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val social: CentralSocialService,
) : MessageHandler<IgnoreListDel> {
    override fun handle(player: Player, message: IgnoreListDel) {
        val uid = player.uid
        val requestedName = message.name
        val token = player.openRuneCentralSessionToken
        val characterId = player.characterId

        if (token == null || characterId <= 0) {
            player.writeSocialMessage("Social is not available right now.")
            return
        }

        db.request(
            request = {
                social.deleteIgnore(
                    sessionToken = token,
                    characterId = characterId,
                    name = requestedName,
                )
            },
            response = { result ->
                val current = uid.resolve(playerList) ?: return@request

                result.fold(
                    onOk = { socialResult ->
                        when (socialResult) {
                            CentralSocialResult.Ok -> {
                                val refreshUid = current.uid

                                db.request(
                                    request = {
                                        social.socialSnapshot(
                                            sessionToken = token,
                                            characterId = characterId,
                                        )
                                    },
                                    response = { refreshResult ->
                                        val refreshed =
                                            refreshUid.resolve(playerList) ?: return@request

                                        refreshResult.fold(
                                            onOk = { sync ->
                                                when (sync) {
                                                    is OpenRuneCentralWorldLink.CentralSocialSnapshotResult.Ok -> {
                                                        refreshed.writeCentralSocialSnapshot(sync.snapshot)
                                                    }

                                                    is OpenRuneCentralWorldLink.CentralSocialSnapshotResult.Failed -> {
                                                        refreshed.writeSocialMessage(sync.message)
                                                    }
                                                }
                                            },
                                            onErr = {
                                                refreshed.writeSocialMessage("Unable to refresh social list right now.")
                                            },
                                        )
                                    },
                                )
                            }

                            CentralSocialResult.Ignored -> Unit

                            is CentralSocialResult.Failed -> {
                                current.writeSocialMessage(socialResult.message)
                            }
                        }
                    },
                    onErr = {
                        current.writeSocialMessage("Unable to delete ignore right now.")
                    },
                )
            },
        )
    }
}
