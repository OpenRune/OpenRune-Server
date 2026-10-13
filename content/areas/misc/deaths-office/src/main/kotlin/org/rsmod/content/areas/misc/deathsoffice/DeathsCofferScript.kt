package org.rsmod.content.areas.misc.deathsoffice

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.invDel
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.ironman.isUltimateIronman
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.other.gravestones.deathCoffer
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.cofferShown by intVarp("varp.if1")
private var Player.selectedSlot by intVarp("varp.if2")
private var Player.selectedCount by intVarp("varp.if3")
private var Player.creditEach by intVarp("varp.if4")

/**
 * Death's Coffer: items sacrificed here are credited at 105% of their price to [deathCoffer], which
 * pays death fees. The client's `script3483` draws the selection from `if2` (inventory slot), `if3`
 * (quantity) and `if4` (credit per item).
 */
class DeathsCofferScript @Inject constructor(private val marketPrices: MarketPrices) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(COFFER_LOC) { openCoffer() }
        onIfModalButton(SIDE_ITEMS) { selectOrExamine(it) }
        onIfModalButton(QUANTITY_1) { setCount(1) }
        onIfModalButton(QUANTITY_5) { setCount(5) }
        onIfModalButton(QUANTITY_X) { countX() }
        onIfModalButton(QUANTITY_ALL) { setCount(Int.MAX_VALUE) }
        onIfModalButton(CONFIRM) { confirm() }
    }

    private fun ProtectedAccess.openCoffer() {
        if (player.isUltimateIronman) {
            // UNVERIFIED: that this is a game message rather than a dialogue box.
            mes(
                "Ultimate Iron Men do not use gravestones like other players do, by the way, so " +
                    "they are not invited to sacrifice items to Death's Coffer to pay the fees."
            )
            return
        }
        player.selectedCount = 0
        player.selectedSlot = -1
        player.cofferShown = player.deathCoffer
        ifSetObj(DISPLAY, BLANK_OBJ, DISPLAY_ZOOM)
        ifOpenMainSidePair(MAIN, SIDE)
        interfaceInvInit(inv, SIDE_ITEMS, objRowCount = 4, objColCount = 7, op1 = "Select")
        ifSetEvents(SIDE_ITEMS, 0 until inv.size, IfEvent.Op1, IfEvent.Op10)
    }

    private fun ProtectedAccess.selectOrExamine(button: IfModalButton) {
        val slot = button.comsub
        val obj = if (slot in inv.indices) inv[slot] else null
        if (obj == null) {
            return
        }
        if (button.op == IfButtonOp.Op10) {
            mes(getInvObj(obj).examine)
            return
        }
        player.creditEach = credit(getInvObj(obj))
        player.selectedCount = 1
        player.selectedSlot = slot
        ifSetObj(DISPLAY, getInvObj(obj).internalName, DISPLAY_ZOOM)
    }

    /** Nothing happens with no selection. */
    private fun ProtectedAccess.setCount(count: Int) {
        if (player.selectedSlot < 0) {
            return
        }
        player.selectedCount = count
    }

    private suspend fun ProtectedAccess.countX() {
        if (player.selectedSlot < 0) {
            return
        }
        player.selectedCount = -1
        player.selectedCount = countDialog("How many would you like to sacrifice?")
    }

    /**
     * The credit goes into the coffer, the selection clears and the portal plays its shrink
     * animation, then its loop three ticks later.
     */
    private suspend fun ProtectedAccess.confirm() {
        val slot = player.selectedSlot
        val obj = if (slot in inv.indices) inv[slot] else null
        if (obj == null || player.selectedCount <= 0) {
            return
        }
        val type = getInvObj(obj)
        if (!isAccepted(type)) {
            mes("You cannot trade that item to Death.")
            return
        }
        val each = credit(type)
        player.creditEach = each
        if (each < MIN_CREDIT) {
            mes(
                "Death will not accept items worth less than 10,000 coins per " +
                    "<col=ef1020>individual item</col>."
            )
            return
        }
        val count = player.selectedCount.coerceAtMost(inv.count(type.internalName))
        val total = each.toLong() * count
        if (player.deathCoffer + total > Int.MAX_VALUE) {
            mes("The coffer is too full to accommodate that.")
            return
        }
        if (player.invDel(inv, type.internalName, count).failure) {
            return
        }
        player.deathCoffer += total.toInt()
        player.cofferShown = player.deathCoffer
        player.creditEach = 0
        player.selectedCount = 0
        player.selectedSlot = -1
        ifSetAnim(PORTAL, seq(PORTAL_SHRINK))
        ifSetObj(DISPLAY, BLANK_OBJ, DISPLAY_ZOOM)
        delay(3)
        ifSetAnim(PORTAL, seq(PORTAL_LOOP))
    }

    // UNVERIFIED: the wiki also refuses tradeables off the Grand Exchange, but the cache's
    // stockmarket flag is false for an amulet of glory(2), which live accepted, so it isn't used
    // here.
    // TODO: The wiki also refuses Leagues and Grid Master rewards, a few named weapons and newly
    // released objs.
    private fun isAccepted(obj: ItemServerType): Boolean {
        val type = if (obj.isCert) ServerCacheManager.getItem(obj.certlink) ?: obj else obj
        return type.tradeable && !type.param(params.bond_item)
    }

    /**
     * 105% of the obj's price, rounded down (an amulet of glory(2) credited 13,351), capped at
     * 2,100,000,000 per obj.
     */
    private fun credit(obj: ItemServerType): Int {
        val price = (marketPrices[obj] ?: 0).toLong()
        return (price * CREDIT_PERCENT / 100).coerceAtMost(MAX_CREDIT).toInt()
    }

    private fun seq(name: String) = ServerCacheManager.getAnim(name.asRSCM(RSCMType.SEQ))

    private companion object {
        const val COFFER_LOC = "loc.death_coffer_access"
        const val MAIN = "interface.death_coffer"
        const val SIDE = "interface.death_coffer_side"
        const val SIDE_ITEMS = "component.death_coffer_side:items"
        const val DISPLAY = "component.death_coffer:display"
        const val PORTAL = "component.death_coffer:portal"
        const val QUANTITY_1 = "component.death_coffer:1"
        const val QUANTITY_5 = "component.death_coffer:5"
        const val QUANTITY_X = "component.death_coffer:x"
        const val QUANTITY_ALL = "component.death_coffer:all"
        const val CONFIRM = "component.death_coffer:confirm"
        const val BLANK_OBJ = "obj.blankobject"
        const val DISPLAY_ZOOM = 400
        const val PORTAL_SHRINK = "seq.poh_portal_shrinkunshrinkloop"
        const val PORTAL_LOOP = "seq.poh_portal_loop"

        const val CREDIT_PERCENT = 105L
        const val MIN_CREDIT = 10_000
        const val MAX_CREDIT = 2_100_000_000L
    }
}
