package org.rsmod.content.interfaces.combat.tab.configs

import dev.openrune.util.WeaponCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.enums.NamedEnums
import org.rsmod.api.testing.GameTestState

class CombatTabConfigTest {
    @Test
    fun GameTestState.`ensure all weapon categories have a stance-save varbit`() =
        runBasicGameTest {
            val enum = NamedEnums.weapon_last_stance_varbits
            val missing = WeaponCategory.entries.filter { it.id !in enum.keys }
            assertEquals(emptyList<WeaponCategory>(), missing) {
                "Found missing weapon categories in `weapon_last_stance_varbits` enum:"
            }
        }
}
