package org.rsmod.api.player.ui

import java.util.concurrent.ConcurrentHashMap
import net.rsprot.protocol.game.incoming.buttons.IfScriptTrigger

public object IfScriptParameterRegistry {
    private val typesByComponent = ConcurrentHashMap<Int, IfScriptTrigger.ParameterTypes>()

    public fun register(componentPacked: Int, vararg parameterType: Char) {
        typesByComponent[componentPacked] = IfScriptTrigger.ParameterTypes.of(*parameterType)
    }

    public fun registerNone(componentPacked: Int) {
        typesByComponent[componentPacked] = IfScriptTrigger.ParameterTypes.NONE
    }

    public operator fun get(componentPacked: Int): IfScriptTrigger.ParameterTypes =
        typesByComponent[componentPacked] ?: IfScriptTrigger.ParameterTypes.NONE
}

public object IfScriptArgType {
    public const val INT_ARRAY: Char = IfScriptTrigger.ParameterTypes.INT_ARRAY
    public const val STRING_ARRAY: Char = IfScriptTrigger.ParameterTypes.STRING_ARRAY
    public const val STRING: Char = IfScriptTrigger.ParameterTypes.STRING
    public const val INT: Char = IfScriptTrigger.ParameterTypes.INT
    public const val NULL: Char = IfScriptTrigger.ParameterTypes.NULL
}
