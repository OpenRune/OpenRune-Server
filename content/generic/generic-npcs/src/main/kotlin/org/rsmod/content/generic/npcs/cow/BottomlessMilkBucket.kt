package org.rsmod.content.generic.npcs.cow

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The bottomless milk bucket holds up to 10,000 uses of milk, filled from buckets of milk (noted
 * or not) or straight from a dairy cow (https://oldschool.runescape.wiki/w/Bottomless_milk_bucket
 * and its transcript). Dialogue types, prompts and order: live capture, rev 241,
 * 20261012T222638-df49f48@1536-1781.
 *
 * TODO(content): [system:bottomless_milk] Chocolatey milk, milky mixture and hangover cure, and
 * using a charge for quests, churns and cats.
 */
class BottomlessMilkBucket @Inject constructor(private val charges: ObjChargeManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(EMPTY) { fill(it.slot, MILK) }
        onOpHeld3(FILLED) { fill(it.slot, MILK) }
        onOpHeld4(EMPTY) { check(it.slot) }
        onOpHeld4(FILLED) { check(it.slot) }
        onOpHeld5(FILLED) { empty(it.slot) }
        for (bucket in listOf(EMPTY, FILLED)) {
            onOpHeldU(bucket, MILK) { fill(bucketSlot(it.firstSlot, it.secondSlot), MILK) }
            onOpHeldU(bucket, MILK_NOTE) {
                fill(bucketSlot(it.firstSlot, it.secondSlot), MILK_NOTE)
            }
        }
    }

    private fun ProtectedAccess.bucketSlot(first: Int, second: Int): Int =
        if (inv[first].isBucket()) first else second

    internal suspend fun ProtectedAccess.check(slot: Int) {
        val bucket = inv[slot] ?: return
        val uses = charges.getCharges(bucket, USES)
        if (uses == 0) {
            objbox(EMPTY, zoom = 400, "Your milk bucket is currently empty.")
            return
        }
        objbox(
            FILLED,
            zoom = 400,
            "Your bottomless milk bucket is currently holding ${number(uses)} " +
                "${if (uses == 1) "use" else "uses"} of regular milk.",
        )
    }

    /** Fills the bucket in [slot], from [preferred] milk first and then the other form. */
    internal suspend fun ProtectedAccess.fill(slot: Int, preferred: String) {
        val bucket = inv[slot]?.takeIf { it.isBucket() } ?: return
        val uses = charges.getCharges(bucket, USES)
        val held = inv.count(MILK) + inv.count(MILK_NOTE)
        if (held == 0) {
            // live capture, rev 241, 20261012T222638-df49f48@1613
            mes("You don't have any milk to fill your bottomless milk bucket with.")
            return
        }
        if (uses >= MAX_USES) {
            // https://oldschool.runescape.wiki/w/Bottomless_milk_bucket#Trivia
            mes("Your bottomless milk bucket is... full.")
            return
        }
        val max = min(held, MAX_USES - uses)
        val amount =
            if (max == 1) {
                1
            } else {
                min(countDialog("How much milk would you like to fill the bucket with? (1 - $max)"), max)
            }
        if (amount <= 0) {
            return
        }
        if (uses == 0 && !confirmFirstFill()) {
            return
        }
        if (inv[slot] != bucket || inv.count(MILK) + inv.count(MILK_NOTE) < amount) {
            return
        }
        val other = if (preferred == MILK) MILK_NOTE else MILK
        val fromPreferred = min(amount, inv.count(preferred))
        val fromOther = amount - fromPreferred
        if (!pour(preferred, fromPreferred)) {
            return
        }
        if (!pour(other, fromOther)) {
            refill(preferred, fromPreferred)
            return
        }
        if (charges.addCharges(inv, slot, amount, USES, MAX_USES).isFailure()) {
            refill(preferred, fromPreferred)
            refill(other, fromOther)
            return
        }
        if (inv.count(BUCKET_NOTE) > 0 && (preferred == MILK_NOTE || fromOther > 0)) {
            // live capture, rev 241, 20261012T222638-df49f48@1603
            mes(
                "As you deposit noted buckets of milk into your bottomless milk bucket, you " +
                    "mysteriously receive noted empty buckets back."
            )
        }
        val poured = if (amount == 1) "a single bucket" else "${number(amount)} buckets"
        objbox(
            FILLED,
            zoom = 400,
            "You fill your bottomless milk bucket with $poured of regular milk. Your bottomless " +
                "milk bucket now contains a total of ${number(uses + amount)} uses.",
        )
    }

    /** Live asks after the amount, and only for an empty bucket. */
    private suspend fun ProtectedAccess.confirmFirstFill(): Boolean {
        objbox(
            EMPTY,
            zoom = 400,
            "If this bucket is emptied later, <col=7f0000>ALL</col> remaining milk inside it will " +
                "be <col=7f0000>LOST</col>.</col>",
        )
        var proceed = false
        startDialogue {
            proceed = choice2("Yes.", true, "No.", false, title = "Do you wish to proceed?")
        }
        return proceed
    }

    /** Swaps [count] of [milk] for empty buckets of the same form, all or nothing. */
    private fun ProtectedAccess.pour(milk: String, count: Int): Boolean =
        count == 0 || invReplace(inv, milk, count, emptyOf(milk)).success

    private fun ProtectedAccess.refill(milk: String, count: Int) {
        if (count > 0) {
            invReplace(inv, emptyOf(milk), count, milk)
        }
    }

    internal suspend fun ProtectedAccess.empty(slot: Int) {
        val bucket = inv[slot]?.takeIf { it.isType(FILLED) } ?: return
        // UNVERIFIED: the warning's dialogue type (the transcript gives only the text).
        objbox(
            FILLED,
            zoom = 400,
            "If you empty the bucket, you will DISCARD all of the milk inside it. You will NOT " +
                "reobtain this milk.",
        )
        var discard = false
        startDialogue {
            discard =
                choice2(
                    "Yes - I acknowledge I'll lose all milk within the bucket.",
                    true,
                    "No - don't proceed.",
                    false,
                    title = "Proceed to discard the contents?",
                )
        }
        if (!discard || inv[slot] != bucket) {
            return
        }
        charges.removeAllCharges(inv, slot, USES)
        // UNVERIFIED: the dialogue type.
        objbox(EMPTY, zoom = 400, "You discard the contents of your bottomless milk bucket.")
    }

    private fun InvObj?.isBucket(): Boolean = isType(EMPTY) || isType(FILLED)

    companion object {
        const val EMPTY = "obj.bottomless_milk_bucket"
        const val FILLED = "obj.bottomless_milk_bucket_filled"
        const val USES = "varobj.charges_16383"
        const val MAX_USES = 10_000
        private const val MILK = "obj.bucket_milk"
        private const val MILK_NOTE = "obj.cert_bucket_milk"
        private const val BUCKET = "obj.bucket_empty"
        private const val BUCKET_NOTE = "obj.cert_bucket_empty"

        private fun emptyOf(milk: String): String = if (milk == MILK) BUCKET else BUCKET_NOTE

        // UNVERIFIED: thousands separators above 999 (the captures stay below).
        fun number(count: Int): String = "%,d".format(count)
    }
}
