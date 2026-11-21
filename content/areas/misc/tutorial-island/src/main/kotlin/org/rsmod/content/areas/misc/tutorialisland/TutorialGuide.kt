package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.HintArrowPosition
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal class TutorialGuide
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val eventBus: EventBus,
) {

    fun advance(player: Player, stage: Int) {
        val current = player.tutorialStage
        if (stage <= current) {
            return
        }
        player.tutorialStage = stage
        apply(player, stage)
    }

    fun apply(player: Player, stage: Int) {
        progress(stage)?.let { player.tutorialOverlayProgress = it }
        player.flashSide = FLASH_SIDE[stage] ?: 0
        TutorialTabs.sync(player, eventBus)
        show(player, stage)
    }

    fun show(player: Player, stage: Int) {
        if (stage == TutorialStage.QUEST_HOUSE_ENTERED && player.attr[RUN_TOGGLED] == true) {
            player.openMesOverlay()
            player.runClientScript(MESOVERLAY, MOVING_ON_RUN_TEXT)
            clearArrow(player)
            return
        }
        val hint = HINTS[stage] ?: return
        player.openMesOverlay()
        player.runClientScript(MESOVERLAY, hint.text ?: " ")
        if (hint.hintId != null) {
            player.runClientScript(OVERLAY_HINT, hint.hintId)
        } else {
            player.ifSetHide("component.tutorial_overlay:hint_layer", true)
        }
        clearArrow(player)
        when (val arrow = hint.arrow) {
            is Arrow.Npc -> {
                val npc =
                    npcRepo.findAll(ZoneKey.from(arrow.coords), ARROW_NPC_ZONE_RADIUS)
                        .filter {
                            it.type.internalName == arrow.name ||
                                it.visType.internalName == arrow.name
                        }
                        .minByOrNull { it.coords.chebyshevDistance(arrow.coords) }
                if (npc != null) {
                    MiscOutput.hintArrowNpc(player, npc.slotId)
                    player.currentHintArrow = 1
                }
            }
            is Arrow.Tile -> {
                val position = TILE_POSITIONS[stage] ?: HintArrowPosition.CENTER
                MiscOutput.hintArrowTile(player, arrow.coords, arrow.height, position)
                player.currentHintArrow = 1
            }
            Arrow.None -> Unit
            null -> Unit
        }
    }

    fun clearArrow(player: Player) {
        MiscOutput.hintArrowReset(player)
        player.currentHintArrow = 0
    }

    fun showSmithingMenuOpened(player: Player) {
        player.openMesOverlay()
        player.runClientScript(MESOVERLAY, SMITHING_MENU_OPENED_TEXT)
    }

    private fun Player.openMesOverlay() {
        ifOpenOverlay("interface.mesoverlay", "component.chatbox:chatoverlay", eventBus)
    }

    fun clear(player: Player) {
        clearArrow(player)
        player.flashSide = 0
        player.ifSetHide("component.tutorial_overlay:hint_layer", true)
        player.ifCloseSub("interface.tutorial_overlay", eventBus)
        player.runClientScript(TUTORIAL_END)
    }

    private fun progress(stage: Int): Int? =
        when (stage) {
            TutorialStage.OUTSIDE -> 2
            TutorialStage.SKILLS_VIEWED -> 3
            TutorialStage.GATE_OPENED -> 4
            TutorialStage.DOUGH_MADE -> 5
            TutorialStage.BREAD_BAKED -> 6
            TutorialStage.QUEST_HOUSE_ENTERED -> 7
            TutorialStage.QUEST_LIST_OPENED -> 8
            TutorialStage.COPPER_MINED -> 9
            TutorialStage.DAGGER_SMITHED -> 10
            TutorialStage.COMBAT_TAB_OPENED -> 11
            TutorialStage.RANGED_GIVEN -> 12
            TutorialStage.COMBAT_GUIDE_DONE -> 14
            TutorialStage.POLL_BOOTH -> 15
            TutorialStage.ACCOUNT_GUIDE_DONE -> 16
            TutorialStage.PRAYER_GUIDE_TALKED_2 -> 17
            TutorialStage.PRAYER_GUIDE_DONE -> 18
            TutorialStage.MAGIC_BOOK_OPENED -> 19
            TutorialStage.RUNES_GIVEN -> 20
            else -> null
        }

    private sealed interface Arrow {
        data class Npc(val name: String, val coords: CoordGrid) : Arrow

        data class Tile(val coords: CoordGrid, val height: Int) : Arrow

        data object None : Arrow
    }

    private data class Hint(
        val text: String? = null,
        val hintId: Int? = null,
        val arrow: Arrow? = null,
    )

    private companion object {
        private const val ARROW_NPC_ZONE_RADIUS = 2
        private val MESOVERLAY = "clientscript.[clientscript,mesoverlay]".asRSCM(RSCMType.CLIENTSCRIPT)
        private val OVERLAY_HINT =
            "clientscript.[clientscript,tutorial_overlay_hint]".asRSCM(RSCMType.CLIENTSCRIPT)
        private val TUTORIAL_END =
            "clientscript.[clientscript,tutorial_end]".asRSCM(RSCMType.CLIENTSCRIPT)
        private const val MOVING_ON_RUN_TEXT =
            "<col=0000ff>Moving on</col><br>Follow the path to the next guide. When you get " +
                "there, click on the door to pass through it. Remember, you can use your arrow " +
                "keys to rotate the camera."
        private const val SMITHING_MENU_OPENED_TEXT =
            "<col=0000ff>Smithing a dagger</col><br>Now you have the smithing menu open, you " +
                "will see a list of all the things you can make. Only the dagger can be made at " +
                "your skill level; this is shown by the white text under it. You'll need to " +
                "select the dagger to continue."

        private val TILE_POSITIONS: Map<Int, HintArrowPosition> =
            mapOf(
                TutorialStage.GUIDE_DONE to HintArrowPosition.WEST,
                TutorialStage.AXE_GIVEN to HintArrowPosition.EAST,
                TutorialStage.SHRIMP_COOKED to HintArrowPosition.EAST,
                TutorialStage.GATE_OPENED to HintArrowPosition.WEST,
                TutorialStage.DOUGH_MADE to HintArrowPosition.EAST,
                TutorialStage.BREAD_BAKED to HintArrowPosition.EAST,
                TutorialStage.QUEST_HOUSE_ENTERED to
                    HintArrowPosition.SOUTH,
                TutorialStage.QUEST_GUIDE_DONE to
                    HintArrowPosition.CENTER,
                TutorialStage.MINING_GUIDE_TALKED to
                    HintArrowPosition.CENTER,
                TutorialStage.TIN_MINED to HintArrowPosition.CENTER,
                TutorialStage.COPPER_MINED_FIRST to HintArrowPosition.CENTER,
                TutorialStage.COPPER_MINED to HintArrowPosition.CENTER,
                TutorialStage.MINING_GUIDE_BAR to
                    HintArrowPosition.CENTER,
                TutorialStage.ANVIL_OPENED to HintArrowPosition.CENTER,
                TutorialStage.DAGGER_SMITHED to HintArrowPosition.EAST,
                TutorialStage.COMBAT_TAB_OPENED to
                    HintArrowPosition.EAST,
                TutorialStage.COMBAT_GUIDE_DONE to
                    HintArrowPosition.CENTER,
                TutorialStage.LADDER_UP to HintArrowPosition.CENTER,
                TutorialStage.BANK_OPENED to HintArrowPosition.CENTER,
                TutorialStage.POLL_BOOTH to HintArrowPosition.WEST,
                TutorialStage.ACCOUNT_GUIDE_DONE to
                    HintArrowPosition.WEST,
                TutorialStage.PRAYER_GUIDE_DONE to
                    HintArrowPosition.WEST,
            )

        private val FLASH_SIDE: Map<Int, Int> =
            mapOf(
                TutorialStage.SETTINGS_REQUESTED to 12,
                TutorialStage.NET_GIVEN to 4,
                TutorialStage.SHRIMP_CAUGHT to 2,
                TutorialStage.QUEST_GUIDE_TALKED to 3,
                TutorialStage.DAGGER_EQUIP_TASK to 5,
                TutorialStage.SWORD_EQUIPPED to 1,
                TutorialStage.ACCOUNT_GUIDE_TALKED to 9,
                TutorialStage.PRAYER_GUIDE_TALKED to 6,
                TutorialStage.MAGIC_GUIDE_TALKED to 7,
                TutorialStage.LEFT_ISLAND to 3,
            )

        private val HINTS: Map<Int, Hint> =
            mapOf(
                TutorialStage.EXPERIENCE_SELECTED to
                    Hint(
                        text =
                            "<col=0000ff>Getting started</col><br>When you're ready to get " +
                                "started, click on the Gielinor Guide. He is indicated by a " +
                                "flashing yellow arrow.",
                        hintId = 4,
                        arrow = Arrow.Npc(TutorialNpc.GIELINOR_GUIDE, CoordGrid(3096, 3107, 0)),
                    ),
                TutorialStage.SETTINGS_REQUESTED to
                    Hint(
                        text =
                            "<col=0000ff>Settings menu</col><br>Please click on the flashing " +
                                "spanner icon found at the bottom right of your screen. This will " +
                                "display your settings menu.",
                    ),
                TutorialStage.SETTINGS_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Settings menu</col><br>On the side panel, you can now " +
                                "see a variety of game settings. You can also click the all " +
                                "settings button to see all available settings. Talk to the " +
                                "Gielinor Guide to continue.",
                        arrow = Arrow.Npc(TutorialNpc.GIELINOR_GUIDE, CoordGrid(3096, 3107, 0)),
                    ),
                TutorialStage.GUIDE_DONE to
                    Hint(
                        text =
                            "<col=0000ff>Moving on</col><br>It's time to meet your first " +
                                "instructor. To continue, all you need to do is click on the " +
                                "door. It's indicated by a flashing yellow arrow. Remember, you " +
                                "can use your arrow keys to rotate the camera.",
                        hintId = 1,
                        arrow = Arrow.Tile(CoordGrid(3098, 3107, 0), 128),
                    ),
                TutorialStage.OUTSIDE to
                    Hint(
                        hintId = 6,
                        arrow = Arrow.Npc(TutorialNpc.SURVIVAL_EXPERT, CoordGrid(3103, 3095, 0)),
                    ),
                TutorialStage.NET_GIVEN to
                    Hint(
                        text =
                            "<col=0000ff>You've been given an item</col><br>To view the item " +
                                "you've been given, you'll need to open your inventory. To do so, " +
                                "click on the flashing backpack icon to the right hand side of " +
                                "your screen.",
                    ),
                TutorialStage.INVENTORY_VIEWED to
                    Hint(
                        text =
                            "<col=0000ff>Inventory</col><br>This is your inventory. You can view " +
                                "all of your items here, including the net you've just been " +
                                "given. Let's use it to catch some shrimp.",
                        hintId = 7,
                        arrow = Arrow.Npc(TutorialNpc.FISHING_SPOT, CoordGrid(3101, 3092, 0)),
                    ),
                TutorialStage.SHRIMP_CAUGHT to
                    Hint(
                        text =
                            "<col=0000ff>You've gained some experience</col><br>Click on the " +
                                "flashing bar graph icon near the inventory button to see your " +
                                "skills menu.",
                        hintId = 8,
                    ),
                TutorialStage.SKILLS_VIEWED to
                    Hint(
                        text =
                            "<col=0000ff>Skills and Experience</col><br>On this menu you can " +
                                "view your skills. Your skills can be leveled up by earning " +
                                "experience. As you level up your skills, you will earn new " +
                                "unlocks. Speak to the survival expert to continue.",
                        arrow = Arrow.Npc(TutorialNpc.SURVIVAL_EXPERT, CoordGrid(3103, 3094, 0)),
                    ),
                TutorialStage.AXE_GIVEN to
                    Hint(
                        text =
                            "<col=0000ff>Woodcutting</col><br>It's time to cook your shrimp. " +
                                "However, you require a fire to do that which means you need some " +
                                "logs. You can cut down trees using your Woodcutting skill, all " +
                                "you need is an axe.",
                        hintId = 9,
                        arrow = Arrow.Tile(CoordGrid(3099, 3095, 0), 160),
                    ),
                TutorialStage.LOGS_CUT to
                    Hint(
                        text =
                            "<col=0000ff>Firemaking</col><br>Now that you have some logs, it's " +
                                "time to light a fire. First, click on the tinderbox in your " +
                                "inventory. Then, with the tinderbox highlighted, click on the " +
                                "logs to use the tinderbox on them.",
                        hintId = 5,
                    ),
                TutorialStage.FIRE_LIT to
                    Hint(
                        text =
                            "<col=0000ff>Cooking</col><br>Now it's time to get cooking. To do " +
                                "so, click on the shrimp in your inventory. Then, with the shrimp " +
                                "highlighted, click on a fire to cook them. If you look at the " +
                                "top left of the screen, you'll see the instructions that you're " +
                                "giving to your character.",
                        hintId = 10,
                    ),
                TutorialStage.SHRIMP_COOKED to
                    Hint(
                        text =
                            "<col=0000ff>Moving on</col><br>Well done, you've just cooked your " +
                                "first meal! Speak to the survival expert if you want a recap, " +
                                "otherwise you can move on. Click on the gate shown and follow " +
                                "the path. Remember, you can use your arrow keys to rotate the " +
                                "camera.",
                        arrow = Arrow.Tile(CoordGrid(3089, 3092, 0), 65),
                    ),
                TutorialStage.GATE_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Moving on</col><br>Follow the path until you get to " +
                                "the door with the yellow arrow above it. Click on the door to " +
                                "open it. If you can't find it, try moving your camera by " +
                                "clicking and dragging your middle mouse button.",
                        arrow = Arrow.Tile(CoordGrid(3079, 3084, 0), 128),
                    ),
                TutorialStage.KITCHEN_ENTERED to
                    Hint(
                        text =
                            "<col=0000ff>Cooking</col><br>Talk to the chef to learn the more " +
                                "advanced aspects of Cooking such as combining ingredients!",
                        arrow = Arrow.Npc(TutorialNpc.MASTER_CHEF, CoordGrid(3074, 3086, 0)),
                    ),
                TutorialStage.CHEF_TALKED to
                    Hint(
                        text =
                            "<col=0000ff>Making dough</col><br>This is the base for many meals. " +
                                "To make dough you must mix flour with water. To do so, click on " +
                                "the flour in your inventory. Then, with the flour highlighted, " +
                                "click on the water to combine them into dough.",
                    ),
                TutorialStage.DOUGH_MADE to
                    Hint(
                        text =
                            "<col=0000ff>Cooking dough</col><br>Now you have made the dough, you " +
                                "can bake it into some bread. To do so, just click on the " +
                                "indicated range.",
                        hintId = 11,
                        arrow = Arrow.Tile(CoordGrid(3075, 3081, 0), 65),
                    ),
                TutorialStage.BREAD_BAKED to
                    Hint(
                        text =
                            "<col=0000ff>Moving on</col><br>Well done! You've baked your first " +
                                "loaf of bread. As you gain experience in Cooking, you will be " +
                                "able to make other things like pies and cakes. You can now use " +
                                "the next door to move on. If you need a recap on anything, talk " +
                                "to the master chef.",
                        hintId = 21,
                        arrow = Arrow.Tile(CoordGrid(3072, 3090, 0), 128),
                    ),
                TutorialStage.QUEST_HOUSE_ENTERED to
                    Hint(
                        text =
                            "<col=0000ff>Fancy a run?</col><br>When navigating the world, you " +
                                "can either run or walk. Running is faster but you can't run for " +
                                "long as you'll soon run out of energy. You can use the flashing " +
                                "orb next to the minimap to toggle running. Why not try it as you " +
                                "head to the next section?",
                        arrow = Arrow.Tile(CoordGrid(3086, 3126, 0), 128),
                    ),
                TutorialStage.QUEST_HOUSE_INSIDE to
                    Hint(
                        text =
                            "<col=0000ff>Quests</col><br>It's time to learn about quests! Just " +
                                "talk to the Quest Guide to get started.",
                        arrow = Arrow.Npc(TutorialNpc.QUEST_GUIDE, CoordGrid(3087, 3123, 0)),
                    ),
                TutorialStage.QUEST_GUIDE_TALKED to
                    Hint(
                        text =
                            "<col=0000ff>Quest journal</col><br>Click on the flashing icon to " +
                                "the left of your inventory.",
                    ),
                TutorialStage.QUEST_LIST_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Quest journal</col><br>This is your quest journal. It " +
                                "lists every quest in the game. Talk to the quest guide again for " +
                                "an explanation on how it works.",
                        arrow = Arrow.Npc(TutorialNpc.QUEST_GUIDE, CoordGrid(3087, 3123, 0)),
                    ),
                TutorialStage.QUEST_GUIDE_DONE to
                    Hint(
                        text =
                            "<col=0000ff>Moving on</col><br>It's time to enter some caves. Click " +
                                "on the ladder to go down to the next area.",
                        arrow = Arrow.Tile(CoordGrid(3088, 3119, 0), 5),
                    ),
                TutorialStage.LADDER_DOWN to
                    Hint(
                        text =
                            "<col=0000ff>Mining and Smithing</col><br>Next let's get you a " +
                                "weapon, or more to the point, you can make your first weapon " +
                                "yourself. Don't panic, the mining instructor will help you. " +
                                "Talk to him and he'll tell you all about it.",
                        arrow = Arrow.Npc(TutorialNpc.MINING_INSTRUCTOR, CoordGrid(3083, 9504, 0)),
                    ),
                TutorialStage.MINING_GUIDE_TALKED to
                    Hint(
                        text =
                            "<col=0000ff>Mining</col><br>It's quite simple really. To mine a " +
                                "rock, all you need to do is click on it. First up, try mining " +
                                "some tin.",
                        hintId = 12,
                        arrow = Arrow.Tile(CoordGrid(3077, 9504, 0), 45),
                    ),
                TutorialStage.TIN_MINED to
                    Hint(
                        text =
                            "<col=0000ff>Mining</col><br>Now that you have some tin ore, you " +
                                "just need some copper. To mine a rock, all you need to do is " +
                                "click on it.",
                        hintId = 12,
                        arrow = Arrow.Tile(CoordGrid(3083, 9501, 0), 45),
                    ),
                TutorialStage.COPPER_MINED_FIRST to
                    Hint(
                        text =
                            "<col=0000ff>Mining</col><br>Now that you have some copper ore, you " +
                                "just need some tin. To mine a rock, all you need to do is " +
                                "click on it.",
                        hintId = 12,
                        arrow = Arrow.Tile(CoordGrid(3077, 9504, 0), 45),
                    ),
                TutorialStage.COPPER_MINED to
                    Hint(
                        text =
                            "<col=0000ff>Smelting</col><br>You now have some tin ore and some " +
                                "copper ore. You can smelt these into a bronze bar. To do so, " +
                                "just click on the indicated furnace. Try it now.",
                        hintId = 13,
                        arrow = Arrow.Tile(CoordGrid(3079, 9496, 0), 128),
                    ),
                TutorialStage.BAR_SMELTED to
                    Hint(
                        text =
                            "<col=0000ff>Smelting</col><br>You've made a bronze bar! Speak to " +
                                "the mining instructor and he'll show you how to make it into a " +
                                "weapon.",
                        arrow = Arrow.Npc(TutorialNpc.MINING_INSTRUCTOR, CoordGrid(3083, 9504, 0)),
                    ),
                TutorialStage.MINING_GUIDE_BAR to
                    Hint(
                        text =
                            "<col=0000ff>Smithing a dagger</col><br>To smith you'll need a " +
                                "hammer and enough metal bars to make the desired item, as well " +
                                "as a handy anvil. To start the process, click on the anvil, or " +
                                "alternatively use the bar on it.",
                        hintId = 14,
                        arrow = Arrow.Tile(CoordGrid(3083, 9499, 0), 50),
                    ),
                TutorialStage.ANVIL_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Smithing a dagger</col><br>Use an anvil to open the " +
                                "smithing menu, in it you will see a list of all the things you " +
                                "can make. Only the dagger can be made at your skill level; this " +
                                "is shown by the white text under it. You'll need to select the " +
                                "dagger to continue.",
                        arrow = Arrow.Tile(CoordGrid(3083, 9499, 0), 50),
                    ),
                TutorialStage.DAGGER_SMITHED to
                    Hint(
                        text =
                            "<col=0000ff>Moving on</col><br>Congratulations, you've made your " +
                                "first weapon. Now it's time to move on. Go through the gates " +
                                "shown by the arrow. Remember, you may need to move the camera " +
                                "to see your surroundings. Speak to the mining instructor for a " +
                                "recap at any time.",
                        arrow = Arrow.Tile(CoordGrid(3094, 9502, 0), 128),
                    ),
                TutorialStage.COMBAT_GATE to
                    Hint(
                        text =
                            "<col=0000ff>Combat</col><br>In this area you will find out about " +
                                "melee and ranged combat. Speak to the guide and he will tell you " +
                                "all about it.",
                        arrow = Arrow.Npc(TutorialNpc.COMBAT_INSTRUCTOR, CoordGrid(3106, 9510, 0)),
                    ),
                TutorialStage.DAGGER_EQUIP_TASK to
                    Hint(
                        text =
                            "<col=0000ff>Equipping items</col><br>You now have access to a new " +
                                "interface. Click on the flashing icon of a man, the one to the " +
                                "right of your backpack icon.",
                    ),
                TutorialStage.WORN_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Worn inventory</col><br>This is your worn inventory. " +
                                "Here you can see what items you have equipped. In the bottom " +
                                "left corner, you will notice a flashing button with a shield " +
                                "and helmet on it. This button lets you view more details on " +
                                "what you have equipped. Click on it now.",
                    ),
                TutorialStage.EQUIPMENT_STATS_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Equipment stats</col><br>You can see what items you " +
                                "are wearing in the worn inventory to the left of the screen, " +
                                "with their combined statistics on the right. Let's add " +
                                "something. Click your dagger to equip it.",
                    ),
                TutorialStage.DAGGER_EQUIPPED to
                    Hint(
                        text =
                            "<col=0000ff>Equipment stats</col><br>You're now holding your " +
                                "dagger. Clothes, armour, weapons and more are equipped like " +
                                "this. Alternatively, you can click items directly from your " +
                                "inventory to equip them without opening the stats window. Speak " +
                                "to the combat instructor to continue.",
                        arrow = Arrow.Npc(TutorialNpc.COMBAT_INSTRUCTOR, CoordGrid(3106, 9510, 0)),
                    ),
                TutorialStage.COMBAT_GUIDE_SWORD to
                    Hint(
                        text =
                            "<col=0000ff>Unequipping items</col><br>To unequip an item, go to " +
                                "your worn inventory and click on the item. Alternatively, " +
                                "equipping a new item into the same slot will unequip the old " +
                                "one. Try this out now by swapping your dagger for the sword and " +
                                "shield that the combat instructor gave you.",
                    ),
                TutorialStage.SWORD_EQUIPPED to
                    Hint(
                        text =
                            "<col=0000ff>Combat interface</col><br>Click on the flashing " +
                                "crossed swords icon to open the combat interface.",
                    ),
                TutorialStage.COMBAT_TAB_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Combat interface</col><br>This is your combat " +
                                "interface. From here, you can select the attack style that " +
                                "you'll use in combat. Using different attack styles will give " +
                                "different types of experience. As well as this, monsters are " +
                                "weak to specific attack styles. Click on the gates to continue.",
                        hintId = 15,
                        arrow = Arrow.Tile(CoordGrid(3110, 9518, 0), 128),
                    ),
                TutorialStage.COMBAT_TAB_CLOSED to
                    Hint(
                        text =
                            "<col=0000ff>Attacking</col><br>It's time to slay some rats! To " +
                                "attack a rat, all you have to do is click on it. This will " +
                                "cause you to walk over and start hitting it.",
                        hintId = 15,
                        arrow = Arrow.Npc(TutorialNpc.GIANT_RAT, CoordGrid(3108, 9519, 0)),
                    ),
                TutorialStage.RAT_ENGAGED to
                    Hint(
                        text =
                            "<col=0000ff>Sit back and watch</col><br>While you are fighting you " +
                                "will see a bar over your head. The bar shows how much health you " +
                                "have left. Your opponent will have one too. You will continue to " +
                                "attack the rat until it's dead or you do something else.",
                    ),
                TutorialStage.RAT_KILLED_MELEE to
                    Hint(
                        text =
                            "<col=0000ff>Well done, you've made your first kill!</col><br>Pass " +
                                "through the gate and talk to the combat instructor. He will " +
                                "give you your next task.",
                        arrow = Arrow.Npc(TutorialNpc.COMBAT_INSTRUCTOR, CoordGrid(3104, 9509, 0)),
                    ),
                TutorialStage.RANGED_GIVEN to
                    Hint(
                        text =
                            "<col=0000ff>Rat ranging</col><br>Before you can use the shortbow " +
                                "and arrows you'll need to equip them by clicking on them. Once " +
                                "equipped with the ranging gear, try killing another rat. You " +
                                "don't need to enter the pen this time. To attack a rat, just " +
                                "click on it.",
                        hintId = 16,
                        arrow = Arrow.Npc(TutorialNpc.GIANT_RAT, CoordGrid(3106, 9519, 0)),
                    ),
                TutorialStage.RAT_ENGAGED_RANGED to
                    Hint(
                        text =
                            "<col=0000ff>Rat ranging</col><br>Before you can use the shortbow " +
                                "and arrows you'll need to equip them by clicking on them. Once " +
                                "equipped with the ranging gear, try killing another rat. You " +
                                "don't need to enter the pen this time. To attack a rat, just " +
                                "click on it.",
                        hintId = 16,
                        arrow = Arrow.Npc(TutorialNpc.GIANT_RAT, CoordGrid(3106, 9519, 0)),
                    ),
                TutorialStage.COMBAT_GUIDE_DONE to
                    Hint(
                        text =
                            "<col=0000ff>Moving on</col><br>You have completed the tasks here. " +
                                "To move on, click on the indicated ladder. If you need to go " +
                                "over any of what you learnt here, just talk to the combat " +
                                "instructor and he'll tell you what he can.",
                        hintId = 22,
                        arrow = Arrow.Tile(CoordGrid(3111, 9531, 0), 128),
                    ),
                TutorialStage.LADDER_UP to
                    Hint(
                        text =
                            "<col=0000ff>Banking</col><br>This is the Bank of Gielinor, where " +
                                "you can store all your most valued items. To open your bank, " +
                                "just click on the indicated booth.",
                        arrow = Arrow.Tile(CoordGrid(3122, 3124, 0), 128),
                    ),
                TutorialStage.BANK_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Banking</col><br>This is your bank. You can store " +
                                "things here for safekeeping. To deposit something from your " +
                                "inventory, just click on it. You can withdraw things in the " +
                                "same way. To continue, close the bank and click on the " +
                                "indicated poll booth.",
                        arrow = Arrow.Tile(CoordGrid(3119, 3121, 0), 128),
                    ),
                TutorialStage.POLL_BOOTH to
                    Hint(
                        text =
                            "<col=0000ff>Moving on</col><br>Polls are run periodically to let " +
                                "the Old School RuneScape community vote on how the game should " +
                                "- or shouldn't - change. When you're ready, move on through the " +
                                "door indicated.",
                        arrow = Arrow.Tile(CoordGrid(3125, 3124, 0), 128),
                    ),
                TutorialStage.ACCOUNT_GUIDE_ARRIVED to
                    Hint(
                        text =
                            "<col=0000ff>Account Management</col><br>The guide here will tell " +
                                "you all about your account. Just click on him to hear what " +
                                "he's got to say.",
                        arrow = Arrow.Npc(TutorialNpc.ACCOUNT_GUIDE, CoordGrid(3127, 3123, 0)),
                    ),
                TutorialStage.ACCOUNT_GUIDE_TALKED to
                    Hint(
                        text =
                            "<col=0000ff>Account Management</col><br>Click on the flashing " +
                                "icon to open your Account Management menu.",
                    ),
                TutorialStage.ACCOUNT_MENU_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Account Management</col><br>This is your Account " +
                                "Management menu where you can control various aspects of your " +
                                "account. Talk to the Account Guide to learn more.",
                        arrow = Arrow.Npc(TutorialNpc.ACCOUNT_GUIDE, CoordGrid(3126, 3123, 0)),
                    ),
                TutorialStage.ACCOUNT_GUIDE_DONE to
                    Hint(
                        text = "<col=0000ff>Moving on</col><br>Continue through the next door.",
                        hintId = 17,
                        arrow = Arrow.Tile(CoordGrid(3130, 3124, 0), 128),
                    ),
                TutorialStage.CHAPEL_ARRIVED to
                    Hint(
                        arrow = Arrow.Npc(TutorialNpc.BROTHER_BRACE, CoordGrid(3127, 3107, 0)),
                    ),
                TutorialStage.PRAYER_GUIDE_TALKED to
                    Hint(
                        text =
                            "<col=0000ff>Prayer menu</col><br>Click on the flashing icon to " +
                                "open the Prayer menu.",
                    ),
                TutorialStage.PRAYER_BOOK_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Prayer menu</col><br>Talk with Brother Brace and " +
                                "he'll tell you about prayers.",
                        arrow = Arrow.Npc(TutorialNpc.BROTHER_BRACE, CoordGrid(3127, 3107, 0)),
                    ),
                TutorialStage.PRAYER_GUIDE_DONE to
                    Hint(
                        text =
                            "<col=0000ff>Your final instructor!<br>You're almost " +
                                "finished on tutorial island. Pass through the door to find the " +
                                "path leading to your final instructor.",
                        hintId = 18,
                        arrow = Arrow.Tile(CoordGrid(3122, 3102, 0), 128),
                    ),
                TutorialStage.MAGIC_GUIDE_ARRIVED to
                    Hint(
                        text =
                            "<col=0000ff>Your final instructor!</col><br>Follow the path to " +
                                "the wizard's house, where you will be shown how to cast " +
                                "spells. When you get there, just talk with the magic " +
                                "instructor.",
                        arrow = Arrow.Npc(TutorialNpc.MAGIC_INSTRUCTOR, CoordGrid(3141, 3088, 0)),
                    ),
                TutorialStage.MAGIC_GUIDE_TALKED to
                    Hint(
                        text =
                            "<col=0000ff>Open up your final menu</col><br>Open up the magic " +
                                "interface by clicking on the flashing icon.",
                    ),
                TutorialStage.MAGIC_BOOK_OPENED to
                    Hint(
                        text =
                            "<col=0000ff>Magic</col><br>This is your magic interface. All of " +
                                "your spells can be found here. The list is currently filtered " +
                                "to only show the spells you have the Magic level for. You can " +
                                "change this in the filter menu. Talk to the instructor to " +
                                "continue.",
                        arrow = Arrow.Npc(TutorialNpc.MAGIC_INSTRUCTOR, CoordGrid(3141, 3088, 0)),
                    ),
                TutorialStage.RUNES_GIVEN to
                    Hint(
                        text =
                            "<col=0000ff>Magic casting</col><br>You now have some runes. All " +
                                "spells require runes to cast them. Look for the Wind Strike " +
                                "spell in your magic interface. Click on this spell to select " +
                                "it and then click on a chicken to cast it. Talk to the " +
                                "instructor if you need more runes.",
                        hintId = 19,
                        arrow = Arrow.Npc(TutorialNpc.CHICKEN, CoordGrid(3139, 3094, 0)),
                    ),
                TutorialStage.WIND_STRIKE_CAST to
                    Hint(
                        text =
                            "<col=0000ff>Magic casting</col><br>You cast a spell! Sadly it " +
                                "didn't have any effect this time, but the more you practice, " +
                                "the better you'll get. Let's try again. Click on the spell to " +
                                "select it and then click on a chicken to cast it. Talk to the " +
                                "instructor if you need more runes.",
                        hintId = 19,
                    ),
                TutorialStage.WIND_STRIKE_RESOLVED to
                    Hint(
                        text =
                            "<col=0000ff>To the mainland!</col><br>You're nearly finished " +
                                "with the tutorial. All you need to do now is move on to the " +
                                "mainland. Just speak with the magic instructor to continue.",
                        hintId = 20,
                        arrow = Arrow.Npc(TutorialNpc.MAGIC_INSTRUCTOR, CoordGrid(3141, 3088, 0)),
                    ),
                TutorialStage.COMPLETE to
                    Hint(
                        text =
                            "<col=0000ff>To the mainland!</col><br>You're nearly finished " +
                                "with the tutorial. All you need to do now is move on to the " +
                                "mainland. Cast your Home Teleport spell to be taken to Lumbridge.",
                        hintId = 23,
                    ),
                TutorialStage.READY_TO_LEAVE to
                    Hint(
                        text =
                            "<col=0000ff>To the mainland!</col><br>You're nearly finished " +
                                "with the tutorial. All you need to do now is move on to the " +
                                "mainland. Cast your Home Teleport spell to be taken to Lumbridge.",
                        hintId = 23,
                    ),
            )
    }
}
