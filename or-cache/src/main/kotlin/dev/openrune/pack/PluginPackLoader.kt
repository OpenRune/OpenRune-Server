package dev.openrune.pack

import io.github.classgraph.ClassGraph

object PluginPackLoader {
    private val packages = arrayOf("dev.openrune.pack", "org.rsmod.content")

    fun load(): List<PluginPack> {
        val packs = mutableListOf<PluginPack>()
        ClassGraph()
            .ignoreClassVisibility()
            .enableClassInfo()
            .disableNestedJarScanning()
            .disableModuleScanning()
            .acceptPackages(*packages)
            .scan()
            .use { result ->
                result.getSubclasses(PluginPack::class.java).directOnly().forEach { info ->
                    val clazz = info.loadClass(PluginPack::class.java)
                    val ctor = clazz.getConstructor()
                    packs += ctor.newInstance()
                }
            }
        return packs
    }
}
