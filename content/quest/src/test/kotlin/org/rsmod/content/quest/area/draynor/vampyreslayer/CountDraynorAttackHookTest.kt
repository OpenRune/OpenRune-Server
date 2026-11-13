package org.rsmod.content.quest.area.draynor.vampyreslayer

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class CountDraynorAttackHookTest {
    private val hook = CountDraynorAttackHook()

    @Test fun `only the player who opened the coffin can attack their Count`() {
        val owner = player(slot = 1)
        val other = player(slot = 2)
        val count = Npc("npc.count_draynor", CoordGrid(3078, 9774, 0))
        count.assignSpawnOwner(owner, nearCycle = 0)

        assertEquals(NpcAttackValidateResult.Pass, hook.validate(owner, count))
        val denied = hook.validate(other, count)
        assertEquals(NpcAttackValidateResult.Deny("This is not your target."), denied)
    }

    @Test fun `unowned Counts and other npcs are left alone`() {
        val player = player(slot = 1)
        val owner = player(slot = 2)
        val unowned = Npc("npc.count_draynor", CoordGrid(3078, 9774, 0))
        val otherNpc = Npc("npc.morgan", CoordGrid(3098, 3268, 0))
        otherNpc.assignSpawnOwner(owner, nearCycle = 0)

        assertEquals(NpcAttackValidateResult.Pass, hook.validate(player, unowned))
        assertEquals(NpcAttackValidateResult.Pass, hook.validate(player, otherNpc))
    }

    @OptIn(InternalApi::class)
    private fun player(slot: Int): Player =
        Player().apply {
            uuid = slot.toLong()
            slotId = slot
            assignUid()
        }

    companion object {
        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
