package org.rsmod.content.areas.revenants

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.*
import org.rsmod.api.player.hook.PlayerTeleportDelayHook
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.content.areas.wilderness.applyForinthrySurge
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.module.PluginModule

class RevenantModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerTeleportDelayHook>(RevenantTeleportDelay::class.java)
        addSetBinding<WeaponMap>(RevenantWeapons::class.java)
        addSetBinding<NpcDeathKillHook>(RevenantMaledictusController::class.java)
        addSetBinding<PlayerDeathDropHook>(RevenantDeathHook::class.java)
        addSetBinding<NpcLootQuantityHook>(RevenantEtherHook::class.java)
        addSetBinding<PvPAttackValidateHook>(RevenantPvpHook::class.java)
        addSetBinding<PvPSkullHook>(RevenantPvpHook::class.java)
        addSetBinding<NpcAttackValidateHook>(RevenantAttackHook::class.java)
    }
}

internal class RevenantEtherHook @Inject constructor() : NpcLootQuantityHook {
    override fun remaining(player: Player, npc: Npc, item: ItemServerType, count: Int): Int =
        if (item.id == RevenantCharges.ETHER.asRSCM() && (RevenantSpecies.of(npc) != null || npc.type.id == RevenantSpecies.MALEDICTUS.asRSCM())) RevenantCharges.absorb(player, count)
        else count
}

internal class RevenantDeathHook @Inject constructor() : PlayerDeathDropHook {
    override fun valueObj(item: InvObj): InvObj = RevenantCharges.kind(item)?.let { item.copy(id = it.empty.asRSCM(), vars = 0) } ?: item
    override fun resolve(context: PlayerDeathContext, result: PlayerDeathDrops.DeathDropResult): PlayerDeathDrops.DeathDropResult {
        val extra = mutableListOf<InvObj>()
        fun transform(item: InvObj): InvObj {
            val kind = RevenantCharges.kind(item) ?: return item
            val count = RevenantCharges.count(item)
            if (kind != RevenantCharges.bracelet && !context.inWilderness) return item
            // Craw's deposit disappears on death; other Wilderness weapons drop their ether.
            val ether = (count - if (kind.charged == "obj.wild_cave_bow_charged") kind.deposit else 0).coerceAtLeast(0)
            if (ether > 0) extra += InvObj(RevenantCharges.ETHER, ether)
            return RevenantCharges.withCount(item, 0)
        }
        val kept = result.kept.map { transform(it) }
        val lostTradeable = result.lostTradeable.map { transform(it) }.toMutableList()
        val lostUntradeable = result.lostUntradeable.map { transform(it) }.partition { getInvObj(it).tradeable }
        lostTradeable += lostUntradeable.first
        if ((context.inRevenantCaves || context.inWilderness && context.isPvpDeath) && context.player.revenantAdmission and 1 != 0 && context.killer != null) {
            extra += InvObj("obj.coins", RevenantAccessScript.FEE)
        }
        return result.copy(kept = kept, lostTradeable = lostTradeable + extra, lostUntradeable = lostUntradeable.second)
    }
    override fun complete(context: PlayerDeathContext) {
        if (context.inRevenantCaves || context.inWilderness && context.isPvpDeath) context.player.revenantAdmission = context.player.revenantAdmission and 1.inv()
    }
    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val transfer = context.inWilderness && context.isPvpDeath && context.player.worn[dev.openrune.util.Wearpos.Front.slot]?.id == "obj.wild_cave_amulet".asRSCM() && context.player.vars["varp.forinthry_surge_expiration"] > context.player.currentMapClock
        return { complete(context); if (transfer) context.killer?.applyForinthrySurge() }
    }
}

internal class RevenantPvpHook @Inject constructor(private val areas: AreaChecker, private val npcs: NpcList) : PvPAttackValidateHook, PvPSkullHook {
    override fun validate(attacker: Player, target: Player): PvPAttackValidateResult =
        if (areas.inArea("area.revenant_caves", attacker.coords) && areas.inArea("area.revenant_caves", target.coords)) PvPAttackValidateResult.BypassSingleWayPvnRestriction
        else PvPAttackValidateResult.Pass
    override fun onPlayerAttack(attacker: Player, target: Player) {
        if (!areas.inArea("area.revenant_caves", target.coords)) return
        for (player in listOf(attacker, target)) {
            val uid = player.vars["varp.aggressive_npc"]
            val npc = npcs.firstOrNull { it.uid.packed == uid }
            if (npc != null && npc.type.id != RevenantSpecies.MALEDICTUS.asRSCM() && areas.inArea("area.revenant_caves", npc.coords)) {
                npc.interaction = null; npc.mode = NpcMode.None; npc.vars["varn.aggressive_player"] = 0
                VarPlayerIntMapSetter.set(player, "varp.aggressive_npc", 0)
            }
        }
    }
}

internal class RevenantAttackHook @Inject constructor() : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        if (npc.type.id == RevenantSpecies.MALEDICTUS.asRSCM()) NpcAttackValidateResult.BypassSingleWayPvnRestriction
        else NpcAttackValidateResult.Pass
}
