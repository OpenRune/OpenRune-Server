package org.rsmod.content.areas.godwars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.bits
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule
import org.rsmod.utils.bits.bitMask

public class GodwarsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(GodwarsKillCountHook::class.java)
    }
}

internal class GodwarsKillCountHook @Inject constructor(private val areas: AreaChecker) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        val npc = context.npc
        if (!areas.inArea("area.godwars_dungeon", player.coords)) return
        val faction = FACTIONS.firstOrNull { it.matches(npc) } ?: return
        player.increment(faction.counterVarbit)
    }

    private fun Player.increment(varbit: String) {
        val next = vars[varbit] + 1
        val type = ServerCacheManager.getVarbit(varbit.asRSCM(RSCMType.VARBIT))
        val max = type?.bits?.bitMask ?: Int.MAX_VALUE.toLong()
        VarPlayerIntMapSetter.set(this, varbit, next.coerceAtMost(max.toInt()))
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
