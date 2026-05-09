package org.rsmod.content.skills.prayer

import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.content.skills.prayer.items.ZealotRobes.shouldConsume
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PrayerBuryEvents : PluginScript() {

    private val bones = Companion.bones

    override fun ScriptContext.startup() {
        bones.forEach { bone ->
            onOpHeld1(bone.item) {
                bury(bone, it.slot)
            }
        }
        onPlayerQueueWithArgs("queue.prayer_bury") { processBuryTick(it.args) }
    }

    private suspend fun ProtectedAccess.bury(row: SkillPrayerRow, slot: Int, ) {
        if (!row.ashes && inArea("area.chaos_temple", player.coords)) {
            val confirmed = choice2(
                "Bury the Bone",
                true, "Cancel", false,
                title = "Are you sure you want to do that?",
            )

            if (!confirmed) {
                return
            }
        }

        stopAction()
        val animation = if (row.ashes) "seq.farming_ingredient_sprinkle" else "seq.human_openchest"
        anim(animation)
        player.soundSynth(2738)
        weakQueue("queue.prayer_bury", 2, BuryTask(row, slot))
    }

    private fun ProtectedAccess.processBuryTick(task: BuryTask) {
        stopAction()

        if (!task.row.ashes) {
            mes("You dig a hole in the ground...")
        }

        if (inv[task.slot]?.isType(task.row.item.internalName) != true) {
            return
        }
        val applyZealotSave = !isDemonicAsh(task.row.item.internalName)
        val shouldConsume = if (applyZealotSave) player.shouldConsume() else true
        if (shouldConsume && invDel(inv = inv, type = task.row.item.internalName, count = 1, slot = task.slot).failure) {
            return
        }

        val message = if (task.row.ashes) "You scatter the ashes." else "You bury the bones."
        mes(message)
        statAdvance("stat.prayer", task.row.exp.toDouble())
        publish(SkillingActionCompleteEvent(player = player, context =
            SkillingActionContext.Prayer(PrayerSkillAction.BuryComplete(
                itemInternal = task.row.item.internalName,
                ashes = task.row.ashes,
                experienceGranted = task.row.exp.toDouble(),
                catacombsBonePrayerRestore = task.row.prayerRestore
            ))),
        )
    }

    private data class BuryTask(
        val row: SkillPrayerRow,
        val slot: Int,
    )

    companion object {
        val bones = SkillPrayerRow.all()

        private val demonicAshes = setOf(
            "obj.infernal_ashes",
            "obj.abyssal_ashes",
            "obj.malicious_ashes",
            "obj.vile_ashes",
            "obj.fiendish_ashes",
        )

        fun isDemonicAsh(itemInternal: String): Boolean = itemInternal in demonicAshes
    }
}
