package org.rsmod.content.bosses.moons.neypotzli

import jakarta.inject.Inject
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal suspend fun ProtectedAccess.neypotzliOpen(hooks: Set<NeypotzliHook>): Boolean {
    for (hook in hooks) {
        if (!with(hook) { mayGather() }) return false
    }
    return true
}

class NeypotzliSupplies
@Inject
constructor(private val hooks: Set<@JvmSuppressWildcards NeypotzliHook>) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CRATES) { chooseKit() }
        onOpLoc2(CRATES) { takeKit(FISHING_KIT) }
        onOpLoc3(CRATES) { takeKit(HUNTING_KIT) }
        onOpLoc4(CRATES) { takeKit(HERBLORE_KIT) }
        onOpLoc1("loc.pmoon_grub_sapling") { collectGrub() }
        onOpHeldU("obj.pestle_and_mortar", GRUB) { crushGrub() }
        onOpHeldU("obj.knife", "obj.bream_fish_raw") {
            cutRitualPart("obj.bream_fish_raw", "obj.pmoon_fish_scales", "You cut some scales off the bream.")
        }
        onOpHeldU("obj.knife", "obj.raw_lizard") {
            cutRitualPart("obj.raw_lizard", "obj.pmoon_lizard_tail", "You cut the tail off the lizard.")
        }
    }

    private suspend fun ProtectedAccess.chooseKit() {
        if (!neypotzliOpen(hooks)) return
        val kit =
            choice3(
                "Fishing supplies.",
                FISHING_KIT,
                "Hunting supplies.",
                HUNTING_KIT,
                "Herblore supplies.",
                HERBLORE_KIT,
                title = "Which supplies would you like?",
            )
        give(kit)
    }

    private suspend fun ProtectedAccess.takeKit(kit: List<KitItem>) {
        if (!neypotzliOpen(hooks)) return
        give(kit)
    }

    private suspend fun ProtectedAccess.give(kit: List<KitItem>) {
        val given = kit.filter { it.always || inv.count(it.obj) == 0 }
        if (given.isEmpty()) {
            mes("You already have everything you need from these crates.")
            return
        }
        if (inv.freeSpace() < given.sumOf { it.count }) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        anim("seq.human_pickuptable_walkmerge")
        soundSynth("synth.pick2")
        for (item in given) invAdd(inv, item.obj, item.count)
        val text = "You take ${given.joinToString(" and ") { it.phrase }} from the crates."
        if (given.size == 1) {
            objbox(given[0].obj, text)
        } else {
            doubleobjbox(given[0].obj, given[1].obj, text)
        }
    }

    private suspend fun ProtectedAccess.collectGrub() {
        if (!neypotzliOpen(hooks)) return
        delay(GRUB_DELAY)
        val space = inv.freeSpace()
        if (space == 0) {
            mes("Your inventory is too full to hold any more grubs.")
            return
        }
        soundSynth("synth.varlamore_pm_rustle_bush_01")
        val extra = space > 1 && statRandom("stat.hunter", EXTRA_LOW, EXTRA_HIGH, 0)
        invAdd(inv, GRUB, if (extra) 2 else 1)
        if (extra) mes("Your hunting knowledge allows you to find an extra grub.")
    }

    private fun ProtectedAccess.crushGrub() {
        val extra = inv.count(GRUB) > 1 && statRandom("stat.herblore", EXTRA_LOW, EXTRA_HIGH, 0)
        if (invReplace(inv, GRUB, if (extra) 2 else 1, "obj.moonlight_grub_paste").failure) return
        anim("seq.human_herbing_grind_restart")
        soundSynth("synth.grind", delay = GRIND_SOUND_DELAY)
        if (extra) mes("Your herblore knowledge allows you to crush an extra grub.")
    }

    private fun ProtectedAccess.cutRitualPart(raw: String, part: String, message: String) {
        if (vars["varbit.pmoon_quest"] !in RITUAL_GATHERING) {
            mes("Nothing interesting happens.")
            return
        }
        if (invReplace(inv, raw, 1, part).failure) return
        anim("seq.human_cutting_knife")
        mes(message)
    }

    private class KitItem(
        val obj: String,
        val phrase: String,
        val count: Int = 1,
        val always: Boolean = false,
    )

    private companion object {
        private const val CRATES = "loc.pmoon_supply_crate"
        private const val GRUB = "obj.moonlight_grub"
        private const val GRUB_DELAY = 2
        private const val GRIND_SOUND_DELAY = 25

        // UNVERIFIED: the odds of an extra grub; only the fact that it scales with level is known.
        private const val EXTRA_LOW = 26
        private const val EXTRA_HIGH = 154

        /** Eyatlalli has asked for the ritual items and has not been given all of them yet. */
        private val RITUAL_GATHERING = 23..26

        private val FISHING_KIT =
            listOf(KitItem("obj.knife", "a knife"), KitItem("obj.big_net", "a big net"))
        private val HUNTING_KIT =
            listOf(
                KitItem("obj.rope", "some rope"),
                KitItem("obj.hunting_butterfly_net", "a butterfly net"),
            )
        private val HERBLORE_KIT =
            listOf(
                KitItem("obj.pestle_and_mortar", "a pestle and mortar"),
                KitItem("obj.vial_water", "some vials of water", count = 2, always = true),
            )
    }
}
