package org.rsmod.content.events.demonicpacts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Player

private const val DROP_RATE_VARP = "varp.drop_rate_multiplier"
private const val SPELLBOOK_VARBIT = "varbit.spellbook"

object LeagueTasks {
    private val byTrack: Map<String, List<OfficialTask>> by lazy { OfficialTasks.all.groupBy { it.track } }

    private val tickTracks =
        listOf(
            "level",
            "level_any",
            "level_all",
            "total_level",
            "combat_level",
            "xp",
            "xp_noncombat",
            "xp_combat",
            "equip",
            "equip_piece",
            "equip_set",
            "obtain",
            "quest",
            "spellbook",
        )

    private val questsByName: Map<String, Quest> by lazy {
        Quest.all().associateBy { it.displayName.lowercase() }
    }

    fun onKill(player: Player, npcName: String) {
        player.leagueTotalKills += 1
        for (task in byTrack["kill"].orEmpty()) {
            if (task.npcs.any { OfficialTasks.namesMatch(npcName, it, allowShorter = true) }) {
                progress(player, task, 1)
            }
        }
    }

    /**
     * A finished skilling action. [subjects] are `obj.` symbols or plain names; a task matches
     * when its verb is [action] and one of its names matches one of them.
     */
    fun onActivity(player: Player, action: String, subjects: List<String>, count: Int, stat: String? = null) {
        val names = subjects.map(::displayName).filter { it.isNotEmpty() }
        if (names.isEmpty()) {
            return
        }
        for (task in byTrack["action"].orEmpty()) {
            if (task.action != action || (task.stat != null && task.stat != stat)) {
                continue
            }
            if (task.items.any { wanted -> names.any { OfficialTasks.namesMatch(it, wanted) } }) {
                progress(player, task, count)
            }
        }
    }

    fun onEvent(player: Player, event: String) {
        for (task in byTrack["event"].orEmpty()) {
            if (task.event == event) {
                progress(player, task, 1)
            }
        }
    }

    fun checkProgress(player: Player) {
        for (track in tickTracks) {
            for (task in byTrack[track].orEmpty()) {
                if (!player.isTaskDone(task) && canProgress(player, task) && player.meets(task)) {
                    complete(player, task)
                }
            }
        }
    }

    private fun progress(player: Player, task: OfficialTask, amount: Int) {
        if (player.isTaskDone(task) || !canProgress(player, task) || !isInTaskArea(player, task)) {
            return
        }
        val counter = task.counter
        if (counter == null || task.count <= 1) {
            complete(player, task)
            return
        }
        val total = (player.vars[counter] + amount).coerceAtMost(task.count)
        VarPlayerIntMapSetter.set(player, counter, total)
        if (total >= task.count) {
            complete(player, task)
        }
    }

    fun complete(player: Player, task: OfficialTask, announce: Boolean = true) {
        if (player.isTaskDone(task)) {
            return
        }
        val settings = DemonicPactsData.settings
        val tierBefore = player.relicTier()
        val points = OfficialTasks.pointsFor(task)

        player.setTaskDone(task)
        player.leagueTasksDone += 1
        player.leaguePoints += points
        if (announce) {
            player.mes("<col=ef1020>League task completed: ${task.name} (+$points points).")
        }

        val bonusEvery = settings.bonusPactPointEveryTasks
        if (bonusEvery > 0 && player.leagueTasksDone % bonusEvery == 0) {
            grantPactPoint(player)
        }

        val tierAfter = player.relicTier()
        if (tierAfter > tierBefore) {
            player.mes("<col=ef1020>You have reached relic tier $tierAfter! Choose a relic in Journal > Leagues > Relics.")
            player.mes(LeagueRates.describe(player))
        }
        refreshDropRate(player)
        player.syncLeagueClientVars()
        checkRegionUnlocks(player)
    }

    /** Reverses [complete]; a bonus pact point the task may have earned is kept. */
    fun undo(player: Player, task: OfficialTask) {
        if (!player.isTaskDone(task)) {
            return
        }
        val points = OfficialTasks.pointsFor(task)
        player.setTaskDone(task, done = false)
        player.leagueTasksDone = (player.leagueTasksDone - 1).coerceAtLeast(0)
        player.leaguePoints = (player.leaguePoints - points).coerceAtLeast(0)
        task.counter?.let { VarPlayerIntMapSetter.set(player, it, 0) }
        player.mes("<col=ef1020>League task taken back: ${task.name} (-$points points).")
        refreshDropRate(player)
        player.syncLeagueClientVars()
    }

    fun grantPactPoint(player: Player) {
        val max = DemonicPactsData.settings.maxPactPointsEarned
        if (player.pactPointsEarned >= max) {
            return
        }
        player.pactPointsEarned += 1
        player.syncLeagueClientVars()
        player.mes(
            "<col=ef1020>You earned a pact point. Unspent: ${player.pactPointsAvailable()}. " +
                "Spend it in the pact tree."
        )
    }

    fun refreshDropRate(player: Player) {
        val percent = (LeagueRates.dropRate(player) * 100).toInt()
        if (player.vars[DROP_RATE_VARP] != percent) {
            VarPlayerIntMapSetter.set(player, DROP_RATE_VARP, percent)
        }
    }

    fun regionSlots(player: Player): Int =
        DemonicPactsData.settings.regionTaskThresholds.count { player.leagueTasksDone >= it }

    private fun checkRegionUnlocks(player: Player) {
        val settings = DemonicPactsData.settings
        val karamja = DemonicPactsData.regionsByKey["karamja"]
        if (
            karamja != null &&
                !player.hasRegion(karamja) &&
                player.leagueTasksDone >= settings.karamjaTasks
        ) {
            player.unlockRegion(karamja)
            player.syncAreaSelections()
            player.mes("<col=ef1020>Karamja is now unlocked!")
        }
        if (settings.regionTaskThresholds.contains(player.leagueTasksDone)) {
            player.mes("<col=ef1020>You can unlock a new area. Open Journal > Leagues > Areas.")
        }
    }

    /** Kills and skilling for an area's task only count while the player is in that area. */
    private fun isInTaskArea(player: Player, task: OfficialTask): Boolean {
        if (task.track != "kill" && task.track != "action") {
            return true
        }
        val region = OfficialTasks.regionFor(task) ?: return true
        return LeagueRegions.regionAt(player.coords) == region
    }

    fun canProgress(player: Player, task: OfficialTask): Boolean {
        val region = OfficialTasks.regionFor(task) ?: return true
        return player.hasRegion(region)
    }

    private fun Player.meets(task: OfficialTask): Boolean =
        when (task.track) {
            "level" -> task.stat != null && statBase(task.stat) >= task.value
            "level_any" -> LEAGUE_STATS.any { it != HITPOINTS && statBase(it) >= task.value }
            "level_all" -> LEAGUE_STATS.all { statBase(it) >= task.value }
            "total_level" -> leagueTotalLevel() >= task.value
            "combat_level" -> combatLevel >= task.value
            "xp" -> task.stat != null && statMap.getXP(task.stat) >= task.value
            "xp_noncombat" -> LEAGUE_STATS.any { it !in COMBAT_STATS && statMap.getXP(it) >= task.value }
            "xp_combat" -> COMBAT_STATS.any { it != PRAYER && statMap.getXP(it) >= task.value }
            "equip" -> wearsAll(task)
            "equip_piece" -> wornNames().any { task.match != null && it.contains(task.match) }
            "equip_set" -> wornNames().count { task.match != null && it.contains(task.match) } >= SET_PIECES
            "obtain" -> holds(task)
            "quest" -> questsByName[task.quest?.lowercase()]?.isQuestCompleted(this) == true
            "spellbook" -> vars[SPELLBOOK_VARBIT].toLong() == task.value
            else -> false
        }

    private fun Player.wornNames(): List<String> =
        worn.mapNotNull { obj -> obj?.let { OfficialTasks.itemName(it.id) } }

    private fun Player.wearsAll(task: OfficialTask): Boolean {
        val groups = OfficialTasks.itemGroups(task) ?: return false
        val wornIds = worn.mapNotNull { it?.id }.toSet()
        return groups.all { group -> group.any { it in wornIds } }
    }

    private fun Player.holds(task: OfficialTask): Boolean {
        val group = OfficialTasks.itemGroups(task)?.firstOrNull() ?: return false
        val bank = invMap.getOrPut("inv.bank")
        var total = 0L
        for (container in listOf(inv, worn, bank)) {
            for (obj in container) {
                if (obj != null && obj.id in group) {
                    total += obj.count
                }
            }
        }
        return total >= task.count
    }

    private fun displayName(subject: String): String {
        if (!subject.startsWith("obj.")) {
            return subject.lowercase()
        }
        val id = runCatching { subject.asRSCM(RSCMType.OBJ) }.getOrNull() ?: return ""
        return OfficialTasks.itemName(id)
    }

    private const val HITPOINTS = "stat.hitpoints"
    private const val PRAYER = "stat.prayer"
    private const val SET_PIECES = 3
}

object LeagueRates {
    fun xpRate(player: Player, stat: String): Double {
        val tier = player.relicTier()
        val base =
            when {
                tier >= 6 -> 16.0
                tier >= 4 -> 12.0
                tier >= 2 -> 8.0
                else -> 5.0
            }
        val combat = if (tier >= 3 && stat in COMBAT_STATS) 1.5 else 1.0
        return base * combat
    }

    fun dropRate(player: Player): Double = if (player.relicTier() >= 4) 5.0 else 2.0

    fun describe(player: Player): String {
        val xp = xpRate(player, "stat.woodcutting").toInt()
        val combat = xpRate(player, "stat.attack")
        val combatText = if (combat > xp) " (combat ${"%.1f".format(combat)}x)" else ""
        return "XP: ${xp}x$combatText. Drops: ${dropRate(player).toInt()}x."
    }
}
