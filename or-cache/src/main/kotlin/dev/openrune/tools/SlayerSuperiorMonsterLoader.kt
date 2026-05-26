package dev.openrune.tools

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.rsconfig.decodeRuneScape
import dev.openrune.toml.rsconfig.decodeRuneScapeBlocks
import dev.openrune.types.SlayerSuperiorMonster
import java.io.File
import kotlin.reflect.typeOf

object SlayerSuperiorMonsterLoader {
    private const val FILE_NAME = "slayer_superior.toml"

    data class SuperiorNpcParam(
        val superiorNpcId: Int,
        val wildernessAvailable: Boolean,
    )

    fun loadNpcSuperiorParams(directory: File, mapper: TomlMapper): Map<Int, SuperiorNpcParam> {
        val file = File(directory, FILE_NAME)
        if (!file.exists()) return emptyMap()

        val grouped = mutableMapOf<Int, MutableList<SuperiorNpcParam>>()
        val blocks = mapper.decodeRuneScapeBlocks(file.toPath())
        for (block in blocks) {
            if (block.name != "slayer_superior") continue
            val entry = mapper.decodeRuneScape(typeOf<SlayerSuperiorMonster>(), block.map.properties) as SlayerSuperiorMonster
            if (entry.superiorNpc <= 0) continue
            val param = SuperiorNpcParam(
                superiorNpcId = entry.superiorNpc,
                wildernessAvailable = entry.wildernessAvailable
            )
            for (npcRef in entry.normalNpcs) {
                grouped.getOrPut(npcRef) { mutableListOf() }.add(param)
            }
        }
        return grouped.mapValues { (_, options) -> options.random() }
    }
}
