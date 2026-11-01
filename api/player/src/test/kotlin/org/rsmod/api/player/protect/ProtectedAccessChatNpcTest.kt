package org.rsmod.api.player.protect

import dev.openrune.ServerCacheManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.capture.CaptureClient
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ProtectedAccessChatNpcTest {
    @OptIn(InternalApi::class)
    @Test
    fun `NPC dialogue without animation displays each page and waits for Continue`() {
        val client = CaptureClient()
        val coroutine = GameCoroutine("chat-npc-no-animation-test")
        val player =
            Player(client = client).apply {
                uuid = 795L
                slotId = 1
                assignUid()
                coords = CoordGrid(3261, 3291, 0)
                activeCoroutine = coroutine
            }
        val npc = Npc("npc.cook", player.coords.translateZ(1))
        val events = EventBus()
        val context =
            ProtectedAccessContextFactory.empty()
                .copy(getEventBus = { events }, getAlignment = { TextAlignment() })
        val access = ProtectedAccess(player, coroutine, context)
        val firstPage = "First line.<br>Second line.<br>Third line.<br>Fourth line."
        val secondPage = "Last line."
        var completed = false
        val dialogue: suspend () -> Unit = {
            access.chatNpcNoAnim(npc, "$firstPage<br>$secondPage")
            completed = true
        }
        dialogue.startCoroutine(
            object : Continuation<Unit> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    result.getOrThrow()
                }
            }
        )

        assertTrue(player.ui.containsModal("interface.chat_left"))
        assertTrue(client.anyOf<IfSetText> { it.text == firstPage })
        assertFalse(client.anyOf<IfSetText> { it.text == secondPage })
        assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
        assertFalse(completed)

        client.clearOutgoing()
        coroutine.resumeWith(ResumePauseButtonInput("component.chat_left:continue", -1))

        assertTrue(player.ui.containsModal("interface.chat_left"))
        assertTrue(client.anyOf<IfSetText> { it.text == secondPage })
        assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
        assertFalse(completed)

        coroutine.resumeWith(ResumePauseButtonInput("component.chat_left:continue", -1))

        assertTrue(completed)
        assertTrue(coroutine.isIdle)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
