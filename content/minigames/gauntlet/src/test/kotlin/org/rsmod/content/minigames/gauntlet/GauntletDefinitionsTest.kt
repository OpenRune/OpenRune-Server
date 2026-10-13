package org.rsmod.content.minigames.gauntlet

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.varp.VarpLifetime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.FoodRow
import org.rsmod.api.table.GauntletRoomSlotsRow
import org.rsmod.api.table.PotionRow
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.content.minigames.gauntlet.layout.MonsterKind

@ResourceLock("ServerCacheManager")
class GauntletDefinitionsTest {
    @Test fun `native Hunllef models retain mode hitpoints and usable attack options`() {
        for (corrupted in listOf(false, true)) for (style in listOf("melee", "ranged", "magic")) {
            val name = "npc.crystal_hunllef_$style${if (corrupted) "_hm" else ""}"
            val npc = ServerCacheManager.getNpc(name.asRSCM())!!
            assertEquals(if (corrupted) 1000 else 600, npc.hitpoints, name)
            assertEquals(5, npc.size, name)
            assertTrue((0..4).any { npc.actions.getOpOrNull(it).equals("Attack", true) }, name)
        }
    }

    @Test fun `all preparation monsters and animations resolve in revision 240`() {
        for (kind in MonsterKind.entries) for (suffix in listOf("", "_hm")) {
            val type = ServerCacheManager.getNpc("npc.crystal_${kind.locName}$suffix".asRSCM())!!
            assertTrue(type.hitpoints > 0)
            for (param in listOf("param.attack_anim", "param.death_anim")) {
                val anim = type.paramsRaw?.get(param.asRSCM()) as? Int ?: continue
                assertTrue(ServerCacheManager.getAnim(anim)!!.tickDuration > 0)
            }
        }
        for (seq in listOf("hunllef_attack_ranged", "hunllef_attack_melee", "hunllef_attack_special", "hunllef_death_part_a", "hunllef_death_part_b")) {
            assertTrue(ServerCacheManager.getAnim("seq.$seq".asRSCM())!!.tickDuration > 0)
        }
    }

    @Test fun `all twelve room templates contain native resource coordinates`() {
        val rows = GauntletRoomSlotsRow.all()
        assertEquals(12, rows.size)
        assertEquals(12, rows.map { it.kind to it.variant }.distinct().size)
        for (row in rows) {
            val tiles = row.rock + row.tree + row.pond + row.herb + row.fibre
            assertTrue(tiles.isNotEmpty())
            assertTrue(tiles.all { it ushr 8 in 0..15 && it and 255 in 0..15 })
        }
    }

    @Test fun `food cooking and potion rows are registered for native consumers`() {
        val fish = FoodRow.all().single { row -> row.items.any { it.id == "obj.gauntlet_food".asRSCM() } }
        assertEquals(20, fish.heal)
        val cooking = CookingFoodsRow.all().single { it.input.id == "obj.gauntlet_raw_food".asRSCM() }
        assertEquals(1, cooking.cookTicks)
        val potion = PotionRow.all().single { it.key == "egniol_potion" }
        assertEquals(4, potion.items.size)
        assertEquals("gauntlet", potion.minigameOnly)
    }

    @Test fun `escrow and pending rewards use permanent native vars`() {
        for (varp in listOf("gauntlet_flags", "gauntlet_reward_tier")) {
            assertEquals(VarpLifetime.Perm, ServerCacheManager.getVarp("varp.$varp".asRSCM())!!.scope)
        }
        assertEquals(VarpLifetime.Temp, ServerCacheManager.getVarp("varp.gauntlet_points".asRSCM())!!.scope)
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
