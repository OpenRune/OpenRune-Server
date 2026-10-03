package org.rsmod.content.other.special.weapons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class WeaponCacheInventoryTest {
    @Test fun `export charged weapon metadata from the installed cache`() {
        val cache = ServerCacheManager.init(240)
        try {
            val items = ServerCacheManager.getItems().values.filter { item ->
                listOf("scythe", "trident", "ayak", "blowpipe", "sanguinesti", "vial of blood").any {
                    item.name.contains(it, ignoreCase = true)
                }
            }
            assertTrue(items.isNotEmpty())
            val report = File("content/other/special-weapons/build/reports/charged-weapon-cache.txt")
            report.parentFile.mkdirs()
            File(report.parentFile, "all-item-symbols.tsv").writeText(ServerCacheManager.getItems().values.joinToString("\n") {
                "${it.id}\t${it.name}\t${RSCM.getReverseMapping(RSCMType.OBJ, it.id)}"
            })
            report.writeText(items.joinToString("\n") { RSCM.getReverseMapping(RSCMType.OBJ, it.id) + " " + it.toString() })
            val methods = File(report.parentFile, "cache-fx-symbols.txt")
            methods.writeText(listOf(RSCMType.SEQ to 20000, RSCMType.SPOTANIM to 10000, RSCMType.PROJANIM to 1000).flatMap { (type, max) ->
                (0..max).mapNotNull { id ->
                    val symbol = runCatching { RSCM.getReverseMapping(type, id) }.getOrNull()
                    symbol
                }
            }.joinToString("\n"))
        } finally { cache.close() }
    }
}
