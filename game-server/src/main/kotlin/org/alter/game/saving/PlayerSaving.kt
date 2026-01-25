package org.alter.game.saving

import dev.openrune.central.LogsResponse
import dev.openrune.central.PlayerLoadResponse
import dev.openrune.central.PlayerSaveLoadResponse
import dev.openrune.central.packet.model.LoginRequestIncoming
import dev.openrune.central.packet.model.PlayerSaveLoadRequestIncoming
import dev.openrune.central.packet.model.PlayerSaveUpsertRequestIncoming

import io.github.oshai.kotlinlogging.KotlinLogging
import net.rsprot.crypto.xtea.XteaKey
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import org.alter.game.Server
import org.alter.game.model.attr.APPEARANCE_SET_ATTR
import org.alter.game.model.attr.NEW_ACCOUNT_ATTR
import org.alter.game.model.attr.PLAYTIME_ATTR
import org.alter.game.model.entity.Client
import org.alter.game.saving.impl.*
import org.bson.Document

object PlayerSaving {

    private val logger = KotlinLogging.logger {}

    private val documents = linkedSetOf(
        DetailSerialisation(),
        AppearanceSerialisation(),
        SkillSerialisation(),
        AttributeSerialisation(),
        TimerSerialisation(),
        ContainersSerialisation(),
        VarpSerialisation(),
    )

    fun loadPlayer(
        client: Client,
        block: LoginBlock<*>,
        callback: (PlayerLoadResult) -> Unit
    ) {
        val passwordAuth =
            block.authentication as? AuthenticationType.PasswordAuthentication
                ?: run {
                    callback(PlayerLoadResult.MALFORMED)
                    return
                }

        val xteaKeys = (block.authentication as? XteaKey)?.key ?: intArrayOf()

        Server.central.packetSender.sendLoginRequest(
            LoginRequestIncoming(
                username = block.username,
                password = passwordAuth.password.asString(),
                xteas = xteaKeys.toList()
            )
        ) { response ->

            when (response.result) {

                PlayerLoadResponse.OFFLINE_SERVER -> callback(PlayerLoadResult.OFFLINE)

                PlayerLoadResponse.ALREADY_ONLINE -> callback(PlayerLoadResult.ALREADY_ONLINE)

                PlayerLoadResponse.NEW_ACCOUNT -> {
                    val login = requireNotNull(response.login)

                    client.uid = login.linkedAccounts.first().uid
                    client.tile = client.world.gameContext.home
                    client.attr[NEW_ACCOUNT_ATTR] = true
                    client.attr[APPEARANCE_SET_ATTR] = false
                    client.attr[PLAYTIME_ATTR] = 0

                    savePlayer(client) { success ->
                        if (!success) {
                            logger.warn {
                                "Failed to persist new account: ${block.username}"
                            }
                            callback(PlayerLoadResult.MALFORMED)
                            return@savePlayer
                        }

                        callback(PlayerLoadResult.NEW_ACCOUNT)
                    }

                    return@sendLoginRequest
                }

                PlayerLoadResponse.LOAD -> {
                    val login = requireNotNull(response.login)
                    client.uid = login.linkedAccounts.first().uid

                    loadAttributes(client, null) { success ->
                        if (!success) {
                            callback(PlayerLoadResult.MALFORMED)
                            return@loadAttributes
                        }

                        callback(PlayerLoadResult.LOAD_ACCOUNT)
                    }

                    return@sendLoginRequest
                }

                PlayerLoadResponse.INVALID_CREDENTIALS -> callback(PlayerLoadResult.INVALID_CREDENTIALS)

                PlayerLoadResponse.INVALID_RECONNECTION -> callback(PlayerLoadResult.INVALID_RECONNECTION)

                PlayerLoadResponse.MALFORMED -> callback(PlayerLoadResult.MALFORMED)
            }
        }
    }

    fun savePlayer(
        client: Client,
        callback: (Boolean) -> Unit
    ) {

        try {
            val account = client.username

            val root = Document().apply {
                documents.forEach { encoder ->
                    put(encoder.name, encoder.asDocument(client))
                }
            }

            Server.central.packetSender.sendPlayerSave(
                PlayerSaveUpsertRequestIncoming(
                    client.uid.value,
                    account,
                    root.toJson()
                )
            ) { res ->
                println(res.result)
                when (res.result) {
                    LogsResponse.SUCCESS -> callback(true)
                    LogsResponse.FAILED -> callback(false)
                }
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to save attributes for client: ${client.loginUsername}"
            }
            callback(false)
        }
    }

    private fun loadAttributes(
        client: Client,
        attributes: Document?,
        callback: (Boolean) -> Unit
    ) {
        try {
            if (attributes != null) {
                decodeAttributes(client, attributes, callback)
                return
            }

            val account = client.username

            Server.central.packetSender.sendPlayerSaveLoadRequest(
                PlayerSaveLoadRequestIncoming(client.uid.value, account)
            ) { res ->
                when (res.result) {
                    PlayerSaveLoadResponse.LOADED -> {
                        try {
                            val root = Document.parse(res.data)
                            for (decoder in documents) {
                                val section = root.get(decoder.name, Document::class.java) ?: run {
                                    callback(false)
                                    return@sendPlayerSaveLoadRequest
                                }

                                decoder.fromDocument(client, section)
                            }
                            callback(true)
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to decode attributes for client: ${client.loginUsername}" }
                            callback(false)
                        }
                    }

                    PlayerSaveLoadResponse.NOT_FOUND,
                    PlayerSaveLoadResponse.MALFORMED -> callback(false)
                }
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to decode attributes for client: ${client.loginUsername}"
            }
            callback(false)
        }
    }

    private fun decodeAttributes(
        client: Client,
        root: Document,
        callback: (Boolean) -> Unit
    ) {
        for (decoder in documents) {
            val section = root.get(decoder.name, Document::class.java) ?: run {
                callback(false)
                return
            }
            decoder.fromDocument(client, section)
        }
        callback(true)
    }

}