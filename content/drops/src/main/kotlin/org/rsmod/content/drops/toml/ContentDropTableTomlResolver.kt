package org.rsmod.content.drops.toml

import dtx.rs.RSWeightedTable
import jakarta.inject.Singleton
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.PendingDropItemConfig
import org.rsmod.api.droptable.toml.DropTableTomlResolver
import org.rsmod.api.droptable.toml.TomlDropHooks
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import dtx.rs.RSPrerollTableBuilder
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.droptable.wearingRingOfWealth
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

@Singleton
public class ContentDropTableTomlResolver @Inject constructor(private val areaChecker: AreaChecker) : DropTableTomlResolver {
    override fun sharedTable(name: String): RSWeightedTable<Player, DropRollItem> {
        val table =
            SHARED_TABLES[name]
                ?: error(
                    "Unknown shared drop table '$name'. " +
                        "Expected one of: ${SHARED_TABLES.keys.sorted().joinToString()}",
                )
        return table
    }

    override fun applyBrimstoneKeyRoll(
        builder: RSPrerollTableBuilder<Player, DropRollItem>,
        konarTaskBonus: Boolean,
    ) {
        builder.brimstoneKeyRoll(konarTaskBonus = konarTaskBonus)
    }

    override fun applyHooks(config: PendingDropItemConfig, hooks: TomlDropHooks) {
        if (hooks.shouldDropLootingBag) {
            config.condition = andCondition(config.condition) { player -> player.shouldDropLootingBag() }
        } else if (hooks.quest != null) {
            val quest = hooks.quest!!
            val questCondition: (Player) -> Boolean =
                when (hooks.questMode?.lowercase()) {
                    "completed" -> { player -> player.hasCompletedQuest(quest) }
                    "not_completed" -> { player -> !player.hasCompletedQuest(quest) }
                    "during", null -> { player -> player.isOnQuest(quest) }
                    else ->
                        error(
                            "Invalid quest_mode '${hooks.questMode}' for quest '$quest'. " +
                                "Use 'during', 'completed', or 'not_completed'.",
                        )
                }
            config.condition = andCondition(config.condition, questCondition)
        }
        if (hooks.requireRingOfWealth) {
            config.condition = andCondition(config.condition) { player -> player.wearingRingOfWealth() }
        }
        if (hooks.excludeRingOfWealth) {
            config.condition = andCondition(config.condition) { player -> !player.wearingRingOfWealth() }
        }
        if (hooks.requireWilderness) {
            config.condition = andCondition(config.condition) { player -> player.coords.isInWilderness(areaChecker) }
        }
        if (hooks.shouldDropBrimstoneKey) {
            config.killCondition = { player, npc, areaChecker ->
                player.shouldDropBrimstoneKey(npc, areaChecker)
            }
        }
        if (hooks.clueScrollBox) {
            val scrollObj = config.obj
            config.transformObj = { player -> player.clueScrollTransformObj(scrollObj) }
        }
    }

    private fun andCondition(
        existing: (Player) -> Boolean,
        next: (Player) -> Boolean,
    ): (Player) -> Boolean = { player -> existing(player) && next(player) }

    private companion object {
        private val SHARED_TABLES: Map<String, RSWeightedTable<Player, DropRollItem>> =
            mapOf(
                "herb" to SharedDropTables.herb,
                "usefulHerb" to SharedDropTables.usefulHerb,
                "combatHerb" to SharedDropTables.combatHerb,
                "gem" to SharedDropTables.gem,
                "seed" to SharedDropTables.seed,
                "rareSeed" to SharedDropTables.rareSeed,
                "megaRare" to SharedDropTables.megaRare,
                "rareDrop" to SharedDropTables.rareDrop,
            )
    }
}
