package org.rsmod.content.interfaces.monsterinfo

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import java.util.Locale
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.droptable.DropPreviewEntry
import org.rsmod.api.droptable.DropTablePreview
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.player.events.NpcExamineEvent
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MonsterInfoScript @Inject constructor(
    private val access: ProtectedAccessLauncher,
    private val tables: DropTableRegistry,
    private val areas: AreaChecker,
) : PluginScript() {
    private data class View(val type: NpcServerType, val entries: List<DropPreviewEntry>, var page: Int = 0)
    private val views = mutableMapOf<Player, View>()

    override fun ScriptContext.startup() {
        onEvent<NpcExamineEvent> {
            if (isMonster(type)) access.launch(player) {
                when (choice3("View monster drop table", 1, "View monster stats", 2, "Nevermind", 3)) {
                    1 -> openDrops(type)
                    2 -> openStats(type)
                    else -> ifClose()
                }
            }
        }
        onIfClose(DROPS) { views.remove(player) }
        onPlayerLogout { views.remove(player) }
        onIfModalButton("component.monster_drops:previous") {
            views[player]?.let { it.page = (it.page - 1).coerceAtLeast(0); render(it) }
        }
        onIfModalButton("component.monster_drops:next") {
            views[player]?.let { it.page = (it.page + 1).coerceAtMost((it.entries.size - 1).coerceAtLeast(0) / PAGE_SIZE); render(it) }
        }
        onIfModalButton("component.monster_drops:stats") { views[player]?.type?.let { openStats(it) } }
        for (tab in listOf("stats_button", "aggressive_button", "defensive_button", "other_button")) {
            onIfModalButton("component.dream_monster_stat:$tab") { }
        }
    }

    private fun ProtectedAccess.openStats(type: NpcServerType) {
        ifClose()
        ifOpenSide("interface.dream_monster_stat")
        player.runClientScript("clientscript.[clientscript,dream_monster_populate]".asRSCM(), MonsterStats.payload(type))
    }

    private fun ProtectedAccess.openDrops(type: NpcServerType) {
        val table = tables.forNpcType(type.internalName, player.coords, areas)
        val view = View(type, table?.let(DropTablePreview::entries).orEmpty())
        ifClose()
        ifOpenSide(DROPS)
        views[player] = view
        render(view)
    }

    private fun ProtectedAccess.render(view: View) {
        ifSetText("component.monster_drops:name", MonsterStats.safeText(view.type.name))
        ifSetText("component.monster_drops:page", "${view.page + 1}/${((view.entries.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)}")
        ifSetText("component.monster_drops:notice", "Base odds per roll; boosts and<br>conditions may change drops.")
        repeat(PAGE_SIZE) { index ->
            val row = view.entries.getOrNull(view.page * PAGE_SIZE + index)
            val text = if (row == null) {
                if (index == 0 && view.entries.isEmpty()) "No registered drop table.<br>Rewards may be encounter-based." else ""
            } else {
                val item = row.item
                val name = item?.let { ServerCacheManager.getItem(it.obj.asRSCM())?.name } ?: "Special / conditional drop"
                val count = item?.let { it.countChoices?.joinToString(",") ?: if (it.count.first == it.count.last) "${it.count.first}" else "${it.count.first}-${it.count.last}" }.orEmpty()
                val odds = if (item == null) "Varies" else if (row.baseChance >= 1) "Always" else if (row.baseChance <= 0) "Disabled" else "1/${String.format(Locale.ROOT, "%.2f", 1 / row.baseChance).removeSuffix(".00") }"
                "<col=ff981f>${MonsterStats.safeText(name)}</col><br>" +
                    (if (count.isBlank()) "" else "x$count  ") + "$odds<br>${row.stage}" +
                    if (row.rolls > 1) " (${row.rolls} rolls)" else ""
            }
            ifSetText("component.monster_drops:row_$index", text)
        }
    }

    companion object {
        private const val DROPS = "interface.monster_drops"
        private const val PAGE_SIZE = 3
        internal fun isMonster(type: NpcServerType): Boolean =
            type.combatLevel >= 0 && (0..4).any { type.actions.getOpOrNull(it).equals("Attack", ignoreCase = true) }
    }
}
