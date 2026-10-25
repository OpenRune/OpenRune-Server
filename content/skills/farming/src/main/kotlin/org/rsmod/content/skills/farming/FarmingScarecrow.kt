package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess

@Singleton
class FarmingScarecrow @Inject constructor(private val farming: FarmingService) {
    suspend fun ProtectedAccess.place(patch: PatchDef) {
        if (patch.kind != PatchKind.FLOWER) return
        val state = farming.state(player, patch)
        if (state.crop != null || state.scarecrow) return mes("There is already something growing in this patch.")
        if (!state.cleared) return mes("This patch needs weeding first.")
        if (invDel(inv, SCARECROW, 1, strict = true).failure) return
        farming.store(player, patch, state.copy(scarecrow = true))
        mes("You place the scarecrow in the flower patch.")
    }

    suspend fun ProtectedAccess.remove(patch: PatchDef) {
        val state = farming.state(player, patch)
        if (!state.scarecrow) return
        if (SPADE !in inv) return mes("You need a spade to do that.")
        if (invAdd(inv, SCARECROW, 1, strict = true).failure) {
            return mes("You don't have enough inventory space to remove the scarecrow.")
        }
        farming.store(player, patch, state.copy(scarecrow = false))
        mes("You remove the scarecrow from the flower patch.")
    }

    companion object {
        const val SCARECROW = "obj.scarecrow_complete"
        private val SCARECROW_ART by lazy {
            val parent = checkNotNull(ServerCacheManager.getObject("loc.farming_flower_patch_1".asRSCM(RSCMType.LOC)))
            val transforms = checkNotNull(parent.multiLoc)
            listOf(
                "loc.farming_scarecrow_weeds_3", "loc.farming_scarecrow_weeds_2",
                "loc.farming_scarecrow_weeds_1", "loc.farming_scarecrow",
            ).map { loc -> transforms.indexOf(loc.asRSCM(RSCMType.LOC)).also { check(it >= 0) } }
        }

        fun transmit(weeds: Int): Int = SCARECROW_ART[weeds.coerceIn(0, PatchState.CLEARED)]
    }
}
