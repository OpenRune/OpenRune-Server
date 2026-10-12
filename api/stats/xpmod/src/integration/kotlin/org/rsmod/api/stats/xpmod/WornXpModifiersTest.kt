package org.rsmod.api.stats.xpmod

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState

class WornXpModifiersTest {
    @Test
    fun GameTestState.`ensure objs with modifiers have required params`() {
        val types =
            ServerCacheManager.getItems().values.filter {
                it.hasParam(params.xpmod_stat) || it.hasParam(params.xpmod_percent)
            }
        for (type in types) {
            assertTrue(type.hasParam(params.xpmod_stat)) { "Type must have `xpmod_stat` set: $type" }
            assertTrue(type.hasParam(params.xpmod_percent)) {
                "Type must have `xpmod_percent` set: $type"
            }
        }
    }
}
