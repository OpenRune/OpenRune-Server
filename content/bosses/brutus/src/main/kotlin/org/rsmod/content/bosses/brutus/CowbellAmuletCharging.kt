package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn4
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.utils.bits.withBits

/**
 * Charging, uncharging and checking the cowbell amulet with air runes, one charge per rune
 * (https://oldschool.runescape.wiki/w/Cowbell_amulet and its transcript). Dialogue types: live
 * capture, rev 241, 20261012T200152-df49f48@50-187, @498.
 */
class CowbellAmuletCharging @Inject constructor(private val charges: ObjChargeManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld4(CHARGED) { check(inv[it.slot]) }
        onOpHeld4(EMPTY) { charge(it.slot) }
        onOpHeld5(CHARGED) { uncharge(it.slot) }
        onOpHeldU(CHARGED, AIR_RUNE) { charge(it.firstSlot) }
        onOpHeldU(EMPTY, AIR_RUNE) { charge(it.firstSlot) }
        onOpWorn4(CHARGED) { check(player.worn[Wearpos.Front.slot]) }
    }

    internal fun ProtectedAccess.check(obj: InvObj?) {
        mes("The amulet has ${plural(charges.getCharges(obj, CHARGES))}.")
    }

    internal suspend fun ProtectedAccess.charge(slot: Int) {
        val runes = inv.count(AIR_RUNE)
        if (runes == 0) {
            mesbox("You don't have any Air Runes to charge the amulet with.")
            return
        }
        val room = MAX_CHARGES - charges.getCharges(inv[slot], CHARGES)
        if (room <= 0) {
            // live capture, rev 241, 20261012T222638-df49f48@209
            mes("The amulet is already fully charged.")
            return
        }
        val max = min(runes, room)
        val requested = min(countDialog("How many charges do you want to apply? (Up to $max)"), max)
        if (requested == 0) {
            return
        }
        val amulet = inv[slot]
        if (!amulet.isType(EMPTY) && !amulet.isType(CHARGED)) {
            return
        }
        val total = charges.getCharges(amulet, CHARGES) + requested
        if (total > MAX_CHARGES || invDel(inv, AIR_RUNE, requested).failure) {
            return
        }
        if (charges.addCharges(inv, slot, requested, CHARGES, MAX_CHARGES).isFailure()) {
            invAdd(inv, AIR_RUNE, requested)
            return
        }
        soundSynth(CHARGE_SYNTH)
        objbox(
            CHARGED,
            zoom = 400,
            "You add ${number(requested)} air ${runeWord(requested)} to your amulet. It now has " +
                "${plural(total)}.",
        )
    }

    internal suspend fun ProtectedAccess.uncharge(slot: Int) {
        val count = charges.getCharges(inv[slot], CHARGES)
        if (count == 0) {
            return
        }
        if (invAdd(inv, AIR_RUNE, count).failure) {
            // UNVERIFIED: the full inventory message.
            mes("You don't have enough inventory space.")
            return
        }
        charges.removeAllCharges(inv, slot, CHARGES)
        // live capture, rev 241, 20261012T200152-df49f48@498
        soundSynth(UNCHARGE_SYNTH)
        doubleobjbox(
            EMPTY,
            AIR_RUNE,
            "You uncharge your amulet by removing ${number(count)} air ${runeWord(count)} from it.",
        )
    }

    companion object {
        const val CHARGED = "obj.cowbell_amulet"
        const val EMPTY = "obj.cowbell_amulet_empty"
        const val AIR_RUNE = "obj.airrune"
        const val CHARGES = "varobj.charges_16383"
        const val MAX_CHARGES = 1_000
        private const val CHARGE_SYNTH = "synth.cowbell_amulet_charge"
        private const val UNCHARGE_SYNTH = "synth.cowbell_amulet_uncharge"

        private val chargeBits by lazy {
            checkNotNull(ServerCacheManager.getVarObj(CHARGES.asRSCM(RSCMType.VAROBJ))).bits
        }

        /** The amulet after a teleport left it [left] charges; at none it turns back empty. */
        fun afterTeleport(amulet: InvObj, left: Int): InvObj =
            if (left <= 0) InvObj(EMPTY) else amulet.copy(vars = amulet.vars.withBits(chargeBits, left))

        // live capture, rev 241, 20261012T222638-df49f48@181-591: counts have thousands separators.
        fun plural(count: Int): String = if (count == 1) "1 charge" else "${number(count)} charges"

        fun number(count: Int): String = NumberFormat.getIntegerInstance(Locale.UK).format(count)

        private fun runeWord(count: Int): String = if (count == 1) "rune" else "runes"
    }
}
