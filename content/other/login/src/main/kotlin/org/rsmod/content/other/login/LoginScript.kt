package org.rsmod.content.other.login

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpServerType
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import net.rsprot.protocol.game.outgoing.misc.client.ResetAnims
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import org.rsmod.api.inv.weight.InvWeight
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.net.central.writeCentralSocialSnapshot
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.UpdateStat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.realm.Realm
import org.rsmod.api.script.onEvent
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.table.DidyouknowRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LoginScript
@Inject
constructor(
    private val realm: Realm,
    private val mapClock: MapClock,
    private val invisibleLevels: InvisibleLevels,
    private val central: OpenRuneCentralWorldLink,
    private val config: ServerConfig
) : PluginScript() {
    private val transmitVars by lazy { transmitVars() }

    private var Player.chatboxUnlocked: Boolean by boolVarBit("varbit.has_displayname_transmitter")

    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.EngineLogin>(0L) { player.engineLogin() }
    }

    private fun Player.engineLogin() {
        sendHighPriority()
        sendLowPriority()
        VarPlayerIntMapSetter.set(this,"varbit.player_in_instance",0)
    }

    private fun Player.sendHighPriority() {
        sendChatFilters()
        sendSocial()
        sendOpVisibility()
        sendWelcomeMessage()
        val validDidYouKnow = DidyouknowRow.all().filter { it.mobileonly != true }.random()
        mes("Did you know? ${validDidYouKnow.tip}", ChatType.DidYouKnow)
        sendVars()
    }

    private fun Player.sendChatFilters() {
        client.write(ChatFilterSettings(0, 0))
    }

    private fun Player.sendOpVisibility() {
        client.write(HideNpcOps(false))
        client.write(HideLocOps(false))
        client.write(HideObjOps(false))
    }


    private fun Player.sendWelcomeMessage() {
        val message = realm.config.loginMessage
        message?.let {
            mes(it.replace("RS Mod", config.name), ChatType.Welcome)
        }

        val broadcast = realm.config.loginBroadcast
        broadcast?.let { mes(it, ChatType.Broadcast) }
    }

    private fun Player.sendVars() {
        client.write(VarpReset)
        chatboxUnlocked = displayName.isNotBlank()
        for (varp in transmitVars) {
            if (varp in vars) {
                resyncVar(varp)
            }
        }
    }

    private fun Player.sendLowPriority() {
        sendInvs()
        runClientScript(2498, 1, 0, 0)
        resetCam()
        runClientScript(828, 1)
        runClientScript(5141)
        runClientScript(626)
        sendPlayerOps()
        runClientScript(876, mapClock.cycle, 0, displayName, "REGULAR")
        sendStats()
        sendRun()
        client.write(ResetAnims)
        client.write(MinimapToggle(0))
    }

    private fun Player.sendInvs() {
        startInvTransmit(inv)
        startInvTransmit(worn)
    }

    private fun Player.resetCam() {
        Camera.camReset(this)
    }

    private fun Player.sendStats() {
        for (stat in ServerCacheManager.getStats().values) {
            val statInternal = RSCM.getReverseMapping(RSCMType.STAT,stat.id)

            val currXp = statMap.getXP(statInternal)
            val currLvl = stat(statInternal)
            val hiddenLvl = currLvl + invisibleLevels.get(this, statInternal)
            UpdateStat.update(this, stat, currXp, currLvl, hiddenLvl)
        }
    }

    private fun Player.sendRun() {
        val weightInGrams = InvWeight.calculateWeightInGrams(this)
        runWeight = weightInGrams
        UpdateRun.weight(this, kg = weightInGrams / 1000)
        UpdateRun.energy(this, runEnergy)
    }

    private fun Player.sendPlayerOps() {
        MiscOutput.setPlayerOp(this, slot = 2, op = null)
        MiscOutput.setPlayerOp(this, slot = 3, op = "Follow")
        MiscOutput.setPlayerOp(this, slot = 4, op = "Trade with")
        MiscOutput.setPlayerOp(this, slot = 5, op = null)
        MiscOutput.setPlayerOp(this, slot = 8, op = "Report")
    }

    private fun transmitVars(): List<VarpServerType> {
        return ServerCacheManager.getVarps().values.filter { !it.transmit.never }.sortedBy { it.id }
    }

    private fun Player.sendSocial() {
        val token = openRuneCentralSessionToken

        if (token == null || characterId <= 0) {
            mes("Social list did not load: missing Central session.")
            return
        }

        when (val result = central.socialSnapshot(token, characterId)) {
            is OpenRuneCentralWorldLink.CentralSocialSnapshotResult.Ok -> {
                writeCentralSocialSnapshot(result.snapshot)
            }

            is OpenRuneCentralWorldLink.CentralSocialSnapshotResult.Failed -> {
                mes("Social list did not load: ${result.message}")
            }
        }
    }

}
