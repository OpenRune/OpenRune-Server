package org.alter.game.pluginnew

import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.shop.PurchasePolicy
import org.alter.game.model.shop.Shop
import org.alter.game.model.shop.ShopCurrency
import org.alter.game.model.shop.StockType
import org.alter.game.pluginnew.event.Event
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.EventManager
import org.alter.rscm.RSCM.getRSCM
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript

abstract class PluginEvent  {

    lateinit var world : World

    var settings: PluginSettings? = null
        internal set

    inline fun <reified T : PluginSettings> getSetting(): T {
        return settings as? T
            ?: throw IllegalStateException(
                "Settings not assigned or wrong type for ${this::class.simpleName}, expected ${T::class.simpleName}"
            )
    }

    open fun isEnabled() = settings?.isEnabled?: true

    abstract fun init()

    inline fun <reified K : Event> on(config: EventListener<K>.() -> EventListener<K>): EventListener<K> {
        return config.invoke(EventListener(K::class)).submit()
    }

    inline fun <reified K : Event> onEvent(noinline action: suspend K.() -> Unit): EventListener<K> {
        val listener = EventListener(K::class)
        listener.action = action
        return listener.submit()
    }

    fun <K : Event> on(type: KClass<K>, config: EventListener<K>.() -> EventListener<K>): EventListener<K> {
        return config.invoke(EventListener(type)).submit()
    }
    
    fun <K : Event> onEvent(type: KClass<K>, action: suspend K.() -> Unit): EventListener<K> {
        val listener = EventListener(type)
        listener.action = action
        return listener.submit()
    }

    fun <K : Event> addFilter(type: KClass<K>, filter: K.() -> Boolean) {
        EventManager.addFilter<K>(type.java, filter)
    }

    /**
     * Spawn an [Npc] on the given coordinates.
     */
    fun spawnNpc(
        npc: String,
        x: Int,
        z: Int,
        height: Int = 0,
        walkRadius: Int = 0,
        direction: Direction = Direction.SOUTH,
        active: Boolean = true,
    ) = spawnNpc(npc, Tile(x, z, height), walkRadius, direction, active)


    /**
     * Spawn an [Npc] on the given [tile].
     */
    fun spawnNpc(
        npc: String,
        tile: Tile,
        walkRadius: Int = 0,
        direction: Direction = Direction.SOUTH,
        active: Boolean = true,
    ) {
        val n = Npc(getRSCM(npc), tile, world)
        n.respawns = true
        n.walkRadius = walkRadius
        n.lastFacingDirection = direction
        n.setActive(active)
        world.spawn(n)
    }

    /**
     * Spawn a [GroundItem] on the given coordinates.
     */
    fun spawnItem(
        item: String,
        amount: Int,
        x: Int,
        z: Int,
        height: Int = 0,
        respawnCycles: Int = GroundItem.DEFAULT_RESPAWN_CYCLES,
    ) {
        val ground = GroundItem(getRSCM(item), amount, Tile(x, z, height))
        ground.respawnCycles = respawnCycles
        world.spawn(ground)
    }

    /**
     * Spawn a [GroundItem] on the given coordinates.
     */
    fun spawnItem(
        item: String,
        amount: Int,
        tile: Tile,
        respawnCycles: Int = GroundItem.DEFAULT_RESPAWN_CYCLES,
    ) {
        val ground = GroundItem(getRSCM(item), amount, tile)
        ground.respawnCycles = respawnCycles
        world.spawn(ground)
    }

    /**
     * Spawn a [GroundItem] on the given coordinates for player
     */
    fun spawnItem(
        item: String,
        amount: Int,
        tile: Tile,
        owner: Player,
    ) {
        val ground = GroundItem(getRSCM(item), amount, tile)
        ground.ownerUID = owner.uid
        world.spawn(ground)
    }

    /**
     * Create a [Shop] in our world.
     */
    fun createShop(
        name: String,
        currency: ShopCurrency,
        stockType: StockType = StockType.NORMAL,
        stockSize: Int = Shop.DEFAULT_STOCK_SIZE,
        purchasePolicy: PurchasePolicy = PurchasePolicy.BUY_TRADEABLES,
        init: Shop.() -> Unit,
    ) {
        val shop = Shop(name, stockType, purchasePolicy, currency, arrayOfNulls(stockSize))
        world.shops[name] = shop
        init(shop)
    }

}