package org.rsmod.content.interfaces.monsterinfo

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import java.util.Locale
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.droptable.DropPreviewEntry
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.player.events.NpcExamineEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MonsterInfoScript @Inject constructor(
    private val access: ProtectedAccessLauncher,
    private val tables: DropTableRegistry,
    private val areas: AreaChecker,
    private val catalogue: MonsterCatalogue,
) : PluginScript() {
    private enum class Search { NPC, ITEM, SOURCES }
    private enum class Tab { DROPS, LOCATIONS, STATS }
    private data class View(
        var query: String = "", var search: Search = Search.NPC,
        var sourceItem: Int = -1, var selected: MonsterEntry? = null,
        var examined: NpcServerType? = null, var tab: Tab = Tab.DROPS,
        var listPage: Int = 0, var page: Int = 0,
    )
    private val views = mutableMapOf<Player, View>()

    override fun ScriptContext.startup() {
        onEvent<NpcExamineEvent> {
            if (isMonster(type)) access.launch(player) {
                when (choice3("View monster drop table", 1, "View monster stats", 2, "Nevermind", 3)) {
                    1 -> open(type)
                    2 -> open(type, stats = true)
                    else -> ifClose()
                }
            }
        }
        onCommand("drops") {
            desc = "Browse monster drops and locations; search NPCs or items"
            cheat { val query = args.joinToString(" "); access.launch(player) { open(query = query) } }
        }
        onIfClose(INTERFACE) { views.remove(player) }
        onPlayerLogout { views.remove(player) }
        onIfModalButton(comp("npc")) { views[player]?.let { it.search = Search.NPC; it.listPage = 0; render(it) } }
        onIfModalButton(comp("item")) { views[player]?.let { it.search = Search.ITEM; it.listPage = 0; render(it) } }
        onIfModalButton(comp("search")) {
            val view = views[player] ?: return@onIfModalButton
            val query = stringDialog("Search by ${if (view.search == Search.ITEM) "item" else "monster"} name or ID:")
            view.query = query.take(60).trim()
            if (view.search == Search.SOURCES) view.search = Search.NPC
            view.listPage = 0
            views[player] = view
            render(view)
        }
        onIfModalButton(comp("clear")) { views[player]?.let { it.query = ""; it.listPage = 0; if (it.search == Search.SOURCES) it.search = Search.ITEM; render(it) } }
        onIfModalButton(comp("list_previous")) { views[player]?.let { it.listPage--; render(it) } }
        onIfModalButton(comp("list_next")) { views[player]?.let { it.listPage++; render(it) } }
        onIfModalButton(comp("previous")) { views[player]?.let { it.page--; render(it) } }
        onIfModalButton(comp("next")) { views[player]?.let { it.page++; render(it) } }
        for (tab in Tab.entries) for (name in listOf(tab.name.lowercase(), "tab_${tab.name.lowercase()}")) onIfModalButton(comp(name)) {
            views[player]?.let { it.tab = tab; it.page = 0; render(it) }
        }
        repeat(LIST_SIZE) { index ->
            for (part in listOf("result", "result_icon")) onIfModalButton(comp("${part}_$index")) {
                views[player]?.let { select(it, index) }
            }
        }
        repeat(PAGE_SIZE) { index ->
            onIfModalButton(comp("icon_$index")) {
                val view = views[player] ?: return@onIfModalButton
                val drop = view.selected?.drops?.getOrNull(view.page * PAGE_SIZE + index)?.item ?: return@onIfModalButton
                if (view.tab != Tab.DROPS) return@onIfModalButton
                view.sourceItem = drop.obj.asRSCM(); view.search = Search.SOURCES; view.listPage = 0
                render(view)
            }
        }
    }

    private fun ProtectedAccess.open(type: NpcServerType? = null, stats: Boolean = false, query: String = "") {
        val selected = type?.let { catalogue.find(it, tables.forNpcType(it.internalName, player.coords, areas)) }
        val view = View(query = query.ifEmpty { type?.name.orEmpty() }, selected = selected, examined = type, tab = if (stats) Tab.STATS else Tab.DROPS)
        ifClose()
        ifOpenMainModal(INTERFACE)
        views[player] = view
        render(view)
    }

    private fun matches(view: View): List<MonsterEntry> =
        if (view.search == Search.SOURCES) catalogue.sources(view.sourceItem) else catalogue.searchMonsters(view.query)

    private fun ProtectedAccess.select(view: View, index: Int) {
        val slot = view.listPage * LIST_SIZE + index
        if (view.search == Search.ITEM) {
            view.sourceItem = catalogue.searchItems(view.query).getOrNull(slot)?.id ?: return
            view.search = Search.SOURCES; view.listPage = 0
        } else {
            view.selected = matches(view).getOrNull(slot) ?: return
            view.examined = view.selected?.type; view.page = 0
        }
        render(view)
    }

    private fun ProtectedAccess.render(view: View) {
        ifSetText(comp("query"), "${if (view.search == Search.ITEM) "Item" else "NPC"}: ${MonsterStats.safeText(view.query).take(42).ifEmpty { "all" }}")
        val items = if (view.search == Search.ITEM) catalogue.searchItems(view.query) else emptyList()
        val monsters = if (view.search != Search.ITEM) matches(view) else emptyList()
        val count = if (view.search == Search.ITEM) items.size else monsters.size
        view.listPage = view.listPage.coerceIn(0, lastPage(count, LIST_SIZE))
        ifSetText(comp("list_page"), "${view.listPage + 1}/${lastPage(count, LIST_SIZE) + 1}")
        repeat(LIST_SIZE) { index ->
            val slot = view.listPage * LIST_SIZE + index
            val item = items.getOrNull(slot)
            val monster = monsters.getOrNull(slot)
            val label = item?.name ?: monster?.label.orEmpty()
            ifSetText(comp("result_$index"), (if (monster != null && monster == view.selected) "<col=ffff00>" else "<col=ff981f>") + MonsterStats.safeText(label) + "</col>")
            ifSetHide(comp("result_icon_$index"), item == null)
            if (item != null) ifSetObj(comp("result_icon_$index"), item.internalName, 1)
        }
        ifSetText(comp("list_status"), if (count == 0) "No results" else if (view.search == Search.SOURCES) "$count NPC tables" else "$count results")
        val type = view.examined ?: view.selected?.type
        ifSetText(comp("name"), MonsterStats.safeText(type?.name ?: "Select a monster"))
        ifSetText(comp("notice"), if (view.search == Search.SOURCES) "Sources: ${MonsterStats.safeText(ServerCacheManager.getItem(view.sourceItem)?.name.orEmpty())}" else "Base odds per roll; conditions and boosts may differ.")
        for (tab in Tab.entries) ifSetText(comp("tab_${tab.name.lowercase()}"), "<col=${if (tab == view.tab) "ffff00" else "ff981f"}>${tab.name.lowercase().replaceFirstChar { it.uppercase() }}</col>")
        val entries = view.selected?.drops.orEmpty()
        val locations = view.selected?.let(catalogue::locations).orEmpty()
        val max = when (view.tab) { Tab.DROPS -> lastPage(entries.size, PAGE_SIZE); Tab.LOCATIONS -> lastPage(locations.size, 6); Tab.STATS -> 3 }
        view.page = view.page.coerceIn(0, max)
        ifSetText(comp("page"), "${view.page + 1}/${max + 1}")
        repeat(PAGE_SIZE) { index ->
            val entry = entries.getOrNull(view.page * PAGE_SIZE + index).takeIf { view.tab == Tab.DROPS }
            val item = entry?.item
            ifSetHide(comp("icon_$index"), item == null)
            if (item != null) ifSetObj(comp("icon_$index"), item.obj, 1)
            ifSetText(comp("row_$index"), entry?.let(::dropName).orEmpty())
            ifSetText(comp("quantity_$index"), item?.let { "Min: ${it.countChoices?.minOrNull() ?: it.count.first}   Max: ${it.countChoices?.maxOrNull() ?: it.count.last}" }.orEmpty())
            ifSetText(comp("chance_$index"), entry?.let { "${odds(it)}<br>${it.stage}${if (it.rolls > 1) " x${it.rolls}" else ""}" }.orEmpty())
        }
        val details = when {
            type == null -> "Search an NPC to view its drops.<br><br>Search an item to find its sources."
            view.tab == Tab.STATS -> MonsterStats.payload(type).split('|')[view.page]
            view.tab == Tab.LOCATIONS -> locations.drop(view.page * 6).take(6).joinToString("<br><br>").ifEmpty { "No registered locations." }
            entries.isEmpty() -> "No registered drop table.<br>Rewards may be encounter-based."
            else -> ""
        }
        ifSetText(comp("details"), details)
    }

    private fun dropName(entry: DropPreviewEntry): String {
        val item = entry.item?.let { ServerCacheManager.getItem(it.obj.asRSCM()) }
        val name = if (item?.isCert == true) ServerCacheManager.getItem(item.certlink)?.name?.plus(" (noted)") else item?.name
        return MonsterStats.safeText(name?.takeIf { it.isNotBlank() } ?: "Special / conditional drop")
    }
    private fun odds(entry: DropPreviewEntry): String = when {
        entry.item == null -> "Varies"
        entry.baseChance >= 1 -> "Always (100%)"
        entry.baseChance <= 0 -> "Disabled"
        else -> "1 / " + String.format(Locale.ROOT, "%.2f", 1 / entry.baseChance).removeSuffix(".00")
    }

    companion object {
        private const val INTERFACE = "interface.monster_drops"
        private const val LIST_SIZE = 8
        private const val PAGE_SIZE = 5
        private fun comp(name: String) = "component.monster_drops:$name"
        private fun lastPage(size: Int, pageSize: Int) = (size - 1).coerceAtLeast(0) / pageSize
        internal fun isMonster(type: NpcServerType): Boolean = type.combatLevel >= 0 && (0..4).any { type.actions.getOpOrNull(it).equals("Attack", ignoreCase = true) }
    }
}
