package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.sign
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifOpenMainSidePair
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerCoordsChanged
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
        onOpNpc1(TutorialNpc.IRONMAN_TUTOR_VISIBLE) { startDialogue(it.npc) { ironmanTutor() } }
    }

    private fun ScriptContext.bindDoors() {
        onOpLoc1(TutorialLoc.DOOR_1) {
            if (player.tutorialStage < TutorialStage.GUIDE_DONE) {
                mesbox("You need to talk to the Gielinor Guide before you are allowed to proceed through this door.")
                return@onOpLoc1
            }
            openDoor(it.loc)
            player.advanceIf(TutorialStage.GUIDE_DONE, TutorialStage.OUTSIDE)
        }
        onOpLoc1(TutorialLoc.GATE_LEFT) {
            if (player.tutorialStage < TutorialStage.SHRIMP_COOKED) {
                mesbox("You need to talk to the Survival Guide and complete her tasks before you are allowed to proceed through this gate.")
                return@onOpLoc1
            }
            openGate(it.loc)
            player.advanceIf(TutorialStage.SHRIMP_COOKED, TutorialStage.GATE_OPENED)
        }
        onOpLoc1(TutorialLoc.GATE_RIGHT) {
            if (player.tutorialStage < TutorialStage.SHRIMP_COOKED) {
                mesbox("You need to talk to the Survival Guide and complete her tasks before you are allowed to proceed through this gate.")
                return@onOpLoc1
            }
            openGate(it.loc)
            player.advanceIf(TutorialStage.SHRIMP_COOKED, TutorialStage.GATE_OPENED)
        }
        onOpLoc1(TutorialLoc.DOOR_2) {
            openDoor(it.loc)
            player.advanceIf(TutorialStage.GATE_OPENED, TutorialStage.KITCHEN_ENTERED)
        }
        onOpLoc1(TutorialLoc.DOOR_3) {
            if (player.tutorialStage < TutorialStage.BREAD_BAKED) {
                mesbox("You need to finish the Master Chef's tasks first.")
                return@onOpLoc1
            }
            openDoor(it.loc)
            player.advanceIf(TutorialStage.BREAD_BAKED, TutorialStage.QUEST_PATH)
            player.advanceIf(TutorialStage.QUEST_PATH, TutorialStage.QUEST_HOUSE_ENTERED)
        }
        onOpLoc1(TutorialLoc.DOOR_4) {
            openDoor(it.loc)
            player.advanceIf(TutorialStage.QUEST_HOUSE_ENTERED, TutorialStage.QUEST_HOUSE_INSIDE)
        }
        onOpLoc1(TutorialLoc.DOOR_6) {
            if (player.tutorialStage < TutorialStage.BANK_OPENED) {
                mesbox("You need to open your bank first.")
                return@onOpLoc1
            }
            if (player.tutorialStage < TutorialStage.POLL_BOOTH) {
                mesbox("You need to visit the poll booth before you can proceed through this door.")
                return@onOpLoc1
            }
            openDoor(it.loc)
            player.advanceIf(TutorialStage.POLL_BOOTH, TutorialStage.ACCOUNT_GUIDE_ARRIVED)
        }
        onOpLoc1(TutorialLoc.DOOR_7) {
            if (player.tutorialStage < TutorialStage.ACCOUNT_GUIDE_DONE) {
                mesbox("You need to talk to the Account Guide before you are allowed to proceed through this door.")
                return@onOpLoc1
            }
            openDoor(it.loc)
            player.advanceIf(TutorialStage.ACCOUNT_GUIDE_DONE, TutorialStage.CHAPEL_ARRIVED)
        }
        onOpLoc1(TutorialLoc.DOOR_8) {
            if (player.tutorialStage < TutorialStage.PRAYER_GUIDE_DONE) {
                mesbox("You need to finish Brother Brace's tasks before you are allowed to proceed through this door.")
                return@onOpLoc1
            }
            openDoor(it.loc)
            player.advanceIf(TutorialStage.PRAYER_GUIDE_DONE, TutorialStage.MAGIC_GUIDE_ARRIVED)
        }
        onOpLoc1(TutorialLoc.LADDER_DOWN) {
            climb(
                CoordGrid(3088, 9520, 0),
                TutorialStage.QUEST_GUIDE_DONE,
                TutorialStage.LADDER_DOWN,
                "I don't think you're ready to go down there yet.",
            )
        }
        onOpLoc1(TutorialLoc.LADDER_DOWN_2) {
            climb(
                CoordGrid(3111, 9526, 0),
                TutorialStage.QUEST_GUIDE_DONE,
                TutorialStage.LADDER_DOWN,
                "I don't think you're ready to go down there yet.",
            )
        }
        onOpLoc1(TutorialLoc.LADDER_UP) {
            climb(
                CoordGrid(3088, 3120, 0),
                TutorialStage.COMBAT_GUIDE_DONE,
                TutorialStage.LADDER_UP,
                "You need to finish the combat tutorial first.",
            )
        }
        onOpLoc1(TutorialLoc.LADDER_UP_2) {
            climb(
                CoordGrid(3111, 3125, 0),
                TutorialStage.COMBAT_GUIDE_DONE,
                TutorialStage.LADDER_UP,
                "You need to finish the combat tutorial first.",
            )
        }
        onOpLoc1(TutorialLoc.COMBAT_GATE_LEFT) {
            if (player.tutorialStage < TutorialStage.DAGGER_SMITHED) {
                mesbox("You need to finish with Mining and Smithing first.")
                return@onOpLoc1
            }
            openGate(it.loc)
            player.advanceIf(TutorialStage.DAGGER_SMITHED, TutorialStage.COMBAT_GATE)
        }
        onOpLoc1(TutorialLoc.COMBAT_GATE_RIGHT) {
            if (player.tutorialStage < TutorialStage.DAGGER_SMITHED) {
                mesbox("You need to finish with Mining and Smithing first.")
                return@onOpLoc1
            }
            openGate(it.loc)
            player.advanceIf(TutorialStage.DAGGER_SMITHED, TutorialStage.COMBAT_GATE)
        }
        onOpLoc1(TutorialLoc.RAT_GATE_LEFT) { useRatGate(it.loc) }
        onOpLoc1(TutorialLoc.RAT_GATE_RIGHT) { useRatGate(it.loc) }
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
                camMoveToV3(CoordGrid(3123, 3123, 0), 450, 232, 100, false)
                camLookAtV3(CoordGrid(3119, 3121, 0), 350, 232, 100, false)
                objbox(
                    TutorialObj.CERT_FURNACE_ICON,
                    "A flag appears on the booth to let you know when you're invited to vote in " +
                        "a poll.",
                )
                camReset()
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
            } else {
                mes("You haven't got anything suitable for cooking! The master chef can help you out with that.")
            }
        }
        onOpNpc1(TutorialNpc.FISHING_SPOT) { catchShrimp() }
        for (tree in listOf(TutorialLoc.TREE, TutorialLoc.TREE_2)) {
            onOpLoc1(tree) { chopTree(it.loc) }
        }
        onOpLoc1(TutorialLoc.OAK_TREE) {
            if (player.tutorialStage == TutorialStage.AXE_GIVEN) {
                mes(
                    "You won't be able to chop oak trees until you have a Woodcutting level of " +
                        "15. You'll advance to higher Woodcutting levels by chopping down normal " +
                        "trees. At higher Woodcutting levels you will find even more trees you " +
                        "can cut.",
                )
            } else {
                mes("Perhaps you've done enough woodcutting now.")
            }
        }
        onOpLoc1(TutorialLoc.TIN_ROCK) { mineTin(it.loc) }
        onOpLoc1(TutorialLoc.COPPER_ROCK) { mineCopper(it.loc) }
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

    private suspend fun ProtectedAccess.climb(
        destination: CoordGrid,
        from: Int,
        to: Int,
        denial: String,
    ) {
        if (player.tutorialStage < from) {
            mesbox(denial)
            return
        }
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

    private fun doorExit(from: CoordGrid, loc: BoundLocInfo, leaves: List<OpenLeaf>): CoordGrid {
        val door = loc.coords
        val leaf = leaves.firstOrNull()?.coords
        val dx = (door.x - from.x).sign
        val dz = (door.z - from.z).sign
        return when (loc.angle) {
            LocAngle.West,
            LocAngle.East -> {
                val step = if (dx != 0) dx else leaf?.let { (it.x - door.x).sign } ?: dz
                door.translateX(step)
            }
            LocAngle.North,
            LocAngle.South -> {
                val step = if (dz != 0) dz else leaf?.let { (it.z - door.z).sign } ?: dx
                door.translateZ(step)
            }
        }
    }

    private suspend fun ProtectedAccess.openDoor(loc: BoundLocInfo) {
        val leaves = DOOR_OPEN[loc.internalName]
        if (leaves == null) {
            locRepo.del(loc, DOOR_OPEN_TICKS)
            return
        }
        locRepo.del(loc, DOOR_OPEN_TICKS)
        soundSynth(62)
        val from = coords
        val dest = doorExit(from, loc, leaves)
        if (from != dest) {
            forcedWalk(listOf(loc.coords, dest), crossTiles = from.chebyshevDistance(dest))
        }
        player.addOpenLeaves(leaves, DOOR_OPEN_TICKS - 1)
        locRepo.change(loc, TutorialLoc.INVISWALL, DOOR_OPEN_TICKS - 1)
    }

    private suspend fun ProtectedAccess.openGate(loc: BoundLocInfo) {
        val spec = GATE_OPEN[loc.internalName]
        if (spec == null) {
            locRepo.del(loc, DOOR_OPEN_TICKS)
            return
        }
        val halves = mutableListOf<LocInfo>()
        for (half in spec.halves) {
            val gate =
                if (half.name == loc.internalName && half.coords == loc.coords) {
                    LocInfo(loc.layer, loc.coords, loc.entity)
                } else {
                    findNearbyLoc(half.coords, half.name)
                }
            if (gate != null) {
                locRepo.del(gate, DOOR_OPEN_TICKS)
                halves += gate
            }
        }
        if (spec === WOODEN_GATE) {
            soundSynth(67)
        }
        val from = coords
        val dest = doorExit(from, loc, spec.leaves)
        if (from != dest) {
            forcedWalk(listOf(loc.coords, dest), crossTiles = from.chebyshevDistance(dest))
        }
        for (gate in halves) {
            locRepo.change(gate, TutorialLoc.INVISWALL, DOOR_OPEN_TICKS - 1)
        }
        player.addOpenLeaves(spec.leaves, DOOR_OPEN_TICKS - 1)
    }

    private fun Player.addOpenLeaves(leaves: List<OpenLeaf>, duration: Int = DOOR_OPEN_TICKS) {
        for (leaf in leaves) {
            locRepo.add(leaf.coords, leaf.id, duration, leaf.angle, LocShape.WallStraight)
        }
    }

    private suspend fun ProtectedAccess.useRatGate(loc: BoundLocInfo) {
        if (player.tutorialStage < TutorialStage.COMBAT_TAB_OPENED) {
            sayCombatInstructor("Oi! Get away from there. Only enter the rat cage when I say so.")
            return
        }
        if (player.tutorialStage in TutorialStage.RANGED_GIVEN until TutorialStage.RAT_ENGAGED_RANGED) {
            sayCombatInstructor("No, don't enter the pit. Range the rats from outside the cage.")
            return
        }
        openGate(loc)
    }

    private suspend fun ProtectedAccess.sayCombatInstructor(line: String) {
        val instructor =
            npcRepo.findAll(ZoneKey.from(player.coords), 2).firstOrNull {
                it.visType.internalName == TutorialNpc.COMBAT_INSTRUCTOR ||
                    it.type.internalName == TutorialNpc.COMBAT_INSTRUCTOR
            }
        if (instructor != null) {
            startDialogue(instructor) { chatNpc(neutral, line) }
        } else {
            mesbox(line)
        }
    }

    private fun findNearbyLoc(origin: CoordGrid, name: String): LocInfo? {
        val id = name.asRSCM(RSCMType.LOC)
        return locRepo.findAll(ZoneKey.from(origin)).firstOrNull {
            it.entity.id == id && it.coords.chebyshevDistance(origin) <= 1
        }
    }

    private suspend fun ProtectedAccess.mesOverlay(text: String) {
        player.ifOpenOverlay("interface.mesoverlay", "component.chatbox:chatoverlay", eventBus)
        runClientScript(MESOVERLAY, text)
    }

    private fun ProtectedAccess.tutorialStat(stat: String, xp: Double) {
        player.statAdvance(stat, xp, rate = 1.0, globalRate = 1.0)
    }

    private suspend fun ProtectedAccess.makeDough() {
        if (player.tutorialStage !in
            TutorialStage.CHEF_TALKED until TutorialStage.BREAD_BAKED
        ) {
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
        TutorialHighlights.clearItems(player)
        guide.advance(player, TutorialStage.DOUGH_MADE)
        objbox(TutorialObj.BREAD_DOUGH, "You make some dough.")
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
        delay(8)
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
        TutorialHighlights.clearItems(player)
        if (player.inv.contains(TutorialObj.RAW_SHRIMP)) {
            TutorialHighlights.item(player, TutorialObj.RAW_SHRIMP)
        }
        guide.advance(player, TutorialStage.FIRE_LIT)
    }

    private suspend fun ProtectedAccess.cookShrimp() {
        if (player.tutorialStage != TutorialStage.FIRE_LIT) {
            return
        }
        mesOverlay("Your character is now attempting to cook some shrimp. This will only take a few seconds.")
        soundSynth(2577)
        delay(3)
        if (invDel(inv, TutorialObj.RAW_SHRIMP, 1).failure) {
            return
        }
        tutorialStat("stat.cooking", 30.0)
        invAdd(inv, TutorialObj.SHRIMP, 1)
        TutorialHighlights.clearItems(player)
        guide.advance(player, TutorialStage.SHRIMP_COOKED)
        objbox(TutorialObj.SHRIMP, "You manage to cook some shrimp.")
    }

    private suspend fun ProtectedAccess.bakeBread() {
        if (player.tutorialStage != TutorialStage.DOUGH_MADE) {
            return
        }
        mesOverlay("Your character is now attempting to bake some bread. This will only take a few seconds.")
        soundSynth(2580)
        delay(5)
        if (invDel(inv, TutorialObj.BREAD_DOUGH, 1).failure) {
            return
        }
        tutorialStat("stat.cooking", 40.0)
        invAdd(inv, TutorialObj.BREAD, 1)
        guide.advance(player, TutorialStage.BREAD_BAKED)
        objbox(TutorialObj.BREAD, "You manage to bake some bread.")
    }

    private suspend fun ProtectedAccess.catchShrimp() {
        if (player.tutorialStage != TutorialStage.INVENTORY_VIEWED) {
            if (player.tutorialStage < TutorialStage.INVENTORY_VIEWED) {
                mes("You cannot fish here yet. You must progress further in the tutorial.")
            }
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
        delay(6)
        invAdd(inv, TutorialObj.RAW_SHRIMP, 1)
        tutorialStat("stat.fishing", 10.0)
        TutorialHighlights.item(player, TutorialObj.RAW_SHRIMP, alert = true)
        guide.advance(player, TutorialStage.SHRIMP_CAUGHT)
        objbox(TutorialObj.RAW_SHRIMP, "You manage to catch some shrimp.")
    }

    private suspend fun ProtectedAccess.chopTree(loc: BoundLocInfo) {
        if (player.tutorialStage != TutorialStage.AXE_GIVEN) {
            when {
                player.tutorialStage < TutorialStage.AXE_GIVEN ->
                    mes("You cannot cut down this tree yet. You must progress further in the tutorial.")
                player.tutorialStage >= TutorialStage.LOGS_CUT ->
                    mes("Perhaps you've done enough woodcutting now.")
            }
            return
        }
        if (!player.inv.contains(TutorialObj.BRONZE_AXE)) {
            mes("You need an axe to chop down this tree.")
            return
        }
        locRepo.change(loc, TutorialLoc.TREE_STUMP, TREE_STUMP_TICKS)
        mesOverlay(
            "Your character is now attempting to cut down the tree. Sit back for a moment " +
                "while he does all the hard work.",
        )
        delay(5)
        if (invAdd(inv, TutorialObj.LOGS, 1).failure) {
            return
        }
        tutorialStat("stat.woodcutting", 25.0)
        TutorialHighlights.item(player, TutorialObj.LOGS)
        TutorialHighlights.item(player, TutorialObj.TINDERBOX)
        guide.advance(player, TutorialStage.LOGS_CUT)
        objbox(TutorialObj.LOGS, "You manage to cut some logs.")
    }

    private suspend fun ProtectedAccess.mineTin(loc: BoundLocInfo) {
        if (player.tutorialStage != TutorialStage.MINING_GUIDE_TALKED) {
            if (player.tutorialStage < TutorialStage.MINING_GUIDE_TALKED) {
                mes("You are not ready to mine yet. Please follow the tutorial and you'll be mining in no time.")
            }
            return
        }
        if (!player.inv.contains(TutorialObj.BRONZE_PICKAXE)) {
            mes("You need a pickaxe to mine this rock.")
            return
        }
        locRepo.change(loc, TutorialLoc.ROCK_DEPLETED, ROCK_DEPLETED_TICKS)
        mesOverlay("Your character is now attempting to mine the rock.<br>This should only take a few seconds.")
        delay(5)
        invAdd(inv, TutorialObj.TIN_ORE, 1)
        tutorialStat("stat.mining", 17.5)
        soundSynth(3600)
        guide.advance(player, TutorialStage.TIN_MINED)
        objbox(TutorialObj.TIN_ORE, "You manage to mine some tin.")
    }

    private suspend fun ProtectedAccess.mineCopper(loc: BoundLocInfo) {
        if (player.tutorialStage != TutorialStage.TIN_MINED) {
            if (player.tutorialStage < TutorialStage.TIN_MINED) {
                mes("You are not ready to mine yet. Please follow the tutorial and you'll be mining in no time.")
            }
            return
        }
        if (!player.inv.contains(TutorialObj.BRONZE_PICKAXE)) {
            mes("You need a pickaxe to mine this rock.")
            return
        }
        locRepo.change(loc, TutorialLoc.ROCK_DEPLETED, ROCK_DEPLETED_TICKS)
        mesOverlay("Your character is now attempting to mine the rock.<br>This should only take a few seconds.")
        delay(5)
        invAdd(inv, TutorialObj.COPPER_ORE, 1)
        tutorialStat("stat.mining", 17.5)
        soundSynth(3600)
        guide.advance(player, TutorialStage.COPPER_MINED)
        objbox(TutorialObj.COPPER_ORE, "You manage to mine some copper.")
    }

    private suspend fun ProtectedAccess.smeltBar() {
        if (player.tutorialStage != TutorialStage.COPPER_MINED) {
            if (player.tutorialStage < TutorialStage.COPPER_MINED) {
                mes("This is a furnace for smelting metal. You'll learn how to use it soon.")
            }
            return
        }
        if (!player.inv.contains(TutorialObj.TIN_ORE) ||
            !player.inv.contains(TutorialObj.COPPER_ORE)
        ) {
            mes("You need some tin and copper ore to smelt a bronze bar.")
            return
        }
        mesOverlay("You smelt the copper and tin together in the furnace.")
        soundSynth(2725)
        delay(3)
        if (invDel(inv, TutorialObj.TIN_ORE, 1).failure ||
            invDel(inv, TutorialObj.COPPER_ORE, 1).failure
        ) {
            return
        }
        mesOverlay("You retrieve a bar of bronze.")
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
            else -> {
                chatNpc(
                    neutral,
                    "Welcome back. You have already learnt the first thing needed to succeed in " +
                        "this world: talking to other people!",
                )
                chatNpc(
                    neutral,
                    "You will find many inhabitants of this world have useful things to say to " +
                        "you. By clicking on them, you can talk to them.",
                )
                chatNpc(
                    happy,
                    "To continue the tutorial go through that door over there and speak to your " +
                        "first instructor!",
                )
            }
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
                    guide.advance(player, TutorialStage.NET_GIVEN)
                    objbox(
                        TutorialObj.NET,
                        "The survival expert gives you a <col=000080>small fishing net<col=000000>.",
                    )
                }
            }
            TutorialStage.NET_GIVEN, TutorialStage.INVENTORY_VIEWED -> {
                chatNpc(
                    neutral,
                    "Hello again. First up, we're going to do some fishing. There's some shrimp " +
                        "in this pond here. Let's try and catch some.",
                )
                if (!player.inv.contains(TutorialObj.NET) &&
                    access.giveTutorialItems(TutorialObj.NET to 1)
                ) {
                    objbox(
                        TutorialObj.NET,
                        "The survival expert gives you a <col=000080>small fishing net<col=000000>.",
                    )
                }
            }
            TutorialStage.SHRIMP_CAUGHT ->
                chatNpc(neutral, "Hello again. You should take a look at that menu before we continue.")
            TutorialStage.SKILLS_VIEWED -> {
                chatPlayer(happy, "I've managed to catch some shrimp.")
                chatNpc(
                    happy,
                    "Excellent work. Next you'll want to cook those shrimp. To do that, you'll " +
                        "need a fire. This brings us on to the Woodcutting and Firemaking skills.",
                )
                if (access.giveTutorialItems(TutorialObj.BRONZE_AXE to 1, TutorialObj.TINDERBOX to 1)) {
                    guide.advance(player, TutorialStage.AXE_GIVEN)
                    doubleobjbox(
                        TutorialObj.BRONZE_AXE,
                        TutorialObj.TINDERBOX,
                        "The survival expert gives you a <col=000080>bronze axe<col=000000> and a " +
                            "<col=000000><col=000080>tinderbox<col=000000>.",
                    )
                }
            }
            TutorialStage.AXE_GIVEN -> {
                chatNpc(
                    happy,
                    "Next up you need to make a fire. First, you'll need to cut down a tree to get " +
                        "some logs.",
                )
                val missing =
                    buildList {
                        if (!player.inv.contains(TutorialObj.BRONZE_AXE)) {
                            add(TutorialObj.BRONZE_AXE to 1)
                        }
                        if (!player.inv.contains(TutorialObj.TINDERBOX)) {
                            add(TutorialObj.TINDERBOX to 1)
                        }
                    }
                if (missing.isNotEmpty() && access.giveTutorialItems(*missing.toTypedArray())) {
                    val axe = missing.any { it.first == TutorialObj.BRONZE_AXE }
                    val tinderbox = missing.any { it.first == TutorialObj.TINDERBOX }
                    when {
                        axe && tinderbox ->
                            doubleobjbox(
                                TutorialObj.BRONZE_AXE,
                                TutorialObj.TINDERBOX,
                                "The survival expert gives you a <col=000080>bronze axe" +
                                    "<col=000000> and a <col=000000><col=000080>tinderbox" +
                                    "<col=000000>.",
                            )
                        axe ->
                            objbox(
                                TutorialObj.BRONZE_AXE,
                                "The survival expert gives you a <col=000080>bronze axe" +
                                    "<col=000000>.",
                            )
                        tinderbox ->
                            objbox(
                                TutorialObj.TINDERBOX,
                                "The survival expert gives you a <col=000080>tinderbox" +
                                    "<col=000000>.",
                            )
                    }
                }
            }
            TutorialStage.LOGS_CUT ->
                chatNpc(
                    happy,
                    "Now that you have some shrimp, you're going to want to cook them. To do " +
                        "that, you'll need a fire.",
                )
            TutorialStage.FIRE_LIT ->
                chatNpc(
                    happy,
                    "Now that you have some shrimp, you're going to want to cook them. All you " +
                        "need to do is use them on a fire.",
                )
            else -> survivalRecap()
        }
    }

    private suspend fun Dialogue.survivalRecap() {
        chatNpc(quiz, "Hello again. Is there something you'd like to hear more about?")
        while (true) {
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
                1 -> {
                    chatPlayer(quiz, "Tell me about my skills again.")
                    chatNpc(
                        neutral,
                        "Every skill is listed in the skills menu. Here you can see what your " +
                            "current levels are and how much experience you have.",
                    )
                    chatNpc(
                        neutral,
                        "As you move your mouse over the various skills the small tooltip will " +
                            "show you the exact amount of experience you have and how much is " +
                            "needed to get to the next level.",
                    )
                    chatNpc(
                        neutral,
                        "You can also click on a skill to open up the relevant skillguide. In " +
                            "the skillguide, you can see all of the unlocks available in that skill.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about Woodcutting again.")
                    chatNpc(
                        neutral,
                        "Woodcutting, eh? Don't worry, newcomer, it's really very easy. Simply " +
                            "equip your axe and click on a nearby tree to chop away.",
                    )
                    chatNpc(
                        neutral,
                        "As you explore the mainland you will discover many different kinds of " +
                            "trees that will require different Woodcutting levels to chop down.",
                    )
                    chatNpc(
                        neutral,
                        "Logs are not only useful for making fires. Many archers use the skill " +
                            "known as Fletching to craft their own bows and arrows from trees.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about Firemaking again.")
                    chatNpc(
                        neutral,
                        "Certainly, newcomer. When you have logs simply use your tinderbox on " +
                            "them. If successful, you will start a fire.",
                    )
                    chatNpc(
                        neutral,
                        "You can also set fire to logs you find lying on the floor already, and " +
                            "some other things can also be set alight...",
                    )
                    chatNpc(neutral, "A tinderbox is always a useful item to keep around!")
                }
                else -> {
                    if (!survivalMoreRecap()) {
                        return
                    }
                }
            }
            chatNpc(quiz, "Is there anything else you'd like to hear more about?")
        }
    }

    private suspend fun Dialogue.survivalMoreRecap(): Boolean {
        return when (choice3("Tell me about Fishing again.", 1, "Tell me about Cooking again.", 2, "Nothing thanks.", 3)) {
            1 -> {
                chatPlayer(quiz, "Tell me about Fishing again.")
                chatNpc(
                    neutral,
                    "Ah, yes. Fishing! Fishing is undoubtedly one of the more popular hobbies " +
                        "here in Gielinor!",
                )
                chatNpc(
                    neutral,
                    "Whenever you see sparkling waters, you can be sure there's probably some " +
                        "good fishing to be had there!",
                )
                chatNpc(
                    neutral,
                    "Not only are fish absolutely delicious when cooked, they will also heal " +
                        "lost health.",
                )
                chatNpc(
                    neutral,
                    "I would recommend everybody has a go at Fishing at least once in their lives!",
                )
                true
            }
            2 -> {
                chatPlayer(quiz, "Tell me about Cooking again.")
                chatNpc(
                    neutral,
                    "Yes, the most basic of survival techniques. Most simple foods can be used on " +
                        "a fire to cook them. If you're feeling a bit fancier, you can also use a " +
                        "range to cook the food instead.",
                )
                chatNpc(
                    neutral,
                    "Eating cooked food will restore lost health. The harder something is to " +
                        "cook, the more it will heal you.",
                )
                true
            }
            else -> {
                chatPlayer(neutral, "Nothing thanks.")
                false
            }
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
                guide.advance(player, TutorialStage.CHEF_TALKED)
                TutorialHighlights.item(player, TutorialObj.POT_FLOUR)
                TutorialHighlights.item(player, TutorialObj.BUCKET_WATER)
                doubleobjbox(
                    TutorialObj.POT_FLOUR,
                    TutorialObj.BUCKET_WATER,
                    "The master chef gives you some <col=000080>flour<col=000000> and some " +
                        "<col=000080>water<col=000000>.",
                )
            }
        } else if (player.tutorialStage < TutorialStage.BREAD_BAKED) {
            chatNpc(neutral, "Time for you to learn the fine art of cooking bread.")
            giveFlourAndWater()
        } else {
            chatNpc(neutral, "Do you need something?")
            when (choice3("Tell me about making dough again.", 1, "Tell me about range cooking again.", 2, "Nothing thanks.", 3)) {
                1 -> {
                    chatPlayer(quiz, "Tell me about making dough again.")
                    chatNpc(
                        neutral,
                        "It's quite simple. Just use a pot of flour on a bucket of water, or vice " +
                            "versa, and you'll make dough. You can fill a bucket with water at any sink.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about range cooking again.")
                    chatNpc(
                        neutral,
                        "The range is the only place you can cook a lot of the more complex foods " +
                            "in Gielinor. To cook on a range, all you need to do is click on it. " +
                            "You'll need to make sure you have the required items in your " +
                            "inventory though.",
                    )
                }
                else -> chatPlayer(neutral, "Nothing thanks.")
            }
        }
    }

    private fun Player.hasItem(obj: String): Boolean =
        inv.contains(obj) || worn.contains(obj)

    private suspend fun Dialogue.giveCombatGear(): Boolean {
        val items = buildList {
            if (!player.hasItem(TutorialObj.BRONZE_SWORD)) add(TutorialObj.BRONZE_SWORD to 1)
            if (!player.hasItem(TutorialObj.WOODEN_SHIELD)) add(TutorialObj.WOODEN_SHIELD to 1)
        }
        if (items.isEmpty()) {
            return true
        }
        if (!access.giveTutorialItems(*items.toTypedArray())) {
            mesbox(
                "You need some new weapons for this section of the tutorial but you don't have " +
                    "enough inventory room. You'll need to drop something. To do so, right-click " +
                    "on an item you don't want and select drop. Once you've done that, speak to " +
                    "the combat instructor again.",
            )
            return false
        }
        doubleobjbox(
            TutorialObj.BRONZE_SWORD,
            TutorialObj.WOODEN_SHIELD,
            "The combat instructor gives you a <col=000080>bronze sword<col=000000> and a " +
                "<col=000000><col=000080>wooden shield<col=000000>.",
        )
        TutorialHighlights.item(player, TutorialObj.BRONZE_SWORD)
        TutorialHighlights.item(player, TutorialObj.WOODEN_SHIELD)
        return true
    }

    private suspend fun Dialogue.giveRangedGear(): Boolean {
        val items = buildList {
            if (!player.hasItem(TutorialObj.SHORTBOW)) add(TutorialObj.SHORTBOW to 1)
            val arrows = player.inv.count(TutorialObj.BRONZE_ARROW) +
                player.worn.count(TutorialObj.BRONZE_ARROW)
            val missingArrows = (50 - arrows).coerceAtLeast(0)
            if (missingArrows > 0) add(TutorialObj.BRONZE_ARROW to missingArrows)
        }
        if (items.isEmpty()) {
            return true
        }
        if (!access.giveTutorialItems(*items.toTypedArray())) {
            mesbox(
                "You need some new weapons for this section of the tutorial but you don't have " +
                    "enough inventory room. You'll need to drop something. To do so, right-click " +
                    "on an item you don't want and select drop. Once you've done that, speak to " +
                    "the combat instructor again.",
            )
            return false
        }
        doubleobjbox(
            TutorialObj.SHORTBOW,
            TutorialObj.BRONZE_ARROW,
            "The combat instructor gives you a <col=000080>shortbow<col=000000> and some " +
                "<col=000000><col=000080>bronze arrows<col=000000>.",
        )
        TutorialHighlights.item(player, TutorialObj.SHORTBOW)
        TutorialHighlights.item(player, TutorialObj.BRONZE_ARROW)
        return true
    }

    private suspend fun Dialogue.giveFlourAndWater() {
        val hasFlour = player.inv.contains(TutorialObj.POT_FLOUR)
        val hasWater = player.inv.contains(TutorialObj.BUCKET_WATER)
        if (hasFlour && hasWater) {
            return
        }
        if (!hasFlour && !hasWater &&
            access.giveTutorialItems(
                TutorialObj.POT_FLOUR to 1,
                TutorialObj.BUCKET_WATER to 1,
            )
        ) {
            doubleobjbox(
                TutorialObj.POT_FLOUR,
                TutorialObj.BUCKET_WATER,
                "The master chef gives you some <col=000080>flour<col=000000> and some " +
                    "<col=000080>water<col=000000>.",
            )
        } else if (!hasFlour && access.giveTutorialItems(TutorialObj.POT_FLOUR to 1)) {
            objbox(
                TutorialObj.POT_FLOUR,
                "The master chef gives you some <col=000080>flour<col=000000>.",
            )
        } else if (!hasWater && access.giveTutorialItems(TutorialObj.BUCKET_WATER to 1)) {
            objbox(
                TutorialObj.BUCKET_WATER,
                "The master chef gives you some <col=000080>water<col=000000>.",
            )
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
                chatNpc(neutral, "Have you not opened that menu yet?")
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
                objbox(
                    TutorialObj.QUEST_START_ICON,
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
            else -> {
                chatNpc(neutral, "Would you like to hear about quests again?")
                when (choice2("Yes!", 1, "Nope, I'm ready to move on!", 2)) {
                    1 -> {
                        chatPlayer(quiz, "Yes!")
                        chatNpc(
                            neutral,
                            "Within your quest list, you'll unsurprisingly find a list of quests. " +
                                "Opening a quest's journal will provide you with more information on it.",
                        )
                        chatNpc(
                            neutral,
                            "If you haven't started the quest, it will tell you where to begin " +
                                "and what requirements you need. If the quest is in progress, it " +
                                "will remind you what to do next.",
                        )
                    }
                    else -> {
                        chatPlayer(neutral, "Nope, I'm ready to move on!")
                        chatNpc(neutral, "Okay then.")
                    }
                }
            }
        }
    }

    private suspend fun Dialogue.miningInstructor() {
        when (player.tutorialStage) {
            TutorialStage.LADDER_DOWN -> {
                guide.advance(player, TutorialStage.MINING_GUIDE_GREETED)
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
                    guide.advance(player, TutorialStage.MINING_GUIDE_TALKED)
                    objbox(
                        TutorialObj.BRONZE_PICKAXE,
                        "The mining instructor gives you a <col=000080>bronze pickaxe<col=000000>.",
                    )
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
                    guide.advance(player, TutorialStage.MINING_GUIDE_BAR)
                    objbox(
                        TutorialObj.HAMMER,
                        "The mining instructor gives you a <col=000080>hammer<col=000000>.",
                    )
                }
            }
            TutorialStage.MINING_GUIDE_BAR, TutorialStage.ANVIL_OPENED -> {
                chatNpc(neutral, "See if you can use the anvil here to make a bronze dagger.")
                if (!player.inv.contains(TutorialObj.HAMMER) &&
                    access.giveTutorialItems(TutorialObj.HAMMER to 1)
                ) {
                    objbox(
                        TutorialObj.HAMMER,
                        "The mining instructor gives you a <col=000080>hammer<col=000000>.",
                    )
                }
            }
            else -> miningRecap()
        }
    }

    private suspend fun Dialogue.miningRecap() {
        if (player.tutorialStage >= TutorialStage.MINING_GUIDE_TALKED &&
            !player.inv.contains(TutorialObj.BRONZE_PICKAXE) &&
            access.giveTutorialItems(TutorialObj.BRONZE_PICKAXE to 1)
        ) {
            objbox(
                        TutorialObj.BRONZE_PICKAXE,
                        "The mining instructor gives you a <col=000080>bronze pickaxe<col=000000>.",
                    )
        }
        if (player.tutorialStage >= TutorialStage.MINING_GUIDE_BAR &&
            !player.inv.contains(TutorialObj.HAMMER) &&
            access.giveTutorialItems(TutorialObj.HAMMER to 1)
        ) {
            objbox(
                        TutorialObj.HAMMER,
                        "The mining instructor gives you a <col=000080>hammer<col=000000>.",
                    )
        }
        chatNpc(neutral, "Would you like me to recap anything?")
        when (choice3("Tell me about Mining again.", 1, "Tell me about Smithing again.", 2, "Nothing thanks.", 3)) {
            1 -> {
                chatPlayer(quiz, "Tell me about Mining again.")
                chatNpc(
                    neutral,
                    "Certainly. To mine you need a pickaxe. Different pickaxes let you mine more " +
                        "efficiently.",
                )
                chatNpc(
                    neutral,
                    "Earlier I gave you a bronze pickaxe. It's the most inefficient pickaxe " +
                        "available, but it's perfect for a beginner.",
                )
                chatNpc(
                    neutral,
                    "To mine, simply click on a rock that contains ore while you have a pickaxe " +
                        "with you, and you will keep mining the rock until you manage to get some " +
                        "ore, or until it is empty.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Tell me about Smithing again.")
                chatNpc(
                    neutral,
                    "Smithing is the art of turning metal bars into weapons and armour. Use a " +
                        "hammer on an anvil while you have some bars in your inventory.",
                )
                chatNpc(
                    neutral,
                    "You can smelt ore into bars at a furnace. Bronze is made from tin and copper.",
                )
            }
            else -> chatPlayer(neutral, "Nothing thanks.")
        }
    }

    private suspend fun Dialogue.combatInstructor() {
        when (player.tutorialStage) {
            TutorialStage.COMBAT_GATE -> {
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
                TutorialHighlights.item(player, TutorialObj.BRONZE_DAGGER)
            }
            TutorialStage.DAGGER_EQUIP_TASK ->
                chatNpc(neutral, "Let's get started by teaching you to wield a weapon.")
            TutorialStage.DAGGER_EQUIPPED -> {
                chatNpc(
                    neutral,
                    "Very good, but that little butter knife isn't going to protect you much. " +
                        "Let's get you something a bit better. Once you're properly equipped we " +
                        "can move on to some actual combat.",
                )
                if (giveCombatGear()) {
                    guide.advance(player, TutorialStage.COMBAT_GUIDE_SWORD)
                }
            }
            TutorialStage.COMBAT_GUIDE_SWORD -> {
                chatNpc(neutral, "Let's get you equipped.")
                giveCombatGear()
            }
            TutorialStage.SWORD_EQUIPPED ->
                chatNpc(neutral, "You should check out the combat interface.")
            TutorialStage.COMBAT_TAB_OPENED, TutorialStage.COMBAT_TAB_CLOSED ->
                chatNpc(
                    neutral,
                    "Time for some actual combat! Head into the pen through the nearby gate and " +
                        "kill one of those rats.",
                )
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
                if (giveRangedGear()) {
                    guide.advance(player, TutorialStage.RANGED_GIVEN)
                }
            }
            TutorialStage.RANGED_GIVEN -> {
                chatNpc(
                    neutral,
                    "Use your bow and arrows to kill one of those rats from outside the pen.",
                )
                giveRangedGear()
            }
            else -> combatRecap()
        }
    }

    private suspend fun Dialogue.combatRecap() {
        chatNpc(neutral, "Do you need something?")
        when (choice2("Tell me about melee combat again.", 1, "Nothing thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "Tell me about melee combat again.")
                chatNpc(
                    neutral,
                    "To attack a monster with melee, click on it while you have a weapon equipped. " +
                        "You will walk over to it and start attacking.",
                )
                chatNpc(
                    neutral,
                    "Different attack styles give experience in different skills. You can choose " +
                        "your attack style in the combat interface.",
                )
            }
            else -> chatPlayer(neutral, "Nothing thanks.")
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
                chatNpc(neutral, "Before we go any further, you should have a look at the Account Management menu.")
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
                        "tools can help with this, such as Bank PINs and two-factor authentication " +
                            "options.",
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
            else -> {
                chatNpc(happy, "Hello. Would you like to hear about your account again?")
                when (choice2("Yes!", 1, "No thanks.", 2)) {
                    1 -> {
                        chatPlayer(quiz, "Yes!")
                        chatNpc(happy, "Excellent. Well there's a few things you can do with your account.")
                        chatNpc(
                            happy,
                            "First, you have membership. All accounts are free to play by " +
                                "default but if you desire, you can buy membership to unlock " +
                                "members' worlds. These worlds give you access to extra skills, " +
                                "areas, quests, and more!",
                        )
                    }
                    else -> chatPlayer(neutral, "No thanks.")
                }
            }
        }
    }

    private suspend fun Dialogue.brotherBrace() {
        when (player.tutorialStage) {
            TutorialStage.CHAPEL_ARRIVED -> {
                chatPlayer(happy, "Good day, brother.")
                chatNpc(happy, "Hello, I'm Brother Brace.<br>I'm here to tell you all about Prayer.")
                guide.advance(player, TutorialStage.PRAYER_GUIDE_TALKED)
            }
            TutorialStage.PRAYER_GUIDE_TALKED ->
                chatNpc(neutral, "You should open the indicated menu to continue.")
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
                guide.advance(player, TutorialStage.PRAYER_GUIDE_TALKED_2)
                guide.advance(player, TutorialStage.PRAYER_GUIDE_DONE)
            }
            else -> {
                chatNpc(happy, "Peace be with you, my child. Can I help you?")
                when (choice2("Tell me about Prayers again.", 1, "I'm fine, thanks.", 2)) {
                    1 -> {
                        chatPlayer(quiz, "Tell me about Prayers again.")
                        chatNpc(
                            neutral,
                            "Ah, yes, your prayer list. Prayers can help a lot in combat. Click " +
                                "on the prayer you wish to use to activate it, and click it again " +
                                "to deactivate it.",
                        )
                        chatNpc(
                            neutral,
                            "Active prayers will drain your Prayer Points, which you can recharge " +
                                "by finding an altar or other holy spot and praying there.",
                        )
                        chatNpc(
                            neutral,
                            "As you noticed, most enemies will drop bones when defeated. Burying " +
                                "bones, by clicking them in your inventory, will gain you Prayer " +
                                "experience.",
                        )
                    }
                    else -> chatPlayer(neutral, "I'm fine, thanks.")
                }
            }
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
                chatNpc(
                    neutral,
                    "All you have to do is open the magic interface by clicking on the flashing icon.",
                )
            player.tutorialStage == TutorialStage.MAGIC_BOOK_OPENED -> {
                chatNpc(
                    neutral,
                    "Currently you can only cast one offensive spell called Wind Strike. Let's " +
                        "try it out on one of those chickens.",
                )
                if (access.giveTutorialItems(TutorialObj.AIR_RUNE to 5, TutorialObj.MIND_RUNE to 5)) {
                    guide.advance(player, TutorialStage.RUNES_GIVEN)
                    TutorialHighlights.windStrike(player)
                    doubleobjbox(
                        TutorialObj.AIR_RUNE,
                        TutorialObj.MIND_RUNE,
                        "Terrova gives you some <col=000080>air runes<col=000000> and " +
                            "<col=000080>mind runes<col=000000>.",
                    )
                }
            }
            player.tutorialStage == TutorialStage.RUNES_GIVEN -> {
                val hasRunes =
                    player.inv.count(TutorialObj.AIR_RUNE) >= 1 &&
                        player.inv.count(TutorialObj.MIND_RUNE) >= 1
                if (hasRunes) {
                    chatNpc(
                        neutral,
                        "Sorry, I don't have any more runes just now. But I see you have plenty.",
                    )
                } else if (access.giveTutorialItems(
                        TutorialObj.AIR_RUNE to 5,
                        TutorialObj.MIND_RUNE to 5,
                    )
                ) {
                    doubleobjbox(
                        TutorialObj.AIR_RUNE,
                        TutorialObj.MIND_RUNE,
                        "Terrova gives you some <col=000080>air runes<col=000000> and " +
                            "<col=000080>mind runes<col=000000>.",
                    )
                    TutorialHighlights.windStrike(player)
                }
            }
            player.tutorialStage == TutorialStage.COMPLETE -> finalMagicDialogue()
            player.tutorialStage >= TutorialStage.READY_TO_LEAVE ->
                chatNpc(
                    happy,
                    "Excellent, I think that covers everything. When you're ready to go, use your " +
                        "home teleport spell to teleport to Lumbridge!",
                )
            else ->
                chatNpc(
                    neutral,
                    "Click on the Wind Strike spell in your magic interface, then click on a " +
                        "chicken to cast it.",
                )
        }
    }

    private suspend fun Dialogue.finalMagicDialogue() {
        chatNpc(
            happy,
            "Well, you're all finished here now. I'll give you a reasonable number of runes when " +
                "you leave.",
        )
        if (choice2("Yes.", 1, "No.", 2, title = "Do you want to go to the mainland?") != 1) {
            return
        }
        player.adventurePaths = 1
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
                    "If you're interested in that, you should go and ask the Ironman tutor before " +
                        "you leave this island. He's standing outside my house.",
                )
                return
            }
            2 -> {
                chatPlayer(happy, "Yes, I do want to become an Ironman.")
                chatNpc(
                    neutral,
                    "If you're interested in that, you should go and ask the Ironman tutor before " +
                        "you leave this island. He's standing outside my house.",
                )
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
        objbox(
            TutorialObj.AP_GUIDE_ICON,
            "When you get to Lumbridge, look out for Adventurer Jon, he can be found outside of " +
                "the Sheared Ram pub.",
        )
        chatNpc(
            happy,
            "Excellent, I think that covers everything. When you're ready to go, use your home " +
                "teleport spell to teleport to Lumbridge!",
        )
        TutorialHighlights.homeTeleport(player)
        guide.advance(player, TutorialStage.READY_TO_LEAVE)
    }

    private suspend fun Dialogue.ironmanTutor() {
        if (player.isAnyIronman) {
            chatPlayer(happy, "Hey there Paul.")
            chatNpc(happy, "Hail, ${player.ironmanName()}! What can I do for you?")
        } else {
            chatNpc(
                happy,
                "Hello, ${player.displayName}. I'm Paul, the Ironman tutor. What can I do for you?",
            )
        }
        while (true) {
            when (
                choice3(
                    "Tell me about Ironman.",
                    1,
                    "I'd like to change my Ironman mode.",
                    2,
                    "I'm fine, thanks.",
                    3,
                )
            ) {
                1 -> aboutIronman()
                2 -> {
                    changeIronmanMode()
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'm fine, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.aboutIronman() {
        chatPlayer(quiz, "Tell me about Ironmen.")
        chatNpc(
            neutral,
            "When you play as an Ironman, you do everything for yourself. You don't trade with " +
                "other players, or take their items, or accept their help.",
        )
        chatNpc(
            neutral,
            "As an Ironman, you choose to have these restrictions imposed on you, so everyone " +
                "knows you're doing it properly.",
        )
        chatNpc(neutral, "If you think you have what it takes, you can choose to become a Hardcore Ironman.")
        chatNpc(
            neutral,
            "In addition to the standard restrictions, Hardcore Ironmen only have one life. In " +
                "the event of a dangerous death, your Hardcore Ironman status will be downgraded " +
                "to that of a standard Ironman, and your stats will be frozen on the Hardcore " +
                "Ironman hiscores.",
        )
        chatNpc(
            neutral,
            "If you have some friends, you could stand alone... Together, as Group Ironmen. Group " +
                "Ironmen have the same restrictions as regular Ironmen except you can trade your " +
                "items with each other!",
        )
        chatNpc(
            neutral,
            "If you are a daring bunch, you could even try Hardcore Group Ironmen. With Hardcore " +
                "Group Ironmen you have a set number of lives, when they reach zero, you will " +
                "become regular Group Ironmen.",
        )
        chatNpc(
            neutral,
            "If you ever decided to invite experienced players to your group, you would become " +
                "Unranked Group Ironmen, which are no longer shown on the Group Ironman hiscores.",
        )
        chatNpc(
            neutral,
            "For the ultimate challenge, you can choose to become an Ultimate Ironman, a game " +
                "mode inspired by the player IronNoBank.",
        )
        chatNpc(
            neutral,
            "In addition to the standard restrictions, Ultimate Ironmen are blocked from using " +
                "the bank, and they drop all their items when they die.",
        )
        chatNpc(
            neutral,
            "While you're on Tutorial Island, you can switch freely between being a standard " +
                "Ironman, an Ultimate Ironman, a Hardcore Ironman, a Group Ironman, a Hardcore " +
                "Group Ironman or a regular character.",
        )
        chatNpc(
            neutral,
            "Once you've left this island, you'll be able to find my colleague Adam in Lumbridge, " +
                "but he'll only let you switch your restrictions downwards, not upwards.",
        )
        chatNpc(
            neutral,
            "So he will let Hardcore Ironmen or Ultimate Ironmen downgrade to standard Ironmen, " +
                "and he'll let all types of Ironman become normal players.",
        )
        chatNpc(
            neutral,
            "I must warn you that Ironman mode is not recommended for new players. You can still " +
                "give it a go if you think you're up for the challenge though.",
        )
    }

    private suspend fun Dialogue.changeIronmanMode() {
        chatPlayer(quiz, "I'd like to change my Ironman mode.")
        access.ifOpenMainModal("interface.ironman_setup")
        chatNpc(
            neutral,
            "Make sure you've got your Ironman settings all sorted before you leave Tutorial " +
                "Island. Some things can't be changed later.",
        )
    }

    private fun Player.ironmanName(): String =
        when (gamemode) {
            PlayerGamemode.ULTIMATE_IRONMAN -> "Ultimate Ironman"
            PlayerGamemode.HARDCORE_IRONMAN -> "Hardcore Ironman"
            PlayerGamemode.GROUP_IRONMAN -> "Group Ironman"
            PlayerGamemode.HARDCORE_GROUP_IRONMAN -> "Hardcore Group Ironman"
            PlayerGamemode.UNRANKED_GROUP_IRONMAN -> "Unranked Group Ironman"
            else -> "Ironman"
        }

    private data class OpenLeaf(val id: String, val coords: CoordGrid, val angle: LocAngle)

    private data class GateHalf(val name: String, val coords: CoordGrid)

    private data class GateSpec(val halves: List<GateHalf>, val leaves: List<OpenLeaf>)

    private companion object {
        private const val FIRE_DURATION = 200
        private const val DOOR_OPEN_TICKS = 5
        private const val TREE_STUMP_TICKS = 5
        private const val ROCK_DEPLETED_TICKS = 5
        private val MESOVERLAY =
            "clientscript.[clientscript,mesoverlay]".asRSCM(RSCMType.CLIENTSCRIPT)

        private val WOODEN_GATE =
            GateSpec(
                halves =
                    listOf(
                        GateHalf(TutorialLoc.GATE_LEFT, CoordGrid(3089, 3092, 0)),
                        GateHalf(TutorialLoc.GATE_RIGHT, CoordGrid(3089, 3091, 0)),
                    ),
                leaves =
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_FENCE_GATE_L,
                            CoordGrid(3090, 3092, 0),
                            LocAngle.North,
                        ),
                        OpenLeaf(
                            TutorialLoc.INACTIVE_FENCE_GATE_R,
                            CoordGrid(3091, 3092, 0),
                            LocAngle.North,
                        ),
                    ),
            )

        private val COMBAT_GATE =
            GateSpec(
                halves =
                    listOf(
                        GateHalf(TutorialLoc.COMBAT_GATE_LEFT, CoordGrid(3094, 9503, 0)),
                        GateHalf(TutorialLoc.COMBAT_GATE_RIGHT, CoordGrid(3094, 9502, 0)),
                    ),
                leaves =
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_METAL_GATE_L,
                            CoordGrid(3095, 9502, 0),
                            LocAngle.South,
                        ),
                        OpenLeaf(
                            TutorialLoc.INACTIVE_METAL_GATE_L,
                            CoordGrid(3095, 9503, 0),
                            LocAngle.North,
                        ),
                    ),
            )

        private val RAT_GATE =
            GateSpec(
                halves =
                    listOf(
                        GateHalf(TutorialLoc.RAT_GATE_LEFT, CoordGrid(3111, 9518, 0)),
                        GateHalf(TutorialLoc.RAT_GATE_RIGHT, CoordGrid(3111, 9519, 0)),
                    ),
                leaves =
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_METAL_GATE_L,
                            CoordGrid(3110, 9518, 0),
                            LocAngle.South,
                        ),
                        OpenLeaf(
                            TutorialLoc.INACTIVE_METAL_GATE_R,
                            CoordGrid(3110, 9519, 0),
                            LocAngle.North,
                        ),
                    ),
            )

        private val DOOR_OPEN: Map<String, List<OpenLeaf>> =
            mapOf(
                TutorialLoc.DOOR_1 to
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_POOR_DOOR,
                            CoordGrid(3097, 3107, 0),
                            LocAngle.North,
                        ),
                    ),
                TutorialLoc.DOOR_2 to
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_POOR_DOOR,
                            CoordGrid(3078, 3084, 0),
                            LocAngle.North,
                        ),
                    ),
                TutorialLoc.DOOR_3 to
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_POOR_DOOR,
                            CoordGrid(3073, 3090, 0),
                            LocAngle.South,
                        ),
                    ),
                TutorialLoc.DOOR_4 to
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_POOR_DOOR,
                            CoordGrid(3086, 3125, 0),
                            LocAngle.West,
                        ),
                    ),
                TutorialLoc.DOOR_6 to
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_POOR_DOOR,
                            CoordGrid(3124, 3124, 0),
                            LocAngle.North,
                        ),
                    ),
                TutorialLoc.DOOR_7 to
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_POOR_DOOR,
                            CoordGrid(3129, 3124, 0),
                            LocAngle.North,
                        ),
                    ),
                TutorialLoc.DOOR_8 to
                    listOf(
                        OpenLeaf(
                            TutorialLoc.INACTIVE_POOR_DOOR,
                            CoordGrid(3122, 3103, 0),
                            LocAngle.East,
                        ),
                    ),
            )

        private val GATE_OPEN: Map<String, GateSpec> =
            mapOf(
                TutorialLoc.GATE_LEFT to WOODEN_GATE,
                TutorialLoc.GATE_RIGHT to WOODEN_GATE,
                TutorialLoc.COMBAT_GATE_LEFT to COMBAT_GATE,
                TutorialLoc.COMBAT_GATE_RIGHT to COMBAT_GATE,
                TutorialLoc.RAT_GATE_LEFT to RAT_GATE,
                TutorialLoc.RAT_GATE_RIGHT to RAT_GATE,
            )
    }
}
