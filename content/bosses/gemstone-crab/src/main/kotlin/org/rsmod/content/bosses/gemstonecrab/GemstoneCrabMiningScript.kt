package org.rsmod.content.bosses.gemstonecrab

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj

@Singleton
public class GemstoneCrabMiningScript
@Inject
constructor(private val crab: GemstoneCrabManager, private val objRepo: ObjRepository) {

    public suspend fun ProtectedAccess.attempt(npc: Npc) {
        val uuid = player.uuid ?: return
        if (crab.remainsNpc !== npc) {
            mes("The crab shell has already crumbled away.")
            return
        }
        if (!crab.isEligibleMiner(uuid)) {
            mes("Your understanding of the gemstone crab is not great enough to mine its shell.")
            return
        }
        if (crab.hasClaimedMining(uuid)) {
            mes("You have already taken your share of this crab's shell.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space to mine this.")
            return
        }
        val pickaxe = Mining.findPickaxe(player)
        if (pickaxe == null) {
            mes("You need a pickaxe to mine this.")
            return
        }

        anim(pickaxeAnim(pickaxe))
        spam("You swing your pick at the crab shell.")
        delay(MINE_DELAY_TICKS)

        if (crab.remainsNpc !== npc) {
            mes("The crab shell has already crumbled away.")
            return
        }
        if (!crab.claimMining(uuid)) {
            return
        }

        for (drop in GemstoneCrabGems.rollAll(player)) {
            val gem = drop.obj
            val count = drop.rollCount(random)
            val gemName = gem.substringAfterLast('.').removePrefix("uncut_").replace('_', ' ')
            if (invAddOrDrop(objRepo, gem, count)) {
                spam("You mine an uncut $gemName from the crab shell.")
            } else {
                mes("You mine an uncut $gemName, but it falls to the floor.")
            }
        }
    }

    private fun pickaxeAnim(pickaxe: InvObj): String {
        val seq = with(Mining.Companion) { getInvObj(pickaxe).pickaxeAnim }
        return RSCM.getReverseMapping(RSCMType.SEQ, seq.id) ?: "seq.human_mine_bronze_pickaxe"
    }

    private companion object {
        private const val MINE_DELAY_TICKS = 3
    }
}
