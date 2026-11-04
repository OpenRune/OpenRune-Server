package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.zone.ZoneKey

class FarmingBagHerblore @Inject constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val locInteractions: LocInteractions,
    private val npcInteractions: NpcInteractions,
) {
    fun reagents(player: Player): Inventory? {
        if (FarmingBags.reagentPouch.openHeld(player) == null) return null
        return if (nearBank(player)) FarmingBags.reagentPouch.store(player) else null
    }

    // shortcut: Standard-bank eligibility is unsourced; extend when its exact predicate is verified.
    private fun nearBank(player: Player): Boolean {
        val zone = ZoneKey.from(player.coords)
        for (npc in npcRepo.findAll(zone, zoneRadius = 1)) {
            if (npc.coords.level != player.coords.level || player.distanceTo(npc) > 1) continue
            val base = npc.visType
            val type = npcInteractions.multiNpc(base, player.vars)
                ?: base.takeUnless { !it.transforms.isNullOrEmpty() || it.multiDefault > 0 }
                ?: continue
            if (type.isType("npc.roguesden_banker") &&
                (0..4).any { type.actions.getOpOrNull(it) == "Bank" }
            ) return true
        }
        for (dx in -1..1) {
            val x = zone.x + dx
            if (x !in 0..ZoneKey.X_BIT_MASK) continue
            for (dz in -1..1) {
                val z = zone.z + dz
                if (z !in 0..ZoneKey.Z_BIT_MASK) continue
                for (loc in locRepo.findAll(ZoneKey(x = x, z = z, level = zone.level))) {
                    val base = ServerCacheManager.getObject(loc.id) ?: continue
                    val bound = BoundLocInfo(loc, base)
                    val shown = locInteractions.multiLoc(bound, base, player.vars)
                    if (shown == null && (!base.transforms.isNullOrEmpty() || base.multiDefault > 0)) {
                        continue
                    }
                    val type = shown?.let { ServerCacheManager.getObject(it.id) } ?: base
                    val visible = shown?.let { BoundLocInfo(loc, type) } ?: bound
                    if (player.distanceTo(visible) > 1) continue
                    if (type.isContentType("content.bank_chest") ||
                        (type.category == BANK_CHEST_CATEGORY &&
                            (0..4).any { type.actions.getOpOrNull(it) == "Use" })
                    ) return true
                }
            }
        }
        return false
    }

    private companion object {
        val BANK_CHEST_CATEGORY by lazy {
            checkNotNull(ServerCacheManager.getObject("loc.castlewars_bankchest".asRSCM(RSCMType.LOC)))
                .category.also { check(it >= 0) }
        }
    }
}
