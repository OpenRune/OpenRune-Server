package org.rsmod.content.other.commands

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
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
import org.rsmod.game.map.Direction
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
            val matches =
                cheats.commands.entries.filter { it.key.contains(filter) }.sortedBy { it.key }
            if (matches.isEmpty()) {
                player.mes("No commands match: '$filter'")
                return
            }
            val lines =
                matches.flatMap { (name, handler) -> wrap("::$name", handler.desc.orEmpty()) }
            val launched =
                protectedAccess.launch(player) {
                    ifOpenMain("interface.questjournal")
                    runClientScript(QUEST_JOURNAL_RESET.asRSCM(RSCMType.CLIENTSCRIPT))
                    ifSetText("component.questjournal:title", "Commands (${matches.size})")
                    val shown = lines.take(QUEST_JOURNAL_LINES)
                    shown.forEachIndexed { index, line ->
                        ifSetText("component.questjournal:qj${index + 1}", line)
                    }
                    runClientScript(
                        QUEST_JOURNAL_SCROLLBAR.asRSCM(RSCMType.CLIENTSCRIPT),
                        1,
                        shown.size,
                    )
                }
            if (!launched) {
                player.mes(constants.dm_busy)
            }
        }

    /** Word-wraps "name - desc" into journal lines, with the name in the quest-log red. */
    private fun wrap(name: String, desc: String): List<String> {
        val lines = mutableListOf<String>()
        var line = StringBuilder("<col=800000>$name</col> -")
        var visible = name.length + 2
        for (word in desc.split(' ')) {
            if (visible + 1 + word.length > LINE_CHARS) {
                lines += line.toString()
                line = StringBuilder(word)
                visible = word.length
            } else {
                line.append(' ').append(word)
                visible += 1 + word.length
            }
        }
        lines += line.toString()
        return lines
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
        /** Clears every `questjournal:qj*` line (`enum_1968` lists them). */
        const val QUEST_JOURNAL_RESET = "clientscript.[clientscript,quest_journal_reset]"

        /**
         * `script2523(boolean reset_scroll, int lines)`: sizes `questjournal:textlayer` to the line
         * count and builds its scrollbar. Without it the journal doesn't scroll.
         */
        const val QUEST_JOURNAL_SCROLLBAR = "clientscript.[clientscript,script2523]"

        /** `questjournal:qj1` to `qj210`. */
        const val QUEST_JOURNAL_LINES = 210

        /** Characters per journal line; upstream's quest log wraps at 64. */
        const val LINE_CHARS = 60

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

/**
 * Jagex's `level,mx,mz,lx,lz` coordinate format, which the client sends for `::tele n 2` (it turns
 * the relative move into absolute coordinates before the server sees it).
 */
private fun jagexCoordGrid(args: List<String>): CoordGrid? {
    val parts = (if (args.size == 1) args[0].split(',') else args).map { it.toIntOrNull() }
    if (parts.size != 5 || parts.any { it == null }) {
        return null
    }
    val (level, mx, mz, lx, lz) = parts.map { it!! }
    return CoordGrid(level, mx, mz, lx, lz)
}

/** `::tele n 2` and friends: a direction and an optional tile count (default 1). */
private val TELE_DIRECTIONS: Map<String, Direction> =
    mapOf(
        "n" to Direction.North,
        "north" to Direction.North,
        "s" to Direction.South,
        "south" to Direction.South,
        "e" to Direction.East,
        "east" to Direction.East,
        "w" to Direction.West,
        "west" to Direction.West,
        "ne" to Direction.NorthEast,
        "nw" to Direction.NorthWest,
        "se" to Direction.SouthEast,
        "sw" to Direction.SouthWest,
    )

/**
 * Handles `::tele name`, `::tele n 2` and Jagex's `level,mx,mz,lx,lz`; returns false for upstream's
 * `x y [level]` coordinates.
 */
internal fun ProtectedAccessLauncher.teleNamed(cheat: Cheat): Boolean {
    val first = cheat.args.firstOrNull() ?: return false
    val jagexCoords = jagexCoordGrid(cheat.args)
    if (jagexCoords != null) {
        val launched = launch(cheat.player) { telejump(jagexCoords, TeleportType.Exempt) }
        if (!launched) {
            cheat.player.mes(constants.dm_busy)
        }
        return true
    }
    if (first.toIntOrNull() != null || ',' in first) {
        return false
    }
    val direction = TELE_DIRECTIONS[first]
    val tiles = if (cheat.args.size == 1) 1 else cheat.args[1].toIntOrNull()
    if (direction != null && tiles != null && cheat.args.size <= 2) {
        val dest = cheat.player.coords.translate(direction.xOff * tiles, direction.zOff * tiles)
        val launched = launch(cheat.player) { telejump(dest, TeleportType.Exempt) }
        if (!launched) {
            cheat.player.mes(constants.dm_busy)
        }
        return true
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

/**
 * Finds an obj by display name for `::item` (e.g. `torva platelegs` -> `obj.torva_legs`),
 * skipping noted and placeholder variants. Returns the lowest id when several share a name.
 */
internal fun findItemByName(input: String): ItemServerType? {
    val name = input.replace('_', ' ')
    return ServerCacheManager.getItems()
        .values
        .filter { it.name.equals(name, ignoreCase = true) }
        .filterNot {
            val symbol = RSCM.getReverseMapping(RSCMType.OBJ, it.id)
            symbol.startsWith("obj.cert_") || symbol.startsWith("obj.placeholder_")
        }
        .minByOrNull { it.id }
}
