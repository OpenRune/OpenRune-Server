package org.rsmod.content.skills.crafting.items

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BucketOfSandScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld4("obj.bucket_sand") { emptyBucket() }
    }

    private fun ProtectedAccess.emptyBucket() {
        invReplace(inv, "obj.bucket_sand", 1, "obj.bucket_empty")
    }
}
