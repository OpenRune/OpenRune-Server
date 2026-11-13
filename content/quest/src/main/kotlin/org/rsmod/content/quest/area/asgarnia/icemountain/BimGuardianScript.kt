package org.rsmod.content.quest.area.asgarnia.icemountain

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class BimGuardianScript
@Inject
constructor(
    private val quest: BelowIceMountain,
    private val guardian: BimGuardian,
    private val cutscene: BimCutscene,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            guardian.ensureFor(player, quest.stage(player))
            cutscene.ensureEntrance(quest.stage(player))
        }
        onOpLoc1("loc.bim_boss_rock") { guardian.minePillar(this, it.loc, quest) }
        onOpLoc1("loc.bim_door") { enterRuins() }
    }

    private suspend fun ProtectedAccess.enterRuins() {
        if (quest.stage(player) < BimStage.INSIDE_CUTSCENE) {
            mes("The entrance is blocked.")
            return
        }
        arriveDelay()
        telejump(CoordGrid(2952, 5762))
        guardian.ensureFor(player, quest.stage(player))
    }
}
