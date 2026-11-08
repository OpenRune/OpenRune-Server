package org.rsmod.content.events.leaguetasks.custom

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.PlayerPickEvent
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
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val flaxTask = task("league_task_pick_6_flax")
    private val cropsTask = task("league_task_pick_6_wheat_6_cabbages_and_6_potatoes")
    private val sweetcornTask = task("league_task_pick_some_sweetcorn_from_a_field")
    private val pineappleTask = task("league_task_pick_a_pineapple_on_karamja")
    private val runTask = task("league_task_turn_off_your_run")
    private val steelTask = task("league_task_smith_steel_in_kourend_castle")

    private val flax = "obj.flax".asRSCM(RSCMType.OBJ)
    private val sweetcorn = "obj.sweetcorn".asRSCM(RSCMType.OBJ)
    private val pineapple = "obj.pineapple".asRSCM(RSCMType.OBJ)
    private val steelBar = "obj.steel_bar".asRSCM(RSCMType.OBJ)
    private val crops =
        mapOf(
            "obj.grain".asRSCM(RSCMType.OBJ) to "varbit.league_task_wheat_picked",
            "obj.cabbage".asRSCM(RSCMType.OBJ) to "varbit.league_task_cabbage_picked",
            "obj.potato".asRSCM(RSCMType.OBJ) to "varbit.league_task_potatoes_picked",
        )
    private val karamja = "area.league_region_karamja".asRSCM(RSCMType.AREA)
    private val kourendCastle = "area.league_kourend_castle".asRSCM(RSCMType.AREA)
    private val runVarp = "varp.option_run".asRSCM(RSCMType.VARP)

    override fun ScriptContext.startup() {
        onEvent<PlayerPickEvent> {
            when (item.id) {
                flax ->
                    if (player.advance(FLAX_PICKED, 6) >= 6) {
                        tasks.complete(player, flaxTask)
                    }
                sweetcorn -> tasks.complete(player, sweetcornTask)
                pineapple ->
                    if (areas.inArea(karamja, player.coords)) {
                        tasks.complete(player, pineappleTask)
                    }
                in crops -> {
                    player.advance(crops.getValue(item.id), 6)
                    if (crops.values.all { player.vars[it] >= 6 }) {
                        tasks.complete(player, cropsTask)
                    }
                }
            }
        }

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
        const val FLAX_PICKED = "varbit.league_task_flax_picked"
        const val RUN = 1
    }
}
