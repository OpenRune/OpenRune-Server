package org.rsmod.content.bosses.moons

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.bosses.moons.MoonsFights.Companion.set
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.isOk
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LunarChest
@Inject
constructor(private val random: GameRandom, private val dropRegistry: DropTableRegistry) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CLOSED) { claim() }
        onOpLoc1(OPEN) { claim() }
        onIfClose(INTERFACE) { player.stopInvTransmit(rewards(player)) }
        onIfModalButton(TAKE_ALL) { takeAll(toBank = false) }
        onIfModalButton(BANK_ALL) { takeAll(toBank = true) }
        onIfModalButton(DISCARD_YES) { discard() }
        onIfModalButton(ITEMS) { takeItem(it.comsub, it.op) }
    }

    private fun rewards(player: Player): Inventory = player.invMap.getOrPut(REWARD_INV)

    private fun ProtectedAccess.claim() {
        val chest = rewards(player)
        if (chest.isNotEmpty()) {
            open(chest)
            return
        }
        val defeated = Moon.entries.filter { player.vars[it.deadVarbit] != 0 }
        val claimed = player.vars[CHEST_COUNT_VARP]
        val unlocked = claimed > 0 || Moon.entries.all { player.vars[it.killsVarp] > 0 }
        if (!unlocked) {
            mes("You need to subdue all three of the Moons before you can claim a reward.")
            return
        }
        if (defeated.isEmpty()) {
            mes("You need to subdue at least one of the Moons before you can claim a reward.")
            return
        }
        val loot = rollLoot()
        for (moon in Moon.entries) player.set(moon.deadVarbit, 0)
        player.set("varbit.pmoon_boss_hide_npc", 0)
        player.set(CHEST_COUNT_VARP, claimed + 1)
        mes("Your Lunar Chest count is: <col=ff0000>${claimed + 1}</col>.")
        for (item in loot) {
            invAdd(chest, item.obj, item.count)
            CollectionLog.grant(player, item.obj, item.count)
        }
        open(chest)
    }

    private fun ProtectedAccess.rollLoot(): List<Reward> {
        val drops =
            when (val result = dropRegistry.forLoc(CLOSED)?.roll(player, ArgMap())?.flatten()) {
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
                else -> emptyList()
            }
        val loot = LinkedHashMap<String, Int>()
        for (drop in drops) {
            if (drop.isNothing || !drop.condition(player)) continue
            loot.merge(drop.obj, drop.rollCount(random), Int::plus)
        }
        return loot.map { Reward(it.key, it.value) }
    }

    private fun ProtectedAccess.open(chest: Inventory) {
        soundSynth(OPEN_SOUND)
        invTransmit(chest)
        ifOpenMainModal(INTERFACE)
        player.runClientScript(
            INV_INIT.asRSCM(RSCMType.CLIENTSCRIPT),
            ITEMS.asRSCM(RSCMType.COMPONENT),
            REWARD_INV.asRSCM(RSCMType.INV),
            COLUMNS,
            ROWS,
            0,
            NO_COMPONENT,
            "Withdraw-1",
            "Withdraw-all",
            "",
            "",
            "",
        )
        ifSetEvents(ITEMS, 0 until chest.size, IfEvent.Op1, IfEvent.Op2, IfEvent.Op10)
    }

    private fun ProtectedAccess.takeAll(toBank: Boolean) {
        val chest = rewards(player)
        val target = if (toBank) bank else inv
        for (slot in chest.indices) {
            val obj = chest[slot] ?: continue
            invMoveFromSlot(
                from = chest,
                into = target,
                fromSlot = slot,
                count = obj.count,
                strict = false,
                uncert = toBank,
            )
        }
        invCompress(chest)
        if (toBank) mes("You deposit your items into the bank.")
        finishIfEmpty(chest)
    }

    private fun ProtectedAccess.takeItem(slot: Int, op: IfButtonOp) {
        val chest = rewards(player)
        val obj = chest[slot] ?: return
        val requested =
            when (op) {
                IfButtonOp.Op1 -> 1
                IfButtonOp.Op2 -> obj.count
                IfButtonOp.Op10 -> return objExamine(chest, slot)
                else -> return
            }
        val moved =
            invMoveFromSlot(
                from = chest,
                into = inv,
                fromSlot = slot,
                count = requested.coerceIn(1, obj.count),
                strict = false,
            )
        if (!moved[0].isOk()) {
            mes("You don't have enough inventory space.")
            return
        }
        invCompress(chest)
        finishIfEmpty(chest)
    }

    private fun ProtectedAccess.discard() {
        invClear(rewards(player))
        ifClose()
    }

    private fun ProtectedAccess.finishIfEmpty(chest: Inventory) {
        if (chest.isNotEmpty()) return
        ifClose()
    }

    private class Reward(val obj: String, val count: Int)

    private companion object {
        private const val CLOSED = "loc.chest_sun01_default01"
        private const val OPEN = "loc.chest_sun01_open01"
        private const val INTERFACE = "interface.pmoon_reward"
        private const val REWARD_INV = "inv.pmoon_rewardinv"
        private const val ITEMS = "component.pmoon_reward:reward_inv"
        private const val TAKE_ALL = "component.pmoon_reward:inv_all"
        private const val BANK_ALL = "component.pmoon_reward:bank_all"
        private const val DISCARD_YES = "component.pmoon_reward:yes"
        private const val INV_INIT = "clientscript.[clientscript,interface_inv_init]"
        private const val CHEST_COUNT_VARP = "varp.total_pmoon_chests"
        private const val OPEN_SOUND = "synth.found_gem"
        private const val COLUMNS = 3
        private const val ROWS = 2
        private const val NO_COMPONENT = -1
    }
}
