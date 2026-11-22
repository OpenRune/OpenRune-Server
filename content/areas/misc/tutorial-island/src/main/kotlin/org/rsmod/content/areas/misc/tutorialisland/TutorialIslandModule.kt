package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.hook.HeldEquipValidateHook
import org.rsmod.api.player.hook.PlayerHomeTeleportDestinationHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.spells.attack.SpellCastHook
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.content.skills.smithing.SmithingRestrictHook
import org.rsmod.plugin.module.PluginModule

internal class TutorialIslandModule : PluginModule() {
    override fun bind() {
        addSetBinding<XpMod>(TutorialXpMod::class.java)
        addSetBinding<NpcDeathKillHook>(TutorialRatKillHook::class.java)
        addSetBinding<NpcAttackValidateHook>(TutorialRatAttackHook::class.java)
        addSetBinding<SpellCastHook>(TutorialSpellCastHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(TutorialTeleportHook::class.java)
        addSetBinding<HeldEquipValidateHook>(TutorialEquipHook::class.java)
        addSetBinding<SmithingRestrictHook>(TutorialSmithingHook::class.java)
        addSetBinding<PlayerHomeTeleportDestinationHook>(
            TutorialHomeTeleportDestination::class.java,
        )
    }
}
