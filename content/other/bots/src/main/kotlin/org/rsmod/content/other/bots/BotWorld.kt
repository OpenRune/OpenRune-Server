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
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.ironman.setGamemode
import org.rsmod.api.player.stat.levelUpFireworks
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
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
    private val worldRepo: WorldRepository,
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
        gamemode: Int? = null,
    ): Boolean {
        if (bots.size >= MAX_BOTS) {
            return false
        }
        val mode = gamemode ?: BotNames.randomMode(random)
        val taken = HashSet<String>()
        for (player in playerList) {
            taken += player.username.lowercase()
            taken += player.displayName.lowercase()
        }
        bots.mapTo(taken) { it.player.username }
        val name = BotNames.pick(random, mode, taken) ?: return false
        val slot = playerList.reserveHighestFree() ?: return false
        val player = Player()
        try {
            val style = random.pickOrNull(styles) ?: DEFAULT_STYLE
            val spot = walkableNear(home, radius)
            preparePlayer(player, slot, activity, spot, style, name, mode)
            val runs = style == RUNNER_STYLE || bank != null
            val bot = Bot(player, activity, home, radius, spot, bank, runs, hotspot, style)
            brain.seedSkill(bot)
            avatars.attach(player)
            bot.phaseTicks = random.of(1, SPAWN_STAGGER_TICKS)
            bots += bot
            return true
        } catch (error: Exception) {
            try {
                avatars.detach(player)
            } finally {
                playerList.unreserve(slot)
                player.destroy()
            }
            throw error
        }
    }

    /**
     * Replaces bots whose displayed identity matches a newly registered player.
     *
     * The replacement takes a different slot when one is free. A full world releases the
     * old slot before retrying the spawn.
     */
    fun replaceNameCollisions(player: Player) {
        val collisions = bots.filter {
            val name = it.player.username
            name.equals(player.username, ignoreCase = true) ||
                name.equals(player.displayName, ignoreCase = true)
        }
        for (bot in collisions) {
            bots.remove(bot)
            try {
                val replaced = try {
                    spawnReplacement(bot)
                } finally {
                    release(bot)
                }
                if (!replaced) spawnReplacement(bot)
            } catch (error: Exception) {
                logger.error(error) { "Could not replace cosmetic bot ${bot.player.displayName}" }
            }
        }
    }

    private fun spawnReplacement(bot: Bot): Boolean =
        spawn(
            bot.activity, bot.home, bot.radius, listOf(bot.style), bot.bank, bot.hotspot,
            gamemode = bot.player.gamemode,
        )

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
        val newLevel = brain.tick(bot)
        movement.process(player)
        facing.process(player)
        buildAreas.process(player)
        if (newLevel != null) celebrateLevelUp(bot, newLevel)
        replyToLevelUp(bot)
        player.clientCycle.update(player)
    }

    private fun celebrateLevelUp(bot: Bot, level: Int) {
        if (level == 99) {
            bot.player.levelUpFireworks(worldRepo, "spotanim.levelup_99_anim")
        } else {
            bot.player.levelUpFireworks(worldRepo)
        }
        if (random.randomBoolean(SILENT_LEVEL_UP_CHANCE)) return

        val candidates = bots.filterTo(ArrayList()) {
            it !== bot && it.levelUpReply == null && it.isNear(bot)
        }
        var delay = random.of(REPLY_DELAY)
        repeat(random.of(1, MAX_LEVEL_UP_REPLIES)) {
            val responder = random.pickOrNull(candidates) ?: return
            candidates.remove(responder)
            responder.levelUpReply = BotLevelUpReply(bot, mapClock.cycle + delay)
            delay += random.of(REPLY_DELAY)
        }
    }

    private fun replyToLevelUp(bot: Bot) {
        val reply = bot.levelUpReply ?: return
        if (mapClock.cycle < reply.cycle) return

        bot.levelUpReply = null
        if (reply.source in bots && bot.isNear(reply.source) && bot.player.publicMessage == null) {
            BotChat.say(bot.player, random.pick(BotChat.congratulations))
        }
    }

    private fun Bot.isNear(other: Bot): Boolean {
        val coords = player.coords
        val target = other.player.coords
        return coords.level == target.level && coords.chebyshevDistance(target) <= LEVEL_UP_REPLY_RADIUS
    }

    @OptIn(InternalApi::class)
    private fun preparePlayer(
        player: Player,
        slot: Int,
        activity: BotActivity,
        home: CoordGrid,
        style: String,
        name: String,
        gamemode: Int,
    ) {
        player.slotId = slot
        player.username = name.lowercase()
        player.displayName = name
        player.setGamemode(gamemode)
        player.hidden = true
        player.coords = home
        player.currentMapClock = mapClock.cycle
        player.worn = Inventory(wornType, arrayOfNulls(WORN_SIZE))
        BotLooks.randomise(player.appearance, random)
        dress(player, style)
        toolFor(activity)?.let { player.worn[Wearpos.RightHand.slot] = InvObj(it) }
        player.buildArea = BuildAreaUtils.calculateBuildArea(ZoneKey.from(home))
    }

    private fun dress(player: Player, style: String) {
        val outfits = BotOutfitsRow.all()
        val outfit = random.pickOrNull(outfits.filter { it.style == style })
        player.appearance.combatLevel =
            if (outfit == null) random.of(FALLBACK_COMBAT)
            else random.of(outfit.minCombat, outfit.maxCombat)
        if (outfit != null) player.equipOutfit(outfit)

        if (player.isAnyIronman && random.randomBoolean(IRONMAN_OUTFIT_CHANCE)) {
            val armourStyle = when (player.gamemode) {
                PlayerGamemode.HARDCORE_IRONMAN -> "hardcore_ironman"
                PlayerGamemode.ULTIMATE_IRONMAN -> "ultimate_ironman"
                PlayerGamemode.GROUP_IRONMAN -> "group_ironman"
                PlayerGamemode.HARDCORE_GROUP_IRONMAN -> "hardcore_group_ironman"
                PlayerGamemode.UNRANKED_GROUP_IRONMAN -> "unranked_group_ironman"
                else -> "ironman"
            }
            val armour = outfits.firstOrNull { it.style == armourStyle }
            if (armour != null) player.equipOutfit(armour)
        }
    }

    private fun Player.equipOutfit(outfit: BotOutfitsRow) {
        for (item in outfit.items) {
            val wearpos = Wearpos[item.wearpos1] ?: continue
            worn[wearpos.slot] = InvObj(item)
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
        pendingSpotanims.clear()
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
        private const val IRONMAN_OUTFIT_CHANCE = 4
        private const val SILENT_LEVEL_UP_CHANCE = 3
        private const val MAX_LEVEL_UP_REPLIES = 2
        private const val LEVEL_UP_REPLY_RADIUS = 7
        private val REPLY_DELAY = 2..4

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
