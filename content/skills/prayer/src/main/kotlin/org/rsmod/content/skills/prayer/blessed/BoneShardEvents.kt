package org.rsmod.content.skills.prayer.blessed

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.prayer.PrayerBlessedBoneRow
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BoneShardEvents : PluginScript() {

    private val rows = PrayerBlessedBoneRow.all()
    private val wines = mapOf(
        "obj.jug_wine" to "obj.jug_wine_blessed",
        "obj.jug_sunfire_wine" to "obj.jug_sunfire_wine_blessed",
    )

    override fun ScriptContext.startup() {

        rows.forEach { row ->
            onOpLocU("loc.varlamore_prayer_activity_altar", row.bone) { blessBones(row) }
            onOpHeld1(row.boneBlessed) { breakDownBone(row) }
            onOpHeldU(row.boneBlessed, "obj.chisel") { breakDownBone(row) }
        }

        wines.forEach { (wine, blessedWine) ->
            onOpLocU("loc.varlamore_prayer_activity_altar", wine) {
                blessWine(wine, blessedWine)
            }
        }

        onOpLoc1("loc.varlamore_prayer_activity_altar") {

            val availableBones = rows.filter { inv.contains(it.bone.internalName) }

            val availableWines = wines.filterKeys(inv::contains)

            val totalOptions = availableBones.size + availableWines.size

            if (totalOptions == 1) {

                availableBones.firstOrNull()?.let { row ->
                    blessBones(row, inv.count(row.bone.internalName))
                    return@onOpLoc1
                }

                availableWines.entries.firstOrNull()?.let { (wine, blessedWine) ->
                    blessWine(wine, blessedWine, inv.count(wine))
                    return@onOpLoc1
                }
            }

            openSkillMulti(SkillMultiConfig(
                verb = "bless",
                entries = buildList {
                    addAll(availableBones.map { SkillMultiEntry(it.bone.internalName) })
                    addAll(availableWines.keys.map(::SkillMultiEntry))
                }),
            ) { selection ->

                val selected = selection.entry.item.internalName

                availableBones.firstOrNull { it.bone.internalName == selected }?.let { row ->
                    blessBones(row, selection.amount)
                    return@openSkillMulti
                }

                availableWines[selected]?.let { blessedWine ->
                    blessWine(selected, blessedWine, selection.amount)
                }
            }
        }

        onPlayerQueueWithArgs("queue.prayer_break_down_bone") { queue ->
            rows.firstOrNull { it.boneBlessed == queue.args }?.let { row ->
                breakDownBone(row)
            }
        }
    }

    private fun ProtectedAccess.breakDownBone(row: PrayerBlessedBoneRow) {
        if (!inv.contains("obj.chisel")) {
            player.mes("You need a chisel to break down this bone.")
            return
        }

        player.anim("seq.human_cutting")

        if (invDel(inv, row.boneBlessed.internalName, 1).success && invAdd(inv, "obj.blessed_bone_shard", row.shardCount,).success) {
            statAdvance("stat.crafting", 5.0)
        }

        if (inv.contains(row.boneBlessed.internalName)) {
            weakQueue("queue.prayer_break_down_bone", 4, row.boneBlessed)
        }

    }

    private fun ProtectedAccess.blessBones(row: PrayerBlessedBoneRow, amount: Int = inv.count(row.bone.internalName)) {
        player.anim("seq.human_openchest")
        player.soundSynth(2738)

        val blessedName = row.boneBlessed.internalName

        val targets = inv.objs.count { it.isType(row.bone) }
        val toConvert = minOf(amount, targets)

        repeat(toConvert) {
            invReplace(inv, row.bone.internalName, 1, blessedName)
        }

        mes("You bless $toConvert bone${if (toConvert == 1) "" else "s"} on the altar.")
    }

    private fun ProtectedAccess.blessWine(wine: String, blessedWine: String, amount: Int = inv.count(wine), ) {

        player.anim("seq.human_openchest")
        player.soundSynth(2738)

        val toConvert = minOf(amount, inv.count(wine))

        repeat(toConvert) {
            invReplace(inv, wine, 1, blessedWine)
        }

        mes("You bless $toConvert jug${if (toConvert == 1) "" else "s"} of wine on the altar.")
    }

}
