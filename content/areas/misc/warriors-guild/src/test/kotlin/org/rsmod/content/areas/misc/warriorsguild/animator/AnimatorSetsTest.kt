package org.rsmod.content.areas.misc.warriorsguild.animator

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.areas.misc.warriorsguild.GuildTestFixture
import org.rsmod.content.areas.misc.warriorsguild.GuildTestFixture.fillInv
import org.rsmod.content.areas.misc.warriorsguild.GuildTestFixture.player
import org.rsmod.content.areas.misc.warriorsguild.lostArmour
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AnimatorSetsTest {
    @Test
    fun `taking a set removes exactly one of each piece and holds it`() {
        val player = player().withBlackSet()
        player.inv[3] = InvObj("obj.black_platebody", 1)
        val set = AnimatorSets.take(player)
        assertEquals(BLACK, set?.index)
        assertEquals(BLACK, player.lostArmour)
        assertEquals(0, player.inv.count("obj.black_full_helm"))
        assertEquals(1, player.inv.count("obj.black_platebody"))
        assertEquals(0, player.inv.count("obj.black_platelegs"))
    }

    @Test
    fun `an incomplete set is not taken`() {
        val player = player()
        player.inv[0] = InvObj("obj.black_full_helm", 1)
        player.inv[1] = InvObj("obj.black_platebody", 1)
        assertNull(AnimatorSets.take(player))
        assertEquals(0, player.lostArmour)
        assertEquals(1, player.inv.count("obj.black_full_helm"))
    }

    @Test
    fun `a second set cannot be taken while one is held`() {
        val player = player().withBlackSet()
        player.lostArmour = 2
        assertNull(AnimatorSets.take(player))
        assertEquals(2, player.lostArmour)
        assertEquals(1, player.inv.count("obj.black_full_helm"))
    }

    @Test
    fun `returning with too little space keeps the hold and adds nothing`() {
        val player = player()
        player.fillInv()
        player.inv[0] = null
        player.inv[1] = null
        player.lostArmour = BLACK
        assertEquals(AnimatorSets.Return.NoSpace, AnimatorSets.returnHeld(player))
        assertEquals(BLACK, player.lostArmour)
        assertEquals(0, player.armourCount())
    }

    @Test
    fun `returning gives the set back once`() {
        val player = player()
        player.lostArmour = BLACK
        assertEquals(AnimatorSets.Return.Returned, AnimatorSets.returnHeld(player))
        assertEquals(AnimatorSets.Return.NothingHeld, AnimatorSets.returnHeld(player))
        assertEquals(0, player.lostArmour)
        assertEquals(3, player.armourCount())
    }

    @Test
    fun `killing the armour releases only its own hold`() {
        val player = player()
        player.lostArmour = BLACK
        AnimatorSets.releaseHold(player, AnimatorSets.byIndex(BRONZE)!!.npc.id)
        assertEquals(BLACK, player.lostArmour)
        AnimatorSets.releaseHold(player, AnimatorSets.byIndex(BLACK)!!.npc.id)
        assertEquals(0, player.lostArmour)
    }

    private fun Player.withBlackSet(): Player = apply {
        inv[0] = InvObj("obj.black_full_helm", 1)
        inv[1] = InvObj("obj.black_platebody", 1)
        inv[2] = InvObj("obj.black_platelegs", 1)
    }

    private fun Player.armourCount(): Int =
        inv.count("obj.black_full_helm") +
            inv.count("obj.black_platebody") +
            inv.count("obj.black_platelegs")

    companion object {
        private const val BRONZE = 1
        private const val BLACK = 4

        @JvmStatic @BeforeAll fun cache() = GuildTestFixture.loadCache()

        @JvmStatic @AfterAll fun restore() = GuildTestFixture.restore()
    }
}
