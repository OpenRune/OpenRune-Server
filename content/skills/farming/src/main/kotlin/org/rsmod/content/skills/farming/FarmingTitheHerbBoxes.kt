package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.ironman.IronmanRestrictions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingTitheHerbBoxes @Inject constructor(
    private val random: GameRandom,
    private val banking: BankInvScript,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (box in listOf(CLOSED, OPEN)) {
            onOpHeld1(box) { if (!take(player, it.slot, it.obj, random)) mes("You don't have enough inventory space.") }
            onOpHeld3(box) { bankAll(this, banking, it.slot, it.obj, random) }
            onOpHeld4(box) { mes("The herb box contains ${remaining(it.obj)} herbs.") }
        }
    }

    companion object {
        const val CLOSED = "obj.nzone_herbbox"
        const val OPEN = "obj.nzone_herbbox_opened"

        private const val ROLLED = 1 shl 28
        private const val HERBS = ROLLED - 1

        internal fun remaining(box: InvObj): Int {
            if (box.vars and ROLLED != 0) {
                var herbs = box.vars and HERBS
                var count = 0
                while (herbs != 0) {
                    count++
                    herbs = herbs ushr 4
                }
                return count
            }
            return if (box.id == CLOSED.asRSCM()) FarmingTitheData.row.herbBoxRolls else box.vars
        }

        private fun prepare(player: Player, slot: Int, expected: InvObj, random: GameRandom): InvObj? {
            val box = player.inv[slot] ?: return null
            if (box != expected || box.count != 1 ||
                box.id != CLOSED.asRSCM() && box.id != OPEN.asRSCM()) return null
            if (player.inv.type.protect && player.isAccessProtected) return null
            val left = remaining(box)
            if (left !in 1..FarmingTitheData.row.herbBoxRolls) return null
            val herbs = FarmingTitheData.row.herbBoxHerbs
            if (box.vars and ROLLED != 0) {
                var packed = box.vars and HERBS
                repeat(left) {
                    if (packed and 15 !in 1..herbs.size) return null
                    packed = packed ushr 4
                }
                return box
            }
            val weight = herbs.sumOf { it.t1 }
            var packed = ROLLED
            repeat(left) { index ->
                var roll = random.of(weight)
                val herb = herbs.indexOfFirst { roll -= it.t1; roll < 0 }
                packed = packed or ((herb + 1) shl (index * 4))
            }
            // Persist every roll before attempting delivery so failed transfers cannot reroll it.
            val rolled = InvObj(OPEN, vars = packed)
            player.inv[slot] = rolled
            return rolled
        }

        private fun reward(box: InvObj): InvObj =
            InvObj(FarmingTitheData.row.herbBoxHerbs[(box.vars and 15) - 1].t0)

        private fun remainder(box: InvObj): InvObj? {
            val herbs = (box.vars and HERBS) ushr 4
            return if (herbs == 0) null else box.copy(vars = ROLLED or herbs)
        }

        internal fun take(player: Player, slot: Int, expected: InvObj, random: GameRandom): Boolean {
            if (remaining(expected) > 1 && player.inv.none { it == null }) return false
            val box = prepare(player, slot, expected, random) ?: return false
            val next = remainder(box)
            return player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete(inv, box.id, 1, slot)
                if (next != null) add(inv, next.id, 1, vars = next.vars, slot = slot)
                add(inv, reward(box).id, 1)
            }.success
        }

        internal fun bankAll(
            access: ProtectedAccess,
            banking: BankInvScript,
            slot: Int,
            expected: InvObj,
            random: GameRandom,
        ): Boolean {
            if (IronmanRestrictions.blockUimBank(access.player)) return false
            var box = prepare(access.player, slot, expected, random) ?: return false
            while (true) {
                val next = remainder(box)
                val deposited = with(banking) {
                    access.invDepositReward(slot, box, reward(box), next)
                }
                if (!deposited) return false
                if (next == null) return true
                box = next
            }
        }
    }
}
