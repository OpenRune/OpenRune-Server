package org.rsmod.content.skills.crafting

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.table.crafting.CraftingHandRow
import org.rsmod.content.skills.crafting.scripts.HeldCraftingScript
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class RancourCraftingTest {
    @Test fun `packed rancour recipe consumes both ingredients and retains confirmation and animation stages`() {
        val cache = ServerCacheManager.init(240)
        try {
            val product = CraftingHandRow.all().map { it.toCraftingProduct() }
                .single { it.output == "obj.amulet_of_rancour" }
            assertEquals(CraftingMode.COMBINE, product.section.mode)
            assertEquals(86, product.level)
            assertEquals(500.0, product.xp / CraftingConstants.FINE_XP_DIVISOR)
            assertEquals(mapOf("obj.araxyte_fang" to 1, "obj.zenyte_amulet_enchanted" to 1),
                product.inputs.associate { it.internal to it.count })
            assertEquals(1, product.outputCount)
            assertEquals(0, product.maxCraftable { if (it == "obj.araxyte_fang") 1 else 0 })
            assertEquals(0, product.maxCraftable { if (it == "obj.zenyte_amulet_enchanted") 1 else 0 })
            assertEquals(1, product.maxCraftable { 1 })
            assertTrue(product.confirmTitles.isNotEmpty())
            assertTrue(product.confirmWarning!!.contains("non-reversible"))
            assertEquals(listOf("seq.human_craft_rancor_start", "seq.human_craft_rancor_end"), product.anims)
            assertEquals(listOf("spotanim.vfx_human_craft_rancor_start", "spotanim.vfx_human_craft_rancor_end"), product.spotanims)
        } finally { cache.close() }
    }

    @Test fun `native held crafting startup registers the fang torture combination`() {
        val cache = ServerCacheManager.init(240)
        try {
            val bus = EventBus()
            val context = ScriptContext(bus, mock(CheatCommandMap::class.java), mock(EngineQueueCache::class.java))
            with(HeldCraftingScript()) { context.startup() }
            val fang = "obj.araxyte_fang".asRSCM()
            val torture = "obj.zenyte_amulet_enchanted".asRSCM()
            assertTrue(bus.contains(HeldUEvents.Type::class.java, EventBus.composeLongKey(fang, torture)) ||
                bus.contains(HeldUEvents.Type::class.java, EventBus.composeLongKey(torture, fang)))
        } finally { cache.close() }
    }
}
