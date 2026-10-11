package org.rsmod.api.obj.plugin

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.inv.storage.ItemStorageSource
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.hook.PlayerObjTakeStorageHook
import org.rsmod.api.player.hook.PlayerObjTakeValidator
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.advanced.onDefaultOpObj3
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ObjTakePlugin
@Inject
constructor(
    private val repo: ObjRepository,
    private val takeValidator: PlayerObjTakeValidator,
    private val storageHooks: Set<@JvmSuppressWildcards PlayerObjTakeStorageHook>,
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
        if (!player.hasInvSpace(obj)) {
            player.mes(Constants.dm_take_invspace)
            return
        }
        player.resetAnim()
        if (player.coords != obj.coords) {
            takeFar(obj)
        } else {
            soundSynth("synth.pick2")
            player.takeClose(obj)
        }
    }

    private suspend fun ProtectedAccess.takeFar(obj: Obj) {
        delay(1)
        if (player.pendingLogout || player.loggingOut || player.hitpoints == 0) return
        anim("seq.human_pickuptable")
        soundSynth("synth.pick2")
        player.takeClose(obj)
    }

    private fun Player.takeClose(obj: Obj) {
        if (pendingLogout || loggingOut || hitpoints == 0) return
        val type = ServerCacheManager.getItem(obj.type) ?: return
        val denial = takeValidator.validate(this, obj, type)
        if (denial != null) { mes(denial); return }
        val hook = storageHooks.firstOrNull { it.inventory(this, obj, type) != null }
        val inventory = hook?.inventory(this, obj, type) ?: inv
        val take = invAdd(inventory, RSCM.getReverseMapping(RSCMType.OBJ, obj.type), obj.count, vars = obj.vars, autoCommit = false, incomingSource = ItemStorageSource.GroundPickup)
        // Recheck after the pickup delay. Removing the ground item first loses it if space changed.
        if (take.failure) { mes(Constants.dm_take_invspace); return }
        val removed = repo.del(obj)
        if (!removed) {
            mes(Constants.dm_take_taken)
            return
        }
        take.commitAll()
        hook?.committed(this, obj, type)
    }

    private fun Player.hasInvSpace(obj: Obj): Boolean {
        val type = ServerCacheManager.getItem(obj.type) ?: return false
        val inventory = storageHooks.firstNotNullOfOrNull { it.inventory(this, obj, type) } ?: inv
        return invAdd(inventory, RSCM.getReverseMapping(RSCMType.OBJ, obj.type), obj.count, vars = obj.vars, autoCommit = false, incomingSource = ItemStorageSource.GroundPickup).success
    }
}
