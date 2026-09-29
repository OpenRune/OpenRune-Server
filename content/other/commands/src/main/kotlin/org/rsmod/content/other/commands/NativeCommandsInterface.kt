package org.rsmod.content.other.commands

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.cheat.CheatHandler
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val INTERFACE = "interface.commands_menu"
private const val COMP_TITLE = "component.commands_menu:title"
private const val COMP_PLAYER = "component.commands_menu:player"
private const val COMP_SEARCH = "component.commands_menu:search"
private const val COMP_PAGE = "component.commands_menu:page"
private const val COMP_CLOSE = "component.commands_menu:close"
private const val COMP_PREV = "component.commands_menu:previous"
private const val COMP_NEXT = "component.commands_menu:next"
private const val COMP_DETAIL_NAME = "component.commands_menu:detail_name"
private const val COMP_DETAIL_DESC = "component.commands_menu:detail_desc"
private const val COMP_DETAIL_RIGHTS = "component.commands_menu:detail_rights"
private const val COMP_USE = "component.commands_menu:use"

private val COMMAND_ROWS =
    arrayOf(
        "component.commands_menu:row_0",
        "component.commands_menu:row_1",
        "component.commands_menu:row_2",
        "component.commands_menu:row_3",
        "component.commands_menu:row_4",
        "component.commands_menu:row_5",
        "component.commands_menu:row_6",
        "component.commands_menu:row_7",
    )

private enum class CommandCategory(val label: String, val component: String) {
    ALL("All commands", "component.commands_menu:category_all"),
    TRAVEL("Travel", "component.commands_menu:category_travel"),
    ACCOUNT("Account", "component.commands_menu:category_account"),
    SOCIAL("Social", "component.commands_menu:category_social"),
    UTILITY("Utility", "component.commands_menu:category_utility"),
    STAFF("Staff", "component.commands_menu:category_staff"),
    DEVELOPMENT("Development", "component.commands_menu:category_development"),
}

private data class CommandEntry(
    val name: String,
    val description: String,
    val requiredRights: Rights?,
    val category: CommandCategory,
)

class NativeCommandsInterface
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val cheatCommandMap: CheatCommandMap,
) : PluginScript() {
    private val pages = mutableMapOf<String, Int>()
    private val categories = mutableMapOf<String, CommandCategory>()
    private val selected = mutableMapOf<String, String>()
    private val searches = mutableMapOf<String, String>()

    private fun classify(name: String, description: String, handler: CheatHandler): CommandCategory {
        val n = name.lowercase()
        val d = description.lowercase()

        if (
            n in setOf(
                "tele", "telezone", "up", "down", "forward", "backwards", "back", "left", "right",
                "mypos", "home", "edge", "varrock", "lumbridge", "falador", "yanille", "wild",
            ) || "teleport" in d
        ) return CommandCategory.TRAVEL

        if (
            n in setOf("gamemode", "master", "reset", "password", "pass", "changepassword", "kdr")
        ) return CommandCategory.ACCOUNT

        if (
            n in setOf("yell", "players", "online", "broadcast") ||
                "message" in d || "player list" in d
        ) return CommandCategory.SOCIAL

        if (
            n in setOf(
                "anim", "animation", "spot", "gfx", "graphic", "synth", "varp", "varbit",
                "getvarp", "getvarbit", "object", "objectdel", "locadd", "locdel", "npc",
                "npcadd", "invadd", "item", "componentdebug", "transmog", "testloot", "die",
                "poison", "venom", "disease", "maxhit",
            ) || "debug" in d || "spawn" in d
        ) return CommandCategory.DEVELOPMENT

        if (
            n in setOf(
                "openbank", "interface", "ifopen", "plugins", "pluginenable", "plugindisable",
                "pluginreload", "loadplugin",
            )
        ) return CommandCategory.UTILITY

        return if (handler.requiredRights != null) CommandCategory.STAFF else CommandCategory.UTILITY
    }

    private fun visibleCommands(player: Player): List<CommandEntry> =
        cheatCommandMap.commands.entries
            .asSequence()
            .filter { it.key != "commands" }
            .filter { (_, handler) ->
                val required = handler.requiredRights
                required == null || player.modLevel.isAtLeast(required)
            }
            .map { (name, handler) ->
                val description = handler.desc ?: ""
                CommandEntry(
                    name = name,
                    description = description,
                    requiredRights = handler.requiredRights,
                    category = classify(name, description, handler),
                )
            }
            .sortedBy { it.name }
            .toList()

    private fun filteredCommands(player: Player): List<CommandEntry> {
        val category = categories[player.username] ?: CommandCategory.ALL
        val query = searches[player.username].orEmpty().trim().lowercase()
        return visibleCommands(player).filter { entry ->
            val categoryMatch = category == CommandCategory.ALL || entry.category == category
            val queryMatch =
                query.isBlank() ||
                    query in entry.name.lowercase() ||
                    query in entry.description.lowercase()
            categoryMatch && queryMatch
        }
    }

    private fun selectedEntry(player: Player, entries: List<CommandEntry>): CommandEntry? {
        val selectedName = selected[player.username]
        val entry = entries.firstOrNull { it.name == selectedName } ?: entries.firstOrNull()
        if (entry != null) selected[player.username] = entry.name else selected.remove(player.username)
        return entry
    }

    private fun ProtectedAccess.renderCommands() {
        val allVisible = visibleCommands(player)
        val entries = filteredCommands(player)
        val pageSize = COMMAND_ROWS.size
        val pageCount = maxOf(1, (entries.size + pageSize - 1) / pageSize)
        val requestedPage = pages[player.username] ?: 0
        val page = requestedPage.coerceIn(0, pageCount - 1)
        pages[player.username] = page
        val start = page * pageSize
        val selectedEntry = selectedEntry(player, entries)

        ifOpenMainModal(INTERFACE)
        ifSetText(COMP_TITLE, "Commands")
        ifSetText(COMP_PLAYER, player.modLevel.name.lowercase().replaceFirstChar { it.uppercase() })

        val search = searches[player.username].orEmpty()
        ifSetText(
            COMP_SEARCH,
            if (search.isBlank()) "Search: all  (::commands <text>)" else "Search: $search",
        )

        val activeCategory = categories[player.username] ?: CommandCategory.ALL
        for (category in CommandCategory.entries) {
            val count =
                if (category == CommandCategory.ALL) {
                    allVisible.size
                } else {
                    allVisible.count { it.category == category }
                }
            val color = if (category == activeCategory) "ff981f" else "ffffff"
            ifSetText(category.component, "<col=$color>${category.label}  $count</col>")
        }

        COMMAND_ROWS.forEachIndexed { rowIndex, component ->
            val entry = entries.getOrNull(start + rowIndex)
            val text =
                if (entry == null) {
                    ""
                } else {
                    val selectedColor = if (entry.name == selectedEntry?.name) "ff981f" else "ffffff"
                    val desc = entry.description.take(44)
                    "<col=$selectedColor>::${entry.name}</col>   <col=d0c4a8>$desc</col>"
                }
            ifSetText(component, text)
        }

        ifSetText(COMP_PAGE, "${entries.size} results / page ${page + 1} of $pageCount")

        if (selectedEntry == null) {
            ifSetText(COMP_DETAIL_NAME, "")
            ifSetText(COMP_DETAIL_DESC, "No command selected.")
            ifSetText(COMP_DETAIL_RIGHTS, "")
        } else {
            ifSetText(COMP_DETAIL_NAME, "::${selectedEntry.name}")
            ifSetText(COMP_DETAIL_DESC, selectedEntry.description.ifBlank { "No description." })
            ifSetText(
                COMP_DETAIL_RIGHTS,
                selectedEntry.requiredRights?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                    ?: "Player",
            )
        }
    }

    private fun resetView(player: Player) {
        pages[player.username] = 0
        categories[player.username] = CommandCategory.ALL
        selected.remove(player.username)
    }

    override fun ScriptContext.startup() {
        onCommand("commands") {
            desc = "Open native commands interface"
            cheat {
                resetView(player)
                searches[player.username] = args.joinToString(" ").trim()
                protectedAccess.launch(player) { renderCommands() }
            }
        }

        onIfClose(INTERFACE) {
            pages.remove(player.username)
            categories.remove(player.username)
            selected.remove(player.username)
            searches.remove(player.username)
        }

        onIfModalButton(COMP_CLOSE) {
            pages.remove(player.username)
            categories.remove(player.username)
            selected.remove(player.username)
            searches.remove(player.username)
            ifClose()
        }

        for (category in CommandCategory.entries) {
            onIfModalButton(category.component) {
                categories[player.username] = category
                pages[player.username] = 0
                selected.remove(player.username)
                renderCommands()
            }
        }

        COMMAND_ROWS.forEachIndexed { rowIndex, component ->
            onIfModalButton(component) {
                val entries = filteredCommands(player)
                val page = pages[player.username] ?: 0
                val entry = entries.getOrNull(page * COMMAND_ROWS.size + rowIndex)
                if (entry != null) selected[player.username] = entry.name
                renderCommands()
            }
        }

        onIfModalButton(COMP_PREV) {
            val current = pages[player.username] ?: 0
            pages[player.username] = (current - 1).coerceAtLeast(0)
            selected.remove(player.username)
            renderCommands()
        }

        onIfModalButton(COMP_NEXT) {
            val entries = filteredCommands(player)
            val pageCount = maxOf(1, (entries.size + COMMAND_ROWS.size - 1) / COMMAND_ROWS.size)
            val current = pages[player.username] ?: 0
            pages[player.username] = (current + 1).coerceAtMost(pageCount - 1)
            selected.remove(player.username)
            renderCommands()
        }

        onIfModalButton(COMP_USE) {
            val entries = filteredCommands(player)
            val entry = selectedEntry(player, entries)
            if (entry != null) cheatCommandMap.execute(player, entry.name, emptyList())
        }
    }
}
