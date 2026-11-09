package org.rsmod.content.other.bots

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.annotations.InternalApi
import org.rsmod.api.game.process.player.PlayerBuildAreaProcessor
import org.rsmod.api.game.process.player.PlayerFaceSquareProcessor
import org.rsmod.api.game.process.player.PlayerMovementProcessor
import org.rsmod.api.net.rsprot.HeadlessAvatars
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.BotOutfitsRow
import org.rsmod.api.utils.map.BuildAreaUtils
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Owns the cosmetic bots. A bot is a [Player] kept out of the [PlayerList] - only its slot is
 * reserved there - so no content, NPC or player-count code can see it; real clients still render it
 * through a headless protocol avatar.
 */
@Singleton
class BotWorld
@Inject
constructor(
    private val playerList: PlayerList,
    private val avatars: HeadlessAvatars,
    private val brain: BotBrain,
    private val movement: PlayerMovementProcessor,
    private val mapClock: MapClock,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
) {
    private val bots = ArrayList<Bot>()
    private val facing = PlayerFaceSquareProcessor()
    private val buildAreas = PlayerBuildAreaProcessor()

    val size: Int
        get() = bots.size

    fun spawn(
        activity: BotActivity,
        home: CoordGrid,
        radius: Int,
        styles: List<String>,
        bank: CoordGrid? = null,
        hotspot: Int = MANUAL_HOTSPOT,
    ): Boolean {
        if (bots.size >= MAX_BOTS) {
            return false
        }
        val slot = playerList.reserveHighestFree() ?: return false
        val style = random.pickOrNull(styles) ?: DEFAULT_STYLE
        val spot = walkableNear(home, radius)
        val player = createPlayer(slot, activity, spot, style)
        avatars.attach(player)
        val runs = style == RUNNER_STYLE || bank != null
        val bot = Bot(player, activity, home, radius, spot, bank, runs, hotspot)
        bot.phaseTicks = random.of(1, SPAWN_STAGGER_TICKS)
        bots += bot
        return true
    }

    fun despawnAll() {
        for (bot in bots) {
            release(bot)
        }
        bots.clear()
    }

    fun despawn(hotspot: Int) {
        bots.removeIf { bot ->
            if (bot.hotspot != hotspot) {
                return@removeIf false
            }
            release(bot)
            true
        }
    }

    fun isAnyNear(hotspot: Int, coords: CoordGrid, distance: Int): Boolean =
        bots.any { it.hotspot == hotspot && it.player.coords.chebyshevDistance(coords) <= distance }

    private fun walkableNear(home: CoordGrid, radius: Int): CoordGrid {
        repeat(SPOT_ATTEMPTS) {
            val candidate = random.of(home, radius)
            if (collision[candidate.x, candidate.z, candidate.level] and BLOCKED_FLAGS == 0) {
                return candidate
            }
        }
        return home
    }

    fun tick() {
        val iterator = bots.iterator()
        while (iterator.hasNext()) {
            val bot = iterator.next()
            try {
                process(bot)
            } catch (e: Exception) {
                logger.error(e) { "Removing bot after tick failure: ${bot.player.displayName}" }
                release(bot)
                iterator.remove()
            }
        }
    }

    fun postTick() {
        for (bot in bots) {
            val player = bot.player
            player.clientCycle.flush(player)
            player.clearPendingUpdates()
        }
    }

    private fun process(bot: Bot) {
        val player = bot.player
        player.currentMapClock = mapClock.cycle
        player.processedMapClock = mapClock.cycle
        player.previousCoords = player.coords
        brain.tick(bot)
        movement.process(player)
        facing.process(player)
        buildAreas.process(player)
        player.clientCycle.update(player)
    }

    @OptIn(InternalApi::class)
    private fun createPlayer(
        slot: Int,
        activity: BotActivity,
        home: CoordGrid,
        style: String,
    ): Player {
        val player = Player()
        val name = BotLooks.name(random, bots.mapTo(HashSet()) { it.player.username })
        player.slotId = slot
        player.username = name.lowercase()
        player.displayName = name
        player.hidden = true
        player.coords = home
        player.currentMapClock = mapClock.cycle
        player.worn = Inventory(wornType, arrayOfNulls(WORN_SIZE))
        BotLooks.randomise(player.appearance, random)
        dress(player, style)
        toolFor(activity)?.let { player.worn[Wearpos.RightHand.slot] = InvObj(it) }
        player.buildArea = BuildAreaUtils.calculateBuildArea(ZoneKey.from(home))
        return player
    }

    private fun dress(player: Player, style: String) {
        val outfit = random.pickOrNull(BotOutfitsRow.all().filter { it.style == style })
        if (outfit == null) {
            player.appearance.combatLevel = random.of(FALLBACK_COMBAT)
            return
        }
        player.appearance.combatLevel = random.of(outfit.minCombat, outfit.maxCombat)
        for (item in outfit.items) {
            val wearpos = Wearpos[item.wearpos1] ?: continue
            player.worn[wearpos.slot] = InvObj(item)
        }
    }

    private fun toolFor(activity: BotActivity): String? =
        when (activity) {
            BotActivity.WOODCUTTING -> random.pick(AXES)
            BotActivity.MINING -> random.pick(PICKAXES)
            else -> null
        }

    private fun release(bot: Bot) {
        val player = bot.player
        brain.forget(bot)
        avatars.detach(player)
        playerList.unreserve(player.slotId)
        player.destroy()
    }

    private fun Player.clearPendingUpdates() {
        pendingSay = null
        pendingStepCount = 0
        pendingTeleport = false
        pendingTelejump = false
        pendingFaceAngle = EntityFaceAngle.NULL
        pendingSequence = EntitySeq.NULL
        appearance.clearRebuildFlag()
    }

    private companion object {
        private val logger = InlineLogger()

        private val wornType by lazy {
            checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM()))
        }

        private val AXES =
            listOf(
                "obj.bronze_axe",
                "obj.iron_axe",
                "obj.steel_axe",
                "obj.mithril_axe",
                "obj.adamant_axe",
                "obj.rune_axe",
            )

        private val PICKAXES =
            listOf(
                "obj.bronze_pickaxe",
                "obj.iron_pickaxe",
                "obj.steel_pickaxe",
                "obj.mithril_pickaxe",
                "obj.adamant_pickaxe",
                "obj.rune_pickaxe",
            )

        private const val DEFAULT_STYLE = "plain"
        private const val RUNNER_STYLE = "graceful"
        private val FALLBACK_COMBAT = 3..60

        const val MANUAL_HOTSPOT = -1

        private const val SPOT_ATTEMPTS = 20
        private const val BLOCKED_FLAGS =
            CollisionFlag.LOC or
                CollisionFlag.BLOCK_WALK or
                CollisionFlag.GROUND_DECOR or
                CollisionFlag.BLOCK_PLAYERS

        private const val MAX_BOTS = 300
        private const val WORN_SIZE = 14
        private const val SPAWN_STAGGER_TICKS = 20
    }
}
