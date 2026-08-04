package org.rsmod.content.skills.shootingstars.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.shootingstars.ALL_TIME_TOTAL_DUST
import org.rsmod.content.skills.shootingstars.MINING_STAR
import org.rsmod.content.skills.shootingstars.SEEN_SHOOTING_STAR
import org.rsmod.content.skills.shootingstars.ShootingStarManager
import org.rsmod.content.skills.shootingstars.ShootingStarStages
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj

@Singleton
class StarMiningScript
@Inject
constructor(
    private val stars: ShootingStarManager,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) {
    fun ProtectedAccess.attempt(rock: BoundLocInfo) {
        if (!stars.isActiveStar(rock)) {
            mes("The star is no longer here.")
            return
        }

        stars.noteDiscovery(player)

        val stage = stars.currentStage()
        if (player.miningLvl < stage.miningLevel) {
            stars.chipUnderLevelProgress()
            mes(
                "This is a size-${stage.size} star. A Mining level of at least " +
                    "${stage.miningLevel} is required to mine this layer. " +
                    "It has been mined ${stars.percentageToNextLevel()}% of the way to the next layer.",
            )
            return
        }
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more stardust.")
            soundSynth("synth.pillory_wrong")
            return
        }
        if (Mining.findPickaxe(player) == null) {
            mes("You need a pickaxe to mine this rock.")
            return
        }

        stars.startLayerMining()
        player.attr[MINING_STAR] = true
        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
            skillAnimDelay = mapClock + 3
            opLoc1(rock)
        } else {
            val pickaxe = Mining.findPickaxe(player) ?: return
            anim(pickaxeAnim(pickaxe))
            spam("You swing your pickaxe at the rock.")
            mine(rock)
        }
    }

    fun ProtectedAccess.mine(rock: BoundLocInfo) {
        if (!stars.isActiveStarCoords(rock.coords)) {
            stopMining("The star is no longer here.")
            return
        }

        val live = stars.currentBoundLoc() ?: run {
            stopMining("The star is no longer here.")
            return
        }

        val stage = stars.currentStage()
        if (player.miningLvl < stage.miningLevel) {
            stopMining(
                "This is a size-${stage.size} star. A Mining level of at least " +
                    "${stage.miningLevel} is required to mine this layer.",
            )
            return
        }

        val pickaxe = Mining.findPickaxe(player)
        if (pickaxe == null) {
            stopMining("You need a pickaxe to mine this rock.")
            return
        }

        if (inv.isFull()) {
            soundSynth("synth.pillory_wrong")
            stopMining("Your inventory is too full to hold any more stardust.")
            return
        }

        stars.startLayerMining()
        player.attr[MINING_STAR] = true

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(pickaxeAnim(pickaxe))
        }

        val delay = Mining.pickaxeActionDelay(pickaxe, random)
        var success = false
        if (actionDelay < mapClock) {
            actionDelay = mapClock + delay
        } else if (actionDelay == mapClock) {
            actionDelay = mapClock + delay
            success = statRandom("stat.mining", SUCCESS_LOW, SUCCESS_HIGH, invisibleLvls)
        }

        if (success) {
            val amount = stars.stardustAmountFor(player)
            if (!awardStardust(amount)) {
                stopMining(null)
                return
            }
            stars.consumeDiscovererBonus(player)
            if (!stars.active) {
                stopMining(null)
                return
            }
            val next = stars.currentBoundLoc() ?: run {
                stopMining(null)
                return
            }
            opLoc3(next)
            return
        }

        opLoc3(live)
    }

    private fun ProtectedAccess.awardStardust(amount: Int): Boolean {
        val added = invAdd(inv, "obj.star_dust", amount)
        if (added.failure) {
            mes("Your inventory is too full to hold any more stardust.")
            return false
        }

        player.attr[ALL_TIME_TOTAL_DUST] = (player.attr[ALL_TIME_TOTAL_DUST] ?: 0) + amount
        player.attr[SEEN_SHOOTING_STAR] = true

        val baseXp =
            if (player.members) ShootingStarStages.XP_MEMBERS else ShootingStarStages.XP_F2P
        val xp = baseXp * xpMods.get(player, "stat.mining")
        statAdvance("stat.mining", xp)
        spam("You manage to mine some stardust.")
        soundSynth(3600)
        return true
    }

    private fun ProtectedAccess.stopMining(message: String?) {
        player.attr[MINING_STAR] = false
        message?.let(::mes)
        resetAnim()
    }

    private fun pickaxeAnim(pickaxe: InvObj): String {
        val seq = with(Mining.Companion) { getInvObj(pickaxe).pickaxeAnim }
        return RSCM.getReverseMapping(RSCMType.SEQ, seq.id)
    }

    companion object {
        private const val SUCCESS_LOW = 74
        private const val SUCCESS_HIGH = 118
    }
}
