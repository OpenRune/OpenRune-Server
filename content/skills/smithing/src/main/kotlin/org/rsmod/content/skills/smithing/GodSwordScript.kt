package org.rsmod.content.skills.smithing

import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.smithing.util.SmithingUtils.hasHammer
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GodSwordScript : PluginScript() {

    private val bladeShardItems =
        mapOf(
            "obj.godwars_godsword_blade1" to setOf(1),
            "obj.godwars_godsword_blade2" to setOf(2),
            "obj.godwars_godsword_blade3" to setOf(3),
            "obj.godwars_godsword_blade1+2" to setOf(1, 2),
            "obj.godwars_godsword_blade1+3" to setOf(1, 3),
            "obj.godwars_godsword_blade2+3" to setOf(2, 3),
        )

    private val bladePartInternals = bladeShardItems.keys

    private val forgeRecipes =
        listOf(
            setOf(1, 2, 3) to "obj.godwars_godsword_blade1+2+3",
            setOf(1, 2) to "obj.godwars_godsword_blade1+2",
            setOf(1, 3) to "obj.godwars_godsword_blade1+3",
            setOf(2, 3) to "obj.godwars_godsword_blade2+3",
        )

    private val hiltToGodsword =
        mapOf(
            "obj.godwars_godsword_hilt_armadyl" to "obj.ags",
            "obj.godwars_godsword_hilt_ancient" to "obj.ancient_godsword",
            "obj.godwars_godsword_hilt_bandos" to "obj.bgs",
            "obj.godwars_godsword_hilt_zamorak" to "obj.zgs",
            "obj.godwars_godsword_hilt_saradomin" to "obj.sgs",
        )

    private val hiltInternals = hiltToGodsword.keys

    override fun ScriptContext.startup() {
        registerBladePartCombinations()
        registerHiltAssembly()
        registerAnvilForging()
    }

    private fun ScriptContext.registerBladePartCombinations() {
        val parts = bladePartInternals.toList()
        for (i in parts.indices) {
            for (j in i + 1 until parts.size) {
                onOpHeldU(parts[i], parts[j]) { rejectBladeCombine() }
            }
        }
    }

    private fun ScriptContext.registerHiltAssembly() {
        hiltInternals.forEach { hilt ->
            onOpHeldU("obj.godwars_godsword_blade1+2+3", hilt) { assembleGodsword(it) }
        }
    }

    private fun ScriptContext.registerAnvilForging() {
        bladePartInternals.forEach { part ->
            onOpLocCategoryU("category.anvil", part) {
                if (!hasHammer()) {
                    mes("You need a hammer to work metal with an anvil.")
                    return@onOpLocCategoryU
                }
                forgeBlade()
            }
        }
    }

    private fun ProtectedAccess.rejectBladeCombine() {
        mes(
            "These pieces of the godsword can't be joined together like that - " +
                "try forging them on an anvil.",
        )
    }

    private fun ProtectedAccess.assembleGodsword(event: HeldUEvents.Type) {
        val hiltInternal =
            when {
                event.first.internalName in hiltInternals -> event.first.internalName
                event.second.internalName in hiltInternals -> event.second.internalName
                else -> return
            }

        val product = hiltToGodsword[hiltInternal] ?: return

        if (invDel(inv, hiltInternal, 1).success && invDel(inv, "obj.godwars_godsword_blade1+2+3", 1).success) {
            invAdd(inv, product, 1)
        }
    }

    private suspend fun ProtectedAccess.forgeBlade() {
        val counts = bladePartInternals.associateWith { inv.count(it) }

        val (toConsume, result) =
            forgeRecipes.firstNotNullOfOrNull { (required, product) ->
                findConsumption(required, counts)?.let { it to product }
            }
                ?: run {
                    mes("You need another part of the godsword blade to forge them together.")
                    return
                }

        mesbox("You set to work, trying to fix the ancient sword...")

        delay(3)
        anim("seq.human_smithing")
        soundSynth(3771)
        delay(4)

        if (toConsume.all { invDel(inv, it, 1).success }) {
            invAdd(inv, result, 1)
            statAdvance("stat.smithing", 100.0)
            objbox(
                result,
                "Even for an experienced smith it is not an easy task, but eventually it is done.",
            )
        }
    }

    /**
     * Finds a combination of items whose shard sets exactly match [required].
     */
    private fun findConsumption(
        required: Set<Int>,
        counts: Map<String, Int>,
    ): List<String>? {
        if (required.isEmpty()) {
            return emptyList()
        }

        for ((item, shards) in bladeShardItems) {
            if (!required.containsAll(shards)) {
                continue
            }
            if ((counts[item] ?: 0) <= 0) {
                continue
            }

            val nextRequired = required - shards
            val nextCounts = counts + (item to counts.getValue(item) - 1)

            findConsumption(nextRequired, nextCounts)?.let { remainder ->
                return listOf(item) + remainder
            }
        }

        return null
    }

}
