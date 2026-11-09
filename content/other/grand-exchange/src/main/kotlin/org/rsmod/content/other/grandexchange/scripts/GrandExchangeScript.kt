package org.rsmod.content.other.grandexchange.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.varp.VarpServerType
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.ClientScripts.mesLayerClose
import org.rsmod.api.player.output.VarpSync
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onApNpc4
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.content.other.grandexchange.GeClientSync
import org.rsmod.content.other.grandexchange.GrandExchange
import org.rsmod.content.other.grandexchange.GrandExchange.Companion.held
import org.rsmod.content.other.grandexchange.market.GeAccount
import org.rsmod.content.other.grandexchange.market.GeOfferType
import org.rsmod.content.other.grandexchange.prices.GePrices
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.uncert
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GrandExchangeScript
@Inject
constructor(
    private val exchange: GrandExchange,
    private val prices: GePrices,
    private val serverConfig: ServerConfig,
) : PluginScript() {
    private var Player.selectedSlot by intVarBit("varbit.ge_selectedslot")
    private var Player.newOfferType by intVarBit("varbit.ge_newoffer_type")
    private var Player.newOfferQuantity by intVarBit("varbit.ge_newoffer_quantity")
    private var Player.customPercent by intVarBit("varbit.ge_price_custom")
    private var Player.searchObj by intVarp("varp.tradingpost_search")
    private var Player.newOfferPriceVar by intVarp("varp.ge_newoffer_price")

    private var Player.newOfferPrice: Long
        get() = newOfferPriceVar.toLong()
        set(value) {
            val price = value.coerceIn(0, GrandExchange.MAX_PRICE).toInt()
            newOfferPriceVar = price
            VarpSync.writeVarp(this, NEW_OFFER_PRICE_VARP, price.toLong())
        }

    override fun ScriptContext.startup() {
        val config = serverConfig.gameplay.grandExchange
        if (!config.enabled) {
            return
        }
        exchange.load()
        prices.start(config)

        onEvent<GameLifecycle.LateCycle> { exchange.tick() }
        onPlayerLogin { exchange.login(player) }
        onPlayerLogout { exchange.logout(player) }

        for (clerk in CLERKS) {
            onApNpc1(clerk) { if (isWithinApRange(it.npc, distance = 2)) talkToClerk(it.npc) }
            onOpNpc1(clerk) { talkToClerk(it.npc) }
            onApNpc3(clerk) { if (isWithinApRange(it.npc, distance = 2)) openExchange() }
            onOpNpc3(clerk) { openExchange() }
            onApNpc4(clerk) { if (isWithinApRange(it.npc, distance = 2)) openHistory() }
            onOpNpc4(clerk) { openHistory() }
        }
        onOpLoc1(BOOTH) { openExchange() }
        onOpLoc2(BOOTH) { openExchange() }

        onIfClose("interface.ge_offers") { player.closeExchange() }

        for (slot in 0 until GeAccount.SLOT_COUNT) {
            onIfModalButton("component.ge_offers:index_$slot") { indexOp(slot, it.comsub, it.op) }
        }
        onIfModalButton("component.ge_offers:back") { player.backToIndex() }
        onIfModalButton("component.ge_offers:history") { openHistory() }
        onIfModalButton("component.ge_offers:setup") { setupOp(it.comsub, it.op) }
        onIfModalButton("component.ge_offers:setup_confirm") { confirm() }
        onIfModalButton("component.ge_offers:details_status") { detailsOp(it.comsub, it.op) }
        onIfModalButton("component.ge_offers:details_collect") { collectOp(it.comsub, it.op) }
        onIfModalButton("component.ge_offers:collectall") { collectAllOp(it.comsub, it.op) }
        onIfModalButton("component.ge_offers_side:items") { offerFromInventory(it.comsub, it.op) }
        onIfModalButton("component.ge_history:exchange") { openExchange() }
    }

    private suspend fun ProtectedAccess.talkToClerk(npc: Npc) {
        var choice = 0
        startDialogue(npc) {
            chatNpc(
                happy,
                "Welcome to the Grand Exchange. Would you like to trade now, or see your " +
                    "trade history?",
            )
            choice = choice2("I'd like to trade.", 1, "Show me my trade history.", 2)
        }
        when (choice) {
            1 -> openExchange()
            2 -> openHistory()
        }
    }

    private fun ProtectedAccess.openExchange() {
        if (!exchange.ready) {
            mes("The Grand Exchange is still opening. Try again in a moment.")
            return
        }
        val account = exchange.account(player)
        player.resetSetup()
        player.selectedSlot = 0
        for (slot in 0 until GeAccount.SLOT_COUNT) {
            VarPlayerIntMapSetter.set(player, "varp.ge_itemsink_obj_$slot", -1)
        }
        if (player.vars["varp.ge_last_offer_item"] <= 0) {
            VarPlayerIntMapSetter.set(player, "varp.ge_last_offer_item", -1)
        }
        GeClientSync.syncAll(player, account)
        ifOpenMainSidePair("interface.ge_offers", "interface.ge_offers_side", -1, -1)
        for (slot in 0 until GeAccount.SLOT_COUNT) {
            player.startInvTransmit(GeClientSync.collectInv(player, slot))
            player.ifSetEvents(
                "component.ge_offers:index_$slot",
                2..4,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
            )
        }
        player.ifSetEvents("component.ge_offers:setup", 0..15, IfEvent.Op1, IfEvent.Op2)
        player.ifSetEvents("component.ge_offers:details_status", 0..1, IfEvent.Op1)
        player.ifSetEvents(
            "component.ge_offers:details_collect",
            2..3,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
        )
        player.ifSetEvents("component.ge_offers:collectall", 0..1, IfEvent.Op1, IfEvent.Op2)
        player.ifSetEvents("component.ge_offers_side:items", 0..27, IfEvent.Op1, IfEvent.Op10)
    }

    private fun Player.closeExchange() {
        for (slot in 0 until GeAccount.SLOT_COUNT) {
            stopInvTransmit(GeClientSync.collectInv(this, slot))
        }
        mesLayerClose(this, constants.meslayer_mode_objsearch)
        selectedSlot = 0
        resetSetup()
    }

    private fun ProtectedAccess.openHistory() {
        if (!exchange.ready) {
            mes("The Grand Exchange is still opening. Try again in a moment.")
            return
        }
        ifOpenMainModal("interface.ge_history")
        val history = GrandExchange.historyOf(exchange.account(player))
        player.runClientScript(HISTORY_INIT)
        history.forEachIndexed { index, entry ->
            player.runClientScript(
                HISTORY_ADDLINE,
                index,
                entry.obj,
                entry.type.ordinal,
                entry.count,
                entry.gold,
                0L,
            )
        }
        player.runClientScript(HISTORY_FINISH)
    }

    private suspend fun ProtectedAccess.indexOp(slot: Int, comsub: Int, op: IfButtonOp) {
        val offer = exchange.book.offer(exchange.owner(player), slot)
        when (comsub) {
            BUTTON_VIEW ->
                when (op) {
                    IfButtonOp.Op1 -> if (offer != null) player.viewOffer(slot, offer.obj)
                    IfButtonOp.Op2 -> exchange.abort(player, slot)?.let(::mes)
                    else -> mes("To change an offer, abort it and place a new one.")
                }
            BUTTON_BUY -> {
                if (offer != null) return
                player.startSetup(slot, GeOfferType.Buy)
                chooseItem()
            }
            BUTTON_SELL -> {
                if (offer != null) return
                player.startSetup(slot, GeOfferType.Sell)
            }
        }
    }

    private suspend fun ProtectedAccess.setupOp(comsub: Int, op: IfButtonOp) {
        if (player.setupSlot() == null) {
            return
        }
        if (comsub == SETUP_CHOOSE_ITEM) {
            if (player.newOfferType == GeOfferType.Buy.ordinal) {
                chooseItem()
            } else {
                mes("Click on an item in your inventory to offer it.")
            }
            return
        }
        if (player.searchObj < 0) {
            return
        }
        when (comsub) {
            in SETUP_QUANTITY_DELTAS.indices -> {
                val delta = SETUP_QUANTITY_DELTAS[comsub]
                if (delta != 0 && op == IfButtonOp.Op1) player.changeQuantity(delta)
            }
            SETUP_ENTER_QUANTITY ->
                if (op == IfButtonOp.Op1) {
                    val amount = countDialog("How many do you wish to ${player.verb()}?")
                    player.newOfferQuantity = player.clampQuantity(amount)
                } else {
                    player.fillQuantityWithCoins()
                }
            SETUP_PRICE_MINUS_1 -> player.newOfferPrice = maxOf(1, player.newOfferPrice - 1)
            SETUP_PRICE_PLUS_1 -> player.newOfferPrice = player.newOfferPrice + 1
            SETUP_PRICE_MINUS_5 -> player.adjustPricePercent(-5)
            SETUP_PRICE_PLUS_5 -> player.adjustPricePercent(5)
            SETUP_PRICE_GUIDE -> player.newOfferPrice = guideOf(player.searchObj)
            SETUP_PRICE_ENTER -> {
                val price = countDialog("Set a price for each item:")
                if (price > 0) player.newOfferPrice = price.toLong()
            }
            SETUP_PRICE_CUSTOM_MINUS,
            SETUP_PRICE_CUSTOM_PLUS -> {
                val sign = if (comsub == SETUP_PRICE_CUSTOM_PLUS) 1 else -1
                if (op == IfButtonOp.Op2 || player.customPercent <= 0) {
                    val percent = countDialog("Set a percentage to change the price by:")
                    player.customPercent = percent.coerceIn(1, 100)
                    return
                }
                player.adjustPricePercent(sign * player.customPercent)
            }
        }
    }

    private fun ProtectedAccess.confirm() {
        val slot = player.setupSlot() ?: return
        val obj = player.searchObj
        if (obj < 0) {
            mes("You must choose an item first.")
            return
        }
        val count = player.newOfferQuantity
        val price = player.newOfferPrice
        val error =
            if (player.newOfferType == GeOfferType.Buy.ordinal) {
                exchange.placeBuy(player, slot, obj, count, price)
            } else {
                exchange.placeSell(player, slot, obj, count, price)
            }
        if (error != null) {
            mes(error)
            return
        }
        player.backToIndex()
    }

    private fun ProtectedAccess.detailsOp(comsub: Int, op: IfButtonOp) {
        val slot = player.selectedSlot - 1
        if (slot !in 0 until GeAccount.SLOT_COUNT || op != IfButtonOp.Op1) {
            return
        }
        when (comsub) {
            0 -> exchange.abort(player, slot)?.let(::mes)
            1 -> mes("To change an offer, abort it and place a new one.")
        }
    }

    private fun ProtectedAccess.collectOp(comsub: Int, op: IfButtonOp) {
        val slot = player.selectedSlot - 1
        if (slot !in 0 until GeAccount.SLOT_COUNT) {
            return
        }
        val offer = exchange.book.offer(exchange.owner(player), slot) ?: return
        val items = comsub == COLLECT_ITEMS
        // `script9909` swaps the item ops when one item is waiting: op1 is then the plain item.
        val noted = if (offer.collectItems == 1) op == IfButtonOp.Op2 else op == IfButtonOp.Op1
        val error =
            exchange.collect(
                player,
                slot,
                items = items,
                coins = !items,
                toBank = op == IfButtonOp.Op3,
                noted = noted,
            )
        error?.let(::mes)
        if (exchange.book.offer(exchange.owner(player), slot) == null) {
            player.backToIndex()
        }
    }

    private fun ProtectedAccess.collectAllOp(comsub: Int, op: IfButtonOp) {
        when (comsub) {
            0 -> exchange.collectAll(player, toBank = op == IfButtonOp.Op2)?.let(::mes)
            1 -> repeatLastOffer()
        }
    }

    private fun ProtectedAccess.repeatLastOffer() {
        val obj = player.vars["varp.ge_last_offer_item"]
        if (obj <= 0) {
            return
        }
        val account = exchange.account(player)
        val slot = (0 until GeAccount.SLOT_COUNT).firstOrNull { account.slots[it] == null }
        if (slot == null) {
            mes("You have no free slots.")
            return
        }
        val type = player.vars["varp.ge_last_offer_type"]
        player.startSetup(slot, GeOfferType.entries[type.coerceIn(0, 1)])
        player.selectObj(obj)
        player.newOfferQuantity = player.vars["varp.ge_last_offer_quantity"].coerceAtLeast(1)
        player.newOfferPrice = player.vars["varp.ge_last_offer_price"].toLong().coerceAtLeast(1)
    }

    private fun ProtectedAccess.offerFromInventory(invSlot: Int, op: IfButtonOp) {
        if (op != IfButtonOp.Op1) {
            return
        }
        val held = player.inv[invSlot] ?: return
        val type = ServerCacheManager.getItem(held.id) ?: return
        val item = uncert(type)
        if (!GrandExchange.tradeable(item)) {
            mes("You can't trade that item on the Grand Exchange.")
            return
        }
        var slot = player.setupSlot()
        if (slot == null || player.newOfferType != GeOfferType.Sell.ordinal) {
            val account = exchange.account(player)
            slot =
                if (slot != null && account.slots[slot] == null) {
                    slot
                } else {
                    (0 until GeAccount.SLOT_COUNT).firstOrNull { account.slots[it] == null }
                }
            if (slot == null) {
                mes("You have no free slots.")
                return
            }
            player.startSetup(slot, GeOfferType.Sell)
        }
        player.selectObj(item.id)
        player.newOfferQuantity = player.available(item.id)
    }

    private suspend fun ProtectedAccess.chooseItem() {
        val type = objDialog("What would you like to buy?")
        if (player.setupSlot() == null) {
            return
        }
        if (!GrandExchange.tradeable(type)) {
            mes("You can't buy that item on the Grand Exchange.")
            return
        }
        player.selectObj(type.id)
        player.newOfferQuantity = 1
    }

    private fun Player.startSetup(slot: Int, type: GeOfferType) {
        resetSetup()
        selectedSlot = slot + 1
        newOfferType = type.ordinal
    }

    private fun Player.resetSetup() {
        searchObj = -1
        newOfferQuantity = 0
        newOfferPrice = 0
        ifSetText("component.ge_offers:setup_desc", "")
        ifSetText("component.ge_offers:setup_marketprice", "")
    }

    private fun Player.viewOffer(slot: Int, obj: Int) {
        selectedSlot = slot + 1
        val type = ServerCacheManager.getItem(obj) ?: return
        ifSetText("component.ge_offers:details_desc", type.examine)
        ifSetText("component.ge_offers:details_marketprice", guideText(obj))
    }

    private fun guideText(obj: Int): String {
        val guide = exchange.guidePrice(obj) ?: return "No guide price."
        return "Guide price: ${guide.formatAmount} coins"
    }

    private fun Player.backToIndex() {
        resetSetup()
        selectedSlot = 0
    }

    private fun Player.setupSlot(): Int? {
        val slot = selectedSlot - 1
        if (slot !in 0 until GeAccount.SLOT_COUNT) return null
        if (exchange.book.offer(exchange.owner(this), slot) != null) return null
        return slot
    }

    private fun Player.selectObj(obj: Int) {
        val type = ServerCacheManager.getItem(obj) ?: return
        searchObj = obj
        newOfferPrice = guideOf(obj)
        ifSetText("component.ge_offers:setup_desc", type.examine)
        ifSetText("component.ge_offers:setup_marketprice", guideText(obj))
    }

    private fun guideOf(obj: Int): Long {
        val guide = exchange.guidePrice(obj)
        if (guide != null && guide > 0) return guide.coerceAtMost(GrandExchange.MAX_PRICE)
        return ServerCacheManager.getItem(obj)?.cost?.toLong()?.coerceAtLeast(1) ?: 1
    }

    private fun Player.verb(): String =
        if (newOfferType == GeOfferType.Buy.ordinal) "buy" else "sell"

    private fun Player.available(obj: Int): Int {
        val type = ServerCacheManager.getItem(obj) ?: return 1
        val noted = GrandExchange.notedOf(type)?.let { inv.held(it) } ?: 0
        val total = inv.held(obj).toLong() + noted
        return total.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun Player.clampQuantity(amount: Int): Int {
        val max = if (newOfferType == GeOfferType.Sell.ordinal) available(searchObj) else Int.MAX_VALUE
        return amount.coerceIn(1, max)
    }

    /** Mirrors the client's `ge_offers_setup_changequantity`, which already redrew the value. */
    private fun Player.changeQuantity(requested: Int) {
        var delta = requested
        val selling = newOfferType == GeOfferType.Sell.ordinal
        val max = if (selling) available(searchObj) else Int.MAX_VALUE
        if (delta == Int.MAX_VALUE) {
            if (selling) {
                newOfferQuantity = max
                return
            }
            delta = 1000
        }
        val current = newOfferQuantity
        newOfferQuantity =
            if (delta > 0) {
                if (delta > 1 && current == 1) delta -= 1
                if (max - delta < current) max else current + delta
            } else if (current <= -delta) {
                1
            } else {
                current + delta
            }
    }

    private fun Player.fillQuantityWithCoins() {
        if (newOfferType != GeOfferType.Buy.ordinal || newOfferPrice <= 0) {
            return
        }
        val coins = inv.held(GrandExchange.COINS).toLong()
        newOfferQuantity = (coins / newOfferPrice).coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun Player.adjustPricePercent(percent: Int) {
        val current = newOfferPrice
        val step = maxOf(1L, current * kotlin.math.abs(percent) / 100)
        newOfferPrice =
            if (percent > 0) {
                minOf(GrandExchange.MAX_PRICE, current + step)
            } else if (step >= current) {
                1
            } else {
                current - step
            }
    }

    private companion object {
        val CLERKS = listOf("npc.ge_clerk_1", "npc.ge_clerk_2", "npc.ge_clerk_3", "npc.ge_clerk_4")
        const val BOOTH = "loc.exchange_bank_wall_exchange"

        val NEW_OFFER_PRICE_VARP: VarpServerType by lazy {
            checkNotNull(ServerCacheManager.getVarp("varp.ge_newoffer_price".asRSCM(RSCMType.VARP)))
        }
        val HISTORY_INIT: Int by lazy {
            "clientscript.[clientscript,ge_history_init]".asRSCM(RSCMType.CLIENTSCRIPT)
        }
        val HISTORY_ADDLINE: Int by lazy {
            "clientscript.[clientscript,ge_history_addline]".asRSCM(RSCMType.CLIENTSCRIPT)
        }
        val HISTORY_FINISH: Int by lazy {
            "clientscript.[clientscript,ge_history_finish]".asRSCM(RSCMType.CLIENTSCRIPT)
        }

        const val BUTTON_VIEW = 2
        const val BUTTON_BUY = 3
        const val BUTTON_SELL = 4

        const val SETUP_CHOOSE_ITEM = 0
        val SETUP_QUANTITY_DELTAS = intArrayOf(0, -1, 1, 1, 10, 100, Int.MAX_VALUE)
        const val SETUP_ENTER_QUANTITY = 7
        const val SETUP_PRICE_MINUS_1 = 8
        const val SETUP_PRICE_PLUS_1 = 9
        const val SETUP_PRICE_MINUS_5 = 10
        const val SETUP_PRICE_GUIDE = 11
        const val SETUP_PRICE_ENTER = 12
        const val SETUP_PRICE_PLUS_5 = 13
        const val SETUP_PRICE_CUSTOM_MINUS = 14
        const val SETUP_PRICE_CUSTOM_PLUS = 15

        const val COLLECT_ITEMS = 2
    }
}
