package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingCompostScript @Inject constructor(private val compost: FarmingCompostService) : PluginScript() {
    override fun ScriptContext.startup() {
        for (obj in listOf(FarmingBottomlessCompost.EMPTY, FarmingBottomlessCompost.FILLED)) {
            onOpHeld1(obj) { if (inv[it.slot] == it.obj) fill(it.slot, it.obj) }
            onOpHeld2(obj) { if (inv[it.slot] == it.obj) check(it.obj) }
            for (form in FarmingCompostSupplies.BUCKETS) {
                onOpHeldU(obj, form.obj) {
                    val bucket = inv[it.firstSlot]?.takeIf { it.isType(obj) } ?: return@onOpHeldU
                    fill(it.firstSlot, bucket, it.secondSlot, form)
                }
            }
        }
        onOpHeld5(FarmingBottomlessCompost.FILLED) { compost.empty(player, it.slot, it.obj) }
        for (recipe in FarmingCompostSupplies.UPGRADES) {
            onOpHeldU(recipe.bucket, recipe.reagent) {
                if (inv.physicalCount(recipe.reagent.internalName) < recipe.cost) {
                    if (recipe.kind == CompostUpgradeKind.ASH) {
                        mes("You need at least one bucket of supercompost and two piles of volcanic ash to create ultracompost.")
                    }
                } else with(compost) { upgrade(it.firstSlot, it.secondSlot, recipe) }
            }
        }
        onOpHeldU("obj.bucket_supercompost", "obj.hosidius_saltpetre") {
            mes("You can't make supercompost from supercompost. That doesn't make sense.")
        }
        onOpHeldU("obj.bucket_ultracompost", "obj.hosidius_saltpetre") {
            mes("That compost is already as good as it gets.")
        }
    }

    private suspend fun ProtectedAccess.check(obj: InvObj) {
        val stored = FarmingBottomlessCompost.contents(obj) ?: return
        val message = if (stored.charges == 0) "Your compost bucket is currently empty."
        else "Your bottomless compost bucket is currently holding ${stored.charges} uses of ${stored.kind.label}."
        objbox(obj, message)
    }

    private suspend fun ProtectedAccess.fill(
        slot: Int,
        expected: InvObj,
        sourceSlot: Int? = null,
        selectedForm: CompostBucket? = null,
    ) {
        val stored = FarmingBottomlessCompost.contents(expected) ?: return
        if (stored.charges >= FarmingBottomlessCompost.capacity - 1) {
            mes(if (stored.charges == FarmingBottomlessCompost.capacity) {
                "Your bottomless compost bucket is full. You won't be able to squeeze any more compost in there."
            } else {
                "Your bottomless compost bucket is just about full. You won't be able to squeeze any more compost in there."
            })
            return
        }
        val sourceIndex = sourceSlot ?: inv.objs.indexOfFirst { candidate ->
            FarmingCompostSupplies.BUCKETS.any {
                candidate.isType(it.obj) && (stored.kind == Compost.NONE || it.kind == stored.kind)
            }
        }
        val source = inv.objs.getOrNull(sourceIndex)
        val form = selectedForm ?: FarmingCompostSupplies.BUCKETS.firstOrNull { source.isType(it.obj) }
        if (source == null || form == null || !source.isType(form.obj)) {
            mes(if (stored.kind == Compost.NONE) {
                "You don't have any compost to fill your bottomless compost bucket with."
            } else {
                "Your bottomless compost bucket contains ${stored.kind.label}. Sadly you don't have any more to fill it with any further."
            })
            return
        }
        if (stored.kind != Compost.NONE && stored.kind != form.kind) {
            mes("Your bottomless compost bucket already contains ${stored.kind.label}. You cannot fill it with ${form.kind.label}.")
            return
        }
        val maximum = minOf(
            inv.physicalCount(form.obj.internalName),
            (FarmingBottomlessCompost.capacity - stored.charges) / FarmingBottomlessCompost.chargesPerBucket,
        )
        val requested = countDialog("How many ${form.kind.label} would you like to fill with? (1 - $maximum)")
        if (requested <= 0) return
        val amount = compost.fill(player, slot, expected, sourceIndex, source, form, requested)
        if (amount == 0) return
        val total = stored.charges + amount * FarmingBottomlessCompost.chargesPerBucket
        val quantity = if (amount == 1) "a single bucket" else "$amount buckets"
        objbox(FarmingBottomlessCompost.FILLED,
            "You fill your bottomless compost bucket with $quantity of ${form.kind.label}. Your bottomless compost bucket now contains a total of $total uses.")
        if (form.noted) mes("As you deposit noted buckets of compost into your bottomless compost bucket, you mysteriously receive noted empty buckets back.")
        if (total == FarmingBottomlessCompost.capacity) mes("Your bottomless compost bucket is now full!")
    }
}
