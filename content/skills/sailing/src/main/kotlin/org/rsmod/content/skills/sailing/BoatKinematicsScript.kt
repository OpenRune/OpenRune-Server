package org.rsmod.content.skills.sailing

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.script.onEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BoatKinematicsScript
@Inject
constructor(private val kinematics: BoatKinematics) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { kinematics.tick() }
    }
}
