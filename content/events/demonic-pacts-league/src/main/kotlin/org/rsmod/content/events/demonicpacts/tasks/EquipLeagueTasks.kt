package org.rsmod.content.events.demonicpacts.tasks

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.PlayerAreaEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerEquip
import org.rsmod.content.events.leaguetasks.LeagueTasks
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EquipLeagueTasks
@Inject
constructor(private val tasks: LeagueTasks, private val areas: AreaChecker) : PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private class InRegion(val task: Int, val area: Int, val objs: Set<Int>)

    private class AnySet(val task: Int, val sets: List<List<Set<Int>>>) {
        val objs: Set<Int> = sets.flatten().flatten().toSet()
    }

    private class OverTime(
        val task: Int,
        val varbit: String,
        val pieces: List<Set<Int>>,
        val needed: Int = pieces.size,
    )

    private val inRegion =
        listOf(
            region("league_task_equip_a_team_cape", "wilderness", TEAM_CAPES),
            region("league_task_equip_granite_shield_in_fremennik", "fremennik", "obj.granite_shield"),
            region(
                "league_task_equip_a_dark_bow_in_tirannwn",
                "tirannwn",
                "obj.darkbow",
                "obj.darkbow_green",
                "obj.darkbow_blue",
                "obj.darkbow_yellow",
                "obj.darkbow_white",
            ),
            region("league_task_equip_a_dragon_2handed_sword", "desert", *DRAGON_2H),
            region(
                "league_task_equip_a_dragon_2handed_sword_in_the_wilderness",
                "wilderness",
                *DRAGON_2H,
            ),
            region(
                "league_task_equip_dragon_pickaxe_in_desert",
                "desert",
                "obj.dragon_pickaxe",
                "obj.dragon_pickaxe_pretty",
                "obj.zalcano_pickaxe",
            ),
            region("league_task_equip_a_pair_of_dragon_boots_in_asgarnia", "asgarnia", *D_BOOTS),
            region(
                "league_task_equip_a_pair_of_dragon_boots_in_wilderness",
                "wilderness",
                *D_BOOTS,
            ),
            region(
                "league_task_equip_a_leafbladed_weapon_tirannwn",
                "tirannwn",
                "obj.leafbladed_sword",
                "obj.leafbladed_battleaxe",
            ),
            region(
                "league_task_equip_a_dragon_chainbody_in_the_kharidian_desert",
                "desert",
                "obj.dragon_chainbody",
                "obj.dragon_chainbody_gold",
            ),
        )

    private val anySet =
        listOf(
            AnySet(
                task("league_task_equip_a_full_tai_bwo_wannai_villager_set"),
                listOf(
                    listOf("hat", "robe", "armband", "sandals").map { piece ->
                        VILLAGER_COLOURS.flatMapTo(HashSet()) {
                            objs("obj.tbw_villager_${piece}_$it")
                        }
                    },
                ),
            ),
            AnySet(
                task("league_task_equip_a_full_god_dragonhide_set"),
                GODS.map { god ->
                    listOf("coif", "chest", "chaps", "vambraces").map {
                        objs("obj.trail_${god}_$it")
                    }
                },
            ),
            AnySet(
                task("league_task_equip_a_full_god_rune_set"),
                GODS.map { god ->
                    listOf(
                        objs("obj.rune_full_helm_$god"),
                        objs("obj.rune_platebody_$god"),
                        objs("obj.rune_platelegs_$god", "obj.rune_plateskirt_$god"),
                        objs("obj.rune_kiteshield_$god"),
                    )
                },
            ),
            AnySet(
                task("league_task_equip_a_full_vestment_set"),
                GODS.map { god ->
                    listOf("mitre", "robe_t", "robe_l", "cloak", "scarf").map {
                        objs("obj.trail_${god}_$it")
                    }
                },
            ),
            AnySet(
                task("league_task_equip_any_full_barrows_armour_set"),
                BROTHERS.map { brother ->
                    listOf("head", "body", "legs", "weapon").map { slot ->
                        BARROWS_CHARGES.flatMapTo(HashSet()) {
                            objs("obj.barrows_${brother}_$slot$it")
                        }
                    }
                },
            ),
            AnySet(
                task("league_task_equip_full_recolour_graceful_outfit"),
                GRACEFUL_RECOLOURS.map { colour ->
                    listOf("hood", "cape", "top", "legs", "gloves", "boots").map { piece ->
                        objs(colour.replace("*", piece))
                    }
                },
            ),
        )

    private val overTime =
        listOf(
            OverTime(
                task("league_task_equip_every_godsword"),
                "varbit.league_worn_godswords",
                listOf(
                    objs("obj.ancient_godsword"),
                    objs("obj.ags", "obj.agsg"),
                    objs("obj.bgs", "obj.bgsg"),
                    objs("obj.sgs", "obj.sgsg"),
                    objs("obj.zgs", "obj.zgsg"),
                ),
            ),
            OverTime(
                task("league_task_equip_every_dagannoth_king_ring"),
                "varbit.league_worn_dagannoth_rings",
                listOf(
                    objs("obj.ranger_ring", "obj.nzone_ranger_ring"),
                    objs("obj.berzerker_ring", "obj.nzone_berzerker_ring"),
                    objs("obj.seer_ring", "obj.nzone_seer_ring"),
                    objs("obj.warrior_ring", "obj.nzone_warrior_ring"),
                ),
            ),
            OverTime(
                task("league_task_equip_all_zenyte_jewelry"),
                "varbit.league_worn_zenyte",
                listOf(
                    objs("obj.zenyte_amulet_enchanted", "obj.zenyte_amulet_ornament"),
                    objs("obj.zenyte_necklace_enchanted", "obj.zenyte_necklace_ornament"),
                    objs(
                        "obj.zenyte_ring_enchanted",
                        "obj.zenyte_ring_enchanted_recoil",
                        "obj.nzone_zenyte_ring_enchanted",
                        "obj.nzone_zenyte_ring_enchanted_recoil",
                    ),
                    objs("obj.zenyte_bracelet_enchanted", "obj.zenyte_bracelet_enchanted_ornament"),
                ),
            ),
            OverTime(
                task("league_task_equip_every_completed_god_book"),
                "varbit.league_worn_god_books",
                listOf("saradomin", "zamorak", "guthix", "armadyl", "bandos", "zaros").map {
                    objs("obj.${it}book_complete")
                },
            ),
            OverTime(
                task("league_task_equip_some_primordial_pegasian_and_eternal_boots"),
                "varbit.league_worn_cerberus_boots",
                listOf(
                    objs("obj.primordial_boots"),
                    objs("obj.pegasian_boots"),
                    objs("obj.eternal_boots"),
                ),
            ),
            OverTime(
                task("league_task_wear_multiple_snelms"),
                "varbit.league_worn_snelms",
                CRAFTED_SNELMS.map { objs(it) },
                needed = 3,
            ),
        )

    private val cerberusTask = task("league_task_equip_some_primordial_pegasian_and_eternal_boots")
    private val avernicMax = "obj.avernic_treads_max".asRSCM(RSCMType.OBJ)
    private val blackChinchompa = "obj.chinchompa_black".asRSCM(RSCMType.OBJ)
    private val chinchompaTasks =
        listOf(
            100 to task("league_task_equip_100_black_chinchompas"),
            250 to task("league_task_equip_250_black_chinchompas"),
            500 to task("league_task_equip_500_black_chinchompas"),
        )

    override fun ScriptContext.startup() {
        tasks.claim(
            *(inRegion.map { it.task } +
                    anySet.map { it.task } +
                    overTime.map { it.task } +
                    chinchompaTasks.map { it.second })
                .toIntArray()
        )
        onPlayerEquip { player.onEquip(item.id) }
        onEvent<PlayerAreaEvents.Entered> {
            for (entry in inRegion) {
                if (entry.area == area && player.wearsAny(entry.objs)) {
                    tasks.complete(player, entry.task)
                }
            }
        }
    }

    private fun Player.onEquip(obj: Int) {
        for (entry in inRegion) {
            if (obj in entry.objs && areas.inArea(entry.area, coords)) {
                tasks.complete(this, entry.task)
            }
        }
        for (entry in anySet) {
            if (obj in entry.objs && entry.sets.any { set -> set.all { wearsAny(it) } }) {
                tasks.complete(this, entry.task)
            }
        }
        for (entry in overTime) {
            val piece = entry.pieces.indexOfFirst { obj in it }
            if (piece < 0) {
                continue
            }
            val worn = vars[entry.varbit] or (1 shl piece)
            VarPlayerIntMapSetter.set(this, entry.varbit, worn)
            if (Integer.bitCount(worn) >= entry.needed) {
                tasks.complete(this, entry.task)
            }
        }
        if (obj == avernicMax) {
            tasks.complete(this, cerberusTask)
        }
        if (obj == blackChinchompa) {
            val stack = worn.firstOrNull { it?.id == blackChinchompa }?.count ?: 0
            for ((needed, task) in chinchompaTasks) {
                if (stack >= needed) {
                    tasks.complete(this, task)
                }
            }
        }
    }

    private fun Player.wearsAny(objs: Set<Int>): Boolean = worn.any { it != null && it.id in objs }

    private fun region(task: String, region: String, vararg objs: String): InRegion =
        InRegion(task(task), "area.league_region_$region".asRSCM(RSCMType.AREA), objs(*objs))

    private fun region(task: String, region: String, objs: List<String>): InRegion =
        region(task, region, *objs.toTypedArray())

    private companion object {
        val GODS = listOf("saradomin", "zamorak", "guthix", "armadyl", "bandos", "ancient")
        val BROTHERS = listOf("ahrim", "dharok", "guthan", "karil", "torag", "verac")
        val BARROWS_CHARGES = listOf("", "_100", "_75", "_50", "_25")
        val VILLAGER_COLOURS = listOf("cream", "blue", "yellow", "purple")
        val DRAGON_2H = arrayOf("obj.dragon_2h_sword", "obj.bh_dragon_2h_sword_corrupted")
        val D_BOOTS =
            arrayOf("obj.dragon_boots", "obj.dragon_boots_gold", "obj.bh_dragon_boots_corrupted")
        val TEAM_CAPES =
            (1..50).map { "obj.wilderness_cape_$it" } +
                listOf("obj.wilderness_cape_i", "obj.wilderness_cape_x", "obj.wilderness_cape_zero")
        val GRACEFUL_RECOLOURS =
            listOf("arceuus", "piscarilius", "lovakengj", "shayzien", "hosidius", "kourend")
                .map { "obj.zeah_graceful_*_$it" } +
                listOf("skillcapecolour", "hallowed", "trailblazer", "adventurer", "wyrm").map {
                    "obj.graceful_*_$it"
                }
        val CRAFTED_SNELMS =
            listOf("red+black", "orange", "blue", "swamp", "yellow").map { "obj.snelm_round_$it" } +
                listOf("red+black", "blue", "swamp", "yellow").map { "obj.snelm_point_$it" }
    }
}
