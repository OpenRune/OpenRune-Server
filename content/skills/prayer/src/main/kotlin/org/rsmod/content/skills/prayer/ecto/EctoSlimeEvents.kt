package org.rsmod.content.skills.prayer.ecto

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EctoSlimeEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs("queue.prayer_ecto_collect_slime") { processSlimeTick(it.args) }
        onOpLocU("loc.ahoy_new_green_floor", "obj.bucket_empty") { startCollectSlime(it.vis) }
    }

    private fun ProtectedAccess.startCollectSlime(pool: BoundLocInfo) {
        if (collectSlime().not()) {
            return
        }
        if (inv.contains("obj.bucket_empty")) {
            weakQueue("queue.prayer_ecto_collect_slime", 2, SlimeTask(pool))
        }
    }

    private fun ProtectedAccess.processSlimeTick(task: SlimeTask) {
        if (!isWithinDistance(task.pool, 1)) {
            return
        }
        if (collectSlime().not()) {
            return
        }
        if (inv.contains("obj.bucket_empty")) {
            weakQueue("queue.prayer_ecto_collect_slime", 2, task)
        }
    }

    private fun ProtectedAccess.collectSlime(): Boolean {
        anim("seq.human_openchest")
        if (invDel(inv, "obj.bucket_empty", 1).failure) {
            return false
        }
        if (invAdd(inv, "obj.bucket_ectoplasm", 1).failure) {
            invAdd(inv, "obj.bucket_empty", 1)
            mes("You don't have enough inventory space.")
            return false
        }
        mes("You fill the bucket from the pool of slime.")
        return true
    }

    private data class SlimeTask(val pool: BoundLocInfo)
}
