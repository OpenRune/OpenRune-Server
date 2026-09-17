package org.rsmod.content.areas.misc.motherlode.scripts

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.areas.misc.motherlode.MotherlodeMine
import org.rsmod.content.areas.misc.motherlode.circuit.MotherlodeWaterCircuit
import org.rsmod.content.areas.misc.motherlode.hasUpperLevel
import org.rsmod.content.areas.misc.motherlode.onUpperLevel
import org.rsmod.content.areas.misc.motherlode.syncMotherlodeVars
import org.rsmod.content.areas.misc.motherlode.veins.MotherlodeVeins
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.events.EventBus
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeMineScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val veins: MotherlodeVeins,
    private val circuit: MotherlodeWaterCircuit,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> {
            veins.tick()
            circuit.tick()
        }

        onPlayerLogin {
            player.onUpperLevel = MotherlodeMine.isUpperFloor(player.coords)
            player.syncMotherlodeVars()
            circuit.resume(player)
        }
        onPlayerLogout { circuit.remove(player) }

        onArea(MotherlodeMine.AREA) {
            player.onUpperLevel = MotherlodeMine.isUpperFloor(player.coords)
            player.syncMotherlodeVars()
            player.ifOpenOverlay(MotherlodeMine.HUD, HUD_TARGET, eventBus)
        }
        onAreaExit(MotherlodeMine.AREA) {
            if (!player.pendingLogout && !player.loggingOut) {
                player.onUpperLevel = false
            }
            player.ifCloseOverlay(MotherlodeMine.HUD, eventBus)
        }

        onOpLoc1("loc.motherlode_entrance") { crawl(MotherlodeMine.FALADOR_ENTRANCE_DEST) }
        onOpLoc1("loc.motherlode_exit") { crawl(MotherlodeMine.FALADOR_EXIT_DEST) }
        onOpLoc1("loc.motherlode_entrance_guild") { crawl(MotherlodeMine.GUILD_ENTRANCE_DEST) }
        onOpLoc1("loc.motherlode_exit_guild") { enterMiningGuild() }

        onOpLoc1("loc.motherlode_ladder_bottom") { climbUp() }
        onOpLoc1("loc.motherlode_ladder_top") { climbDown() }

        onOpLoc1("loc.blast_bank_chest") { tryOpenBank() }
    }

    private suspend fun ProtectedAccess.crawl(dest: CoordGrid) {
        arriveDelay()
        anim("seq.human_longcrawl")
        soundSynth(CRAWL_SOUND, loops = 2, delay = 4)
        delay(1)
        telejump(dest)
        player.onUpperLevel = false
        resetAnim()
    }

    private suspend fun ProtectedAccess.enterMiningGuild() {
        if (player.miningLvl < MINING_GUILD_LEVEL) {
            startDialogue {
                chatNpcSpecific(
                    "Dwarf",
                    "npc.motherlode_mguild_guard",
                    neutral,
                    "Sorry, but you're not experienced enough to go in there.",
                )
                mesbox("You need a Mining level of $MINING_GUILD_LEVEL to access the Mining Guild.")
            }
            return
        }
        crawl(MotherlodeMine.GUILD_EXIT_DEST)
    }

    private suspend fun ProtectedAccess.climbUp() {
        if (!player.hasUpperLevel) {
            mes("You need to pay Prospector Percy to access the upper level of the mine.")
            return
        }
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(LADDER_CYCLES)
        telejump(MotherlodeMine.LADDER_BOTTOM_DEST)
        player.onUpperLevel = true
        resetAnim()
    }

    private suspend fun ProtectedAccess.climbDown() {
        arriveDelay()
        anim("seq.human_reachforladdertop")
        delay(LADDER_CYCLES)
        telejump(MotherlodeMine.LADDER_TOP_DEST)
        player.onUpperLevel = false
        resetAnim()
    }

    private companion object {
        const val HUD_TARGET = "component.toplevel_osrs_stretch:overlay_hud"
        const val CRAWL_SOUND = 2454
        const val LADDER_CYCLES = 2
        const val MINING_GUILD_LEVEL = 60
    }
}
