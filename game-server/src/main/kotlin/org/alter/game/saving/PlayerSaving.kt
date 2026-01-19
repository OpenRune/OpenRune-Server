package org.alter.game.saving

import dev.openrune.central.api.PlayerLoadResponse
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

    fun loadPlayer(client: Client, block: LoginBlock<*>): PlayerLoadResult {
        val passwordAuth = block.authentication as AuthenticationType.PasswordAuthentication
        val xteaKeys = (block.authentication as? XteaKey)?.key ?: intArrayOf()

        val loginRequest = Server.centralApiClient.requestLogin(
            username = block.username,
            password = passwordAuth.password.asString(),
            xteas = xteaKeys
        )

        return when (loginRequest.result) {
            PlayerLoadResponse.OFFLINE_SERVER -> PlayerLoadResult.OFFLINE

            PlayerLoadResponse.NEW_ACCOUNT -> {
                val login = requireNotNull(loginRequest.login)
                client.uid = login.linkedAccounts.first().uid
                client.tile = client.world.gameContext.home
                client.attr.put(NEW_ACCOUNT_ATTR, true)
                client.attr.put(APPEARANCE_SET_ATTR, false)
                client.attr.put(PLAYTIME_ATTR, 0)
                savePlayer(client)
                PlayerLoadResult.NEW_ACCOUNT
            }

            PlayerLoadResponse.LOAD -> {
                val login = requireNotNull(loginRequest.login)
                client.uid = login.linkedAccounts.first().uid

                loadAttributes(client, null)

                PlayerLoadResult.LOAD_ACCOUNT
            }

            PlayerLoadResponse.INVALID_CREDENTIALS -> PlayerLoadResult.INVALID_CREDENTIALS
            PlayerLoadResponse.INVALID_RECONNECTION -> PlayerLoadResult.INVALID_RECONNECTION
            PlayerLoadResponse.MALFORMED -> PlayerLoadResult.MALFORMED
        }
    }

    fun savePlayer(client: Client): Boolean {
        return try {
            val account = client.username

            val root = Document().apply {
                documents.forEach { encoder ->
                    put(encoder.name, encoder.asDocument(client))
                }
            }

            val resp = Server.centralApiClient.savePlayer(
                uid = client.uid,
                account = account,
                data = root.toJson()
            )

            resp.ok
        } catch (e: Exception) {
            logger.error(e) { "Failed to save attributes for client: ${client.loginUsername}" }
            false
        }
    }

    private fun loadAttributes(client: Client, attributes: Document?): Boolean {
        return try {
            val root: Document =
                attributes ?: run {
                    val account = client.username

                    val resp = Server.centralApiClient.loadPlayer(client.uid, account)
                    if (!resp.ok) return false

                    val json = resp.data ?: return false
                    Document.parse(json)
                }

            documents.forEach { decoder ->
                val section = root.get(decoder.name, Document::class.java) ?: return false
                decoder.fromDocument(client, section)
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to decode attributes for client: ${client.loginUsername}" }
            false
        }
    }
}