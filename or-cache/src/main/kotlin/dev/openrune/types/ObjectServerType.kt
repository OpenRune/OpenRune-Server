package dev.openrune.types

import dev.openrune.ParamMap
import dev.openrune.TypedParamType
import dev.openrune.definition.Definition
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.type.ParamType
import dev.openrune.resolve
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.seralizer.ParamSerializer
import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField

@RsTableHeaders("object")
data class ObjectServerType(
    override var id: Int = -1,
    var width: Int = 1,
    var length: Int = 1,
    var forceApproachFlags: Int = 0,
    var blockWalk: Int = 2,
    var blockRange: Boolean = true,
    var breakRouteFinding: Boolean = false,
    var contentGroup: Int = -1,
    @param:TomlField(["params"],serializer = ParamSerializer::class)
    var paramsRaw: MutableMap<Int, Any>? = null,
    var name: String = "",
    var actions: EntityOpsDefinition = EntityOpsDefinition(),
    var multiVarBit: Int = -1,
    var multiDefault: Int = -1,
    var multiVarp: Int = -1,
    var transforms: MutableList<Int>? = null,
    var category: Int = -1,
    var desc: String = "",
) : Definition {

    var paramMap: ParamMap? = null

    val multiLoc: IntArray
        get() = transforms?.toIntArray() ?: intArrayOf()


    val internalName: String
        get() = RSCM.getReverseMapping(RSCMType.LOC, id)

    public fun <T : Any> param(type: ParamType): T = paramMap.resolve(type)

    public fun <T : Any> param(type: TypedParamType<T>): T = paramMap.resolve(type)

    public fun <T : Any> paramOrNull(type: ParamType): T? = paramMap?.getOrNull(type)

    public fun <T : Any> paramOrNull(type: TypedParamType<T>): T? = paramMap?.getOrNull(type)

    public fun hasParam(type: ParamType): Boolean = paramMap?.contains(type) == true

    public fun hasParam(type: TypedParamType<*>): Boolean = paramOrNull(type) != null

    public fun isContentType(content: String): Boolean {
        return contentGroup == content.asRSCM(RSCMType.CONTENT)
    }

}
