package dev.openrune.tools

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.rsconfig.decodeRuneScape
import dev.openrune.toml.rsconfig.decodeRuneScapeBlocks
import dev.openrune.types.SlayerNpcData
import dev.openrune.types.SlayerTargetMonster
import dev.openrune.types.SlayerTaskKey
import java.io.File
import kotlin.reflect.typeOf

object SlayerTargetMonsterLoader {
    private const val FILE_NAME = "slayer_target_monsters.toml"

    fun loadNpcSlayerData(directory: File, mapper: TomlMapper): Map<Int, SlayerNpcData> {
        val file = File(directory, FILE_NAME)
        if (!file.exists()) return emptyMap()

        val entries = mutableListOf<SlayerTargetMonster>()
        val blocks = mapper.decodeRuneScapeBlocks(file.toPath())
        for (block in blocks) {
            if (block.name != "slayer_target_monster") continue
            entries += mapper.decodeRuneScape(typeOf<SlayerTargetMonster>(), block.map.properties) as SlayerTargetMonster
        }
        return collect(entries)
    }

    fun collect(entries: List<SlayerTargetMonster>): Map<Int, SlayerNpcData> {
        val keysByNpc = LinkedHashMap<Int, MutableList<Int>>()
        val variantByNpc = HashMap<Int, Int>()
        val levelByNpc = HashMap<Int, Int>()
        for (entry in entries) {
            val key = SlayerTaskKey.of(entry.targetId, entry.bossId)
            for (npcRef in entry.targets) {
                val keys = keysByNpc.getOrPut(npcRef) { mutableListOf() }
                if (key > 0 && key !in keys) keys += key
                if (entry.variant != 0) variantByNpc[npcRef] = entry.variant
                if (entry.level >= 0) levelByNpc[npcRef] = entry.level
            }
        }
        return keysByNpc.mapValues { (npc, keys) ->
            SlayerNpcData(keys, variantByNpc[npc] ?: 0, levelByNpc[npc] ?: -1)
        }
    }
}
