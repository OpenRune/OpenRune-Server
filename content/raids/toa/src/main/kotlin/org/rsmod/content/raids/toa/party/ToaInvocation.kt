package org.rsmod.content.raids.toa.party

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.StructType
import dev.openrune.types.enums.enum

/**
 * The category groupings for TOA invocations.
 *
 * Cache struct param 1161 stores these starting at value 3,
 * so ATTEMPTS = 3, TIME_LIMIT = 4, etc.
 */
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

/**
 * A single TOA invocation, resolved from a cache struct.
 *
 * Each struct contains:
 *  - param 1159 → index (bit position in the invocation bitmaps)
 *  - param 1161 → category (offset by 3 into [ToaInvocationCategory])
 *  - param 1162 → level modifier (raid level added when active)
 */
data class ToaInvocation(
    val structId: Int,
    val name: String,
    val index: Int,
    val category: ToaInvocationCategory,
    val levelModifier: Int,
) {
    companion object {
        /** Cache enum that maps int keys → invocation struct IDs. */
        private const val INVOCATION_ENUM_ID = 4664

        private const val PARAM_INDEX = 1159
        private const val PARAM_NAME = 1160
        private const val PARAM_CATEGORY = 1161
        private const val PARAM_LEVEL_MODIFIER = 1162

        /** Cache category param values start at 3, not 0. */
        private const val CATEGORY_OFFSET = 3

        /** All invocations, loaded once from the cache. */
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
                ?: error("Struct ${struct.id} has unknown category $categoryId (ordinal $categoryOrdinal)")

            val levelModifier = params[PARAM_LEVEL_MODIFIER] as? Int
                ?: error("Struct ${struct.id} missing param $PARAM_LEVEL_MODIFIER")

            return ToaInvocation(
                structId = struct.id,
                name = name,
                index = index,
                category = category,
                levelModifier = levelModifier,
            )
        }
    }
}
