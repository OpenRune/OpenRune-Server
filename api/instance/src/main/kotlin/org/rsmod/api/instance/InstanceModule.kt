package org.rsmod.api.instance

import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.module.ExtendedModule

public object InstanceModule : ExtendedModule() {
    override fun bind() {
        bindInstance<InstanceManager>()
        bindInstance<InstanceRepository>()
        addSetBinding<PlayerPostTickHook>(InstanceScript::class.java)
    }
}
