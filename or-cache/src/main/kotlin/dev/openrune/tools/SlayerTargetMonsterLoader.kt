package dev.openrune.tools

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.rsconfig.decodeRuneScape
import dev.openrune.toml.rsconfig.decodeRuneScapeBlocks
import dev.openrune.types.SlayerTargetMonster
import java.io.File
import kotlin.reflect.typeOf

object SlayerTargetMonsterLoader {
    private const val FILE_NAME = "slayer_target_monsters.toml"

    fun loadNpcSlayerTaskIds(directory: File, mapper: TomlMapper): Map<Int, Int> {
        val file = File(directory, FILE_NAME)
        if (!file.exists()) return emptyMap()

        val npcToTask = mutableMapOf<Int, Int>()
        val blocks = mapper.decodeRuneScapeBlocks(file.toPath())
        for (block in blocks) {
            if (block.name != "slayer_target_monster") continue
            val entry = mapper.decodeRuneScape(typeOf<SlayerTargetMonster>(), block.map.properties) as SlayerTargetMonster
            for (npcRef in entry.targets) {
                npcToTask[npcRef] = entry.targetId
            }
        }
        return npcToTask
    }
}
