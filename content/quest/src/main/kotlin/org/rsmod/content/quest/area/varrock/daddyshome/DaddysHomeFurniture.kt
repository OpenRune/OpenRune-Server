package org.rsmod.content.quest.area.varrock.daddyshome

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.table.DaddysHomeCrateRow
import org.rsmod.api.table.DaddysHomeFurnitureRow
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object Furniture {
    const val Untouched = 0
    const val Broken = 1
    const val Cleared = 2
    const val Built = 3

    val all: List<DaddysHomeFurnitureRow>
        get() = DaddysHomeFurnitureRow.all()

    val kitchenStool: DaddysHomeFurnitureRow
        get() = DaddysHomeFurnitureRow.getRow("dbrow.daddys_home_furniture_kitchen_stool")

    val bedroomStool: DaddysHomeFurnitureRow
        get() = DaddysHomeFurnitureRow.getRow("dbrow.daddys_home_furniture_bedroom_stool")

    val chair: DaddysHomeFurnitureRow
        get() = DaddysHomeFurnitureRow.getRow("dbrow.daddys_home_furniture_chair")

    val kitchenTable: DaddysHomeFurnitureRow
        get() = DaddysHomeFurnitureRow.getRow("dbrow.daddys_home_furniture_kitchen_table")

    val bedroomTable: DaddysHomeFurnitureRow
        get() = DaddysHomeFurnitureRow.getRow("dbrow.daddys_home_furniture_bedroom_table")

    val bed: DaddysHomeFurnitureRow
        get() = DaddysHomeFurnitureRow.getRow("dbrow.daddys_home_furniture_bed")

    val carpet: DaddysHomeFurnitureRow
        get() = DaddysHomeFurnitureRow.getRow("dbrow.daddys_home_furniture_carpet")
}

internal val DaddysHomeFurnitureRow.varbitType: VarBitType
    get() = checkNotNull(ServerCacheManager.getVarbit(varbit)) { "Missing varbit: $varbit" }

internal fun DaddysHomeFurnitureRow.isSameFurniture(other: DaddysHomeFurnitureRow): Boolean =
    rowId == other.rowId

private val DaddysHomeFurnitureRow.menuLabel: String
    get() {
        val materials = buildList {
            if (planks > 0) add("$planks $plankName${if (planks == 1) "" else "s"}")
            if (nails > 0) add("$nails nails")
            if (cloth > 0) add("$cloth ${if (cloth == 1) "bolt" else "bolts"} of cloth")
        }
        return "${label.replaceFirstChar { it.uppercase() }} (${materials.joinToString(", ")})"
    }

class DaddysHomeFurniture @Inject constructor(private val daddysHome: DaddysHomeQuest) :
    PluginScript() {

    override fun ScriptContext.startup() {
        for (furniture in Furniture.all) {
            val loc = furniture.loc.internalName
            if (furniture.op == FIFTH_OP) {
                onOpLoc5(loc) { interact(furniture, it.loc) }
            } else {
                onOpLoc1(loc) { interact(furniture, it.loc) }
            }
        }
        onOpLoc1(DaddysHomeQuest.Crates) { searchCrates(it.loc) }
        onOpHeld1(DaddysHomeQuest.Crate) { openCrate() }
    }

    private suspend fun ProtectedAccess.interact(
        furniture: DaddysHomeFurnitureRow,
        loc: BoundLocInfo,
    ) {
        arriveDelay()
        faceLoc(loc)
        when (daddysHome.furnitureState(player, furniture)) {
            Furniture.Broken -> demolish(furniture)
            Furniture.Cleared -> build(furniture)
            else -> mes("Nothing interesting happens.")
        }
    }

    private suspend fun ProtectedAccess.demolish(furniture: DaddysHomeFurnitureRow) {
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
        mes("You demolish the ${furniture.demolished}.")
    }

    private suspend fun ProtectedAccess.build(furniture: DaddysHomeFurnitureRow) {
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
        statAdvance("stat.construction", furniture.xp.toDouble())
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

    private fun ProtectedAccess.missingMaterials(furniture: DaddysHomeFurnitureRow): String? =
        when {
            inv.count(furniture.plank.internalName) < furniture.planks ->
                "You don't have enough ${furniture.plankName}s to build that."
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

    private fun ProtectedAccess.consumeMaterials(furniture: DaddysHomeFurnitureRow): Boolean {
        val nails = nailPlan(furniture.nails) ?: return false
        val result =
            player.invTransaction(inv) {
                val from = select(inv)
                if (furniture.planks > 0) {
                    delete {
                        this.from = from
                        this.obj = furniture.plank.id
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
                daddysHome.furnitureState(player, Furniture.bed) < Furniture.Built
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
                for (item in DaddysHomeCrateRow.all()) {
                    insert {
                        this.into = from
                        this.obj = item.obj.id
                        this.strictCount = item.count
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
        const val FIFTH_OP = 5

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
    }
}
