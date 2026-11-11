package org.rsmod.content.events.demonicpacts.tasks

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.vars.PlayerVarChangedEvent
import org.rsmod.api.player.vars.PlayerVarWatch
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTasks
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MiscLeagueTasks
@Inject
constructor(private val tasks: LeagueTasks, private val areas: AreaChecker) : PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private val runTask = task("league_task_turn_off_your_run")
    private val steelTask = task("league_task_smith_steel_in_kourend_castle")

    private val steelBar = "obj.steel_bar".asRSCM(RSCMType.OBJ)
    private val kourendCastle = "area.league_kourend_castle".asRSCM(RSCMType.AREA)
    private val runVarp = "varp.option_run".asRSCM(RSCMType.VARP)

    override fun ScriptContext.startup() {
        tasks.claim(runTask, steelTask)

        PlayerVarWatch.watch(runVarp)
        onEvent<PlayerVarChangedEvent> {
            if (varp == runVarp && previous == RUN && value != RUN) {
                tasks.complete(player, runTask)
            }
        }

        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            val anvil = product.source as? SkillingProductSource.Anvil ?: return@onEvent
            if (anvil.bar.output.id == steelBar && areas.inArea(kourendCastle, player.coords)) {
                tasks.complete(player, steelTask)
            }
        }
    }

    private companion object {
        const val RUN = 1
    }
}
