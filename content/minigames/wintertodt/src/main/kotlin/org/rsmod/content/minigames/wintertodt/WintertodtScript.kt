package org.rsmod.content.minigames.wintertodt

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.baseFiremakingLvl
import org.rsmod.api.player.worn.WornUnequipResult
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.other.consumables.food.FoodConsumedEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WintertodtScript
@Inject
constructor(private val game: WintertodtGame) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { game.tick() }
        onEvent<FoodConsumedEvent> { game.onFoodConsumed(player, item, healAmount) }
        onPlayerLogin { game.onLogin(player) }
        onPlayerLogout { game.onLogout(player) }
        onPlayerCoordsChanged { game.onCoordsChanged(player) }

        onOpLoc1("loc.wint_door") {
            if (Wintertodt.isInPrison(player.coords)) leavePrison() else enterPrison()
        }
        onOpLoc2("loc.wint_door") {
            game.stopAction(player)
            mesbox(game.peekMessage())
        }

        onOpLoc1(Wintertodt.BRAZIER_UNLIT) { light(it.loc) }
        onOpLoc1(Wintertodt.BRAZIER_BROKEN) { fix(it.loc) }
        onOpLoc1(Wintertodt.BRAZIER_LIT) { feed(it.loc) }
        onApLoc1(Wintertodt.BRAZIER_UNLIT) { apLight(it.loc) }
        onApLoc1(Wintertodt.BRAZIER_BROKEN) { apFix(it.loc) }
        onApLoc1(Wintertodt.BRAZIER_LIT) { apFeed(it.loc) }

        onOpLoc1("loc.wint_roots") {
            faceLoc(it.loc)
            game.beginChop(player, it.loc)
        }
        onOpLoc1("loc.wint_herb_roots") {
            faceLoc(it.loc)
            game.beginPick(player, it.loc)
        }

        onOpLoc1("loc.wint_chest_vial") { game.takeVials(player, 1) }
        onOpLoc2("loc.wint_chest_vial") { game.takeVials(player, 5) }
        onOpLoc3("loc.wint_chest_vial") { game.takeVials(player, 10) }
        onOpLoc1("loc.wint_chest_hammer") { game.takeTool(player, "obj.hammer") }
        onOpLoc1("loc.wint_chest_knife") { game.takeTool(player, "obj.knife") }
        onOpLoc1("loc.wint_chest_axe") { game.takeTool(player, "obj.bronze_axe") }
        onOpLoc1("loc.wint_chest_tinderbox") { game.takeTool(player, "obj.tinderbox") }
        onOpLoc1("loc.wint_bankchest") {
            game.stopAction(player)
            tryOpenBank()
        }

        onOpLoc1("loc.wint_reward_pile") { startCartSearch(it.loc, 1) }
        onOpLoc2("loc.wint_reward_pile") {
            game.stopAction(player)
            game.checkCart(player)
        }
        onOpLoc3("loc.wint_reward_pile") { startCartSearch(it.loc, 5) }
        onOpLoc1(Wintertodt.PILLAR_JUMP) { jumpPillar(it.loc) }

        onOpHeldU("obj.knife", Wintertodt.ROOT) { game.beginFletch(player) }
        onOpHeldU("obj.fletching_knife", Wintertodt.ROOT) { game.beginFletch(player) }
        onOpHeldU(Wintertodt.HERB, Wintertodt.VIAL) { game.combinePotion(player) }

        for (potion in POTIONS) {
            onOpHeld1(potion) { game.drinkPotion(player, it.slot) }
            onOpHeld2(potion) { game.drinkPotion(player, it.slot) }
        }

        onOpHeld3(Wintertodt.TORCH) {
            invReplace(inv, Wintertodt.TORCH, 1, Wintertodt.TORCH_OFFHAND)
        }
        onOpHeld3(Wintertodt.TORCH_OFFHAND) {
            invReplace(inv, Wintertodt.TORCH_OFFHAND, 1, Wintertodt.TORCH)
        }
        onOpWorn4(Wintertodt.TORCH) {
            swapTorchWorn(it.slot, Wintertodt.TORCH, Wintertodt.TORCH_OFFHAND, Wearpos.LeftHand)
        }
        onOpWorn4(Wintertodt.TORCH_OFFHAND) {
            swapTorchWorn(it.slot, Wintertodt.TORCH_OFFHAND, Wintertodt.TORCH, Wearpos.RightHand)
        }

        onOpNpc1(Wintertodt.NPC_PYROMANCER) { helpPyromancer(it.npc) }
        onOpNpc1(Wintertodt.NPC_PYROMANCER_DOWN) { helpPyromancer(it.npc) }
        onOpNpcU(Wintertodt.NPC_PYROMANCER) { useItemOnPyromancer(it) }
        onOpNpcU(Wintertodt.NPC_PYROMANCER_DOWN) { useItemOnPyromancer(it) }

        onOpNpc1(IGNISIA) {
            game.stopAction(player)
            talkToIgnisia(it.npc)
        }
        onOpNpc3(IGNISIA) {
            game.stopAction(player)
            exchangeWithIgnisia(it.npc)
        }
        onOpNpc1(Wintertodt.NPC_ISH) {
            game.stopAction(player)
            talkToIsh(it.npc)
        }
        onOpNpc1(Wintertodt.NPC_KALT) {
            game.stopAction(player)
            talkToKalt(it.npc)
        }
        onOpNpc1(Wintertodt.NPC_BREWMA) {
            game.stopAction(player)
            talkToBrewma(it.npc)
        }
        onOpNpcU(Wintertodt.NPC_BREWMA) {
            game.stopAction(player)
            useItemOnBrewma(it)
        }
        onOpHeld1(EXTRA_CRATE) {
            game.stopAction(player)
            game.openExtraCrate(player, it.slot)
        }

        onCommand("wt") {
            desc = "Wintertodt debug"
            invalidArgs =
                "Usage: ::wt start|energy <n>|points <n>|warmth <n>|pool <n>|search|fletch|" +
                    "check|attack"
            cheat {
                when (args.getOrNull(0)?.lowercase()) {
                    "start" -> game.startRound()
                    "energy" -> game.setEnergy(args[1].toInt())
                    "points" -> game.setPoints(player, args[1].toInt())
                    "warmth" -> game.setWarmth(player, args[1].toInt())
                    "pool" -> game.setRewardPool(player, args[1].toInt())
                    "search" -> {
                        val message =
                            game.beginSearch(player, args.getOrNull(1)?.toIntOrNull() ?: 1)
                        if (message != null) player.mes(message)
                    }
                    "fletch" -> game.beginFletch(player)
                    "check" -> game.checkCart(player)
                    "attack" ->
                        when (args.getOrNull(1)?.lowercase()) {
                            "area" -> game.forceAreaAttack()
                            "brazier" ->
                                game.forceBrazierAttack(args.getOrNull(2)?.toIntOrNull() ?: 0)
                            "pyro" ->
                                game.forcePyromancerAttack(args.getOrNull(2)?.toIntOrNull() ?: 0)
                            else -> game.forceAttack()
                        }
                    "status" -> game.debugStatus(player)
                    "axe" -> game.debugAxe(player)
                    "say" -> game.debugPyroSay()
                    "locs" -> game.debugLocations(player)
                    "lockwarmth" -> game.toggleWarmthLock(player)
                    "break" -> game.forceBrazierBreak()
                    "logout" -> player.manualLogout = true
                    else -> player.mes(invalidArgs ?: "Usage: ::wt start|energy|points|warmth|attack")
                }
            }
        }
    }

    private suspend fun ProtectedAccess.helpPyromancer(npc: Npc) {
        game.stopAction(player)
        arriveDelay()
        faceEntitySquare(npc)
        if (!game.isPyromancerDown(npc)) {
            player.mes("You don't need to do that.")
            startDialogue(npc) { chatNpc(neutral, "No need! I'm fine, thanks.") }
            return
        }
        game.healPyromancer(player, npc)
    }

    private suspend fun ProtectedAccess.useItemOnPyromancer(event: NpcUDefaultEvents.OpType) {
        game.stopAction(player)
        if (event.objType.internalName in POTIONS) {
            arriveDelay()
            faceEntitySquare(event.npc)
            if (!game.isPyromancerDown(event.npc)) {
                player.mes("You don't need to do that.")
                startDialogue(event.npc) {
                    chatNpc(neutral, "No need! I'm fine, thanks.")
                }
                return
            }
            game.healPyromancer(player, event.npc, event.invSlot)
        } else {
            event.npc.say("That's no use!")
        }
    }

    private suspend fun ProtectedAccess.enterPrison() {
        game.stopAction(player)
        if (player.baseFiremakingLvl < Wintertodt.FM_LEVEL) {
            mes("You need a Firemaking level of ${Wintertodt.FM_LEVEL} to enter.")
            return
        }
        if (player.wintNeedsWarning == 1) {
            player.wintNeedsWarning = 0
            startDialogue {
                mesbox(
                    "Please be aware that the Wintertodt has been modified since your last " +
                        "encounter; for full details, please visit our website for the news " +
                        "article dated October 2024, or consult the Old School Runescape Wiki.",
                )
            }
        }
        telejump(Wintertodt.ENTRY)
        game.enter(player)
    }

    private suspend fun ProtectedAccess.leavePrison() {
        game.stopAction(player)
        telejump(Wintertodt.EXIT)
        game.leave(player)
    }

    private suspend fun ProtectedAccess.light(loc: BoundLocInfo) {
        game.stopAction(player)
        val index = game.brazierIndex(loc.coords)
        if (index < 0) return
        if (!game.canLight(player, index)) return
        arriveDelay()
        faceLoc(loc)
        val lightAnim =
            if (game.hasBrumaTorch(player)) Wintertodt.ANIM_LIGHT_TORCH
            else Wintertodt.ANIM_LIGHT
        anim(lightAnim)
        delay(3)
        game.lightBrazier(player, index)
        resetAnim()
    }

    private suspend fun ProtectedAccess.apLight(loc: BoundLocInfo) {
        if (!isWithinApRange(loc, BRAZIER_AP_RANGE)) return
        light(loc)
    }

    private suspend fun ProtectedAccess.fix(loc: BoundLocInfo) {
        game.stopAction(player)
        val index = game.brazierIndex(loc.coords)
        if (index < 0) return
        if (!game.canFix(player, index)) return
        arriveDelay()
        faceLoc(loc)
        anim(Wintertodt.ANIM_FIX)
        soundSynth(930)
        delay(2)
        game.fixBrazier(player, index)
        delay(1)
        mes("You fix the brazier.")
        resetAnim()
    }

    private suspend fun ProtectedAccess.apFix(loc: BoundLocInfo) {
        if (!isWithinApRange(loc, BRAZIER_AP_RANGE)) return
        fix(loc)
    }

    private suspend fun ProtectedAccess.feed(loc: BoundLocInfo) {
        game.stopAction(player)
        val index = game.brazierIndex(loc.coords)
        if (index < 0) return
        arriveDelay()
        faceLoc(loc)
        game.beginFeed(player, index)
    }

    private suspend fun ProtectedAccess.apFeed(loc: BoundLocInfo) {
        if (!isWithinApRange(loc, BRAZIER_AP_RANGE)) return
        feed(loc)
    }

    private suspend fun ProtectedAccess.startCartSearch(loc: BoundLocInfo, batch: Int) {
        game.stopAction(player)
        arriveDelay()
        faceLoc(loc)
        delay(1)
        val message = game.beginSearch(player, batch)
        if (message != null) mesbox(message)
    }

    private suspend fun ProtectedAccess.jumpPillar(loc: BoundLocInfo) {
        game.stopAction(player)
        arriveDelay()
        if (player.agilityLvl < Wintertodt.PILLAR_JUMP_LEVEL) {
            mes(
                "You need an Agility level of ${Wintertodt.PILLAR_JUMP_LEVEL} to negotiate " +
                    "this obstacle.",
            )
            return
        }
        if (game.pillarJumpLocked(player, loc.coords)) {
            mes(Wintertodt.PILLAR_JUMP_LOCKED)
            return
        }
        val west = player.coords.x > loc.coords.x
        val dest = loc.coords.translate(if (west) -1 else 1, 0)
        val facing = if (west) FACE_WEST else FACE_EAST
        game.lockPillarJump(player, loc.coords)
        faceLoc(loc)
        anim(Wintertodt.PILLAR_JUMP_ANIM)
        soundSynth(Wintertodt.PILLAR_JUMP_SYNTH)
        exactMove(player.coords, dest, delay1 = 15, delay2 = 30, dir = facing)
        statAdvance("stat.agility", Wintertodt.PILLAR_JUMP_XP)
    }

    private fun ProtectedAccess.swapTorchWorn(
        fromSlot: Int,
        fromType: String,
        toType: String,
        targetWearpos: Wearpos,
    ) {
        if (worn[fromSlot]?.isType(fromType) != true) return
        val targetFree = worn[targetWearpos.slot] == null
        val unequip = wornUnequip(fromSlot)
        if (unequip is WornUnequipResult.Fail) {
            unequip.message?.let(::mes)
            return
        }
        invReplace(inv, fromType, 1, toType)
        if (targetFree) {
            val swapped = inv.indexOfFirst { it?.isType(toType) == true }
            if (swapped >= 0) invEquip(swapped)
        }
    }

    private companion object {
        const val FACE_WEST = 512
        const val FACE_EAST = 1536
        const val BRAZIER_AP_RANGE = 1

        val POTIONS =
            listOf(
                Wintertodt.POTION4,
                Wintertodt.POTION3,
                Wintertodt.POTION2,
                Wintertodt.POTION1,
            )
    }
}
