package dev.openrune.tools

import dev.openrune.types.SlayerTargetMonster
import dev.openrune.types.SlayerTaskKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SlayerTargetMonsterLoaderTest {

    @Test
    fun `an npc listed under several tasks keeps every task`() {
        val data =
            SlayerTargetMonsterLoader.collect(
                listOf(
                    SlayerTargetMonster(targetId = 4, targets = listOf(10, 11)),
                    SlayerTargetMonster(targetId = 124, targets = listOf(10)),
                    SlayerTargetMonster(targetId = 4, targets = listOf(10)),
                )
            )

        assertEquals(listOf(4, 124), data.getValue(10).keys)
        assertEquals(listOf(4), data.getValue(11).keys)
    }

    @Test
    fun `a boss block keys the npc by boss id instead of the generic boss task`() {
        val keys =
            SlayerTargetMonsterLoader.collect(
                    listOf(
                        SlayerTargetMonster(targetId = 25, targets = listOf(7)),
                        SlayerTargetMonster(
                            targetId = SlayerTaskKey.BOSS_TASK_ID,
                            bossId = 23,
                            targets = listOf(7),
                        ),
                    )
                )
                .getValue(7)
                .keys

        assertEquals(listOf(25, SlayerTaskKey.of(SlayerTaskKey.BOSS_TASK_ID, 23)), keys)
        assertFalse(SlayerTaskKey.BOSS_TASK_ID in keys)
    }

    @Test
    fun `a block without a task still sets the variant and level of its npcs`() {
        val data =
            SlayerTargetMonsterLoader.collect(
                listOf(
                    SlayerTargetMonster(targetId = 43, variant = 2, level = 60, targets = listOf(5)),
                    SlayerTargetMonster(level = 1, targets = listOf(6)),
                )
            )

        assertEquals(2, data.getValue(5).variant)
        assertEquals(60, data.getValue(5).level)
        assertEquals(emptyList<Int>(), data.getValue(6).keys)
        assertEquals(1, data.getValue(6).level)
    }
}
