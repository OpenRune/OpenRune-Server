package org.rsmod.content.minigames.wintertodt

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseFiremakingLvl
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.other.consumables.food.FoodConsumedEvent
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
        onOpLoc2("loc.wint_door") { game.peek(player) }

        onOpLoc1(Wintertodt.BRAZIER_UNLIT) { light(it.loc) }
        onOpLoc1(Wintertodt.BRAZIER_BROKEN) { fix(it.loc) }
        onOpLoc1(Wintertodt.BRAZIER_LIT) { feed(it.loc) }

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
        onOpLoc1("loc.wint_bankchest") { tryOpenBank() }

        onOpLoc1("loc.wint_reward_pile") { searchCart(it.loc, 1) }
        onOpLoc2("loc.wint_reward_pile") { game.checkCart(player) }
        onOpLoc3("loc.wint_reward_pile") { searchCart(it.loc, 5) }

        onOpHeldU("obj.knife", Wintertodt.ROOT) { game.beginFletch(player) }
        onOpHeldU("obj.fletching_knife", Wintertodt.ROOT) { game.beginFletch(player) }
        onOpHeldU(Wintertodt.HERB, Wintertodt.VIAL) { game.combinePotion(player) }

        for (potion in POTIONS) {
            onOpHeld1(potion) { game.drinkPotion(player, it.slot) }
            onOpHeld2(potion) { game.drinkPotion(player, it.slot) }
        }

        onOpNpc1(Wintertodt.NPC_PYROMANCER) { game.healPyromancer(player, it.npc) }
        onOpNpc1(Wintertodt.NPC_PYROMANCER_DOWN) { game.healPyromancer(player, it.npc) }

        onCommand("wt") {
            desc = "Wintertodt debug"
            invalidArgs = "Usage: ::wt start|energy <n>|points <n>|warmth <n>|attack"
            cheat {
                when (args.getOrNull(0)?.lowercase()) {
                    "start" -> game.startRound()
                    "energy" -> game.setEnergy(args[1].toInt())
                    "points" -> game.setPoints(player, args[1].toInt())
                    "warmth" -> game.setWarmth(player, args[1].toInt())
                    "pool" -> game.setRewardPool(player, args[1].toInt())
                    "search" -> game.searchCart(player, args.getOrNull(1)?.toIntOrNull() ?: 1)
                    "check" -> game.checkCart(player)
                    "attack" ->
                        when (args.getOrNull(1)?.lowercase()) {
                            "area" -> game.forceAreaAttack()
                            "brazier" -> game.forceBrazierAttack()
                            "pyro" -> game.forcePyromancerAttack()
                            else -> game.forceAttack()
                        }
                    "status" -> game.debugStatus(player)
                    "locs" -> game.debugLocations(player)
                    "lockwarmth" -> game.toggleWarmthLock(player)
                    "break" -> game.forceBrazierBreak()
                    else -> player.mes(invalidArgs ?: "Usage: ::wt start|energy|points|warmth|attack")
                }
            }
        }
    }

    private suspend fun ProtectedAccess.enterPrison() {
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
        telejump(Wintertodt.EXIT)
        game.leave(player)
    }

    private suspend fun ProtectedAccess.light(loc: BoundLocInfo) {
        val index = game.brazierIndex(loc.coords)
        if (index < 0) return
        if (!game.canLight(player, index)) return
        arriveDelay()
        val lightAnim =
            if (game.hasBrumaTorch(player)) Wintertodt.ANIM_LIGHT_TORCH
            else Wintertodt.ANIM_LIGHT
        anim(lightAnim)
        delay(2)
        game.lightBrazier(player, index)
        resetAnim()
    }

    private suspend fun ProtectedAccess.fix(loc: BoundLocInfo) {
        val index = game.brazierIndex(loc.coords)
        if (index < 0) return
        if (!game.canFix(player, index)) return
        arriveDelay()
        anim(Wintertodt.ANIM_FIX)
        delay(2)
        game.fixBrazier(player, index)
        resetAnim()
    }

    private suspend fun ProtectedAccess.feed(loc: BoundLocInfo) {
        val index = game.brazierIndex(loc.coords)
        if (index < 0) return
        arriveDelay()
        game.beginFeed(player, index)
    }

    private suspend fun ProtectedAccess.searchCart(loc: BoundLocInfo, count: Int) {
        arriveDelay()
        delay(3)
        game.searchCart(player, count)
    }

    private companion object {
        val POTIONS =
            listOf(
                Wintertodt.POTION4,
                Wintertodt.POTION3,
                Wintertodt.POTION2,
                Wintertodt.POTION1,
            )
    }
}
