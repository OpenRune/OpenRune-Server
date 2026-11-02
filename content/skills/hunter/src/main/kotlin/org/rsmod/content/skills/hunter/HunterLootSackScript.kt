package org.rsmod.content.skills.hunter

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.table.hunter.HunterLootSackCascadesRow
import org.rsmod.api.table.hunter.HunterLootSacksRow
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.interfaces.collectionlog.collectionTransmit
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.skills.hunter.HunterRumours.Companion.COMPLETED
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterLootSackScript
@Inject
constructor(private val objRepo: ObjRepository, private val pets: PetRewards) : PluginScript() {
    override fun ScriptContext.startup() {
        for (sack in HunterLootSacksRow.all()) {
            onOpHeld1(sack.sack.internalName) { open(sack) }
        }
    }

    private fun ProtectedAccess.open(sack: HunterLootSacksRow) {
        if (invDel(inv, sack.sack.internalName).failure) return
        val weight = sack.loot.size + if (sack.herbs) 2 else 1
        repeat(sack.rolls) {
            val roll = random.of(weight)
            when {
                roll < sack.loot.size -> {
                    val (obj, min, max, noted) = sack.loot[roll]
                    give(if (noted) ocCert(obj.internalName) else obj, random.of(min, max))
                }
                roll == sack.loot.size && sack.herbs -> give(cascade(HERBS, HERBLORE), CASCADE_COUNT, true)
                else -> give(cascade(LOGS, WOODCUTTING), CASCADE_COUNT, true)
            }
        }
        milestones(sack)
        if (sack.uniques) uniques(sack.expertUniques)
        soundSynth(OPEN_SYNTH)
    }

    private fun ProtectedAccess.cascade(row: String, stat: String): ItemServerType {
        val entries = HunterLootSackCascadesRow.getRow(row).entries
        val level = stat(stat)
        for ((obj, low, high, req) in entries) {
            if (level < req) continue
            if (random.randomDouble() < SkillingSuccessRate.successRate(low, high, level.coerceIn(1, 99), 99)) {
                return obj
            }
        }
        return entries.last().t0
    }

    private fun ProtectedAccess.give(type: ItemServerType, count: Int, noted: Boolean = false) {
        invAddOrDropType(objRepo, if (noted) ocCert(type.internalName) else type, count)
    }

    private fun ProtectedAccess.unique(obj: String) {
        invAddOrDrop(objRepo, obj)
        CollectionLog.grant(player, obj)
    }

    private fun ProtectedAccess.milestones(sack: HunterLootSacksRow) {
        val given = vars[TORN_BLUEPRINTS]
        if (vars[COMPLETED] >= ENHANCED_MILESTONE && given and TORN_ENHANCED == 0 && !obtained(ENHANCED)) {
            invAddOrDrop(objRepo, TORN_ENHANCED_BLUEPRINT)
            VarPlayerIntMapSetter.set(player, TORN_BLUEPRINTS, vars[TORN_BLUEPRINTS] or TORN_ENHANCED)
        }
        if (
            sack.expertUniques && vars[COMPLETED] >= PERFECTED_MILESTONE && given and TORN_PERFECTED == 0 &&
                !obtained(PERFECTED)
        ) {
            invAddOrDrop(objRepo, TORN_PERFECTED_BLUEPRINT)
            VarPlayerIntMapSetter.set(player, TORN_BLUEPRINTS, vars[TORN_BLUEPRINTS] or TORN_PERFECTED)
        }
    }

    private fun ProtectedAccess.uniques(expert: Boolean) {
        if (random.of(UNIQUE_DEN) == 0) unique(ENHANCED)
        if (random.of(UNIQUE_DEN) == 0 && !owns(HUNTSMANS_KIT)) unique(HUNTSMANS_KIT)
        if (random.of(UNIQUE_DEN) == 0) unique(OUTFIT.firstOrNull { !obtained(it) } ?: OUTFIT[random.of(OUTFIT.size)])
        if (!expert) return
        if (random.of(UNIQUE_DEN) == 0 && obtained(ENHANCED)) unique(PERFECTED)
        val petDen = if (player.statMap.getXP(HUNTER_STAT) >= MAX_XP) PET_DEN_MAX_XP else PET_DEN
        if (random.of(petDen) == 0) pets.give(player, PET)
    }

    private fun ProtectedAccess.obtained(obj: String): Boolean = player.collectionTransmit.count(obj) > 0

    private fun ProtectedAccess.owns(obj: String): Boolean =
        obj in inv || obj in player.worn || obj in inv(HunterRumours.BANK)

    internal companion object {
        const val HERBS = "dbrow.hunter_loot_sack_herbs"
        const val LOGS = "dbrow.hunter_loot_sack_logs"
        const val HERBLORE = "stat.herblore"
        const val WOODCUTTING = "stat.woodcutting"
        const val HUNTER_STAT = "stat.hunter"
        const val CASCADE_COUNT = 4
        const val OPEN_SYNTH = "synth.hunter_loot_sack_open"

        const val TORN_BLUEPRINTS = "varp.hunter_rumour_torn_blueprints"
        const val TORN_ENHANCED = 1
        const val TORN_PERFECTED = 2
        const val ENHANCED_MILESTONE = 100
        const val PERFECTED_MILESTONE = 250

        const val ENHANCED = "obj.hg_whistle_blueprint_2"
        const val PERFECTED = "obj.hg_whistle_blueprint_3"
        const val TORN_ENHANCED_BLUEPRINT = "obj.hg_whistle_blueprint_2_torn"
        const val TORN_PERFECTED_BLUEPRINT = "obj.hg_whistle_blueprint_3_torn"
        const val HUNTSMANS_KIT = "obj.huntsmans_kit"
        const val PET = "obj.quetzalpet"
        const val SUPPLIES = "obj.hg_lootsack_trade"
        val OUTFIT = listOf(HunterOutfit.HOOD, HunterOutfit.TOP, HunterOutfit.LEGS, HunterOutfit.BOOTS)
        val TRADEABLE = OUTFIT.toSet() + ENHANCED + PERFECTED

        const val UNIQUE_DEN = 50
        const val PET_DEN = 1000
        const val PET_DEN_MAX_XP = 67
        const val MAX_XP = 200_000_000
    }
}
