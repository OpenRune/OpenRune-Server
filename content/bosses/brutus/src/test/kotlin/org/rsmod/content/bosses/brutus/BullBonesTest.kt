package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BullBonesTest {
    @Test
    fun `burying the same bones twice grants the xp once`() {
        val f = Fixture()
        val bones = f.dropBones()
        f.bury(bones, choices = listOf(YES))
        assertEquals(0, f.groundBones())
        assertEquals(PRAYER_XP, f.prayerXp())
        f.bury(bones, choices = listOf(YES))
        assertEquals(0, f.groundBones())
        assertEquals(PRAYER_XP, f.prayerXp())
    }

    @Test
    fun `bones gone before the prompt is answered grant nothing`() {
        val f = Fixture()
        val bones = f.dropBones()
        f.start(bones)
        f.objRepo.del(bones)
        f.finish(listOf(YES))
        assertEquals(0, f.prayerXp())
    }

    @Test
    fun `declining leaves the bones where they lie`() {
        val f = Fixture()
        val bones = f.dropBones()
        f.bury(bones, choices = listOf(NO))
        assertEquals(1, f.groundBones())
        assertEquals(0, f.prayerXp())
    }

    @Test
    fun `don't ask again skips the prompt next time`() {
        val f = Fixture()
        f.bury(f.dropBones(), choices = listOf(YES_ALWAYS))
        f.bury(f.dropBones(), choices = emptyList())
        assertEquals(0, f.groundBones())
        assertEquals(PRAYER_XP * 2, f.prayerXp())
    }

    private class Fixture {
        private val coroutine = GameCoroutine("bull-bones-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
        val objRepo = ObjRepository(clock, objRegistry)
        private val script = BullBones(objRepo)
        private val context =
            ProtectedAccessContextFactory.empty().copy(
                getEventBus = { EventBus() },
                getAlignment = { TextAlignment() },
            )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = SilentClient()
                uuid = 795L
                observerUUID = 795L
                slotId = 1
                assignUid()
                coords = CoordGrid(3261, 3291, 0)
                currentMapClock = 100
                processedMapClock = 100
            }

        fun dropBones(): Obj = objRepo.add(BONES, player.coords, duration = 100, receiver = player)

        fun groundBones(): Int =
            objRegistry.findAll(player.coords).filter { it.type == BONES.asRSCM() }.sumOf { it.count }

        fun prayerXp(): Int = player.statMap.getXP("stat.prayer")

        fun bury(bones: Obj, choices: List<Int>) {
            start(bones)
            finish(choices)
        }

        fun start(bones: Obj) {
            result = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { with(script) { access.bury(bones) } }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finish(choices: List<Int>) {
            val options = choices.iterator()
            repeat(50) {
                if (coroutine.isIdle) return
                if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                    coroutine.resumeWith(ResumePauseButtonInput("component.chatmenu:options", options.next()))
                } else {
                    player.currentMapClock++
                    player.processedMapClock = player.currentMapClock
                    coroutine.advance()
                }
                result?.getOrThrow()
            }
            fail<Unit>("Interaction did not finish")
        }
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val BONES = "obj.bull_bones"
        private const val PRAYER_XP = 40
        private const val YES = 1
        private const val YES_ALWAYS = 2
        private const val NO = 3

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
