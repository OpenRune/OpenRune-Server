package org.rsmod.content.other.commands

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.constants
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statRestoreAll
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.PlayerInterfaceUpdates
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DevCommands
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val playerList: PlayerList,
) : PluginScript() {
    private lateinit var cheats: CheatCommandMap

    override fun ScriptContext.startup() {
        cheats = cheatCommandMap
        onCommand("commands", "List available commands", ::commands)
        onCommand("setlevel", "Set one stat's level", ::setLevel) {
            invalidArgs = "Use as ::setlevel stat level (ex: attack 70, hp 99)"
        }
        onCommand("heal", "Restore stats, hitpoints, prayer and run energy", ::heal)
        onCommand("players", "List online players", ::players)
        onCommand("teleto", "Teleport to a player", ::teleTo) {
            invalidArgs = "Use as ::teleto playerName"
        }
    }

    private fun commands(cheat: Cheat) =
        with(cheat) {
            val filter = args.joinToString("_")
            val matches = cheats.commands.entries.filter { it.key.contains(filter) }
            if (matches.isEmpty()) {
                player.mes("No commands match: '$filter'")
                return
            }
            val lines =
                matches
                    .sortedBy { it.key }
                    .map { (name, handler) -> "<col=800000>::$name</col> - ${handler.desc}" }
            val launched =
                protectedAccess.launch(player) {
                    ifOpenMainModal("interface.longscroll")
                    runClientScript(
                        LONGSCROLL_SETUP.asRSCM(RSCMType.CLIENTSCRIPT),
                        "Commands (${matches.size})",
                        lines.joinToString("<br>"),
                    )
                }
            if (!launched) {
                player.mes(constants.dm_busy)
            }
        }

    private fun setLevel(cheat: Cheat): Unit =
        with(cheat) {
            val type = findStat(args[0])
            if (type == null) {
                player.mes("Unknown stat: '${args[0]}'")
                return
            }
            val stat = statName(type)
            val level = args[1].toInt().coerceIn(type.minLevel, type.maxLevel)
            player.setStatLevel(stat, level)
            player.mes("Set ${stat.removePrefix("stat.")} to level $level.")
        }

    private fun heal(cheat: Cheat) =
        with(cheat) {
            player.statRestoreAll(allStats())
            player.runEnergy = constants.run_max_energy
            UpdateRun.energy(player, player.runEnergy)
            player.mes("You have been healed.")
        }

    private fun players(cheat: Cheat) =
        with(cheat) {
            val online = playerList.toList()
            player.mes("Players online (${online.size}):")
            for (other in online.sortedBy { it.displayName.lowercase() }) {
                player.mes("  ${other.displayName} - ${other.coords}")
            }
        }

    private fun teleTo(cheat: Cheat): Unit =
        with(cheat) {
            // Client cheats arrive lowercased and split on spaces; display names use spaces.
            val name = args.joinToString(" ").replace("_", " ")
            if (name.isBlank()) {
                player.mes("Use as ::teleto playerName")
                return
            }
            val target = playerList.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
            if (target == null) {
                player.mes("No player online named: '$name'")
                return
            }
            if (target === player) {
                player.mes("You can't teleport to yourself.")
                return
            }
            val label = "${target.displayName} (${target.coords})"
            protectedAccess.devTeleport(player, target.coords, label)
        }

    @OptIn(InternalApi::class)
    private fun Player.setStatLevel(stat: String, targetLevel: Int) {
        val xp = PlayerSkillXPTable.getXPFromLevel(targetLevel)
        val baseLevel = statMap.getBaseLevel(stat)
        if (baseLevel > targetLevel) {
            statMap.setCurrentLevel(stat, baseLevel)
            statMap.setXP(stat, xp)
            statMap.setBaseLevel(stat, targetLevel.toByte())
            statSub(stat, constant = stat(stat) - targetLevel, percent = 0)
            appearance.combatLevel = PlayerSkillXP.calculateCombatLevel(this)
            PlayerInterfaceUpdates.updateCombatLevel(this)
            return
        }
        statMap.setCurrentLevel(stat, targetLevel.toByte())
        statAdvance(stat, (xp - statMap.getXP(stat)).toDouble(), rate = 1.0)
    }

    private fun findStat(arg: String): StatType? {
        val key = STAT_ALIASES[arg] ?: arg
        val stats = ServerCacheManager.getStats().values
        val named = stats.associateBy { statName(it).removePrefix("stat.") }
        return named[key] ?: named.filterKeys { it.startsWith(key) }.values.singleOrNull()
    }

    private fun allStats(): List<String> = ServerCacheManager.getStats().values.map(::statName)

    private fun statName(type: StatType): String = RSCM.getReverseMapping(RSCMType.STAT, type.id)

    private companion object {
        /**
         * `[clientscript,longscroll_setup](string title, string text)`: sets the title and text of
         * `longscroll` and sizes its scroll layer to the text.
         */
        const val LONGSCROLL_SETUP = "clientscript.[clientscript,longscroll_setup]"

        val STAT_ALIASES =
            mapOf(
                "att" to "attack",
                "def" to "defence",
                "str" to "strength",
                "hp" to "hitpoints",
                "range" to "ranged",
                "pray" to "prayer",
                "mage" to "magic",
                "wc" to "woodcutting",
                "fm" to "firemaking",
                "rc" to "runecrafting",
                "con" to "construction",
            )
    }
}

/**
 * Standard spellbook teleport: anim 714, spotanim 111 at height 92 and synth 200, arriving 3 ticks
 * after the cast with anim 715. 714 ends with the model hidden, so 715 is required to show the
 * player again. The wiki gives no tick timing; values match Reign's `JeweleryTeleports` (rev 240).
 */
internal fun ProtectedAccessLauncher.devTeleport(player: Player, dest: CoordGrid, label: String) {
    val launched =
        launch(player) {
            anim("seq.human_castteleport")
            spotanim("spotanim.teleport_casting", height = 92)
            soundSynth("synth.teleport_all")
            delay(3)
            telejump(dest, TeleportType.Exempt)
            anim("seq.human_castteleport_reverse")
            mes("Teleported to $label.")
        }
    if (!launched) {
        player.mes(constants.dm_busy)
    }
}

/** Handles `::tele name`; returns false when the args aren't a location name. */
internal fun ProtectedAccessLauncher.teleNamed(cheat: Cheat): Boolean {
    val first = cheat.args.firstOrNull() ?: return false
    if (first.toIntOrNull() != null || ',' in first) {
        return false
    }
    val name = cheat.args.joinToString("_")
    val location = TeleLocations.find(name)
    if (location == null) {
        cheat.player.mes("Unknown location: '$name'. Locations:")
        TeleLocations.all.map { it.name }.chunked(6).forEach { cheat.player.mes(it.joinToString()) }
        return true
    }
    devTeleport(cheat.player, location.coords, "${location.name} (${location.coords})")
    return true
}
