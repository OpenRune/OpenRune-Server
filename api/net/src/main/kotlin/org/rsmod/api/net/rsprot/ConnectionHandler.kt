package org.rsmod.api.net.rsprot

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import net.rsprot.crypto.xtea.XteaKey
import net.rsprot.protocol.api.GameConnectionHandler
import net.rsprot.protocol.api.login.GameLoginResponseHandler
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import net.rsprot.protocol.loginprot.incoming.util.OtpAuthenticationType
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import org.rsmod.api.account.AccountManager
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.loader.request.AccountLoadAuth
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.game.process.PluginScriptBootGate
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.net.rsprot.player.AccountLoadResponseHook
import org.rsmod.api.realm.Realm
import org.rsmod.api.registry.account.AccountRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.totp.Totp
import org.rsmod.api.totp.laravel.TwoFactorSecretResolver
import org.rsmod.api.totp.useSecret
import org.rsmod.events.EventBus
import org.rsmod.game.GameUpdate
import org.rsmod.game.entity.Player

class ConnectionHandler
@Inject
private constructor(
    private val realm: Realm,
    private val config: ServerConfig,
    private val update: GameUpdate,
    private val eventBus: EventBus,
    private val playerReg: PlayerRegistry,
    private val accountReg: AccountRegistry,
    private val accountManager: AccountManager,
    private val totp: Totp,
    private val twoFactorSecretResolver: TwoFactorSecretResolver,
    private val openRuneCentral: OpenRuneCentralWorldLink,
    private val gameDatabase: GameDatabase,
    private val characterAccountRepository: CharacterAccountRepository,
    private val scriptBootGate: PluginScriptBootGate,
) : GameConnectionHandler<Player> {
    private val logger = InlineLogger()

    private val world: Int
        get() = config.world

    private companion object {
        private const val PASSWORD_HASH_TIMING_INFO_MS = 50L
    }

    override fun onLogin(
        responseHandler: GameLoginResponseHandler<Player>,
        block: LoginBlock<AuthenticationType>,
    ) {
        if (!scriptBootGate.isReady()) {
            responseHandler.writeFailedResponse(LoginResponse.LoginServerOffline)
            return
        }

        if (accountManager.isLoaderShuttingDown()) {
            responseHandler.writeFailedResponse(LoginResponse.LoginServerOffline)
            return
        }

        if (accountManager.isLoaderRejectingRequests()) {
            responseHandler.writeFailedResponse(LoginResponse.LoginServerNoReply)
            return
        }

        when (val auth = block.authentication) {
            is AuthenticationType.PasswordAuthentication -> passLogin(responseHandler, block, auth)
            is AuthenticationType.TokenAuthentication -> tokenLogin(responseHandler, block, auth)
        }
    }

    private fun passLogin(
        responseHandler: GameLoginResponseHandler<Player>,
        block: LoginBlock<AuthenticationType>,
        auth: AuthenticationType.PasswordAuthentication,
    ) {
        val password = auth.password.asCharArray()
        try {
            passLogin(responseHandler, block, auth, password)
        } finally {
            // `password` char array is already cleared during `computePasswordHash`, but that is
            // an implementation detail in the password hashing interface; we ensure to clear it
            // after usage regardless.
            password.fill('\u0000')
            auth.password.clear()
        }
    }

    private fun passLogin(
        responseHandler: GameLoginResponseHandler<Player>,
        block: LoginBlock<AuthenticationType>,
        auth: AuthenticationType.PasswordAuthentication,
        password: CharArray,
    ) {
        // This may be filtered earlier at the protocol layer (e.g., rsprot), but we defensively
        // check again to ensure the password is not empty.
        if (password.isEmpty()) {
            responseHandler.writeFailedResponse(LoginResponse.InvalidUsernameOrPassword)
            return
        }
        // Capture a local snapshot, as `realm.config` is mutable and may change.
        val realmConfig = realm.config
        if (!openRuneCentral.isEnabled) {
            logger.error {
                "OpenRune Central is required for login but is not configured. " +
                    "Set `central` in game.yml (`host` + `world-key`, or `same-instance: true` for embedded), " +
                    "or env OPENRUNE_CENTRAL_HOST and OPENRUNE_WORLD_KEY."
            }
            responseHandler.writeFailedResponse(LoginResponse.LoginServerOffline)
            return
        }
        val loadAuth = auth.otpAuthentication.toAccountLoadAuth()
        val username = block.username

        val responseHook =
            AccountLoadResponseHook(
                world = world,
                config = realmConfig,
                loginTimingLogs = config.loginTimingLogs,
                update = update,
                eventBus = eventBus,
                accountRegistry = accountReg,
                playerRegistry = playerReg,
                loginBlock = block,
                channelResponses = responseHandler,
                inputPassword = password.copyOf(),
                verifyTotp = ::verifyTotp,
                resolveTotpSecret = twoFactorSecretResolver::resolveStoredSecret,
                openRuneCentral = openRuneCentral,
                database = gameDatabase,
                characterRepository = characterAccountRepository,
            )

        // Central auth runs after the account row exists (see AccountLoadResponseHook) so character id
        // and password are available; do not kick off here with a null character id.
        val passwordForLocalCreate = password.copyOf()
        val requestSubmitted =
            accountManager.loadOrCreate(
                loadAuth,
                username,
                {
                    try {
                        val hashStart = System.nanoTime()
                        val hash = computePasswordHash(passwordForLocalCreate)
                        val hashMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - hashStart)
                        if (config.loginTimingLogs && hashMs >= PASSWORD_HASH_TIMING_INFO_MS) {
                            logger.info { "Login password hash (new local account) user='$username' elapsed=${hashMs}ms" }
                        }
                        hash ?: error("Password hash failed")
                    } finally {
                        passwordForLocalCreate.fill('\u0000')
                    }
                },
                responseHook,
            )

        if (!requestSubmitted) {
            passwordForLocalCreate.fill('\u0000')
            responseHandler.writeFailedResponse(LoginResponse.LoginServerLoadError)
        }
    }

    private fun computePasswordHash(password: CharArray): String? {
        return try {
            val plain = password.concatToString()
            openRuneCentral.passwordHasher().hash(plain)
        } catch (e: Exception) {
            logger.error { "Password hashing error: ${e::class.simpleName}" }
            null
        }
    }

    private fun verifyTotp(secret: CharArray, code: String): Boolean {
        return try {
            useSecret(secret) { totp.verifyCode(it, code) }
        } catch (e: Exception) {
            logger.error { "Totp verification error: ${e::class.simpleName}" }
            false
        }
    }

    // TODO: Token authentication handling.
    private fun tokenLogin(
        responseHandler: GameLoginResponseHandler<Player>,
        block: LoginBlock<AuthenticationType>,
        @Suppress("unused") auth: AuthenticationType.TokenAuthentication,
    ) {
        logger.warn { "Unhandled login authentication for: $block" }
        responseHandler.writeFailedResponse(LoginResponse.InvalidLoginPacket)
    }

    private fun OtpAuthenticationType.toAccountLoadAuth(): AccountLoadAuth =
        when (this) {
            is OtpAuthenticationType.NoMultiFactorAuthentication -> {
                AccountLoadAuth.UnknownDevice
            }
            is OtpAuthenticationType.TrustedAuthenticator -> {
                AccountLoadAuth.AuthCodeInputTrusted(otp)
            }
            is OtpAuthenticationType.UntrustedAuthentication -> {
                AccountLoadAuth.AuthCodeInputUntrusted(otp)
            }
            is OtpAuthenticationType.TrustedComputer -> {
                AccountLoadAuth.TrustedDevice(identifier)
            }
        }

    override fun onReconnect(
        responseHandler: GameLoginResponseHandler<Player>,
        block: LoginBlock<XteaKey>,
    ) {
        // TODO: Reconnection.
        responseHandler.writeFailedResponse(LoginResponse.ConnectFail)
    }
}
