package org.rsmod.content.skills.farming

import dev.openrune.ParamReferences.param
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetObj
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class FarmingGeomancy @Inject constructor(private val farming: FarmingService) {
    private val patchNames by lazy { enum<Int, String>("farming_geomancy_patch_names") }
    private val pictures by lazy { enum<Int, ComponentType>("farming_geomancy_pictures") }
    private val displayObjects by lazy {
        val cropNames = enum<ItemServerType, String>("farming_geomancy_crop_names")
        val named = cropNames.filterValuesNotNull()
        val cleaned = HerbloreCleaningRow.all().associate { it.input.id to it.output }
        FarmingCrops.all.associate { crop ->
            val product = crop.produce?.let { checkNotNull(ServerCacheManager.getItem(it.asRSCM(RSCMType.OBJ))) }
            val clean = product?.let { cleaned[it.id] }
            val display = when {
                product != null && product in cropNames -> product
                clean != null && clean in cropNames -> clean
                else -> named.single { (_, name) ->
                    name.equals(crop.name, ignoreCase = true) ||
                        name.equals("${crop.name} plant", ignoreCase = true)
                }.key
            }
            crop.index to display.internalName
        }
    }
    private val panels by lazy {
        FarmingPatches.all.map { patch ->
            val id = patch.locs.firstNotNullOfOrNull { symbol ->
                checkNotNull(ServerCacheManager.getObject(symbol.asRSCM(RSCMType.LOC)))
                    .paramOrNull(PATCH_ID)
            }
            val panelId = checkNotNull(id) { "Missing native Farming id: ${patch.site}" }
            val picture = pictures.getValue(panelId)
            Panel(patch, panelId, RSCM.getReverseMapping(RSCMType.COMPONENT, picture.packed))
        }
    }

    fun open(access: ProtectedAccess) {
        farming.refresh(access.player)
        access.ifOpenMainModal(INTERFACE)
    }

    fun update(player: Player) {
        if (INTERFACE !in player.ui) return
        for (panel in panels) {
            val state = farming.state(player, panel.patch)
            val crop = state.crop
            val obj = when {
                state.scarecrow -> "obj.scarecrow_complete"
                crop != null -> displayObjects.getValue(crop.index)
                state.cleared -> "obj.bucket_empty"
                else -> WEEDS
            }
            val flags = flags(
                state,
                panel.patch.kind.waterable && crop != null && !state.grown(crop),
                farming.flowerProtected(player, panel.patch),
                panel.patch.site == "southwest_hosidius" && FarmingPatches.diseaseFree(player, panel.patch),
            )
            val stage = if (crop == null) 0 else state.stage + 1
            val maximum = if (crop == null) 0 else crop.stages + 1
            val minimum = farming.minimumYield(player, panel.patch) ?: 0
            val coord = packedProgress(stage, maximum, minimum, state.watered)
            val picture = when (obj) {
                "obj.bucket_empty" -> "obj.blankobject"
                "obj.vodka" -> "obj.spirit_tree_dummy"
                else -> obj
            }
            player.ifSetObj(panel.picture, picture, 400)
            player.runClientScript(
                SET_PANEL.asRSCM(RSCMType.CLIENTSCRIPT),
                panel.id, obj.asRSCM(RSCMType.OBJ), coord, flags,
                patchNames[panel.id]?.lowercase() ?: "patch",
            )
        }
    }

    private data class Panel(val patch: PatchDef, val id: Int, val picture: String)

    internal companion object {
        const val INTERFACE = "interface.farming_view"
        private const val SET_PANEL = "clientscript.[clientscript,farming_view_setpanel]"
        private val PATCH_ID by lazy { param<Int>("farming_patch_id") }

        fun packedProgress(stage: Int, maximum: Int, minimumYield: Int, watered: Boolean): Int =
            (if (watered) 1 shl 28 else 0) or
                (((minimumYield.coerceAtLeast(0) shl 6) or stage.coerceIn(0, 63)) shl 14) or
                maximum.coerceIn(0, 63)

        fun flags(state: PatchState, waterable: Boolean, flower: Boolean, hosidiusProtected: Boolean): Int {
            var flags = when (state.health) {
                Health.HEALTHY -> 0
                Health.DISEASED -> 1
                Health.DEAD -> 2
            }
            if (state.paid) flags = flags or (1 shl 2)
            if (flower) flags = flags or (1 shl 3)
            if (waterable) flags = flags or (1 shl 4)
            when (state.compost) {
                Compost.NONE -> Unit
                Compost.NORMAL -> flags = flags or (1 shl 5)
                Compost.SUPER -> flags = flags or (1 shl 6)
                Compost.ULTRA -> flags = flags or (1 shl 7)
            }
            if (hosidiusProtected) flags = flags or (1 shl 8)
            return flags
        }
    }
}

class FarmingGeomancyScript @Inject constructor(private val geomancy: FarmingGeomancy) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen(FarmingGeomancy.INTERFACE) { geomancy.update(player) }
        onEvent<FarmingRefreshEvent> { geomancy.update(player) }
        onEvent<FarmingPatchChangeEvent> { geomancy.update(player) }
    }
}
