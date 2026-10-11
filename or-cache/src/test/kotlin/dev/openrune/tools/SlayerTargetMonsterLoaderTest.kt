package dev.openrune.tools

import dev.openrune.types.SlayerTargetMonster
import dev.openrune.types.SlayerTaskKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SlayerTargetMonsterLoaderTest {

    @Test
    fun `an npc listed under several tasks keeps every task`() {
        val keys =
            SlayerTargetMonsterLoader.collectTaskKeys(
                listOf(
                    SlayerTargetMonster(targetId = 4, targets = listOf(10, 11)),
                    SlayerTargetMonster(targetId = 124, targets = listOf(10)),
                    SlayerTargetMonster(targetId = 4, targets = listOf(10)),
                )
            )

        assertEquals(listOf(4, 124), keys[10])
        assertEquals(listOf(4), keys[11])
    }

    @Test
    fun `a boss block keys the npc by boss id instead of the generic boss task`() {
        val keys =
            SlayerTargetMonsterLoader.collectTaskKeys(
                listOf(
                    SlayerTargetMonster(targetId = 25, targets = listOf(7)),
                    SlayerTargetMonster(targetId = SlayerTaskKey.BOSS_TASK_ID, bossId = 23, targets = listOf(7)),
                )
            )

        assertEquals(listOf(25, SlayerTaskKey.of(SlayerTaskKey.BOSS_TASK_ID, 23)), keys[7])
        assertFalse(SlayerTaskKey.BOSS_TASK_ID in keys.getValue(7))
    }
}
