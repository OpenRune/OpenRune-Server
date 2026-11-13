package org.rsmod.content.other.treasuretrails

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import kotlin.math.floor
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.enums.ChanceEnums
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.table.TrailTierRow
import org.rsmod.content.drops.hasUnlockedEasyCombat
import org.rsmod.content.drops.hasUnlockedEliteCombat
import org.rsmod.content.drops.hasUnlockedHardCombat
import org.rsmod.content.drops.hasUnlockedMediumCombat
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

object ClueVesselOdds {
    private const val GEODE_FACTOR = 0.7
    private const val COMBAT_ACHIEVEMENT_FACTOR = 0.95

    /**
     * One in the returned number. [baseChance] is the skilling pet rate for the resource, adjusted
     * by the combat achievement tier before the level and clue tier terms. [modifierTenths] is the
     * tier's `vessel_modifier`.
     */
    fun denominator(
        baseChance: Int,
        level: Int,
        modifierTenths: Int,
        geode: Boolean,
        combatAchievement: Boolean,
        wildernessWealth: Boolean,
    ): Int {
        var base = baseChance
        if (geode) base = floor(base * GEODE_FACTOR).toInt()
        if (combatAchievement) base = floor(base * COMBAT_ACHIEVEMENT_FACTOR).toInt()
        var denominator = floor(base.toDouble() / (100 + level) * modifierTenths / 10).toInt()
        if (wildernessWealth) denominator /= 2
        return denominator.coerceAtLeast(1)
    }
}

/**
 * Rolls every clue tier the player may receive, independently, on each skilling product of mining
 * (geodes), woodcutting (nests) and fishing (bottles). Base chances are the pet chances of the
 * same resource. Once X Marks the Spot is done the vessel is a scroll box instead.
 */
class ClueVesselScript
@Inject
constructor(
    private val random: GameRandom,
    private val objRepo: ObjRepository,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    private class Source(
        val skill: String,
        val chances: Map<String, Int>,
        val geode: Boolean,
        val ground: Boolean,
        val kind: String,
        val vessel: (TrailTierRow) -> ItemServerType?,
    )

    private val sources: Map<String, Source> =
        listOf(
                Source(
                    skill = "stat.mining",
                    chances = chances(ChanceEnums.rock_golem_chance.backing),
                    geode = true,
                    ground = false,
                    kind = "geode",
                    vessel = { it.geodeObj },
                ),
                Source(
                    skill = "stat.woodcutting",
                    chances = chances(ChanceEnums.beaver_chance.backing),
                    geode = false,
                    ground = true,
                    kind = TrailFlow.NEST,
                    vessel = { it.nestObj },
                ),
                Source(
                    skill = "stat.fishing",
                    chances = chances(ChanceEnums.heron_pet_chance.backing),
                    geode = false,
                    ground = false,
                    kind = "bottle",
                    vessel = { it.bottleObj },
                ),
            )
            .associateBy { it.skill }

    override fun ScriptContext.startup() {
        onEvent<SkillingProductPrepareEvent> { roll(product) }
        for (tier in TrailTiers.playableTiers()) {
            for (source in sources.values) {
                val vessel = source.vessel(tier) ?: continue
                onOpHeld1(vessel.symbol()) { with(TrailFlow) { openVessel(objRepo, tier, vessel, source.kind) } }
            }
        }
    }

    private fun roll(product: SkillingProduct) {
        if (product.cancelled || product.isBonus) return
        val source = sources[product.skill] ?: return
        val base = source.chances[product.item] ?: return
        val player = product.player
        val boxes = ClueTakeHook.boxesUnlocked(player)
        val wildernessWealth =
            "obj.ring_of_wealth_i" in player.worn && player.coords.isInWilderness(areaChecker)
        val level = player.stat(product.skill)
        for (tier in TrailTiers.playableTiers()) {
            val vessel = source.vessel(tier) ?: continue
            if (!eligible(player, tier, vessel, boxes)) continue
            val denominator =
                ClueVesselOdds.denominator(
                    baseChance = base,
                    level = level,
                    modifierTenths = tier.vesselModifier ?: continue,
                    geode = source.geode,
                    combatAchievement = combatAchievement(player, tier),
                    wildernessWealth = wildernessWealth,
                )
            if (random.of(denominator) != 0) continue
            deliver(player, source, if (boxes) tier.boxObj else vessel)
        }
    }

    private fun eligible(
        player: Player,
        tier: TrailTierRow,
        vessel: ItemServerType,
        boxes: Boolean,
    ): Boolean {
        if (boxes) return TrailTiers.canReceive(player, tier, boxes = true)
        return TrailTiers.ownedClues(player, tier.difficulty) == 0 && !TrailTiers.owns(player, vessel)
    }

    private fun combatAchievement(player: Player, tier: TrailTierRow): Boolean =
        when (tier.difficulty) {
            TrailTiers.EASY -> player.hasUnlockedEasyCombat()
            TrailTiers.MEDIUM -> player.hasUnlockedMediumCombat()
            TrailTiers.HARD -> player.hasUnlockedHardCombat()
            TrailTiers.ELITE -> player.hasUnlockedEliteCombat()
            else -> false
        }

    private fun deliver(player: Player, source: Source, item: ItemServerType) {
        if (source.ground) {
            objRepo.add(item.symbol(), player.coords, duration = NEST_DESPAWN, receiver = player)
            player.mes("<col=ff0000>A bird's nest falls out of the tree.</col>")
            return
        }
        if (player.invAddOrDrop(objRepo, item.symbol())) {
            player.mes("You find a ${label(item, source)}!")
        } else {
            player.mes("A ${label(item, source)} falls to the ground as your inventory is too full.")
        }
    }

    private fun label(item: ItemServerType, source: Source): String =
        if (TrailTiers.boxDifficulty(item) != null) "scroll box" else "clue ${source.kind}"

    private fun chances(map: Map<ItemServerType, Int?>): Map<String, Int> =
        map.mapNotNull { (item, base) -> base?.let { item.internalName to it } }.toMap()

    private companion object {
        const val NEST_DESPAWN = 200
    }
}
