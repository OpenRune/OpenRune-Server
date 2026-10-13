package org.rsmod.content.other.combatachievements

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.rsmod.api.combatachievements.CombatAchievementCatalogue
import org.rsmod.api.combatachievements.CombatAchievements
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.net.rsprot.handlers.ResumePauseButtonHandler
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class CombatAchievementsInterfaceTest {
    @Test
    fun `accepted cache initializes native browsers and passes the hidden boss selection component`() {
        for ((screen, script) in listOf("ca_overview" to 4804, "ca_tasks" to 4816, "ca_bosses" to 4791, "ca_rewards" to 4811, "ca_boss" to 4835)) {
            val init = ServerCacheManager.fromComponent("component.$screen:universe").onLoad
            assertEquals(script, init?.first(), screen)
        }
        val bossInit = ServerCacheManager.fromComponent("component.ca_bosses:universe").onLoad
        assertEquals("component.ca_bosses:bosses_button_click_pause".asRSCM(RSCMType.COMPONENT), bossInit?.last())
        val filter = ServerCacheManager.fromComponent("component.ca_tasks:tier_filter").onLoad
        assertArrayEquals(arrayOf(4821, 3967), filter?.take(2)?.toTypedArray())
    }

    @Test
    fun `login refreshes real progress and native overview tier button opens filtered tasks`() {
        val f = Fixture()
        f.bus.publish(SessionStateEvent.Initialize(f.player))
        verify(f.achievements).refresh(f.player)
        f.open("ca_overview")
        f.click("ca_overview:ca_buttons_click", 2)

        assertTrue(f.player.ui.containsModal("interface.ca_tasks"))
        assertEquals(3, f.player.vars["varbit.ca_task_filter_tier"])
        assertEquals(0, f.player.vars["varbit.ca_task_filter_monster"])
        assertNull(f.player.activeCoroutine)
    }

    @Test
    fun `opening native overview derives progress from persisted completion words`() {
        val f = Fixture()
        repeat(10) { f.achievements.recordBarrowsChest(f.player, allBrothersKilled = false) }
        VarPlayerIntMapSetter.set(f.player, "varbit.ca_points", 999)
        f.open("ca_overview")

        assertEquals(1, f.player.vars["varbit.ca_points"])
        assertEquals(1, f.player.vars["varbit.ca_total_tasks_completed_easy"])
        assertEquals(1, f.player.vars["varbit.ca_total_tasks_completed_barrows"])
        assertEquals(2697, f.player.vars["varbit.ca_threshold_grandmaster"])
    }

    @Test
    fun `native burger menu switches overview tasks bosses and rewards without a paused script`() {
        val f = Fixture()
        f.open("ca_overview")
        f.click("ca_overview:burger_menu_frame", 12)
        f.click("ca_tasks:burger_menu_frame", 14)
        assertTrue(f.player.ui.containsModal("interface.ca_bosses"))
        f.click("ca_bosses:burger_menu_frame", 16)
        assertTrue(f.player.ui.containsModal("interface.ca_rewards"))
        f.click("ca_rewards:burger_menu_frame", 10)
        assertTrue(f.player.ui.containsModal("interface.ca_overview"))
        assertNull(f.player.activeCoroutine)
    }

    @Test
    fun `native search independent boss key opens details and then that bosses task list`() {
        val f = Fixture()
        f.open("ca_bosses")
        VarPlayerIntMapSetter.set(f.player, "varp.if1", 600)
        f.selectBoss(33)

        assertTrue(f.player.ui.containsModal("interface.ca_boss"))
        assertEquals(33, f.player.vars["varbit.ca_boss_selected"])
        assertEquals(-1, f.player.vars["varp.if1"])
        assertTrue(f.player.ui.closeQueue.isEmpty())
        assertNull(f.player.activeCoroutine)
        f.click("ca_boss:progress_bar", 0)
        assertTrue(f.player.ui.containsModal("interface.ca_tasks"))
        assertEquals(33, f.player.vars["varbit.ca_task_filter_monster"])
        assertEquals(0, f.player.vars["varbit.ca_task_filter_tier"])
    }

    @Test
    fun `native reward tabs save selection and invalid or closed selectors cannot navigate`() {
        val f = Fixture()
        f.open("ca_rewards")
        f.click("ca_rewards:tiers_click", 5)
        assertEquals(5, f.player.vars["varbit.ca_last_reward_tier_selected"])
        f.click("ca_rewards:tiers_click", 6)
        assertEquals(5, f.player.vars["varbit.ca_last_reward_tier_selected"])

        f.open("ca_bosses")
        f.selectBoss(0)
        f.selectBoss(65535)
        assertTrue(f.player.ui.containsModal("interface.ca_bosses"))
        f.player.ifClose(f.bus)
        f.selectBoss(5)
        assertFalse(f.player.ui.containsModal("interface.ca_boss"))
        assertEquals(0, f.player.vars["varbit.ca_boss_selected"])
    }

    @Test
    fun `boss list button remains usable after a stateless selection`() {
        val f = Fixture()
        f.open("ca_bosses")
        f.selectBoss(5)
        f.click("ca_boss:ca_boss_stats_click", 0)
        assertTrue(f.player.ui.containsModal("interface.ca_bosses"))
        f.selectBoss(33)
        assertEquals(33, f.player.vars["varbit.ca_boss_selected"])
    }

    private class Fixture {
        val bus = EventBus()
        val player = Player()
        val achievements = spy(CombatAchievements(CombatAchievementCatalogue()))
        private val launcher: ProtectedAccessLauncher

        init {
            val factory = mock(ProtectedAccessContextFactory::class.java)
            `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }))
            launcher = ProtectedAccessLauncher(factory)
            with(CombatAchievementsScript(achievements, bus)) {
                ScriptContext(bus, CheatCommandMap(), EngineQueueCache()).startup()
            }
            VarPlayerIntMapSetter.set(player, "varbit.ca_boss_selected", 0)
        }

        fun open(screen: String) {
            player.ifOpenMainModal("interface.$screen", bus)
        }

        fun click(component: String, sub: Int) {
            val constructor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }
            val packet = constructor.newInstance("component.$component".asRSCM(RSCMType.COMPONENT), sub, -1, 1, null) as If3Button
            If3ButtonHandler(bus, launcher).handle(player, packet)
        }

        fun selectBoss(boss: Int) {
            ResumePauseButtonHandler(bus).handle(
                player,
                ResumePauseButton(CombinedId("component.ca_bosses:bosses_button_click_pause".asRSCM(RSCMType.COMPONENT)), boss),
            )
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
