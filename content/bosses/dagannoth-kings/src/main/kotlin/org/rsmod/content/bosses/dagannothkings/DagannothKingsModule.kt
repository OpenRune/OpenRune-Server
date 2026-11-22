package org.rsmod.content.bosses.dagannothkings

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.plugin.module.PluginModule

class DagannothKingsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(DagannothKingsSlayerGate::class.java)
        addSetBinding<NpcAttackValidateHook>(DagannothWaterGate::class.java)
        addSetBinding<NpcAttackValidateHook>(DagannothDoorSupportGate::class.java)
        addSetBinding<NpcDeathDropHook>(DagannothBonesNotedHook::class.java)
        addSetBinding<NpcDeathKillHook>(WaterbirthDoorSupports::class.java)
    }
}
