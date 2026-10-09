package org.rsmod.content.generic.items.packs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import org.rsmod.api.enums.NamedEnums.item_pack_contents
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ItemPackScript @Inject constructor() : PluginScript() {

    override fun ScriptContext.startup() {
        item_pack_contents.filterValuesNotNull().forEach { (key, value) ->
            onOpHeld1(key) {
                openPack(OpenTask(key, value))
            }
        }
        onPlayerQueueWithArgs<OpenTask>("queue.item_pack_open") { openPack(it.args) }
    }

    private fun ProtectedAccess.openPack(task: OpenTask) {
        if (task.pack !in inv) {
            return
        }

        val result = player.invTransaction(inv) {
            val target = select(inv)
            delete(target, task.pack.id, count = 1)
            add(target, task.output.id, count = CONTENTS_PER_PACK, cert = task.output.canCert)
        }
        if (!result.success) {
            mes("You don't have enough inventory space to open that.")
            return
        }

        if (task.pack in inv) {
            weakQueue("queue.item_pack_open", OPEN_INTERVAL, task)
        }
    }

    private data class OpenTask(val pack: ItemServerType, val output: ItemServerType)

    private companion object {
        private const val CONTENTS_PER_PACK = 100
        private const val OPEN_INTERVAL = 2
    }
}
