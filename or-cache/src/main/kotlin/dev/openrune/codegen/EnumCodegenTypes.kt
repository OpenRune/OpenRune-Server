package dev.openrune.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM

/**
 * When several row/column enum ids map to definitions, require a single (keyType, valueType)
 * shape (or fall back to [templateId]'s definition).
 */
internal fun unifiedEnumKeyValueTypes(
    enumIds: List<Int>,
    enums: Map<Int, EnumType>,
    templateId: Int?,
): Pair<VarType, VarType>? {
    val fromRows =
        enumIds
            .mapNotNull { id -> enums[id]?.let { it.keyType to it.valueType } }
            .distinct()
            .singleOrNull()
    return fromRows ?: templateId?.let { id -> enums[id]?.let { it.keyType to it.valueType } }
}

/**
 * Kotlin [TypeName] pair for [EnumType] map entries, including nested
 * `EnumTypeMap<…, EnumTypeMap<…, …>>` when key or value slot is [VarType.ENUM] (references another
 * enum by id / `enum.*` RSCM string in [EnumType.values]).
 */
internal fun kotlinTypePairForEnumDefinition(
    enumDef: EnumType,
    enums: Map<Int, EnumType>,
    enumTypeMapClass: ClassName,
): Pair<TypeName, TypeName> {
    val k = kotlinTypeForEnumSlot(enumDef.keyType, enumDef, enums, enumTypeMapClass, keySide = true)
    val v = kotlinTypeForEnumSlot(enumDef.valueType, enumDef, enums, enumTypeMapClass, keySide = false)
    return k to v
}

private fun kotlinTypeForEnumSlot(
    vt: VarType,
    host: EnumType,
    enums: Map<Int, EnumType>,
    enumTypeMapClass: ClassName,
    keySide: Boolean,
): TypeName {
    if (vt != VarType.ENUM) {
        return kotlinTypeForVarType(vt, false)
    }
    val innerIds = innerEnumIdsFromHost(host, keySide)
    if (innerIds.isEmpty()) {
        return kotlinTypeForVarType(VarType.INT, false)
    }
    unifiedEnumKeyValueTypes(innerIds, enums, innerIds.firstOrNull()) ?: return kotlinTypeForVarType(VarType.INT, false)
    val repInnerId = innerIds.firstOrNull { enums.containsKey(it) } ?: return kotlinTypeForVarType(VarType.INT, false)
    val innerDef = enums.getValue(repInnerId)
    val (ik, iv) = kotlinTypePairForEnumDefinition(innerDef, enums, enumTypeMapClass)
    return enumTypeMapClass.parameterizedBy(ik, iv)
}

private fun innerEnumIdsFromHost(host: EnumType, keySide: Boolean): List<Int> {
    val seq =
        if (keySide) {
            host.values.keys.asSequence()
        } else {
            host.values.values.asSequence()
        }
    return seq.mapNotNull { rawSlotToInnerEnumId(it) }.distinct().sorted().toList()
}

private fun rawSlotToInnerEnumId(raw: Any?): Int? =
    when (raw) {
        null -> null
        is Int -> raw.takeIf { it > 0 }
        is Number -> raw.toInt().takeIf { it > 0 }
        is String -> {
            val t = raw.trim()
            when {
                t.startsWith("enum.") -> runCatching { RSCM.getRSCM(t) }.getOrNull()?.takeIf { it > 0 }
                else -> t.toIntOrNull()?.takeIf { it > 0 }
            }
        }
        else -> null
    }
