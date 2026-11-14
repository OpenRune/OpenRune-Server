package org.rsmod.content.quest.area.asgarnia.icemountain

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
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
        onPlayerSoftTimer(BIM_GUARDIAN_FINISH_TIMER) {
            guardian.resumeFinish(player, quest)
        }
        onOpLoc1("loc.bim_boss_rock") { guardian.minePillar(this, it.loc, quest) }
        onOpLoc1("loc.bim_door") { enterRuins() }
        onOpLoc1("loc.bim_exit") { exitRuins() }
    }

    private suspend fun ProtectedAccess.enterRuins() {
        val stage = quest.stage(player)
        if (stage < BimStage.INSIDE_CUTSCENE) {
            mes("The entrance is blocked.")
            return
        }
        arriveDelay()
        if (stage == BimStage.GUARDIAN_FIGHT) {
            guardian.enterFight(this, quest)
            return
        }
        telejump(CoordGrid(2952, 5762))
        guardian.ensureFor(player, stage)
    }

    private suspend fun ProtectedAccess.exitRuins() {
        if (quest.stage(player) == BimStage.GUARDIAN_FIGHT) {
            guardian.releaseScene()
        }
        arriveDelay()
        telejump(CoordGrid(2998, 3494))
    }
}
