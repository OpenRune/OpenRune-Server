package org.rsmod.content.events.leaguetasks.relics

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

/**
 * Each league's relic tiers, read from its `league_types` struct. A `league_relics:clickzones` slot
 * is `tier * 3 + (relic - 1)`, and a tier's choice is stored as the relic's key (1 to 3) in
 * `varbit.league_relic_selection_<tier>`.
 */
@Singleton
public class LeagueRelics @Inject constructor() {
    private val tiersByLeague: Map<Int, List<RelicTier>> = loadAll()
    private val unlockSpotanims: Map<Int, String> = loadUnlockSpotanims()

    public fun tiers(player: Player): List<RelicTier> =
        tiersByLeague[player.vars["varbit.league_type"]].orEmpty()

    public fun unlockSpotanim(player: Player): String? =
        unlockSpotanims[player.vars["varbit.league_type"]]

    public fun tierAt(player: Player, slot: Int): RelicTier? =
        tiers(player).getOrNull(slot / RELICS_PER_TIER)

    public fun relicAt(player: Player, slot: Int): Relic? =
        tierAt(player, slot)?.relics?.get(slot % RELICS_PER_TIER + 1)

    public fun selected(player: Player, tier: RelicTier): Int = player.vars[selectionVarbit(tier)]

    public fun isTierUnlocked(player: Player, tier: RelicTier): Boolean =
        player.vars["varp.league_points_claimed"] >= tier.points

    public fun select(player: Player, tier: RelicTier, relic: Relic) {
        VarPlayerIntMapSetter.set(player, selectionVarbit(tier), relic.key)
    }

    private fun selectionVarbit(tier: RelicTier): String =
        "varbit.league_relic_selection_${tier.index}"

    public data class RelicTier(
        val index: Int,
        val points: Int,
        val passivesStruct: Int,
        val passives: String,
        val relics: Map<Int, Relic>,
    )

    public data class Relic(val key: Int, val struct: Int, val name: String, val description: String)

    private companion object {
        const val RELICS_PER_TIER = 3

        fun loadAll(): Map<Int, List<RelicTier>> {
            val tiersParam = "param.league_relic_tiers".asRSCM(RSCMType.PARAM)
            val leagues = mutableMapOf<Int, List<RelicTier>>()
            for ((type, structId) in enum<Int, Int>("league_types")) {
                val league = structId?.let(ServerCacheManager::getStruct) ?: continue
                val tierEnum = league.params?.get(tiersParam) as? Int ?: continue
                leagues[type] =
                    enum<Int, Int>(tierEnum).mapNotNull { (index, tierStruct) ->
                        tierStruct?.let { tier(index, it) }
                    }
            }
            return leagues
        }

        fun loadUnlockSpotanims(): Map<Int, String> {
            val spotanimParam = "param.league_relic_unlock_spotanim".asRSCM(RSCMType.PARAM)
            val spotanims = mutableMapOf<Int, String>()
            for ((type, structId) in enum<Int, Int>("league_types")) {
                val league = structId?.let(ServerCacheManager::getStruct) ?: continue
                val spotanim = league.params?.get(spotanimParam) as? Int ?: continue
                spotanims[type] = RSCM.getReverseMapping(RSCMType.SPOTANIM, spotanim)
            }
            return spotanims
        }

        fun tier(index: Int, structId: Int): RelicTier? {
            val struct = ServerCacheManager.getStruct(structId) ?: return null
            val relicEnum =
                struct.params?.get("param.league_relic_tier_relics".asRSCM(RSCMType.PARAM)) as? Int
                    ?: return null
            val passivesStruct =
                struct.params?.get("param.league_relic_tier_passives".asRSCM(RSCMType.PARAM)) as? Int
            val passives =
                passivesStruct
                    ?.let(ServerCacheManager::getStruct)
                    ?.getString("param.league_relic_passive_description".asRSCM(RSCMType.PARAM))
                    .orEmpty()
            val relics =
                enum<Int, Int>(relicEnum).mapNotNull { (key, relicStruct) ->
                    relicStruct?.let { relic(key, it) }?.let { key to it }
                }
            return RelicTier(
                index = index,
                points = struct.getInt("param.league_relic_tier_points".asRSCM(RSCMType.PARAM)),
                passivesStruct = passivesStruct ?: -1,
                passives = passives,
                relics = relics.toMap(),
            )
        }

        fun relic(key: Int, structId: Int): Relic? {
            val struct = ServerCacheManager.getStruct(structId) ?: return null
            return Relic(
                key = key,
                struct = structId,
                name = struct.getString("param.league_relic_name".asRSCM(RSCMType.PARAM)),
                description =
                    struct.getString("param.league_relic_description".asRSCM(RSCMType.PARAM)),
            )
        }
    }
}
