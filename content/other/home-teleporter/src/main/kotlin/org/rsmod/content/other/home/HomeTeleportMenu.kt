package org.rsmod.content.other.home

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
internal class HomeTeleportMenu @Inject constructor(private val travel: HomeTravel) : PluginScript() {
    private data class View(var category: TeleportCategory? = TeleportCategory.Cities, var query: String = "", var page: Int = 0, var selected: String? = null)
    private val views = mutableMapOf<Player, View>()
    override fun ScriptContext.startup() {
        onIfClose(INTERFACE) { views.remove(player) }
        onPlayerLogout { views.remove(player) }
        for (category in TeleportCategory.entries) onIfModalButton(comp("tab_${category.key}")) {
            views[player]?.let { it.category = category; it.query = ""; it.page = 0; it.selected = null; render(it) }
        }
        onIfModalButton(comp("search")) {
            val view = views[player] ?: return@onIfModalButton
            val query = stringDialog("Search all teleport destinations:").trim().take(40).replace(Regex("[<>|]"), "")
            ifClose()
            view.query = query; view.category = null; view.page = 0; view.selected = null
            ifOpenMainModal(INTERFACE)
            views[player] = view
            render(view)
        }
        onIfModalButton(comp("clear")) {
            views[player]?.let { it.query = ""; it.page = 0; it.selected = null; render(it) }
        }
        onIfModalButton(comp("previous")) { views[player]?.let { it.page--; it.selected = null; render(it) } }
        onIfModalButton(comp("next")) { views[player]?.let { it.page++; it.selected = null; render(it) } }
        repeat(PAGE_SIZE) { index ->
            for (part in listOf("row", "icon", "name", "status")) onIfModalButton(comp("${part}_$index")) {
                val view = views[player] ?: return@onIfModalButton
                val row = choices(view).getOrNull(view.page * PAGE_SIZE + index) ?: return@onIfModalButton
                view.selected = row.key
                render(view)
            }
        }
        onIfModalButton(comp("teleport")) {
            val view = views[player] ?: return@onIfModalButton
            val row = choices(view).singleOrNull { it.key == view.selected } ?: return@onIfModalButton
            travel.travel(this, row)
        }
    }

    fun open(access: ProtectedAccess) = with(access) {
        ifClose()
        ifOpenMainModal(INTERFACE)
        val view = View()
        views[player] = view
        render(view)
    }

    private fun choices(view: View) = TeleportCatalogue.matches(view.category, view.query)
    private fun ProtectedAccess.render(view: View) {
        val choices = choices(view)
        val pages = maxOf(1, (choices.size + PAGE_SIZE - 1) / PAGE_SIZE)
        view.page = view.page.coerceIn(0, pages - 1)
        ifSetText(comp("query"), if (view.query.isBlank()) "${view.category?.label ?: "All destinations"} - ${choices.size}" else "Search: ${view.query} (${choices.size})")
        ifSetText(comp("page"), "${view.page + 1}/$pages")
        for (category in TeleportCategory.entries) ifSetText(comp("tab_${category.key}"), "<col=${if (category == view.category) "ffff00" else "ff981f"}>${category.label}</col>")
        repeat(PAGE_SIZE) { index ->
            val row = choices.getOrNull(view.page * PAGE_SIZE + index)
            for (part in listOf("row", "icon", "name", "status")) ifSetHide(comp("${part}_$index"), row == null)
            if (row != null) {
                ifSetObj(comp("icon_$index"), row.category.icon, 1)
                ifSetText(comp("name_$index"), "<col=${if (row.key == view.selected) "ffff00" else if (row.available) "ff981f" else "999999"}>${row.name}</col>")
                ifSetText(comp("status_$index"), if (!row.available) "<col=999999>Coming soon</col>" else if (row.dangerous) "<col=ff5555>Wilderness</col>" else "<col=80ff80>Available</col>")
            }
        }
        val selected = choices.singleOrNull { it.key == view.selected }
        ifSetText(comp("detail"), selected?.description ?: "Select a destination. Search covers every category.")
        ifSetHide(comp("teleport"), selected?.available != true)
    }

    internal companion object {
        const val INTERFACE = "interface.home_teleports"
        const val PAGE_SIZE = 6
        fun comp(name: String) = "component.home_teleports:$name"
    }
}
