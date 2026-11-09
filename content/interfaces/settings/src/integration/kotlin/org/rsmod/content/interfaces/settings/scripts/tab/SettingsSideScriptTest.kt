package org.rsmod.content.interfaces.settings.scripts.tab

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.player.ui.ifOpenTop
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.testing.GameTestState

class SettingsSideScriptTest {
    @Test
    fun GameTestState.`a stone op enabled by the server selects its panel`() =
        runGameTest(SettingsSideScript::class) {
            player.ifOpenTop(TOPLEVEL)
            player.ifSetEvents(STONE, -1..-1, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4)
            player.ifButton(ServerCacheManager.fromComponent(STONE), op = IfButtonOp.Op4)
            advance()
            assertEquals(DISPLAY_PANEL, player.vars[PANEL_VARBIT])
        }

    @Test
    fun GameTestState.`a stone op the server did not enable is ignored`() =
        runGameTest(SettingsSideScript::class) {
            player.ifOpenTop(TOPLEVEL)
            player.ifSetEvents(STONE, -1..-1, IfEvent.Op1, IfEvent.Op2)
            player.ifButton(ServerCacheManager.fromComponent(STONE), op = IfButtonOp.Op4)
            advance()
            assertEquals(CONTROL_PANEL, player.vars[PANEL_VARBIT])
        }

    private companion object {
        const val TOPLEVEL = "interface.toplevel_osrs_stretch"
        const val STONE = "component.toplevel_osrs_stretch:stone11"
        const val PANEL_VARBIT = "varbit.settings_side_panel_tab"
        const val CONTROL_PANEL = 0
        const val DISPLAY_PANEL = 2
    }
}
