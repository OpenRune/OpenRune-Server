package org.rsmod.content.minigames.gauntlet

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.random.Random
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.instances.withInstanceLeaveTransition
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.content.minigames.gauntlet.layout.GauntletLayout
import org.rsmod.content.minigames.gauntlet.layout.LayoutTemplateBuilder
import org.rsmod.content.minigames.gauntlet.layout.RoomContentsGenerator
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal var Player.inGauntlet by boolVarBit("varbit.player_in_gauntlet")
internal var Player.gauntletCorrupted by boolVarBit("varbit.gauntlet_corrupted")
internal var Player.gauntletStart by intVarBit("varbit.gauntlet_start")

@Singleton
class GauntletRuns
@Inject
constructor(
    private val manager: InstanceManager,
    private val clock: MapClock,
    private val lighting: GauntletLighting,
    private val spawner: GauntletContents,
    private val regions: RegionRegistry,
    private val rewards: GauntletRewards,
    private val bryn: GauntletBryn,
) {
    private val active = HashMap<InstanceId, GauntletRun>()

    fun runFor(npc: Npc): GauntletRun? = manager.instanceForNpc(npc)?.let { active[it] }

    fun runFor(player: Player): GauntletRun? =
        manager.sessionForPlayer(player)?.let { active[it.id] }

    suspend fun ProtectedAccess.enter(mode: GauntletMode) {
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        if (!with(bryn) { canEnter() }) return
        val layout = GauntletLayout.generate(Random)
        val contents = RoomContentsGenerator.generate(layout, mode, GauntletContents.SLOTS, Random)
        val spec = spec(mode, layout)
        when (val result = manager.create(player, KEY, spec, InstanceAccess.Private, clock.cycle)) {
            is InstanceManager.Result.Failed -> mes(result.reason)
            is InstanceManager.Result.Created ->
                withInstanceEnterTransition(InstanceEnterTransition()) {
                    active.keys.removeIf { manager.sessionForId(it) == null }
                    val run = GauntletRun(layout, mode, contents)
                    active[result.session.id] = run
                    telejump(result.enter, TeleportType.Exempt)
                    manager.finalizeEntry(player, result.session, clock.cycle)
                    with(lighting) { lightEntry(result.enter, run) }
                    regions[result.enter]?.let { spawner.spawnHunllef(it, run, player) }
                    GauntletHolding.store(player)
                    GauntletHolding.resetVitals(player)
                    GauntletHolding.giveStartingKit(player, mode)
                    player.inGauntlet = true
                    player.gauntletCorrupted = mode.corrupted
                    player.gauntletStart = layout.startIndex
                    mes("You enter the Gauntlet.")
                    ifOpenOverlay(OVERLAY, OVERLAY_TARGET)
                    runClientScript(TIMER_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT), mode.timerTicks, if (mode.corrupted) 1 else 0)
                    player.softTimer(TIME_LIMIT_TIMER, mode.timerTicks)
                }
            is InstanceManager.Result.Joined -> Unit
        }
    }

    suspend fun ProtectedAccess.leave(loot: Boolean = false) {
        val session = manager.sessionForPlayer(player) ?: return
        val run = active[session.id]
        if (loot && run != null) rewards.settleByPoints(player, run)
        active.remove(session.id)
        withInstanceLeaveTransition(InstanceEnterTransition()) {
            val exit = manager.leave(player, session, clock.cycle)
            telejump(exit, TeleportType.Exempt)
        }
        if (run != null && rewards.isFirstNormalCompletion(player, run)) {
            with(bryn) { corruptedUnlock() }
        }
    }

    private fun spec(mode: GauntletMode, layout: GauntletLayout): InstanceSpec =
        InstanceSpec(
            fee = 0,
            maxPlayers = 1,
            reclaimTicks = 0,
            graceTicks = 0,
            destroyWhenEmpty = true,
            area =
                InstanceArea.template(
                    template = LayoutTemplateBuilder.build(layout, mode),
                    enterCoord = mode.enterTile(),
                    exitCoord = GauntletLobby.LOBBY,
                ),
            settingsRowId = 0,
        )

    companion object {
        const val KEY = "gauntlet"
        const val OVERLAY = "interface.gauntlet_overlay"
        const val OVERLAY_TARGET = "component.toplevel_osrs_stretch:overlay_hud"
        const val TIME_LIMIT_TIMER = "timer.gauntlet_time_limit"
        private const val TIMER_SCRIPT = "clientscript.[clientscript,gauntlet_timer_update]"
    }
}
