package org.rsmod.api.obj.plugin

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.hook.PlayerObjTakeReceiver
import org.rsmod.api.player.hook.PlayerObjTakeValidator
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.advanced.onDefaultOpObj3
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.objtx.TransactionResultList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ObjTakePlugin
@Inject
constructor(
    private val repo: ObjRepository,
    private val takeValidator: PlayerObjTakeValidator,
    private val takeReceiver: PlayerObjTakeReceiver,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onDefaultOpObj3 { triggerTake(it.obj) }
    }

    private suspend fun ProtectedAccess.triggerTake(obj: Obj) {
        val type = ServerCacheManager.getItem(obj.type) ?: return
        val denial = takeValidator.validate(player, obj, type)
        if (denial != null) {
            mes(denial)
            return
        }
        if (!player.hasInvSpace(obj) && !takeReceiver.canReceive(player, type, obj.count)) {
            player.mes(Constants.dm_take_invspace)
            return
        }
        player.resetAnim()
        if (player.coords != obj.coords) {
            takeFar(obj, type)
        } else {
            soundSynth("synth.pick2")
            player.takeClose(obj, type)
        }
    }

    private suspend fun ProtectedAccess.takeFar(obj: Obj, type: ItemServerType) {
        delay(1)
        anim("seq.human_pickuptable")
        soundSynth("synth.pick2")
        player.takeClose(obj, type)
    }

    private fun Player.takeClose(obj: Obj, type: ItemServerType) {
        val received = takeReceiver.canReceive(this, type, obj.count)
        val take = if (received) null else transaction(obj)
        if (take != null && take.failure) {
            mes(Constants.dm_take_invspace)
            return
        }
        if (!repo.del(obj)) {
            mes(Constants.dm_take_taken)
            return
        }
        if (take != null) {
            take.commitAll()
        } else {
            check(takeReceiver.receive(this, type, obj.count)) { "Receiver refused $obj after accepting it." }
        }
    }

    private fun Player.hasInvSpace(obj: Obj): Boolean = transaction(obj).success

    private fun Player.transaction(obj: Obj): TransactionResultList<InvObj> =
        invAdd(inv, obj.type, obj.count, autoCommit = false)
}
