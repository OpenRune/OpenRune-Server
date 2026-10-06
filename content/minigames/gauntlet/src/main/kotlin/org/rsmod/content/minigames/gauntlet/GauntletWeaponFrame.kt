package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.plugin.module.PluginModule

class GauntletWeaponFrameModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(GauntletWeaponFrameHook::class.java)
    }
}

@Singleton
class GauntletWeaponFrameHook
@Inject
constructor(private val runs: GauntletRuns, private val objRepo: ObjRepository) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        val corrupted = npc.isType("npc.crystal_rat_hm") || npc.isType("npc.crystal_spider_hm") ||
            npc.isType("npc.crystal_bat_hm")
        val normal = npc.isType("npc.crystal_rat") || npc.isType("npc.crystal_spider") ||
            npc.isType("npc.crystal_bat")
        if (!corrupted && !normal) return
        val run = runs.runFor(context.hero) ?: return
        if (run.weaponFrameGiven) return
        run.weaponFrameGiven = true
        val frame = gauntletObj("generic_component", corrupted)
        objRepo.add(frame, context.dropCoords, constants.lootdrop_duration, context.hero, 1)
    }
}
