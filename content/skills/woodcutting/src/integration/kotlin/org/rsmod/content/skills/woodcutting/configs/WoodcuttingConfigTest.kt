package org.rsmod.content.skills.woodcutting.configs

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.cutSuccessRates
import org.rsmod.game.inv.InvObj

class WoodcuttingConfigTest {
    @Test
    fun GameTestState.`ensure all axe objs have required params`() = runBasicGameTest {
        val axes =
            ServerCacheManager.getItems().values.filter {
                it.isContentType("content.woodcutting_axe")
            }
        for (axe in axes) {
            assertTrue(axe.hasParam(params.levelrequire)) { "Axe missing levelrequire: $axe" }
            assertTrue(axe.hasParam(params.skill_anim)) { "Axe missing skill_anim: $axe" }
        }
    }

    @Test
    fun GameTestState.`ensure all tree locs have required params`() = runBasicGameTest {
        val trees =
            ServerCacheManager.getObjects().values.filter { it.isContentType("content.tree") }
        for (tree in trees) {
            assertTrue(tree.hasParam(params.levelrequire))
            assertTrue(tree.hasParam(params.skill_productitem))
            assertTrue(tree.hasParam(params.skill_xp))
            assertTrue(tree.hasParam(params.next_loc_stage))
            assertTrue(tree.hasParam(params.respawn_time))
            assertTrue(tree.hasParam(WoodcuttingParams.success_rates))
        }

        // Trees which turn into stumps after a set period of time.
        val timed = trees.filter { it.hasParam(params.despawn_time) }
        for (tree in timed) {
            assertFalse(tree.hasParam(params.deplete_chance))
        }

        // Trees which turn into stumps based on random rates.
        val standard = trees - timed.toSet()
        for (tree in standard) {
            assertTrue(tree.hasParam(params.deplete_chance))
            assertTrue(tree.param(params.deplete_chance) in 0..255)
        }

        // Trees which have a variable respawn time.
        val respawnVariable =
            trees.filter {
                it.hasParam(params.respawn_time_low) || it.hasParam(params.respawn_time_high)
            }
        for (tree in respawnVariable) {
            assertTrue(tree.hasParam(params.respawn_time_low))
            assertTrue(tree.hasParam(params.respawn_time_high))
            assertEquals(0, tree.param(params.respawn_time))
        }
    }

    @Test
    @Disabled(
        "woodcutting_axe_success_rates has no entry for some axes (e.g. Corrupted axe), " +
            "so cutSuccessRates() throws `Unable to get axe rates`"
    )
    fun GameTestState.`ensure all trees define success rates for every axe`() = runBasicGameTest {
        val trees =
            ServerCacheManager.getObjects().values.filter { it.isContentType("content.tree") }
        val axes =
            ServerCacheManager.getItems().values.filter {
                it.isContentType("content.woodcutting_axe")
            }
        for (tree in trees) {
            for (axe in axes) {
                val (lowRate, highRate) =
                    assertDoesNotThrow("Axe success rates not defined: axe=$axe, tree=$tree") {
                        cutSuccessRates(tree, InvObj(axe))
                    }
                assertTrue(lowRate > 0)
                assertTrue(highRate > 0)
            }
        }
    }
}
