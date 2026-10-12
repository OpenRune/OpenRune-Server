package org.rsmod.content.travel.canoe.configs

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.rsmod.api.enums.CanoeShapingAxeEnums
import org.rsmod.api.enums.NamedEnums
import org.rsmod.api.testing.GameTestState

class CanoeConfigTest {
    @Test
    @Disabled("canoe_shaping_axe_anims has no entry for Crystal axe or Corrupted axe")
    fun GameTestState.`ensure all axe objs have canoe shaping anim`() = runBasicGameTest {
        assertNoMissingAxes(CanoeShapingAxeEnums.canoe_shaping_axe_anims, "canoe_shaping_axe_anims")
    }

    @Test
    @Disabled("canoe_station_axe_rates has no entry for Crystal axe or Corrupted axe")
    fun GameTestState.`ensure all axe objs have station axe rates`() = runBasicGameTest {
        assertNoMissingAxes(NamedEnums.canoe_station_axe_rates, "canoe_station_axe_rates")
    }

    @Test
    @Disabled("canoe_shaping_axe_rates has no entry for Crystal axe or Corrupted axe")
    fun GameTestState.`ensure all axe objs have shaping axe rates`() = runBasicGameTest {
        assertNoMissingAxes(CanoeShapingAxeEnums.canoe_shaping_axe_rates, "canoe_shaping_axe_rates")
    }

    private fun assertNoMissingAxes(enum: EnumTypeMap<ItemServerType, *>, name: String) {
        val axes =
            ServerCacheManager.getItems().values.filter {
                it.isContentType("content.woodcutting_axe")
            }
        val required = enum.keys.map(ItemServerType::id)
        val missing = axes.filter { it.id !in required }
        assertEquals(emptyList<ItemServerType>(), missing) {
            "Found missing axe objs in `$name` enum:"
        }
    }
}
