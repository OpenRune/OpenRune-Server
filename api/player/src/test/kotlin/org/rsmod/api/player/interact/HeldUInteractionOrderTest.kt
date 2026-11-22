package org.rsmod.api.player.interact

import dev.openrune.types.ItemServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.player.events.interact.HeldUDefaultEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.events.EventBus

class HeldUInteractionOrderTest {
    private val eventBus = EventBus()
    private val interactions = HeldUInteractions(eventBus)
    private val logs = ItemServerType(id = 1511, name = "Logs")
    private val tinderbox = ItemServerType(id = 590, name = "Tinderbox")

    @Test
    fun `reverse exact pair beats selected default`() {
        eventBus.subscribeSuspend(
            HeldUEvents.Type::class.java,
            EventBus.composeLongKey(tinderbox.id, logs.id),
        ) {}
        eventBus.subscribeSuspend(
            HeldUDefaultEvents.Type::class.java,
            logs.id.toLong(),
        ) {}

        val trigger = interactions.resolveTrigger(logs, 0, tinderbox, 1)

        assertTrue(trigger is HeldUEvents.Type)
        trigger as HeldUEvents.Type
        assertEquals(tinderbox.id, trigger.first.id)
        assertEquals(logs.id, trigger.second.id)
        assertEquals(0, trigger.secondSlot)
        assertEquals(1, trigger.firstSlot)
    }

    @Test
    fun `selected default is used when no pair exists`() {
        eventBus.subscribeSuspend(
            HeldUDefaultEvents.Type::class.java,
            logs.id.toLong(),
        ) {}

        val trigger = interactions.resolveTrigger(logs, 0, tinderbox, 1)

        assertTrue(trigger is HeldUDefaultEvents.Type)
        trigger as HeldUDefaultEvents.Type
        assertEquals(logs.id, trigger.first.id)
        assertEquals(tinderbox.id, trigger.second.id)
    }

    @Test
    fun `reverse default is used when selected has no handler`() {
        eventBus.subscribeSuspend(
            HeldUDefaultEvents.Type::class.java,
            tinderbox.id.toLong(),
        ) {}

        val trigger = interactions.resolveTrigger(logs, 0, tinderbox, 1)

        assertTrue(trigger is HeldUDefaultEvents.Type)
        trigger as HeldUDefaultEvents.Type
        assertEquals(tinderbox.id, trigger.first.id)
        assertEquals(logs.id, trigger.second.id)
    }
}
