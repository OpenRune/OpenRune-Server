package org.rsmod.content.areas.godwars

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
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
        FOLLOWER_VARBITS[context.npc.id]?.let { player.increment(it) }
        AVATAR_VARPS[context.npc.id]?.let { player.increment(it) }
    }

    private fun Player.increment(varp: String) {
        VarPlayerIntMapSetter.set(this, varp, vars[varp] + 1)
    }

    private companion object {
        private const val BANDOS_VARBIT = "varbit.godwars_counter_bandos"
        private const val ARMADYL_VARBIT = "varbit.godwars_counter_armadyl"
        private const val SARADOMIN_VARBIT = "varbit.godwars_counter_saradomin"
        private const val ZAMORAK_VARBIT = "varbit.godwars_counter_zamorak"

        private val BANDOS_FOLLOWERS =
            listOf(
                "npc.godwars_spiritual_bandos_mage",
                "npc.godwars_spiritual_bandos_ranger",
                "npc.godwars_spiritual_bandos_warrior",
                "npc.godwars_ancient_ork1",
                "npc.godwars_ancient_ork2",
                "npc.godwars_ancient_ork3",
                "npc.godwars_ancient_ork4",
                "npc.godwars_ancient_cyclops",
                "npc.godwars_ancient_cyclops2",
                "npc.godwars_ancient_jogre",
                "npc.godwars_ancient_ogre",
                "npc.godwars_ancient_hobgoblin",
                "npc.godwars_goblin1",
                "npc.godwars_goblin2",
                "npc.godwars_goblin3",
                "npc.godwars_goblin4",
                "npc.godwars_goblin5",
                "npc.godwars_sergeant_goblin1",
                "npc.godwars_sergeant_goblin2",
                "npc.godwars_sergeant_goblin3",
                "npc.godwars_bandos_avatar",
            )

        private val ARMADYL_FOLLOWERS =
            listOf(
                "npc.godwars_spiritual_armadyl_mage",
                "npc.godwars_spiritual_armadyl_ranger",
                "npc.godwars_spiritual_armadyl_warrior",
                "npc.godwars_armadyl_female_armor01_blue",
                "npc.godwars_armadyl_female_armor01_green",
                "npc.godwars_armadyl_female_armor01_red",
                "npc.godwars_armadyl_female_armor02_blue",
                "npc.godwars_armadyl_female_armor02_green",
                "npc.godwars_armadyl_female_armor02_red",
                "npc.godwars_armadyl_female_armor03_blue",
                "npc.godwars_armadyl_male_armor01_blue",
                "npc.godwars_armadyl_male_armor01_green",
                "npc.godwars_armadyl_male_armor01_red",
                "npc.godwars_armadyl_male_armor02_blue",
                "npc.godwars_armadyl_male_armor02_green",
                "npc.godwars_armadyl_male_armor02_red",
                "npc.godwars_armadyl_male_armor03_green",
                "npc.godwars_armadyl_male_armor03_red",
                "npc.godwars_armadyl_bodyguard_geerin",
                "npc.godwars_armadyl_bodyguard_kilisa",
                "npc.godwars_armadyl_bodyguard_skree",
                "npc.godwars_armadyl_avatar",
            )

        private val SARADOMIN_FOLLOWERS =
            listOf(
                "npc.godwars_spiritual_saradomin_mage",
                "npc.godwars_spiritual_saradomin_ranger",
                "npc.godwars_spiritual_saradomin_warrior",
                "npc.godwars_saradomin_knight_1",
                "npc.godwars_saradomin_knight_2",
                "npc.godwars_ancient_saradomin_wizard",
                "npc.godwars_saradomin_unicorn",
                "npc.godwars_saradomin_lion",
                "npc.godwars_saradomin_centaur",
                "npc.godwars_saradomin_avatar",
            )

        private val ZAMORAK_FOLLOWERS =
            listOf(
                "npc.godwars_spiritual_zamorak_mage",
                "npc.godwars_spiritual_zamorak_ranger",
                "npc.godwars_spiritual_zamorak_warrior",
                "npc.godwars_bloodveld",
                "npc.godwars_gorak",
                "npc.godwars_icefiend_1",
                "npc.godwars_pyrefiend_1",
                "npc.godwars_ancient_hellhound",
                "npc.godwars_ancient_imp",
                "npc.godwars_ancient_vampire",
                "npc.godwars_ancient_werewolf1",
                "npc.godwars_ancient_werewolf2",
                "npc.godwars_ancient_black_demon",
                "npc.godwars_ancient_greater_demon",
                "npc.godwars_ancient_lesser_demon",
                "npc.godwars_zamorak_avatar",
            )

        private val FOLLOWER_VARBITS: Map<Int, String> =
            buildMap {
                putVarp(BANDOS_FOLLOWERS, BANDOS_VARBIT)
                putVarp(ARMADYL_FOLLOWERS, ARMADYL_VARBIT)
                putVarp(SARADOMIN_FOLLOWERS, SARADOMIN_VARBIT)
                putVarp(ZAMORAK_FOLLOWERS, ZAMORAK_VARBIT)
            }

        private val AVATAR_VARPS: Map<Int, String> =
            buildMap {
                putVarp(listOf("npc.godwars_bandos_avatar"), "varp.total_bandos_kills")
                putVarp(listOf("npc.godwars_armadyl_avatar"), "varp.total_armadyl_kills")
                putVarp(listOf("npc.godwars_saradomin_avatar"), "varp.total_saradomin_kills")
                putVarp(listOf("npc.godwars_zamorak_avatar"), "varp.total_zamorak_kills")
            }

        private fun MutableMap<Int, String>.putVarp(npcNames: List<String>, varp: String) {
            for (name in npcNames) {
                put(name.asRSCM(RSCMType.NPC), varp)
            }
        }
    }
}
