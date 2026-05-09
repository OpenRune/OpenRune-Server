package org.rsmod.content.skills.prayer.blessed

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.prayer.items.ZealotRobes.countConsumed
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LibationBowlEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLocU("loc.varlamore_libation_bowl", "obj.jug_wine_blessed") { fillLibationBowl(sunfire = false) }
        onOpLocU("loc.varlamore_libation_bowl", "obj.jug_sunfire_wine_blessed") { fillLibationBowl(sunfire = true) }
        onOpLoc1("loc.varlamore_libation_bowl") { fillLibationBowlFromMenu() }
        onOpLoc1("loc.varlamore_libation_bowl_full") { startLibationSacrifice(it.vis) }
        onOpLoc2("loc.varlamore_libation_bowl_full") { mes("The libation bowl has $libationWineCharges charges remaining.") }
        onPlayerQueueWithArgs("queue.prayer_libation_sacrifice") { processLibationTick(it.args) }
    }

    private suspend fun ProtectedAccess.fillLibationBowl(sunfire: Boolean) {
        if (player.basePrayerLvl < 30) {
            mes("You need a Prayer level of 30 to use the libation bowl.")
            return
        }
        val wine = if (sunfire) "obj.jug_sunfire_wine_blessed" else "obj.jug_wine_blessed"
        if (invDel(inv, wine, 1).failure) {
            return
        }
        libationWineCharges = 400
        libationSunfire = if (sunfire) 1 else 0
        objbox(wine, "You pour some blessed ${if (sunfire) "sunfire" else ""} wine into the libation bowl.")
    }

    private suspend fun ProtectedAccess.fillLibationBowlFromMenu() {
        val hasBlessedWine = inv.contains("obj.jug_wine_blessed")
        val hasBlessedSunfireWine = inv.contains("obj.jug_sunfire_wine_blessed")
        when {
            hasBlessedWine && hasBlessedSunfireWine -> {
                openSkillMulti(SkillMultiConfig(verb = "use", entries = listOf(
                    SkillMultiEntry("obj.jug_wine_blessed"),
                    SkillMultiEntry("obj.jug_sunfire_wine_blessed")
                ))) { selection ->
                    when (selection.entry.item.internalName) {
                        "obj.jug_sunfire_wine_blessed" -> fillLibationBowl(sunfire = true)
                        "obj.jug_wine_blessed" -> fillLibationBowl(sunfire = false)
                    }
                }
            }
            hasBlessedSunfireWine -> fillLibationBowl(sunfire = true)
            hasBlessedWine -> fillLibationBowl(sunfire = false)
            else -> mes("You need a jug of blessed wine to fill the libation bowl.")
        }
    }

    private suspend fun ProtectedAccess.startLibationSacrifice(loc: BoundLocInfo) {
        if (player.basePrayerLvl < 30) {
            mes("You need a Prayer level of 30 to use the libation bowl.")
            return
        }
        if (player.prayerLvl < 2) {
            mesbox("You need at least two prayer points to sacrifice blessed bone shards into the libation bowl.")
            return
        }
        val task = LibationTask(loc = loc)
        if (!canLibationSacrifice(task)) {
            return
        }
        processLibationTick(task)
    }

    private fun ProtectedAccess.processLibationTick(task: LibationTask) {
        if (!canLibationSacrifice(task)) {
            return
        }
        val wineXpPerShard = if (libationSunfire == 1) 6.0 else 5.0
        val availableShards = inv.count("obj.blessed_bone_shard")
        val wineCharges = libationWineCharges
        val prayerShardCapacity = player.prayerLvl / 2
        val count = minOf(100, availableShards, wineCharges, prayerShardCapacity)
        if (count <= 0) {
            return
        }
        val consumedCount = player.countConsumed(count)

        player.anim("seq.human_pray_blessed_bone_shards_01")
        if (consumedCount > 0) {
            invDel(inv, "obj.blessed_bone_shard", consumedCount)
        }
        libationWineCharges = (wineCharges - count).coerceAtLeast(0)
        statSub("stat.prayer", constant = 2, percent = 0)
        statAdvance("stat.prayer", count * wineXpPerShard)

        if (libationWineCharges <= 0) {
            if (!tryAutoRefillLibationBowl()) {
                mes("The libation bowl runs dry.")
                return
            }
        }

        if (canLibationSacrifice(task)) {
            weakQueue("queue.prayer_libation_sacrifice", 4, task)
        }
    }

    private fun ProtectedAccess.canLibationSacrifice(task: LibationTask): Boolean {
        return when {
            !isWithinDistance(task.loc, 1) -> false
            libationWineCharges <= 0 -> false
            inv.count("obj.blessed_bone_shard") <= 0 -> false
            player.prayerLvl < 2 -> false
            else -> true
        }
    }

    private fun ProtectedAccess.tryAutoRefillLibationBowl(): Boolean {
        val preferredSunfire = libationSunfire == 1
        val nextSunfire = when {
            preferredSunfire && inv.contains("obj.jug_sunfire_wine_blessed") -> true
            !preferredSunfire && inv.contains("obj.jug_wine_blessed") -> false
            inv.contains("obj.jug_sunfire_wine_blessed") -> true
            inv.contains("obj.jug_wine_blessed") -> false
            else -> return false
        }

        val wine = if (nextSunfire) "obj.jug_sunfire_wine_blessed" else "obj.jug_wine_blessed"
        if (invDel(inv, wine, 1).failure) {
            return false
        }

        libationSunfire = if (nextSunfire) 1 else 0
        libationWineCharges = 400
        mes("You pour some more blessed ${if (nextSunfire) "sunfire " else ""}wine into the libation bowl.")
        return true
    }

    private data class LibationTask(val loc: BoundLocInfo)

    private var ProtectedAccess.libationWineCharges by intVarBit("varbit.varlamore_prayer_winequant")
    private var ProtectedAccess.libationSunfire by intVarBit("varbit.varlamore_prayer_winetype")

}
