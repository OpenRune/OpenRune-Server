package org.rsmod.content.quest.area.varrock.daddyshome

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc5
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal enum class Furniture(
    val loc: String,
    val varbit: String,
    val label: String,
    val remaining: String,
    val plank: String,
    val planks: Int,
    val nails: Int,
    val cloth: Int,
    val xp: Double,
) {
    KitchenStool(
        "loc.daddyshome_stool_1",
        "varbit.daddyshome_stool_1",
        "wooden stool",
        "A broken stool in the kitchen",
        DaddysHomeQuest.Plank,
        planks = 1,
        nails = 2,
        cloth = 0,
        xp = 29.0,
    ),
    BedroomStool(
        "loc.daddyshome_stool_2",
        "varbit.daddyshome_stool_2",
        "wooden stool",
        "A broken stool in the bedroom",
        DaddysHomeQuest.Plank,
        planks = 1,
        nails = 2,
        cloth = 0,
        xp = 29.0,
    ),
    Chair(
        "loc.daddyshome_chair",
        "varbit.daddyshome_chair",
        "wooden chair",
        "A broken chair in the bedroom",
        DaddysHomeQuest.Plank,
        planks = 2,
        nails = 2,
        cloth = 0,
        xp = 58.0,
    ),
    KitchenTable(
        "loc.daddyshome_table_1",
        "varbit.daddyshome_table_1",
        "wooden table",
        "A broken table in the kitchen",
        DaddysHomeQuest.Plank,
        planks = 3,
        nails = 4,
        cloth = 0,
        xp = 87.0,
    ),
    BedroomTable(
        "loc.daddyshome_table_2",
        "varbit.daddyshome_table_2",
        "wooden table",
        "A broken table in the bedroom",
        DaddysHomeQuest.Plank,
        planks = 3,
        nails = 4,
        cloth = 0,
        xp = 87.0,
    ),
    Bed(
        "loc.daddyshome_bed",
        "varbit.daddyshome_bed",
        "waxwood bed",
        "The old campbed",
        DaddysHomeQuest.WaxwoodPlank,
        planks = 3,
        nails = 0,
        cloth = 2,
        xp = 207.0,
    ),
    Carpet(
        "loc.daddyshome_carpet_middle",
        "varbit.daddyshome_carpet",
        "carpet",
        "The rotten carpet",
        DaddysHomeQuest.Plank,
        planks = 0,
        nails = 0,
        cloth = 3,
        xp = 45.0,
    );

    val menuLabel: String
        get() {
            val plankName = if (plank == DaddysHomeQuest.Plank) "plank" else "waxwood plank"
            val materials = buildList {
                if (planks > 0) add("$planks $plankName${if (planks == 1) "" else "s"}")
                if (nails > 0) add("$nails nails")
                if (cloth > 0) add("$cloth ${if (cloth == 1) "bolt" else "bolts"} of cloth")
            }
            return "${label.replaceFirstChar { it.uppercase() }} (${materials.joinToString(", ")})"
        }

    companion object {
        const val Untouched = 0
        const val Broken = 1
        const val Cleared = 2
        const val Built = 3
    }
}

class DaddysHomeFurniture @Inject constructor(private val daddysHome: DaddysHomeQuest) :
    PluginScript() {

    override fun ScriptContext.startup() {
        for (furniture in Furniture.entries) {
            if (furniture == Furniture.Carpet) {
                onOpLoc5(furniture.loc) { interact(furniture, it.loc) }
            } else {
                onOpLoc1(furniture.loc) { interact(furniture, it.loc) }
            }
        }
        onOpLoc1(DaddysHomeQuest.Crates) { searchCrates(it.loc) }
        onOpHeld1(DaddysHomeQuest.Crate) { openCrate() }
    }

    private suspend fun ProtectedAccess.interact(furniture: Furniture, loc: BoundLocInfo) {
        arriveDelay()
        faceLoc(loc)
        when (daddysHome.furnitureState(player, furniture)) {
            Furniture.Broken -> demolish(furniture)
            Furniture.Cleared -> build(furniture)
            else -> mes("Nothing interesting happens.")
        }
    }

    private suspend fun ProtectedAccess.demolish(furniture: Furniture) {
        if (daddysHome.stage(player) < DaddysHomeQuest.Removing) return
        anim(BuildSeq)
        delay(BuildDelay)
        if (daddysHome.furnitureState(player, furniture) != Furniture.Broken) return
        daddysHome.setFurnitureState(player, furniture, Furniture.Cleared)
        if (
            daddysHome.stage(player) == DaddysHomeQuest.Removing &&
                daddysHome.allFurnitureAtLeast(player, Furniture.Cleared)
        ) {
            daddysHome.quest.setQuestStage(this, DaddysHomeQuest.Removed)
        }
        mes("You demolish the ${demolishName(furniture)}.")
    }

    private fun demolishName(furniture: Furniture): String =
        when (furniture) {
            Furniture.Bed -> "old campbed"
            Furniture.Carpet -> "rotten carpet"
            else -> "broken ${furniture.label.removePrefix("wooden ")}"
        }

    private suspend fun ProtectedAccess.build(furniture: Furniture) {
        if (daddysHome.stage(player) < DaddysHomeQuest.Building) {
            mes("You should talk to Old Man Yarlo before you start building.")
            return
        }
        if (!hasHammer() || !hasSaw()) {
            mes(missingToolsMessage())
            return
        }
        missingMaterials(furniture)?.let {
            mes(it)
            return
        }
        var confirmed = false
        startDialogue {
            confirmed =
                choice2(furniture.menuLabel, true, "Cancel.", false, title = BuildMenuTitle)
        }
        if (!confirmed) return
        anim(BuildSeq)
        delay(BuildDelay)
        if (daddysHome.furnitureState(player, furniture) != Furniture.Cleared) return
        if (!consumeMaterials(furniture)) {
            mes("You don't have the materials to build that.")
            return
        }
        daddysHome.setFurnitureState(player, furniture, Furniture.Built)
        statAdvance("stat.construction", furniture.xp)
        mes("You build the ${furniture.label}.")
    }

    private fun ProtectedAccess.hasHammer(): Boolean = Hammers.any { it in inv }

    private fun ProtectedAccess.hasSaw(): Boolean = Saws.any { it in inv }

    private fun ProtectedAccess.missingToolsMessage(): String =
        when {
            !hasHammer() && !hasSaw() -> "You need a hammer and a saw to build furniture."
            !hasHammer() -> "You need a hammer to build furniture."
            else -> "You need a saw to build furniture."
        }

    private fun ProtectedAccess.missingMaterials(furniture: Furniture): String? =
        when {
            inv.count(furniture.plank) < furniture.planks ->
                if (furniture.plank == DaddysHomeQuest.Plank) {
                    "You don't have enough planks to build that."
                } else {
                    "You don't have enough waxwood planks to build that."
                }
            nailCount() < furniture.nails -> "You don't have enough nails to build that."
            inv.count(Cloth) < furniture.cloth ->
                "You don't have enough bolts of cloth to build that."
            else -> null
        }

    private fun ProtectedAccess.nailCount(): Int = Nails.sumOf { inv.count(it) }

    private fun ProtectedAccess.nailPlan(count: Int): List<Pair<String, Int>>? {
        var left = count
        val plan = mutableListOf<Pair<String, Int>>()
        for (nail in Nails) {
            if (left == 0) break
            val take = minOf(left, inv.count(nail))
            if (take > 0) plan += nail to take
            left -= take
        }
        return if (left == 0) plan else null
    }

    private fun ProtectedAccess.consumeMaterials(furniture: Furniture): Boolean {
        val nails = nailPlan(furniture.nails) ?: return false
        val result =
            player.invTransaction(inv) {
                val from = select(inv)
                if (furniture.planks > 0) {
                    delete {
                        this.from = from
                        this.obj = furniture.plank.asRSCM(RSCMType.OBJ)
                        this.strictCount = furniture.planks
                    }
                }
                for ((nail, count) in nails) {
                    delete {
                        this.from = from
                        this.obj = nail.asRSCM(RSCMType.OBJ)
                        this.strictCount = count
                    }
                }
                if (furniture.cloth > 0) {
                    delete {
                        this.from = from
                        this.obj = Cloth.asRSCM(RSCMType.OBJ)
                        this.strictCount = furniture.cloth
                    }
                }
            }
        return result.success
    }

    private suspend fun ProtectedAccess.searchCrates(loc: BoundLocInfo) {
        arriveDelay()
        faceLoc(loc)
        val stage = daddysHome.stage(player)
        val needsLogs =
            stage in DaddysHomeQuest.Building until DaddysHomeQuest.Complete &&
                daddysHome.furnitureState(player, Furniture.Bed) < Furniture.Built
        if (!needsLogs) {
            mes("You search the crates but find nothing interesting.")
            return
        }
        if (invAdd(inv, DaddysHomeQuest.WaxwoodLogs, WaxwoodLogCount).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        startDialogue {
            objbox(
                DaddysHomeQuest.WaxwoodLogs,
                "The crate contains that water-repellant waxwood Old Man Yarlo described. You " +
                    "take some.",
            )
        }
    }

    private fun ProtectedAccess.openCrate() {
        val opened =
            player.invTransaction(inv) {
                val from = select(inv)
                delete {
                    this.from = from
                    this.obj = DaddysHomeQuest.Crate.asRSCM(RSCMType.OBJ)
                    this.strictCount = 1
                }
                for ((obj, count) in CrateContents) {
                    insert {
                        this.into = from
                        this.obj = obj.asRSCM(RSCMType.OBJ)
                        this.strictCount = count
                    }
                }
            }
        if (opened.failure) {
            mes("You don't have enough inventory space to open that.")
            return
        }
        daddysHome.markCrateOpened(player)
        mes("You open the crate and take out the supplies.")
    }

    internal companion object {
        const val BuildSeq = "seq.human_poh_build"
        const val BuildDelay = 3
        const val BuildMenuTitle = "Furniture Creation Menu"
        const val WaxwoodLogCount = 3
        const val Cloth = "obj.cloth"

        val Hammers = listOf("obj.hammer", "obj.imcando_hammer")
        val Saws = listOf("obj.poh_saw", "obj.wearable_saw", "obj.eyeglo_crystal_saw")
        val Nails =
            listOf(
                "obj.nails_dragon",
                "obj.nails_rune",
                "obj.nails_adamant",
                "obj.nails_mithril",
                "obj.nails_black",
                "obj.nails",
                "obj.nails_iron",
                "obj.nails_bronze",
            )

        val CrateContents =
            listOf(
                "obj.cert_woodplank" to 25,
                "obj.nails_mithril" to 50,
                "obj.cert_steel_bar" to 5,
                "obj.cert_plank_oak" to 10,
                "obj.cert_cloth" to 8,
                "obj.poh_tablet_teleporttohouse" to 5,
                "obj.poh_tablet_faladorteleport" to 1,
            )
    }
}
