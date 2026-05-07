package org.rsmod.content.other.login

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.VarpType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpServerType
import dev.openrune.types.varp.baseVar
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import net.rsprot.protocol.game.outgoing.misc.client.ResetAnims
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import org.rsmod.api.inv.weight.InvWeight
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
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import net.rsprot.protocol.game.outgoing.social.FriendListLoaded
import org.rsmod.api.social.social
import org.rsmod.api.social.SocialData
import org.rsmod.api.social.pushChatModes
import org.rsmod.api.social.pushFriends
import org.rsmod.api.social.pushIgnores
import org.rsmod.api.social.pushPrivateChatMode

class LoginScript
@Inject
constructor(
    private val realm: Realm,
    private val mapClock: MapClock,
    private val invisibleLevels: InvisibleLevels,
    private val config: ServerConfig,
) : PluginScript() {
    private val transmitVars by lazy { transmitVars() }

    private var Player.chatboxUnlocked: Boolean by boolVarBit("varbit.has_displayname_transmitter")
    private var Player.hideRoofs by boolVarBit("varbit.option_hide_rooftops")

    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.EngineLogin>(0L) { player.engineLogin() }
    }

    private fun Player.engineLogin() {
        sendHighPriority()
        sendLowPriority()
    }

    private fun Player.sendHighPriority() {
        sendChatFilters()
        sendSocial()
        sendOpVisibility()
        sendWelcomeMessage()
        sendVars()
    }

    private fun Player.sendSocial() {
        client.write(FriendListLoaded)
        pushPrivateChatMode()
        pushFriends()
        pushIgnores()
    }

    private fun Player.sendChatFilters() {
        pushChatModes()
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
        setDefaultAudioOptions()
        hideRoofs = true
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

    private fun Player.setDefaultAudioOptions() {
        setDefaultVarp("varp.option_master_volume", DEFAULT_AUDIO_VOLUME)
        setDefaultVarp("varp.option_music", DEFAULT_AUDIO_VOLUME)
        setDefaultVarp("varp.option_sounds", DEFAULT_AUDIO_VOLUME)
        setDefaultVarp("varp.option_areasounds", DEFAULT_AUDIO_VOLUME)
        setDefaultDesktopAudioOptions()
        setUnmuteAudioSavedOptions()
    }

    private fun Player.setDefaultDesktopAudioOptions() {
        val desktopAudio =  ServerCacheManager.getVarbit("varbit.option_master_volume_desktop".asRSCM(RSCMType.VARBIT))!!.baseVar
        if (desktopAudio !in vars) {
            setVarBit("varbit.option_master_volume_desktop", DEFAULT_AUDIO_VOLUME)
            setVarBit("varbit.option_music_desktop", DEFAULT_AUDIO_VOLUME)
            setVarBit("varbit.option_master_volume_saved_desktop", DEFAULT_UNMUTE_VOLUME)
            setVarBit("varbit.option_music_saved_desktop", DEFAULT_UNMUTE_VOLUME)
        }

        val desktopEffects = ServerCacheManager.getVarbit("varbit.option_sounds_desktop".asRSCM(RSCMType.VARBIT))!!.baseVar
        if (desktopEffects !in vars) {
            setVarBit("varbit.option_sounds_desktop", DEFAULT_AUDIO_VOLUME)
            setVarBit("varbit.option_areasounds_desktop", DEFAULT_AUDIO_VOLUME)
            setVarBit("varbit.option_sounds_saved_desktop", DEFAULT_UNMUTE_VOLUME)
            setVarBit("varbit.option_areasounds_saved_desktop", DEFAULT_UNMUTE_VOLUME)
        }
    }

    private fun Player.setUnmuteAudioSavedOptions() {
        setVarBit("varbit.option_master_volume_saved_desktop", DEFAULT_UNMUTE_VOLUME)
        setVarBit("varbit.option_music_saved_desktop", DEFAULT_UNMUTE_VOLUME)
        setVarBit("varbit.option_sounds_saved_desktop", DEFAULT_UNMUTE_VOLUME)
        setVarBit("varbit.option_areasounds_saved_desktop", DEFAULT_UNMUTE_VOLUME)
        setVarBit("varbit.option_master_volume_saved", DEFAULT_UNMUTE_VOLUME)
        setVarBit("varbit.option_music_saved", DEFAULT_UNMUTE_VOLUME)
        setVarBit("varbit.option_sounds_saved", DEFAULT_UNMUTE_VOLUME)
        setVarBit("varbit.option_areasounds_saved", DEFAULT_UNMUTE_VOLUME)
    }

    private fun Player.setDefaultVarp(varp: String, value: Int) {
        if (vars.contains(varp)) {
            VarPlayerIntMapSetter.set(this, varp, value)
        }
    }

    private fun Player.setVarBit(varbit: String, value: Int) {
        VarPlayerIntMapSetter.set(this, varbit, value)
    }

    private fun transmitVars(): List<VarpServerType> {
        return ServerCacheManager.getVarps().values.filter { !it.transmit.never }.sortedBy { it.id }
    }

    private companion object {
        private const val DEFAULT_AUDIO_VOLUME = 100
        private const val DEFAULT_UNMUTE_VOLUME = 5
    }
}
