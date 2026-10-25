package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType

@Singleton
class FarmingCompostService @Inject constructor(
    private val farming: FarmingService,
    private val xpMods: XpModifiers,
) {
    fun handles(obj: String): Boolean =
        FarmingBottomlessCompost.isBucket(obj) || FarmingCompostSupplies.bucket(obj)?.noted == false

    suspend fun ProtectedAccess.apply(patch: PatchDef, obj: String, slot: Int) {
        val selected = inv[slot]?.takeIf { it.isType(obj) } ?: return
        val ordinary = FarmingCompostSupplies.bucket(obj)?.takeUnless { it.noted }
        val stored = FarmingBottomlessCompost.contents(selected)
        val kind = ordinary?.kind ?: stored?.kind ?: return
        if (kind == Compost.NONE) {
            mes("Your compost bucket is currently empty.")
            return
        }
        anim(ANIM_COMPOST)
        delay(2)
        resetAnim()
        val result = farming.treat(player, patch, kind) {
            if (inv[slot] != selected) false
            else if (ordinary != null) replace(player, slot, selected, InvObj(ordinary.empty))
            else replace(player, slot, selected, FarmingBottomlessCompost.bucket(kind, checkNotNull(stored).charges - 1))
        }
        when (result) {
            TreatResult.WEEDS -> mes("The patch needs to be raked first.")
            TreatResult.ALREADY -> mes("This patch has already been treated.")
            TreatResult.DEAD, TreatResult.GROWN -> mes("You can't treat this crop with compost now.")
            TreatResult.UNPAID, TreatResult.STALE -> Unit
            TreatResult.TREATED -> {
                farming.awardPatchXp(player, patch, FarmingComposts.get(kind).xp)
                mes("You treat the ${patch.kind.label} with ${kind.label}.")
            }
        }
    }

    internal fun fill(
        player: Player,
        slot: Int,
        expected: InvObj,
        sourceSlot: Int,
        source: InvObj,
        form: CompostBucket,
        requested: Int,
    ): Int {
        val inv = player.inv
        if (slot == sourceSlot || inv[slot] != expected || inv[sourceSlot] != source || !source.isType(form.obj)) return 0
        val stored = FarmingBottomlessCompost.contents(expected) ?: return 0
        if (stored.kind != Compost.NONE && stored.kind != form.kind) return 0
        val room = (FarmingBottomlessCompost.capacity - stored.charges) / FarmingBottomlessCompost.chargesPerBucket
        val amount = minOf(requested, room, inv.physicalCount(form.obj.internalName))
        if (amount <= 0) return 0
        val next = FarmingBottomlessCompost.bucket(form.kind, stored.charges + amount * FarmingBottomlessCompost.chargesPerBucket)
        val result = player.invTransaction(inv) {
            val target = select(inv)
            delete(target, expected.id, 1, slot)
            delete(target, form.obj.id, amount, sourceSlot)
            add(target, next.id, 1, next.vars, slot)
            add(target, form.empty.id, amount)
        }
        return if (result.success) amount else 0
    }

    internal fun upgrade(
        player: Player,
        bucketSlot: Int,
        expectedBucket: InvObj,
        reagentSlot: Int,
        expectedReagent: InvObj,
        recipe: CompostUpgrade,
    ): Boolean {
        val inv = player.inv
        if (bucketSlot == reagentSlot || inv[bucketSlot] != expectedBucket || inv[reagentSlot] != expectedReagent) return false
        if (!expectedBucket.isType(recipe.bucket) || !expectedReagent.isType(recipe.reagent)) return false
        return player.invTransaction(inv) {
            val target = select(inv)
            delete(target, recipe.bucket.id, 1, bucketSlot)
            delete(target, recipe.reagent.id, recipe.cost, reagentSlot)
            add(target, recipe.output.id, 1, slot = bucketSlot)
            if (recipe.kind == CompostUpgradeKind.POTION) add(target, recipe.remainder.id, 1, slot = reagentSlot)
        }.success
    }

    internal suspend fun ProtectedAccess.upgrade(bucketSlot: Int, reagentSlot: Int, recipe: CompostUpgrade) {
        var slot = bucketSlot
        var reagentIndex = reagentSlot
        var first = true
        while (true) {
            val bucket = inv[slot]?.takeIf { it.isType(recipe.bucket) } ?: return
            val reagent = inv[reagentIndex]?.takeIf { it.isType(recipe.reagent) } ?: return
            if (inv.physicalCount(recipe.reagent.internalName) < recipe.cost) return
            val ticks = if (first) recipe.ticks else recipe.repeatTicks
            if (ticks > 0) delay(ticks)
            if (!upgrade(player, slot, bucket, reagentIndex, reagent, recipe)) return
            if (recipe.xp > 0) statAdvance(STAT_FARMING, recipe.xp * xpMods.get(player, STAT_FARMING))
            when (recipe.kind) {
                CompostUpgradeKind.POTION -> {
                    mes("You apply the supercompost potion to the bucket of compost.")
                    mes("The compost transforms into supercompost.")
                    return
                }
                CompostUpgradeKind.ASH -> spam("The supercompost transforms into ultracompost.")
                CompostUpgradeKind.SALTPETRE -> spam("The compost transforms into supercompost.")
            }
            slot = inv.objs.indexOfFirst { it.isType(recipe.bucket) }
            reagentIndex = inv.objs.indexOfFirst { it.isType(recipe.reagent) }
            if (slot < 0 || reagentIndex < 0) return
            first = false
        }
    }

    internal fun empty(player: Player, slot: Int, expected: InvObj): Boolean {
        val contents = FarmingBottomlessCompost.contents(expected) ?: return false
        if (contents.charges == 0) return false
        return replace(player, slot, expected, InvObj(FarmingBottomlessCompost.EMPTY))
    }

    private fun replace(player: Player, slot: Int, expected: InvObj, next: InvObj): Boolean {
        if (player.inv[slot] != expected) return false
        return player.invTransaction(player.inv) {
            val target = select(player.inv)
            delete(target, expected.id, 1, slot)
            add(target, next.id, 1, next.vars, slot)
        }.success
    }
}
