package org.rsmod.content.events.demonicpacts.tasks

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.NpcKilledEvent
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcKilled
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.events.leaguetasks.LeagueTasks
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CombatLeagueTasks
@Inject
constructor(private val tasks: LeagueTasks, private val areas: AreaChecker) : PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private val oryxTask = task("league_task_kill_a_oryx_with_melee")
    private val jaguarTask = task("league_task_kill_a_jaguar_without_taking_any_damage")
    private val jubsterTask = task("league_task_kill_a_jubster_with_beret")
    private val rockCrabTask = task("league_task_kill_a_rock_crab_one_hit")
    private val chickensTask = task("league_task_kill_three_chickens_in_6_seconds")
    private val penguinsTask = task("league_task_kill_8_penguins_within_5_seconds")
    private val kerisTask = task("league_task_hit_150_with_the_keris_partisan")
    private val elementalTask = task("league_task_deal_66_damage_with_elemental_spell")
    private val mummyTask = task("league_task_set_a_mummy_ablaze")
    private val surgeTask = task("league_task_cast_surge_at_black_dragon_in_kandarin")

    private val oryx = npcs("npc.varlamore_oryx")
    private val jaguar = npcs("npc.varlamore_jaguar")
    private val jubster = npcs("npc.tol_jubster")
    private val rockCrabs = npcs("npc.horror_rockcrab", "npc.horror_rockcrab_small")
    private val chickens =
        npcs(
            "npc.chicken",
            "npc.chicken_brown",
            "npc.newbiechicken",
            "npc.misc_chicken",
            "npc.misc_chicken_brown",
            "npc.farm_chicken_brown",
            "npc.farm_chicken_dark_brown",
            "npc.farm_chicken_tan",
            "npc.farm_chicken_brown_outdoors",
            "npc.farm_chicken_dark_brown_outdoors",
            "npc.farm_chicken_tan_outdoors",
            "npc.farm_chicken_brown_indoors",
            "npc.farm_chicken_dark_brown_indoors",
            "npc.farm_chicken_tan_indoors",
        )
    private val penguins = npcs("npc.penguin", "npc.peng_generic", "npc.peng_generic_surface")
    private val mummies =
        npcs(
            "npc.deserttreasure_mummy_1",
            "npc.deserttreasure_mummy_2",
            "npc.deserttreasure_mummy_3",
            "npc.deserttreasure_mummy_4",
            "npc.deserttreasure_mummy_5",
            "npc.ntk_mummy_1",
            "npc.ntk_mummy_2",
            "npc.ntk_mummy_3",
            "npc.ntk_mummy_4",
            "npc.ntk_mummy_5",
        )
    private val blackDragons =
        npcs(
            "npc.black_dragon",
            "npc.black_dragon2",
            "npc.black_dragon3",
            "npc.black_dragon4",
            "npc.black_dragon5",
            "npc.black_dragon_strongholdcave_1",
            "npc.black_dragon_strongholdcave_2",
            "npc.black_dragon_strongholdcave_3",
        )

    private val berets =
        objs("obj.berret_black", "obj.berret_blue", "obj.berret_white", "obj.berret_red")
    private val kerisPartisans =
        objs(
            "obj.keris_partisan",
            "obj.keris_partisan_breach",
            "obj.keris_partisan_corruption",
            "obj.keris_partisan_sun",
            "obj.keris_partisan_amascut",
        )
    private val fireSpells =
        objs(
            "obj.13_fire_strike",
            "obj.35_fire_bolt",
            "obj.59_fire_blast",
            "obj.75_fire_wave",
            "obj.95_fire_surge",
        )
    private val elementalSpells =
        fireSpells +
            objs(
                "obj.01_wind_strike",
                "obj.05_water_strike",
                "obj.09_earth_strike",
                "obj.17_wind_bolt",
                "obj.23_water_bolt",
                "obj.29_earth_bolt",
                "obj.41_wind_blast",
                "obj.47_water_blast",
                "obj.53_earth_blast",
                "obj.62_wind_wave",
                "obj.65_water_wave",
                "obj.70_earth_wave",
                "obj.81_wind_surge",
                "obj.85_water_surge",
                "obj.90_earth_surge",
            )
    private val waterSurge = "obj.85_water_surge".asRSCM(RSCMType.OBJ)
    private val kandarin = "area.league_region_kandarin".asRSCM(RSCMType.AREA)

    override fun ScriptContext.startup() {
        tasks.claim(
            oryxTask,
            jaguarTask,
            jubsterTask,
            rockCrabTask,
            chickensTask,
            penguinsTask,
            kerisTask,
            elementalTask,
            mummyTask,
            surgeTask,
        )
        onNpcKilled {
            if (isHero) {
                onHeroKill()
            }
        }
        onEvent<PlayerAttackEvents.HitImpact> {
            val npc = target as? Npc ?: return@onEvent
            val weapon = hit.righthandType()?.id
            val spell = hit.secondaryType()?.id
            if (hit.damage >= 150 && weapon in kerisPartisans) {
                tasks.complete(player, kerisTask)
            }
            if (hit.damage >= 66 && spell in elementalSpells) {
                tasks.complete(player, elementalTask)
            }
            if (hit.damage > 0 && spell in fireSpells && npc.isAny(mummies)) {
                tasks.complete(player, mummyTask)
            }
            if (spell == waterSurge && npc.isAny(blackDragons)) {
                if (areas.inArea(kandarin, player.coords)) {
                    tasks.complete(player, surgeTask)
                }
            }
        }
        onPlayerSoftTimer(CHICKEN_WINDOW) {
            player.clearSoftTimer(CHICKEN_WINDOW)
            VarPlayerIntMapSetter.set(player, CHICKEN_KILLS, 0)
        }
        onPlayerSoftTimer(PENGUIN_WINDOW) {
            player.clearSoftTimer(PENGUIN_WINDOW)
            VarPlayerIntMapSetter.set(player, PENGUIN_KILLS, 0)
        }
    }

    private fun NpcKilledEvent.onHeroKill() {
        when {
            npc.isAny(oryx) && killingStyle == HitType.Melee -> tasks.complete(player, oryxTask)
            npc.isAny(jaguar) && !tookDamage -> tasks.complete(player, jaguarTask)
            npc.isAny(jubster) && player.worn.any { it != null && it.id in berets } ->
                tasks.complete(player, jubsterTask)
            npc.isAny(rockCrabs) && dealtKillingHit && (killingHit?.damage ?: 0) >= 50 ->
                tasks.complete(player, rockCrabTask)
            npc.isAny(chickens) -> timedKill(CHICKEN_KILLS, CHICKEN_WINDOW, 10, 3, chickensTask)
            npc.isAny(penguins) -> timedKill(PENGUIN_KILLS, PENGUIN_WINDOW, 8, 8, penguinsTask)
        }
    }

    private fun NpcKilledEvent.timedKill(
        counter: String,
        window: String,
        ticks: Int,
        needed: Int,
        task: Int,
    ) {
        if (player.vars[counter] == 0) {
            player.softTimer(window, cycles = ticks)
        }
        if (player.advance(counter, needed) >= needed) {
            tasks.complete(player, task)
        }
    }

    private fun Npc.isAny(types: Set<Int>): Boolean = type.id in types || visType.id in types

    private companion object {
        const val CHICKEN_KILLS = "varbit.league_window_chicken_kills"
        const val PENGUIN_KILLS = "varbit.league_window_penguin_kills"
        const val CHICKEN_WINDOW = "timer.league_chicken_window"
        const val PENGUIN_WINDOW = "timer.league_penguin_window"
    }
}
