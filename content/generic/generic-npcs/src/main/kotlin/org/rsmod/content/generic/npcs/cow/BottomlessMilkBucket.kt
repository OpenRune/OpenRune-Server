package org.rsmod.content.generic.npcs.cow

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.bottomless.milk.MilkSources
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.type.cert
import org.rsmod.game.type.uncert
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): [system:bottomless_milk] using a charge for churns and the remaining quests.
class BottomlessMilkBucket @Inject constructor(private val charges: ObjChargeManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(EMPTY) { fill(it.slot, used = null) }
        onOpHeld3(FILLED) { fill(it.slot, used = null) }
        onOpHeld4(EMPTY) { check(it.slot) }
        onOpHeld4(FILLED) { check(it.slot) }
        onOpHeld5(FILLED) { empty(it.slot) }
        val forms = MilkSources.types.values.flatMap { listOfNotNull(it, noted(it)) }
        for (bucket in listOf(EMPTY, FILLED)) {
            for (milk in forms) {
                onOpHeldU(bucket, milk) { fill(bucketSlot(it.firstSlot, it.secondSlot), milk) }
            }
        }
    }

    private fun ProtectedAccess.bucketSlot(first: Int, second: Int): Int =
        if (inv[first].isBucket()) first else second

    internal suspend fun ProtectedAccess.check(slot: Int) {
        val bucket = inv[slot] ?: return
        val type = MilkSources.typeIn(bucket)
        if (type == null) {
            objbox(EMPTY, zoom = 400, "Your milk bucket is currently empty.")
            return
        }
        val uses = MilkSources.uses(bucket)
        val count = if (uses == 1) "one use" else "${number(uses)} uses"
        objbox(
            FILLED,
            zoom = 400,
            "Your bottomless milk bucket is currently holding $count of ${MilkSources.nameOf(type)}.",
        )
    }

    // The milk it holds, or the first milk held for an empty one; with [used], from that form
    // of it first and then the other.
    internal suspend fun ProtectedAccess.fill(slot: Int, used: String?) {
        val bucket = inv[slot]?.takeIf { it.isBucket() } ?: return
        val uses = MilkSources.uses(bucket)
        val held = MilkSources.types.filterValues { held(it) > 0 }.keys
        val bucketType = MilkSources.typeIn(bucket)
        val type =
            when {
                used != null -> MilkSources.typeOf(unnoted(used))
                bucketType != null -> bucketType
                // UNVERIFIED: which milk an empty bucket takes when several are held.
                else -> held.minOrNull()
            }
        if (used != null && bucketType != null && type != bucketType) {
            mes(
                "Your bottomless milk bucket already contains a different type of milk. You " +
                    "cannot fill it with ${type(unnoted(used)).name.lowercase()}."
            )
            return
        }
        if (type == null || type !in held) {
            if (held.isNotEmpty()) {
                mes("You don't have any more of this type of milk to fill it with.")
            } else {
                mes("You don't have any milk to fill your bottomless milk bucket with.")
            }
            return
        }
        if (uses >= MAX_USES) {
            mes("Your bottomless milk bucket is... full.")
            return
        }
        val milk = checkNotNull(MilkSources.types[type])
        val note = noted(milk)
        val max = min(held(milk), MAX_USES - uses)
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
        if (inv[slot] != bucket || held(milk) < amount) {
            return
        }
        val preferred = used ?: milk
        val other = if (preferred == milk) note else milk
        val fromPreferred = min(amount, inv.count(preferred))
        val fromOther = amount - fromPreferred
        if (!pour(preferred, fromPreferred)) {
            return
        }
        if (other != null && !pour(other, fromOther)) {
            refill(preferred, fromPreferred)
            return
        }
        if (charges.addCharges(inv, slot, amount, USES, MAX_USES).isFailure()) {
            refill(preferred, fromPreferred)
            other?.let { refill(it, fromOther) }
            return
        }
        if (uses == 0) {
            inv[slot] = MilkSources.withType(checkNotNull(inv[slot]), type)
        }
        if (note != null && inv.count(BUCKET_NOTE) > 0 && (preferred == note || fromOther > 0)) {
            // UNVERIFIED: the wording for milks other than regular milk.
            mes(
                "As you deposit noted buckets of milk into your bottomless milk bucket, you " +
                    "mysteriously receive noted empty buckets back."
            )
        }
        soundSynth("synth.bucket_pour")
        val poured = if (amount == 1) "a single bucket" else "${number(amount)} buckets"
        objbox(
            FILLED,
            zoom = 400,
            "You fill your bottomless milk bucket with $poured of ${MilkSources.nameOf(type)}. Your " +
                "bottomless milk bucket now contains a total of ${number(uses + amount)} uses.",
        )
    }

    private fun ProtectedAccess.held(milk: String): Int =
        inv.count(milk) + (noted(milk)?.let { inv.count(it) } ?: 0)

    // Live asks after the amount, and only for an empty bucket.
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

    private fun ProtectedAccess.pour(milk: String, count: Int): Boolean =
        count == 0 || invReplace(inv, milk, count, emptyOf(milk)).success

    private fun ProtectedAccess.refill(milk: String, count: Int) {
        if (count > 0) {
            invReplace(inv, emptyOf(milk), count, milk)
        }
    }

    internal suspend fun ProtectedAccess.empty(slot: Int) {
        val bucket = inv[slot]?.takeIf { it.isType(FILLED) } ?: return
        objbox(
            FILLED,
            zoom = 400,
            "If you empty the bucket, you will <col=7f0000>DISCARD</col> all of the milk inside it. " +
                "<col=7f0000>You will NOT reobtain this milk.</col>",
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
        MilkSources.empty(inv, slot)
        soundSynth("synth.bottomless_milk_bucket_discard")
        objbox(EMPTY, zoom = 400, "You discard the contents of your bottomless milk bucket.")
    }

    private fun InvObj?.isBucket(): Boolean = isType(EMPTY) || isType(FILLED)

    companion object {
        const val EMPTY = MilkSources.BUCKET_EMPTY
        const val FILLED = MilkSources.BUCKET_FILLED
        const val USES = MilkSources.USES
        const val MAX_USES = MilkSources.MAX_USES
        private const val BUCKET = "obj.bucket_empty"
        private const val BUCKET_NOTE = "obj.cert_bucket_empty"

        private fun emptyOf(milk: String): String = if (isNoted(milk)) BUCKET_NOTE else BUCKET

        private fun type(name: String): ItemServerType =
            checkNotNull(ServerCacheManager.getItem(name.asRSCM(RSCMType.OBJ)))

        private fun isNoted(name: String): Boolean = type(name).isCert

        private fun unnoted(name: String): String = uncert(type(name)).internalName

        private fun noted(milk: String): String? =
            cert(milk).takeIf { it.isCert }?.internalName

        // UNVERIFIED: thousands separators above 999 (the captures stay below).
        fun number(count: Int): String = "%,d".format(count)
    }
}
