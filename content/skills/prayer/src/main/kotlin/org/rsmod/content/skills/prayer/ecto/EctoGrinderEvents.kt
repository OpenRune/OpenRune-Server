package org.rsmod.content.skills.prayer.ecto

import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EctoGrinderEvents : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs("queue.prayer_ecto_grinder") { processGrinderTick(it.args) }

        onOpLoc1("loc.ahoy_grinder_handle") { windGrinder() } // Wind
        onOpLoc2("loc.ahoy_grinder_handle") { grinderStatus() } // Status

        onOpLoc1("loc.ahoy_grinder_chute") { collectBonemealFromBin() } // Empty

        ECTO_RECIPES.forEach { recipe ->
            onOpLocU("loc.ahoy_grinder_loader", recipe.bones) { addBonesToLoader(it, recipe) }
        }
    }

    private fun ProtectedAccess.addBonesToLoader(event: LocUEvents.Op, recipe: EctoRecipe) {
        if (ectoGrinderStatus == 1) {
            mes("You need to grind the bones already inside the grinder first.")
            return
        }
        if (ectoGrinderRecipe != 0) {
            mes("You need to collect the ground bones from the bin first.")
            return
        }
        if (recipe.prayerLevel > 1 && player.basePrayerLvl < recipe.prayerLevel) {
            mes("You need a Prayer level of at least ${recipe.prayerLevel} to grind these bones.")
            return
        }
        if (!inv.contains(recipe.bones) || !inv.contains("obj.pot_empty")) {
            return
        }
        weakQueue(
            "queue.prayer_ecto_grinder",
            2,
            GrinderTask(stage = GrinderStage.AddingBones, auto = true, recipeId = recipe.id),
        )
    }

    private fun ProtectedAccess.windGrinder() {
        when {
            ectoGrinderStatus == 0 && ectoGrinderRecipe != 0 -> {
                mes("You need to collect the ground bones from the bin first.")
                return
            }
            ectoGrinderStatus == 0 -> {
                mes("You have to add some bones in the loader first.")
                return
            }
        }
        val recipeId = ectoGrinderRecipe
        weakQueue(
            "queue.prayer_ecto_grinder",
            2,
            GrinderTask(stage = GrinderStage.Grinding, auto = false, recipeId = recipeId),
        )
    }

    private fun ProtectedAccess.collectBonemealFromBin() {
        if (ectoGrinderStatus == 1) {
            mes("You need to grind the bones already inside the grinder first.")
            return
        }
        val recipe = findEctoRecipe(ectoGrinderRecipe)
        if (recipe == null) {
            mes("There's no bonemeal in the bin right now.")
            return
        }
        if (!inv.contains("obj.pot_empty")) {
            mes("You need an empty pot to collect the bonemeal.")
            return
        }
        weakQueue(
            "queue.prayer_ecto_grinder",
            2,
            GrinderTask(stage = GrinderStage.Collecting, auto = false, recipeId = recipe.id),
        )
    }

    private suspend fun ProtectedAccess.processGrinderTick(task: GrinderTask) {
        val recipe = findEctoRecipe(task.recipeId) ?: return
        when (task.stage) {
            GrinderStage.AddingBones -> {
                if (!forcePathToExactTile(COORD_GRINDER_LOADER)) {
                    return
                }
                if (invDel(inv, recipe.bones, 1).failure) {
                    return
                }
                faceSquare(coords.translateZ(1))
                anim("seq.ahoy_bone_dump")
                ectoGrinderStatus = 1
                ectoGrinderRecipe = recipe.id
                mes("You place the bones into the loader.")
                weakQueue(
                    "queue.prayer_ecto_grinder",
                    4,
                    GrinderTask(stage = GrinderStage.Grinding, auto = task.auto, recipeId = recipe.id),
                )
            }
            GrinderStage.Grinding -> {
                if (!forcePathToExactTile(COORD_GRINDER_WHEEL)) {
                    return
                }
                if (ectoGrinderStatus != 1) {
                    return
                }
                faceSquare(coords.translateZ(1))
                anim("seq.ahoy_bone_grind")
                ectoGrinderStatus = 0
                mes("You grind the bones into bonemeal.")
                weakQueue(
                    "queue.prayer_ecto_grinder",
                    4,
                    GrinderTask(stage = GrinderStage.Collecting, auto = task.auto, recipeId = recipe.id),
                )
            }
            GrinderStage.Collecting -> {
                if (!forcePathToExactTile(COORD_GRINDER_BIN)) {
                    return
                }
                if (invDel(inv, "obj.pot_empty", 1).failure) {
                    return
                }
                if (invAdd(inv, recipe.bonemeal, 1).failure) {
                    invAdd(inv, "obj.pot_empty", 1)
                    mes("You don't have enough inventory space.")
                    return
                }
                faceSquare(coords.translateZ(1))
                anim("seq.ahoy_fillbucket_bonedust")
                ectoGrinderRecipe = 0
                mes("You collect the bonemeal from the bin.")
                if (task.auto && inv.contains(recipe.bones) && inv.contains("obj.pot_empty")) {
                    weakQueue(
                        "queue.prayer_ecto_grinder",
                        4,
                        GrinderTask(stage = GrinderStage.AddingBones, auto = true, recipeId = recipe.id),
                    )
                }
            }
        }
    }

    private suspend fun ProtectedAccess.forcePathToExactTile(coord: CoordGrid): Boolean {
        repeat(16) {
            if (coords == coord) {
                return true
            }
            walk(coord)
            delay(1)
        }
        return coords == coord
    }

    private fun ProtectedAccess.grinderStatus() {
        val recipe = findEctoRecipe(ectoGrinderRecipe)
        when {
            ectoGrinderStatus == 1 && recipe != null -> mes("The bone grinder contains some ${recipe.bonesName.lowercase()} inside it.")
            ectoGrinderRecipe != 0 && recipe != null -> mes("The bone grinder contains some ground ${recipe.bonesName.lowercase()} inside it.")
            else -> mes("The bone grinder is currently empty.")
        }
    }

    private data class GrinderTask(val stage: GrinderStage, val auto: Boolean, val recipeId: Int)

    private enum class GrinderStage {
        AddingBones,
        Grinding,
        Collecting,
    }

    private companion object {
        private val COORD_GRINDER_LOADER: CoordGrid = CoordGrid(3660, 3524, 1)
        private val COORD_GRINDER_WHEEL: CoordGrid = CoordGrid(3659, 3524, 1)
        private val COORD_GRINDER_BIN: CoordGrid = CoordGrid(3658, 3524, 1)
    }
}
