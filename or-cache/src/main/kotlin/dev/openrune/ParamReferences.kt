package dev.openrune

import dev.openrune.rscm.RSCM.asRSCM
import kotlin.reflect.KClass

public object ParamReferences {

    public inline fun <reified T : Any> param(internal: String): TypedParamType<T> =
        param(T::class, internal)

    public fun <T : Any> param(type: KClass<T>, internal: String): TypedParamType<T> {
        val id = "param.${internal}".asRSCM()
        return ParamTypes.byId(id, type)
    }
}
