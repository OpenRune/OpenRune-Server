package org.rsmod.content.other.treasuretrails

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.hook.PrayerEnableHook
import org.rsmod.api.player.legs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.torso
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.cluehelper.CluehelperClueSkillchallengeRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

/** Sherlock challenges that are completed by doing something rather than handing in an item. */
object ClueChallengeTasks {
    const val DONE = "varp.trail_challenge_done"
    const val COMPLETED_MESSAGE = "You have completed your elite/master level challenge!"

    internal const val BURN_YEW = "dbrow.cluehelper_skillchallenge_elite_18"
    internal const val CHIVALRY = "dbrow.cluehelper_skillchallenge_elite_22"
    internal const val DUST_DEVIL = "dbrow.cluehelper_skillchallenge_elite_10"
    internal const val COSMIC_RUNES = "dbrow.cluehelper_skillchallenge_elite_20"
    internal const val SHAYZIEN_PLATEBODY = "dbrow.cluehelper_skillchallenge_elite_23"
    internal const val NICKEL = "dbrow.cluehelper_skillchallenge_elite_nickel"

    internal const val SLAY_NECHRYAEL = "dbrow.cluehelper_skillchallenge_master_4"
    internal const val SLAY_LIZARDMAN_SHAMAN = "dbrow.cluehelper_skillchallenge_master_19"
    internal const val SLAY_FIYR_SHADE = "dbrow.cluehelper_skillchallenge_master_24"
    internal const val EQUIP_WHIP = "dbrow.cluehelper_skillchallenge_master_0"

    private val PRODUCT_TASKS =
        mapOf(
            "obj.rune_med_helm" to "dbrow.cluehelper_skillchallenge_master_1",
            "obj.unstrung_dragonstone_amulet" to "dbrow.cluehelper_skillchallenge_master_6",
            "obj.antivenom1" to "dbrow.cluehelper_skillchallenge_master_10",
            "obj.antivenom2" to "dbrow.cluehelper_skillchallenge_master_10",
            "obj.antivenom3" to "dbrow.cluehelper_skillchallenge_master_10",
            "obj.antivenom4" to "dbrow.cluehelper_skillchallenge_master_10",
            "obj.bloodrune" to "dbrow.cluehelper_skillchallenge_master_14",
            "obj.brutal_1doserangerspotion" to "dbrow.cluehelper_skillchallenge_master_15",
            "obj.brutal_2doserangerspotion" to "dbrow.cluehelper_skillchallenge_master_15",
        )

    private val BURN_TASKS =
        mapOf(
            "obj.yew_logs" to BURN_YEW,
            "obj.magic_logs" to "dbrow.cluehelper_skillchallenge_master_7",
            "obj.redwood_logs" to "dbrow.cluehelper_skillchallenge_master_8",
        )

    private val GEAR_TASKS =
        mapOf(
            "obj.runite_ore" to
                GearTask("dbrow.cluehelper_skillchallenge_master_11", ::wearsProspector),
            "obj.raw_anglerfish" to
                GearTask("dbrow.cluehelper_skillchallenge_master_20", ::wearsAngler),
            "obj.redwood_logs" to
                GearTask("dbrow.cluehelper_skillchallenge_master_21", ::wearsLumberjack),
        )

    private class GearTask(val row: String, val worn: (Player) -> Boolean)

    private fun wearsProspector(player: Player): Boolean =
        player.torso.isType("obj.varrock_armour_elite") ||
            wearsSet(
                player,
                hat = listOf("obj.motherlode_reward_hat", "obj.motherlode_reward_hat_gold"),
                top = listOf("obj.motherlode_reward_top", "obj.motherlode_reward_top_gold"),
                legs = listOf("obj.motherlode_reward_legs", "obj.motherlode_reward_legs_gold"),
                boots = listOf("obj.motherlode_reward_boots", "obj.motherlode_reward_boots_gold"),
            )

    private fun wearsAngler(player: Player): Boolean =
        wearsSet(
            player,
            hat = listOf("obj.spirit_angler_hat", "obj.trawler_reward_hat"),
            top = listOf("obj.spirit_angler_top", "obj.trawler_reward_top"),
            legs = listOf("obj.spirit_angler_legs", "obj.trawler_reward_legs"),
            boots = listOf("obj.spirit_angler_boots", "obj.trawler_reward_boots"),
        )

    private fun wearsLumberjack(player: Player): Boolean =
        wearsSet(
            player,
            hat = listOf("obj.forestry_lumberjack_hat", "obj.ramble_lumberjack_hat"),
            top = listOf("obj.forestry_lumberjack_top", "obj.ramble_lumberjack_top"),
            legs = listOf("obj.forestry_lumberjack_legs", "obj.ramble_lumberjack_legs"),
            boots = listOf("obj.forestry_lumberjack_boots", "obj.ramble_lumberjack_boots"),
        )

    private fun wearsSet(
        player: Player,
        hat: List<String>,
        top: List<String>,
        legs: List<String>,
        boots: List<String>,
    ): Boolean =
        hat.any { player.hat.isType(it) } &&
            top.any { player.torso.isType(it) } &&
            legs.any { player.legs.isType(it) } &&
            boots.any { player.feet.isType(it) }

    private val actionSteps: Set<Int> =
        (listOf(
                BURN_YEW,
                CHIVALRY,
                DUST_DEVIL,
                COSMIC_RUNES,
                SHAYZIEN_PLATEBODY,
                NICKEL,
                SLAY_NECHRYAEL,
                SLAY_LIZARDMAN_SHAMAN,
                SLAY_FIYR_SHADE,
                EQUIP_WHIP,
            ) +
                PRODUCT_TASKS.values +
                BURN_TASKS.values +
                GEAR_TASKS.values.map { it.row })
            .map { CluehelperClueSkillchallengeRow.getRow(it).rowId }
            .toSet()

    fun isAction(step: ClueStep): Boolean = step.rowId in actionSteps

    fun isDone(player: Player, step: ClueStep): Boolean = player.vars[DONE] == step.rowId

    fun complete(player: Player, task: String) {
        val rowId = CluehelperClueSkillchallengeRow.getRow(task).rowId
        val held =
            TrailFlow.heldTrails(player).any { it.step.kind == ClueKind.SkillChallenge && it.step.rowId == rowId }
        if (held && player.vars[DONE] != rowId) {
            VarPlayerIntMapSetter.set(player, DONE, rowId)
            player.mes(COMPLETED_MESSAGE)
        }
    }

    fun onSkilling(player: Player, context: SkillingActionContext) {
        if (context is SkillingActionContext.Firemaking) {
            BURN_TASKS[context.log]?.let { complete(player, it) }
        }
        if (context !is SkillingActionContext.Product || context.isBonus) return
        val name = ServerCacheManager.getItem(context.item.asRSCM(RSCMType.OBJ))?.name
        PRODUCT_TASKS[context.item]?.let { complete(player, it) }
        GEAR_TASKS[context.item]?.let { if (it.worn(player)) complete(player, it.row) }
        when {
            context.item == NICKEL_ORE -> complete(player, NICKEL)
            context.item == COSMIC_RUNE && context.count > context.inputCount ->
                complete(player, COSMIC_RUNES)
            name != null && isShayzienPlatebody(name) -> complete(player, SHAYZIEN_PLATEBODY)
        }
    }

    private fun isShayzienPlatebody(name: String): Boolean {
        val tier = SHAYZIEN_PLATEBODY_NAME.matchEntire(name)?.groupValues?.get(1)?.toInt()
        return tier != null && tier >= SHAYZIEN_MIN_TIER
    }

    fun clear(player: Player) {
        VarPlayerIntMapSetter.set(player, DONE, 0)
    }

    private const val NICKEL_ORE = "obj.nickel_ore"
    private const val COSMIC_RUNE = "obj.cosmicrune"
    private const val SHAYZIEN_MIN_TIER = 2
    private val SHAYZIEN_PLATEBODY_NAME = Regex("""Shayzien platebody \((\d)\)""")
}

class ClueChallengePrayerHook : PrayerEnableHook {
    override fun onEnable(player: Player, prayerVarbit: String) {
        if (prayerVarbit == CHIVALRY_VARBIT) {
            ClueChallengeTasks.complete(player, ClueChallengeTasks.CHIVALRY)
        }
    }

    private companion object {
        const val CHIVALRY_VARBIT = "varbit.prayer_chivalry"
    }
}

class ClueChallengeKillHook @Inject constructor(private val areas: AreaChecker) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val task =
            when (context.npc.type.name) {
                DUST_DEVIL_NAME -> ClueChallengeTasks.DUST_DEVIL
                LIZARDMAN_SHAMAN_NAME -> ClueChallengeTasks.SLAY_LIZARDMAN_SHAMAN
                FIYR_SHADE_NAME -> ClueChallengeTasks.SLAY_FIYR_SHADE
                NECHRYAEL_NAME ->
                    ClueChallengeTasks.SLAY_NECHRYAEL.takeIf {
                        areas.inArea(SLAYER_TOWER, context.npc.coords)
                    }
                else -> null
            }
        task?.let { ClueChallengeTasks.complete(context.hero, it) }
    }

    private companion object {
        const val DUST_DEVIL_NAME = "Dust devil"
        const val LIZARDMAN_SHAMAN_NAME = "Lizardman shaman"
        const val FIYR_SHADE_NAME = "Fiyr Shade"
        const val NECHRYAEL_NAME = "Nechryael"
        const val SLAYER_TOWER = "area.slayer_tower"
    }
}
