package dev.openrune.map

import dev.openrune.map.npc.MapNpcDefinition
import dev.openrune.map.obj.MapObjDefinition
import org.rsmod.map.CoordGrid

public interface GameMapSpawnSink {
    public fun onNpcSpawn(def: MapNpcDefinition, coords: CoordGrid)

    public fun onObjSpawn(def: MapObjDefinition, coords: CoordGrid)
}
