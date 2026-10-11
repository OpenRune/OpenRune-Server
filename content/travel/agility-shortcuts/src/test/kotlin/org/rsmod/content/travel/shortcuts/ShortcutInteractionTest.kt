package org.rsmod.content.travel.shortcuts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.inv.storage.*
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.loc.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class ShortcutInteractionTest {
    @Test fun `both native bridge sides consume materials once and allow the actual transformed crossing`() {
        val f = Fixture(CoordGrid(3503, 3562)); f.bridgeMaterials()
        f.set("varbit.fenk_built_bridge_north", 1); f.set("varbit.fenk_built_bridge_south", 1)
        f.op("loc.fenk_bridge_multi_north", CoordGrid(3503, 3562)); f.finish()
        assertEquals(2, f.player.vars["varbit.fenk_built_bridge_north"])
        assertEquals(0, f.player.inv.physicalCount("obj.plank_teak")); assertEquals(0, f.player.inv.physicalCount("obj.nails_mithril"))
        assertEquals(550, f.player.statMap.getXP("stat.construction"))
        f.op("loc.fenk_bridge_multi_north", CoordGrid(3503, 3562)); f.finish()
        assertEquals(CoordGrid(3503, 3562), f.player.coords)
        f.player.coords = CoordGrid(3503, 3559); f.bridgeMaterials()
        f.op("loc.fenk_bridge_multi_south_mirror", CoordGrid(3503, 3559)); f.finish()
        assertEquals(2, f.player.vars["varbit.fenk_built_bridge_south"]); assertEquals(2, f.player.vars["varbit.fenk_bridge_span"])
        assertEquals(1100, f.player.statMap.getXP("stat.construction"))
        f.op("loc.fenk_bridge_multi_south_mirror", CoordGrid(3503, 3559)); f.finish()
        assertEquals(CoordGrid(3503, 3562), f.player.coords)
    }

    @Test fun `bridge repair failure interruptions and virtual planks preserve or commit all materials atomically`() {
        for (mode in 0..4) {
            val f = Fixture(CoordGrid(3503, 3562)); f.bridgeMaterials(); f.set("varbit.fenk_built_bridge_north", 1)
            if (mode == 0)f.player.inv[1] = null
            f.op("loc.fenk_bridge_multi_north", CoordGrid(3503, 3562))
            when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.coords = CoordGrid(3500, 3562); 4 -> f.player.inv[2] = null }
            f.finish(); assertEquals(1, f.player.vars["varbit.fenk_built_bridge_north"])
            assertEquals(0, f.player.statMap.getXP("stat.construction")); assertEquals(if (mode == 0 || mode == 4)5 else 6, f.player.inv.physicalCount("obj.plank_teak"))
        }
        val f = Fixture(CoordGrid(3503, 3562)); f.bridgeMaterials(); f.set("varbit.fenk_built_bridge_north", 1)
        for (i in 1..6)f.player.inv[i] = null
        f.storedPlanks = 6
        f.op("loc.fenk_bridge_multi_north", CoordGrid(3503, 3562)); f.finish()
        assertEquals(2, f.player.vars["varbit.fenk_built_bridge_north"]); assertEquals(0, f.storedPlanks)
        assertEquals(0, f.player.inv.physicalCount("obj.nails_mithril"))
    }

    @Test fun `real long rope use attaches the native Darkmeyer transform only after a successful uninterrupted action`() {
        for (mode in 0..3) {
            val f = Fixture(CoordGrid(3668, 3375)); f.player.inv[0] = InvObj("obj.long_rope")
            f.use("loc.darkm_outer_wall_2h_shortcut", CoordGrid(3668, 3375), 0)
            when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.inv[0] = null; 3 -> f.player.coords = CoordGrid(3664, 3375) }
            f.finish(); assertEquals(if (mode == 0)1 else 0, f.player.vars["varbit.darkm_shortcut_inner"])
            if (mode != 2)assertEquals(if (mode == 0)0 else 1, f.player.inv.physicalCount("obj.long_rope"))
        }
    }

    @Test fun `native shortcut click grants experience once and interrupted travel does not undo external movement`() {
        val r = ShortcutRoutes.all.first { it.loc == "loc.slayertower_window_shortcut_through" }
        val f = Fixture(r.origin)
        f.player.coords = r.origin; f.op(r.loc, r.origin)
        f.player.coords = r.origin.translateX(10); f.finish()
        assertEquals(r.origin.translateX(10), f.player.coords); assertEquals(0, f.player.statMap.getXP("stat.agility"))
        f.player.coords = r.origin; f.op(r.loc, r.origin); f.finish()
        assertEquals(r.destination, f.player.coords); assertEquals(r.xp.toInt(), f.player.statMap.getXP("stat.agility"))
    }

    @Test fun `native Viyeldi long rope setup is permanent and cancels without consumption`() {
        for (mode in 0..3) {
            val f = Fixture(CoordGrid(2381, 4714)); f.player.inv[0] = InvObj("obj.long_rope")
            f.use("loc.viyeldi_shortcut_rock_multi", CoordGrid(2379, 4714), 0)
            when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.inv[0] = null; 3 -> f.player.coords = CoordGrid(2380, 4711) }
            f.finish(); assertEquals(if (mode == 0) 1 else 0, f.player.vars["varbit.viyeldi_shortcut"])
            if (mode != 2) assertEquals(if (mode == 0) 0 else 1, f.player.inv.physicalCount("obj.long_rope"))
        }
    }

    @Test fun `native blocked laboratory wall requires a usable pickaxe and boosted Mining 78`() {
        for (mode in 0..4) {
            val f = Fixture(CoordGrid(3536, 9768))
            if (mode != 0) f.player.inv[0] = InvObj("obj.rune_pickaxe")
            if (mode == 1) f.player.statMap.setCurrentLevel("stat.mining", 77)
            f.op("loc.myq5_wall_cave_shortcut_6_parent", CoordGrid(3535, 9768))
            if (mode == 3) f.player.inv[0] = null
            if (mode == 4) f.player.pendingLogout = true
            f.finish(); assertEquals(if (mode == 2) 1 else 0, f.player.vars["varbit.myq5_lab_sc_unlocked"])
            assertEquals(0, f.player.statMap.getXP("stat.mining"))
            if (mode == 2) {
                f.op("loc.myq5_wall_cave_shortcut_6_parent", CoordGrid(3535, 9768)); f.finish()
                assertEquals(CoordGrid(3500, 9805), f.player.coords)
            }
        }
    }

    @Test fun `broken raft grapple uses native approach and respects late equipment changes`() {
        for (mode in 0..3) {
            val f = Fixture(CoordGrid(3246, 3180))
            f.player.worn[3] = InvObj("obj.xbows_crossbow_runite")
            f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope")
            if (mode == 1) f.player.statMap.setCurrentLevel("stat.ranged", 36)
            f.ap("loc.xbows_raft_br", CoordGrid(3253, 3180))
            if (mode == 2) f.player.worn[13] = null
            if (mode == 3) f.player.coords = CoordGrid(3244, 3178)
            f.finish()
            assertEquals(when (mode) { 0 -> CoordGrid(3259, 3180); 3 -> CoordGrid(3244, 3178); else -> CoordGrid(3246, 3180) }, f.player.coords)
            assertEquals(0, f.player.statMap.getXP("stat.agility"))
        }
    }

    @Test fun `native tunnel pieces work from both shores and Waterbirth remains one way`() {
        for (symbol in listOf("loc.slayer_cave_tunnel_left", "loc.slayer_cave_tunnel_right")) {
            val f = Fixture(CoordGrid(2429, 9806)); f.op(symbol, CoordGrid(2430, 9806)); f.finish(); assertEquals(2435, f.player.coords.x)
            f.op(symbol, CoordGrid(2434, 9807)); f.finish(); assertEquals(2429, f.player.coords.x)
        }
        val f = Fixture(CoordGrid(1854, 4406, 3)); f.op("loc.dagannoth_crevice", CoordGrid(1853, 4406, 3)); f.finish()
        assertEquals(CoordGrid(1863, 4406, 1), f.player.coords); assertEquals(10, f.player.statMap.getXP("stat.agility"))
        f.op("loc.dagannoth_crevice", CoordGrid(1861, 4406, 1)); f.finish(); assertEquals(CoordGrid(1863, 4406, 1), f.player.coords); assertEquals(10, f.player.statMap.getXP("stat.agility"))
    }

    @Test fun `native Vampyrium rubble must be mined before either direction of pillar traversal`() {
        val f = Fixture(CoordGrid(2571, 7790))
        f.ap("loc.vampyrium_stepping_stone_shortcut_pillar", CoordGrid(2571, 7794)); f.finish()
        assertEquals(CoordGrid(2571, 7790), f.player.coords)
        f.player.inv[0] = InvObj("obj.rune_pickaxe")
        f.op("loc.vampyrium_stepping_shortcut_ending", CoordGrid(2571, 7791)); f.finish()
        assertEquals(1, f.player.vars["varbit.vampyrium_traversal_rock_mined"]); assertEquals(0, f.player.vars["varbit.myq6"])
        f.ap("loc.vampyrium_stepping_stone_shortcut_pillar", CoordGrid(2571, 7794)); f.finish()
        assertEquals(CoordGrid(2575, 7802), f.player.coords); assertEquals(5, f.player.statMap.getXP("stat.agility"))
        f.ap("loc.vampyrium_stepping_stone_shortcut_pillar", CoordGrid(2571, 7800)); f.finish()
        assertEquals(CoordGrid(2571, 7790), f.player.coords); assertEquals(11, f.player.statMap.getXP("stat.agility"))
    }

    @Test fun `Vampyrium slide cooldown is shared by both native rock shortcuts and persists in account attributes`() {
        val f = Fixture(CoordGrid(2573, 7767))
        f.op("loc.voros_rockslide02_shortcut_down_big", CoordGrid(2574, 7767)); f.finish()
        assertEquals(550, f.player.statMap.getXP("stat.agility")); assertTrue(f.player.attr[ShortcutScript.VAMPYRIUM_LAST_SLIDE] != null)
        f.op("loc.voros_rockslide02_shortcut_up_big", CoordGrid(2579, 7767)); f.finish()
        assertEquals(550, f.player.statMap.getXP("stat.agility"))
        f.player.coords = CoordGrid(2553, 7823)
        f.op("loc.voros_rockslide02_shortcut_down", CoordGrid(2554, 7822)); f.finish()
        assertEquals(577, f.player.statMap.getXP("stat.agility"))
        f.player.attr[ShortcutScript.VAMPYRIUM_LAST_SLIDE] = java.time.Instant.now().epochSecond.toInt() - 451
        f.player.coords = CoordGrid(2573, 7767)
        f.op("loc.voros_rockslide02_shortcut_down_big", CoordGrid(2574, 7767)); f.finish()
        assertEquals(1127, f.player.statMap.getXP("stat.agility"))
        val old = f.player.attr[ShortcutScript.VAMPYRIUM_LAST_SLIDE]
        f.player.coords = CoordGrid(2573, 7767); f.op("loc.voros_rockslide02_shortcut_down_big", CoordGrid(2574, 7767)); f.player.pendingLogout = true; f.finish()
        assertEquals(old, f.player.attr[ShortcutScript.VAMPYRIUM_LAST_SLIDE]); assertEquals(1127, f.player.statMap.getXP("stat.agility"))
    }

    @Test fun `Vampyrium route levels differ by direction and both native bushes cross in both directions`() {
        val f = Fixture(CoordGrid(2573, 7767)); f.player.statMap.setCurrentLevel("stat.agility", 77)
        f.op("loc.voros_rockslide02_shortcut_down_big", CoordGrid(2574, 7767)); f.finish(); assertEquals(CoordGrid(2573, 7767), f.player.coords)
        f.player.coords = CoordGrid(2580, 7767); f.player.statMap.setCurrentLevel("stat.agility", 61)
        f.op("loc.voros_rockslide02_shortcut_up_big", CoordGrid(2579, 7767)); f.finish(); assertEquals(CoordGrid(2573, 7767), f.player.coords)
        for ((from, loc, to) in listOf(Triple(CoordGrid(2656, 7816), CoordGrid(2657, 7816), CoordGrid(2658, 7816)), Triple(CoordGrid(2666, 7837), CoordGrid(2667, 7837), CoordGrid(2668, 7837)))) {
            val g = Fixture(from); g.player.statMap.setCurrentLevel("stat.agility", 63)
            g.op("loc.bush_shortcut", loc); g.finish(); assertEquals(from, g.player.coords)
            g.player.statMap.setCurrentLevel("stat.agility", 64)
            g.op("loc.bush_shortcut", loc); g.finish(); assertEquals(to, g.player.coords)
            g.op("loc.bush_shortcut", loc); g.finish(); assertEquals(from, g.player.coords); assertEquals(4, g.player.statMap.getXP("stat.agility"))
        }
    }

    @Test fun `virtual quest options never overwrite native Blood Moon progress and RespectProgress still denies access`() {
        val old = org.rsmod.content.quest.manager.QuestRequirements.activePolicy()
        try {
            org.rsmod.content.quest.manager.QuestRequirements.install(org.rsmod.content.quest.manager.QuestRequirementPolicy(org.rsmod.content.quest.manager.QuestRequirementMode.RespectProgress))
            val f = Fixture(CoordGrid(2559, 7820))
            f.op("loc.voros_rockslide02_shortcut_up", CoordGrid(2557, 7819)); f.finish(); assertEquals(CoordGrid(2559, 7820), f.player.coords); assertEquals(0, f.player.vars["varbit.myq6"])
            f.set("varbit.myq6", 200)
            f.op("loc.voros_rockslide02_shortcut_up", CoordGrid(2557, 7819)); f.finish(); assertEquals(CoordGrid(2553, 7823), f.player.coords); assertEquals(200, f.player.vars["varbit.myq6"])
        } finally { org.rsmod.content.quest.manager.QuestRequirements.install(old) }
    }

    @Test fun `native crossbow grapple routes accept low level bows and quiver ammo without combat ammo restrictions`() {
        val r = ShortcutRoutes.all.first { it.loc == "loc.xbows_rock_hilltop_basic" && it.gear == "GEAR_GRAPPLE" }
        for (bow in listOf("obj.phoenix_crossbow", "obj.crossbow", "obj.xbows_crossbow_bronze", "obj.dttd_bone_crossbow", "obj.barrows_karil_weapon", "obj.hunting_crossbow_sunlight", "obj.xbows_crossbow_runite")) {
            for (extra in listOf(false, true)) {
                val f = Fixture(r.origin); f.player.statMap.setCurrentLevel("stat.agility", 32)
                f.player.worn[3] = InvObj(bow)
                if (extra) {
                    f.player.worn[1] = InvObj("obj.dizanas_quiver_uncharged")
                    org.rsmod.api.player.worn.DizanasQuiver.store(f.player, InvObj("obj.xbows_grapple_tip_bolt_mithril_rope"))
                } else f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope")
                f.op(r.loc, r.origin); f.finish()
                assertEquals(r.destination, f.player.coords, "$bow extra=$extra")
                verify(f.random).of(25)
                assertEquals(1, if (extra) org.rsmod.api.player.worn.DizanasQuiver.stored(f.player)?.count else f.player.worn[13]?.count)
            }
        }
    }

    @Test fun `native route denies decorative and unfinished crossbows and unworn stored grapples`() {
        val r = ShortcutRoutes.all.first { it.loc == "loc.xbows_rock_hilltop_basic" && it.gear == "GEAR_GRAPPLE" }
        for (bow in listOf("obj.pride23_crossbow", "obj.xbows_crossbow_unstrung_runite", "obj.bronze_longsword")) {
            val f = Fixture(r.origin); f.player.statMap.setCurrentLevel("stat.agility", 32)
            f.player.worn[3] = InvObj(bow); f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope")
            f.op(r.loc, r.origin); f.finish(); assertEquals(r.origin, f.player.coords); verify(f.random, never()).of(25)
        }
        val f = Fixture(r.origin); f.player.statMap.setCurrentLevel("stat.agility", 32); f.player.worn[3] = InvObj("obj.xbows_crossbow_runite")
        f.player.inv[0] = InvObj("obj.dizanas_quiver_uncharged")
        org.rsmod.api.player.worn.DizanasQuiver.store(f.player, InvObj("obj.xbows_grapple_tip_bolt_mithril_rope"))
        f.op(r.loc, r.origin); f.finish(); assertEquals(r.origin, f.player.coords); verify(f.random, never()).of(25)
    }

    @Test fun `successful native grapple crossing consumes one grapple for exactly one of twenty five outcomes`() {
        val r = ShortcutRoutes.all.first { it.loc == "loc.xbows_rock_hilltop_basic" && it.gear == "GEAR_GRAPPLE" }
        for (roll in 0..24) for (extra in listOf(false, true)) {
            val f = Fixture(r.origin); f.player.statMap.setCurrentLevel("stat.agility", 32); `when`(f.random.of(25)).thenReturn(roll)
            f.player.worn[3] = InvObj("obj.dttd_bone_crossbow")
            if (extra) { f.player.worn[1] = InvObj("obj.dizanas_quiver_uncharged"); org.rsmod.api.player.worn.DizanasQuiver.store(f.player, InvObj("obj.xbows_grapple_tip_bolt_mithril_rope", 2)) } else f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope", 2)
            f.op(r.loc, r.origin); f.finish(); assertEquals(r.destination, f.player.coords)
            val remaining = if (extra) org.rsmod.api.player.worn.DizanasQuiver.stored(f.player) else f.player.worn[13]
            assertEquals(if (roll == 0) 1 else 2, remaining?.count); verify(f.random).of(25)
        }
    }

    @Test fun `grapple cancellation and late stat removal roll back crossing without spending ammo`() {
        val r = ShortcutRoutes.all.first { it.loc == "loc.xbows_rock_hilltop_basic" && it.gear == "GEAR_GRAPPLE" }
        for (mode in 0..3) {
            val f = Fixture(r.origin); f.player.statMap.setCurrentLevel("stat.agility", 32)
            f.player.worn[3] = InvObj("obj.xbows_crossbow_runite"); f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope")
            f.op(r.loc, r.origin)
            when (mode) { 0 -> f.player.worn[3] = null; 1 -> f.player.statMap.setCurrentLevel("stat.ranged", 34); 2 -> f.player.pendingLogout = true; 3 -> f.player.coords = r.origin.translateX(20) }
            f.finish(); verify(f.random, never()).of(25); assertEquals(1, f.player.worn[13]?.count)
            if (mode == 0 || mode == 1) assertEquals(r.origin, f.player.coords)
            if (mode == 3) assertEquals(r.origin.translateX(20), f.player.coords)
        }
    }

    @Test fun `barehanded native shortcut does not require ranged gear or roll grapple loss`() {
        val r = ShortcutRoutes.all.first { it.loc == "loc.xbows_jungletree_karamja_basic" }
        val f = Fixture(r.origin); f.player.statMap.setCurrentLevel("stat.agility", 78); f.player.statMap.setCurrentLevel("stat.ranged", 1); f.player.statMap.setCurrentLevel("stat.strength", 1)
        f.set("varbit.observatory_shortcut_rope", 1)
        f.op(r.loc, r.origin); f.finish(); assertEquals(r.destination, f.player.coords); verify(f.random, never()).of(25)
    }

    @Test fun `Observatory setup consumes its first grapple once and interrupted setup leaves rope unset`() {
        for (mode in 0..5) {
            val f = Fixture(CoordGrid(2449, 3155)); f.player.worn[3] = InvObj("obj.xbows_crossbow_bronze"); f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope", 2)
            f.op("loc.xbows_rock_not_grappled", CoordGrid(2448, 3155))
            when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.statMap.setCurrentLevel("stat.agility", 22); 3 -> f.player.statMap.setCurrentLevel("stat.ranged", 23); 4 -> f.player.statMap.setCurrentLevel("stat.strength", 27); 5 -> f.player.worn[3] = null }
            f.finish(); assertEquals(if (mode == 0) 1 else 0, f.player.vars["varbit.observatory_shortcut_rope"])
            assertEquals(if (mode == 0) 1 else 2, f.player.worn[13]?.count); verify(f.random, never()).of(25)
            if (mode == 0) { f.op("loc.xbows_rock_not_grappled", CoordGrid(2448, 3155)); f.finish(); assertEquals(1, f.player.worn[13]?.count) }
        }
        val old = org.rsmod.content.quest.manager.QuestRequirements.activePolicy()
        try {
            org.rsmod.content.quest.manager.QuestRequirements.install(org.rsmod.content.quest.manager.QuestRequirementPolicy(org.rsmod.content.quest.manager.QuestRequirementMode.RespectProgress))
            val f = Fixture(CoordGrid(2449, 3155)); f.player.worn[3] = InvObj("obj.xbows_crossbow_bronze"); f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope")
            f.op("loc.xbows_rock_not_grappled", CoordGrid(2448, 3155)); f.finish(); assertEquals(0, f.player.vars["varbit.observatory_shortcut_rope"]); assertEquals(1, f.player.worn[13]?.count)
        } finally { org.rsmod.content.quest.manager.QuestRequirements.install(old) }
    }

    @Test fun `raft arrival rechecks equipment before accepting the crossing and rolling loss`() {
        val f = Fixture(CoordGrid(3246, 3180)); f.player.worn[3] = InvObj("obj.xbows_crossbow_runite"); f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope")
        f.ap("loc.xbows_raft_br", CoordGrid(3253, 3180))
        repeat(6) { if (f.player.coords == CoordGrid(3246, 3180)) f.advance(1) }
        assertEquals(CoordGrid(3259, 3180), f.player.coords)
        f.player.worn[3] = null; f.finish(); assertEquals(CoordGrid(3246, 3180), f.player.coords); verify(f.random, never()).of(25); assertEquals(1, f.player.worn[13]?.count)
    }

    @Test fun `all native God Wars handhold pieces enforce Agility sixty and cross in the authored direction`() {
        for (r in ShortcutRoutes.all.filter { it.loc in setOf("loc.godwars_climbing_rocks_inactive", "loc.godwars_climbing_rocks_inactive_plain") }) {
            val f = Fixture(r.origin); f.player.statMap.setCurrentLevel("stat.agility", 59); f.op(r.loc, r.origin); f.finish(); assertEquals(r.origin, f.player.coords)
            f.player.statMap.setCurrentLevel("stat.agility", 60); f.op(r.loc, r.origin); f.finish(); assertEquals(r.destination, f.player.coords)
        }
    }

    @Test fun `native bridge surface never bypasses the two repaired sides`() {
        val f = Fixture(CoordGrid(3503, 3559)); f.set("varbit.fenk_built_bridge_south", 2)
        f.op("loc.fenk_bridgesurface", CoordGrid(3503, 3558)); f.finish(); assertEquals(CoordGrid(3503, 3559), f.player.coords)
        f.set("varbit.fenk_built_bridge_north", 2); f.op("loc.fenk_bridgesurface", CoordGrid(3503, 3558)); f.finish(); assertEquals(CoordGrid(3503, 3562), f.player.coords)
    }

    @Test fun `native Motherlode shortcuts preserve diaries and reject interruption in every direction`() {
        for ((loc, exit, diary) in listOf(Triple(CoordGrid(3760, 5670), CoordGrid(3765, 5671), true), Triple(CoordGrid(3764, 5671), CoordGrid(3759, 5670), true), Triple(CoordGrid(3744, 5642), CoordGrid(3745, 5646), false), Triple(CoordGrid(3745, 5645), CoordGrid(3744, 5641), false))) {
            for (mode in 0..4) {
                val f = Fixture(loc)
                if (diary && mode != 4) { f.set("varbit.falador_diary_easy_complete", 1); f.set("varbit.falador_diary_medium_complete", 1) }
                f.op("loc.motherlode_shortcut", loc)
                when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.coords = loc.translateX(12); 3 -> f.player.statMap.setCurrentLevel("stat.agility", 53) }
                f.finish(); assertEquals(if (mode == 0 || mode == 4 && !diary) exit else if (mode == 2) loc.translateX(12) else loc, f.player.coords)
            }
        }
    }

    @Test fun `native Draynor underwall traversal checks both directions and interruption before teleport`() {
        for ((symbol, origin, exit) in listOf(Triple("loc.draynor_diary_under_wall_w", CoordGrid(3066, 3257), CoordGrid(3070, 3257)), Triple("loc.draynor_diary_under_wall_e", CoordGrid(3070, 3257), CoordGrid(3066, 3257)))) for (mode in 0..3) {
            val f = Fixture(origin); f.op(symbol, origin)
            when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.statMap.setCurrentLevel("stat.agility", 41) }
            f.finish(); assertEquals(if (mode == 0) exit else origin, f.player.coords)
        }
    }

    @Test fun `every owned catalogue route crosses through its native option producer with fulfilled requirements`() {
        val old = org.rsmod.content.quest.manager.QuestRequirements.activePolicy()
        try {
            org.rsmod.content.quest.manager.QuestRequirements.install(org.rsmod.content.quest.manager.QuestRequirementPolicy(org.rsmod.content.quest.manager.QuestRequirementMode.AssumeCompleted))
            val errors = mutableListOf<String>()
            for (r in ShortcutRoutes.all.filter { it.loc !in ShortcutRoutes.external }) {
                val f = Fixture(r.origin)
                r.gate?.let { f.set(it, r.gateValue) }
                if (r.loc.startsWith("loc.fenk_bridge")) { f.set("varbit.fenk_built_bridge_north", 2); f.set("varbit.fenk_built_bridge_south", 2) }
                f.player.worn[3] = InvObj("obj.xbows_crossbow_runite"); f.player.worn[13] = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope"); f.player.worn[10] = InvObj("obj.death_climbingboots")
                val base = ServerCacheManager.getObject(r.loc.asRSCM())!!
                val variants = if (base.multiLoc.isNotEmpty()) base.multiLoc.asSequence().mapNotNull { ServerCacheManager.getObject(it) }.toList() else listOf(base)
                val type = variants.first { t -> t.id !in setOf(5160, 57710) && (0..4).any { t.actions.getOpOrNull(it).equals(r.option, true) } }
                val index = (0..4).first { type.actions.getOpOrNull(it).equals(r.option, true) }
                val symbol = dev.openrune.rscm.RSCM.getReverseMapping(dev.openrune.rscm.RSCMType.LOC, type.id)
                if (r.loc == "loc.vampyrium_stepping_stone_shortcut_pillar") f.ap(symbol, r.origin)
                else {
                    val source = if (r.loc == "loc.wint_pillar_jump") CoordGrid((r.origin.x + r.destination.x) / 2, 4023) else r.origin
                    f.option(symbol, source, InteractionOp.entries[index])
                }
                f.advance(maxOf(20, r.ticks + 10, r.stages.size + 10))
                if (r.destination != f.player.coords) errors += "${r.loc} ${r.option} ${r.origin} -> ${r.destination}; actual ${f.player.coords}"
            }
            java.nio.file.Files.writeString(java.nio.file.Path.of("../shortcut-producer-conflicts.txt"), errors.joinToString("\n"))
            assertTrue(errors.isEmpty(), errors.joinToString("\n"))
        } finally { org.rsmod.content.quest.manager.QuestRequirements.install(old) }
    }

    private class Fixture(origin: CoordGrid) {
        val bus = EventBus(); val ctx = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val map = shortcutTestCollision
        val player = Player(client).apply {
            coords = origin; currentMapClock = 100; processedMapClock = 100; inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn")
            for (stat in listOf("stat.agility", "stat.construction", "stat.mining", "stat.attack", "stat.hitpoints", "stat.ranged", "stat.strength")) { statMap.setBaseLevel(stat, 99); statMap.setCurrentLevel(stat, 99) }
        }
        var storedPlanks = 0
        val storage = PlayerItemStorage(setOf(object : PlayerItemStorageHook {
            override fun shouldProcess(ctx: PlayerItemStorageContext) = ctx.itemInternal == "obj.plank_teak" && ctx.inventory === player.inv
            override fun contains(ctx: PlayerItemStorageContext) = storedPlanks
            override fun add(ctx: PlayerItemStorageContext, amount: Int) = 0
            override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int { val n = minOf(amount, storedPlanks); storedPlanks -= n; return n }
        }))
        val producer = LocInteractions(BoundValidator(map), bus)
        val useProducer = LocUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }.newInstance(bus)
        val random = mock(GameRandom::class.java)
        val launcher: ProtectedAccessLauncher; val queues: PlayerQueueProcessor
        init {
            for (l in 0..3)for (x in origin.x - 32..origin.x + 32 step 8)for (z in origin.z - 32..origin.z + 32 step 8)map.allocateIfAbsent(x, z, l)
            for (route in ShortcutRoutes.all) map.allocateIfAbsent(route.destination.x, route.destination.z, route.destination.level)
            `when`(random.of(25)).thenReturn(1)
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getCollision = { map }, getAreaChecker = { mock(AreaChecker::class.java) }, getAlignment = { TextAlignment() }, getRandom = { random })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context)
            launcher = ProtectedAccessLauncher(factory); queues = PlayerQueueProcessor(bus, launcher)
            with(InvTransactionsScript(storage)) { ctx.startup() }
            with(ShortcutScript(map, XpModifiers(emptySet()))) { ctx.startup() }
            with(ShortcutSetupScript(storage, XpModifiers(emptySet()))) { ctx.startup() }
            with(org.rsmod.content.areas.city.lumbridge.BrokenRaft()) { ctx.startup() }
            with(org.rsmod.content.areas.misc.motherlode.scripts.MotherlodeShortcutScript()) { ctx.startup() }
            with(org.rsmod.content.areas.city.portsarim.PortSarimLocScript(mock(org.rsmod.api.repo.loc.LocRepository::class.java))) { ctx.startup() }
        }
        fun set(symbol: String, value: Int) = VarPlayerIntMapSetter.set(player, symbol, value)
        fun bound(symbol: String, coords: CoordGrid) = BoundLocInfo(LocInfo(2, coords, LocEntity(symbol.asRSCM(), 10, 0)), ServerCacheManager.getObject(symbol.asRSCM())!!)
        fun op(symbol: String, coords: CoordGrid) = option(symbol, coords, InteractionOp.Op1)
        fun option(symbol: String, coords: CoordGrid, op: InteractionOp) { val loc = bound(symbol, coords); producer.interact(player, loc, op); val event = checkNotNull(producer.opTrigger(player, loc, op)); launcher.launch(player) { assertTrue(bus.publish(this, event)) } }
        fun ap(symbol: String, coords: CoordGrid) { val loc = bound(symbol, coords); producer.interact(player, loc, InteractionOp.Op1); val event = checkNotNull(producer.apTrigger(player, loc, InteractionOp.Op1)); launcher.launch(player) { assertTrue(bus.publish(this, event)) } }
        fun use(symbol: String, coords: CoordGrid, slot: Int) { val loc = bound(symbol, coords); val type = ServerCacheManager.getItem(player.inv[slot]!!.id)!!; launcher.launch(player) { useProducer.interactOp(this, loc, loc, ServerCacheManager.getObject(loc.id)!!, type, inv, slot) } }
        fun advance(ticks: Int) { repeat(ticks) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance(); queues.process(player) } }
        fun finish() = advance(14)
        fun bridgeMaterials() { player.inv.fillNulls(); player.inv[0] = InvObj("obj.hammer"); for (i in 1..6)player.inv[i] = InvObj("obj.plank_teak"); player.inv[7] = InvObj("obj.nails_mithril", 28) }
    }
    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
        val client = object : Client<Any, Any> { override fun write(message: Any) = Unit; override fun close() = Unit; override fun read(player: Player) = Unit; override fun flush() = Unit; override fun flushHighPriority() = Unit; override fun unregister(service: Any, player: Player) = Unit }
    }
}
