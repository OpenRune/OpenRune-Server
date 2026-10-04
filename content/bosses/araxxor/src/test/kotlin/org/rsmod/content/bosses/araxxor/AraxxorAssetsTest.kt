package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.definition.codec.SpotAnimCodec
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("ServerCacheManager")
class AraxxorAssetsTest {
    @Test fun `native boss egg spider and graphic assets match encounter assumptions`() {
        val cache = ServerCacheManager.init(240)
        try {
            val boss = checkNotNull(ServerCacheManager.getNpc(AraxxorAssets.BOSS.asRSCM()))
            assertEquals(AraxxorCycle.MAX_HP, boss.hitpoints)
            assertEquals(7, boss.size)
            assertNotNull(ServerCacheManager.getNpc(AraxxorAssets.CORPSE.asRSCM()))
            for (kind in AraxyteKind.entries) {
                assertEquals(65, ServerCacheManager.getNpc(kind.egg.asRSCM())!!.hitpoints)
                assertEquals(58, ServerCacheManager.getNpc(kind.spider.asRSCM())!!.hitpoints)
            }
            for (seq in AraxxorAssets.sequences) assertTrue(seq.asRSCM(RSCMType.SEQ) >= 0)
            val codec = SpotAnimCodec(240)
            for (spot in AraxxorAssets.spots) {
                val id = spot.asRSCM(RSCMType.SPOTANIM)
                val definition = codec.loadData(id, checkNotNull(cache.data(2, 13, id, null)))
                assertTrue(definition.modelId >= 0)
                assertFalse(definition.animationId in AraxxorAssets.sequences.map { it.asRSCM(RSCMType.SEQ) })
            }
            assertTrue(AraxxorAssets.ACID.asRSCM(RSCMType.LOC) >= 0)
        } finally { cache.close() }
    }
}
