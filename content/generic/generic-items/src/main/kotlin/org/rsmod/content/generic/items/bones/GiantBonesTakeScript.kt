package org.rsmod.content.generic.items.bones

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.script.onOpObj4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GiantBonesTakeScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        val bones =
            ServerCacheManager.getItem(GIANT_BONES.asRSCM(RSCMType.OBJ))
                ?: error("Missing obj type: $GIANT_BONES")
        onOpObj4(bones) { mes(TOO_HEAVY_MESSAGE) }
    }

    private companion object {
        private const val GIANT_BONES = "obj.giant_bones"
        private const val TOO_HEAVY_MESSAGE = "I don't think those will fit in my backpack..."
    }
}
