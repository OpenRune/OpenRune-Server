package dev.openrune.util

import dev.openrune.ParamMap
import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.impl.definitionOpcodeParams
import dev.openrune.toParamMap
import kotlin.reflect.KMutableProperty1

private fun toParamMap(param: Map<Int, Any?>): ParamMap {
    val cleaned = param
        .filterValues { it != null }
        .mapValues { it.value as Any }

    return ParamMap(cleaned)
}

fun <T> DefinitionOpcodeParamMap(
    opcode: Int,
    property: KMutableProperty1<T, MutableMap<Int, Any>?>,
    property2: KMutableProperty1<T, ParamMap?>
): DefinitionOpcode<T> = definitionOpcodeParams(
    opcode,
    { def -> property.get(def) },
    { def, map ->
        val nonNullMap = map
            ?.filterValues { it != null }
            ?.mapValues { it.value as Any }
            ?.toMutableMap()

        property.set(def, nonNullMap)
        property2.set(def, nonNullMap?.toParamMap())
    }
)
