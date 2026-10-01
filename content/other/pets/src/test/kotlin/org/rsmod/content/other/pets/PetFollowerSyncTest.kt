package org.rsmod.content.other.pets

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.varp.VarpLarge
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import net.rsprot.protocol.game.outgoing.varp.VarpSmall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.client.Client
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@OptIn(InternalApi::class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PetFollowerSyncTest {
    @Test
    fun `relogin with production player defaults restores the follower after the login varp reset`() {
        val fixture = Fixture()
        val oldSession = fixture.player(slot = 1, processed = 100)
        val form = Pets.all.first { it.name == "Butch" }.base
        fixture.followers.spawn(oldSession, form)
        val oldUid = checkNotNull(fixture.followers.follower(oldSession)).uid.packed

        fixture.client(oldSession).messages.clear()
        fixture.followers.onLogout(oldSession)
        assertNull(fixture.followers.follower(oldSession))
        assertEquals(form.objId, oldSession.followerObj)
        assertEquals(listOf(0), fixture.followerUpdates(oldSession))

        val other = fixture.player(slot = 2, processed = 100)
        fixture.followers.spawn(other, form)
        val otherPet = checkNotNull(fixture.followers.follower(other))
        val newSession = fixture.player(slot = 3).apply {
            uuid = oldSession.uuid
            assignUid()
            followerObj = oldSession.followerObj
        }
        assertEquals(-1, newSession.processedMapClock)
        newSession.client.write(VarpReset)
        newSession.resyncVar("varp.follower_npc")

        // PlayerLoginProcess runs after PlayerMainProcess, but before the post-tick hooks.
        fixture.followers.onPostTick(newSession)
        val restored = checkNotNull(fixture.followers.follower(newSession))
        assertNotEquals(oldUid, restored.uid.packed)
        assertTrue(fixture.followers.isFollowerOf(restored, newSession))
        assertFalse(fixture.followers.isFollowerOf(otherPet, newSession))
        assertEquals(listOf(0, restored.uid.packed), fixture.followerUpdates(newSession))
        assertEquals(restored.slotId, fixture.clientFollowerSlot(newSession))

        newSession.processedMapClock = 101
        repeat(3) { fixture.followers.onPostTick(newSession) }
        assertEquals(listOf(0, restored.uid.packed), fixture.followerUpdates(newSession))
    }

    @Test
    fun `spawn at map clock zero also transmits the follower uid once`() {
        val fixture = Fixture()
        val player = fixture.player(slot = 1, processed = 0)

        fixture.followers.spawn(player, Pets.all.first().base)

        val follower = checkNotNull(fixture.followers.follower(player))
        assertEquals(listOf(follower.uid.packed), fixture.followerUpdates(player))
    }

    @Test
    fun `normal spawn sends one follower update through the existing setter`() {
        val fixture = Fixture()
        val player = fixture.player(slot = 1, processed = 100)

        fixture.followers.spawn(player, Pets.all.first().base)

        val follower = checkNotNull(fixture.followers.follower(player))
        assertEquals(listOf(follower.uid.packed), fixture.followerUpdates(player))
    }

    @Test
    fun `metamorphosis clears the old follower and sends the new uid once`() {
        val fixture = Fixture()
        val player = fixture.player(slot = 1, processed = 100)
        val forms = Pets.all.first { it.forms.size > 1 }.forms
        fixture.followers.spawn(player, forms[0])
        fixture.client(player).messages.clear()

        fixture.followers.spawn(player, forms[1])

        val replacement = checkNotNull(fixture.followers.follower(player))
        assertEquals(forms[1].objId, player.followerObj)
        assertEquals(listOf(0, replacement.uid.packed), fixture.followerUpdates(player))
    }

    @Test
    fun `login without a saved follower does not spawn or send a follower update`() {
        val fixture = Fixture()
        val player = fixture.player(slot = 1)

        fixture.followers.onPostTick(player)

        assertNull(fixture.followers.follower(player))
        assertTrue(fixture.followerUpdates(player).isEmpty())
    }

    private class Fixture {
        private val clock = MapClock(100)
        private val collision = CollisionFlagMap().apply {
            for (x in 3192..3216 step 8) {
                for (z in 3192..3216 step 8) {
                    allocateIfAbsent(x, z, 0)
                }
            }
        }
        private val npcs = NpcList()
        private val repository = NpcRepository(clock, NpcRegistry(npcs, collision, EventBus()), npcs)
        val followers = PetFollowers(repository, npcs, clock, collision)

        fun player(slot: Int, processed: Int? = null) = Player(client = RecordingClient()).apply {
            coords = CoordGrid(3204, 3204)
            previousCoords = coords
            slotId = slot
            uuid = slot.toLong()
            assignUid()
            currentMapClock = 100
            if (processed != null) processedMapClock = processed
        }

        fun client(player: Player) = player.client as RecordingClient

        fun followerUpdates(player: Player): List<Int> {
            val id = "varp.follower_npc".asRSCM(RSCMType.VARP)
            return client(player).messages.mapNotNull {
                when (it) {
                    is VarpLarge -> if (it.id == id) it.value else null
                    is VarpSmall -> if (it.id == id) it.value else null
                    else -> null
                }
            }
        }

        fun clientFollowerSlot(player: Player): Int {
            val id = "varp.follower_npc".asRSCM(RSCMType.VARP)
            var value = 0
            for (message in client(player).messages) {
                when (message) {
                    VarpReset -> value = 0
                    is VarpLarge -> if (message.id == id) value = message.value
                    is VarpSmall -> if (message.id == id) value = message.value
                }
            }
            return value and 65535
        }
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() = Unit
        override fun read(player: Player) = Unit
        override fun flush() = Unit
        override fun flushHighPriority() = Unit
        override fun unregister(service: Any, player: Player) = Unit
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun loadCache() {
            ServerCacheManager.init(240).close()
        }
    }
}
