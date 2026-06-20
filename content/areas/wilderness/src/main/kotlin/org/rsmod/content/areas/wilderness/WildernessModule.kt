package org.rsmod.content.areas.wilderness

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.death.PvPPlayerHitHook
import org.rsmod.api.death.PvPSkullHook
import org.rsmod.api.death.PvPSpecialAttackHook
import org.rsmod.api.player.hook.PlayerGroundItemDropHook
import org.rsmod.api.player.hook.PlayerInvUpdateHook
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

public class WildernessModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathHook>(WildernessDeathHook::class.java)
        addSetBinding<PlayerDeathCleanupHook>(WildernessDeathCleanupHook::class.java)
        addSetBinding<PvPAttackValidateHook>(WildernessPvPHook::class.java)
        addSetBinding<PvPSkullHook>(WildernessPvPHook::class.java)
        addSetBinding<PvPPlayerHitHook>(WildernessPvPHook::class.java)
        addSetBinding<PvPSpecialAttackHook>(WildernessSpecialAttackHook::class.java)
        addSetBinding<NpcDeathKillHook>(ForinthrySurgeKillHook::class.java)
        addSetBinding<PlayerInvUpdateHook>(WildernessLootKeySkullHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(WildernessTeleportHook::class.java)
        addSetBinding<PlayerGroundItemDropHook>(WildernessGroundItemHook::class.java)
        addSetBinding<PlayerObjTakeValidateHook>(WildernessGroundItemHook::class.java)
    }
}
