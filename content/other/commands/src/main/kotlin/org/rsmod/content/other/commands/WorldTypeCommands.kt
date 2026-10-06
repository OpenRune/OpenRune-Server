package org.rsmod.content.other.commands

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.db.gateway.model.isOk
import org.rsmod.api.game.process.player.WorldTypeSwitchService
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.realm.Realm
import org.rsmod.api.script.onCommand
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Swaps the character's save in place without dropping the connection; see [WorldTypeSwitchService]. */
class WorldTypeCommands
@Inject
constructor(
    private val config: ServerConfig,
    private val realm: Realm,
    private val repository: CharacterAccountRepository,
    private val db: GameDbManager,
    private val switchService: WorldTypeSwitchService,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private val logger = InlineLogger()

    private val served: List<WorldType> by lazy {
        WorldType.supportedFrom(config.worldTypes)
    }

    override fun ScriptContext.startup() {
        onCommand("worldtype") {
            desc = "Pick a world type to play"
            cheat { protectedAccess.launch(player) { worldTypeMenu() } }
        }
    }

    private suspend fun ProtectedAccess.worldTypeMenu() {
        if (served.size < 2) {
            player.mes("This world only serves ${served.first().label}.")
            return
        }
        if (switchService.isSwitching(player)) {
            player.mes("You are already switching world type.")
            return
        }

        val current = player.worldType
        val labels =
            served.map { worldType ->
                if (worldType == current) "${worldType.label} (current)" else worldType.label
            }

        val index = menu("Switch world type", hotkeys = true, choices = labels)
        val target = served.getOrNull(index)
        if (target == null) {
            player.mes("That isn't a world type this world serves.")
            logger.warn { "Menu returned an out-of-range choice: index=$index served=${served.size}" }
            return
        }
        if (target == current) {
            player.mes("You are already playing ${target.label}.")
            return
        }
        switchTo(player, target)
    }

    private fun switchTo(player: Player, target: WorldType) {
        when (val result = switchService.switch(player, target, served, realm.config.spawnCoord)) {
            is WorldTypeSwitchService.Result.Started -> {
                player.mes("Switching to ${target.label}...")
                rememberChoice(player, target)
            }
            is WorldTypeSwitchService.Result.Busy -> player.mes(result.message)
            is WorldTypeSwitchService.Result.AlreadySwitching ->
                player.mes("You are already switching world type.")
            is WorldTypeSwitchService.Result.NotServed ->
                player.mes("This world does not serve ${target.label}.")
            is WorldTypeSwitchService.Result.AlreadyOnWorldType ->
                player.mes("You are already playing ${target.label}.")
        }
    }

    /** Persisted so the next login lands on the same mode without the player picking again. */
    private fun rememberChoice(player: Player, target: WorldType) {
        val accountId = player.accountId
        db.request(
            request = { connection ->
                repository.updateActiveWorldType(connection, accountId, target)
                GameDbResult.Ok(Unit)
            },
            response = { result ->
                if (!result.isOk()) {
                    logger.warn { "Could not store active world type for accountId=$accountId" }
                }
            },
        )
    }
}
