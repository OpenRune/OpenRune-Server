package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.BasType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.hat
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.baseAgilityLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Runs every obstacle in `dbtable.agility_obstacles`: op1 (or ap1 within its range) on one of its
 * locs walks the player to the lane start and plays the lane tick by tick, granting the xp when it
 * lands, and tracks the lap. A failable obstacle rolls its success curve first and plays its fall
 * instead on a miss; the lap carries on once the obstacle is crossed. An npc obstacle (the Werewolf
 * stick) lands on op1 of its npc once it takes what it asks for, and a step-on obstacle (the
 * Agility Pyramid's stone blocks) once a player stands still on a lane's start. A lap passed in
 * order counts towards the course's lap count, pays its lap bonus and rolls for a mark of grace
 * (https://oldschool.runescape.wiki/w/Agility). The client's agility helper is fed as it is live.
 * A shortcut, a row with no course, answers its own loc op, checks its own level and gates and
 * pays its xp on landing outside any lap. A course's own rules beyond its lanes come from the
 * [AgilityCourseHook]s.
 */
class AgilityCourseScript
@Inject
internal constructor(
    private val marks: MarksOfGrace,
    private val hooks: Set<@JvmSuppressWildcards AgilityCourseHook>,
    private val pets: AgilityPets,
    private val shouts: NpcShouts,
    private val followers: PetFollowers,
    private val playerRegistry: PlayerRegistry,
    private val routeFactory: RouteFactory,
    private val protectedAccess: ProtectedAccessLauncher,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val obstacles = AgilityObstacle.loadAll()
        val laps = CourseLaps(obstacles)
        val byLoc = obstacles.flatMap { obstacle -> obstacle.locs.map { it to obstacle } }
        for ((key, candidates) in byLoc.groupBy({ it.first.id to it.second.op }, { it.second })) {
            val (loc, op) = key
            val type = candidates.first().locs.first { it.id == loc }
            onOpLoc(op, type) { cross(candidates, laps, it) }
            if (op != 1) {
                continue
            }
            val apRange = candidates.mapNotNull { it.apRange }.maxOrNull() ?: continue
            onApLoc1(type) {
                if (isWithinApRange(it.loc, apRange)) {
                    cross(candidates, laps, it.loc)
                }
            }
        }
        val byNpc = obstacles.flatMap { obstacle -> obstacle.npcs.map { it to obstacle } }
        for ((npc, candidates) in byNpc.groupBy({ it.first.id }, { it.second })) {
            val type = candidates.first().npcs.first { it.id == npc }
            onOpNpc1(type) { handIn(candidates, laps) }
        }
        val stepOns =
            obstacles
                .filter { it.isStepOn }
                .flatMap { obstacle ->
                    obstacle.lanes.mapIndexed { index, lane -> lane.start to (obstacle to index) }
                }
                .toMap()
        if (stepOns.isNotEmpty()) {
            val zones = stepOnZones(stepOns.keys)
            onEvent<GameLifecycle.LateCycle> { stepOn(stepOns, zones, laps) }
        }
    }

    private fun ScriptContext.onOpLoc(
        op: Int,
        type: ObjectServerType,
        action: suspend ProtectedAccess.(BoundLocInfo) -> Unit,
    ) {
        when (op) {
            1 -> onOpLoc1(type) { action(it.loc) }
            2 -> onOpLoc2(type) { action(it.loc) }
            3 -> onOpLoc3(type) { action(it.loc) }
            4 -> onOpLoc4(type) { action(it.loc) }
            else -> onOpLoc5(type) { action(it.loc) }
        }
    }

    // The registry files a player under their new zone only after LateCycle, so one who ran onto a
    // step-on tile this tick is still listed in a zone up to a run's steps away.
    private fun stepOnZones(tiles: Set<CoordGrid>): List<ZoneKey> {
        val reach = -MoveSpeed.Run.steps..MoveSpeed.Run.steps
        val zones = mutableSetOf<ZoneKey>()
        for (tile in tiles) {
            for (x in reach) {
                reach.mapTo(zones) { z -> ZoneKey.from(tile.translate(x, z)) }
            }
        }
        return zones.toList()
    }

    private fun stepOn(
        stepOns: Map<CoordGrid, Pair<AgilityObstacle, Int>>,
        zones: List<ZoneKey>,
        laps: CourseLaps,
    ) {
        for (player in playerRegistry.findAll(zones)) {
            val (obstacle, laneIndex) = stepOns[player.coords] ?: continue
            if (player.hasMovedThisCycle && !obstacle.walkOn || player.busy) {
                continue
            }
            if (player.agilityLvl < obstacle.course.level) {
                continue
            }
            protectedAccess.launch(player) {
                play(obstacle.lanes[laneIndex], land = { land(obstacle, laps) }, hit = {})
            }
        }
    }

    private suspend fun ProtectedAccess.handIn(
        candidates: List<AgilityObstacle>,
        laps: CourseLaps,
    ) {
        val obstacle =
            candidates.firstOrNull { laps.continues(player.lapCourse, player.lapStep, it) }
                ?: candidates.first()
        if (!canAttempt(obstacle)) {
            return
        }
        val handIn = checkNotNull(obstacle.handIn) { "Npc obstacle without a hand-in: $obstacle" }
        val carried = invTotal(inv, handIn.obj)
        if (carried == 0 || invDel(inv, handIn.obj, count = carried).failure) {
            mes(handIn.refused)
            return
        }
        mes(handIn.given, ChatType.Spam)
        land(obstacle, laps)
    }

    private suspend fun ProtectedAccess.cross(
        candidates: List<AgilityObstacle>,
        laps: CourseLaps,
        loc: BoundLocInfo,
    ) {
        val (obstacle, laneIndex) =
            choose(candidates, laps, loc)
                ?: run {
                    // UNVERIFIED: wording guessed.
                    mes("You can't do that from here.")
                    return
                }
        if (!canAttempt(obstacle) || !hooks.all { it.mayStart(this, obstacle) }) {
            return
        }
        val lane = obstacle.lanes[laneIndex]
        lane.opMessages.forEach { mes(it.text, ChatType.Spam) }
        arriveDelay()
        if (obstacle.step == 0) {
            val course = obstacle.course
            player.helperHighlightedCourse = course.helperCourse
            // UNVERIFIED: captures show only the highlighted course changing; live likely sets the
            // current course on entering the course, which the helper's lap scripts need.
            player.helperCurrentCourse = course.helperCourse
        } else if (!obstacle.isRecovery && player.helperHighlightedCourse == 0) {
            player.helperHighlightedCourse = obstacle.course.helperCourse
        }
        if (!walkToStart(lane.start)) {
            return
        }
        start(obstacle)
        val helper = !obstacle.isShortcut
        val failLane = obstacle.failLanes[laneIndex]
        if (failLane != null && fails(obstacle, lane)) {
            play(failLane, land = {}, hit = { fall(obstacle) }, helper)
            return
        }
        play(lane, land = { land(obstacle, laps) }, hit = {}, helper)
        lane.endMesboxes.forEach { mesbox(it.text) }
        obstacle.landMesbox?.let { mesbox(it) }
    }

    private fun ProtectedAccess.fails(obstacle: AgilityObstacle, lane: ObstacleLane): Boolean {
        val success = obstacle.success ?: return hooks.any { it.falls(this, obstacle, lane) }
        return !statRandom(AGILITY, success.low, success.high, 0)
    }

    /**
     * The obstacle among those sharing the clicked loc that the player can start from where they
     * stand, preferring one whose lane crosses the clicked [loc], then the one that carries their
     * lap on, then the nearest, and the lane they start.
     */
    private fun ProtectedAccess.choose(
        candidates: List<AgilityObstacle>,
        laps: CourseLaps,
        loc: BoundLocInfo,
    ): Pair<AgilityObstacle, Int>? {
        val reachable =
            candidates.mapNotNull { obstacle ->
                obstacle.laneFrom(coords).takeIf { it >= 0 }?.let { obstacle to it }
            }
        val startable =
            reachable
                .filter { (obstacle, lane) -> obstacle.lanes[lane].passes(loc) }
                .ifEmpty { reachable }
        return startable.firstOrNull { (obstacle, _) ->
            laps.continues(player.lapCourse, player.lapStep, obstacle)
        }
            ?: startable.minByOrNull { (obstacle, lane) ->
                coords.chebyshevDistance(obstacle.lanes[lane].start)
            }
    }

    private suspend fun ProtectedAccess.canAttempt(obstacle: AgilityObstacle): Boolean {
        val gates = obstacle.shortcut
        if (gates != null && !opens(gates)) {
            return false
        }
        if (!obstacle.isShortcut && !admitsToCourse(obstacle)) {
            return false
        }
        val requirement = obstacle.requirement
        if (requirement != null && !meets(requirement, obstacle.requirementObjs)) {
            refuse(requirement, obstacle.requirementObjs)
            return false
        }
        val stat = obstacle.stats.firstOrNull { player.stat(it.stat) < it.level }
        if (stat != null) {
            // UNVERIFIED: wording guessed.
            mes("You need ${stat.displayName.withArticle()} level of ${stat.level} to do that.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.opens(gates: ShortcutGates): Boolean {
        if (player.agilityLvl < gates.level) {
            refuseLevel(gates)
            return false
        }
        val quest = gates.quest
        val questDone = quest == null || hasQuest(quest, startedOnly = false)
        if (questDone && gates.varbits.all { vars[it.varbit] in it.values }) {
            return true
        }
        val refusal =
            gates.refusal
                // UNVERIFIED: wording guessed.
                ?: quest
                    ?.takeUnless { questDone }
                    ?.let { "You need to complete ${it.displayName} to use this shortcut." }
                // UNVERIFIED: wording guessed.
                ?: "You can't use this shortcut yet."
        mes(refusal)
        return false
    }

    private suspend fun ProtectedAccess.refuseLevel(gates: ShortcutGates) {
        val text = gates.levelRefusal
        val obj = gates.levelRefusalObj
        when {
            text != null && obj != null -> objbox(obj, zoom = LEVEL_REFUSAL_ZOOM, text)
            text != null -> mesbox(text)
            else -> mesbox("You need an Agility level of ${gates.level} to do that.")
        }
    }

    private suspend fun ProtectedAccess.admitsToCourse(obstacle: AgilityObstacle): Boolean {
        val course = obstacle.course
        if (player.agilityLvl < course.level) {
            // UNVERIFIED: wording guessed.
            mes("You need an Agility level of ${course.level} to attempt this course.")
            return false
        }
        if (course.levelUnboostable == true && player.baseAgilityLvl < course.level) {
            mes("Your Agility boost won't help you here.")
            return false
        }
        val quest = obstacle.courseGamevals.quest
        if (quest != null && !hasQuest(quest, startedOnly = course.questStarted == true)) {
            // UNVERIFIED: wording guessed.
            mes("You need to have done more of ${quest.displayName} to attempt this course.")
            return false
        }
        val form = AgilityForms.requiredFor(course)
        if (form != null && player.transmog?.id != form.npc.id) {
            form.refusal?.let { mes(it) }
            return false
        }
        if (!hooks.all { it.admits(this, course) }) {
            return false
        }
        if (course.noFollowers == true && followers.hasFollower(player)) {
            // UNVERIFIED: wording guessed.
            mes("You can't take a follower across these obstacles.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.start(obstacle: AgilityObstacle) {
        obstacle.startVarbit?.let { VarPlayerIntMapSetter.set(player, it.varbit, it.value) }
        for ((obj, broken) in obstacle.breaks) {
            for (slot in inv.indices) {
                if (inv[slot]?.id == obj.id) {
                    invReplaceSlot(inv, slot, count = 1, broken)
                }
            }
        }
    }

    private fun ProtectedAccess.hasQuest(quest: RequiredQuest, startedOnly: Boolean): Boolean =
        QuestRequirements.hasCompleted(player, quest.key) ||
            (startedOnly && QuestRequirements.isOnQuest(player, quest.key))

    private fun ProtectedAccess.meets(requirement: WornRequirement, objs: List<String>): Boolean {
        val wearsObj = objs.any { worn.count(it) > 0 }
        return when (requirement) {
            WornRequirement.NinjaGreegree -> wearsObj
            WornRequirement.CrossbowGrapple -> {
                val weapon = getOrNull(player.righthand)
                wearsObj && weapon != null && weapon.isCategoryType(CROSSBOW)
            }
            WornRequirement.BareHead -> player.hat == null
            WornRequirement.Worn -> wearsObj
            WornRequirement.NotCarried -> !wearsObj && objs.none { it in inv }
        }
    }

    private suspend fun ProtectedAccess.refuse(requirement: WornRequirement, objs: List<String>) {
        when (requirement) {
            WornRequirement.NinjaGreegree ->
                mes("Only the stealthiest and most agile monkey can use this!")
            WornRequirement.CrossbowGrapple ->
                if (objs.none { worn.count(it) > 0 }) {
                    mes("You need a mithril grapple-tipped bolt with a rope to do that.")
                } else {
                    // UNVERIFIED: wording guessed.
                    mes("You need a crossbow and a mithril grapple equipped to do that.")
                }
            WornRequirement.BareHead ->
                startDialogue {
                    chatNpcSpecific(
                        "Agility Trainer",
                        "npc.werewolf_trainer_2",
                        mesanim("mesanim.skullball_werewolf"),
                        "You need to take your headgear off before you try the Deathslide, " +
                            "otherwise you won't be able to get a good enough grip with your " +
                            "teeth.",
                    )
                }
            // UNVERIFIED: wording guessed.
            WornRequirement.Worn -> mes("You need the right equipment to do that.")
            // UNVERIFIED: Mairin's line on the bowl, as she says it when talked to.
            WornRequirement.NotCarried ->
                startDialogue {
                    chatNpcSpecific(
                        "Mairin",
                        "npc.fossil_mermaid_deepwater",
                        sad,
                        "You're not going into deeper water with that bowl.",
                    )
                }
        }
    }

    /**
     * Walks the player along a route to [start], a step a tick; returns `false` when no route gets
     * there. A start no route reaches, as a hole or a ladder's own tile, is stepped onto from the
     * tile beside it the route ends on.
     */
    private suspend fun ProtectedAccess.walkToStart(start: CoordGrid): Boolean {
        if (coords == start) {
            return true
        }
        val route = routeFactory.create(player.avatar, start)
        val end = route.lastOrNull()?.let { CoordGrid(it.x, it.z, it.level) } ?: coords
        if (end.chebyshevDistance(start) > 1) {
            return false
        }
        if (end != coords && !walkTo(end)) {
            return false
        }
        if (coords != start) {
            forcedWalk(start, crossTiles = 1)
        }
        return coords == start
    }

    private suspend fun ProtectedAccess.walkTo(dest: CoordGrid): Boolean {
        player.routeRequest = RouteRequestCoord(dest)
        player.tempMoveSpeed = MoveSpeed.Walk
        while (true) {
            val from = coords
            delay(1)
            if (coords == dest) {
                return true
            }
            val routed = player.routeRequest == null
            if (routed && (player.routeDestination.isEmpty() || coords == from)) {
                player.abortRoute()
                return false
            }
        }
    }

    /**
     * Plays [lane] a tick at a time. Messages go to the chat, or into a message box without a
     * continue button on a tick that has a mesbox action; that box closes as the lane ends.
     */
    private suspend fun ProtectedAccess.play(
        lane: ObstacleLane,
        land: () -> Unit,
        hit: () -> Unit,
        helper: Boolean = true,
    ) {
        val actionsByTick = lane.actions.groupBy { it.tick }
        val messagesByTick = lane.messages.groupBy { it.tick }
        val saysByTick = lane.says.groupBy { it.tick }
        val previousBas = player.bas
        var finished = false
        var mesboxOpen = false
        player.busy = true
        try {
            for (tick in 0..lane.lastTick) {
                val actions = actionsByTick[tick].orEmpty()
                val mesbox = actions.any { it.op == ActionOp.Mesbox }
                for (message in messagesByTick[tick].orEmpty()) {
                    if (mesbox) {
                        mesboxNp(message.text)
                        mesboxOpen = true
                    } else {
                        mes(message.text, ChatType.Spam)
                    }
                }
                saysByTick[tick]?.forEach { say(it.text) }
                for (action in actions) {
                    when (action.op) {
                        ActionOp.Land -> land()
                        ActionOp.Hit -> hit()
                        else -> perform(action, actions, previousBas, helper)
                    }
                }
                if (tick < lane.lastTick) {
                    delay(1)
                }
            }
            finished = true
        } finally {
            if (!finished && coords != lane.end) {
                telejump(lane.end, TeleportType.Exempt)
            }
            if (player.bas !== previousBas) {
                player.bas = previousBas
                rebuildAppearance()
            }
            if (mesboxOpen) {
                ifCloseSub(MESSAGE_BOX)
            }
            player.busy = false
        }
    }

    private fun ProtectedAccess.perform(
        action: ObstacleAction,
        tickActions: List<ObstacleAction>,
        previousBas: BasType?,
        helper: Boolean,
    ) {
        when (action.op) {
            ActionOp.Step -> teleport(action.coord, TeleportType.Exempt)
            ActionOp.Face -> faceSquare(action.coord)
            ActionOp.Anim -> anim(action.symbol, action.arg1)
            ActionOp.ResetAnim -> resetAnim()
            ActionOp.Sound -> soundSynth(action.symbol, loops = action.arg1, delay = action.arg2)
            ActionOp.Balance -> {
                val seq = action.ref
                player.bas = BasType(-1, seq, seq, seq, seq, seq, seq, seq)
                rebuildAppearance()
            }
            ActionOp.BalanceOff -> {
                player.bas = previousBas
                rebuildAppearance()
            }
            ActionOp.ExactMove -> {
                val path = tickActions.firstOrNull { it.op == ActionOp.ExactPath }
                exactMove(action, from = path?.coord ?: coords, to = path?.ref?.let(::CoordGrid))
            }
            ActionOp.Telejump -> telejump(action.coord, TeleportType.Exempt)
            ActionOp.FaceAngle -> player.pendingFaceAngle = EntityFaceAngle(action.ref)
            ActionOp.Xp -> payXp(action.ref, helper)
            ActionOp.Varbit -> vars[action.symbol] = action.arg1
            ActionOp.Spotanim -> spotanim(action.symbol, delay = action.arg1, height = action.arg2)
            ActionOp.LocAnim -> {
                val loc = locRepo.findExact(action.coord, LocShape.CentrepieceStraight)
                if (loc != null) {
                    worldRepo.locAnim(loc, action.symbol)
                }
            }
            ActionOp.NpcSay -> shouts.say(coords, action.symbol, action.arg1, action.arg2)
            ActionOp.ExactPath,
            ActionOp.Land,
            ActionOp.Hit,
            ActionOp.Wait,
            ActionOp.Mesbox -> Unit
        }
    }

    private fun ProtectedAccess.exactMove(move: ObstacleAction, from: CoordGrid, to: CoordGrid?) {
        val dest = move.coord
        val end = to ?: dest
        telejump(dest, TeleportType.Exempt)
        player.pendingExactMove =
            EntityExactMove(
                deltaX1 = from.x - dest.x,
                deltaZ1 = from.z - dest.z,
                deltaX2 = end.x - dest.x,
                deltaZ2 = end.z - dest.z,
                clientDelay1 = move.arg1,
                clientDelay2 = move.arg2,
                direction = move.ref,
            )
    }

    private fun ProtectedAccess.fall(obstacle: AgilityObstacle) {
        val damage = obstacle.fallDamage ?: return
        val share = player.hitpoints * damage.hpNumerator / damage.hpDenominator
        queueHit(1, HitType.Typeless, random.of(damage.min, damage.max) + share)
    }

    private fun ProtectedAccess.land(obstacle: AgilityObstacle, laps: CourseLaps) {
        if (obstacle.isShortcut) {
            payXp(obstacle.xpTenths, helper = false)
            obstacle.stats.forEach { statAdvance(it.stat, it.xpTenths / 10.0) }
            return
        }
        val course = obstacle.course
        val lapDone = player.advanceLap(obstacle, laps)
        if (obstacle.step == 0 && course.handInLaps != true) {
            runClientScript(START_LAP.asRSCM(RSCMType.CLIENTSCRIPT), player.currentMapClock)
        }
        if (lapDone) {
            player.helperHighlightedCourse = 0
            val count = player.addLap(obstacle.courseGamevals)
            marks.rollLap(this, course)
            LapCounter.announce(this, course, count)
        }
        hooks.forEach { it.landed(this, obstacle) }
        if (lapDone) {
            LapTimes.finish(this, course)
            hooks.forEach { it.lapDone(this, course) }
        }
        if (obstacle.step == 0) {
            LapTimes.start(player)
        }
        payXp(obstacle.xpTenthsFor(player), helper = !obstacle.isRecovery)
        for (stat in obstacle.stats) {
            statAdvance(stat.stat, stat.xpTenths / 10.0)
        }
        if (!lapDone) {
            return
        }
        val lapBonus = course.lapBonusFor(player)
        if (lapBonus > 0) {
            runClientScript(ADD_XP.asRSCM(RSCMType.CLIENTSCRIPT), lapBonus)
            statAdvance(AGILITY, lapBonus / 10.0)
        }
        val lapStat = obstacle.courseGamevals.lapStat
        if (lapStat != null) {
            statAdvance(lapStat.stat, lapStat.xpTenths / 10.0)
        }
        runClientScript(END_LAP.asRSCM(RSCMType.CLIENTSCRIPT), player.currentMapClock, 0)
        pets.roll(this, course)
    }

    private fun ProtectedAccess.payXp(tenths: Int, helper: Boolean = true) {
        if (tenths <= 0) {
            return
        }
        if (helper) {
            runClientScript(ADD_XP.asRSCM(RSCMType.CLIENTSCRIPT), tenths)
        }
        statAdvance(AGILITY, tenths / 10.0)
    }

    private fun AgilityCoursesRow.lapBonusFor(player: Player): Int {
        val perLevel = lapBonusPerLevel
        if (perLevel.size != 2) {
            return lapBonusXp
        }
        return minOf(perLevel[1], lapBonusXp + perLevel[0] * player.baseAgilityLvl)
    }

    private fun AgilityObstacle.xpTenthsFor(player: Player): Int {
        val diary = diaryXp ?: return xpTenths
        return if (player.vars[diary.varbit] != 0) diary.xpTenths else xpTenths
    }

    /**
     * Records [obstacle] in the lap; returns `true` when it finished an in-order lap. A way back
     * after a fall leaves the lap as it was.
     */
    private fun Player.advanceLap(obstacle: AgilityObstacle, laps: CourseLaps): Boolean {
        if (obstacle.isRecovery || obstacle.course.handInLaps == true) {
            return false
        }
        val courseId = obstacle.course.rowId
        val inOrder = obstacle.step == 0 || laps.continues(lapCourse, lapStep, obstacle)
        if (!inOrder || obstacle.step == laps.lastStep(courseId)) {
            lapCourse = 0
            lapStep = 0
            return inOrder
        }
        lapCourse = courseId
        lapStep = obstacle.step + 1
        return false
    }

    private fun Player.addLap(gamevals: CourseGamevals): Int {
        val laps = vars[gamevals.lapCount] + 1
        VarPlayerIntMapSetter.set(this, gamevals.lapCount, laps)
        return laps
    }

    private fun ObstacleLane.passes(loc: BoundLocInfo): Boolean {
        val minX = minOf(start.x, end.x) - 1
        val maxX = maxOf(start.x, end.x) + 1
        val minZ = minOf(start.z, end.z) - 1
        val maxZ = maxOf(start.z, end.z) + 1
        val locMaxX = loc.x + loc.adjustedWidth - 1
        val locMaxZ = loc.z + loc.adjustedLength - 1
        return loc.x <= maxX && locMaxX >= minX && loc.z <= maxZ && locMaxZ >= minZ
    }

    private fun String.withArticle(): String =
        if (firstOrNull()?.lowercaseChar() in VOWELS) "an $this" else "a $this"

    private companion object {
        const val AGILITY = "stat.agility"
        const val START_LAP = "clientscript.[clientscript,helper_agility_start_lap]"
        const val ADD_XP = "clientscript.[clientscript,helper_agility_add_xp]"
        const val END_LAP = "clientscript.[clientscript,helper_agility_end_lap]"
        const val CROSSBOW = "category.crossbow"
        const val MESSAGE_BOX = "interface.messagebox"
        const val LEVEL_REFUSAL_ZOOM = 400
        val VOWELS = setOf('a', 'e', 'i', 'o', 'u')

        var Player.lapCourse by intVarBit("varbit.agility_lap_course")
        var Player.lapStep by intVarBit("varbit.agility_lap_step")
        var Player.busy by boolVarBit("varbit.busy")
        var Player.helperHighlightedCourse by intVarBit("varbit.helper_agility_highlighted_course")
        var Player.helperCurrentCourse by intVarBit("varbit.helper_agility_current_course")
    }
}
