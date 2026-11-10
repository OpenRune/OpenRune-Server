package org.rsmod.content.other.bots

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.movement.RouteRequestLoc
import org.rsmod.game.movement.RouteRequestPathingEntity
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Drives a bot's look of being busy. Bots only ever change their own avatar: they never run an
 * interaction, so the trees, rocks and fishing spots they "work" are untouched for real players.
 */
class BotBrain
@Inject
constructor(
    private val locRegistry: LocRegistry,
    private val npcRegistry: NpcRegistry,
    private val random: GameRandom,
    private val mapClock: MapClock,
) {
    private val claimed = HashSet<CoordGrid>()

    /**
     * Advances the bot's behaviour by one tick.
     *
     * Returns the new skill level if the bot gained a level during a skilling action.
     */
    fun tick(bot: Bot): Int? {
        when (bot.phase) {
            BotPhase.RESTING -> rest(bot)
            BotPhase.WALKING -> walk(bot)
            BotPhase.WORKING -> return work(bot)
        }
        return null
    }

    /**
     * Gives a skilling bot a starting level and some progress toward its next level.
     *
     * These stats belong to the cosmetic avatar and last until it despawns.
     */
    fun seedSkill(bot: Bot) {
        val skill = bot.activity.skill ?: return
        val level = random.of(INITIAL_SKILL_LEVELS)
        val levelXp = PlayerSkillXPTable.getXPFromLevel(level)
        val nextXp = PlayerSkillXPTable.getXPFromLevel(level + 1)
        val remaining = bot.activity.workXp * random.of(FIRST_LEVEL_ACTIONS)
        setSkill(bot, skill, maxOf(levelXp, nextXp - remaining), level)
    }

    fun forget(bot: Bot) {
        unclaim(bot)
    }

    private fun rest(bot: Bot) {
        maybeChat(bot)
        if (--bot.phaseTicks > 0) {
            return
        }
        if (bot.onErrand) {
            bot.onErrand = false
            walkTo(bot, random.of(bot.home, bot.radius), run = true)
            return
        }
        val bank = bot.bank
        if (bank != null && bot.tripsSinceBank >= random.of(TRIPS_PER_BANK_RUN)) {
            bot.tripsSinceBank = 0
            bot.onErrand = true
            walkTo(bot, random.of(bank, BANK_SPREAD), run = true)
            return
        }
        if (bot.player.coords.distanceTo(bot.home) > bot.radius + STRAY_DISTANCE) {
            walkTo(bot, bot.home)
            return
        }
        when (bot.activity) {
            BotActivity.WOODCUTTING -> startLocWork(bot, CHOP_OPS)
            BotActivity.MINING -> startLocWork(bot, MINE_OPS)
            BotActivity.FISHING -> startFishing(bot)
            BotActivity.QUESTING -> startQuestStep(bot)
            BotActivity.WANDERING -> walkTo(bot, random.of(bot.home, bot.radius), bot.runs)
            BotActivity.IDLING -> idle(bot)
        }
    }

    private fun walk(bot: Bot) {
        if (bot.player.routeDestination.isNotEmpty() && --bot.phaseTicks > 0) {
            return
        }
        val loc = bot.targetLoc
        val npc = bot.targetNpc
        when {
            loc != null -> arriveAtLoc(bot, loc)
            npc != null -> arriveAtNpc(bot, npc)
            bot.onErrand -> rest(bot, random.of(BANK_TICKS))
            else -> rest(bot, random.of(REST_TICKS))
        }
    }

    private fun work(bot: Bot): Int? {
        if (--bot.phaseTicks <= 0 || !isTargetStillThere(bot)) {
            stopWork(bot)
            return null
        }
        if (mapClock.cycle < bot.nextAnimCycle) {
            return null
        }
        bot.workAnim?.let { bot.player.anim(it) }
        bot.nextAnimCycle = mapClock.cycle + ANIM_INTERVAL
        return gainWorkXp(bot)
    }

    private fun gainWorkXp(bot: Bot): Int? {
        val skill = bot.activity.skill ?: return null
        val previous = bot.player.statBase(skill)
        if (previous >= MAX_SKILL_LEVEL) return null

        val stats = bot.player.statMap
        val xp = stats.getXP(skill) + bot.activity.workXp
        val level = PlayerSkillXPTable.getLevelFromXP(xp)
        if (level == previous) {
            stats.setXP(skill, xp)
            return null
        }
        setSkill(bot, skill, xp, level)
        return level
    }

    private fun setSkill(bot: Bot, skill: String, xp: Int, level: Int) {
        val stats = bot.player.statMap
        stats.setXP(skill, xp)
        stats.setBaseLevel(skill, level.toByte())
        stats.setCurrentLevel(skill, level.toByte())
    }

    private fun startLocWork(bot: Bot, ops: Set<String>) {
        val loc = random.pickOrNull(locsNear(bot, ops))
        if (loc == null) {
            walkTo(bot, random.of(bot.home, bot.radius))
            return
        }
        val type = ServerCacheManager.getObject(loc.id) ?: return rest(bot, IDLE_FALLBACK)
        bot.targetLoc = loc
        claim(bot, loc.coords)
        bot.player.routeRequest =
            RouteRequestLoc(
                destination = loc.coords,
                width = type.width,
                length = type.length,
                shape = loc.entity.shape,
                angle = loc.entity.angle,
                forceApproachFlags = type.forceApproachFlags,
            )
        bot.phase = BotPhase.WALKING
        bot.phaseTicks = WALK_TIMEOUT
    }

    private fun startFishing(bot: Bot) {
        val spot = random.pickOrNull(fishingSpotsNear(bot))
        if (spot == null) {
            walkTo(bot, random.of(bot.home, bot.radius))
            return
        }
        bot.targetNpc = spot
        claim(bot, spot.coords)
        bot.player.routeRequest = RouteRequestPathingEntity(spot.avatar)
        bot.phase = BotPhase.WALKING
        bot.phaseTicks = WALK_TIMEOUT
    }

    private fun arriveAtLoc(bot: Bot, loc: LocInfo) {
        val type = ServerCacheManager.getObject(loc.id)
        if (type == null || !isAdjacent(bot.player.coords, loc.coords, type.width, type.length)) {
            bot.targetLoc = null
            unclaim(bot)
            return rest(bot, IDLE_FALLBACK)
        }
        bot.player.faceLoc(loc, type.width, type.length)
        bot.workAnim = toolAnim(bot)
        startWork(bot, loc.coords)
    }

    private fun startQuestStep(bot: Bot) {
        val npc = random.pickOrNull(questNpcsNear(bot))
        if (npc == null) {
            walkTo(bot, random.of(bot.home, bot.radius))
            return
        }
        bot.targetNpc = npc
        bot.player.routeRequest = RouteRequestPathingEntity(npc.avatar)
        bot.phase = BotPhase.WALKING
        bot.phaseTicks = WALK_TIMEOUT
    }

    private fun arriveAtNpc(bot: Bot, npc: Npc) {
        val reach = if (bot.activity == BotActivity.QUESTING) TALK_DISTANCE else 1
        if (!npc.isSlotAssigned || bot.player.coords.distanceTo(npc.coords) > reach) {
            bot.targetNpc = null
            unclaim(bot)
            return rest(bot, IDLE_FALLBACK)
        }
        bot.player.faceNpc(npc)
        if (bot.activity == BotActivity.QUESTING) {
            bot.workAnim = null
            startWork(bot, npc.coords)
            bot.phaseTicks = random.of(TALK_TICKS)
            return
        }
        bot.workAnim = fishingAnim(npc)
        startWork(bot, npc.coords)
    }

    private fun startWork(bot: Bot, target: CoordGrid) {
        bot.workTarget = target
        bot.phase = BotPhase.WORKING
        bot.phaseTicks = random.of(WORK_TICKS)
        bot.nextAnimCycle = mapClock.cycle
    }

    private fun stopWork(bot: Bot) {
        bot.player.resetAnim()
        if (bot.targetNpc != null) {
            bot.player.resetFaceEntity()
        }
        bot.targetLoc = null
        bot.targetNpc = null
        bot.workAnim = null
        bot.tripsSinceBank++
        unclaim(bot)
        rest(bot, random.of(REST_TICKS))
    }

    private fun isTargetStillThere(bot: Bot): Boolean {
        val loc = bot.targetLoc
        if (loc != null) {
            return locRegistry.isValid(loc.coords, loc.id)
        }
        val npc = bot.targetNpc ?: return true
        if (bot.activity == BotActivity.QUESTING) {
            return npc.isSlotAssigned && bot.player.coords.distanceTo(npc.coords) <= TALK_DISTANCE
        }
        return npc.isSlotAssigned && npc.coords == bot.workTarget
    }

    private fun idle(bot: Bot) {
        if (bot.player.coords.distanceTo(bot.spot) > IDLE_STEP_RADIUS) {
            walkTo(bot, bot.spot)
            return
        }
        when (random.of(4)) {
            0 -> walkTo(bot, random.of(bot.spot, IDLE_STEP_RADIUS))
            1 -> {
                bot.player.faceSquare(random.of(bot.player.coords, IDLE_STEP_RADIUS))
                rest(bot, random.of(IDLE_TICKS))
            }
            else -> rest(bot, random.of(IDLE_TICKS))
        }
    }

    private fun walkTo(bot: Bot, dest: CoordGrid, run: Boolean = false) {
        bot.targetLoc = null
        bot.targetNpc = null
        unclaim(bot)
        if (run) {
            bot.player.tempMoveSpeed = MoveSpeed.Run
        }
        bot.player.routeRequest = RouteRequestCoord(dest)
        bot.phase = BotPhase.WALKING
        bot.phaseTicks = WALK_TIMEOUT
    }

    private fun claim(bot: Bot, target: CoordGrid) {
        unclaim(bot)
        claimed += target
        bot.claim = target
    }

    private fun unclaim(bot: Bot) {
        val claim = bot.claim ?: return
        claimed -= claim
        bot.claim = null
    }

    private fun rest(bot: Bot, ticks: Int) {
        bot.phase = BotPhase.RESTING
        bot.phaseTicks = ticks
    }

    private fun maybeChat(bot: Bot) {
        val chance = if (bot.activity == BotActivity.IDLING) IDLER_CHAT_CHANCE else CHAT_CHANCE
        if (random.of(chance) != 0) {
            return
        }
        val text = random.pick(BotChat.linesFor(bot.activity))
        BotChat.say(bot.player, text)
    }

    private fun toolAnim(bot: Bot): String? {
        val tool = bot.player.righthand ?: return null
        val seq = getInvObj(tool).paramOrNull(params.skill_anim) ?: return null
        return RSCM.getReverseMapping(RSCMType.SEQ, seq.id)
    }

    private fun fishingAnim(npc: Npc): String {
        val ops = (0 until OP_COUNT).mapNotNull { npc.type.actions.getOpOrNull(it)?.lowercase() }
        return when {
            "harpoon" in ops -> "seq.human_harpoon"
            "cage" in ops -> "seq.human_lobster"
            "big net" in ops -> "seq.human_largenet"
            "net" in ops || "small net" in ops -> "seq.human_smallnet"
            else -> "seq.human_fishing_casting"
        }
    }

    private fun locsNear(bot: Bot, ops: Set<String>): List<LocInfo> =
        zonesNear(bot)
            .flatMap(locRegistry::findAll)
            .filter { it.coords.distanceTo(bot.home) <= bot.radius && it.coords !in claimed }
            .filter { loc ->
                val type = ServerCacheManager.getObject(loc.id) ?: return@filter false
                (0 until OP_COUNT).any { type.actions.getOpOrNull(it)?.lowercase() in ops }
            }
            .toList()

    private fun fishingSpotsNear(bot: Bot): List<Npc> =
        zonesNear(bot)
            .flatMap(npcRegistry::findAll)
            .filter { it.coords.distanceTo(bot.home) <= bot.radius && it.coords !in claimed }
            .filter { it.name.contains(FISHING_SPOT, ignoreCase = true) }
            .toList()

    private fun questNpcsNear(bot: Bot): List<Npc> =
        zonesNear(bot)
            .flatMap(npcRegistry::findAll)
            .filter { it.coords.distanceTo(bot.home) <= bot.radius }
            .filter { !it.name.contains(FISHING_SPOT, ignoreCase = true) && it.name != NULL_NAME }
            .toList()

    private fun zonesNear(bot: Bot): Sequence<ZoneKey> {
        val home = bot.home
        val minX = (home.x - bot.radius) shr ZONE_SHIFT
        val maxX = (home.x + bot.radius) shr ZONE_SHIFT
        val minZ = (home.z - bot.radius) shr ZONE_SHIFT
        val maxZ = (home.z + bot.radius) shr ZONE_SHIFT
        return sequence {
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    yield(ZoneKey(x, z, home.level))
                }
            }
        }
    }

    private fun isAdjacent(coords: CoordGrid, loc: CoordGrid, width: Int, length: Int): Boolean {
        val dx = maxOf(loc.x - coords.x, 0, coords.x - (loc.x + width - 1))
        val dz = maxOf(loc.z - coords.z, 0, coords.z - (loc.z + length - 1))
        return coords.level == loc.level && dx <= 1 && dz <= 1
    }

    private fun CoordGrid.distanceTo(other: CoordGrid): Int =
        if (level != other.level) Int.MAX_VALUE else chebyshevDistance(other)

    private companion object {
        private val CHOP_OPS = setOf("chop down", "chop")
        private val MINE_OPS = setOf("mine")
        private const val FISHING_SPOT = "fishing spot"
        private const val OP_COUNT = 5

        private const val NULL_NAME = "null"
        private const val TALK_DISTANCE = 2
        private val TALK_TICKS = 8..25
        private val TRIPS_PER_BANK_RUN = 3..6
        private val BANK_TICKS = 4..12
        private const val BANK_SPREAD = 2

        private val WORK_TICKS = 20..70
        private val REST_TICKS = 3..15
        private val IDLE_TICKS = 10..40
        private const val IDLE_FALLBACK = 5
        private const val IDLE_STEP_RADIUS = 2
        private const val WALK_TIMEOUT = 40
        private const val STRAY_DISTANCE = 10
        private const val ANIM_INTERVAL = 4
        private val INITIAL_SKILL_LEVELS = 25..50
        private val FIRST_LEVEL_ACTIONS = 15..55
        private const val MAX_SKILL_LEVEL = 99
        private const val ZONE_SHIFT = 3
        private const val CHAT_CHANCE = 1500
        private const val IDLER_CHAT_CHANCE = 300
    }
}
