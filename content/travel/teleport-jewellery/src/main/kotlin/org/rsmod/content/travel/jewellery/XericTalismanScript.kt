package org.rsmod.content.travel.jewellery

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.hook.ItemTeleportCaster
import org.rsmod.api.player.hook.TeleportStyle
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldSubOp
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.script.onOpWorn5
import org.rsmod.api.script.onOpWorn6
import org.rsmod.api.script.onOpWorn7
import org.rsmod.api.table.TeleportJewelleryDestinationsRow
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class XericTalismanScript @Inject constructor(private val caster: ItemTeleportCaster) :
    PluginScript() {
    private val destinations: List<TeleportJewelleryDestinationsRow> =
        DESTINATIONS.map(TeleportJewelleryDestinationsRow::getRow)
    private val charges =
        XericTalismanCharges(
            chargedName = CHARGED,
            inertName = INERT,
            fangName = FANG,
            bits = checkNotNull(ServerCacheManager.getVarObj(CHARGE_VAR.asRSCM(RSCMType.VAROBJ)))
                .bits,
            max = MAX_CHARGES,
        )

    override fun ScriptContext.startup() {
        onOpHeldSubOp(CHARGED) { if (it.op == HeldOp.Op3) teleport(inv, it.slot, it.subop) }
        onOpHeld4(CHARGED) { check(inv[it.slot]) }
        onOpHeld5(CHARGED) { uncharge(it.slot) }
        onOpHeld3(INERT) { check(inv[it.slot]) }
        onOpHeld4(INERT) { dismantle(it.slot) }
        onOpHeldU(CHARGED, FANG) { charge(it.firstSlot) }
        onOpHeldU(INERT, FANG) { charge(it.firstSlot) }

        onOpWorn2(CHARGED) { teleport(player.worn, it.slot, 0) }
        onOpWorn3(CHARGED) { teleport(player.worn, it.slot, 1) }
        onOpWorn4(CHARGED) { teleport(player.worn, it.slot, 2) }
        onOpWorn5(CHARGED) { teleport(player.worn, it.slot, 3) }
        onOpWorn6(CHARGED) { teleport(player.worn, it.slot, HONOUR) }
        onOpWorn7(CHARGED) { check(player.worn[Wearpos.Front.slot]) }
        onOpWorn2(INERT) { check(player.worn[Wearpos.Front.slot]) }
    }

    private fun ProtectedAccess.check(obj: InvObj?) {
        mes(
            when (val count = charges.charges(obj)) {
                0 -> "The talisman has no charges."
                1 -> "The talisman has one charge."
                else -> "The talisman has $count charges."
            }
        )
    }

    private suspend fun ProtectedAccess.teleport(container: Inventory, slot: Int, destination: Int) {
        if (actionDelay > mapClock) {
            return
        }
        val obj = container[slot] ?: return
        val row = destinations.getOrNull(destination)
        if (row == null) {
            mesbox(LOCKED_DESTINATION)
            return
        }
        if (charges.charges(obj) == 0) {
            check(obj)
            return
        }
        with(caster) {
            cast(TeleportType.Standard, row.coords, TeleportStyle.Xeric) {
                charges.spend(container, slot) != null
            }
        }
    }

    private suspend fun ProtectedAccess.charge(slot: Int) {
        when (val result = charges.charge(inv, slot)) {
            ChargeResult.Vanished -> return
            ChargeResult.Full -> mes("The talisman is full.")
            is ChargeResult.Charged -> mesbox("Your talisman now has ${result.total} charges.")
        }
    }

    private suspend fun ProtectedAccess.uncharge(slot: Int) {
        when (val result = charges.uncharge(inv, slot)) {
            UnchargeResult.Vanished,
            UnchargeResult.Empty -> return
            UnchargeResult.NoSpace -> mes("You don't have enough inventory space.")
            is UnchargeResult.Uncharged ->
                if (result.count == 1) {
                    mesbox("You remove a lizard fang from your talisman.")
                } else {
                    mesbox("You remove ${result.count} lizard fangs from your talisman.")
                }
        }
    }

    private suspend fun ProtectedAccess.dismantle(slot: Int) {
        val confirmed = choice2("Yes", true, "No", false, "Dismantle your Xeric Talisman?")
        if (!confirmed) {
            return
        }
        if (charges.dismantle(inv, slot, DISMANTLE_FANGS)) {
            mesbox("You dismantle the talisman into fang-like shards.")
        }
    }

    private companion object {
        private const val CHARGED = "obj.xeric_talisman"
        private const val INERT = "obj.xeric_talisman_empty"
        private const val FANG = "obj.lizardman_fang"
        private const val CHARGE_VAR = "varobj.charges_16383"
        private const val MAX_CHARGES = 1_000
        private const val DISMANTLE_FANGS = 100
        private const val HONOUR = 4
        private const val LOCKED_DESTINATION = "You have not yet unlocked that destination."

        private val DESTINATIONS =
            listOf(
                "dbrow.teleport_jewellery_xeric_lookout",
                "dbrow.teleport_jewellery_xeric_glade",
                "dbrow.teleport_jewellery_xeric_inferno",
                "dbrow.teleport_jewellery_xeric_heart",
            )
    }
}
