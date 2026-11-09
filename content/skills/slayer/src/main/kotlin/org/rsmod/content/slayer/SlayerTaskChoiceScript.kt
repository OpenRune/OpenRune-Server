package org.rsmod.content.slayer

import org.rsmod.api.script.onIfModalButton
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerTaskChoiceScript : PluginScript() {
    override fun ScriptContext.startup() {
        onIfModalButton(SlayerTaskChoiceInterface.CLOSE) {
            SlayerTaskChoiceInterface.onClose(this)
        }
    }
}
