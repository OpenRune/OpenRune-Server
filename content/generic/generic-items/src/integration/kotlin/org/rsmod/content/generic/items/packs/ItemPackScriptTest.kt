package org.rsmod.content.generic.items.packs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.inv.HeldOpScript
import org.rsmod.api.inv.InvOpenScript
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class ItemPackScriptTest {
    @Test
    fun GameTestState.`opening a pack swaps it for its contents`() =
        runGameTest(HeldOpScript::class, InvOpenScript::class, ItemPackScript::class) {
            // given
            player.inv[0] = InvObj(PACK)

            // when
            player.opHeld1(invSlot = 0)
            advance()

            // then
            assertEquals(0, player.inv.count(PACK))
            assertEquals(CONTENTS_PER_PACK, player.inv.count(CONTENTS))
        }

    @Test
    fun GameTestState.`opening a pack in a full inventory puts the contents in its slot`() =
        runGameTest(HeldOpScript::class, InvOpenScript::class, ItemPackScript::class) {
            // given
            player.fillInv()
            player.inv[0] = InvObj(PACK)

            // when
            player.opHeld1(invSlot = 0)
            advance()

            // then
            assertEquals(0, player.inv.count(PACK))
            assertEquals(CONTENTS_PER_PACK, player.inv.count(CONTENTS))
            assertEquals(player.inv.size - 1, player.inv.count(FILLER))
            assertMessageNotSent(NO_SPACE_MESSAGE)
        }

    @Test
    fun GameTestState.`opening a pack that would overflow the contents stack keeps the pack`() =
        runGameTest(HeldOpScript::class, InvOpenScript::class, ItemPackScript::class) {
            // given
            player.inv[0] = InvObj(PACK)
            player.inv[1] = InvObj(CONTENTS, NEARLY_FULL_STACK)

            // when
            player.opHeld1(invSlot = 0)
            advance()

            // then
            assertEquals(1, player.inv.count(PACK))
            assertEquals(NEARLY_FULL_STACK, player.inv.count(CONTENTS))
            assertMessageSent(NO_SPACE_MESSAGE)
        }

    @Test
    fun GameTestState.`two open clicks in one tick open the pack once`() =
        runGameTest(HeldOpScript::class, InvOpenScript::class, ItemPackScript::class) {
            // given
            player.inv[0] = InvObj(PACK)

            // when
            player.opHeld1(invSlot = 0)
            player.opHeld1(invSlot = 0)
            advance()

            // then
            assertEquals(0, player.inv.count(PACK))
            assertEquals(CONTENTS_PER_PACK, player.inv.count(CONTENTS))
        }

    @Test
    fun GameTestState.`one click keeps opening the remaining packs`() =
        runGameTest(HeldOpScript::class, InvOpenScript::class, ItemPackScript::class) {
            // given
            player.inv[0] = InvObj(PACK)
            player.inv[1] = InvObj(PACK)

            // when
            player.opHeld1(invSlot = 0)
            advance(ticks = 1 + OPEN_INTERVAL)

            // then
            assertEquals(0, player.inv.count(PACK))
            assertEquals(CONTENTS_PER_PACK * 2, player.inv.count(CONTENTS))
        }

    private companion object {
        const val PACK = "obj.pack_feather"
        const val CONTENTS = "obj.feather"
        const val FILLER = "obj.beer"
        const val CONTENTS_PER_PACK = 100
        const val OPEN_INTERVAL = 2
        const val NEARLY_FULL_STACK = Int.MAX_VALUE - CONTENTS_PER_PACK + 1
        const val NO_SPACE_MESSAGE = "You don't have enough inventory space to open that."
    }
}
