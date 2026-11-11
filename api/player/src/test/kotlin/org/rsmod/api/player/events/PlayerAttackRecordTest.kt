package org.rsmod.api.player.events

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PlayerAttackRecordTest {
    private val events = EventBus()
    private val player = Player()
    private val goblin = npc()
    private val imp = npc()
    private val results = mutableListOf<PlayerAttackEvents.AttackResult>()

    private fun subscribe() {
        events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) { results += this }
    }

    private fun begin() =
        events.beginAttackRecord(player, goblin, AttackKind.Melee, weapon = null, spell = null)

    @Test
    fun `no subscriber means no record`() {
        begin()

        assertNull(player.attackRecord)
    }

    @Test
    fun `an attack that did nothing publishes nothing, such as waiting on the delay`() {
        subscribe()
        begin()

        events.finishAttackRecord(player)

        assertTrue(results.isEmpty())
        assertNull(player.attackRecord)
    }

    @Test
    fun `a miss and a hit are both reported`() {
        subscribe()
        begin()
        player.attackRecord!!.addAccuracy(goblin, HitType.Melee, success = false)
        player.attackRecord!!.addAccuracy(goblin, HitType.Melee, success = true)
        player.attackRecord!!.addDamage(goblin, HitType.Melee, maxHit = 12, damage = 7)

        events.finishAttackRecord(player)

        val rolls = results.single().rolls
        assertEquals(
            listOf(false to 0, true to 7),
            rolls.map { it.success to it.damage },
        )
        assertEquals(12, rolls[1].maxHit)
    }

    @Test
    fun `each victim of a multi-target attack gets its own roll`() {
        subscribe()
        begin()
        val record = player.attackRecord!!
        record.addAccuracy(goblin, HitType.Magic, success = true)
        record.addAccuracy(imp, HitType.Magic, success = true)
        record.addDamage(goblin, HitType.Magic, maxHit = 30, damage = 20)
        record.addDamage(imp, HitType.Magic, maxHit = 30, damage = 25)

        events.finishAttackRecord(player)

        val rolls = results.single().rolls
        assertEquals(2, rolls.size)
        assertSame(goblin, rolls[0].target)
        assertEquals(20, rolls[0].damage)
        assertSame(imp, rolls[1].target)
        assertEquals(25, rolls[1].damage)
    }

    @Test
    fun `a damage roll with no accuracy roll still counts as a landed roll`() {
        subscribe()
        begin()

        player.attackRecord!!.addDamage(goblin, HitType.Magic, maxHit = 10, damage = 4)
        events.finishAttackRecord(player)

        val roll = results.single().rolls.single()
        assertTrue(roll.success)
        assertEquals(4, roll.damage)
    }

    @Test
    fun `several accuracy rolls then one damage roll fill the latest one`() {
        subscribe()
        begin()
        val record = player.attackRecord!!
        record.addAccuracy(goblin, HitType.Melee, success = true)
        record.addAccuracy(goblin, HitType.Melee, success = true)
        record.addDamage(goblin, HitType.Melee, maxHit = 40, damage = 40)

        events.finishAttackRecord(player)

        assertEquals(listOf(0, 40), results.single().rolls.map { it.damage })
    }

    @Test
    fun `a special with no rolls still publishes`() {
        subscribe()
        begin()

        player.attackRecord!!.special = true
        events.finishAttackRecord(player)

        assertTrue(results.single().special)
    }

    @Test
    fun `resources and final delay are reported`() {
        subscribe()
        begin()
        val record = player.attackRecord!!
        record.cycles = 4
        record.addResource(ResourceKind.Ammo, item = null, count = 1, saved = true)

        events.finishAttackRecord(player)

        val result = results.single()
        assertEquals(4, result.cycles)
        assertTrue(result.resources.single().saved)
        assertFalse(result.special)
    }

    @Test
    fun `the record is gone before handlers run so an extra attack is not recorded`() {
        var seenByHandler: PlayerAttackRecord? = player.attackRecord
        var ran = false
        events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) {
            ran = true
            seenByHandler = player.attackRecord
        }
        begin()
        assertNotNull(player.attackRecord)
        player.attackRecord!!.special = true

        events.finishAttackRecord(player)

        assertTrue(ran)
        assertNull(seenByHandler)
    }

    @Test
    fun `finishing twice publishes once`() {
        subscribe()
        begin()
        player.attackRecord!!.special = true

        events.finishAttackRecord(player)
        events.finishAttackRecord(player)

        assertEquals(1, results.size)
    }

    @Test
    fun `a discarded record publishes nothing`() {
        subscribe()
        begin()
        player.attackRecord!!.special = true

        player.discardAttackRecord()
        events.finishAttackRecord(player)

        assertTrue(results.isEmpty())
    }

    private fun npc(): Npc =
        Npc(NpcServerType(id = 1, name = "Test", size = 1, hitpoints = 100), CoordGrid.ZERO)
}
