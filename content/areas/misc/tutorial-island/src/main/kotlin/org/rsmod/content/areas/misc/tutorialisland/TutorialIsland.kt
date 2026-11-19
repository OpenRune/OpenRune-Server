package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class TutorialIsland
@Inject
constructor(
    private val guide: TutorialGuide,
    private val locRepo: LocRepository,
    private val eventBus: EventBus,
) : PluginScript() {

    override fun ScriptContext.startup() {
        bindInstructors()
        bindDoors()
        bindInterfaces()
        bindSkilling()
        bindDamageFloor()
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
        onOpLoc1(TutorialLoc.DOOR_1) { player.advanceIf(10, 20) }
        for (gate in listOf(TutorialLoc.GATE_LEFT, TutorialLoc.GATE_RIGHT)) {
            onOpLoc1(gate) { player.advanceIf(120, 130) }
        }
        onOpLoc1(TutorialLoc.DOOR_2) { player.advanceIf(130, 140) }
        onOpLoc1(TutorialLoc.DOOR_3) { player.advanceIf(170, 183) }
        onOpLoc1(TutorialLoc.DOOR_4) { player.advanceIf(183, 200) }
        for (door in listOf(TutorialLoc.DOOR_6, TutorialLoc.DOOR_7)) {
            onOpLoc1(door) { player.advanceIf(525, 530) }
        }
        onOpLoc1(TutorialLoc.DOOR_8) { player.advanceIf(540, 550) }
        onOpLoc1(TutorialLoc.DOOR_9) { player.advanceIf(610, 620) }
        for (ladder in listOf(TutorialLoc.LADDER_DOWN, TutorialLoc.LADDER_DOWN_2)) {
            onOpLoc1(ladder) { player.advanceIf(250, 260) }
        }
        for (ladder in listOf(TutorialLoc.LADDER_UP, TutorialLoc.LADDER_UP_2)) {
            onOpLoc1(ladder) { player.advanceIf(500, 510) }
        }
        for (gate in listOf(TutorialLoc.DOOR_4_LEFT, TutorialLoc.DOOR_4_RIGHT)) {
            onOpLoc1(gate) { player.advanceIf(360, 370) }
        }
        for (gate in listOf(TutorialLoc.DOOR_5_LEFT, TutorialLoc.DOOR_5_RIGHT)) {
            onOpLoc1(gate) { player.advanceIf(440, 450) }
        }
    }

    private fun ScriptContext.bindInterfaces() {
        onIfOpen("interface.settings_side") { player.advanceIf(3, 7) }
        onIfOpen("interface.inventory") { player.advanceIf(30, 40) }
        onIfOpen("interface.stats") { player.advanceIf(50, 60) }
        onIfOpen("interface.wornitems") { player.advanceIf(390, 400) }
        onIfOpen("interface.equipment") { player.advanceIf(400, 405) }
        onIfOpen("interface.combat_interface") { player.advanceIf(430, 440) }
        onIfOpen("interface.questlist") { player.advanceIf(220, 230) }
        onIfOpen("interface.questjournal") { player.advanceIf(230, 240) }
        onIfOpen("interface.prayerbook") { player.advanceIf(560, 570) }
        onIfOpen("interface.magic_spellbook") { player.advanceIf(630, 640) }
        onIfOpen("interface.account") { player.advanceIf(531, 532) }
        onIfOpen("interface.bankside") { player.openTutorialBank() }
        onIfClose("interface.bankmain") { player.advanceIf(520, 525) }
        onIfModalButton("component.smithing:dagger") {
            if (player.tutorialStage in 330..350) {
                delay(4)
                guide.advance(player, TutorialStage.COMBAT_GATE)
            }
        }
    }

    private fun ScriptContext.bindSkilling() {
        onOpHeldU(TutorialObj.POT_FLOUR, TutorialObj.BUCKET_WATER) { makeDough() }
        onOpHeldU(TutorialObj.TINDERBOX, TutorialObj.LOGS) { lightFire() }
        onOpLocU(TutorialLoc.FIRE, TutorialObj.RAW_SHRIMP) { cookShrimp() }
        onOpLocU(TutorialLoc.RANGE, TutorialObj.BREAD_DOUGH) { bakeBread() }
        onOpHeld1(TutorialObj.BONES) { buryBones() }
        onOpNpc1(TutorialNpc.FISHING_SPOT) { catchShrimp() }
        for (tree in listOf(TutorialLoc.TREE, TutorialLoc.TREE_2, TutorialLoc.OAK_TREE)) {
            onOpLoc1(tree) { chopTree() }
        }
        onOpLoc1(TutorialLoc.TIN_ROCK) { mineTin() }
        onOpLoc1(TutorialLoc.COPPER_ROCK) { mineCopper() }
        onOpLoc1(TutorialLoc.FURNACE) { smeltBar() }
        onEvent<SkillingActionCompleteEvent> { skillingComplete() }
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

    private fun Player.advanceIf(from: Int, to: Int) {
        if (tutorialStage == from) {
            guide.advance(this, to)
        }
    }

    private fun Player.openTutorialBank() {
        if (tutorialStage == TutorialStage.BANK_OPENED) {
            guide.advance(this, TutorialStage.BANK_TUTORIAL)
        }
        if (tutorialStage == TutorialStage.BANK_OPENED || tutorialStage == TutorialStage.BANK_TUTORIAL) {
            val bank = invMap.getOrPut("inv.bank")
            if (bank.count(TutorialObj.COINS) == 0) {
                invAdd(bank, TutorialObj.COINS, 25)
            }
        }
    }

    private fun SkillingActionCompleteEvent.skillingComplete() {
        if (!player.inTutorial) {
            return
        }
        val context = context as? SkillingActionContext.Product ?: return
        val stage = player.tutorialStage
        val advanced =
            when (context.item) {
                TutorialObj.RAW_SHRIMP -> if (stage == TutorialStage.INVENTORY_VIEWED) 50 else null
                TutorialObj.LOGS -> if (stage == TutorialStage.AXE_GIVEN) 80 else null
                TutorialObj.BRONZE_BAR -> if (stage == TutorialStage.BAR_SMELTED) 320 else null
                TutorialObj.BRONZE_DAGGER ->
                    when (stage) {
                        TutorialStage.DAGGER_SMITHED -> 350
                        TutorialStage.DAGGER_SELECTED -> 360
                        else -> null
                    }
                else -> null
            }
        advanced?.let { guide.advance(player, it) }
    }

    private suspend fun ProtectedAccess.makeDough() {
        if (player.tutorialStage != TutorialStage.CHEF_TALKED) {
            return
        }
        if (!player.inv.contains(TutorialObj.POT_FLOUR) || !player.inv.contains(TutorialObj.BUCKET_WATER)) {
            return
        }
        if (invDel(inv, TutorialObj.POT_FLOUR, 1).failure || invDel(inv, TutorialObj.BUCKET_WATER, 1).failure) {
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
        if (!player.inv.contains(TutorialObj.LOGS) || !player.inv.contains(TutorialObj.TINDERBOX)) {
            return
        }
        anim("seq.human_createfire")
        mes("You attempt to light the logs.")
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
        guide.advance(player, TutorialStage.FIRE_LIT)
    }

    private suspend fun ProtectedAccess.cookShrimp() {
        if (player.tutorialStage != TutorialStage.FIRE_LIT) {
            return
        }
        if (invDel(inv, TutorialObj.RAW_SHRIMP, 1).failure) {
            return
        }
        mesbox("You manage to cook some shrimp.")
        statAdvance("stat.cooking", 30.0)
        invAdd(inv, TutorialObj.SHRIMP, 1)
        guide.advance(player, TutorialStage.SHRIMP_COOKED)
    }

    private suspend fun ProtectedAccess.bakeBread() {
        if (player.tutorialStage != TutorialStage.DOUGH_MADE) {
            return
        }
        if (invDel(inv, TutorialObj.BREAD_DOUGH, 1).failure) {
            return
        }
        mesbox("You manage to bake some bread.")
        statAdvance("stat.cooking", 40.0)
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
        spam("You cast out your net...")
        delay(3)
        invAdd(inv, TutorialObj.RAW_SHRIMP, 1)
        statAdvance("stat.fishing", 10.0)
        spam("You manage to catch some shrimp.")
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
        spam("You swing your axe at the tree.")
        delay(4)
        if (invAdd(inv, TutorialObj.LOGS, 1).failure) {
            return
        }
        statAdvance("stat.woodcutting", 25.0)
        spam("You manage to cut some logs.")
        guide.advance(player, TutorialStage.LOGS_CUT)
    }

    private suspend fun ProtectedAccess.mineTin() {
        if (player.tutorialStage != TutorialStage.TIN_MINED) {
            return
        }
        if (!player.inv.contains(TutorialObj.BRONZE_PICKAXE)) {
            mes("You need a pickaxe to mine this rock.")
            return
        }
        spam("You swing your pick at the rock.")
        delay(3)
        invAdd(inv, "obj.tin_ore", 1)
        statAdvance("stat.mining", 17.5)
        spam("You manage to mine some tin.")
        guide.advance(player, TutorialStage.COPPER_MINED)
    }

    private suspend fun ProtectedAccess.mineCopper() {
        if (player.tutorialStage != TutorialStage.COPPER_MINED) {
            return
        }
        if (!player.inv.contains(TutorialObj.BRONZE_PICKAXE)) {
            mes("You need a pickaxe to mine this rock.")
            return
        }
        spam("You swing your pick at the rock.")
        delay(3)
        invAdd(inv, "obj.copper_ore", 1)
        statAdvance("stat.mining", 17.5)
        spam("You manage to mine some copper.")
        guide.advance(player, TutorialStage.BAR_SMELTED)
    }

    private suspend fun ProtectedAccess.smeltBar() {
        if (player.tutorialStage != TutorialStage.BAR_SMELTED) {
            return
        }
        if (!player.inv.contains("obj.tin_ore") || !player.inv.contains("obj.copper_ore")) {
            mes("You need some tin and copper ore to smelt a bronze bar.")
            return
        }
        if (invDel(inv, "obj.tin_ore", 1).failure || invDel(inv, "obj.copper_ore", 1).failure) {
            return
        }
        mesbox("You smelt the copper and tin together in the furnace.")
        delay(2)
        mesbox("You retrieve a bar of bronze.")
        statAdvance("stat.smithing", 6.2)
        invAdd(inv, TutorialObj.BRONZE_BAR, 1)
        guide.advance(player, TutorialStage.MINING_GUIDE_BAR)
    }

    private suspend fun ProtectedAccess.buryBones() {
        if (invDel(inv, TutorialObj.BONES, 1).failure) {
            return
        }
        mes("You bury the bones.")
        statAdvance("stat.prayer", 4.5)
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
                    happy,
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
                    happy,
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
            else ->
                chatNpc(
                    happy,
                    "Hello again. Is there something you'd like to hear more about?",
                )
        }
    }

    private suspend fun Dialogue.masterChef() {
        if (player.tutorialStage == TutorialStage.KITCHEN_ENTERED) {
            chatNpc(
                happy,
                "Ah! Welcome, newcomer. I am the Master Chef, Lev. It is here that I will teach " +
                    "you how to cook food truly fit for a king.",
            )
            if (access.giveTutorialItems(TutorialObj.POT_FLOUR to 1, TutorialObj.BUCKET_WATER to 1)) {
                mesbox(
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
            TutorialStage.QUEST_HOUSE_ENTERED -> {
                chatNpc(
                    neutral,
                    "Ah. Welcome, adventurer. I'm here to tell you all about quests. Let's start " +
                        "by opening your quest list.",
                )
                guide.advance(player, TutorialStage.QUEST_LIST_OPENED)
            }
            TutorialStage.QUEST_JOURNAL_READ -> {
                chatNpc(
                    neutral,
                    "Now that you have the quest list open, you can see all the quests within it. " +
                        "Opening a quest's journal will provide you with more information on it.",
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
            TutorialStage.MINING_GUIDE_TALKED -> {
                chatNpc(
                    happy,
                    "Hi there. You must be new around here. So what do I call you? 'Newcomer' seems " +
                        "so impersonal, and if we're going to be working together, I'd rather call " +
                        "you by name.",
                )
                chatPlayer(happy, "You can call me ${player.displayName}.")
                chatNpc(
                    happy,
                    "Ok then, ${player.displayName}. My name is Dezzick and I'm a miner by trade. " +
                        "Let's teach you how to mine.",
                )
                if (access.giveTutorialItems(TutorialObj.BRONZE_PICKAXE to 1)) {
                    mesbox("The mining instructor gives you a <col=000080>bronze pickaxe<col=000000>.")
                    guide.advance(player, TutorialStage.TIN_MINED)
                }
            }
            TutorialStage.MINING_GUIDE_BAR -> {
                chatNpc(
                    neutral,
                    "Now that you've got a bar, you can smith it into a weapon. To smith something, " +
                        "you need a hammer and an anvil. There's some anvils just here that you can " +
                        "use. See if you can make a bronze dagger.",
                )
                if (access.giveTutorialItems(TutorialObj.HAMMER to 1)) {
                    mesbox("The mining instructor gives you a <col=000080>hammer<col=000000>.")
                    guide.advance(player, TutorialStage.DAGGER_SMITHED)
                }
            }
            else -> chatNpc(neutral, "Keep practising your mining and smithing.")
        }
    }

    private suspend fun Dialogue.combatInstructor() {
        when (player.tutorialStage) {
            TutorialStage.COMBAT_GUIDE_TALKED -> {
                chatNpc(
                    neutral,
                    "Do I look like I care? To me you're just another newcomer who thinks they're " +
                        "ready to fight.",
                )
                chatNpc(
                    happy,
                    "I am Vannaka, the greatest swordsman alive, and I'm here to teach you the " +
                        "basics of combat.",
                )
                chatNpc(neutral, "Let's get started by teaching you to wield a weapon.")
                guide.advance(player, TutorialStage.DAGGER_EQUIPPED)
            }
            TutorialStage.EQUIPMENT_STATS_OPENED -> {
                chatNpc(
                    happy,
                    "Very good, but that little butter knife isn't going to protect you much. " +
                        "Let's get you something a bit better. Once you're properly equipped we " +
                        "can move on to some actual combat.",
                )
                if (access.giveTutorialItems(TutorialObj.BRONZE_SWORD to 1, TutorialObj.WOODEN_SHIELD to 1)) {
                    mesbox(
                        "The combat instructor gives you a <col=000080>bronze sword<col=000000> " +
                            "and a <col=000000><col=000080>wooden shield<col=000000>.",
                    )
                    guide.advance(player, TutorialStage.SWORD_EQUIPPED)
                }
            }
            TutorialStage.COMBAT_GUIDE_MELEE -> {
                chatNpc(
                    happy,
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
            TutorialStage.ACCOUNT_GUIDE_TALKED -> {
                chatNpc(happy, "I'm the Account Guide. I'm here to tell you about your account.")
                chatNpc(
                    neutral,
                    "There's various things you can do with your account which is where the Account " +
                        "Management menu comes into play. Let's take a look at it now.",
                )
                guide.advance(player, TutorialStage.ACCOUNT_MENU_REQUESTED)
            }
            TutorialStage.ACCOUNT_MENU_OPENED -> {
                chatNpc(
                    neutral,
                    "As you can see, there's a few things you can do with your account. First, you " +
                        "have membership. All accounts are free to play by default but if you " +
                        "desire, you can buy membership to unlock members' worlds.",
                )
                chatNpc(
                    neutral,
                    "Your account is very valuable and it's important you look after it. Various " +
                        "tools can help with this, such as Bank PINs and two-factor authentication.",
                )
                chatNpc(
                    happy,
                    "Anyway, that's everything I have to tell you about accounts. Let me know if " +
                        "you need a recap, otherwise head on to your next instructor.",
                )
                guide.advance(player, TutorialStage.ACCOUNT_GUIDE_DONE)
            }
            else -> chatNpc(happy, "Ask me about your account whenever you like.")
        }
    }

    private suspend fun Dialogue.brotherBrace() {
        when (player.tutorialStage) {
            TutorialStage.CHAPEL_ENTERED -> {
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
        when (player.tutorialStage) {
            TutorialStage.MAGIC_GUIDE_TALKED -> {
                chatNpc(
                    happy,
                    "Good day, newcomer. My name is Terrova. I'm here to tell you about Magic. " +
                        "Let's start by opening your magic interface.",
                )
                guide.advance(player, TutorialStage.MAGIC_BOOK_OPENED)
            }
            TutorialStage.RUNES_GIVEN -> {
                chatNpc(
                    neutral,
                    "Currently you can only cast one offensive spell called Wind Strike. Let's " +
                        "try it out on one of those chickens.",
                )
                if (access.giveTutorialItems(TutorialObj.AIR_RUNE to 25, TutorialObj.MIND_RUNE to 15)) {
                    mesbox(
                        "Terrova gives you some <col=000080>air runes<col=000000> and " +
                            "<col=000080>mind runes<col=000000>.",
                    )
                    guide.advance(player, TutorialStage.WIND_STRIKE_CAST)
                }
            }
            TutorialStage.WIND_STRIKE_CAST -> {
                chatNpc(
                    happy,
                    "Well done. You've completed your training here on Tutorial Island. Take these " +
                        "runes with you, and speak to me again when you're ready to leave.",
                )
                guide.advance(player, TutorialStage.COMPLETE)
            }
            TutorialStage.COMPLETE -> {
                chatNpc(
                    happy,
                    "You're nearly finished with the tutorial. All you need to do now is move on " +
                        "to the mainland. Just speak with me to continue.",
                )
                guide.advance(player, TutorialStage.READY_TO_LEAVE)
            }
            TutorialStage.READY_TO_LEAVE ->
                chatNpc(
                    happy,
                    "Excellent, I think that covers everything. When you're ready to go, use your " +
                        "home teleport spell to teleport to Lumbridge!",
                )
            else -> chatNpc(happy, "Magic is a powerful skill. Keep practising.")
        }
    }

    private suspend fun Dialogue.ironmanTutor() {
        chatNpc(
            happy,
            "Hello, ${player.displayName}. I'm the Ironman tutor. You can choose to become an " +
                "Ironman while you're on Tutorial Island. Some things can't be changed later.",
        )
    }

    private companion object {
        private const val FIRE_DURATION = 200
    }
}
