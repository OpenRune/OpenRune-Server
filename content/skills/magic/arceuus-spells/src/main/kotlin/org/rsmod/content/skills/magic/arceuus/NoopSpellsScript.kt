package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NoopSpellsScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (spell in NoopSpell.entries) {
            onIfOverlayButton(spell.component) { cast(spell) }
        }
    }

    private fun ProtectedAccess.cast(spell: NoopSpell) {
        val spellObj = ServerCacheManager.getItem(spell.obj.asRSCM(RSCMType.OBJ)) ?: return
        val magicSpell = spells.getObjSpell(spellObj) ?: return

        if (runes.attemptCast(player, magicSpell).isFailure()) {
            return
        }

        statAdvance("stat.magic", magicSpell.castXp)
        anim(spell.anim)
        spotanim(spell.spotanim)
    }

    internal enum class NoopSpell(
        val obj: String,
        val component: String,
        val anim: String,
        val spotanim: String,
    ) {
        ResurrectCrops(
            obj = "obj.br_swordfish",
            component = "component.magic_spellbook:resurrect_crops",
            anim = "seq.res_necromancy_plant_spell",
            spotanim = "spotanim.arceuus_crop_res_spotanim",
        ),
        LesserGhost(
            obj = "obj.osb8_monkey_cow",
            component = "component.magic_spellbook:resurrect_lesser_ghost",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_ghost_cast_spotanim",
        ),
        LesserSkeleton(
            obj = "obj.osb8_monkey_bob",
            component = "component.magic_spellbook:resurrect_lesser_skeleton",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_skeleton_cast_spotanim",
        ),
        LesserZombie(
            obj = "obj.osb8_monkey_donie",
            component = "component.magic_spellbook:resurrect_lesser_zombie",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_zombie_cast_spotanim",
        ),
        SuperiorGhost(
            obj = "obj.osb8_bamboo",
            component = "component.magic_spellbook:resurrect_superior_ghost",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_ghost_cast_spotanim",
        ),
        SuperiorSkeleton(
            obj = "obj.osb8_monkey_unicorn",
            component = "component.magic_spellbook:resurrect_superior_skeleton",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_skeleton_cast_spotanim",
        ),
        SuperiorZombie(
            obj = "obj.osb8_monkey_sheep",
            component = "component.magic_spellbook:resurrect_superior_zombie",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_zombie_cast_spotanim",
        ),
        GreaterGhost(
            obj = "obj.osb8_monkey_trap",
            component = "component.magic_spellbook:resurrect_greater_ghost",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_ghost_cast_spotanim",
        ),
        GreaterSkeleton(
            obj = "obj.osb8_monkey_aereck",
            component = "component.magic_spellbook:resurrect_greater_skeleton",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_skeleton_cast_spotanim",
        ),
        GreaterZombie(
            obj = "obj.osb8_unicorn_horn",
            component = "component.magic_spellbook:resurrect_greater_zombie",
            anim = "seq.human_spellcast_resurrect",
            spotanim = "spotanim.resurrect_zombie_cast_spotanim",
        ),
    }
}
