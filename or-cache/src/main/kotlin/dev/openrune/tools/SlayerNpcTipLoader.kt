package dev.openrune.tools

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.rsconfig.decodeRuneScape
import dev.openrune.toml.rsconfig.decodeRuneScapeBlocks
import dev.openrune.types.SlayerNpcTip
import java.io.File
import kotlin.reflect.typeOf

object SlayerNpcTipLoader {
    private const val FILE_NAME = "slayer_npc_tips.toml"

    fun loadNpcSlayerTaskTips(directory: File, mapper: TomlMapper): Map<Int, String> {
        val file = File(directory, FILE_NAME)
        if (!file.exists()) return emptyMap()

        val npcToTip = mutableMapOf<Int, String>()
        val blocks = mapper.decodeRuneScapeBlocks(file.toPath())
        for (block in blocks) {
            if (block.name != "npc_tip") continue
            val entry = mapper.decodeRuneScape(typeOf<SlayerNpcTip>(), block.map.properties) as SlayerNpcTip
            val tip = entry.tip.takeIf { it.isNotBlank() } ?: continue
            for (npcRef in entry.targets) {
                npcToTip[npcRef] = tip
            }
        }
        return npcToTip
    }
}
