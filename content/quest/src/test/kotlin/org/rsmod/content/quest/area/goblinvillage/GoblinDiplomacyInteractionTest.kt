package org.rsmod.content.quest.area.goblinvillage

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.content.quest.area.QuestTalkFixture
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class GoblinDiplomacyInteractionTest {
    private fun fixture() =
        QuestTalkFixture(CoordGrid(2957, 3511, 0)) { GoblinDiplomacy() }.apply { randomValue = 1 }

    private val QuestTalkFixture.stage get() = player.vars["varbit.gobdip_main"]

    private fun QuestTalkFixture.start() = talk(GENERAL, 3, 3, 1, 3)

    private fun QuestTalkFixture.searchCrate(loc: String) {
        val type = checkNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)))
        val bound =
            BoundLocInfo(LocInfo(0, player.coords.translateX(1), LocEntity(type.id, 10, 0)), type)
        interact { assertTrue(events.publish(it, LocEvents.Op1(bound, bound, type))) }
    }

    private fun QuestTalkFixture.slot(obj: String): Int =
        (0 until 28).first { player.inv[it]?.id == obj.asRSCM(RSCMType.OBJ) }

    private fun QuestTalkFixture.use(first: String, second: String) {
        val a = checkNotNull(ServerCacheManager.getItem(first.asRSCM(RSCMType.OBJ)))
        val b = checkNotNull(ServerCacheManager.getItem(second.asRSCM(RSCMType.OBJ)))
        val event = HeldUEvents.Type(a, slot(first), b, slot(second))
        interact { assertTrue(events.publish(it, event)) }
    }

    private fun QuestTalkFixture.useOnGeneral(obj: String) {
        val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
        val npcType = checkNotNull(ServerCacheManager.getNpc(GENERAL.asRSCM(RSCMType.NPC)))
        val npc = Npc(GENERAL, player.coords.translateZ(1))
        interact { assertTrue(events.publish(it, NpcUDefaultEvents.OpType(npc, slot(obj), type, npcType))) }
    }

    @Test fun `accepting the new colour starts the quest at 3`() {
        val f = fixture()
        f.start()
        assertEquals(3, f.stage)
        assertEquals(3, f.player.vars["varp.goblinquest"] and 0x3f)
    }

    @Test fun `each crate gives one goblin mail once`() {
        val f = fixture()
        for (crate in CRATES) f.searchCrate(crate)
        assertEquals(3, f.count(MAIL))
        for (crate in CRATES) f.searchCrate(crate)
        assertEquals(3, f.count(MAIL))
        assertTrue(f.output().contains("but but find nothing of interest"), f.output())
    }

    @Test fun `dyeing and mixing take one of each and give one result`() {
        val f = fixture()
        f.give("obj.reddye")
        f.give("obj.yellowdye")
        f.use("obj.reddye", "obj.yellowdye")
        assertEquals(listOf(0, 0, 1), listOf("obj.reddye", "obj.yellowdye", ORANGE_DYE).map { f.count(it) })
        f.give(MAIL)
        f.give(MAIL)
        f.use(ORANGE_DYE, MAIL)
        assertEquals(listOf(0, 1, 1), listOf(ORANGE_DYE, MAIL, ORANGE_MAIL).map { f.count(it) })
    }

    @Test fun `orange then blue then brown complete the quest once`() {
        val f = fixture()
        f.start()
        f.give(ORANGE_MAIL)
        f.give(BLUE_MAIL)
        f.give(MAIL)
        f.talk(GENERAL, 4)
        assertEquals(4, f.stage)
        assertEquals(0, f.count(ORANGE_MAIL))
        assertEquals(1, f.player.vars["varbit.gobdip_grubfoot_vis"])

        f.useOnGeneral(MAIL)
        assertEquals(4, f.stage)
        assertEquals(1, f.count(MAIL))
        assertTrue(f.output().contains("We tell you get blue armour."), f.output())

        f.useOnGeneral(BLUE_MAIL)
        assertEquals(5, f.stage)
        assertEquals(2, f.player.vars["varbit.gobdip_grubfoot_vis"])

        f.useOnGeneral(MAIL)
        assertEquals(6, f.stage)
        assertEquals(0, f.count(MAIL))
        assertEquals(1, f.count("obj.gold_bar"))
        assertEquals(200, f.player.statMap.getXP("stat.crafting"))
    }

    companion object {
        const val GENERAL = "npc.general_bentnoze_red"
        const val MAIL = "obj.goblin_armour"
        const val ORANGE_MAIL = "obj.goblin_armour_orange"
        const val BLUE_MAIL = "obj.goblin_armour_darkblue"
        const val ORANGE_DYE = "obj.orangedye"
        val CRATES =
            listOf(
                "loc.goblin_outpost_large_crate_armour1",
                "loc.goblin_outpost_large_crate_armour2",
                "loc.goblin_outpost_large_crate_armour3",
            )

        @JvmStatic @BeforeAll fun cache() = QuestTalkFixture.setUpCache()

        @JvmStatic @AfterAll fun restore() = QuestTalkFixture.restore()
    }
}
