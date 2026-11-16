package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType

@Singleton
class FarmingCompostService @Inject constructor(private val farming: FarmingService) {
    fun handles(obj: String): Boolean = FarmingComposts.byObj(obj) != null

    suspend fun ProtectedAccess.apply(patch: PatchDef, obj: String, slot: Int) {
        val selected = inv[slot]?.takeIf { it.isType(obj) } ?: return
        val kind = FarmingComposts.byObj(obj)?.kind ?: return
        anim(ANIM_POUR_COMPOST)
        soundSynth(SOUND_COMPOST)
        delay(2)
        val result = farming.treat(player, patch, kind) { inv[slot] == selected && emptyBucket(player, slot, selected) }
        when (result) {
            TreatResult.WEEDS -> mes("The patch needs to be raked first.")
            TreatResult.ALREADY -> mes("This patch has already been treated.")
            TreatResult.DEAD, TreatResult.GROWN -> mes("You can't treat this crop with compost now.")
            TreatResult.UNPAID, TreatResult.STALE -> Unit
            TreatResult.TREATED -> {
                farming.awardPatchXp(player, patch, FarmingComposts.get(kind).xp)
                mes("You treat the ${patch.kind.label} with ${kind.label}.")
            }
        }
    }

    private fun emptyBucket(player: Player, slot: Int, expected: InvObj): Boolean =
        player.invTransaction(player.inv) {
            val target = select(player.inv)
            delete(target, expected.id, 1, slot)
            add(target, EMPTY_BUCKET.asRSCM(), 1, slot = slot)
        }.success

    private companion object {
        const val EMPTY_BUCKET = "obj.bucket_empty"
    }
}
