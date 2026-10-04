package org.rsmod.content.raids.toa.party

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.StructType
import dev.openrune.types.enums.enum

enum class ToaInvocationCategory {
    ATTEMPTS,
    TIME_LIMIT,
    HELPFUL_SPIRIT,
    PATH_LEVEL,
    PRAYER,
    RESTORATION,
    PATHS,
    AKKHA,
    KEPHRI,
    ZEBAK,
    BA_BA,
    THE_WARDENS,
    BLAZING_TOMBS,
}

data class ToaInvocation(
    val structId: Int,
    val name: String,
    val index: Int,
    val category: ToaInvocationCategory,
    val levelModifier: Int,
    val prerequisiteStructId: Int?,
) {

    val prerequisite: ToaInvocation?
        get() = prerequisiteStructId?.let { id -> ALL.firstOrNull { it.structId == id } }

    val dependents: List<ToaInvocation>
        get() = ALL.filter { it.prerequisiteStructId == structId }

    val eventOnly: Boolean
        get() = category == ToaInvocationCategory.BLAZING_TOMBS
    companion object {
        private const val INVOCATION_ENUM_ID = 4664
        private const val PARAM_INDEX = 1159
        private const val PARAM_NAME = 1160
        private const val PARAM_CATEGORY = 1161
        private const val PARAM_LEVEL_MODIFIER = 1162
        private const val PARAM_PREREQUISITE = 1346
        private const val CATEGORY_OFFSET = 3

        val ALL: List<ToaInvocation> by lazy { loadAll() }

        private fun loadAll(): List<ToaInvocation> {
            val invocationEnum = enum<Int, Int>(INVOCATION_ENUM_ID)
            return invocationEnum.backing.entries
                .sortedBy { it.key }
                .mapNotNull { (_, structId) ->
                    structId?.let { ServerCacheManager.getStruct(it) }?.let(::fromStruct)
                }
        }

        private fun fromStruct(struct: StructType): ToaInvocation {
            val params = struct.params
                ?: error("Struct ${struct.id} has no params")

            val index = params[PARAM_INDEX] as? Int
                ?: error("Struct ${struct.id} missing param $PARAM_INDEX")

            val name = params[PARAM_NAME] as? String
                ?: "Unknown"

            val categoryId = params[PARAM_CATEGORY] as? Int
                ?: error("Struct ${struct.id} missing param $PARAM_CATEGORY")

            val categoryOrdinal = categoryId - CATEGORY_OFFSET
            val category = ToaInvocationCategory.entries.getOrNull(categoryOrdinal)
                ?: error(
                    "Struct ${struct.id} has unknown category $categoryId " +
                        "(ordinal $categoryOrdinal)"
                )

            val levelModifier = params[PARAM_LEVEL_MODIFIER] as? Int
                ?: error("Struct ${struct.id} missing param $PARAM_LEVEL_MODIFIER")

            val prerequisiteStructId = params[PARAM_PREREQUISITE] as? Int

            return ToaInvocation(
                structId = struct.id,
                name = name,
                index = index,
                category = category,
                levelModifier = levelModifier,
                prerequisiteStructId = prerequisiteStructId,
            )
        }
    }
}
