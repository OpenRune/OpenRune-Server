package org.rsmod.content.events.demonicpacts

import com.github.michaelbull.logging.InlineLogger
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.manager.CombatStyleGroup
import org.rsmod.api.combat.manager.PlayerCombatModifiers
import org.rsmod.api.config.constants
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.ironman.setGamemode
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.StatXpModifiers
import org.rsmod.api.player.stat.XpSource
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class DemonicPactsLeagueScript @Inject constructor(private val collision: CollisionFlagMap) :
    PluginScript() {
    private val logger = InlineLogger()

    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    override val worldTypeDenyMessage =
        "That only works on the Demonic Pacts League. Switch with ::worldtype."

    private val settings: LeagueSettings
        get() = DemonicPactsData.settings

    private val startCoord: CoordGrid
        get() = CoordGrid(settings.startX, settings.startZ, settings.startLevel)

    override fun ScriptContext.startup() {
        logger.info {
            "Demonic Pacts League: ${DemonicPactsData.nodes.size} pacts, " +
                "${DemonicPactsData.regions.size} regions, ${DemonicPactsData.tasks.size} tasks."
        }
        installModifiers()

        onPlayerLogin { player.onLeagueLogin() }
        onPlayerSoftTimer(LEAGUE_TICK_TIMER) { player.leagueTick() }
        onPlayerCoordsChanged { onMoved(player, lastKnownCoords) }

        registerCommands()
    }

    private fun installModifiers() {
        val previousXp = StatXpModifiers.modifier
        StatXpModifiers.modifier = { player, stat ->
            if (player.isDemonicPactsLeague) {
                LeagueRates.xpRate(player, stat)
            } else {
                previousXp(player, stat)
            }
        }

        val previousAccuracy = PlayerCombatModifiers.accuracy
        PlayerCombatModifiers.accuracy = { player, npc, style ->
            if (player.isDemonicPactsLeague) {
                1.0 + player.pactEffects().accuracyPercent / 100.0
            } else {
                previousAccuracy(player, npc, style)
            }
        }

        val previousMaxHit = PlayerCombatModifiers.maxHit
        PlayerCombatModifiers.maxHit = { player, npc, style ->
            if (player.isDemonicPactsLeague) {
                leagueMaxHitMultiplier(player, npc, style)
            } else {
                previousMaxHit(player, npc, style)
            }
        }
    }

    private fun leagueMaxHitMultiplier(player: Player, npc: Npc, style: CombatStyleGroup): Double {
        val effects = player.pactEffects()
        val stylePercent =
            when (style) {
                CombatStyleGroup.Melee -> effects.meleeDamagePercent
                CombatStyleGroup.Ranged -> effects.rangedDamagePercent
                CombatStyleGroup.Magic -> effects.magicDamagePercent
            }
        var distancePercent = 0
        if (effects.maxHitDistancePercent > 0) {
            val distance = player.coords.chebyshevDistance(npc.coords)
            distancePercent = effects.maxHitDistancePercent * (1 + distance / 3)
        }
        return 1.0 + (stylePercent + distancePercent) / 100.0
    }

    private fun Player.onLeagueLogin() {
        if (!dpInitialised) {
            startLeague()
        }
        if (gamemode == PlayerGamemode.NORMAL) {
            setGamemode(PlayerGamemode.IRONMAN)
        }
        xpRate = 1.0
        if (settings.nearBrokenMapSquare(coords.x, coords.z)) {
            moveTo(startCoord)
            mes("You were moved away from an area this cache cannot load.")
        }
        softTimer(LEAGUE_TICK_TIMER, LEAGUE_TICK_CYCLES)
        softTimer(PRAYER_REGEN_TIMER, PRAYER_REGEN_CYCLES)
        LeagueTasks.refreshDropRate(this)
        fillRunEnergy()
        mes("<col=ef1020>Welcome to the Demonic Pacts League.</col> Type ::league for your progress.")
    }

    private fun Player.startLeague() {
        dpInitialised = true
        pactPointsEarned = settings.startingPactPoints
        DemonicPactsData.regionsByKey["varlamore"]?.let { unlockRegion(it) }
        raiseToLevel("stat.herblore", HERBLORE_START_XP, 3)
        raiseToLevel("stat.runecrafting", RUNECRAFT_START_XP, 5)
        moveTo(startCoord)
        mes("Yama has bound you to Varlamore. Earn pact points and spend them with ::pacts.")
    }

    private fun Player.raiseToLevel(stat: String, xp: Double, level: Int) {
        if (statBase(stat) < level) {
            statAdvance(stat, xp, rate = 1.0, globalRate = 1.0, source = XpSource.Manual)
        }
    }

    private fun Player.leagueTick() {
        if (!isDemonicPactsLeague) {
            return
        }
        LeagueTasks.checkProgress(this)
        LeagueTasks.refreshDropRate(this)
        fillRunEnergy()
    }

    private fun onMoved(player: Player, lastKnownCoords: CoordGrid) {
        if (!player.isDemonicPactsLeague) {
            return
        }
        player.fillRunEnergy()
        if (LeagueRegions.isAllowed(player, player.coords)) {
            return
        }
        val region = LeagueRegions.regionAt(player.coords)
        player.mes("<col=ef1020>Your pact forbids it: you have not unlocked ${region?.name}.")
        val back =
            lastKnownCoords.takeIf {
                LeagueRegions.isAllowed(player, it) && collision.isZoneValid(it)
            } ?: startCoord
        player.moveTo(back)
    }

    private fun Player.moveTo(dest: CoordGrid) {
        if (!collision.isZoneValid(dest)) {
            logger.warn { "Demonic Pacts League: invalid destination $dest for $username" }
            return
        }
        PathingEntityCommon.telejump(this, collision, dest)
    }

    private fun Player.fillRunEnergy() {
        if (runEnergy < constants.run_max_energy) {
            runEnergy = constants.run_max_energy
            UpdateRun.energy(this, runEnergy)
        }
    }

    private fun ScriptContext.registerCommands() {
        onCommand("league") {
            desc = "Show your Demonic Pacts League progress"
            cheat { player.showSummary() }
        }
        onCommand("pacts") {
            desc = "List pacts: ::pacts [owned|all|todo]"
            cheat { player.listPacts(args.firstOrNull() ?: "available") }
        }
        onCommand("pact") {
            desc = "Show a pact: ::pact <id>"
            invalidArgs = "Use ::pact <id>, for example ::pact AA."
            cheat { player.showPact(args[0]) }
        }
        onCommand("pactbuy") {
            desc = "Buy a pact: ::pactbuy <id>"
            invalidArgs = "Use ::pactbuy <id>, for example ::pactbuy AA."
            cheat { player.buyPact(args[0]) }
        }
        onCommand("pactreset") {
            desc = "Reset your pacts (limited uses): ::pactreset confirm"
            cheat { player.resetPacts(args.firstOrNull()) }
        }
        onCommand("tasks") {
            desc = "List league tasks: ::tasks [region|todo|done] [page]"
            cheat { player.listTasks(args) }
        }
        onCommand("regions") {
            desc = "List league regions and unlock slots"
            cheat { player.listRegions() }
        }
        onCommand("unlockregion") {
            desc = "Unlock a league region: ::unlockregion <region>"
            invalidArgs = "Use ::unlockregion <region>, for example ::unlockregion asgarnia."
            cheat { player.chooseRegion(args[0]) }
        }
        onCommand("dpregion") {
            desc = "Show the league region you are standing in"
            cheat {
                val region = LeagueRegions.regionAt(player.coords)
                player.mes("${player.coords}: ${region?.name ?: "no region (always open)"}")
            }
        }
        onCommand("dppoints") {
            desc = "Admin: add league points: ::dppoints <amount>"
            requiredRights = Rights.ADMINISTRATOR
            cheat {
                player.leaguePoints += args[0].toInt()
                LeagueTasks.refreshDropRate(player)
                player.mes("League points: ${player.leaguePoints}, tier ${player.relicTier()}.")
            }
        }
        onCommand("dppactpoints") {
            desc = "Admin: add pact points: ::dppactpoints <amount>"
            requiredRights = Rights.ADMINISTRATOR
            cheat {
                repeat(args[0].toInt()) { LeagueTasks.grantPactPoint(player) }
            }
        }
        onCommand("dplockregions") {
            desc = "Admin: re-lock league regions to the ones you have earned"
            requiredRights = Rights.ADMINISTRATOR
            cheat {
                player.resetRegions()
                player.mes("Regions reset: ${player.unlockedRegions().joinToString { it.name }}.")
                player.mes("Use ::regions to choose any region slots you have earned.")
            }
        }
        onCommand("dpallregions") {
            desc = "Admin: unlock every league region"
            requiredRights = Rights.ADMINISTRATOR
            cheat {
                DemonicPactsData.regions.forEach { player.unlockRegion(it) }
                player.mes("All regions unlocked.")
            }
        }
    }

    private fun Player.showSummary() {
        val tier = relicTier()
        val next = settings.tierThresholds.getOrNull(tier)
        val nextText = if (next != null) " (next tier at $next)" else ""
        mes("<col=ef1020>Demonic Pacts League")
        mes("Points: $leaguePoints, relic tier $tier$nextText.")
        mes("Tasks: $leagueTasksDone of ${DemonicPactsData.tasks.size}. Kills: $leagueTotalKills.")
        mes(LeagueRates.describe(this))
        mes(
            "Pacts: ${ownedPacts().size} held, ${pactPointsAvailable()} to spend, " +
                "${PactTree.resetsRemaining(this)} resets left."
        )
        mes("Regions: ${unlockedRegions().joinToString { it.name }}.")
    }

    private fun Player.listPacts(filter: String) {
        val nodes =
            when (filter.lowercase()) {
                "owned" -> ownedPacts()
                "all" -> DemonicPactsData.nodes
                else -> PactTree.purchasable(this)
            }
        val heading =
            when (filter.lowercase()) {
                "owned" -> "Pacts held"
                "all" -> "All pacts"
                else -> "Pacts you can buy now"
            }
        mes("<col=ef1020>$heading (${nodes.size}), ${pactPointsAvailable()} points to spend:")
        if (nodes.isEmpty()) {
            mes("None.")
            return
        }
        for (node in nodes) {
            mes(node.summary(this))
        }
        mes("::pact <id> for details, ::pactbuy <id> to buy. [unimplemented] pacts do nothing yet.")
    }

    private fun Player.showPact(id: String) {
        val node = DemonicPactsData.nodesById[id.uppercase()]
        if (node == null) {
            mes("There is no pact with the id '$id'.")
            return
        }
        mes(node.summary(this))
        val linked = DemonicPactsData.neighbours[node.id].orEmpty().sorted()
        mes("Connects to: ${linked.joinToString()}.")
        val state =
            when {
                ownsPact(node) -> "You hold this pact."
                PactTree.canReach(this, node) -> "You can buy this pact with ::pactbuy ${node.id}."
                else -> "Buy a connected pact first."
            }
        mes(state)
    }

    private fun Player.buyPact(id: String) {
        val node = DemonicPactsData.nodesById[id.uppercase()]
        if (node == null) {
            mes("There is no pact with the id '$id'.")
            return
        }
        when (PactTree.buy(this, node)) {
            PactPurchase.Bought -> {
                mes("<col=ef1020>You make a pact: ${node.text}")
                if (!node.isImplemented) {
                    mes("This pact's effect is not implemented on this server yet.")
                }
                LeagueTasks.checkProgress(this)
            }
            PactPurchase.AlreadyOwned -> mes("You already hold that pact.")
            PactPurchase.NoPoints -> mes("You have no pact points to spend.")
            PactPurchase.NotConnected -> mes("That pact is not connected to one you hold.")
        }
    }

    private fun Player.resetPacts(confirm: String?) {
        val remaining = PactTree.resetsRemaining(this)
        if (confirm?.lowercase() != "confirm") {
            mes("This clears every pact you hold. Resets left: $remaining.")
            mes("Type ::pactreset confirm to go ahead.")
            return
        }
        if (PactTree.reset(this)) {
            mes("<col=ef1020>Your pacts are broken. Resets left: ${PactTree.resetsRemaining(this)}.")
        } else {
            mes("You have no pact resets left.")
        }
    }

    private fun Player.listTasks(args: List<String>) {
        val filter = args.firstOrNull()?.lowercase() ?: "todo"
        val page = (args.getOrNull(1)?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val tasks =
            DemonicPactsData.tasks.filter { task ->
                when (filter) {
                    "todo" -> !isTaskDone(task)
                    "done" -> isTaskDone(task)
                    "all" -> true
                    else -> task.region == filter
                }
            }
        val pages = max(1, (tasks.size + TASKS_PER_PAGE - 1) / TASKS_PER_PAGE)
        val shown = tasks.drop((page - 1) * TASKS_PER_PAGE).take(TASKS_PER_PAGE)
        mes("<col=ef1020>Tasks: $filter, page $page of $pages (${tasks.size} tasks)")
        for (task in shown) {
            val done = if (isTaskDone(task)) "<col=00ff00>[done]</col> " else ""
            val pact = if (task.pact) " <col=ff981f>[pact]</col>" else ""
            mes("$done${task.name} - ${task.tier}, ${task.region}$pact")
        }
        if (page < pages) {
            mes("::tasks $filter ${page + 1} for more.")
        }
    }

    private fun Player.listRegions() {
        val slots = LeagueTasks.regionSlots(this)
        val chosen = chosenRegionCount()
        mes("<col=ef1020>Regions (tasks done: $leagueTasksDone)")
        for (region in DemonicPactsData.regions.filter { it.bit >= 0 }) {
            val state =
                when {
                    hasRegion(region) -> "<col=00ff00>unlocked</col>"
                    region.unlockable -> "locked"
                    region.key == "karamja" -> "unlocks at ${settings.karamjaTasks} tasks"
                    else -> "<col=ff0000>closed this league</col>"
                }
            mes("${region.name} (${region.key}): $state")
        }
        val thresholds = settings.regionTaskThresholds.joinToString()
        mes("Region choices used: $chosen of $slots (new choices at $thresholds tasks).")
        if (chosen < slots) {
            mes("Choose one with ::unlockregion <key>.")
        }
    }

    private fun Player.chooseRegion(key: String) {
        val region = DemonicPactsData.regionsByKey[key.lowercase()]
        when {
            region == null || !region.unlockable -> mes("'$key' is not a region you can choose.")
            hasRegion(region) -> mes("You have already unlocked ${region.name}.")
            chosenRegionCount() >= LeagueTasks.regionSlots(this) ->
                mes("You need more completed tasks before choosing another region.")
            else -> {
                unlockRegion(region)
                mes("<col=ef1020>${region.name} is now unlocked!")
                LeagueTasks.checkProgress(this)
            }
        }
    }

    private fun PactNode.summary(player: Player): String {
        val owned = if (player.ownsPact(this)) "<col=00ff00>[held]</col> " else ""
        val unimplemented = if (isImplemented) "" else " <col=9f9f9f>[unimplemented]</col>"
        return "$owned<col=ff981f>$id</col> ($size): $text$unimplemented"
    }

    private companion object {
        const val LEAGUE_TICK_TIMER = "timer.dp_league_tick"
        const val PRAYER_REGEN_TIMER = "timer.dp_prayer_regen"
        const val LEAGUE_TICK_CYCLES = 10
        const val PRAYER_REGEN_CYCLES = 15
        const val TASKS_PER_PAGE = 10
        const val HERBLORE_START_XP = 174.0
        const val RUNECRAFT_START_XP = 388.0
    }
}
