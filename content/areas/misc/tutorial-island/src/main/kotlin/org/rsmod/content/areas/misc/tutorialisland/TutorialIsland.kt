package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifOpenMainSidePair
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class TutorialIsland
@Inject
constructor(
    private val guide: TutorialGuide,
    private val exit: TutorialExit,
    private val protectedAccess: ProtectedAccessLauncher,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val xpMods: XpModifiers,
    private val eventBus: EventBus,
) : PluginScript() {

    override fun ScriptContext.startup() {
        bindInstructors()
        bindDoors()
        bindObjects()
        bindSkilling()
        bindDamageFloor()
        onPlayerCoordsChanged {
            if (player.tutorialStage == TutorialStage.READY_TO_LEAVE &&
                player.coords.chebyshevDistance(TutorialCoords.LUMBRIDGE) <= 4
            ) {
                protectedAccess.launch(player) { with(exit) { complete() } }
            }
        }
    }

    private fun ScriptContext.bindInstructors() {
        onOpNpc1(TutorialNpc.GIELINOR_GUIDE) { startDialogue(it.npc) { gielinorGuide() } }
        onOpNpc1(TutorialNpc.SURVIVAL_EXPERT) { startDialogue(it.npc) { survivalExpert() } }
        onOpNpc1(TutorialNpc.MASTER_CHEF) { startDialogue(it.npc) { masterChef() } }
        onOpNpc1(TutorialNpc.QUEST_GUIDE) { startDialogue(it.npc) { questGuide() } }
        onOpNpc1(TutorialNpc.MINING_INSTRUCTOR) { startDialogue(it.npc) { miningInstructor() } }
        onOpNpc1(TutorialNpc.COMBAT_INSTRUCTOR) { startDialogue(it.npc) { combatInstructor() } }
        onOpNpc1(TutorialNpc.ACCOUNT_GUIDE) { startDialogue(it.npc) { accountGuide() } }
        onOpNpc1(TutorialNpc.BROTHER_BRACE) { startDialogue(it.npc) { brotherBrace() } }
        onOpNpc1(TutorialNpc.MAGIC_INSTRUCTOR) { startDialogue(it.npc) { magicInstructor() } }
        onOpNpc1(TutorialNpc.IRONMAN_TUTOR) { startDialogue(it.npc) { ironmanTutor() } }
    }

    private fun ScriptContext.bindDoors() {
        onOpLoc1(TutorialLoc.DOOR_1) {
            player.openDoor(it.loc)
            player.advanceIf(TutorialStage.GUIDE_DONE, TutorialStage.OUTSIDE)
        }
        onOpLoc1(TutorialLoc.GATE_LEFT) {
            player.openGate(it.loc, TutorialLoc.GATE_RIGHT)
            player.advanceIf(TutorialStage.SHRIMP_COOKED, TutorialStage.GATE_OPENED)
        }
        onOpLoc1(TutorialLoc.GATE_RIGHT) {
            player.openGate(it.loc, TutorialLoc.GATE_LEFT)
            player.advanceIf(TutorialStage.SHRIMP_COOKED, TutorialStage.GATE_OPENED)
        }
        onOpLoc1(TutorialLoc.DOOR_2) {
            player.openDoor(it.loc)
            player.advanceIf(TutorialStage.GATE_OPENED, TutorialStage.KITCHEN_ENTERED)
        }
        onOpLoc1(TutorialLoc.DOOR_3) {
            player.openDoor(it.loc)
            player.advanceIf(TutorialStage.BREAD_BAKED, TutorialStage.QUEST_PATH)
            player.advanceIf(TutorialStage.QUEST_PATH, TutorialStage.QUEST_HOUSE_ENTERED)
        }
        onOpLoc1(TutorialLoc.DOOR_4) {
            player.openDoor(it.loc)
            player.advanceIf(TutorialStage.QUEST_HOUSE_ENTERED, TutorialStage.QUEST_HOUSE_INSIDE)
        }
        onOpLoc1(TutorialLoc.DOOR_6) {
            player.openDoor(it.loc)
            player.advanceIf(TutorialStage.POLL_BOOTH, TutorialStage.ACCOUNT_GUIDE_ARRIVED)
        }
        onOpLoc1(TutorialLoc.DOOR_7) {
            player.openDoor(it.loc)
            player.advanceIf(TutorialStage.ACCOUNT_GUIDE_DONE, TutorialStage.CHAPEL_ARRIVED)
        }
        onOpLoc1(TutorialLoc.DOOR_8) {
            player.openDoor(it.loc)
            player.advanceIf(TutorialStage.PRAYER_GUIDE_DONE, TutorialStage.MAGIC_GUIDE_ARRIVED)
        }
        onOpLoc1(TutorialLoc.LADDER_DOWN) {
            climb(
                CoordGrid(3088, 9520, 0),
                TutorialStage.QUEST_GUIDE_DONE,
                TutorialStage.LADDER_DOWN,
            )
        }
        onOpLoc1(TutorialLoc.LADDER_DOWN_2) {
            climb(
                CoordGrid(3111, 9526, 0),
                TutorialStage.QUEST_GUIDE_DONE,
                TutorialStage.LADDER_DOWN,
            )
        }
        onOpLoc1(TutorialLoc.LADDER_UP) {
            climb(
                CoordGrid(3088, 3120, 0),
                TutorialStage.COMBAT_GUIDE_DONE,
                TutorialStage.LADDER_UP,
            )
        }
        onOpLoc1(TutorialLoc.LADDER_UP_2) {
            climb(
                CoordGrid(3111, 3125, 0),
                TutorialStage.COMBAT_GUIDE_DONE,
                TutorialStage.LADDER_UP,
            )
        }
        onOpLoc1(TutorialLoc.COMBAT_GATE_LEFT) {
            player.openGate(it.loc, TutorialLoc.COMBAT_GATE_RIGHT)
            player.advanceIf(TutorialStage.DAGGER_SMITHED, TutorialStage.COMBAT_GATE)
        }
        onOpLoc1(TutorialLoc.COMBAT_GATE_RIGHT) {
            player.openGate(it.loc, TutorialLoc.COMBAT_GATE_LEFT)
            player.advanceIf(TutorialStage.DAGGER_SMITHED, TutorialStage.COMBAT_GATE)
        }
        onOpLoc1(TutorialLoc.RAT_GATE_LEFT) { useRatGate(it.loc, TutorialLoc.RAT_GATE_RIGHT) }
        onOpLoc1(TutorialLoc.RAT_GATE_RIGHT) { useRatGate(it.loc, TutorialLoc.RAT_GATE_LEFT) }
    }

    private fun ScriptContext.bindObjects() {
        onOpLoc1(TutorialLoc.BANK_BOOTH) {
            if (player.tutorialStage == TutorialStage.LADDER_UP) {
                guide.advance(player, TutorialStage.BANK_OPENED)
            }
            player.ifOpenMainSidePair("interface.bankmain", "interface.bankside", -1, -2, eventBus)
        }
        onOpLoc1(TutorialLoc.POLL_BOOTH) {
            if (player.tutorialStage == TutorialStage.BANK_OPENED) {
                mesbox(
                    "Poll booths are found in towns across the world. The Old School RuneScape " +
                        "community is invited to vote on future game updates, to decide whether " +
                        "each update should or shouldn't be released.",
                )
                mesbox(
                    "Voting is open to <col=7f0000>members</col> with a <col=7f0000>skill total " +
                        "of 300</col>. To preserve the old-school character of this game, an " +
                        "update must gain <col=7f0000>70%</col> support to be released.",
                )
                mesbox("A flag appears on the booth to let you know when you're invited to vote in a poll.")
                guide.advance(player, TutorialStage.POLL_BOOTH)
            }
        }
    }

    private fun ScriptContext.bindSkilling() {
        onOpHeldU(TutorialObj.BUCKET_WATER, TutorialObj.POT_FLOUR) { makeDough() }
        onOpHeldU(TutorialObj.TINDERBOX, TutorialObj.LOGS) { lightFire() }
        onOpLocU(TutorialLoc.FIRE, TutorialObj.RAW_SHRIMP) { cookShrimp() }
        onOpLocU(TutorialLoc.RANGE, TutorialObj.BREAD_DOUGH) { bakeBread() }
        onOpLoc1(TutorialLoc.RANGE) {
            if (player.tutorialStage == TutorialStage.DOUGH_MADE) {
                bakeBread()
            }
        }
        onOpNpc1(TutorialNpc.FISHING_SPOT) { catchShrimp() }
        for (tree in listOf(TutorialLoc.TREE, TutorialLoc.TREE_2, TutorialLoc.OAK_TREE)) {
            onOpLoc1(tree) { chopTree() }
        }
        onOpLoc1(TutorialLoc.TIN_ROCK) { mineTin() }
        onOpLoc1(TutorialLoc.COPPER_ROCK) { mineCopper() }
        onOpLoc1(TutorialLoc.FURNACE) { smeltBar() }
        onOpHeld1(TutorialObj.BONES) { buryBones() }
    }

    private fun ScriptContext.bindDamageFloor() {
        onEvent<PlayerHitEvents.Modify> {
            if (player.inTutorial) {
                val floor = (player.hitpoints - 1).coerceAtLeast(0)
                if (hit.damage > floor) {
                    hit.damage = floor
                }
            }
        }
    }

    private suspend fun ProtectedAccess.climb(destination: CoordGrid, from: Int, to: Int) {
        if (player.tutorialStage != from) {
            return
        }
        anim("seq.human_reachforladder")
        delay(1)
        telejump(destination, TeleportType.Exempt)
        guide.advance(player, to)
    }

    private fun Player.advanceIf(from: Int, to: Int) {
        if (tutorialStage == from) {
            guide.advance(this, to)
        }
    }

    private fun Player.openDoor(loc: BoundLocInfo) {
        locRepo.del(loc, DOOR_OPEN_TICKS)
    }

    private fun Player.openGate(loc: BoundLocInfo, otherName: String) {
        locRepo.del(loc, DOOR_OPEN_TICKS)
        findNearbyLoc(loc.coords, otherName)?.let { locRepo.del(it, DOOR_OPEN_TICKS) }
    }

    private suspend fun ProtectedAccess.useRatGate(loc: BoundLocInfo, otherName: String) {
        if (player.tutorialStage in TutorialStage.RANGED_GIVEN until TutorialStage.RAT_ENGAGED_RANGED) {
            mesbox("No, don't enter the pit. Range the rats from outside the cage.")
            return
        }
        player.openGate(loc, otherName)
    }

    private fun findNearbyLoc(origin: CoordGrid, name: String): LocInfo? {
        val id = name.asRSCM(RSCMType.LOC)
        return locRepo.findAll(ZoneKey.from(origin)).firstOrNull {
            it.entity.id == id && it.coords.chebyshevDistance(origin) <= 1
        }
    }

    private suspend fun ProtectedAccess.mesOverlay(text: String) {
        runClientScript(MESOVERLAY, text)
    }

    private fun ProtectedAccess.tutorialStat(stat: String, xp: Double) {
        player.statAdvance(stat, xp * xpMods.get(player, stat))
    }

    private suspend fun ProtectedAccess.makeDough() {
        if (player.tutorialStage != TutorialStage.CHEF_TALKED) {
            return
        }
        if (!player.inv.contains(TutorialObj.POT_FLOUR) ||
            !player.inv.contains(TutorialObj.BUCKET_WATER)
        ) {
            return
        }
        if (invDel(inv, TutorialObj.POT_FLOUR, 1).failure ||
            invDel(inv, TutorialObj.BUCKET_WATER, 1).failure
        ) {
            return
        }
        invAdd(inv, TutorialObj.BREAD_DOUGH, 1)
        invAdd(inv, TutorialObj.POT_EMPTY, 1)
        invAdd(inv, TutorialObj.BUCKET_EMPTY, 1)
        mesbox("You make some dough.")
        guide.advance(player, TutorialStage.DOUGH_MADE)
    }

    private suspend fun ProtectedAccess.lightFire() {
        if (player.tutorialStage != TutorialStage.LOGS_CUT) {
            return
        }
        if (!player.inv.contains(TutorialObj.LOGS) ||
            !player.inv.contains(TutorialObj.TINDERBOX)
        ) {
            return
        }
        mesOverlay("Your character is now attempting to light a fire. This should only take a few seconds.")
        anim("seq.human_createfire")
        delay(4)
        if (invDel(inv, TutorialObj.LOGS, 1).failure) {
            return
        }
        locRepo.add(
            player.coords,
            TutorialLoc.FIRE,
            FIRE_DURATION,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
        tutorialStat("stat.firemaking", 40.0)
        guide.advance(player, TutorialStage.FIRE_LIT)
    }

    private suspend fun ProtectedAccess.cookShrimp() {
        if (player.tutorialStage != TutorialStage.FIRE_LIT) {
            return
        }
        mesOverlay("Your character is now attempting to cook some shrimp. This will only take a few seconds.")
        delay(2)
        if (invDel(inv, TutorialObj.RAW_SHRIMP, 1).failure) {
            return
        }
        mesbox("You manage to cook some shrimp.")
        tutorialStat("stat.cooking", 30.0)
        invAdd(inv, TutorialObj.SHRIMP, 1)
        guide.advance(player, TutorialStage.SHRIMP_COOKED)
    }

    private suspend fun ProtectedAccess.bakeBread() {
        if (player.tutorialStage != TutorialStage.DOUGH_MADE) {
            return
        }
        mesOverlay("Your character is now attempting to bake some bread. This will only take a few seconds.")
        delay(2)
        if (invDel(inv, TutorialObj.BREAD_DOUGH, 1).failure) {
            return
        }
        mesbox("You manage to bake some bread.")
        tutorialStat("stat.cooking", 40.0)
        invAdd(inv, TutorialObj.BREAD, 1)
        guide.advance(player, TutorialStage.BREAD_BAKED)
    }

    private suspend fun ProtectedAccess.catchShrimp() {
        if (player.tutorialStage != TutorialStage.INVENTORY_VIEWED) {
            return
        }
        if (!player.inv.contains(TutorialObj.NET)) {
            mes("You need a net to catch shrimp.")
            return
        }
        mesOverlay(
            "Your character is now attempting to catch some shrimp. Sit back for a moment " +
                "while he does all the hard work.",
        )
        delay(3)
        invAdd(inv, TutorialObj.RAW_SHRIMP, 1)
        tutorialStat("stat.fishing", 10.0)
        mesbox("You manage to catch some shrimp.")
        guide.advance(player, TutorialStage.SHRIMP_CAUGHT)
    }

    private suspend fun ProtectedAccess.chopTree() {
        if (player.tutorialStage != TutorialStage.AXE_GIVEN) {
            return
        }
        if (!player.inv.contains(TutorialObj.BRONZE_AXE)) {
            mes("You need an axe to chop down this tree.")
            return
        }
        mesOverlay(
            "Your character is now attempting to cut down the tree. Sit back for a moment " +
                "while he does all the hard work.",
        )
        delay(4)
        if (invAdd(inv, TutorialObj.LOGS, 1).failure) {
            return
        }
        tutorialStat("stat.woodcutting", 25.0)
        mesbox("You manage to cut some logs.")
        guide.advance(player, TutorialStage.LOGS_CUT)
    }

    private suspend fun ProtectedAccess.mineTin() {
        if (player.tutorialStage != TutorialStage.MINING_GUIDE_TALKED) {
            return
        }
        if (!player.inv.contains(TutorialObj.BRONZE_PICKAXE)) {
            mes("You need a pickaxe to mine this rock.")
            return
        }
        mesOverlay("Your character is now attempting to mine the rock.<br>This should only take a few seconds.")
        delay(3)
        invAdd(inv, TutorialObj.TIN_ORE, 1)
        tutorialStat("stat.mining", 17.5)
        mesbox("You manage to mine some tin.")
        guide.advance(player, TutorialStage.TIN_MINED)
    }

    private suspend fun ProtectedAccess.mineCopper() {
        if (player.tutorialStage != TutorialStage.TIN_MINED) {
            return
        }
        if (!player.inv.contains(TutorialObj.BRONZE_PICKAXE)) {
            mes("You need a pickaxe to mine this rock.")
            return
        }
        mesOverlay("Your character is now attempting to mine the rock.<br>This should only take a few seconds.")
        delay(3)
        invAdd(inv, TutorialObj.COPPER_ORE, 1)
        tutorialStat("stat.mining", 17.5)
        mesbox("You manage to mine some copper.")
        guide.advance(player, TutorialStage.COPPER_MINED)
    }

    private suspend fun ProtectedAccess.smeltBar() {
        if (player.tutorialStage != TutorialStage.COPPER_MINED) {
            return
        }
        if (!player.inv.contains(TutorialObj.TIN_ORE) ||
            !player.inv.contains(TutorialObj.COPPER_ORE)
        ) {
            mes("You need some tin and copper ore to smelt a bronze bar.")
            return
        }
        mesOverlay("You smelt the copper and tin together in the furnace.")
        delay(2)
        if (invDel(inv, TutorialObj.TIN_ORE, 1).failure ||
            invDel(inv, TutorialObj.COPPER_ORE, 1).failure
        ) {
            return
        }
        mesbox("You retrieve a bar of bronze.")
        tutorialStat("stat.smithing", 6.2)
        invAdd(inv, TutorialObj.BRONZE_BAR, 1)
        guide.advance(player, TutorialStage.BAR_SMELTED)
    }

    private suspend fun ProtectedAccess.buryBones() {
        if (invDel(inv, TutorialObj.BONES, 1).failure) {
            return
        }
        mes("You bury the bones.")
        tutorialStat("stat.prayer", 4.5)
    }

    private suspend fun Dialogue.gielinorGuide() {
        when {
            player.tutorialStage <= TutorialStage.EXPERIENCE_SELECTED -> {
                chatNpc(
                    happy,
                    "Greetings! I see you are a new arrival to the world of Gielinor. My job is " +
                        "to welcome all new visitors. So welcome!",
                )
                chatNpc(
                    happy,
                    "You have already learned the first thing needed to succeed in this world: " +
                        "talking to other people!",
                )
                chatNpc(
                    neutral,
                    "You will find many inhabitants of this world have useful things to say to " +
                        "you. By clicking on them you can talk to them.",
                )
                chatNpc(happy, "Now then, let's start by looking at your settings menu.")
                guide.advance(player, TutorialStage.SETTINGS_REQUESTED)
            }
            player.tutorialStage == TutorialStage.SETTINGS_REQUESTED ->
                chatNpc(neutral, "You should take a look at that menu before we continue.")
            player.tutorialStage == TutorialStage.SETTINGS_OPENED -> {
                chatNpc(
                    happy,
                    "Looks like you're making good progress! The menu you've just opened is one " +
                        "of many. You'll learn about the rest as you progress through the tutorial.",
                )
                chatNpc(happy, "Anyway, I'd say it's time for you to go and meet your first instructor!")
                guide.advance(player, TutorialStage.GUIDE_DONE)
            }
            else ->
                chatNpc(
                    happy,
                    "You're making good progress. Talk to your instructors to continue your training.",
                )
        }
    }

    private suspend fun Dialogue.survivalExpert() {
        when (player.tutorialStage) {
            TutorialStage.OUTSIDE -> {
                chatNpc(
                    neutral,
                    "Hello there, newcomer. My name is Brynna. My job is to teach you about the " +
                        "skills you can use to survive in this world.",
                )
                chatNpc(
                    neutral,
                    "The first skill we're going to look at is Fishing. There's some shrimp in " +
                        "this pond here. Let's try and catch some.",
                )
                if (access.giveTutorialItems(TutorialObj.NET to 1)) {
                    mesbox("The survival expert gives you a <col=000080>small fishing net<col=000000>.")
                    guide.advance(player, TutorialStage.NET_GIVEN)
                }
            }
            TutorialStage.NET_GIVEN, TutorialStage.INVENTORY_VIEWED, TutorialStage.SHRIMP_CAUGHT ->
                chatNpc(neutral, "Hello again. You should take a look at that menu before we continue.")
            TutorialStage.SKILLS_VIEWED -> {
                chatNpc(
                    happy,
                    "Excellent work. Next you'll want to cook those shrimp. To do that, you'll " +
                        "need a fire. This brings us on to the Woodcutting and Firemaking skills.",
                )
                if (access.giveTutorialItems(TutorialObj.BRONZE_AXE to 1, TutorialObj.TINDERBOX to 1)) {
                    mesbox(
                        "The survival expert gives you a <col=000080>bronze axe<col=000000> and a " +
                            "<col=000000><col=000080>tinderbox<col=000000>.",
                    )
                    guide.advance(player, TutorialStage.AXE_GIVEN)
                }
            }
            else -> survivalRecap()
        }
    }

    private suspend fun Dialogue.survivalRecap() {
        chatNpc(quiz, "Hello again. Is there something you'd like to hear more about?")
        when (
            choice4(
                "Tell me about my skills again.",
                1,
                "Tell me about Woodcutting again.",
                2,
                "Tell me about Firemaking again.",
                3,
                "More options...",
                4,
            )
        ) {
            1 ->
                chatNpc(
                    neutral,
                    "Your skills can be levelled up by earning experience. As you level up your " +
                        "skills, you will earn new unlocks.",
                )
            2 ->
                chatNpc(
                    neutral,
                    "Woodcutting is a skill that allows you to cut down trees for logs. All you " +
                        "need is an axe.",
                )
            3 ->
                chatNpc(
                    neutral,
                    "Firemaking is a skill that allows you to light fires. Use a tinderbox on " +
                        "some logs to light them.",
                )
            else -> survivalMoreRecap()
        }
    }

    private suspend fun Dialogue.survivalMoreRecap() {
        when (choice2("Tell me about Fishing again.", 1, "No thanks.", 2)) {
            1 ->
                chatNpc(
                    neutral,
                    "Fishing is a skill that allows you to catch fish. You'll need a net or a " +
                        "fishing rod, depending on what you want to catch.",
                )
            else -> Unit
        }
    }

    private suspend fun Dialogue.masterChef() {
        if (player.tutorialStage == TutorialStage.KITCHEN_ENTERED) {
            chatNpc(
                neutral,
                "Ah! Welcome, newcomer. I am the Master Chef, Lev. It is here that I will teach " +
                    "you how to cook food truly fit for a king.",
            )
            chatPlayer(angry, "I already know how to cook. Brynna taught me just now.")
            chatNpc(
                laugh,
                "Hahahahahaha! You call THAT cooking? Some shrimp on an open log fire? Oh, no, " +
                    "no, no. I am going to teach you the fine art of cooking bread.",
            )
            if (access.giveTutorialItems(TutorialObj.POT_FLOUR to 1, TutorialObj.BUCKET_WATER to 1)) {
                doubleobjbox(
                    TutorialObj.POT_FLOUR,
                    TutorialObj.BUCKET_WATER,
                    "The master chef gives you some <col=000080>flour<col=000000> and some " +
                        "<col=000080>water<col=000000>.",
                )
                guide.advance(player, TutorialStage.CHEF_TALKED)
            }
        } else {
            chatNpc(happy, "Hello again. How is your cooking coming along?")
        }
    }

    private suspend fun Dialogue.questGuide() {
        when (player.tutorialStage) {
            TutorialStage.QUEST_HOUSE_INSIDE -> {
                chatNpc(
                    neutral,
                    "Ah. Welcome, adventurer. I'm here to tell you all about quests. Let's start " +
                        "by opening your quest list.",
                )
                guide.advance(player, TutorialStage.QUEST_GUIDE_TALKED)
            }
            TutorialStage.QUEST_GUIDE_TALKED ->
                chatNpc(neutral, "You should take a look at your quest list before we continue.")
            TutorialStage.QUEST_LIST_OPENED -> {
                chatNpc(
                    neutral,
                    "Now that you have the quest list open, you can see all the quests within it. " +
                        "Opening a quest's journal will provide you with more information on it.",
                )
                chatNpc(
                    neutral,
                    "If you haven't started the quest, it will tell you where to begin and what " +
                        "requirements you need. If the quest is in progress, it will remind you " +
                        "what to do next.",
                )
                chatNpc(
                    neutral,
                    "It's very easy to find quest start points. Just look out for the quest icon " +
                        "on your minimap. You should see one marking this house.",
                )
                mesbox(
                    "The minimap in the top right corner of the screen has various icons to show " +
                        "different points of interest. Look for the icon to the left to find quest " +
                        "start points.",
                )
                chatNpc(
                    neutral,
                    "The quests themselves can vary greatly from collecting beads to hunting down " +
                        "dragons. Completing quests will reward you with all sorts of things, such " +
                        "as new areas and better weapons!",
                )
                chatNpc(
                    neutral,
                    "There's not a lot more I can tell you about questing. You have to experience " +
                        "the thrill of it yourself to fully understand. Let me know if you want a " +
                        "recap, otherwise you can move on.",
                )
                guide.advance(player, TutorialStage.QUEST_GUIDE_DONE)
            }
            else ->
                chatNpc(
                    neutral,
                    "The quest list and journal are useful tools. Talk to your next instructor to continue.",
                )
        }
    }

    private suspend fun Dialogue.miningInstructor() {
        when (player.tutorialStage) {
            TutorialStage.LADDER_DOWN -> {
                chatNpc(
                    neutral,
                    "Hi there. You must be new around here. So what do I call you? 'Newcomer' seems " +
                        "so impersonal, and if we're going to be working together, I'd rather call " +
                        "you by name.",
                )
                chatPlayer(neutral, "You can call me ${player.displayName}.")
                chatNpc(
                    neutral,
                    "Ok then, ${player.displayName}. My name is Dezzick and I'm a miner by trade. " +
                        "Let's teach you how to mine.",
                )
                chatNpc(
                    happy,
                    "Mining is very simple, all you need is a pickaxe. The rocks around here " +
                        "contain tin and copper. Why don't you get started by mining some?",
                )
                if (access.giveTutorialItems(TutorialObj.BRONZE_PICKAXE to 1)) {
                    mesbox("The mining instructor gives you a <col=000080>bronze pickaxe<col=000000>.")
                    guide.advance(player, TutorialStage.MINING_GUIDE_TALKED)
                }
            }
            TutorialStage.BAR_SMELTED -> {
                chatPlayer(quiz, "I have a bronze bar. What now?")
                chatNpc(
                    happy,
                    "Now that you've got a bar, you can smith it into a weapon. To smith something, " +
                        "you need a hammer and an anvil. There's some anvils just here that you can " +
                        "use. See if you can make a bronze dagger.",
                )
                if (access.giveTutorialItems(TutorialObj.HAMMER to 1)) {
                    mesbox("The mining instructor gives you a <col=000080>hammer<col=000000>.")
                    guide.advance(player, TutorialStage.MINING_GUIDE_BAR)
                }
            }
            else -> chatNpc(neutral, "Keep practising your mining and smithing.")
        }
    }

    private suspend fun Dialogue.combatInstructor() {
        when (player.tutorialStage) {
            TutorialStage.COMBAT_GATE -> {
                guide.advance(player, TutorialStage.COMBAT_GUIDE_TALKED)
                chatPlayer(happy, "Hi! My name's ${player.displayName}.")
                chatNpc(
                    angry,
                    "Do I look like I care? To me you're just another newcomer who thinks they're " +
                        "ready to fight.",
                )
                chatNpc(
                    neutral,
                    "I am Vannaka, the greatest swordsman alive, and I'm here to teach you the " +
                        "basics of combat.",
                )
                chatNpc(neutral, "Let's get started by teaching you to wield a weapon.")
                guide.advance(player, TutorialStage.DAGGER_EQUIP_TASK)
            }
            TutorialStage.DAGGER_EQUIPPED -> {
                chatNpc(
                    neutral,
                    "Very good, but that little butter knife isn't going to protect you much. " +
                        "Let's get you something a bit better. Once you're properly equipped we " +
                        "can move on to some actual combat.",
                )
                if (access.giveTutorialItems(TutorialObj.BRONZE_SWORD to 1, TutorialObj.WOODEN_SHIELD to 1)) {
                    mesbox(
                        "The combat instructor gives you a <col=000080>bronze sword<col=000000> " +
                            "and a <col=000000><col=000080>wooden shield<col=000000>.",
                    )
                    guide.advance(player, TutorialStage.COMBAT_GUIDE_SWORD)
                }
            }
            TutorialStage.RAT_KILLED_MELEE -> {
                chatPlayer(happy, "I did it! I killed a giant rat!")
                chatNpc(
                    neutral,
                    "I saw. You seem better at this than I thought. Now that you have grasped basic " +
                        "swordplay, let's move on.",
                )
                chatNpc(
                    neutral,
                    "Next up we have ranged combat. With this you can kill foes from a distance. " +
                        "With ranged, you'll be able to attack the rats without entering the pit " +
                        "meaning they won't be able to fight back.",
                )
                if (access.giveTutorialItems(TutorialObj.SHORTBOW to 1, TutorialObj.BRONZE_ARROW to 50)) {
                    mesbox(
                        "The combat instructor gives you a <col=000080>shortbow<col=000000> and " +
                            "some <col=000000><col=000080>bronze arrows<col=000000>.",
                    )
                    guide.advance(player, TutorialStage.RANGED_GIVEN)
                }
            }
            else -> chatNpc(neutral, "Keep practising your combat skills.")
        }
    }

    private suspend fun Dialogue.accountGuide() {
        when (player.tutorialStage) {
            TutorialStage.ACCOUNT_GUIDE_ARRIVED -> {
                chatPlayer(happy, "Hello. Who are you?")
                chatNpc(happy, "I'm the Account Guide. I'm here to tell you about your account.")
                chatPlayer(quiz, "What's an account?")
                chatNpc(
                    happy,
                    "Your character IS your account! It's what you are using to play the game " +
                        "right now. Everyone here has one and some people even have multiple!",
                )
                chatNpc(
                    neutral,
                    "There's various things you can do with your account which is where the Account " +
                        "Management menu comes into play. Let's take a look at it now.",
                )
                guide.advance(player, TutorialStage.ACCOUNT_GUIDE_TALKED)
            }
            TutorialStage.ACCOUNT_GUIDE_TALKED ->
                chatNpc(neutral, "You should take a look at your Account Management menu before we continue.")
            TutorialStage.ACCOUNT_MENU_OPENED -> {
                chatNpc(happy, "As you can see, there's a few things you can do with your account.")
                chatNpc(
                    happy,
                    "First, you have membership. All accounts are free to play by default but if " +
                        "you desire, you can buy membership to unlock members' worlds. These worlds " +
                        "give you access to extra skills, areas, quests, and more!",
                )
                chatPlayer(quiz, "What's a world?")
                chatNpc(
                    happy,
                    "A world is what you play the game on. Most worlds can hold up to 2000 players. " +
                        "They are split into two main types: free worlds and members' worlds. You " +
                        "can swap worlds with the World Switcher in the Logout menu.",
                )
                chatNpc(
                    happy,
                    "You are currently on a free world. If you want to switch to a members' world " +
                        "and enjoy all of the benefits, you'll need to purchase membership. You can " +
                        "do so using the store in the Account Management menu.",
                )
                chatNpc(
                    happy,
                    "Bonds are another thing that you should know about. You can purchase Bonds " +
                        "from the store in the Account Management menu. Once you have some Bonds, " +
                        "you can redeem them for membership!",
                )
                chatPlayer(quiz, "But why would I use a Bond instead of just buying membership normally?")
                chatNpc(
                    happy,
                    "Unlike membership, Bonds can be traded with other players. You could purchase " +
                        "a Bond and then sell it to another player for some gold. Alternatively, " +
                        "you could use your gold to buy a Bond from another player!",
                )
                chatPlayer(
                    quiz,
                    "So if I used my gold to buy a Bond from another player, I could get membership " +
                        "without spending any real money?",
                )
                chatNpc(happy, "Exactly!")
                chatNpc(
                    happy,
                    "Next up, you have your inbox and name changer. The inbox is where you'll receive " +
                        "important information related to your account and the game. Be sure to " +
                        "keep an eye on it.",
                )
                chatNpc(
                    neutral,
                    "The name changer allows you to change your display name if you don't like your " +
                        "current one. Be aware though that this is only available to accounts with " +
                        "membership.",
                )
                chatPlayer(quiz, "Awesome. Is there anything else I should know about accounts?")
                chatNpc(
                    happy,
                    "Your account is very valuable and it's important you look after it. Various " +
                        "tools can help with this, such as Bank PINs and two-factor authentication.",
                )
                chatNpc(
                    happy,
                    "You'll find more information on account security on the Customer Support " +
                        "webpage. You can use the Useful Links section of the Account Management " +
                        "menu to get there.",
                )
                chatNpc(
                    happy,
                    "Anyway, that's everything I have to tell you about accounts. Let me know if you " +
                        "need a recap, otherwise head on to your next instructor.",
                )
                guide.advance(player, TutorialStage.ACCOUNT_GUIDE_DONE)
            }
            else -> chatNpc(happy, "Ask me about your account whenever you like.")
        }
    }

    private suspend fun Dialogue.brotherBrace() {
        when (player.tutorialStage) {
            TutorialStage.CHAPEL_ARRIVED -> {
                chatPlayer(happy, "Good day, brother.")
                chatNpc(happy, "Hello, I'm Brother Brace.<br>I'm here to tell you all about Prayer.")
                guide.advance(player, TutorialStage.PRAYER_GUIDE_TALKED)
            }
            TutorialStage.PRAYER_BOOK_OPENED -> {
                chatNpc(
                    neutral,
                    "This is your Prayer list. Prayers can help a lot in combat. Click on the " +
                        "prayer you wish to use to activate it, and click it again to deactivate it.",
                )
                chatNpc(
                    neutral,
                    "Active prayers will drain your Prayer Points, which you can recharge by " +
                        "finding an altar or other holy spot and praying there.",
                )
                chatNpc(
                    neutral,
                    "As you noticed, most enemies will drop bones when defeated. Burying bones, " +
                        "by clicking them in your inventory, will gain you Prayer experience.",
                )
                chatNpc(
                    happy,
                    "That's all I have to teach you for now. If you need a reminder about your " +
                        "prayers, just speak to me again.",
                )
                guide.advance(player, TutorialStage.PRAYER_GUIDE_DONE)
            }
            else -> chatNpc(happy, "Prayer is a useful skill. Keep it in mind.")
        }
    }

    private suspend fun Dialogue.magicInstructor() {
        when {
            player.tutorialStage == TutorialStage.MAGIC_GUIDE_ARRIVED -> {
                chatPlayer(happy, "Hello.")
                chatNpc(
                    happy,
                    "Good day, newcomer. My name is Terrova. I'm here to tell you about Magic. " +
                        "Let's start by opening your magic interface.",
                )
                guide.advance(player, TutorialStage.MAGIC_GUIDE_TALKED)
            }
            player.tutorialStage == TutorialStage.MAGIC_GUIDE_TALKED ->
                chatNpc(neutral, "Open up your magic interface by clicking on the flashing icon.")
            player.tutorialStage == TutorialStage.MAGIC_BOOK_OPENED -> {
                chatNpc(
                    neutral,
                    "Currently you can only cast one offensive spell called Wind Strike. Let's " +
                        "try it out on one of those chickens.",
                )
                if (access.giveTutorialItems(TutorialObj.AIR_RUNE to 5, TutorialObj.MIND_RUNE to 5)) {
                    mesbox(
                        "Terrova gives you some <col=000080>air runes<col=000000> and " +
                            "<col=000080>mind runes<col=000000>.",
                    )
                    guide.advance(player, TutorialStage.RUNES_GIVEN)
                }
            }
            player.tutorialStage == TutorialStage.RUNES_GIVEN ->
                chatNpc(neutral, "You should try casting Wind Strike on one of those chickens.")
            player.tutorialStage == TutorialStage.COMPLETE -> finalMagicDialogue()
            player.tutorialStage >= TutorialStage.READY_TO_LEAVE ->
                chatNpc(
                    happy,
                    "Excellent, I think that covers everything. When you're ready to go, use your " +
                        "home teleport spell to teleport to Lumbridge!",
                )
            else -> chatNpc(happy, "Magic is a powerful skill. Keep practising.")
        }
    }

    private suspend fun Dialogue.finalMagicDialogue() {
        chatNpc(
            happy,
            "Well, you're all finished here now. I'll give you a reasonable number of runes when " +
                "you leave.",
        )
        chatNpc(neutral, "Were you planning to become an Ironman, by the way?")
        when (
            choice3(
                "What's an Ironman?",
                1,
                "Yes, I do want to become an Ironman.",
                2,
                "No, I'm not planning to do that.",
                3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "What's an Ironman?")
                chatNpc(
                    neutral,
                    "An Ironman is a player who is entirely self-sufficient. Ironmen cannot trade " +
                        "with other players, use the Grand Exchange, or take items from other " +
                        "players' drops.",
                )
                finalMagicDialogue()
                return
            }
            2 -> {
                chatPlayer(happy, "Yes, I do want to become an Ironman.")
                access.ifOpenMainModal("interface.ironman_setup")
                return
            }
            else -> chatPlayer(neutral, "No, I'm not planning to do that.")
        }
        chatNpc(
            happy,
            "When you get to the mainland you will find yourself in the town of Lumbridge. If " +
                "you want some ideas on where to go next, talk to my friend Adventurer Jon. You " +
                "can't miss him; he's wearing light blue armour with a huge sword. There are also " +
                "many tutors willing to teach you about the many skills you could learn.",
        )
        mesbox(
            "When you get to Lumbridge, look out for Adventurer Jon, he can be found outside of " +
                "the Sheared Ram pub.",
        )
        chatNpc(
            happy,
            "Excellent, I think that covers everything. When you're ready to go, use your home " +
                "teleport spell to teleport to Lumbridge!",
        )
        guide.advance(player, TutorialStage.READY_TO_LEAVE)
    }

    private suspend fun Dialogue.ironmanTutor() {
        chatNpc(
            happy,
            "Hello, ${player.displayName}. I'm the Ironman tutor. You can choose to become an " +
                "Ironman while you're on Tutorial Island. Some things can't be changed later.",
        )
        when (choice3("Tell me about Ironmen.", 1, "I'd like to become an Ironman.", 2, "I'm fine, thanks.", 3)) {
            1 ->
                chatNpc(
                    neutral,
                    "An Ironman is a player who is entirely self-sufficient. Ironmen cannot trade " +
                        "with other players, use the Grand Exchange, or take items from other " +
                        "players' drops.",
                )
            2 -> {
                chatPlayer(happy, "I'd like to become an Ironman.")
                access.ifOpenMainModal("interface.ironman_setup")
            }
            else -> chatPlayer(neutral, "I'm fine, thanks.")
        }
    }

    private companion object {
        private const val FIRE_DURATION = 200
        private const val DOOR_OPEN_TICKS = 5
        private val MESOVERLAY =
            "clientscript.[clientscript,mesoverlay]".asRSCM(RSCMType.CLIENTSCRIPT)
    }
}
