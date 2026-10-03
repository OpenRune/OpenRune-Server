package org.rsmod.content.other.pets

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class PetMenu @Inject constructor(private val rewards: PetRewards) : PluginScript() {
    private data class View(var query: String = "", var page: Int = 0)
    private val views = mutableMapOf<Player, View>()

    override fun ScriptContext.startup() {
        onIfClose(INTERFACE) { views.remove(player) }
        onPlayerLogout { views.remove(player) }
        onIfModalButton(comp("search")) {
            if (!allowed()) return@onIfModalButton
            val view = views[player] ?: return@onIfModalButton
            view.query = stringDialog("Find a pet by name or category:").trim().take(60)
            view.page = 0
            views[player] = view
            render(view)
        }
        onIfModalButton(comp("clear")) { if (allowed()) views[player]?.let { it.query = ""; it.page = 0; render(it) } }
        onIfModalButton(comp("previous")) { if (allowed()) views[player]?.let { it.page--; render(it) } }
        onIfModalButton(comp("next")) { if (allowed()) views[player]?.let { it.page++; render(it) } }
        repeat(PAGE_SIZE) { index ->
            for (part in listOf("pet", "icon")) onIfModalButton(comp("${part}_$index")) {
                if (!allowed()) return@onIfModalButton
                val view = views[player] ?: return@onIfModalButton
                val pet = matches(view.query).getOrNull(view.page * PAGE_SIZE + index) ?: return@onIfModalButton
                ifClose()
                rewards.give(player, pet.base.obj)
            }
        }
    }

    fun open(access: ProtectedAccess) = with(access) {
        if (!allowed()) return@with
        ifClose()
        ifOpenMainModal(INTERFACE)
        val view = View()
        views[player] = view
        render(view)
    }

    private fun ProtectedAccess.allowed(): Boolean {
        if (player.modLevel.isAtLeast(Rights.ADMINISTRATOR)) return true
        ifClose()
        return false
    }

    private fun ProtectedAccess.render(view: View) {
        val pets = matches(view.query)
        val last = (pets.size - 1).coerceAtLeast(0) / PAGE_SIZE
        view.page = view.page.coerceIn(0, last)
        ifSetText(comp("query"), "Search: ${safe(view.query).ifEmpty { "all pets" }}")
        ifSetText(comp("page"), "Page ${view.page + 1}/${last + 1}")
        ifSetText(comp("status"), if (pets.isEmpty()) "No pets found" else "${pets.size} pets - select a pet to obtain it")
        repeat(PAGE_SIZE) { index ->
            val pet = pets.getOrNull(view.page * PAGE_SIZE + index)
            ifSetHide(comp("icon_$index"), pet == null)
            if (pet != null) ifSetObj(comp("icon_$index"), pet.base.obj, 1)
            ifSetText(comp("pet_$index"), pet?.name?.let(::safe).orEmpty())
            ifSetText(comp("category_$index"), pet?.category?.name?.let(::safe).orEmpty())
        }
    }

    companion object {
        private const val INTERFACE = "interface.pet_menu"
        private const val PAGE_SIZE = 18
        private fun comp(name: String) = "component.pet_menu:$name"
        private fun safe(text: String) = text.replace(Regex("[<>|]"), "")
        internal fun matches(query: String): List<Pet> = Pets.all.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.name.contains(query, ignoreCase = true)
        }.sortedBy { it.name }
    }
}
