package org.rsmod.content.minigames.gauntlet.layout

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.minigames.gauntlet.GauntletMode

class RoomContentsTest {
    private val slots =
        RoomSlots.fromTemplates(
            listOf(RoomKind.MIDDLE, RoomKind.EDGE, RoomKind.CORNER).flatMap { kind ->
                (0 until GauntletLayout.VARIANTS).map { variant ->
                    val tiles = (2..13).flatMap { x -> (2..13 step 3).map { z -> Tile(x, z) } }
                    Triple(kind, variant, ResourceKind.entries.associateWith { tiles })
                }
            }
        )

    private fun generate(seed: Int, mode: GauntletMode = GauntletMode.NORMAL) =
        Random(seed).let { random ->
            val layout = GauntletLayout.generate(random)
            layout to RoomContentsGenerator.generate(layout, mode, slots, random)
        }

    @Test
    fun `only playable rooms get contents`() {
        val (layout, contents) = generate(1)
        assertEquals(47, contents.size)
        assertTrue(layout.startIndex !in contents)
        assertTrue(GauntletLayout.BOSS_INDEX !in contents)
    }

    @Test
    fun `extreme edge rooms always hold exactly one demi-boss`() {
        repeat(100) { seed ->
            val (_, contents) = generate(seed)
            for ((x, z) in listOf(3 to 0, 6 to 3, 3 to 6, 0 to 3)) {
                val room = contents.getValue(GauntletLayout.index(x, z))
                assertEquals(1, room.monsters.size)
                assertTrue(room.monsters.single().kind in MonsterKind.DEMI)
                assertTrue(room.resources.isEmpty())
            }
        }
    }

    @Test
    fun `normal mode guarantees three nodes of each kind in the inner ring`() {
        repeat(100) { seed ->
            val (layout, contents) = generate(seed)
            val inner =
                layout.all().filter {
                    it.x in 2..4 && it.z in 2..4 && it.index in contents
                }
            for (kind in listOf(ResourceKind.DEPOSIT, ResourceKind.LINUM, ResourceKind.PHREN)) {
                assertTrue(
                    inner.any { room ->
                        contents.getValue(room.index).resources.count { it.kind == kind } >= 3
                    },
                    "seed $seed missing guaranteed $kind room",
                )
            }
        }
    }

    @Test
    fun `corrupted mode guarantees a three fishing spot room`() {
        repeat(100) { seed ->
            val (layout, contents) = generate(seed, GauntletMode.CORRUPTED)
            val inner = layout.all().filter { it.x in 2..4 && it.z in 2..4 && it.index in contents }
            assertTrue(
                inner.any { room ->
                    contents.getValue(room.index).resources.count {
                        it.kind == ResourceKind.FISHING
                    } >= 3
                },
                "seed $seed missing fishing room",
            )
        }
    }

    @Test
    fun `spawns never overlap`() {
        repeat(50) { seed ->
            val (_, contents) = generate(seed)
            for (room in contents.values) {
                val tiles = room.resources.map { it.tile } + room.monsters.map { it.tile }
                assertEquals(tiles.size, tiles.toSet().size)
            }
        }
    }
}
