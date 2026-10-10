package org.rsmod.content.raids.toa.party

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.StructType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
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
        private const val INVOCATION_ENUM = "toa_invocations"
        private const val PARAM_INDEX = "param.toa_invocation_index"
        private const val PARAM_NAME = "param.toa_invocation_name"
        private const val PARAM_CATEGORY = "param.toa_invocation_category"
        private const val PARAM_LEVEL_MODIFIER = "param.toa_invocation_level_modifier"
        private const val PARAM_PREREQUISITE = "param.toa_invocation_prerequisite"
        private const val CATEGORY_OFFSET = 3

        val ALL: List<ToaInvocation> by lazy { loadAll() }

        private fun loadAll(): List<ToaInvocation> {
            val invocationEnum = enum<Int, Int>(INVOCATION_ENUM)
            return invocationEnum.backing.entries
                .sortedBy { it.key }
                .mapNotNull { (_, structId) ->
                    structId?.let { ServerCacheManager.getStruct(it) }?.let(::fromStruct)
                }
        }

        private fun fromStruct(struct: StructType): ToaInvocation {
            val params = struct.params
                ?: error("Struct ${struct.id} has no params")

            val index = params[PARAM_INDEX.asRSCM(RSCMType.PARAM)] as? Int
                ?: error("Struct ${struct.id} missing param $PARAM_INDEX")

            val name = params[PARAM_NAME.asRSCM(RSCMType.PARAM)] as? String
                ?: "Unknown"

            val categoryId = params[PARAM_CATEGORY.asRSCM(RSCMType.PARAM)] as? Int
                ?: error("Struct ${struct.id} missing param $PARAM_CATEGORY")

            val categoryOrdinal = categoryId - CATEGORY_OFFSET
            val category = ToaInvocationCategory.entries.getOrNull(categoryOrdinal)
                ?: error(
                    "Struct ${struct.id} has unknown category $categoryId " +
                        "(ordinal $categoryOrdinal)"
                )

            val levelModifier = params[PARAM_LEVEL_MODIFIER.asRSCM(RSCMType.PARAM)] as? Int
                ?: error("Struct ${struct.id} missing param $PARAM_LEVEL_MODIFIER")

            val prerequisiteStructId = params[PARAM_PREREQUISITE.asRSCM(RSCMType.PARAM)] as? Int

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
