package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal var Player.tutorialStage by intVarp("varp.tutorial")
internal var Player.tutorialOverlayProgress by intVarp("varp.tutorial_progress_overlay")
internal var Player.currentHintArrow by intVarBit("varbit.current_hint_arrow")
internal var Player.hasDisplayNameTransmitter by boolVarBit("varbit.has_displayname_transmitter")
internal var Player.displayNameStatus by intVarBit("varbit.displayname_status")
internal var Player.playerDesignBodyType by intVarBit("varbit.player_design_bodytype")
internal var Player.transmitPronouns by intVarBit("varbit.settings_transmit_pronouns")
internal var Player.tutorialFreeStuffCheck by intVarBit("varbit.tutorial_freestuff_check")
internal var Player.adventurePaths by intVarBit("varbit.adventurepath_player_participating")

internal val Player.inTutorial: Boolean
    get() = tutorialStage in TutorialStage.START until TutorialStage.LEFT_ISLAND

internal fun Player.advanceTutorial(stage: Int) {
    if (tutorialStage < stage) {
        tutorialStage = stage
    }
}

internal object TutorialStage {
    const val START = 1
    const val EXPERIENCE_SELECTED = 2
    const val SETTINGS_REQUESTED = 3
    const val SETTINGS_OPENED = 7
    const val GUIDE_DONE = 10
    const val OUTSIDE = 20
    const val NET_GIVEN = 30
    const val INVENTORY_VIEWED = 40
    const val SHRIMP_CAUGHT = 50
    const val SKILLS_VIEWED = 60
    const val AXE_GIVEN = 70
    const val LOGS_CUT = 80
    const val FIRE_LIT = 90
    const val SHRIMP_COOKED = 120
    const val GATE_OPENED = 130
    const val KITCHEN_ENTERED = 140
    const val CHEF_TALKED = 150
    const val DOUGH_MADE = 160
    const val BREAD_BAKED = 170
    const val QUEST_HOUSE_DOOR = 183
    const val QUEST_HOUSE_ENTERED = 200
    const val QUEST_LIST_OPENED = 220
    const val QUEST_JOURNAL_READ = 230
    const val QUEST_GUIDE_DONE = 240
    const val LADDER_DOWN = 250
    const val MINING_GUIDE_TALKED = 260
    const val TIN_MINED = 270
    const val COPPER_MINED = 300
    const val BAR_SMELTED = 310
    const val MINING_GUIDE_BAR = 320
    const val DAGGER_SMITHED = 330
    const val ANVIL_OPENED = 340
    const val DAGGER_SELECTED = 350
    const val COMBAT_GATE = 360
    const val COMBAT_GUIDE_TALKED = 370
    const val DAGGER_EQUIPPED = 390
    const val WORN_OPENED = 400
    const val EQUIPMENT_STATS_OPENED = 405
    const val COMBAT_GUIDE_EQUIP = 410
    const val SWORD_EQUIPPED = 420
    const val COMBAT_TAB_OPENED = 430
    const val RAT_PIT_OPENED = 440
    const val RAT_KILLED_MELEE = 460
    const val COMBAT_GUIDE_MELEE = 470
    const val RANGED_GIVEN = 480
    const val RAT_KILLED_RANGED = 490
    const val COMBAT_GUIDE_DONE = 500
    const val BANK_OPENED = 510
    const val BANK_TUTORIAL = 520
    const val POLL_BOOTH = 525
    const val ACCOUNT_GUIDE_TALKED = 530
    const val ACCOUNT_MENU_REQUESTED = 531
    const val ACCOUNT_MENU_OPENED = 532
    const val ACCOUNT_GUIDE_DONE = 540
    const val CHAPEL_ENTERED = 550
    const val PRAYER_GUIDE_TALKED = 560
    const val PRAYER_BOOK_OPENED = 570
    const val PRAYER_GUIDE_DONE = 610
    const val MAGIC_GUIDE_TALKED = 620
    const val MAGIC_BOOK_OPENED = 630
    const val RUNES_GIVEN = 640
    const val WIND_STRIKE_CAST = 650
    const val COMPLETE = 671
    const val READY_TO_LEAVE = 680
    const val LEFT_ISLAND = 1000
}

internal object TutorialCoords {
    val START = CoordGrid(3094, 3107, 0)
    val LUMBRIDGE = CoordGrid(3232, 3232, 0)
    val ADVENTURER_JON = CoordGrid(3232, 3235, 0)
}

internal object TutorialNpc {
    const val GIELINOR_GUIDE = "npc.newbie_basics_instructor"
    const val SURVIVAL_EXPERT = "npc.newbie_survival_instructor"
    const val MASTER_CHEF = "npc.newbie_cook_instructor"
    const val QUEST_GUIDE = "npc.newbie_quest_instructor"
    const val MINING_INSTRUCTOR = "npc.newbie_mining_instructor"
    const val COMBAT_INSTRUCTOR = "npc.newbie_combat_instructor"
    const val ACCOUNT_GUIDE = "npc.newbie_account_instructor"
    const val BROTHER_BRACE = "npc.brother_noob"
    const val MAGIC_INSTRUCTOR = "npc.newbie_magic_instructor"
    const val IRONMAN_TUTOR = "npc.ironman_tutor_2"
    const val BANKER = "npc.noobbanker"
    const val GIANT_RAT = "npc.newbiegiantrat"
    const val CHICKEN = "npc.newbiechicken"
    const val FISHING_SPOT = "npc.0_48_48_newbiefishing"
}

internal object TutorialObj {
    const val NET = "obj.net"
    const val RAW_SHRIMP = "obj.newbieraw_shrimp"
    const val SHRIMP = "obj.newbieshrimp"
    const val LOGS = "obj.newbielogs"
    const val BONES = "obj.newbiebones"
    const val POT_FLOUR = "obj.newbie_pot_flour"
    const val BUCKET_WATER = "obj.bucket_water"
    const val BUCKET_EMPTY = "obj.bucket_empty"
    const val POT_EMPTY = "obj.pot_empty"
    const val BREAD_DOUGH = "obj.bread_dough"
    const val BREAD = "obj.bread"
    const val BRONZE_AXE = "obj.bronze_axe"
    const val BRONZE_PICKAXE = "obj.bronze_pickaxe"
    const val TINDERBOX = "obj.tinderbox"
    const val HAMMER = "obj.hammer"
    const val BRONZE_BAR = "obj.bronze_bar"
    const val BRONZE_DAGGER = "obj.bronze_dagger"
    const val BRONZE_SWORD = "obj.bronze_sword"
    const val WOODEN_SHIELD = "obj.wooden_shield"
    const val SHORTBOW = "obj.shortbow"
    const val BRONZE_ARROW = "obj.bronze_arrow"
    const val AIR_RUNE = "obj.airrune"
    const val MIND_RUNE = "obj.mindrune"
    const val WATER_RUNE = "obj.waterrune"
    const val EARTH_RUNE = "obj.earthrune"
    const val BODY_RUNE = "obj.bodyrune"
    const val COINS = "obj.coins"
}

internal object TutorialLoc {
    const val TREE = "loc.newbietree"
    const val TREE_2 = "loc.newbietree2"
    const val OAK_TREE = "loc.newbieoaktree"
    const val TIN_ROCK = "loc.newbietinrock"
    const val COPPER_ROCK = "loc.newbiecopperrock"
    const val FURNACE = "loc.newbiefurnace"
    const val ANVIL = "loc.anvil"
    const val RANGE = "loc.newbierange"
    const val FIRE = "loc.newbiefire"
    const val BANK_BOOTH = "loc.newbiebankbooth"
    const val LADDER_DOWN = "loc.newbieladdertop1"
    const val LADDER_DOWN_2 = "loc.newbieladdertop2"
    const val LADDER_UP = "loc.newbieladder1"
    const val LADDER_UP_2 = "loc.newbieladder2"
    const val DOOR_1 = "loc.newbie_door1"
    const val DOOR_2 = "loc.newbie_door2"
    const val DOOR_3 = "loc.newbie_door3"
    const val DOOR_4 = "loc.newbie_door4"
    const val DOOR_6 = "loc.newbie_door6"
    const val DOOR_7 = "loc.newbie_door7"
    const val DOOR_8 = "loc.newbie_door8"
    const val DOOR_9 = "loc.newbie_door9"
    const val GATE_LEFT = "loc.newbiegateclosedl2"
    const val GATE_RIGHT = "loc.newbiegateclosedr2"
    const val DOOR_4_LEFT = "loc.newbiedoor4l"
    const val DOOR_4_RIGHT = "loc.newbiedoor4r"
    const val DOOR_5_LEFT = "loc.newbiedoor5_l"
    const val DOOR_5_RIGHT = "loc.newbiedoor5_r"
}
