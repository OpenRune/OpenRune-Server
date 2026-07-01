package org.rsmod.api.net.rsprot.player

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.types.ModLevelType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import net.rsprot.protocol.api.login.GameLoginResponseHandler
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import net.rsprot.protocol.loginprot.outgoing.util.AuthenticatorResponse
import org.rsmod.api.account.character.main.CharacterAccountApplier
import dev.or2.central.account.AccountData
import dev.or2.central.account.Rights
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.loader.request.AccountLoadAuth
import org.rsmod.api.account.loader.request.AccountLoadCallback
import org.rsmod.api.account.loader.request.AccountLoadResponse
import org.rsmod.api.account.loader.request.isNewAccount
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.net.central.CentralAuthResult
import org.rsmod.api.net.central.InflightCentralAuth
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
import org.rsmod.map.CoordGrid

class AccountLoadResponseHook(
    private val world: Int,
    private val config: RealmConfig,
    private val loginTimingLogs: Boolean,
    private val update: GameUpdate,
    private val eventBus: EventBus,
    private val accountRegistry: AccountRegistry,
    private val playerRegistry: PlayerRegistry,
    private val devModeModLevel: ModLevelType,
    private val loginBlock: LoginBlock<AuthenticationType>,
    private val channelResponses: GameLoginResponseHandler<Player>,
    private val inputPassword: CharArray,
    private val verifyTotp: (CharArray, String) -> Boolean,
    private val resolveTotpSecret: (String) -> CharArray?,
    private val openRuneCentral: OpenRuneCentralWorldLink,
    private val database: GameDatabase,
    private val characterRepository: CharacterAccountRepository,
) : AccountLoadCallback {
    private var pendingCentralSessionToken: ByteArray? = null

    private var pendingCentralRights: String? = null

    private var pendingTrustedDevice: PendingTrustedDevice? = null

    private var inflightCentralAuth: InflightCentralAuth? = null

    internal fun kickoffInflightCentralAuth(
        accountName: String,
        loginCharacterId: Int?,
    ) {
        if (!openRuneCentral.isEnabled) {
            return
        }
        if (loginCharacterId == null || loginCharacterId <= 0) {
            return
        }
        inflightCentralAuth =
            openRuneCentral.beginAuthenticate(
                accountName,
                inputPassword,
                loginCharacterId,
            )
    }

    private data class PendingTrustedDevice(
        val deviceId: Int,
        val verifiedAt: LocalDateTime,
    )

    override fun invoke(response: AccountLoadResponse) {
        try {
            handleLoadResponse(response)
        } finally {
            // Ok responses continue on the game thread; secrets and pending state are cleared there
            // or via writeErrorResponse on early failures.
            if (response !is AccountLoadResponse.Ok) {
                clearLoginSecrets()
                pendingCentralSessionToken?.fill(0)
                pendingCentralSessionToken = null
                pendingCentralRights = null
                pendingTrustedDevice = null
            }
        }
    }

    private fun handleLoadResponse(response: AccountLoadResponse) {
        when (response) {
            is AccountLoadResponse.Ok.NewAccount -> {
                kickoffInflightCentralAuth(
                    response.account.accountName,
                    response.account.characterData.characterId,
                )
                safeQueueLogin(response)
            }
            is AccountLoadResponse.Ok.LoadAccount -> {
                kickoffInflightCentralAuth(
                    response.account.accountName,
                    response.account.characterData.characterId,
                )
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
        val twoFactor = response.account.twoFactorAuth
        if (twoFactor.twoFactorConfirmed) {
            val storedSecret = twoFactor.twoFactorSecret
            if (storedSecret == null) {
                writeErrorResponse(LoginResponse.InvalidAuthenticatorCode)
                logger.error { "Two-factor enabled without a stored secret: ${response.account}" }
                return
            }

            val secret = resolveTotpSecret(storedSecret)
            if (secret == null) {
                writeErrorResponse(LoginResponse.InvalidAuthenticatorCode)
                logger.error {
                    "Could not decode two-factor secret for ${response.account.accountName} " +
                        "(set app-key in api/totp/src/main/resources/laravel-settings.yml for Laravel secrets)"
                }
                return
            }

            try {
                val authResponse = validateTwoFactor(response.account, response.auth, secret)
                if (authResponse != null) {
                    writeErrorResponse(authResponse)
                    return
                }
            } finally {
                secret.fill('\u0000')
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
                val correctCode = verifyTotp(secret, auth.otp.toString().padStart(6, '0'))
                if (!correctCode) {
                    LoginResponse.InvalidAuthenticatorCode
                } else {
                    val verifiedAt = LocalDateTime.now()
                    val resolved = resolveVerifiedTrustedDevice(account, auth)
                    pendingTrustedDevice = PendingTrustedDevice(resolved.deviceId, verifiedAt)
                    null
                }
            }
        }

    private data class ResolvedTrustedDevice(
        val deviceId: Int,
        val isNewDevice: Boolean,
    )

    private fun resolveVerifiedTrustedDevice(
        account: AccountData,
        auth: AccountLoadAuth.CodeInput,
    ): ResolvedTrustedDevice =
        when (auth) {
            is AccountLoadAuth.AuthCodeInputUntrusted -> ResolvedTrustedDevice(randomInt(), isNewDevice = true)
            is AccountLoadAuth.AuthCodeInputTrusted -> {
                val reauthDeviceId = TrustedDeviceReauthStore.take(account.accountName)
                if (reauthDeviceId != null) {
                    ResolvedTrustedDevice(reauthDeviceId, isNewDevice = false)
                } else {
                    ResolvedTrustedDevice(randomInt(), isNewDevice = true)
                }
            }
        }

    private fun requiresTwoFactorAuth(
        account: AccountData,
        auth: AccountLoadAuth.InitialRequest,
    ): Boolean {
        val deviceAuth =
            when (auth) {
                is AccountLoadAuth.TrustedDevice -> TrustedDeviceAuth.Known(auth.identifier)
                AccountLoadAuth.UnknownDevice -> TrustedDeviceAuth.Unknown
            }
        val requiresAuth =
            TrustedDevicePolicy.requiresTwoFactor(
                trustedDevices = account.trustedDevices,
                auth = deviceAuth,
            )
        if (requiresAuth) {
            TrustedDevicePolicy.rememberReauthIfNeeded(
                accountName = account.accountName,
                trustedDevices = account.trustedDevices,
                auth = deviceAuth,
            )
        }
        return requiresAuth
    }

    private fun safeQueueLogin(response: AccountLoadResponse.Ok) {
        try {
            val player = createPlayer(response).apply { applyConfigTransforms(config) }
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
        pendingTrustedDevice?.let { pending ->
            upsertTrustedDevice(player.trustedDevices, pending.deviceId, pending.verifiedAt)
            player.lastKnownDevice = pending.deviceId
            pendingTrustedDevice = null
        }
        player.ui.setWindowStatus(
            width = loginBlock.width,
            height = loginBlock.height,
            resizable = loginBlock.resizable,
        )
        player.newAccount = fromResponse.isNewAccount()
        return player
    }

    private fun applyCentralSessionToPlayer(player: Player) {
        pendingCentralSessionToken?.let { token ->
            player.openRuneCentralSessionToken = token.copyOf()
        }
        pendingCentralSessionToken = null
        applyCentralStaffFromPending(player)
    }

    private fun applyCentralStaffFromPending(player: Player) {
        pendingCentralRights?.takeIf { it.isNotBlank() }?.let { rights ->
            CharacterAccountApplier.resolveModLevelFromRights(Rights.fromRightsColumn(rights))?.let { resolved ->
                player.modLevel = resolved
            }
        }
        pendingCentralRights = null
    }

    public val LOGIN_EXIT_COORD: AttributeKey<Int> = AttributeKey(persistenceKey = "instance_exit_coord")

    private fun Player.applyConfigTransforms(config: RealmConfig) {
        if (!newAccount) {
            //This is very hacky but updating be weird
            val hasExit = attr[LOGIN_EXIT_COORD]
            if (hasExit != null) {
                coords = CoordGrid(hasExit)
                attr.remove(LOGIN_EXIT_COORD)
            }
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
        val startedAt = System.nanoTime()
        try {
            handleGameLogin(player, loadResponse)
        } catch (e: Exception) {
            writeErrorResponse(LoginResponse.ConnectFail)
            logger.error(e) { "Error handling login for player: $player" }
        } finally {
            val elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            if (!loginTimingLogs) {
                return
            }
            if (elapsedMs >= GAME_LOGIN_TIMING_INFO_MS) {
                val message =
                    "Login game-thread user='${player.username}' characterId=${loadResponse.account.characterData.characterId} " +
                        "elapsed=${elapsedMs}ms"
                if (elapsedMs >= GAME_LOGIN_TIMING_WARN_MS) {
                    logger.warn { message }
                } else {
                    logger.info { message }
                }
            }
        }
    }

    private fun handleGameLogin(player: Player, loadResponse: AccountLoadResponse.Ok) {
        if (playerRegistry.isOnline(player.userId)) {
            writeErrorResponse(LoginResponse.Duplicate)
            return
        }

        val characterId = loadResponse.account.characterData.characterId
        val duplicateCheckStart = System.nanoTime()
        val sessionHeldElsewhere =
            database.withTransactionBlocking { connection ->
                characterRepository.isActiveSessionOnOtherWorld(
                    connection,
                    characterId,
                    world,
                    ONLINE_SESSION_STALE_SECONDS,
                )
            }
        val duplicateCheckMs =
            java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - duplicateCheckStart)
        if (loginTimingLogs && duplicateCheckMs >= GAME_LOGIN_TIMING_INFO_MS) {
            logger.info {
                "Login duplicate-check user='${player.username}' characterId=$characterId elapsed=${duplicateCheckMs}ms"
            }
        }
        if (sessionHeldElsewhere) {
            writeErrorResponse(LoginResponse.Duplicate)
            return
        }

        if (!runCentralAuth(loadResponse.account.accountName, characterId)) {
            return
        }
        applyCentralSessionToPlayer(player)

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
        val responseStart = System.nanoTime()
        val session = channelResponses.writeSuccessfulResponse(response, loginBlock)
        val responseMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - responseStart)

        val disconnectionHook = Runnable { player.clientDisconnected.set(true) }
        session.setDisconnectionHook(disconnectionHook)

        // `setDisconnectionHook` will invoke the disconnection hook instantly if the session
        // is not active at this point. Since the channel is no longer connected, we can no-op
        // and return early.
        if (player.clientDisconnected.get()) {
            return
        }

        player.slotId = slotId
        val eventsStart = System.nanoTime()
        eventBus.publish(SessionStart(player, session))
        val register = playerRegistry.add(player)
        if (register.isSuccess()) {
            persistTrustedDevicesIfNeeded(loadResponse.account.accountId, player)
            eventBus.publish(SessionStateEvent.Login(player))
            eventBus.publish(SessionStateEvent.EngineLogin(player))
            val eventsMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - eventsStart)
            if (loginTimingLogs && (eventsMs >= GAME_LOGIN_TIMING_INFO_MS || responseMs >= GAME_LOGIN_TIMING_INFO_MS)) {
                logger.info {
                    "Login game-thread phases user='${player.username}' duplicateCheck=${duplicateCheckMs}ms " +
                        "writeResponse=${responseMs}ms events=${eventsMs}ms"
                }
            }
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
            is AccountLoadAuth.AuthCodeInputUntrusted -> {
                val deviceId = lastKnownDevice ?: randomInt()
                lastKnownDevice = deviceId
                AuthenticatorResponse.AuthenticatorCode(deviceId)
            }
            is AccountLoadAuth.AuthCodeInputTrusted -> {
                val deviceId = lastKnownDevice ?: randomInt()
                lastKnownDevice = deviceId
                AuthenticatorResponse.AuthenticatorCode(deviceId)
            }
            is AccountLoadAuth.TrustedDevice -> {
                lastKnownDevice = auth.identifier
                AuthenticatorResponse.AuthenticatorCode(auth.identifier)
            }
            AccountLoadAuth.UnknownDevice -> AuthenticatorResponse.NoAuthenticator
        }

    private fun persistTrustedDevicesIfNeeded(accountId: Int, player: Player) {
        if (player.trustedDevices.isEmpty()) {
            return
        }
        database.withTransactionBlocking { connection ->
            characterRepository.saveTrustedDevices(connection, accountId, player.trustedDevices)
        }
    }

    private fun writeErrorResponse(response: LoginResponse) {
        clearLoginSecrets()
        channelResponses.writeFailedResponse(response)
    }

    private fun clearLoginSecrets() {
        inputPassword.fill('\u0000')
        inflightCentralAuth = null
    }

    private fun runCentralAuth(
        accountName: String,
        loginCharacterId: Int,
    ): Boolean {
        val result = resolveCentralAuth(accountName, loginCharacterId)
        return when (result) {
            is CentralAuthResult.Ok -> {
                pendingCentralSessionToken = result.sessionToken.copyOf()
                pendingCentralRights = result.centralRights
                clearLoginSecrets()
                true
            }
            CentralAuthResult.Skipped -> {
                clearLoginSecrets()
                // Central world-link not configured; game DB is the authority — do not block login.
                true
            }
            is CentralAuthResult.Denied -> {
                writeErrorResponse(result.response)
                false
            }
        }
    }

    private fun resolveCentralAuth(
        accountName: String,
        loginCharacterId: Int,
    ): CentralAuthResult {
        val inflight = inflightCentralAuth
        if (inflight != null) {
            inflightCentralAuth = null
            return openRuneCentral.awaitInflight(inflight)
        }
        return openRuneCentral.authenticate(
            accountName,
            inputPassword,
            loginCharacterId.takeIf { it > 0 },
        )
    }

    private companion object {
        private const val ONLINE_SESSION_STALE_SECONDS = 120L

        /** Once the game update timer hits this value, we reject any further login requests. */
        private const val UPDATE_TIMER_REJECT_BUFFER = 25

        private const val GAME_LOGIN_TIMING_INFO_MS = 100L

        private const val GAME_LOGIN_TIMING_WARN_MS = 600L

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
