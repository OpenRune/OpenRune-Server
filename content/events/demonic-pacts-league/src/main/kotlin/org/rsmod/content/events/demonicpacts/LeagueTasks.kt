package org.rsmod.content.events.demonicpacts

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

private const val DROP_RATE_VARP = "varp.drop_rate_multiplier"

object LeagueTasks {
    fun onKill(player: Player, npcName: String) {
        player.leagueTotalKills += 1
        for (task in DemonicPactsData.tasks) {
            if (task.type != "kill" || player.isTaskDone(task)) {
                continue
            }
            if (task.npc.equals(npcName, ignoreCase = true) && player.canProgress(task)) {
                complete(player, task)
            }
        }
        checkProgress(player)
    }

    fun checkProgress(player: Player) {
        for (task in DemonicPactsData.tasks) {
            if (task.type == "kill" || player.isTaskDone(task) || !player.canProgress(task)) {
                continue
            }
            if (player.meets(task)) {
                complete(player, task)
            }
        }
    }

    fun complete(player: Player, task: LeagueTask) {
        val settings = DemonicPactsData.settings
        val tierBefore = player.relicTier()
        val points = settings.pointsFor(task.tier)

        player.setTaskDone(task)
        player.leagueTasksDone += 1
        player.leaguePoints += points
        player.mes(
            "<col=ef1020>League task completed: ${task.name} (${task.tier}, +$points points)."
        )

        val bonusEvery = settings.bonusPactPointEveryTasks
        val bonus = bonusEvery > 0 && player.leagueTasksDone % bonusEvery == 0
        if (task.pact || bonus) {
            grantPactPoint(player)
        }

        val tierAfter = player.relicTier()
        if (tierAfter > tierBefore) {
            player.mes("<col=ef1020>You have reached relic tier $tierAfter!")
            player.mes(LeagueRates.describe(player))
        }
        refreshDropRate(player)
        checkRegionUnlocks(player)
    }

    fun grantPactPoint(player: Player) {
        val max = DemonicPactsData.settings.maxPactPointsEarned
        if (player.pactPointsEarned >= max) {
            return
        }
        player.pactPointsEarned += 1
        player.mes(
            "<col=ef1020>You earned a pact point. Unspent: ${player.pactPointsAvailable()}. " +
                "See ::pacts."
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
            player.mes("<col=ef1020>Karamja is now unlocked!")
        }
        if (settings.regionTaskThresholds.contains(player.leagueTasksDone)) {
            player.mes("<col=ef1020>You can choose a new region to unlock. See ::regions.")
        }
    }

    private fun Player.canProgress(task: LeagueTask): Boolean {
        if (task.region == "general") {
            return true
        }
        val region = DemonicPactsData.regionsByKey[task.region] ?: return true
        return hasRegion(region)
    }

    private fun Player.meets(task: LeagueTask): Boolean =
        when (task.type) {
            "kills" -> leagueTotalKills >= task.value
            "level" -> task.stat != null && statBase(task.stat) >= task.value
            "total_level" -> leagueTotalLevel() >= task.value
            "combat_level" -> combatLevel >= task.value
            "pacts_owned" -> ownedPacts().size >= task.value
            else -> false
        }
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
