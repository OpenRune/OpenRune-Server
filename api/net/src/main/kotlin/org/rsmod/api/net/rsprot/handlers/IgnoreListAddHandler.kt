package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.social.IgnoreListAdd
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.net.central.CentralSocialResult
import org.rsmod.api.net.central.CentralSocialService
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.net.central.writeCentralSocialSnapshot
import org.rsmod.api.social.writeSocialMessage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class IgnoreListAddHandler
@Inject
constructor(
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val social: CentralSocialService,
) : MessageHandler<IgnoreListAdd> {
    override fun handle(player: Player, message: IgnoreListAdd) {
        val uid = player.uid
        val requestedName = message.name
        val characterId = player.characterId

        if (characterId <= 0) {
            player.writeSocialMessage("Social is not available right now.")
            return
        }

        db.request(
            request = {
                social.addIgnore(
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
                        current.writeSocialMessage("Unable to ignore player right now.")
                    },
                )
            },
        )
    }
}
