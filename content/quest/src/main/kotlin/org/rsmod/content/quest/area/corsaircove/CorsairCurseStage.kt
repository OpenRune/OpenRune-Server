package org.rsmod.content.quest.area.corsaircove

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.map.CoordGrid

internal object CorsairCurseStage {
    const val NotStarted: Int = 0

    const val Started: Int = 5

    const val TockSailedAhead: Int = 10

    const val AtTheCove: Int = 15

    const val TheoriesRuledOut: Int = 20

    const val TockToldOfDinner: Int = 25

    const val GnocciNamedIthoi: Int = 30

    const val ArsenNamedMotive: Int = 35

    const val IthoiConfessed: Int = 40

    const val IthoiChallenged: Int = 45

    const val DriftwoodBurning: Int = 49

    const val IthoiExposed: Int = 50

    // Ithoi's bed body resolves to -1 from stage 52 onward and must not be respawned manually.
    const val KillOrdered: Int = 52

    const val IthoiSlain: Int = 55

    const val Complete: Int = 60
}

internal object CorsairCurseCounts {

    const val ColinInterviewed: Int = 1

    const val ColinTelescopeSeen: Int = 2

    const val ColinReassured: Int = 3

    const val GnocciInterviewed: Int = 1

    const val GnocciDollDug: Int = 2

    const val GnocciReassured: Int = 3

    const val ArsenTellingStory: Int = 1

    const val ArsenInterviewed: Int = 2

    const val ArsenRelicTaken: Int = 3

    const val ArsenRelicReturned: Int = 4

    const val ArsenToldOfTess: Int = 5

    const val ArsenReassured: Int = 6

    const val IthoiInterviewed: Int = 1
}

internal var ProtectedAccess.corscursCabinBoy by intVarBit("varbit.corscurs_cabinboy")

internal var ProtectedAccess.corscursCook by intVarBit("varbit.corscurs_cook")

internal var ProtectedAccess.corscursThief by intVarBit("varbit.corscurs_thief")

internal var ProtectedAccess.corscursNavigator by intVarBit("varbit.corscurs_navigator")

internal object CorsairCurseRefs {
    const val QuestKey: String = "quest_corsaircurse"

    // corsair_cove_resource_entry belongs to the resource area, not this quest.
    const val QuestVarp: String = "varp.corscurs"

    const val StageVarbit: String = "varbit.corscurs_progress"

    const val CaptainCrossroads: String = "npc.corsair_captain_crossroads"

    const val Captain1Op: String = "npc.corsair_captain_1op"

    const val FerryRimmington: String = "npc.corsair_ferry_rimmington"

    const val FerryCoveDocks: String = "npc.corsair_ferry_covedocks"

    const val Captain2Ops: String = "npc.corsair_captain_2ops"

    const val CabinBoy2Ops: String = "npc.corscurs_cabinboy_2ops"

    const val CaptainThrone: String = "npc.corsair_captain_throne"

    const val CaptainThroneChild: String = "npc.corsair_captain_throne_child"

    const val NavigatorSick: String = "npc.corscurs_navigator_sick"

    const val Navigator: String = "npc.corscurs_navigator"

    const val CabinBoySick: String = "npc.corscurs_cabinboy_sick"

    const val CabinBoy: String = "npc.corscurs_cabinboy"

    const val CabinBoyTelescope: String = "npc.corscurs_cabinboy_telescope"

    const val CookSick: String = "npc.corscurs_cook_sick"

    const val Cook: String = "npc.corscurs_cook"

    const val CookFishing: String = "npc.corscurs_cook_fishing"

    const val CookDigging: String = "npc.corscurs_cook_digging"

    const val ThiefSick: String = "npc.corscurs_thief_sick"

    const val Thief: String = "npc.corscurs_thief"

    const val ThiefPostquest: String = "npc.corscurs_thief_postquest"

    const val NavigatorCombat: String = "npc.corscurs_navigator_combat"

    const val NavigatorRunning: String = "npc.corscurs_navigator_running"

    const val Tess: String = "npc.ogress_tess"

    const val Ogre: String = "npc.corscurs_ogre"

    const val Relic: String = "obj.corscurs_relic"

    const val Tinderbox: String = "obj.tinderbox"

    const val Spade: String = "obj.spade"

    const val Driftwood: String = "loc.corscurs_driftwood_multi"

    const val Telescope: String = "loc.corscurs_telescope"

    const val SpareTinderbox: String = "loc.raids_icedemon_tinderbox"

    const val SandWithSpade: String = "loc.sand_withspade"

    const val SandWithoutSpade: String = "loc.sand_withoutspade"

    const val CoveGangplank: String = "loc.ds2_corsair_cove_shipplank"

    const val RimmingtonPlankOn: String = "loc.sailing_rimmington_shipplank_on"

    const val RimmingtonPlankOff: String = "loc.sailing_rimmington_shipplank_off"

    val CoveShipDeck: CoordGrid = CoordGrid(2578, 2837, 1)

    val RimmingtonShipDeck: CoordGrid = CoordGrid(2909, 3229, 1)

    val SandCoords: CoordGrid = CoordGrid(2504, 2839, 0)

    val SpareTinderboxCoords: CoordGrid = CoordGrid(2543, 2862, 1)

    val SandWithSpadeCoords: CoordGrid = CoordGrid(2552, 2846, 0)

    val IthoiHut: CoordGrid = CoordGrid(2529, 2839, 1)

    const val RecommendedCombatLevel: Int = 20

    const val CompletionJingle: String = "jingle.high_seas"

    const val SailingJingle: Int = 171

    const val TakeSpadeSound: String = "synth.take_axe"

    const val TakeTinderboxSound: String = "synth.pick2"

    const val DigSound: String = "synth.digspade"

    const val ClockworkSound: String = "synth.poh_clockwork"

    const val SplashSound: String = "synth.watersplash"

    const val SplashSpotanim: String = "spotanim.watersplash"

    const val Doll: String = "npc.corscurs_doll"

    const val FireLitSound: String = "synth.fire_lit"

    const val FireCracklingSound: String = "synth.fire_crackling_med"

    const val WaterBoltCastSound: String = "synth.waterbolt_cast_and_fire"

    const val WaterBoltHitSound: String = "synth.waterbolt_hit"

    const val SteamSound: String = "synth.steam_high"

    const val TelescopeAnim: String = "seq.human_use_wom_telecope"

    const val StopMusic: String = "midi.stop_music"
}
