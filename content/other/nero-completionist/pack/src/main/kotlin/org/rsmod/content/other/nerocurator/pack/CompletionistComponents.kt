package org.rsmod.content.other.nerocurator.pack

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.filesystem.Cache
import java.nio.file.Files
import java.nio.file.Path

/** Keep the bounded pack route on the same native component metadata as a full cache build. */
fun refreshCompletionistComponents(root: Path, cache: Cache) {
    val interfaces = GameValHandler.readGameVal(GameValGroupTypes.IFTYPES, cache, 240)
    val custom = interfaces.mapNotNull { it.elementAs<Interface>() }
        .single { it.name == "nero_cape_customiser" }
    require(custom.components.size == 84)
    val path = root.resolve(".data/gamevals/component.rscm")
    val previous = if (Files.isRegularFile(path)) Files.readAllLines(path) else emptyList()
    val lines = previous.filterNot { it.startsWith("nero_cape_customiser:") } +
        custom.components.map { "${custom.name}:${it.name}=${it.packed}" }
    Files.write(path, lines)
}
