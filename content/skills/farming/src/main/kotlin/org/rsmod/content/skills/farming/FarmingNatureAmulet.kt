package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingNatureAmulet @Inject constructor(private val farming: FarmingService) : PluginScript() {
    override fun ScriptContext.startup() {
        for (patch in FarmingPatches.all) {
            for (loc in patch.locs) {
                onOpLocU(loc, AMULET) { bind(patch) }
            }
        }
        onOpHeld1(AMULET) { checkPatch() }
        onOpWorn2(AMULET) { checkPatch() }
        onEvent<FarmingRefreshEvent> { refreshed(player, after) }
        onEvent<FarmingPatchChangeEvent> { remember(player, patch) }
    }

    private fun ProtectedAccess.bind(patch: PatchDef) {
        if (AMULET !in inv) return
        val state = farming.state(player, patch)
        VarPlayerIntMapSetter.set(player, BOUND_PATCH, patch.varp.asRSCM(RSCMType.VARP))
        VarPlayerIntMapSetter.set(player, LAST_STATUS, state.pack())
        mes("You bind the Amulet of Nature to this patch.")
    }

    private fun ProtectedAccess.checkPatch() {
        val patch = bound(player)
        if (patch == null) {
            mes("The Amulet of Nature is not bound to a patch.")
            return
        }
        val state = farming.state(player, patch)
        val crop = state.crop
        val type = patch.kind.name.lowercase().replace('_', ' ')
        val location = patch.site.replace('_', ' ')
        mes("You rub the Amulet of Nature, which says... 'I am bound to your $type patch in $location.'")
        mes(
            when {
                crop == null -> "There is nothing growing in your patch."
                state.health == Health.DEAD -> "I am afraid that your patch has succumbed to disease and died, my friend. I am truly sorry."
                state.health == Health.DISEASED -> "I am afraid that your patch has succumbed to disease, my friend. You must attend to it immediately, before it dies."
                state.grown(crop) -> "Your patch has now grown to fruition, my friend."
                else -> "Your patch is growing nicely, free from disease and is at state ${state.stage}/${crop.stages}."
            }
        )
    }

    internal fun refreshed(player: Player, after: Array<PatchState>) {
        val patch = bound(player) ?: return
        val index = FarmingPatches.all.indexOf(patch)
        val current = after[index]
        val remembered = PatchState.unpack(player.vars[LAST_STATUS])
        VarPlayerIntMapSetter.set(player, LAST_STATUS, current.pack())
        if (!shouldNotify(current, remembered)) return
        if (AMULET in player.inv || AMULET in player.worn) {
            player.mes("A low hum resonates from the Amulet of Nature - it seeks audience with you.")
        }
    }

    private fun remember(player: Player, patch: PatchDef) {
        if (bound(player) == patch) {
            VarPlayerIntMapSetter.set(player, LAST_STATUS, farming.stored(player, patch).pack())
        }
    }

    private fun bound(player: Player): PatchDef? {
        val varp = player.vars[BOUND_PATCH]
        if (varp == 0) return null
        return FarmingPatches.all.firstOrNull { it.varp.asRSCM(RSCMType.VARP) == varp }
    }

    companion object {
        const val AMULET = "obj.nature_amulet"
        private const val BOUND_PATCH = "varp.farming_nature_patch"
        private const val LAST_STATUS = "varp.farming_nature_status"

        internal fun shouldNotify(after: PatchState, remembered: PatchState): Boolean {
            val final = status(after)
            return final != 0 && (final != status(remembered) || after.cropIndex != remembered.cropIndex)
        }

        private fun status(state: PatchState): Int {
            val crop = state.crop ?: return 0
            return when {
                state.health == Health.DEAD -> 2
                state.health == Health.DISEASED -> 1
                state.grown(crop) -> 3
                else -> 0
            }
        }
    }
}
