package org.rsmod.content.other.special.weapons.scripts.charge

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ranged.BlowpipeAmmo
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Loading darts and (for the toxic family) Zulrah's scales into a blowpipe, plus the real cache
 * options confirmed via a live cache dump: `Check`/`Unload`/`Uncharge`. Both the empty and
 * already-loaded cache variants accept more ammo - a blowpipe may only hold one dart type at a
 * time, enforced by [BlowpipeAmmo.storeDarts] itself.
 *
 * Real op layout, confirmed cache-side (`iop2=Wield, iop3=Check, iop4=Unload, iop5=Uncharge` on
 * the toxic loaded variants; `iop2=Wield, iop3=Check, iop5=Unload` on rosewood, which has no
 * scales and so no Uncharge). `Uncharge`/`Unload` deliberately replace the item's default Drop op
 * at position 5 - that's real Jagex behavior on this weapon, not something introduced here.
 *
 * `Check` while worn: the real cache lists `param.wear_op1=Check`, but in this engine op1 while
 * worn is hardcoded "Remove" regardless of what `wear_op1` claims - registering `onOpWorn1` made
 * the Remove button silently run Check instead of unequipping. Used `onOpWorn2` instead (the same
 * slot `TumekensShadowCharging` already uses for its own worn-Check), which is a genuinely free
 * slot in this engine and doesn't touch Remove.
 *
 * "Use scales on blowpipe" used to do nothing (only "blowpipe on scales" worked), despite
 * `onOpHeldU`'s own dispatch correctly trying both click orders. Root cause wasn't this
 * registration at all: Zulrah's scales are also a Herblore ingredient (Extended antivenom+,
 * `obj.antivenom+3` + scales), and `FinishedPotionsEvents` registers a catch-all "any herblore
 * ingredient used on anything" handler for every such item. Since that catch-all matched first
 * whenever scales were the item clicked first, and it silently no-ops when the second item isn't a
 * matching potion, dispatch never got to try the reversed order that would've reached this file's
 * own (pipe, scale) handler. Fixed by excluding `obj.snakeboss_scale` from that catch-all (see
 * `FinishedPotionsEvents.heldUExclude`) - the Extended antivenom+ recipe still works via
 * `obj.antivenom+3`'s own catch-all.
 */
class BlowpipeCharging @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        val dartLoadablePipes = BlowpipeAmmo.toxicBlowpipeAliases() + ROSEWOOD_ALIASES
        for (pipe in dartLoadablePipes) {
            for (dart in BlowpipeAmmo.toxicDartAliases()) {
                onOpHeldU(pipe, dart) { loadDarts(inv, it.firstSlot, it.secondSlot, dart) }
            }
        }

        for (pipe in BlowpipeAmmo.toxicBlowpipeAliases()) {
            onOpHeldU(pipe, SCALE_ITEM) { loadScales(inv, it.firstSlot) }
        }

        for (pipe in BlowpipeAmmo.toxicLoadedBlowpipeAliases()) {
            onOpHeld3(pipe) { checkAmmo(inv[it.slot]) }
            onOpWorn2(pipe) { checkAmmo(player.righthand) }
        }
        onOpHeld3(ROSEWOOD_LOADED) { checkAmmo(inv[it.slot]) }
        onOpWorn2(ROSEWOOD_LOADED) { checkAmmo(player.righthand) }

        // Toxic: Unload (darts only, keeps scales) is op4, Uncharge (darts + scales) is op5.
        onOpHeld4("obj.toxic_blowpipe_loaded") { unloadDarts(inv, it.slot) }
        onOpHeld4("obj.toxic_blowpipe_loaded_ornament") { unloadDarts(inv, it.slot) }
        onOpHeld5("obj.toxic_blowpipe_loaded") { uncharge(inv, it.slot) }
        onOpHeld5("obj.toxic_blowpipe_loaded_ornament") { uncharge(inv, it.slot) }

        // Rosewood has no scales, so its only removal op ("Unload") sits at op5 instead.
        onOpHeld5(ROSEWOOD_LOADED) { unloadDarts(inv, it.slot) }
    }

    private suspend fun ProtectedAccess.loadDarts(
        inventory: Inventory,
        pipeSlot: Int,
        dartSlot: Int,
        dartAlias: String,
    ) {
        val dartObj = inventory[dartSlot] ?: return
        val dartType = getInvObj(dartObj)
        val available = dartObj.count

        val question = "How many darts do you want to load? (up to ${available.formatAmount})"
        val requested = countDialog(question).coerceIn(1, available)

        val added = BlowpipeAmmo.storeDarts(inventory, pipeSlot, dartType, requested)
        if (added <= 0) {
            mes("You can't load that many darts, or that dart type doesn't match what's already loaded.")
            return
        }

        val removed = invDel(inv, dartAlias, added)
        if (removed.failure) {
            return
        }
        mes("You load ${added.formatAmount} darts into your blowpipe.")
    }

    private suspend fun ProtectedAccess.loadScales(inventory: Inventory, pipeSlot: Int) {
        val available = invTotal(inv, SCALE_ITEM)
        if (available <= 0) {
            mes("You don't have any Zulrah's scales.")
            return
        }

        val question = "How many scales do you want to load? (up to ${available.formatAmount})"
        val requested = countDialog(question).coerceIn(1, available)

        val added = BlowpipeAmmo.storeScales(inventory, pipeSlot, requested)
        if (added <= 0) {
            mes("Your blowpipe is already full of scales.")
            return
        }

        val removed = invDel(inv, SCALE_ITEM, added)
        if (removed.failure) {
            return
        }
        mes("You load ${added.formatAmount} Zulrah's scales into your blowpipe.")
    }

    private fun ProtectedAccess.checkAmmo(obj: InvObj?) {
        val dartCount = BlowpipeAmmo.darts(obj)
        if (dartCount <= 0) {
            mes("Your blowpipe has no darts loaded.")
            return
        }
        val dart = BlowpipeAmmo.storedDart(obj)
        val dartName = dart?.type?.name?.lowercase() ?: "darts"
        if (BlowpipeAmmo.isToxic(obj)) {
            val scaleCount = BlowpipeAmmo.scales(obj)
            mes(
                "Your blowpipe currently has ${dartCount.formatAmount} $dartName and " +
                    "${scaleCount.formatAmount} charges of scales."
            )
        } else {
            mes("Your blowpipe currently has ${dartCount.formatAmount} $dartName loaded.")
        }
    }

    private suspend fun ProtectedAccess.unloadDarts(inventory: Inventory, slot: Int) {
        val obj = inventory[slot] ?: return
        val count = BlowpipeAmmo.darts(obj)
        if (count <= 0) {
            mes("Your blowpipe has no darts loaded.")
            return
        }
        if (inv.freeSpace() < 1) {
            mes("You don't have enough inventory space to unload your darts.")
            return
        }

        val unloaded = BlowpipeAmmo.unloadDarts(inventory, slot) ?: return
        val dart = unloaded.dart
        if (dart != null && unloaded.count > 0) {
            invAddOrDropType(objRepo, dart.type, unloaded.count)
        }
        mes("You remove ${unloaded.count.formatAmount} darts from your blowpipe.")
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, slot: Int) {
        val obj = inventory[slot] ?: return
        val dartCount = BlowpipeAmmo.darts(obj)
        val scaleCount = BlowpipeAmmo.scales(obj)
        if (dartCount <= 0 && scaleCount <= 0) {
            mes("Your blowpipe is already empty.")
            return
        }

        val spaceNeeded = (if (dartCount > 0) 1 else 0) + (if (scaleCount > 0) 1 else 0)
        if (inv.freeSpace() < spaceNeeded) {
            mes("You don't have enough inventory space to uncharge your blowpipe.")
            return
        }

        val confirmed =
            choice2(
                "Proceed.",
                true,
                "Cancel.",
                false,
                title = "Uncharge all darts and scales from your blowpipe?",
            )
        if (!confirmed) {
            return
        }

        val contents = BlowpipeAmmo.uncharge(inventory, slot) ?: return
        val dart = contents.dart
        if (dart != null && contents.dartCount > 0) {
            invAddOrDropType(objRepo, dart.type, contents.dartCount)
        }
        if (contents.scaleCount > 0) {
            invAddOrDrop(objRepo, SCALE_ITEM, contents.scaleCount)
        }
        mes("You uncharge your blowpipe.")
    }

    private companion object {
        const val SCALE_ITEM: String = "obj.snakeboss_scale"
        const val ROSEWOOD_LOADED: String = "obj.rosewood_blowpipe"
        val ROSEWOOD_ALIASES = listOf(ROSEWOOD_LOADED, "obj.rosewood_blowpipe_empty")
    }
}
