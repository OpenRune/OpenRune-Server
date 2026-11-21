package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.map.CoordGrid

/** Runs alone: the quest requirement policy it switches is shared by every test. */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class ShortcutQuestGateTest {
    @Test
    fun GameTestState.`a quest shortcut refuses until the quest is done`() = runShortcutTest {
        setAgility(99)
        startAt(CoordGrid(2486, 3515, 0))
        val previous = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        try {
            val messages = refuseShortcut("loc.gnome_stronghold_sc_rock_top")

            val refusal = "You need to complete The Grand Tree to use this shortcut."
            assertEquals(listOf(refusal), messages)
        } finally {
            QuestRequirements.install(previous)
        }
    }

    @Test
    fun GameTestState.`a quest shortcut opens once the quest is done`() = runShortcutTest {
        setAgility(99)
        startAt(CoordGrid(2486, 3515, 0))

        val ticks = crossShortcut("loc.gnome_stronghold_sc_rock_top")

        assertEquals(CoordGrid(2489, 3521, 0), ticks.last().coords)
    }
}
