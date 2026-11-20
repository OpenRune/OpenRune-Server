package org.rsmod.content.events.demonicpacts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.NpcDropReceiveHook
import org.rsmod.game.entity.Player
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.content.skills.herblore.BrewBonus
import org.rsmod.content.skills.herblore.HerbloreHook
import dev.openrune.ServerCacheManager
import org.rsmod.plugin.module.PluginModule

class DemonicPactsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(LeagueKillHook::class.java)
        addSetBinding<NpcDeathDropHook>(SoulHarvestDropHook::class.java)
        addSetBinding<NpcDropReceiveHook>(MinionLootReceiveHook::class.java)
        addSetBinding<HerbloreHook>(ForagerHerbloreHook::class.java)
    }
}

internal class LeagueKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        if (player.isDemonicPactsLeague) {
            LeagueTasks.onKill(player, context.npc.name)
        }
    }
}

/**
 * Soul Harvest: bones and ashes a monster drops for you become soul shards, one per regular bone's
 * worth of Prayer xp, straight into your inventory.
 */
internal class SoulHarvestDropHook : NpcDeathDropHook {
    private val prayerXpByItem: Map<String, Double> by lazy {
        SkillPrayerRow.all().associate { it.item.internalName to it.exp.toDouble() }
    }

    override fun tryConsume(context: NpcDeathDropContext): Boolean {
        val player = context.hero
        if (context.dropType.isCert || !player.hasRelic(RelicNames.SOUL_HARVEST)) {
            return false
        }
        val xp = prayerXpByItem[context.dropType.internalName] ?: return false
        val bonesXp = prayerXpByItem[REGULAR_BONES] ?: return false
        val shards = maxOf(1, Math.round(xp / bonesXp).toInt())
        if (player.invAdd(player.inv, SOUL_SHARD.asRSCM(RSCMType.OBJ), shards).failure) {
            return false
        }
        player.mes("<col=ef1020>Soul Harvest: +$shards soul shards.")
        return true
    }

    private companion object {
        const val REGULAR_BONES = "obj.bones"
        const val SOUL_SHARD = "obj.league_soul_shard"
    }
}

/**
 * Minion: while summoned, it picks up your kills' drops into your inventory, noted where possible.
 * Bones and ashes are left for Soul Harvest when you have it.
 */
internal class MinionLootReceiveHook : NpcDropReceiveHook {
    private val bonesAndAshes: Set<String> by lazy { SkillPrayerRow.all().map { it.item.internalName }.toSet() }

    override fun tryReceive(receiver: Player, obj: String, count: Int): Boolean {
        if (!RelicCompanionsScript.isMinionActive(receiver)) {
            return false
        }
        if (obj in bonesAndAshes && receiver.hasRelic(RelicNames.SOUL_HARVEST)) {
            return false
        }
        val id = obj.asRSCM(RSCMType.OBJ)
        return receiver.invAdd(receiver.inv, id, count, cert = true).success
    }
}

/** Friendly Forager: potions come out with 4 doses instead of 3. */
internal class ForagerHerbloreHook : HerbloreHook {
    private val fourDoseByName: Map<String, String> by lazy {
        ServerCacheManager.getItems()
            .values
            .filter { !it.isCert && !it.isPlaceholder && it.name.endsWith("(4)") }
            .associate { it.name.lowercase() to it.internalName }
    }

    override fun brewBonus(player: Player, potion: HerbloreFinishedRow): BrewBonus? {
        if (!player.hasRelic(RelicNames.FRIENDLY_FORAGER)) {
            return null
        }
        val name = potion.output.name
        if (!name.endsWith("(3)")) {
            return null
        }
        val fourDose = fourDoseByName[name.dropLast(3).lowercase() + "(4)"] ?: return null
        return BrewBonus(fourDose, "The forager's pouch helps you make a 4-dose potion.")
    }
}
