package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Every name below is a real `loc.*` gameval from the 240 cache. */
class HotspotNamingTest {
    @Test
    fun `numbered hotspots yield their slot`() {
        assertEquals(1, HotspotNaming.slotOf("loc.poh_parlour_1"))
        assertEquals(7, HotspotNaming.slotOf("loc.poh_parlour_7"))
        assertEquals(8, HotspotNaming.slotOf("loc.poh_garden_8"))
        assertEquals(2, HotspotNaming.slotOf("loc.poh_crude_garden_2"))
    }

    @Test
    fun `multi-tile parts share one slot`() {
        assertEquals(4, HotspotNaming.slotOf("loc.poh_parlour_4_middle"))
        assertEquals(4, HotspotNaming.slotOf("loc.poh_parlour_4_side"))
        assertEquals(4, HotspotNaming.slotOf("loc.poh_parlour_4_corner"))
        assertEquals(5, HotspotNaming.slotOf("loc.poh_parlour_5_scrolls"))
    }

    @Test
    fun `a part suffix fused onto the digit shares one slot`() {
        assertEquals(3, HotspotNaming.slotOf("loc.poh_workshop_3a"))
        assertEquals(3, HotspotNaming.slotOf("loc.poh_workshop_3e"))
        assertEquals(4, HotspotNaming.slotOf("loc.poh_dungeon_4l"))
        assertEquals(4, HotspotNaming.slotOf("loc.poh_dungeon_4r"))
        assertEquals(5, HotspotNaming.slotOf("loc.poh_dungeon_5l"))
        assertEquals(5, HotspotNaming.slotOf("loc.poh_posh_garden_5mid"))
        assertEquals(5, HotspotNaming.slotOf("loc.poh_posh_garden_5cor"))
    }

    @Test
    fun `a digit buried mid-name is not a slot`() {
        assertNull(HotspotNaming.slotOf("loc.poh_hall1_1_stairs_up"))
        assertNull(HotspotNaming.slotOf("loc.poh_hall1_1_stairs_top"))
        assertNull(HotspotNaming.slotOf("loc.poh_oubliette_1_type8"))
        assertNull(HotspotNaming.slotOf("loc.poh_oubliette_1_type8_ogre"))
        assertNull(HotspotNaming.slotOf("loc.poh_gr_1_wall_combat"))
    }

    @Test
    fun `doorways are never hotspots`() {
        assertTrue(HotspotNaming.isDoor("loc.poh_hotspot_doorl_rimmington"))
        assertTrue(HotspotNaming.isDoor("loc.poh_hotspot_door_dungeon3"))
        assertNull(HotspotNaming.slotOf("loc.poh_hotspot_doorl_rimmington"))
        assertFalse(HotspotNaming.isNamedHotspot("loc.poh_hotspot_doorr_yanille"))
    }

    @Test
    fun `named hotspots are placed by the room's override table`() {
        assertEquals(4, HotspotNaming.overrideSlot("chapel", "loc.poh_chapelwindow_hotspot_rimmington"))
        assertEquals(
            6,
            HotspotNaming.overrideSlot("costume room", "loc.poh_cos_room_fancy_dress_box_hotspot"),
        )
        assertEquals(1, HotspotNaming.overrideSlot("menagerie", "loc.poh_menagerie_pethouse_hotspot"))
        assertEquals(7, HotspotNaming.overrideSlot("menagerie", "loc.poh_menagerie_petfeeder_hotspot"))
        assertEquals(3, HotspotNaming.overrideSlot("menagerie outdoors", "loc.poh_menagerie_habitat_feature"))
    }

    @Test
    fun `several locs mapping to one slot are its parts`() {
        assertEquals(5, HotspotNaming.overrideSlot("menagerie", "loc.poh_menagerie_combatring_hotspot"))
        assertEquals(
            5,
            HotspotNaming.overrideSlot("menagerie", "loc.poh_menagerie_combatring_mat_hotspot"),
        )
        for (part in listOf("theme_edge", "theme_outercorner", "theme_path_1", "theme_feature")) {
            assertEquals(4, HotspotNaming.overrideSlot("superior garden", "loc.poh_superior_garden_hotspot_$part"))
        }
        for (part in listOf("rug_side", "rug_corner", "rug_middle")) {
            assertEquals(4, HotspotNaming.overrideSlot("league hall", "loc.poh_leaguehall_${part}_hotspot"))
        }
    }

    @Test
    fun `the longest matching override fragment wins`() {
        assertEquals(
            6,
            HotspotNaming.overrideSlot("superior garden", "loc.poh_superior_garden_hotspot_seating_a_left"),
        )
        assertEquals(
            7,
            HotspotNaming.overrideSlot("superior garden", "loc.poh_superior_garden_hotspot_seating_b_right"),
        )
        assertEquals(
            5,
            HotspotNaming.overrideSlot("superior garden", "loc.poh_superior_garden_hotspot_fence_post_m"),
        )
    }

    @Test
    fun `league hall numbers its three pedestals as separate slots`() {
        assertEquals(1, HotspotNaming.overrideSlot("league hall", "loc.poh_leaguehall_pedestal_hotspot_1"))
        assertEquals(2, HotspotNaming.overrideSlot("league hall", "loc.poh_leaguehall_pedestal_hotspot_2"))
        assertEquals(3, HotspotNaming.overrideSlot("league hall", "loc.poh_leaguehall_pedestal_hotspot_3"))
    }

    @Test
    fun `a room with no override table yields nothing`() {
        assertNull(HotspotNaming.overrideSlot("parlour", "loc.poh_parlour_1"))
        assertNull(HotspotNaming.overrideSlot("chapel", "loc.poh_dynamic_window"))
    }

    @Test
    fun `a trailing part number is not read by the numbered convention`() {
        assertNull(HotspotNaming.slotOf("loc.poh_leaguehall_pedestal_hotspot_1"))
        assertNull(HotspotNaming.slotOf("loc.poh_superior_garden_hotspot_theme_path_2"))
        assertTrue(HotspotNaming.isNamedHotspot("loc.poh_leaguehall_pedestal_hotspot_1"))
    }

    @Test
    fun `ordinary scenery is not a hotspot`() {
        assertNull(HotspotNaming.slotOf("loc.village_wall"))
        assertNull(HotspotNaming.slotOf("loc.poh_grass"))
        assertNull(HotspotNaming.slotOf("loc.poh_dynamic_window"))
        assertFalse(HotspotNaming.isNamedHotspot("loc.poh_grass"))
    }

    @Test
    fun `fragments drop the room prefix longest first`() {
        assertEquals(
            listOf("cosroomcaperack", "roomcaperack", "caperack", "rack"),
            HotspotNaming.fragments("loc.poh_cos_room_cape_rack_hotspot"),
        )
        assertEquals(
            listOf("menageriepethouse", "pethouse"),
            HotspotNaming.fragments("loc.poh_menagerie_pethouse_hotspot"),
        )
        assertEquals(
            listOf("leaguehallpedestal", "pedestal"),
            HotspotNaming.fragments("loc.poh_leaguehall_pedestal_hotspot_1"),
        )
    }

    @Test
    fun `named hotspots match the slot that builds them`() {
        val costumeRoom =
            mapOf(
                0 to listOf("Oak cape rack", "Teak cape rack", "Mahogany cape rack"),
                1 to listOf("Oak armour case", "Teak armour case"),
                2 to listOf("Oak toy box", "Teak toy box"),
            )

        assertEquals(0, HotspotNaming.matchSlot("loc.poh_cos_room_cape_rack_hotspot", costumeRoom))
        assertEquals(1, HotspotNaming.matchSlot("loc.poh_cos_room_armour_case_hotspot", costumeRoom))
        assertEquals(2, HotspotNaming.matchSlot("loc.poh_cos_room_toy_box_hotspot", costumeRoom))
    }

    @Test
    fun `a fragment matching several slots is rejected rather than guessed`() {
        val ambiguous =
            mapOf(
                0 to listOf("Oak cape rack"),
                1 to listOf("Oak magic rack"),
            )

        assertNull(HotspotNaming.matchSlot("loc.poh_cos_room_rack_hotspot", ambiguous))
        assertEquals(0, HotspotNaming.matchSlot("loc.poh_cos_room_cape_rack_hotspot", ambiguous))
    }

    @Test
    fun `a named hotspot with no matching furniture yields nothing`() {
        val slots = mapOf(0 to listOf("Oak cape rack"))

        assertNull(HotspotNaming.matchSlot("loc.poh_menagerie_pethouse_hotspot", slots))
    }
}
