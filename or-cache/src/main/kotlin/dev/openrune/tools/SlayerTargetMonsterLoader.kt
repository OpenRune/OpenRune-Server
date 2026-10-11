package dev.openrune.tools

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.rsconfig.decodeRuneScape
import dev.openrune.toml.rsconfig.decodeRuneScapeBlocks
import dev.openrune.types.SlayerTargetMonster
import dev.openrune.types.SlayerTaskKey
import java.io.File
import kotlin.reflect.typeOf

object SlayerTargetMonsterLoader {
    private const val FILE_NAME = "slayer_target_monsters.toml"

    fun loadNpcSlayerTaskKeys(directory: File, mapper: TomlMapper): Map<Int, List<Int>> {
        val file = File(directory, FILE_NAME)
        if (!file.exists()) return emptyMap()

        val entries = mutableListOf<SlayerTargetMonster>()
        val blocks = mapper.decodeRuneScapeBlocks(file.toPath())
        for (block in blocks) {
            if (block.name != "slayer_target_monster") continue
            entries += mapper.decodeRuneScape(typeOf<SlayerTargetMonster>(), block.map.properties) as SlayerTargetMonster
        }
        return collectTaskKeys(entries)
    }

    fun collectTaskKeys(entries: List<SlayerTargetMonster>): Map<Int, List<Int>> {
        val keysByNpc = LinkedHashMap<Int, MutableList<Int>>()
        for (entry in entries) {
            val key = SlayerTaskKey.of(entry.targetId, entry.bossId)
            for (npcRef in entry.targets) {
                val keys = keysByNpc.getOrPut(npcRef) { mutableListOf() }
                if (key !in keys) keys += key
            }
        }
        return keysByNpc
    }
}
