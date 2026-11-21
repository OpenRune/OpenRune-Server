package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.game.entity.Player

internal object TutorialTabs {
    private const val PREFIX = "component.toplevel_osrs_stretch"

    private val ALL_STONES =
        listOf(
            "stone0",
            "stone1",
            "stone2",
            "stone3",
            "stone4",
            "stone5",
            "stone6",
            "stone7",
            "stone8",
            "stone9",
            "stone10",
            "stone11",
            "stone12",
            "stone13",
        )

    private val UNLOCKS =
        mapOf(
            "stone11" to TutorialStage.SETTINGS_REQUESTED,
            "stone3" to TutorialStage.NET_GIVEN,
            "stone1" to TutorialStage.SHRIMP_CAUGHT,
            "stone2" to TutorialStage.QUEST_GUIDE_TALKED,
            "stone4" to TutorialStage.DAGGER_EQUIP_TASK,
            "stone0" to TutorialStage.SWORD_EQUIPPED,
            "stone8" to TutorialStage.ACCOUNT_GUIDE_TALKED,
            "stone5" to TutorialStage.PRAYER_GUIDE_TALKED,
            "stone6" to TutorialStage.MAGIC_GUIDE_TALKED,
        )

    private val FULL_MASKS =
        mapOf(
            "stone0" to arrayOf(IfEvent.Op1),
            "stone1" to arrayOf(IfEvent.Op1),
            "stone2" to arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op6),
            "stone3" to arrayOf(IfEvent.Op1),
            "stone4" to arrayOf(IfEvent.Op1),
            "stone5" to arrayOf(IfEvent.Op1, IfEvent.Op2),
            "stone6" to arrayOf(IfEvent.Op1, IfEvent.Op2),
            "stone7" to arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op6),
            "stone8" to arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4),
            "stone9" to arrayOf(IfEvent.Op1, IfEvent.Op2),
            "stone10" to arrayOf(IfEvent.Op1, IfEvent.Op2),
            "stone11" to arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4),
            "stone12" to arrayOf(IfEvent.Op1),
            "stone13" to arrayOf(IfEvent.Op1),
        )

    fun sync(player: Player) {
        if (!player.inTutorial) {
            if (player.tutorialStage >= TutorialStage.LEFT_ISLAND) {
                unlockAll(player)
            }
            return
        }
        for (stone in ALL_STONES) {
            val unlockStage = UNLOCKS[stone]
            if (unlockStage != null && player.tutorialStage >= unlockStage) {
                enable(player, stone)
            } else {
                disable(player, stone)
            }
        }
    }

    fun unlockAll(player: Player) {
        for (stone in ALL_STONES) {
            enable(player, stone)
        }
    }

    private fun enable(player: Player, stone: String) {
        val events = FULL_MASKS.getValue(stone)
        player.ifSetEvents("$PREFIX:$stone", -1..-1, *events)
    }

    private fun disable(player: Player, stone: String) {
        player.ifSetEvents("$PREFIX:$stone", -1..-1)
    }
}
