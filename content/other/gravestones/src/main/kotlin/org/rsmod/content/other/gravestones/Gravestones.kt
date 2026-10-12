package org.rsmod.content.other.gravestones

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.death.DeathGrave
import org.rsmod.api.death.PlayerGraveHook
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.typeCoordVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.map.collision.get
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

private const val COORD_VARP = "varp.gravestone_coord"
private const val NPC_VARP = "varp.gravestone_npc"

val Player.grave: Inventory
    get() = invMap.getOrPut("inv.gravestone")

var Player.graveCoords: CoordGrid?
    get() = vars[COORD_VARP].takeIf { it != 0 && it != CoordGrid.NULL.packed }?.let(::CoordGrid)
    set(value) = VarPlayerIntMapSetter.set(this, COORD_VARP, value?.packed ?: 0)
var Player.graveDuration by intVarBit("varbit.gravestone_duration")
var Player.feePaid by boolVarBit("varbit.gravestone_fee_paid")
internal val Player.autoEquip by boolVarBit("varbit.settings_gravestone_autoequip")
private var Player.graveVisible by intVarBit("varbit.gravestone_visible")
private val Player.graveType by intVarBit("varbit.gravestone_type")
internal var Player.graveNpc: NpcUid?
    get() = vars[NPC_VARP].takeIf { it != 0 && it != NpcUid.NULL.packed }?.let(::NpcUid)
    set(value) = VarPlayerIntMapSetter.set(this, NPC_VARP, value?.packed ?: 0)
private var Player.creationDeathCoord by typeCoordVarp("varp.gravestone_creation_death_coord")
private var Player.hasMerged by boolVarBit("varbit.gravestone_has_merged")
private var Player.feeUnder100k by intVarBit("varbit.gravestone_feethreshold_under100k")
private var Player.feeUnder1m by intVarBit("varbit.gravestone_feethreshold_under1m")
private var Player.feeUnder10m by intVarBit("varbit.gravestone_feethreshold_under10m")

/**
 * Graves (https://oldschool.runescape.wiki/w/Grave). A grave is an npc of type
 * `gravestone_<style>_<n>`, a multinpc on `gravestone_visible` that is only the visible `Grave` to
 * a player whose varbit is `n`, so each live grave gets its own `n` and only its owner sees it.
 */
@Singleton
class Gravestones
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcList: NpcList,
    private val worldQueues: WorldQueueList,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val marketPrices: MarketPrices,
    private val eventBus: EventBus,
) : PlayerGraveHook {
    private val visibleSlots = HashMap<Int, Npc>()

    // TODO: Graves only start after the first visit to Death's Office, a PvP world's grave can
    // only be looted on one, and a grave on Entrana moves to the new death tile
    // (https://oldschool.runescape.wiki/w/Grave).
    // TODO: On a second death live first sends unstackables past 28 and resources to Death's
    // Office and drops food and potions under the grave (https://oldschool.runescape.wiki/w/Grave).
    override fun store(
        player: Player,
        deathCoords: CoordGrid,
        grave: DeathGrave,
        items: List<InvObj>,
    ): List<InvObj> {
        close(player)
        val existing = player.graveCoords != null
        // UNVERIFIED: live sends what doesn't fit to Death's Office; this drops it on the floor.
        val unheld = storeInGrave(player.grave, items, ::value)
        // UNVERIFIED: a second death replaces the layout remembered from the first.
        GraveLayout.record(player)
        player.graveDuration = DURATION
        player.feePaid = false
        if (existing) {
            player.hasMerged = true
            // https://oldschool.runescape.wiki/w/Grave lists it after "Oh dear, you are dead!".
            // UNVERIFIED: that it comes a tick later, like the new-grave message.
            worldQueues.add(1) { player.mes(MERGED_MESSAGE) }
        } else {
            val worldCoords = grave.coords ?: deathCoords
            player.creationDeathCoord = worldCoords
            player.graveCoords = graveTile(worldCoords)
            player.hasMerged = false
            spawn(player)
            // live capture, rev 241, 20261012T233700-df49f48@960: a tick after the death message.
            worldQueues.add(1) { player.mes(CREATED_MESSAGE) }
        }
        player.softTimer(TIMER, TIMER_INTERVAL)
        return unheld
    }

    fun spawn(player: Player) {
        val coords = player.graveCoords ?: return
        despawn(player)
        val slot = allocateSlot()
        // UNVERIFIED: that any gravestone_type but 0 is the angel bought at Death's Office.
        val style = if (player.graveType == 0) "default" else "angel"
        val npc = Npc("npc.gravestone_${style}_%03d".format(slot), coords)
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        // The cache leaves the grave types on the default wander mode.
        npc.noneMode()
        visibleSlots[slot] = npc
        player.graveVisible = slot
        player.graveNpc = npc.uid
        // live capture, rev 241, 20261012T233700-df49f48@1018: a hint arrow on the grave.
        MiscOutput.hintArrowNpc(player, npc.slotId)
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

    /** Removes the emptied grave. Live clears the vars a tick after the last take (@1043). */
    fun remove(player: Player) {
        despawn(player)
        MiscOutput.hintArrowReset(player)
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
        // TODO: The timer also stops while the player is idle for more than 10 seconds
        // (https://oldschool.runescape.wiki/w/Grave).
        if (player.ui.containsOverlay(INTERFACE)) {
            return
        }
        // live capture, rev 241, 20261012T233700-df49f48@962: 3 off every 3 ticks.
        player.graveDuration = (player.graveDuration - TIMER_INTERVAL).coerceAtLeast(0)
        if (player.graveDuration == 0) {
            // TODO: Live sends the items to Death's Office with "Your gravestone has expired."
            // (live capture, rev 240, 20260811T152500-59bcb26@24). Until the office exists the
            // grave stays, so nothing is lost.
            player.clearSoftTimer(TIMER)
        }
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

    // TODO: Live also discounts deaths at some bosses until a kill count
    // (`script1797`, https://oldschool.runescape.wiki/w/Grave#Discounted_deaths).
    fun sendFeeThresholds(player: Player) {
        val values = player.grave.map { obj -> obj?.let(::value) }
        val (under100k, under1m, under10m) = GraveFees.thresholds(values)
        // live capture, rev 241, 20261012T233700-df49f48@1030: under100k=7, under1m=1.
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
     * death on Brutus' gate tile (3262, 3294) at (3264, 3291) (live capture, rev 241,
     * 20261012T233700-df49f48@959, @1018).
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

        // live capture, rev 241, 20261012T233700-df49f48@959: gravestone_duration=1500.
        const val DURATION = 1500

        private const val MAX_VISIBLE_SLOT = 255
        private const val GRAVE_TILE_RADIUS = 3

        private const val CREATED_MESSAGE =
            "Some of your dropped items are being held in a gravestone, near where you died."
        private const val MERGED_MESSAGE =
            "Some of your items have been added to your previous gravestone."
    }
}
