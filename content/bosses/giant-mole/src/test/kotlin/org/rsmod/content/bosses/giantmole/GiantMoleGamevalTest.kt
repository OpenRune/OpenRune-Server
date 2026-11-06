package org.rsmod.content.bosses.giantmole

import dev.openrune.gamevals.GameValProvider
import dev.openrune.rscm.RSCM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GiantMoleGamevalTest {
    @Test
    fun `giant mole cache symbols resolve`() {
        assertEquals(5779, RSCM.getRSCM("npc.mole_giant"))
        assertEquals(5780, RSCM.getRSCM("npc.mole_baby_01"))

        assertEquals(3309, RSCM.getRSCM("seq.mole_ready"))
        assertEquals(3310, RSCM.getRSCM("seq.mole_death"))
        assertEquals(3311, RSCM.getRSCM("seq.mole_defend"))
        assertEquals(3312, RSCM.getRSCM("seq.mole_attack"))
        assertEquals(3314, RSCM.getRSCM("seq.mole_burrow_down"))
        assertEquals(3315, RSCM.getRSCM("seq.mole_burrow_up"))
        assertEquals(830, RSCM.getRSCM("seq.human_dig"))

        assertEquals(96, RSCM.getRSCM("interface.darkness_dark"))
        assertEquals(98, RSCM.getRSCM("interface.darkness_medium"))
        assertEquals(568, RSCM.getRSCM("interface.cws_warning_3"))

        assertEquals(12201, RSCM.getRSCM("loc.mole_well"))
        assertEquals(12202, RSCM.getRSCM("loc.mole_hill"))
        assertEquals(12229, RSCM.getRSCM("loc.mole_rope_01"))
        assertEquals(12230, RSCM.getRSCM("loc.mole_rope_02"))

        assertEquals(33, RSCM.getRSCM("obj.lit_candle"))
        assertEquals(36, RSCM.getRSCM("obj.unlit_candle"))
        assertEquals(32, RSCM.getRSCM("obj.lit_black_candle"))
        assertEquals(38, RSCM.getRSCM("obj.unlit_black_candle"))
        assertEquals(594, RSCM.getRSCM("obj.torch_lit"))
        assertEquals(596, RSCM.getRSCM("obj.torch_unlit"))
        assertEquals(4524, RSCM.getRSCM("obj.oil_lamp_lit"))
        assertEquals(4522, RSCM.getRSCM("obj.oil_lamp_unlit"))
        assertEquals(4550, RSCM.getRSCM("obj.bullseye_lantern_lit"))
        assertEquals(4531, RSCM.getRSCM("obj.candle_lantern_lit"))
        assertEquals(4539, RSCM.getRSCM("obj.oil_lantern_lit"))
        assertEquals(952, RSCM.getRSCM("obj.spade"))
        assertEquals(590, RSCM.getRSCM("obj.tinderbox"))

        assertEquals(532, RSCM.getRSCM("obj.big_bones"))
        assertEquals(7416, RSCM.getRSCM("obj.mole_claw"))
        assertEquals(7417, RSCM.getRSCM("obj.cert_mole_claw"))
        assertEquals(7418, RSCM.getRSCM("obj.mole_skin"))
        assertEquals(7419, RSCM.getRSCM("obj.cert_mole_skin"))
        assertEquals(12646, RSCM.getRSCM("obj.molepet"))
        assertEquals(33382, RSCM.getRSCM("obj.immaculate_mole_skin"))

        assertEquals(1515, RSCM.getRSCM("varp.total_mole_kills"))
        assertEquals(278, RSCM.getRSCM("varbit.darkness_level"))
        assertEquals(3853, RSCM.getRSCM("varbit.cws_warning_3"))
        assertEquals(4464, RSCM.getRSCM("varbit.falador_diary_hard_complete"))
        assertEquals(6532, RSCM.getRSCM("varbit.my2arm_fire_mole"))
    }

    @Test
    fun `giant mole sound and custom symbols resolve`() {
        assertEquals(1470, RSCM.getRSCM("synth.digspade"))
        assertEquals(1384, RSCM.getRSCM("synth.roof_collapse"))
        assertEquals(1581, RSCM.getRSCM("synth.insect_bites"))
        assertEquals(1582, RSCM.getRSCM("synth.swamp_cave_insects"))
        assertEquals(1641, RSCM.getRSCM("synth.mole_burrow_down"))
        assertEquals(1642, RSCM.getRSCM("synth.mole_attack"))
        assertEquals(1645, RSCM.getRSCM("synth.mole_death"))
        assertEquals(1646, RSCM.getRSCM("synth.mole_hit"))
        assertEquals(1647, RSCM.getRSCM("synth.mud_pie_impact"))
        assertEquals(570, RSCM.getRSCM("spotanim.mole_mud_splat_spotanim"))
        assertEquals(571, RSCM.getRSCM("spotanim.mole_mud_cloud_spotanim"))
        assertEquals(572, RSCM.getRSCM("spotanim.mole_mude_hole_spotanim"))
        assertEquals(573, RSCM.getRSCM("spotanim.mole_mud_hole_up_spotanim"))
        assertEquals(896, RSCM.getRSCM("clientscript.[clientscript,mole_go_splat]"))
        assertEquals(12027, RSCM.getRSCM("models.mole_go_splat_model"))

        assertEquals(64430, RSCM.getRSCM("varn.giant_mole_burrow_lock"))
        assertEquals(64431, RSCM.getRSCM("varn.giant_mole_last_burrow_check"))
        assertEquals(64420, RSCM.getRSCM("timer.giant_mole_insect_warning"))
        assertEquals(64421, RSCM.getRSCM("timer.giant_mole_insect_swarm"))
        assertEquals(64422, RSCM.getRSCM("timer.giant_mole_insect_damage"))
    }

    private companion object {
        @JvmStatic
        @BeforeAll
        fun loadGamevals() {
            GameValProvider.load()
        }
    }
}
