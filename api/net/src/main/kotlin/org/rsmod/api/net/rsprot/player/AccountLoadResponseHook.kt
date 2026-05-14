package org.rsmod.api.net.rsprot.player

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.types.ModLevelType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import net.rsprot.protocol.api.login.GameLoginResponseHandler
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import net.rsprot.protocol.loginprot.outgoing.util.AuthenticatorResponse
import kotlinx.coroutines.runBlocking
import org.rsmod.api.account.character.main.CharacterAccountApplier
import dev.or2.central.account.AccountData
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.loader.request.AccountLoadAuth
import org.rsmod.api.account.loader.request.AccountLoadCallback
import org.rsmod.api.account.loader.request.AccountLoadResponse
import org.rsmod.api.account.loader.request.isNewAccount
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.net.central.CentralAuthResult
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.realm.RealmConfig
import org.rsmod.api.registry.account.AccountRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.player.isSuccess
import org.rsmod.events.EventBus
import org.rsmod.game.GameUpdate
import org.rsmod.game.GameUpdate.Companion.isCountdown
import org.rsmod.game.GameUpdate.Companion.isUpdating
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent

class AccountLoadResponseHook(
    private val world: Int,
    private val config: RealmConfig,
    private val update: GameUpdate,
    private val eventBus: EventBus,
    private val accountRegistry: AccountRegistry,
    private val playerRegistry: PlayerRegistry,
    private val devModeModLevel: ModLevelType,
    private val loginBlock: LoginBlock<AuthenticationType>,
    private val channelResponses: GameLoginResponseHandler<Player>,
    private val inputPassword: CharArray,
    private val verifyTotp: (CharArray, String) -> Boolean,
    private val openRuneCentral: OpenRuneCentralWorldLink,
    private val database: GameDatabase,
    private val characterRepository: CharacterAccountRepository,
) : AccountLoadCallback {
    private var pendingCentralSessionToken: ByteArray? = null

    private var pendingCentralRights: String? = null

    override fun invoke(response: AccountLoadResponse) {
        try {
            handleLoadResponse(response)
        } finally {
            inputPassword.fill('\u0000')
            pendingCentralSessionToken?.fill(0)
            pendingCentralSessionToken = null
            pendingCentralRights = null
        }
    }

    private fun handleLoadResponse(response: AccountLoadResponse) {
        when (response) {
            is AccountLoadResponse.Ok.NewAccount -> {
                if (!runCentralAuth(response.account.accountName, response.account.characterData.characterId)) {
                    return
                }
                safeQueueLogin(response)
            }
            is AccountLoadResponse.Ok.LoadAccount -> {
                validateAndQueueLogin(response)
            }
            is AccountLoadResponse.Err.AccountNotFound -> {
                writeErrorResponse(LoginResponse.InvalidUsernameOrPassword)
            }
            is AccountLoadResponse.Err.Timeout -> {
                writeErrorResponse(LoginResponse.Timeout)
            }
            is AccountLoadResponse.Err.InternalServiceError -> {
                writeErrorResponse(LoginResponse.LoginServerLoadError)
            }
            is AccountLoadResponse.Err.ShutdownInProgress -> {
                writeErrorResponse(LoginResponse.UpdateInProgress)
            }
            is AccountLoadResponse.Err.Exception -> {
                writeErrorResponse(LoginResponse.UnknownReplyFromLoginServer)
            }
        }
    }

    private fun validateAndQueueLogin(response: AccountLoadResponse.Ok.LoadAccount) {
        // Note: We could move this branch to `handleLoadResponse`, but we intentionally keep it
        // here to mirror the production login flow.
        if (!runCentralAuth(response.account.accountName, response.account.characterData.characterId)) {
            return
        }

        val verifyTwoFactor = response.account.twofaEnabled
        if (verifyTwoFactor) {
            val secret = response.account.twofaSecret
            if (secret == null) {
                writeErrorResponse(LoginResponse.InvalidAuthenticatorCode)
                logger.error { "Two-factor enabled without a stored secret: ${response.account}" }
                return
            }

            val authResponse =
                validateTwoFactor(response.account, response.auth, secret.toCharArray())
            if (authResponse != null) {
                writeErrorResponse(authResponse)
                return
            }
        }

        // `CharacterAccountRepository.save` always sets `last_logout` in same UPDATE as
        // `last_login`. Row with `last_login` set but `last_logout` null = DB corruption,
        // legacy schema drift, or partial write — not same as first login (both null).
        val inconsistentSessionTimestamps =
            response.account.characterData.lastLogin != null && response.account.characterData.lastLogout == null
        if (inconsistentSessionTimestamps) {
            logger.error {
                "Character last_login set but last_logout null (invalid row) — login aborted: " +
                    response.account
            }
            writeErrorResponse(LoginResponse.InvalidSave)
            return
        }

        safeQueueLogin(response)
    }

    private fun validateTwoFactor(
        account: AccountData,
        auth: AccountLoadAuth,
        secret: CharArray,
    ): LoginResponse? =
        when (auth) {
            is AccountLoadAuth.InitialRequest -> {
                val requiresAuth = requiresTwoFactorAuth(account, auth)
                if (requiresAuth) {
                    LoginResponse.Authenticator
                } else {
                    null
                }
            }
            is AccountLoadAuth.CodeInput -> {
                val correctCode = verifyTotp(secret, auth.otp.toString())
                if (!correctCode) {
                    LoginResponse.InvalidAuthenticatorCode
                } else {
                    null
                }
            }
        }

    private fun requiresTwoFactorAuth(
        account: AccountData,
        auth: AccountLoadAuth.InitialRequest,
    ): Boolean {
        val requiresImmediateCode =
            when (auth) {
                is AccountLoadAuth.TrustedDevice -> account.knownDevice != auth.identifier
                AccountLoadAuth.UnknownDevice -> true
            }

        if (requiresImmediateCode) {
            return true
        }

        val lastVerifiedMs = account.twofaLastVerifiedMillis ?: return true
        val zone = ZoneId.systemDefault()
        val daysSince =
            ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(lastVerifiedMs).atZone(zone).toLocalDate(),
                LocalDate.now(zone),
            )
        return daysSince >= DAYS_BETWEEN_2FA_VERIFICATION
    }

    private fun safeQueueLogin(response: AccountLoadResponse.Ok) {
        try {
            val player = createPlayer(response).apply { applyConfigTransforms(config) }
            applyCentralStaffFromPending(player)
            accountRegistry.queueLogin(player, response, ::safeHandleGameLogin)
        } catch (e: Exception) {
            writeErrorResponse(LoginResponse.ConnectFail)
            logger.error(e) { "Could not queue login for account: ${response.account}" }
        }
    }

    private fun createPlayer(fromResponse: AccountLoadResponse.Ok): Player {
        val player = Player()
        for (transform in fromResponse.transforms) {
            transform.apply(player)
        }
        pendingCentralSessionToken?.let { token ->
            player.openRuneCentralSessionToken = token.copyOf()
        }
        pendingCentralSessionToken = null
        player.ui.setWindowStatus(
            width = loginBlock.width,
            height = loginBlock.height,
            resizable = loginBlock.resizable,
        )
        player.newAccount = fromResponse.isNewAccount()
        return player
    }

    private fun applyCentralStaffFromPending(player: Player) {
        pendingCentralRights?.takeIf { it.isNotBlank() }?.let { rights ->
            CharacterAccountApplier.resolveModLevelFromRights(rights)?.let { resolved ->
                player.modLevel = resolved
            }
        }
        pendingCentralRights = null
    }

    private fun Player.applyConfigTransforms(config: RealmConfig) {
        if (!newAccount) {
            return
        }
        coords = config.spawnCoord
        xpRate = config.baseXpRate
        if (config.autoAssignDisplayNames) {
            displayName = username.toDisplayName()
        }
        if (config.devMode) {
            modLevel = devModeModLevel
        }
    }

    // Since logins are processed on the game thread, we isolate player-specific failures to prevent
    // them from affecting the server. Exceptions are caught, logged, and a generic failure response
    // is sent to the player's channel.
    private fun safeHandleGameLogin(player: Player, loadResponse: AccountLoadResponse.Ok) {
        try {
            handleGameLogin(player, loadResponse)
        } catch (e: Exception) {
            writeErrorResponse(LoginResponse.ConnectFail)
            logger.error(e) { "Error handling login for player: $player" }
        }
    }

    private fun handleGameLogin(player: Player, loadResponse: AccountLoadResponse.Ok) {
        if (playerRegistry.isOnline(player.userId)) {
            writeErrorResponse(LoginResponse.Duplicate)
            return
        }

        val characterId = loadResponse.account.characterData.characterId
        val sessionHeldElsewhere =
            runBlocking {
                database.withTransaction { connection ->
                    characterRepository.isActiveSessionOnOtherWorld(
                        connection,
                        characterId,
                        world,
                        ONLINE_SESSION_STALE_SECONDS,
                    )
                }
            }
        if (sessionHeldElsewhere) {
            writeErrorResponse(LoginResponse.Duplicate)
            return
        }

        val slotId = playerRegistry.nextFreeSlot()
        if (slotId == null) {
            writeErrorResponse(LoginResponse.ServerFull)
            return
        }

        val updateState = update.state
        if (updateState.isUpdating()) {
            writeErrorResponse(LoginResponse.UpdateInProgress)
            return
        }

        if (updateState.isCountdown() && updateState.current <= UPDATE_TIMER_REJECT_BUFFER) {
            writeErrorResponse(LoginResponse.UpdateInProgress)
            return
        }

        val response = player.createLoginResponse(slotId, loadResponse.auth)
        val session = channelResponses.writeSuccessfulResponse(response, loginBlock)

        val disconnectionHook = Runnable { player.clientDisconnected.set(true) }
        session.setDisconnectionHook(disconnectionHook)

        // `setDisconnectionHook` will invoke the disconnection hook instantly if the session
        // is not active at this point. Since the channel is no longer connected, we can no-op
        // and return early.
        if (player.clientDisconnected.get()) {
            return
        }

        player.slotId = slotId
        eventBus.publish(SessionStart(player, session))
        val register = playerRegistry.add(player)
        if (register.isSuccess()) {
            eventBus.publish(SessionStateEvent.Login(player))
            eventBus.publish(SessionStateEvent.EngineLogin(player))
            return
        }
        logger.warn { "Failed to register player: $register (player=$player)" }
        session.requestClose()
    }

    private fun Player.createLoginResponse(slotId: Int, auth: AccountLoadAuth) =
        LoginResponse.Ok(
            authenticatorResponse = authenticatorResponse(auth),
            staffModLevel = modLevel.clientCode,
            playerMod = modLevel.hasAccessTo("modlevel.moderator"),
            index = slotId,
            member = members,
            accountHash = accountHash,
            userId = userId,
            userHash = userHash,
        )

    private fun Player.authenticatorResponse(auth: AccountLoadAuth): AuthenticatorResponse =
        when (auth) {
            is AccountLoadAuth.AuthCodeInputTrusted,
            is AccountLoadAuth.AuthCodeInputUntrusted,
            is AccountLoadAuth.TrustedDevice -> {
                val knownDevice = lastKnownDevice ?: randomInt()
                lastKnownDevice = knownDevice
                AuthenticatorResponse.AuthenticatorCode(knownDevice)
            }
            AccountLoadAuth.UnknownDevice -> AuthenticatorResponse.NoAuthenticator
        }

    private fun writeErrorResponse(response: LoginResponse) {
        channelResponses.writeFailedResponse(response)
    }

    private fun runCentralAuth(
        accountName: String,
        loginCharacterId: Int,
    ): Boolean {
        return when (val result = openRuneCentral.authenticate(accountName, inputPassword, loginCharacterId)) {
            CentralAuthResult.Skipped -> {
                // Central world-link not configured; game DB is the authority — do not block login.
                true
            }
            is CentralAuthResult.Denied -> {
                writeErrorResponse(result.response)
                false
            }
            is CentralAuthResult.Ok -> {
                pendingCentralSessionToken = result.sessionToken.copyOf()
                pendingCentralRights = result.centralRights
                true
            }
        }
    }

    private companion object {
        private const val DAYS_BETWEEN_2FA_VERIFICATION = 30

        private const val ONLINE_SESSION_STALE_SECONDS = 120L

        /** Once the game update timer hits this value, we reject any further login requests. */
        private const val UPDATE_TIMER_REJECT_BUFFER = 25

        private val logger = InlineLogger()

        private var Player.newAccount by boolVarBit("varbit.new_player_account")

        // TODO: Decide how to deal with email login usernames.
        private fun String.toDisplayName(): String {
            return trim().split(Regex(" +")).joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        @Suppress("konsist.avoid usage of stdlib Random in functions")
        private fun randomInt(): Int = java.util.concurrent.ThreadLocalRandom.current().nextInt()
    }
}
