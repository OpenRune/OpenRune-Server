package org.rsmod.content.drops.tables.monsters

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.with
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.droptable.DropTablePreview
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@ResourceLock("ServerCacheManager")
class AraxxorRewardsTest {
    @Test fun `destroy doubles pet chance and contains only pet and elite clue`() {
        val harvest = DropTablePreview.entries(araxxorDropTable)
        val destroy = DropTablePreview.entries(araxxorDestroyDropTable)
        assertEquals(setOf("obj.araxxorpet", "obj.trail_elite_emote_exp1"), destroy.mapNotNull { it.item?.obj }.toSet())
        val harvestPet = harvest.single { it.item?.obj == "obj.araxxorpet" }.baseChance
        assertEquals(harvestPet * 2, destroy.single { it.item?.obj == "obj.araxxorpet" }.baseChance)
        assertEquals(1.0 / 1500, destroy.single { it.item?.obj == "obj.araxxorpet" }.baseChance)
    }

    @Test fun `morph requires a timed sub75 second kill and is suppressed after unlocking`() {
        val player = Player()
        for (ticks in listOf(null, 0, 125, 1000)) {
            assertTrue(araxxorMorphRoll.roll(player, ArgMap(KillRollContext.elapsedTicks with ticks)) is RollResult.Nothing)
        }
        for (ticks in listOf(1, 124)) {
            assertTrue(araxxorMorphRoll.roll(player, ArgMap(KillRollContext.elapsedTicks with ticks)) is RollResult.Single)
        }
        val unlock = checkNotNull(ServerCacheManager.getVarbit("varbit.pet_nid_rax".asRSCM()))
        player.vars.backing[unlock.baseVar.id] = 1 shl unlock.bits.first
        assertTrue(araxxorMorphRoll.roll(player, ArgMap(KillRollContext.elapsedTicks with 100)) is RollResult.Nothing)
        player.vars.backing[unlock.baseVar.id] = 0
        player.invMap.getOrPut("inv.bank")[0] = InvObj("obj.araxxor_pet_morph")
        assertTrue(araxxorMorphRoll.roll(player, ArgMap(KillRollContext.elapsedTicks with 100)) is RollResult.Nothing)
    }

    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
