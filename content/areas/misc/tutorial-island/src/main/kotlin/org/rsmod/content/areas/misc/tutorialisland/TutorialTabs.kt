package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

internal object TutorialTabs {
    private data class Tab(
        val stone: String,
        val icon: String,
        val interf: String,
        val target: String,
        val events: Array<IfEvent>,
        val unlockStage: Int? = null,
        val alwaysUnlocked: Boolean = false,
    )

    private val TABS =
        listOf(
            Tab(
                stone = "component.toplevel_osrs_stretch:stone0",
                icon = "component.toplevel_osrs_stretch:icon0",
                interf = "interface.combat_interface",
                target = "component.toplevel_osrs_stretch:side0",
                events = arrayOf(IfEvent.Op1),
                unlockStage = TutorialStage.SWORD_EQUIPPED,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone1",
                icon = "component.toplevel_osrs_stretch:icon1",
                interf = "interface.stats",
                target = "component.toplevel_osrs_stretch:side1",
                events = arrayOf(IfEvent.Op1),
                unlockStage = TutorialStage.SHRIMP_CAUGHT,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone2",
                icon = "component.toplevel_osrs_stretch:icon2",
                interf = "interface.side_journal",
                target = "component.toplevel_osrs_stretch:side2",
                events =
                    arrayOf(
                        IfEvent.Op1,
                        IfEvent.Op2,
                        IfEvent.Op3,
                        IfEvent.Op4,
                        IfEvent.Op5,
                        IfEvent.Op6,
                    ),
                unlockStage = TutorialStage.QUEST_GUIDE_TALKED,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone3",
                icon = "component.toplevel_osrs_stretch:icon3",
                interf = "interface.inventory",
                target = "component.toplevel_osrs_stretch:side3",
                events = arrayOf(IfEvent.Op1),
                unlockStage = TutorialStage.NET_GIVEN,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone4",
                icon = "component.toplevel_osrs_stretch:icon4",
                interf = "interface.wornitems",
                target = "component.toplevel_osrs_stretch:side4",
                events = arrayOf(IfEvent.Op1),
                unlockStage = TutorialStage.DAGGER_EQUIP_TASK,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone5",
                icon = "component.toplevel_osrs_stretch:icon5",
                interf = "interface.prayerbook",
                target = "component.toplevel_osrs_stretch:side5",
                events = arrayOf(IfEvent.Op1, IfEvent.Op2),
                unlockStage = TutorialStage.PRAYER_GUIDE_TALKED,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone6",
                icon = "component.toplevel_osrs_stretch:icon6",
                interf = "interface.magic_spellbook",
                target = "component.toplevel_osrs_stretch:side6",
                events = arrayOf(IfEvent.Op1, IfEvent.Op2),
                unlockStage = TutorialStage.MAGIC_GUIDE_TALKED,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone7",
                icon = "component.toplevel_osrs_stretch:icon7",
                interf = "interface.side_channels",
                target = "component.toplevel_osrs_stretch:side7",
                events =
                    arrayOf(
                        IfEvent.Op1,
                        IfEvent.Op2,
                        IfEvent.Op3,
                        IfEvent.Op4,
                        IfEvent.Op5,
                        IfEvent.Op6,
                    ),
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone8",
                icon = "component.toplevel_osrs_stretch:icon8",
                interf = "interface.account",
                target = "component.toplevel_osrs_stretch:side8",
                events = arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4),
                unlockStage = TutorialStage.ACCOUNT_GUIDE_TALKED,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone9",
                icon = "component.toplevel_osrs_stretch:icon9",
                interf = "interface.friends",
                target = "component.toplevel_osrs_stretch:side9",
                events = arrayOf(IfEvent.Op1, IfEvent.Op2),
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone10",
                icon = "component.toplevel_osrs_stretch:icon10",
                interf = "interface.logout",
                target = "component.toplevel_osrs_stretch:side10",
                events = arrayOf(IfEvent.Op1, IfEvent.Op2),
                alwaysUnlocked = true,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone11",
                icon = "component.toplevel_osrs_stretch:icon11",
                interf = "interface.settings_side",
                target = "component.toplevel_osrs_stretch:side11",
                events = arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4),
                unlockStage = TutorialStage.SETTINGS_REQUESTED,
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone12",
                icon = "component.toplevel_osrs_stretch:icon12",
                interf = "interface.emote",
                target = "component.toplevel_osrs_stretch:side12",
                events = arrayOf(IfEvent.Op1),
            ),
            Tab(
                stone = "component.toplevel_osrs_stretch:stone13",
                icon = "component.toplevel_osrs_stretch:icon13",
                interf = "interface.music",
                target = "component.toplevel_osrs_stretch:side13",
                events = arrayOf(IfEvent.Op1),
            ),
        )

    fun sync(player: Player, eventBus: EventBus) {
        if (!player.inTutorial) {
            if (player.tutorialStage >= TutorialStage.LEFT_ISLAND) {
                unlockAll(player, eventBus)
            }
            return
        }
        for (tab in TABS) {
            val unlocked =
                tab.alwaysUnlocked ||
                    (tab.unlockStage != null && player.tutorialStage >= tab.unlockStage)
            if (unlocked) {
                enable(player, tab, eventBus)
            } else {
                disable(player, tab, eventBus)
            }
        }
    }

    fun unlockAll(player: Player, eventBus: EventBus) {
        for (tab in TABS) {
            enable(player, tab, eventBus)
        }
    }

    private fun enable(player: Player, tab: Tab, eventBus: EventBus) {
        if (!player.ui.containsOverlay(tab.interf)) {
            player.ifOpenOverlay(tab.interf, tab.target, eventBus)
        }
        player.ifSetHide(tab.stone, false)
        player.ifSetHide(tab.icon, false)
        player.ifSetEvents(tab.stone, -1..-1, *tab.events)
    }

    private fun disable(player: Player, tab: Tab, eventBus: EventBus) {
        if (player.ui.containsOverlay(tab.interf)) {
            player.ifCloseSub(tab.interf, eventBus)
        }
        player.ifSetHide(tab.stone, true)
        player.ifSetHide(tab.icon, true)
        player.ifSetEvents(tab.stone, -1..-1)
    }
}
