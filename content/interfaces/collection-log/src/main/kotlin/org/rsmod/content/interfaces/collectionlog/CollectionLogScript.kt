package org.rsmod.content.interfaces.collectionlog

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CollectionLogScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    private var Player.lastTab: Int by intVarBit("varbit.collection_last_tab")
    private var Player.lastCategory: Int by intVarBit("varbit.collection_last_category")

    /** The collection log bases the displayed items from an inventory rather than varbits */
    private val Player.collectionTransmit: Inventory
        get() = invMap.getOrPut("inv.collection_transmit")

    override fun ScriptContext.startup() {
        onIfOpen("interface.collection") {
            player.startInvTransmit(player.collectionTransmit)
            player.syncCollectionTransmitFromVarbits()
            player.registerCategoryRowEvents()
            player.registerBurgerMenuEvents("component.collection:burger_menu_frame")
            player.drawCollectionLog(
                player.lastTab.coerceIn(BOSS_TAB, OTHER_TAB),
                player.lastCategory.coerceAtLeast(0),
            )
        }
        onIfClose("interface.collection") { player.stopInvTransmit(player.collectionTransmit) }

        onIfOpen("interface.collection_overview") {
            player.registerOverviewSubsectionEvents()
            player.registerBurgerMenuEvents("component.collection_overview:burger_menu_frame")
            player.drawCollectionOverview()
        }

        onIfOverlayButton("component.collection:boss_tab") { player.switchTab(BOSS_TAB) }
        onIfOverlayButton("component.collection:raid_tab") { player.switchTab(RAID_TAB) }
        onIfOverlayButton("component.collection:clue_tab") { player.switchTab(CLUE_TAB) }
        onIfOverlayButton("component.collection:minigame_tab") { player.switchTab(MINIGAME_TAB) }
        onIfOverlayButton("component.collection:other_tab") { player.switchTab(OTHER_TAB) }
        onIfOverlayButton("component.collection:close") { player.closeCollectionLog() }
        onIfOverlayButton("component.collection_overview:close") {
            player.closeCollectionOverview()
        }

        onIfOverlayButton("component.collection:boss_background") {
            player.selectCategory(BOSS_TAB, it.comsub)
        }
        onIfOverlayButton("component.collection:raid_background") {
            player.selectCategory(RAID_TAB, it.comsub)
        }
        onIfOverlayButton("component.collection:clue_background") {
            player.selectCategory(CLUE_TAB, it.comsub)
        }
        onIfOverlayButton("component.collection:minigame_background") {
            player.selectCategory(MINIGAME_TAB, it.comsub)
        }
        onIfOverlayButton("component.collection:other_background") {
            player.selectCategory(OTHER_TAB, it.comsub)
        }

        onIfOverlayButton("component.collection_overview:subsection_buttons_click") {
            player.openCategoryFromOverview(it.comsub)
        }

        onIfOverlayButton("component.collection:burger_menu_frame") {
            player.handleLogBurgerMenuClick(it.comsub)
        }
        onIfOverlayButton("component.collection_overview:burger_menu_frame") {
            player.handleOverviewBurgerMenuClick(it.comsub)
        }
    }

    private fun Player.registerCategoryRowEvents() {
        val range = 0..MAX_CATEGORY_ROWS
        ifSetEvents("component.collection:boss_background", range, IfEvent.Op1)
        ifSetEvents("component.collection:raid_background", range, IfEvent.Op1)
        ifSetEvents("component.collection:clue_background", range, IfEvent.Op1)
        ifSetEvents("component.collection:minigame_background", range, IfEvent.Op1)
        ifSetEvents("component.collection:other_background", range, IfEvent.Op1)
    }

    private fun Player.selectCategory(tab: Int, category: Int) {
        lastTab = tab
        lastCategory = category
        drawCollectionLog(tab, category)
    }

    private fun Player.registerOverviewSubsectionEvents() {
        ifSetEvents(
            "component.collection_overview:subsection_buttons_click",
            BOSS_TAB..OTHER_TAB,
            IfEvent.Op1,
        )
    }

    private fun Player.openCategoryFromOverview(tab: Int) {
        lastTab = tab
        lastCategory = 0
        openLogFromOverview()
    }

    private fun Player.registerBurgerMenuEvents(component: String) {
        ifSetEvents(component, BURGER_VIEW_LOG_ROW..BURGER_VIEW_OVERVIEW_ROW, IfEvent.Op1)
    }

    private fun Player.handleLogBurgerMenuClick(comsub: Int) {
        if (comsub == BURGER_VIEW_OVERVIEW_ROW) {
            openOverviewFromLog()
        }
    }

    private fun Player.handleOverviewBurgerMenuClick(comsub: Int) {
        if (comsub == BURGER_VIEW_LOG_ROW) {
            openLogFromOverview()
        }
    }

    private fun Player.openLogFromOverview() {
        closeCollectionOverview()
        ifOpenOverlay("interface.collection", eventBus)
    }

    private fun Player.openOverviewFromLog() {
        closeCollectionLog()
        ifOpenOverlay("interface.collection_overview", eventBus)
    }

    private fun Player.closeCollectionLog() {
        ifCloseSub("interface.collection", eventBus)
    }

    private fun Player.closeCollectionOverview() {
        ifCloseSub("interface.collection_overview", eventBus)
    }

    private fun Player.switchTab(tab: Int) {
        lastTab = tab
        lastCategory = 0
        drawCollectionLog(tab, 0)
    }

    /** Adds all items from the player's collection log to the collection transmit inventory */
    private fun Player.syncCollectionTransmitFromVarbits() {
        val inv = collectionTransmit
        for ((objId, varbit) in CollectionLogItems.all()) {
            val objType = ServerCacheManager.getItem(objId) ?: continue
            val received = vars[varbit]
            if (received > 0 && !inv.contains(objType)) {
                invAdd(inv, objId, received)
            }
        }
    }

    private fun Player.drawCollectionLog(tab: Int, category: Int) {
        applyCollectionCount()
        applyCategoryCounts(tab, category)
        val components = TAB_COMPONENTS.getValue(tab)
        runClientScript(
            "clientscript.[clientscript,collection_draw]".asRSCM(RSCMType.CLIENTSCRIPT),
            tab,
            components.container,
            components.background,
            components.text,
            components.scrollbar,
            TAB_STRUCT_BASE + tab,
            category,
        )
    }

    private fun Player.drawCollectionOverview() {
        applyCollectionCount()
        applyTabCounts()
        runClientScript(
            "clientscript.[clientscript,collection_overview_draw]".asRSCM(RSCMType.CLIENTSCRIPT),
            "component.collection_overview:subsection_general_content".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:subsection_buttons".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:subsection_buttons_click".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:subsection_progress".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:progress_bar".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:progress_bar_rank_left".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:progress_bar_rank_right".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:progress_left_text".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:progress_right_text".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:latest_items_data".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:burger_btn_menu".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:burger_menu_frame".asRSCM(RSCMType.COMPONENT),
            "component.collection_overview:burger_menu_overlay".asRSCM(RSCMType.COMPONENT),
        )
    }

    private fun Player.applyTabCounts() {
        for (tab in TAB_COUNT_VARPS.indices) {
            val (countVarp, maxVarp) = TAB_COUNT_VARPS[tab]
            VarPlayerIntMapSetter.set(
                this,
                countVarp,
                CollectionLogCategories.tabObtainedCount(this, tab),
            )
            VarPlayerIntMapSetter.set(this, maxVarp, CollectionLogCategories.tabTotalCount(tab))
        }
    }

    private fun Player.applyCategoryCounts(tab: Int, comsub: Int) {
        val countVarps = CollectionLogCategories.forCategory(tab, comsub)?.countVarps.orEmpty()
        for (i in COUNT_TARGET_VARPS.indices) {
            val value = countVarps.getOrNull(i)?.let { vars[it] } ?: 0
            VarPlayerIntMapSetter.set(this, COUNT_TARGET_VARPS[i], value)
        }
        // Placeholder until we have system that saves PBs
        for (varp in PB_TARGET_VARPS) {
            VarPlayerIntMapSetter.set(this, varp, 0)
        }
    }

    private class TabComponents(
        container: String,
        background: String,
        text: String,
        scrollbar: String,
    ) {
        val container: Int = container.asRSCM(RSCMType.COMPONENT)
        val background: Int = background.asRSCM(RSCMType.COMPONENT)
        val text: Int = text.asRSCM(RSCMType.COMPONENT)
        val scrollbar: Int = scrollbar.asRSCM(RSCMType.COMPONENT)
    }

    private companion object {
        const val BOSS_TAB = 0
        const val RAID_TAB = 1
        const val CLUE_TAB = 2
        const val MINIGAME_TAB = 3
        const val OTHER_TAB = 4
        const val TAB_STRUCT_BASE = 471
        const val MAX_CATEGORY_ROWS = 300

        const val BURGER_VIEW_LOG_ROW = 10

        const val BURGER_VIEW_OVERVIEW_ROW = 12

        val COUNT_TARGET_VARPS =
            listOf(
                "varp.collection_category_count",
                "varp.collection_category_count2",
                "varp.collection_category_count3",
            )
        val PB_TARGET_VARPS =
            listOf(
                "varp.collection_personal_best_transmit",
                "varp.collection_personal_best_transmit_2",
            )

        val TAB_COUNT_VARPS =
            listOf(
                "varp.collection_count_bosses" to "varp.collection_count_bosses_max",
                "varp.collection_count_raids" to "varp.collection_count_raids_max",
                "varp.collection_count_clues" to "varp.collection_count_clues_max",
                "varp.collection_count_minigames" to "varp.collection_count_minigames_max",
                "varp.collection_count_other" to "varp.collection_count_other_max",
            )

        val TAB_COMPONENTS =
            mapOf(
                BOSS_TAB to
                    TabComponents(
                        "component.collection:boss_container",
                        "component.collection:boss_background",
                        "component.collection:boss_text",
                        "component.collection:boss_scrollbar",
                    ),
                RAID_TAB to
                    TabComponents(
                        "component.collection:raid_container",
                        "component.collection:raid_background",
                        "component.collection:raid_text",
                        "component.collection:raid_scrollbar",
                    ),
                CLUE_TAB to
                    TabComponents(
                        "component.collection:clue_container",
                        "component.collection:clue_background",
                        "component.collection:clue_text",
                        "component.collection:clue_scrollbar",
                    ),
                MINIGAME_TAB to
                    TabComponents(
                        "component.collection:minigame_container",
                        "component.collection:minigame_background",
                        "component.collection:minigame_text",
                        "component.collection:minigame_scrollbar",
                    ),
                OTHER_TAB to
                    TabComponents(
                        "component.collection:other_container",
                        "component.collection:other_background",
                        "component.collection:other_text",
                        "component.collection:other_scrollbar",
                    ),
            )
    }
}
