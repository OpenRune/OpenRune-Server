package org.rsmod.content.other.commands

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.mechanics.toxins.impl.PlayerDisease
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.combatClearQueue
import org.rsmod.api.player.godMode
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.realm.Realm
import org.rsmod.api.spells.autocast.MagicSpellbookManager
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.client.DiagnosticClient
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AdminUtilityCommands
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val playerList: PlayerList,
    private val realm: Realm,
    private val spellbooks: MagicSpellbookManager,
) : PluginScript() {
    private val logger = InlineLogger()

    private val diagnosticOutcomeCategories =
        setOf("blocked_invalid_action", "unhandled_handler", "invalid_interface_state")

    override fun ScriptContext.startup() {
        onCommand("god", "Toggle god mode", ::god) {
            invalidArgs = "Use as ::god [on|off] (ex: ::god or ::god off)"
        }
        onCommand("b", "Open bank", ::bank)
        onCommand("teleto", "Teleport to player", ::teleTo) {
            invalidArgs = "Use as ::teleto name (ex: ::teleto zezima)"
        }
        onCommand("teletome", "Teleport player to you", ::teleToMe) {
            invalidArgs = "Use as ::teletome name (ex: ::teletome zezima)"
        }
        onCommand("gwd", "Teleport to God Wars Dungeon", ::gwd)
        onCommand("bandos", "Teleport to Bandos boss door", ::bandos)
        onCommand("spellbook", "Switch active magic spellbook", ::spellbook) {
            invalidArgs = SPELLBOOK_USAGE
        }
        onCommand("statedump", "Write explorer state dump to server log", ::stateDump)
        onCommand("diagoutcome", "Record an explorer diagnostic outcome", ::diagnosticOutcome)
    }

    private fun god(cheat: Cheat) =
        with(cheat) {
            val enabled =
                when (val arg = args.getOrNull(0)?.lowercase()) {
                    null -> !player.godMode
                    "on", "true", "1", "yes" -> true
                    "off", "false", "0", "no" -> false
                    else -> {
                        player.mes("Use as ::god [on|off].")
                        return@with
                    }
                }

            player.godMode = enabled
            if (enabled) {
                player.combatClearQueue()
                PlayerPoison.clear(player)
                PlayerVenom.clear(player)
                PlayerDisease.clear(player)
                player.statHeal("stat.hitpoints", constant = 0, percent = 100)
            }
            player.mes(
                if (enabled) {
                    "God mode enabled. Equipment bonuses are boosted by +1000."
                } else {
                    "God mode disabled."
                }
            )
        }

    private fun bank(cheat: Cheat) =
        with(cheat) {
            protectedAccess.launch(player) {
                ifOpenMainSidePair(main = "interface.bankmain", side = "interface.bankside")
            }
        }

    private fun teleTo(cheat: Cheat) =
        with(cheat) {
            val target = findOnlinePlayerArgOrNull("Use as ::teleto name.") ?: return@with
            val targetName = target.commandDisplayName()
            if (target === player) {
                player.mes("You are already at your own position.")
                return@with
            }
            val dest = target.coords
            val launched =
                protectedAccess.launch(player) {
                    player.mes("Teleported to `$targetName`.")
                    telejump(dest)
                }
            if (!launched) {
                player.mes("You are busy.")
            }
        }

    private fun teleToMe(cheat: Cheat) =
        with(cheat) {
            val target = findOnlinePlayerArgOrNull("Use as ::teletome name.") ?: return@with
            val targetName = target.commandDisplayName()
            if (target === player) {
                player.mes("You are already here.")
                return@with
            }
            val admin = player
            val adminName = admin.commandDisplayName()
            val dest = admin.coords
            val launched =
                protectedAccess.launch(target) {
                    player.mes("Teleported to `$adminName`.")
                    telejump(dest)
                }
            if (launched) {
                admin.mes("Teleported `$targetName` to you.")
            } else {
                admin.mes("`$targetName` is busy.")
            }
        }

    private fun gwd(cheat: Cheat) =
        with(cheat) {
            val coords = CoordGrid(2881, 5310, 2)
            protectedAccess.launch(player) {
                player.mes("Teleported to $coords.")
                telejump(coords)
            }
        }

    private fun bandos(cheat: Cheat) =
        with(cheat) {
            val coords = CoordGrid(2862, 5354, 2)
            protectedAccess.launch(player) {
                player.mes("Teleported to $coords.")
                telejump(coords)
            }
        }

    private fun spellbook(cheat: Cheat) =
        with(cheat) {
            if (args.size != 1) {
                player.mes(SPELLBOOK_USAGE)
                return@with
            }

            val spellbook = parseSpellbook(args[0])
            if (spellbook == null) {
                player.mes(SPELLBOOK_USAGE)
                return@with
            }

            when (spellbooks.setSpellbook(player, spellbook)) {
                is MagicSpellbookManager.ChangeResult.Changed -> {
                    player.mes("Active spellbook: ${spellbook.displayName}. Autocast cleared.")
                }
                is MagicSpellbookManager.ChangeResult.Unchanged -> {
                    player.mes("Active spellbook is already ${spellbook.displayName}.")
                }
            }
        }

    private fun stateDump(cheat: Cheat) =
        with(cheat) {
            if (!realm.config.devMode) {
                player.mes("State dump is only available in dev mode.")
                return@with
            }
            val diagnostics = player.client as? DiagnosticClient
            if (diagnostics == null) {
                player.mes("No session diagnostics are attached to this client.")
                return@with
            }
            logger.info { diagnostics.diagnosticsStateDump() }
            player.mes("State dump written to server log.")
        }

    private fun diagnosticOutcome(cheat: Cheat) =
        with(cheat) {
            if (!realm.config.devMode) {
                player.mes("Diagnostic outcomes are only available in dev mode.")
                return@with
            }
            val diagnostics = player.client as? DiagnosticClient
            if (diagnostics == null) {
                player.mes("No session diagnostics are attached to this client.")
                return@with
            }
            val category = args.getOrNull(0) ?: "blocked_invalid_action"
            if (category !in diagnosticOutcomeCategories) {
                player.mes("Use as ::diagoutcome [${diagnosticOutcomeCategories.joinToString("|")}]")
                return@with
            }
            diagnostics.recordDiagnosticOutcome(
                category,
                "manual diagnostic outcome command by ${player.displayName}",
            )
            logger.info { diagnostics.diagnosticsStateDump() }
            player.mes("Diagnostic outcome '$category' written to server log.")
        }

    private fun parseSpellbook(arg: String): Spellbook? =
        when (arg.lowercase().removeSuffix("s").replace("-", "_")) {
            "standard", "normal" -> Spellbook.Standard
            "ancient" -> Spellbook.Ancients
            "lunar" -> Spellbook.Lunars
            "arceuus" -> Spellbook.Arceuus
            else -> null
        }

    private val Spellbook.displayName: String
        get() =
            when (this) {
                Spellbook.Standard -> "Standard"
                Spellbook.Ancients -> "Ancients"
                Spellbook.Lunars -> "Lunar"
                Spellbook.Arceuus -> "Arceuus"
            }

    private fun Cheat.findOnlinePlayerArgOrNull(usage: String): Player? {
        val rawName = args.joinToString(" ").trim()
        if (rawName.isBlank()) {
            player.mes(usage)
            return null
        }
        val target = findOnlinePlayer(rawName)
        if (target == null) {
            player.mes("Could not find online player `$rawName`.")
            return null
        }
        return target
    }

    private fun findOnlinePlayer(name: String): Player? {
        val normalized = name.normalizedPlayerName()
        return playerList.firstOrNull { it.matchesPlayerName(normalized) }
    }

    private fun Player.matchesPlayerName(normalizedName: String): Boolean =
        username.normalizedPlayerName() == normalizedName ||
            displayName.normalizedPlayerName() == normalizedName ||
            previousDisplayName.normalizedPlayerName() == normalizedName

    private fun Player.commandDisplayName(): String = displayName.ifBlank { username }

    private fun String.normalizedPlayerName(): String =
        replace('_', ' ').trim().replace(PlayerNameWhitespaceRegex, " ").lowercase()

    private companion object {
        private const val SPELLBOOK_USAGE =
            "Use as ::spellbook standard|ancients|lunar|arceuus"

        private val PlayerNameWhitespaceRegex = Regex("\\s+")
    }
}
