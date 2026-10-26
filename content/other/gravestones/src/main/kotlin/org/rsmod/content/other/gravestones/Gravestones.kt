package org.rsmod.content.other.gravestones

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.death.DeathGrave
import org.rsmod.api.death.PlayerGraveHook
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.SupplyItems
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.vars.typeCoordVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.map.collision.get
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

private const val COORD_VARP = "varp.gravestone_coord"
private const val NPC_VARP = "varp.gravestone_npc"

val Player.grave: Inventory
    get() = invMap.getOrPut("inv.gravestone")

val Player.deathOffice: Inventory
    get() = invMap.getOrPut("inv.death_permanent")

var Player.graveCoords: CoordGrid?
    get() = vars[COORD_VARP].takeIf { it != 0 && it != CoordGrid.NULL.packed }?.let(::CoordGrid)
    set(value) = VarPlayerIntMapSetter.set(this, COORD_VARP, value?.packed ?: 0)
var Player.graveDuration by intVarBit("varbit.gravestone_duration")
var Player.feePaid by boolVarBit("varbit.gravestone_fee_paid")

/**
 * Set from the first death that loses items until the player leaves Death's Office through the
 * portal; the grave's timer waits until then.
 */
var Player.inDeathTutorial by boolVarBit("varbit.gravestone_death_tutorial")

/**
 * Whether the player has met Death. Live sets it on the tutorial's arrival, not on walking into the
 * office. UNVERIFIED: that live keys the first-death tutorial on this rather than on a server-only
 * var.
 */
var Player.metDeath by boolVarBit("varbit.steam_visited_death")

/** The map clock of the player's last input; 0 before any since login. */
private var Player.currentHintArrow by intVarBit("varbit.current_hint_arrow")

internal var Player.lastInput by intVarp("varp.gravestone_last_input")

private fun Player.isIdle(): Boolean =
    lastInput != 0 && currentMapClock - lastInput > Gravestones.IDLE_CYCLES

internal val Player.autoEquip by boolVarBit("varbit.settings_gravestone_autoequip")
private var Player.graveVisible by intVarBit("varbit.gravestone_visible")
var Player.graveType by intVarBit("varbit.gravestone_type")
internal var Player.graveNpc: NpcUid?
    get() = vars[NPC_VARP].takeIf { it != 0 && it != NpcUid.NULL.packed }?.let(::NpcUid)
    set(value) = VarPlayerIntMapSetter.set(this, NPC_VARP, value?.packed ?: 0)
private var Player.creationDeathCoord by typeCoordVarp("varp.gravestone_creation_death_coord")
private var Player.hasMerged by boolVarBit("varbit.gravestone_has_merged")
private var Player.feeUnder100k by intVarBit("varbit.gravestone_feethreshold_under100k")
private var Player.feeUnder1m by intVarBit("varbit.gravestone_feethreshold_under1m")
private var Player.feeUnder10m by intVarBit("varbit.gravestone_feethreshold_under10m")

/**
 * A grave is an npc of type `gravestone_<style>_<n>`, a multinpc on `gravestone_visible` that is
 * only the visible `Grave` to a player whose varbit is `n`, so each live grave gets its own `n` and
 * only its owner sees it.
 */
@Singleton
class Gravestones
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val npcList: NpcList,
    private val worldQueues: WorldQueueList,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val marketPrices: MarketPrices,
    private val eventBus: EventBus,
) : PlayerGraveHook {
    private val visibleSlots = HashMap<Int, Npc>()

    // TODO: A PvP world's grave can only be looted on one, and a grave on Entrana moves to the new
    // death tile like a Wilderness PvM death's.
    override fun store(
        player: Player,
        deathCoords: CoordGrid,
        grave: DeathGrave,
        items: List<InvObj>,
    ): List<InvObj> {
        val existing = player.graveCoords != null
        if (!existing && items.isEmpty()) {
            return items
        }
        close(player)
        if (existing) {
            prune(player)
            // "If a grave only contains resources and supplies, and dying sends nothing new to the
            // grave, then the grave disappears".
            if (items.isEmpty()) {
                if (player.grave.isEmpty()) {
                    remove(player)
                }
                return items
            }
        }
        // UNVERIFIED: live sends what doesn't fit to Death's Office; this drops it on the floor.
        val unheld = storeInGrave(player.grave, items, ::value)
        // UNVERIFIED: a second death replaces the layout remembered from the first.
        GraveLayout.record(player)
        player.graveDuration = DURATION
        player.feePaid = false
        if (existing && grave.relocate) {
            val worldCoords = grave.coords ?: deathCoords
            player.creationDeathCoord = worldCoords
            player.graveCoords = graveTile(worldCoords)
            spawn(player)
        }
        if (existing) {
            player.hasMerged = true
            // After "Oh dear, you are dead!". UNVERIFIED: that it comes a tick later, like the
            // new-grave message.
            worldQueues.add(1) { player.mes(MERGED_MESSAGE) }
        } else {
            val worldCoords = grave.coords ?: deathCoords
            player.creationDeathCoord = worldCoords
            player.graveCoords = graveTile(worldCoords)
            player.hasMerged = false
            if (!player.metDeath) {
                // 1500, then 1501 at once, which it stays at until the player leaves Death's
                // Office.
                player.inDeathTutorial = true
                player.graveDuration = DURATION + 1
            }
            spawn(player)
            worldQueues.add(1) { player.mes(CREATED_MESSAGE) }
        }
        player.softTimer(TIMER, TIMER_INTERVAL)
        return unheld
    }

    /**
     * Before a death adds to an existing grave, the grave sends its resources and any unstackable
     * past [MAX_UNSTACKABLE] to Death's Office and drops its food and potions beneath it, so a
     * grave can't stockpile them.
     */
    private fun prune(player: Player) {
        val grave = player.grave
        val coords = player.graveCoords ?: return
        val toOffice = mutableListOf<InvObj>()
        for (slot in grave.indices) {
            val obj = grave[slot] ?: continue
            val type = getInvObj(obj)
            when {
                SupplyItems.isFoodOrPotion(type) -> {
                    grave[slot] = null
                    // The pile "will despawn after one hour". UNVERIFIED: that only the owner sees
                    // it, as with their other death drops.
                    objRepo.add(Obj.fromOwner(player, coords, obj), SUPPLY_DURATION)
                }
                GraveResources.isResource(type) -> {
                    grave[slot] = null
                    toOffice += obj
                }
                !type.isStackable && obj.count > MAX_UNSTACKABLE -> {
                    grave[slot] = obj.copy(count = MAX_UNSTACKABLE)
                    toOffice += obj.copy(count = obj.count - MAX_UNSTACKABLE)
                }
            }
        }
        storeInGrave(grave, emptyList(), ::value)
        if (toOffice.isNotEmpty()) {
            sendToOffice(player, toOffice)
            // Before "Oh dear, you are dead!".
            player.mes(PRUNED_MESSAGE)
        }
    }

    /**
     * The grave collapses and its contents go to Death's Office, as when the timer runs out or
     * Death collects them early.
     */
    fun expire(player: Player) {
        if (player.graveCoords == null) {
            return
        }
        close(player)
        player.mes(EXPIRED_MESSAGE)
        val items = player.grave.filterNotNull { true }
        player.grave.fillNulls()
        sendToOffice(player, items)
        remove(player)
    }

    /**
     * Death's Office holds 120 slots; "items from an expired gravestone [are] deleted when the
     * limit is reached".
     */
    private fun sendToOffice(player: Player, items: List<InvObj>) {
        // UNVERIFIED: that the office sorts by value like the grave, and which items go first
        // when it's full (the least valuable are deleted here).
        storeInGrave(player.deathOffice, items, ::value)
    }

    fun spawn(player: Player) {
        val coords = player.graveCoords ?: return
        despawn(player)
        val slot = allocateSlot()
        val style = if (player.graveType == 0) "default" else "angel"
        val npc = Npc("npc.gravestone_${style}_%03d".format(slot), coords)
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        // The cache leaves the grave types on the default wander mode.
        npc.noneMode()
        visibleSlots[slot] = npc
        player.graveVisible = slot
        player.graveNpc = npc.uid
    }

    fun despawn(player: Player) {
        val npc = player.graveNpc?.resolve(npcList)
        player.graveNpc = null
        if (npc == null) {
            return
        }
        visibleSlots.values.remove(npc)
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    fun close(player: Player) {
        player.ifCloseOverlay(INTERFACE, eventBus)
    }

    /** Removes the emptied grave. Live clears the vars a tick after the last take. */
    fun remove(player: Player) {
        despawn(player)
        player.clearSoftTimer(TIMER)
        player.graveDuration = 0
        player.graveVisible = 0
        player.graveCoords = null
        player.feePaid = false
        player.hasMerged = false
        GraveLayout.clear(player)
    }

    fun tick(player: Player) {
        if (player.graveCoords == null) {
            player.clearSoftTimer(TIMER)
            return
        }
        pointAtGrave(player)
        if (player.inDeathTutorial) {
            return
        }
        if (player.ui.containsOverlay(INTERFACE) || player.isIdle()) {
            return
        }
        player.graveDuration = (player.graveDuration - TIMER_INTERVAL).coerceAtLeast(0)
        if (player.graveDuration == 0) {
            expire(player)
        }
    }

    /** The hint arrow is resent every timer tick while the player is near the grave. */
    private fun pointAtGrave(player: Player) {
        val coords = player.graveCoords ?: return
        val npc = player.graveNpc?.resolve(npcList) ?: return
        if (!player.isWithinDistance(coords, HINT_RANGE)) {
            return
        }
        player.currentHintArrow = 0
        MiscOutput.hintArrowReset(player)
        player.currentHintArrow = HINT_ARROW_NPC
        MiscOutput.hintArrowNpc(player, npc.slotId)
    }

    /** Starts the countdown now, on the tick the first-death tutorial ends. */
    fun startCountdown(player: Player) {
        player.inDeathTutorial = false
        player.graveDuration = (player.graveDuration - TIMER_INTERVAL).coerceAtLeast(0)
        player.softTimer(TIMER, TIMER_INTERVAL)
    }

    fun value(obj: InvObj): Long {
        val type = getInvObj(obj)
        // UNVERIFIED: the price graves band by; this is the one deaths use to pick kept items.
        return (marketPrices[type] ?: type.cost).toLong().coerceAtLeast(1)
    }

    fun isPaySlot(player: Player, slot: Int): Boolean {
        val obj = player.grave[slot] ?: return false
        return GraveFees.isPaid(value(obj))
    }

    fun fee(player: Player): Int {
        if (player.feePaid) {
            return 0
        }
        val slots = player.grave.filterNotNull { true }.map { value(it) to it.count }
        return GraveFees.totalFee(slots, player.isAnyIronman)
    }

    // TODO: Live also discounts deaths at some bosses until a kill count (`script1797`).
    fun sendFeeThresholds(player: Player) {
        val values = player.grave.map { obj -> obj?.let(::value) }
        val (under100k, under1m, under10m) = GraveFees.thresholds(values)
        player.feeUnder100k = under100k
        player.feeUnder1m = under1m
        // UNVERIFIED: live left under10m at 0 with nothing above 1m; the client draws the same.
        player.feeUnder10m = under10m
    }

    private fun allocateSlot(): Int {
        val free = (1..MAX_VISIBLE_SLOT).filter { it !in visibleSlots }
        // UNVERIFIED: how live picks the slot (247 and 125 in our captures).
        return random.pickOrNull(free) ?: random.of(1, MAX_VISIBLE_SLOT)
    }

    /**
     * The death tile, or a free tile near it when something stands there. Live put the grave of a
     * death on Brutus' gate tile (3262, 3294) at (3264, 3291).
     */
    private fun graveTile(coords: CoordGrid): CoordGrid {
        if (collision[coords] == 0) {
            return coords
        }
        // UNVERIFIED: the search; a random free tile in the nearest ring that has one.
        for (radius in 1..GRAVE_TILE_RADIUS) {
            val ring =
                (-radius..radius).flatMap { dx ->
                    (-radius..radius).mapNotNull { dz ->
                        if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) != radius) {
                            null
                        } else {
                            coords.translate(dx, dz)
                        }
                    }
                }
            val free = ring.filter { collision[it] == 0 }
            if (free.isNotEmpty()) {
                return random.pick(free)
            }
        }
        return coords
    }

    companion object {
        const val INTERFACE = "interface.gravestone_generic"
        const val TIMER = "timer.gravestone"
        const val TIMER_INTERVAL = 3

        const val DURATION = 1500

        private const val MAX_VISIBLE_SLOT = 255
        private const val GRAVE_TILE_RADIUS = 3

        private const val CREATED_MESSAGE =
            "Some of your dropped items are being held in a gravestone, near where you died."
        private const val MERGED_MESSAGE =
            "Some of your items have been added to your previous gravestone."
        private const val PRUNED_MESSAGE =
            "Some items have been moved to Death's Office from your gravestone."

        private const val EXPIRED_MESSAGE =
            "<col=ef1020>Your gravestone has expired.</col> Items held there can now be reclaimed " +
                "at <col=ef1020>Death's Office</col>, accessed from near the respawn points."

        // Unstackables "cannot be stockpiled up to 56".
        const val MAX_UNSTACKABLE = 28

        // Live paused 17 ticks after a click, but kept counting 17 after a key press. UNVERIFIED:
        // 16 or 17.
        const val IDLE_CYCLES = 16

        // UNVERIFIED: live showed the arrow 19 tiles from the grave but not 24.
        private const val HINT_RANGE = 20
        private const val HINT_ARROW_NPC = 9
        private const val SUPPLY_DURATION = 6000
    }
}
