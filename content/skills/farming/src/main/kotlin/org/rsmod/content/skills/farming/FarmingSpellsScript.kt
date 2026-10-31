package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpLocT
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingSpellsScript
@Inject
constructor(
    private val farming: FarmingService,
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val xpMods: XpModifiers,
    private val geomancy: FarmingGeomancy,
    private val worldRepo: WorldRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLocT(CURE) {
            val target = checkNotNull(ServerCacheManager.getObject(it.loc.id)).internalName
            val patch = FarmingPatches.byLoc(target)
            if (patch == null) {
                mes(
                    if (target.startsWith("loc.farming_compost_")) "Bins don't often get diseased."
                    else "Umm... this spell won't cure that!"
                )
            } else {
                cure(patch)
            }
        }
        onOpLocT(FERTILE) {
            val target = checkNotNull(ServerCacheManager.getObject(it.loc.id)).internalName
            val patch = FarmingPatches.byLoc(target)
            if (patch == null) {
                mes(
                    if (target.startsWith("loc.farming_compost_")) "No, that would be silly."
                    else "Um... I don't want to fertilize that!"
                )
            } else {
                fertile(patch)
            }
        }
        onIfOverlayButton(HUMIDIFY) {
            if (!FarmingHumidify.hasTarget(player)) {
                return@onIfOverlayButton mes("You have nothing to humidify.")
            }
            val spell = spell(HUMIDIFY)
            if (runes.attemptCast(player, spell).isFailure()) return@onIfOverlayButton
            FarmingHumidify.fill(player)
            magicXp(spell)
            anim("seq.dream_player_humidify_spell")
            spotanim("spotanim.dream_humidify_spell_spotanim")
            delay(4)
        }
        onIfOverlayButton(GEOMANCY) {
            val spell = spell(GEOMANCY)
            if (runes.attemptCast(player, spell).isFailure()) return@onIfOverlayButton
            magicXp(spell)
            anim("seq.lunar_human_magic_geomancy")
            spotanim("spotanim.quest_lunar_spellbook_geomancy", delay = 30)
            soundArea(worldRepo, coords, "synth.farming_geomancy", delay = 15, radius = 3)
            delay(4)
            geomancy.open(this)
        }
    }

    private fun ProtectedAccess.cure(patch: PatchDef) {
        val before = farming.state(player, patch)
        val spell = spell(CURE)
        val result = farming.cure(player, patch, expected = before, allowPruning = true) {
            !runes.attemptCast(player, spell).isFailure()
        }
        when (result) {
            CureResult.CURED -> {
                magicXp(spell)
                farmingXp(patch, 91.5)
                anim("seq.quest_lunar_cure_plant")
            }
            CureResult.EMPTY ->
                mes(if (before.cleared) "There's nothing there to cure." else "The weeds are healthy enough already.")
            CureResult.DEAD ->
                mes(
                    "It says 'Cure' not 'Resurrect'. Although death may arise from disease, " +
                        "it is not in itself a disease and hence cannot be cured. So there."
                )
            CureResult.NOT_DISEASED ->
                mes(if (before.crop?.let(before::grown) == true) "That's not diseased." else "It's growing just fine.")
            CureResult.PRUNE_ONLY, CureResult.STALE, CureResult.UNPAID -> Unit
        }
    }

    private suspend fun ProtectedAccess.fertile(patch: PatchDef) {
        val before = farming.state(player, patch)
        val crop = before.crop
        if (crop != null && before.grown(crop)) {
            return mes("Composting it isn't going to make it any bigger.")
        }
        val spell = spell(FERTILE)
        if (!runes.canCastSpell(player, spell)) return
        val unlocked = vars[ULTRA_UNLOCK] == 1
        if (unlocked && inv.count(ASH) < 2 && vars[SOIL_WARNING] == 0) {
            val answer = choice3(
                "Cast with supercompost.", 1,
                "Cast with supercompost, and don't warn me again.", 2,
                "Cancel.", 3,
                title = "You need two volcanic ash to use ultracompost.",
            )
            if (answer == 3) return
            if (answer == 2) VarPlayerIntMapSetter.set(player, SOIL_WARNING, 1)
        }
        val ultra = unlocked && inv.count(ASH) >= 2
        val compost = if (ultra) Compost.ULTRA else Compost.SUPER
        when (farming.treat(player, patch, compost, expected = before) {
            if (runes.attemptCast(player, spell).isFailure()) false
            else {
                if (ultra) check(invDel(inv, ASH, 2).success)
                true
            }
        }) {
            TreatResult.TREATED -> {
                publish(FarmingFertileSoilEvent(player, patch))
                magicXp(spell)
                farmingXp(patch, 18.0)
                anim("seq.quest_lunar_spell_raining_fertile_soil")
                spotanim("spotanim.quest_lunar_spellbook_fertile_soil_spot_anim")
            }
            TreatResult.WEEDS -> mes("This patch needs weeding first.")
            TreatResult.GROWN -> mes("Composting it isn't going to make it any bigger.")
            TreatResult.ALREADY, TreatResult.DEAD, TreatResult.STALE, TreatResult.UNPAID -> Unit
        }
    }

    private fun spell(component: String): MagicSpell =
        spells.allSpells().single { it.component.packed == component.asRSCM(RSCMType.COMPONENT) }

    private fun ProtectedAccess.magicXp(spell: MagicSpell) {
        statAdvance("stat.magic", spell.castXp * xpMods.get(player, "stat.magic"))
    }

    private fun ProtectedAccess.farmingXp(patch: PatchDef, xp: Double) {
        val bonus = 1 + FarmingPatches.xpBonusPercent(player, patch) / 100.0
        statAdvance(STAT_FARMING, xp * bonus * xpMods.get(player, STAT_FARMING))
    }

    internal companion object {
        const val CURE = "component.magic_spellbook:cure_plant"
        const val FERTILE = "component.magic_spellbook:fertile_soil"
        const val HUMIDIFY = "component.magic_spellbook:humidify"
        const val GEOMANCY = "component.magic_spellbook:geomancy"
        const val ULTRA_UNLOCK = "varbit.fossil_mine_unlocked_ultrafertilesoil"
        const val SOIL_WARNING = "varp.farming_fertile_soil_warning"
        const val ASH = "obj.fossil_volcanic_ash"
    }
}
