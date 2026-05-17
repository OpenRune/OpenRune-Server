package org.rsmod.api.player.ui

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

public interface IfScriptArgs

public object IfScriptArgsCodec {
    private val codecs = ConcurrentHashMap<KClass<*>, Codec<*>>()

    public fun parameterTypes(kClass: KClass<*>): CharArray = codec(kClass).parameterTypes

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> decode(kClass: KClass<T>, args: List<Any>): T = codec(kClass).decode(args) as T

    public fun registerParameterTypes(componentPacked: Int, kClass: KClass<*>) {
        IfScriptParameterRegistry.register(componentPacked, *parameterTypes(kClass))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> codec(kClass: KClass<T>): Codec<T> =
        codecs.getOrPut(kClass) { buildCodec(kClass) } as Codec<T>

    private fun buildCodec(kClass: KClass<*>): Codec<*> {
        val constructor =
            kClass.primaryConstructor
                ?: error("IfScriptArgs type ${kClass.simpleName} must declare a primary constructor.")
        val parameterTypes = constructor.parameters.map(::parameterWireType).toCharArray()
        return Codec(parameterTypes) { args -> decodeInstance(constructor, args) }
    }

    private fun parameterWireType(parameter: KParameter): Char {
        parameter.findAnnotation<IfScriptParam>()?.type?.let { return it }
        return wireType(parameter.type, parameter.name ?: "parameter")
    }

    private fun wireType(type: KType, name: String): Char =
        when (type.classifier) {
            Int::class -> IfScriptArgType.INT
            String::class -> IfScriptArgType.STRING
            IntArray::class -> IfScriptArgType.INT_ARRAY
            Array::class -> {
                val element = type.arguments.singleOrNull()?.type
                when (element?.classifier) {
                    String::class -> IfScriptArgType.STRING_ARRAY
                    else ->
                        error(
                            "Unsupported IfScriptArgs array element type for `$name`: " +
                                "${classifierName(element?.classifier) ?: type}."
                        )
                }
            }
            else ->
                error(
                    "Unsupported IfScriptArgs parameter type for `$name`: ${classifierName(type.classifier)}. " +
                        "Use Int, String, IntArray, Array<String>, or @IfScriptParam."
                )
        }

    private fun decodeInstance(constructor: KFunction<*>, args: List<Any>): Any {
        val parameters = constructor.parameters
        check(args.size == parameters.size) {
            "Expected ${parameters.size} IfScriptTrigger args for ${constructor.returnType.classifier}, got ${args.size}."
        }
        val values =
            parameters.mapIndexed { index, parameter ->
                castArg(args[index], parameter.type, parameter.name ?: "parameter$index")
            }
        return checkNotNull(constructor.call(*values.toTypedArray())) {
            "IfScriptArgs constructor ${constructor.returnType.classifier} returned null."
        }
    }

    private fun castArg(value: Any, type: KType, name: String): Any =
        when (type.classifier) {
            Int::class -> value as Int
            String::class -> value as String
            IntArray::class -> value as IntArray
            Array::class -> {
                val element = type.arguments.singleOrNull()?.type?.classifier
                when (element) {
                    String::class -> value as Array<String>
                    else -> error("Unsupported IfScriptArgs array element type for `$name`.")
                }
            }
            else -> error("Unsupported IfScriptArgs parameter type for `$name`.")
        }

    private fun classifierName(classifier: kotlin.reflect.KClassifier?): String? =
        (classifier as? KClass<*>)?.simpleName

    private class Codec<T : Any>(
        val parameterTypes: CharArray,
        private val decode: (List<Any>) -> T,
    ) {
        fun decode(args: List<Any>): T = decode.invoke(args)
    }
}
