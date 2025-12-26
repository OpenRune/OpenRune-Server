package org.alter

import net.rsprot.protocol.game.outgoing.camera.CamReset
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import net.rsprot.protocol.game.outgoing.misc.client.ResetAnims
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings
import net.rsprot.protocol.game.outgoing.misc.player.SetPlayerOp
import net.rsprot.protocol.game.outgoing.misc.player.UpdateRunEnergy
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import org.alter.api.ChatMessageType
import org.alter.api.CommonClientScripts
import org.alter.api.ext.boolVarBit
import org.alter.api.ext.message
import org.alter.api.ext.runClientScript
import org.alter.api.ext.syncVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onEngineLogin

class LoginEvent : PluginEvent() {

    private var Player.chatboxUnlocked: Boolean by boolVarBit("varbits.has_displayname_transmitter")
    private var Player.hideRoofs by boolVarBit("varbits.option_hide_rooftops")

    override fun init() {
        onEngineLogin { engineLogin(player) }
    }

    private fun engineLogin(player: Player) {
        sendHighPriority(player)
        sendLowPriority(player)
    }

    private fun sendHighPriority(player: Player) {
        sendChatFilters(player)
        sendOpVisibility(player)
        player.message("Welcome to ${world.gameContext.name}.", ChatMessageType.WELCOME)
        sendVars(player)
    }

    private fun sendChatFilters(player: Player) {
        player.write(ChatFilterSettings(0, 0))
    }

    private fun sendOpVisibility(player: Player) {
        player.write(HideNpcOps(false))
        player.write(HideLocOps(false))
        player.write(HideObjOps(false))
    }

    private fun sendLowPriority(player: Player) {
        player.startInvTransmit(player.inventory)
        player.startInvTransmit(player.equipment)

        player.runClientScript(CommonClientScripts.ACCOUNT_INFO_UPDATE, 1, 0, 0)
        player.write(CamReset)
        player.runClientScript(CommonClientScripts.CAMERA)
        player.runClientScript(CommonClientScripts.MEMBERS, 1)
        player.runClientScript(CommonClientScripts.ORBS_WORLDMAP_KEY)
        sendPlayerOps(player)
        player.runClientScript(CommonClientScripts.PLUGINS, 0, 0, player.username, "REGULAR")

        player.write(UpdateRunEnergy(player.runEnergy.toInt()))
        player.write(ResetAnims)
        player.write(MinimapToggle(0))

    }


    private fun sendPlayerOps(player: Player) {
        setPlayerOp(player, slot = 2, op = null)
        setPlayerOp(player, slot = 3, op = "Follow")
        setPlayerOp(player, slot = 4, op = "Trade with")
        setPlayerOp(player, slot = 5, op = null)
        setPlayerOp(player, slot = 8, op = "Report")
    }

    fun setPlayerOp(player: Player, slot: Int, op: String?, priority: Boolean = false) {
        player.write(SetPlayerOp(slot, priority, op))
    }

    private fun sendVars(player: Player) {
        player.write(VarpReset)
        player.chatboxUnlocked = player.username.isNotBlank()
        player.hideRoofs = true
        player.syncVarp("varp.option_attackpriority_npc")
        player.syncVarp("varp.option_attackpriority")
    }

}