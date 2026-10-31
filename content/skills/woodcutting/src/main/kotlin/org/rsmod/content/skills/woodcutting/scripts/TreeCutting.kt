package org.rsmod.content.skills.woodcutting.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.skilling.SkillingAwardResult
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.cutSuccessRates
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.findAxe
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.hasBlockedCrystalAxe
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.hasBlockedInfernalAxe
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLevelReq
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeXp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj

/** The per-attempt cutting rules, shared by wild trees and farmed trees. */
@Singleton
class TreeCutting
@Inject
constructor(private val xpMods: XpModifiers, private val invisibleLvls: InvisibleLevels) {
    /** Level and free-slot checks; messages the player on failure. */
    fun ProtectedAccess.canCut(type: ObjectServerType): Boolean {
        if (player.woodcuttingLvl < type.treeLevelReq) {
            mes("You need a Woodcutting level of ${type.treeLevelReq} to chop down this tree.")
            return false
        }

        if (inv.isFull()) {
            val product = type.treeLogs
            mes("Your inventory is too full to hold any more ${product.name.lowercase()}.")
            soundSynth("synth.pillory_wrong")
            return false
        }
        return true
    }

    /** Starts the swing anim and message; false (with a message) when there is no usable axe. */
    fun ProtectedAccess.beginSwing(type: ObjectServerType): Boolean {
        val axe = findAxe(player, type)
        if (axe == null) {
            mesAxeMissing()
            return false
        }
        anim(axeAnim(axe))
        spam("You swing your axe at the tree.")
        return true
    }

    /**
     * One tick of cutting. [depletes] runs every tick after the success roll and says whether the
     * tree is used up; [onDeplete] then runs after the logs are awarded. Returns false when a
     * precondition (axe, level, inventory space) stopped the cut.
     */
    fun ProtectedAccess.cutTick(
        tree: BoundLocInfo,
        type: ObjectServerType,
        depletes: (cutLogs: Boolean) -> Boolean,
        onDeplete: () -> Unit,
    ): Boolean {
        val axe = findAxe(player, type)
        if (axe == null) {
            mesAxeMissing()
            return false
        }

        if (!canCut(type)) {
            return false
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(axeAnim(axe))
        }

        var cutLogs = false

        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
        } else if (actionDelay == mapClock) {
            val (low, high) = cutSuccessRates(type, axe)
            cutLogs = statRandom("stat.woodcutting", low, high, invisibleLvls)
        }

        val despawn = depletes(cutLogs)

        if (cutLogs) {
            val logs = type.treeLogs
            val xp = type.treeXp * xpMods.get(player, "stat.woodcutting")
            val product =
                SkillingProduct(
                    player = player,
                    skill = "stat.woodcutting",
                    item = RSCM.getReverseMapping(RSCMType.OBJ, logs.id),
                    count = 1,
                    experience = xp,
                    grantsExperience = true,
                    source = SkillingProductSource.Woodcutting(tree, logs),
                )
            if (awardSkillingProduct(product) == SkillingAwardResult.Success) {
                spam("You get some ${logs.name.lowercase()}.")
            }
        }

        if (despawn) {
            onDeplete()
        }
        return true
    }

    private fun ProtectedAccess.axeAnim(axe: InvObj): String =
        RSCM.getReverseMapping(RSCMType.SEQ, getInvObj(axe).axeWoodcuttingAnim.id)

    private fun ProtectedAccess.mesAxeMissing() {
        mes("You need an axe to chop down this tree.")
        when {
            player.hasBlockedInfernalAxe() ->
                mes("You need a Firemaking level of 85 to use the infernal axe.")
            player.hasBlockedCrystalAxe() ->
                mes("You need to complete Song of the Elves to use this axe.")
            else -> mes("You do not have an axe which you have the woodcutting level to use.")
        }
    }
}
