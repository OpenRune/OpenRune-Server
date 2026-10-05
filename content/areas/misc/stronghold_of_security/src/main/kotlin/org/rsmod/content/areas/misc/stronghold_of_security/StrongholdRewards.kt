package org.rsmod.content.areas.misc.stronghold_of_security

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StrongholdRewards @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (floor in StrongholdFloor.entries) {
            if (floor == StrongholdFloor.Death) {
                onOpLoc1(floor.reward) { openCradle() }
            } else {
                onOpLoc1(floor.reward) { openChest(floor) }
            }
        }
    }

    private suspend fun ProtectedAccess.openChest(floor: StrongholdFloor) {
        arriveDelay()
        if (player.hasClaimed(floor)) {
            mes("You have already claimed the reward from here.")
            return
        }
        claim(floor)
        invAddOrDrop(objRepo, "obj.coins", floor.coins)
        startDialogue { objbox("obj.coins", chestText(floor)) }
        mes("You have unlocked the ${floor.emoteName} emote.")
    }

    private suspend fun ProtectedAccess.openCradle() {
        arriveDelay()
        val floor = StrongholdFloor.Death
        if (!player.hasClaimed(floor)) {
            claim(floor)
            mes("You open the Cradle of Life and feel your wounds and weariness melt away.")
            mes("You have unlocked the ${floor.emoteName} emote.")
        }
        chooseBoots()
    }

    private suspend fun ProtectedAccess.chooseBoots() {
        var boots = ""
        startDialogue {
            boots =
                choice3(
                    "Fancy boots",
                    FancyBoots,
                    "Fighting boots",
                    FightingBoots,
                    "Fancier boots",
                    FancierBoots,
                    title = "Which boots do you take?",
                )
        }
        invAddOrDrop(objRepo, boots)
        val name = ServerCacheManager.getItem(boots.asRSCM(RSCMType.OBJ))?.name
        startDialogue { objbox(boots, "You take a pair of ${name?.lowercase()} from the cradle.") }
    }

    private fun ProtectedAccess.claim(floor: StrongholdFloor) {
        player.markClaimed(floor)
        player.unlockEmote(floor)
        if (floor == StrongholdFloor.Pestilence) {
            restoreAll()
        } else {
            restoreLifeAndPrayer()
        }
    }

    private fun ProtectedAccess.restoreLifeAndPrayer() {
        restoreIfDrained("stat.hitpoints")
        restoreIfDrained("stat.prayer")
    }

    private fun ProtectedAccess.restoreAll() {
        for (stat in ServerCacheManager.getStats().values) {
            restoreIfDrained(RSCM.getReverseMapping(RSCMType.STAT, stat.id))
        }
    }

    private fun ProtectedAccess.restoreIfDrained(stat: String) {
        if (stat(stat) < statBase(stat)) {
            statRestore(stat)
        }
    }

    private fun chestText(floor: StrongholdFloor): String =
        when (floor) {
            StrongholdFloor.War ->
                "You open the Gift of Peace and find 2,000 coins. A great calm washes over you, " +
                    "restoring your hitpoints and prayer points."
            StrongholdFloor.Famine ->
                "You search the Grain of Plenty and find 3,000 coins. You feel nourished, and " +
                    "your hitpoints and prayer points are restored."
            StrongholdFloor.Pestilence ->
                "You open the Box of Health and find 5,000 coins. The medicine inside restores " +
                    "all of your stats."
            StrongholdFloor.Death -> error("The Cradle of Life has no coins.")
        }

    internal companion object {
        const val FancyBoots = "obj.sos_boots"
        const val FightingBoots = "obj.sos_boots2"
        const val FancierBoots = "obj.sos_boots3"
    }
}
