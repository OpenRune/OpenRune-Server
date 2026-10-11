package org.rsmod.content.quest.area.varrock.daddyshome

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import jakarta.inject.Inject
import org.rsmod.api.enums.DaddysHomeEnums.daddys_home_hammers
import org.rsmod.api.enums.DaddysHomeEnums.daddys_home_nails
import org.rsmod.api.enums.DaddysHomeEnums.daddys_home_saws
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

internal object DaddysHomeTools {
    val hammers: List<ItemServerType>
        get() = daddys_home_hammers.inOrder()

    val saws: List<ItemServerType>
        get() = daddys_home_saws.inOrder()

    val nails: List<ItemServerType>
        get() = daddys_home_nails.inOrder()

    private fun EnumTypeMap<Int, ItemServerType>.inOrder(): List<ItemServerType> =
        backing.entries.sortedBy { it.key }.mapNotNull { it.value }
}

internal object Furniture {
    const val UNTOUCHED = 0
    const val BROKEN = 1
    const val CLEARED = 2
    const val BUILT = 3

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

private val clothType: ItemServerType
    get() = checkNotNull(ServerCacheManager.getItem(DaddysHomeFurniture.CLOTH.asRSCM(RSCMType.OBJ)))

private val DaddysHomeFurnitureRow.menuText: String
    get() {
        val materials = buildList {
            if (planks > 0) add("${plank.name}: $planks")
            if (nails > 0) add("${DaddysHomeFurniture.NAILS_LABEL}: $nails")
            if (cloth > 0) add("${clothType.name}: $cloth")
        }
        val lines = materials + List(DaddysHomeFurniture.MENU_LINES - materials.size) { "" }
        return "${menuObj.name}|${lines.joinToString("<br>")}"
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
        onOpLoc1(DaddysHomeQuest.CRATES) { searchCrates(it.loc) }
        onOpHeld1(DaddysHomeQuest.CRATE) { openCrate() }
    }

    private suspend fun ProtectedAccess.interact(
        furniture: DaddysHomeFurnitureRow,
        loc: BoundLocInfo,
    ) {
        arriveDelay()
        faceLoc(loc)
        when (daddysHome.furnitureState(player, furniture)) {
            Furniture.BROKEN -> demolish(furniture)
            Furniture.CLEARED -> build(furniture)
            else -> mes("Nothing interesting happens.")
        }
    }

    private suspend fun ProtectedAccess.demolish(furniture: DaddysHomeFurnitureRow) {
        if (daddysHome.stage(player) < DaddysHomeQuest.REMOVING) return
        anim(DEMOLISH_SEQ)
        delay(DEMOLISH_DELAY)
        if (daddysHome.furnitureState(player, furniture) != Furniture.BROKEN) return
        daddysHome.setFurnitureState(player, furniture, Furniture.CLEARED)
        if (
            daddysHome.stage(player) == DaddysHomeQuest.REMOVING &&
                daddysHome.allFurnitureAtLeast(player, Furniture.CLEARED)
        ) {
            daddysHome.quest.setQuestStage(this, DaddysHomeQuest.REMOVED)
        }
        mes("You demolish the ${furniture.demolished}.")
    }

    private suspend fun ProtectedAccess.build(furniture: DaddysHomeFurnitureRow) {
        if (daddysHome.stage(player) < DaddysHomeQuest.BUILDING) {
            mes("You should talk to Old Man Yarlo before you start building.")
            return
        }
        if (!hasHammer() || !hasSaw()) {
            mes(missingToolsMessage())
            return
        }
        if (!chooseFromBuildMenu(furniture)) return
        missingMaterials(furniture)?.let {
            mes(it)
            return
        }
        anim(BUILD_SEQ)
        delay(BUILD_DELAY)
        if (daddysHome.furnitureState(player, furniture) != Furniture.CLEARED) return
        if (!consumeMaterials(furniture)) {
            mes("You don't have the materials to build that.")
            return
        }
        daddysHome.setFurnitureState(player, furniture, Furniture.BUILT)
        statAdvance("stat.construction", furniture.xp.toDouble())
        mes("You build the ${furniture.label}.")
    }

    private suspend fun ProtectedAccess.chooseFromBuildMenu(
        furniture: DaddysHomeFurnitureRow
    ): Boolean {
        ifOpenMainModal(BUILD_MENU)
        val canBuild = missingMaterials(furniture) == null
        runClientScript(
            BUILD_MENU_ENTRY_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT),
            BUILD_MENU_SLOT,
            furniture.menuObj.id,
            furniture.level,
            furniture.menuText,
            if (canBuild) 1 else 0,
        )
        runClientScript(BUILD_MENU_LAYOUT_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT), BUILD_MENU_SLOT, 0)
        ifSetEvents(BUILD_MENU_ENTRY, -1..-1, IfEvent.Op1, IfEvent.Op10)
        ifSetEvents(BUILD_MENU_CONTENTS, BUILD_MENU_SLOT..BUILD_MENU_SLOT, IfEvent.PauseButton)
        val input = pauseButton()
        ifClose()
        return input.isComponentType(BUILD_MENU_CONTENTS) && input.isSubcomponent(BUILD_MENU_SLOT)
    }

    private fun ProtectedAccess.hasHammer(): Boolean =
        DaddysHomeTools.hammers.any { it.internalName in inv }

    private fun ProtectedAccess.hasSaw(): Boolean =
        DaddysHomeTools.saws.any { it.internalName in inv }

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
            inv.count(CLOTH) < furniture.cloth ->
                "You don't have enough bolts of cloth to build that."
            else -> null
        }

    private fun ProtectedAccess.nailCount(): Int =
        DaddysHomeTools.nails.sumOf { inv.count(it.internalName) }

    private fun ProtectedAccess.nailPlan(count: Int): List<Pair<String, Int>>? {
        var left = count
        val plan = mutableListOf<Pair<String, Int>>()
        for (nail in DaddysHomeTools.nails) {
            if (left == 0) break
            val take = minOf(left, inv.count(nail.internalName))
            if (take > 0) plan += nail.internalName to take
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
                        this.obj = CLOTH.asRSCM(RSCMType.OBJ)
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
            stage in DaddysHomeQuest.BUILDING until DaddysHomeQuest.COMPLETE &&
                daddysHome.furnitureState(player, Furniture.bed) < Furniture.BUILT
        if (!needsLogs) {
            mes("You search the crates but find nothing interesting.")
            return
        }
        if (invAdd(inv, DaddysHomeQuest.WAXWOOD_LOGS, WAXWOOD_LOG_COUNT).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        startDialogue {
            objbox(
                DaddysHomeQuest.WAXWOOD_LOGS,
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
                    this.obj = DaddysHomeQuest.CRATE.asRSCM(RSCMType.OBJ)
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
        const val BUILD_SEQ = "seq.human_poh_build"
        const val BUILD_DELAY = 3
        const val DEMOLISH_SEQ = "seq.human_throw_away"
        const val DEMOLISH_DELAY = 2
        const val BUILD_MENU = "interface.poh_furniture_creation"
        const val BUILD_MENU_CONTENTS = "component.poh_furniture_creation:contents"
        const val BUILD_MENU_ENTRY = "component.poh_furniture_creation:01"
        const val BUILD_MENU_ENTRY_SCRIPT = "clientscript.poh_furniture_creation_entry_old"
        const val BUILD_MENU_LAYOUT_SCRIPT = "clientscript.[clientscript,script1406]"
        const val BUILD_MENU_SLOT = 1
        const val MENU_LINES = 4
        const val NAILS_LABEL = "Nails"
        const val WAXWOOD_LOG_COUNT = 3
        const val CLOTH = "obj.cloth"
        const val FIFTH_OP = 5
    }
}
