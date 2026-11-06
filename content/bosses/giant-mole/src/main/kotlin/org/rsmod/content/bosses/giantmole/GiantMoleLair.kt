package org.rsmod.content.bosses.giantmole

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val WARNING_INTERFACE = "interface.cws_warning_3"
private const val WARNING_INTERFACE_ID = 568
private const val WARNING_TEXT_COMPONENT = 14
private const val WARNING_PROCEED_COMPONENT = 17
private const val WARNING_CANCEL_COMPONENT = 18

private const val WARNING_TEXT =
    "It would probably be a good idea to bring a tinderbox<br>in case your light source is extinguished."
private const val PROCEED_TEXT = "Proceed regardless."
private const val CANCEL_TEXT = "Don't go down yet."
internal const val NO_LIGHT_MESSAGE =
    "It's going to be dark down there, I should bring a light source."
private const val ENTRY_MESSAGE =
    "You seem to have dropped down into a network of mole tunnels."
private const val THE_MAD_MOLE_MIDI = 573

internal val GIANT_MOLE_ENTRY = CoordGrid(1752, 5237)
internal val GIANT_MOLE_EXIT = CoordGrid(2986, 3316)

class GiantMoleLair
@Inject
constructor(
    private val environment: MoleHoleEnvironment,
    private val playerList: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLocU("loc.mole_hill", "obj.spade") { digFromHill() }
        onOpLoc1("loc.mole_hill") { peekMoleHole() }
        for (rope in ROPE_LOCS) {
            onOpLoc1(rope) { leaveMoleHole() }
            onOpLoc2(rope) { leaveMoleHole() }
        }

        onProtectedEvent<IfModalButton>(warningButton(WARNING_PROCEED_COMPONENT)) {
            proceedWarning()
        }
        onProtectedEvent<IfModalButton>(warningButton(WARNING_CANCEL_COMPONENT)) { ifClose() }

        onPlayerLogin { environment.onLogin(player) }
        onPlayerLogout { environment.onLogout(player) }
        onPlayerSoftTimer(GIANT_MOLE_INSECT_WARNING_TIMER) {
            environment.onWarningTimer(player)
        }
        onPlayerSoftTimer(GIANT_MOLE_INSECT_SWARM_TIMER) { environment.onSwarmTimer(player) }
        onPlayerSoftTimer(GIANT_MOLE_INSECT_DAMAGE_TIMER) { environment.onDamageTimer(player) }
    }

    private suspend fun ProtectedAccess.digFromHill() {
        if (!environment.hasLightSource(player) && !environment.hasPermanentLight(player)) {
            playDig()
            mes(NO_LIGHT_MESSAGE)
            return
        }
        playDig()
        if (!environment.hasTinderbox(player) && !environment.hasPermanentLight(player)) {
            openMoleWarning()
            return
        }
        enterMoleHole(environment)
    }

    private suspend fun ProtectedAccess.proceedWarning() {
        ifClose()
        playDig()
        enterMoleHole(environment)
    }

    private suspend fun ProtectedAccess.leaveMoleHole() {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        resetAnim()
        environment.leave(player)
        telejump(GIANT_MOLE_EXIT)
    }

    private fun ProtectedAccess.peekMoleHole() {
        val count =
            playerList.count {
                it !== player && it.regionUid == null && GiantMoleRules.inMoleLair(it.coords)
            }
        val adventurers =
            when (count) {
                0 -> "no adventurers"
                1 -> "1 adventurer"
                else -> "$count adventurers"
            }
        mes("You look inside the mole hill and see $adventurers inside the mole tunnels.")
    }

    private fun warningButton(component: Int): Long =
        (WARNING_INTERFACE_ID.toLong() shl 16) or component.toLong()

    private companion object {
        private val ROPE_LOCS = listOf("loc.mole_rope_01", "loc.mole_rope_02")
    }
}

internal suspend fun ProtectedAccess.playDig() {
    anim("seq.human_dig")
    soundSynth("synth.digspade")
    delay(2)
    resetAnim()
}

internal fun ProtectedAccess.openMoleWarning() {
    player.client.write(IfSetText(WARNING_INTERFACE_ID, WARNING_TEXT_COMPONENT, WARNING_TEXT))
    player.client.write(IfSetText(WARNING_INTERFACE_ID, WARNING_PROCEED_COMPONENT, PROCEED_TEXT))
    player.client.write(IfSetText(WARNING_INTERFACE_ID, WARNING_CANCEL_COMPONENT, CANCEL_TEXT))
    VarPlayerIntMapSetter.set(
        player,
        "varbit.cws_warning_3",
        player.vars["varbit.cws_warning_3"] + 1,
    )
    ifOpenMainModal(WARNING_INTERFACE)
}

internal fun ProtectedAccess.enterMoleHole(environment: MoleHoleEnvironment) {
    telejump(GIANT_MOLE_ENTRY)
    spam(ENTRY_MESSAGE)
    soundSynth("synth.roof_collapse")
    player.midiSong(THE_MAD_MOLE_MIDI, fadeOutSpeed = 60, fadeInDelay = 60)
    player.ifSetText("component.music:now_playing_text", "The Mad Mole")
    environment.enter(player)
}
