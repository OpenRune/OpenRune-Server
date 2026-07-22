package org.rsmod.content.areas.godwars

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

public class GodwarsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(GodwarsKillCountHook::class.java)
    }
}

internal class GodwarsKillCountHook @Inject constructor() : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        val npc = context.npc
        val faction = FACTIONS.firstOrNull { it.matches(npc) } ?: return
        player.increment(faction.counterVarbit)
    }

    private fun Player.increment(varp: String) {
        VarPlayerIntMapSetter.set(this, varp, vars[varp] + 1)
    }

    private class Faction(
        avatarName: String,
        private val bodyguardCategory: String,
        private val followerCategory: String,
        val counterVarbit: String,
    ) {
        val avatarId: Int by lazy { avatarName.asRSCM(RSCMType.NPC) }

        fun matches(npc: Npc): Boolean =
            npc.id == avatarId ||
                npc.visType.isCategoryType(bodyguardCategory) ||
                npc.visType.isCategoryType(followerCategory)
    }

    private companion object {
        private val FACTIONS =
            listOf(
                Faction(
                    avatarName = "npc.godwars_bandos_avatar",
                    bodyguardCategory = "category.godwars_bandos_bodyguard",
                    followerCategory = "category.godwars_bandos_follower",
                    counterVarbit = "varbit.godwars_counter_bandos",
                ),
                Faction(
                    avatarName = "npc.godwars_armadyl_avatar",
                    bodyguardCategory = "category.godwars_armadyl_bodyguard",
                    followerCategory = "category.godwars_armadyl_follower",
                    counterVarbit = "varbit.godwars_counter_armadyl",
                ),
                Faction(
                    avatarName = "npc.godwars_saradomin_avatar",
                    bodyguardCategory = "category.godwars_saradomin_bodyguard",
                    followerCategory = "category.godwars_saradomin_follower",
                    counterVarbit = "varbit.godwars_counter_saradomin",
                ),
                Faction(
                    avatarName = "npc.godwars_zamorak_avatar",
                    bodyguardCategory = "category.godwars_zamorak_bodyguard",
                    followerCategory = "category.godwars_zamorak_follower",
                    counterVarbit = "varbit.godwars_counter_zamorak",
                ),
            )
    }
}
