package org.rsmod.content.other.pets

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.or2.central.account.Rights
import net.rsprot.protocol.game.outgoing.interfaces.IfSetObject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.input.ResumePStringDialogInput
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class PetMenuTest {
    @Test fun `gallery sends inventory models searches and uses existing reward rules`() {
        val f = Fixture()
        f.open()
        assertTrue(f.player.ui.containsModal("interface.pet_menu"))
        assertEquals(18, f.client.messages.count { it is IfSetObject })
        f.click("search")
        val pet = Pets.all.first { it.name == "Butch" }
        f.player.resumeActiveCoroutine(ResumePStringDialogInput(pet.name))
        assertEquals(pet.name, f.text("pet_0"))
        f.click("icon_0")
        verify(f.rewards).give(f.player, pet.base.obj)
        assertFalse(f.player.ui.containsModal("interface.pet_menu"))
    }

    @Test fun `non-admin cannot open or submit a stale gallery action`() {
        val f = Fixture()
        f.open()
        f.player.modLevel = Rights.NONE
        f.click("icon_0")
        f.open()
        verifyNoInteractions(f.rewards)
        assertFalse(f.player.ui.containsModal("interface.pet_menu"))
    }

    @Test fun `empty results and out of range clicks never award a pet`() {
        val f = Fixture()
        f.open(); f.click("search")
        f.player.resumeActiveCoroutine(ResumePStringDialogInput("no-pet-has-this-name"))
        f.click("next"); f.click("icon_17")
        assertEquals("No pets found", f.text("status"))
        assertEquals("Page 1/1", f.text("page"))
        verifyNoInteractions(f.rewards)
    }

    @Test fun `gallery fits fixed mode and every pet has a valid item model`() {
        val ui = ServerCacheManager.getInterface("interface.pet_menu".asRSCM())!!
        assertEquals(18, ui.components.values.count { it.type == 5 })
        for (c in ui.components.values) {
            assertTrue(c.x >= 0 && c.y >= 0 && c.x + c.width <= 512 && c.y + c.height <= 334, c.internalName)
            if (c.type == 4) assertTrue(c.textShadow)
        }
        for (pet in Pets.all) assertNotNull(ServerCacheManager.getItem(pet.base.objId), pet.name)
    }

    private class Fixture {
        val events = EventBus()
        val client = RecordingClient()
        val player = Player(client).apply { username = "pet-menu-test"; modLevel = Rights.ADMINISTRATOR }
        val rewards = mock(PetRewards::class.java)
        val menu = PetMenu(rewards)
        val launcher: ProtectedAccessLauncher
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java)
            `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { events }))
            launcher = ProtectedAccessLauncher(factory)
            with(menu) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
        }
        fun open() { launcher.launchLenient(player) { menu.open(this) } }
        fun click(name: String) {
            val component = ServerCacheManager.fromComponent("component.pet_menu:$name".asRSCM())
            launcher.launchLenient(player) { events.publish(this, IfModalButton(component, -1, null, IfButtonOp.Op1)) }
        }
        fun text(name: String) = client.messages.filterIsInstance<IfSetText>().last { it.combinedId == "component.pet_menu:$name".asRSCM() }.text
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
        @JvmStatic @BeforeAll fun loadCache() { ServerCacheManager.init(240).close() }
    }
}
