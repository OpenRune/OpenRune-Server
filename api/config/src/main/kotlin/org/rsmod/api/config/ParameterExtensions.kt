package org.rsmod.api.config

import dev.openrune.TypedParamType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import kotlin.reflect.KProperty
import org.rsmod.game.stat.PlayerStatMap

fun <T : Any> locParam(param: TypedParamType<T>): ParameterProperty<T> = ParameterProperty(param)

fun locXpParam(param: TypedParamType<Int>): ParameterXPProperty = ParameterXPProperty(param)

fun <T : Any> objParam(param: TypedParamType<T>): ParameterProperty<T> = ParameterProperty(param)

fun objXpParam(param: TypedParamType<Int>): ParameterXPProperty = ParameterXPProperty(param)

class ParameterProperty<T : Any>(private val param: TypedParamType<T>) {
    operator fun getValue(thisRef: ObjectServerType, property: KProperty<*>): T =
        thisRef.param(param)

    operator fun getValue(thisRef: ItemServerType, property: KProperty<*>): T = thisRef.param(param)
}

class ParameterXPProperty(private val param: TypedParamType<Int>) {
    operator fun getValue(thisRef: ObjectServerType, property: KProperty<*>): Double {
        val fineXp = thisRef.param(param)
        return fineXp / PlayerStatMap.XP_FINE_PRECISION.toDouble()
    }

    operator fun getValue(thisRef: ItemServerType, property: KProperty<*>): Double {
        val fineXp = thisRef.param(param)
        return fineXp / PlayerStatMap.XP_FINE_PRECISION.toDouble()
    }
}
