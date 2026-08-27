package org.rsmod.content.other.spawn.pack

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.pack.PluginPack

class SpawnPluginPack : PluginPack() {
    override fun interfaces(): List<InterfaceType> = listOf(buildSpawnMenuInterface())
}
