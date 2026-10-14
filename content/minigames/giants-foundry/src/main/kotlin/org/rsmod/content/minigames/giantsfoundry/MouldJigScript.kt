package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The mould jig and its setup interface (`giants_foundry_mould`). The interface's clientscripts
 * write the tab and mould selections client-side; the server mirrors them from the button ops.
 * https://oldschool.runescape.wiki/w/Giants%27_Foundry#Mould_setup
 */
class MouldJigScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(JIG_EMPTY) { setup() }
        onOpLoc1(JIG_MOULD) { checkMoulds() }
        onOpLoc1(JIG_FILLED) { pickUp(hot = true) }
        onOpLoc1(JIG_COOLED) { pickUp(hot = false) }
        onIfModalButton(SIDE_MENU) { selectTab(it) }
        onIfModalButton(CONTENT) { selectMould(it) }
        onIfModalButton(SET_BUTTON) { setMoulds() }
        onIfModalButton(RESET_BUTTON) { resetMoulds() }
    }

    private suspend fun ProtectedAccess.setup() {
        if (
            player.makingQuestSword() &&
                player.sleepingGiantsTutorial < SleepingGiants.TUTORIAL_CRUCIBLE_FULL
        ) {
            startDialogue {
                chatPlayer(
                    quiz,
                    "Maybe I should talk with Kovac first before messing around with anything.",
                )
            }
            return
        }
        if (player.commissionWord1 == 0) {
            mesbox("You need a commission before you can set up the mould.")
            return
        }
        ifOpenMainModal(INTERFACE)
        ifSetEvents(SIDE_MENU, 0..SIDE_MENU_CHILDREN, IfEvent.Op1)
        ifSetEvents(CONTENT, 0..CONTENT_CHILDREN, IfEvent.Op1)
    }

    private fun ProtectedAccess.checkMoulds() {
        val names =
            listOf(
                FoundryData.mould(MouldPart.Forte, player.mouldForte),
                FoundryData.mould(MouldPart.Blade, player.mouldBlade),
                FoundryData.mould(MouldPart.Tip, player.mouldTip),
            ).mapNotNull { it?.name }
        mes("The mould jig is set up with: ${names.joinToString()}.")
    }

    /** Tabs are built by clientscript `script5291`, 9 components each, op1 on the first. */
    private fun ProtectedAccess.selectTab(event: IfModalButton) {
        val tab = event.comsub / TAB_CHILDREN
        if (tab in MouldPart.entries.indices) {
            player.mouldTab = tab
        }
    }

    /**
     * Mould rows are 17 components each (`giants_foundry_mould_setup_list_item`); the position in
     * the part's enum is `comsub / 17 + 1`, as in `giants_foundry_mould_setup_mould_select`.
     */
    private fun ProtectedAccess.selectMould(event: IfModalButton) {
        val part = MouldPart.entries.getOrNull(player.mouldTab) ?: return
        val mould = FoundryData.mould(part, event.comsub / ROW_CHILDREN + 1) ?: return
        if (!isUsable(mould)) {
            return
        }
        when (part) {
            MouldPart.Forte -> player.mouldForte = mould.position
            MouldPart.Blade -> player.mouldBlade = mould.position
            MouldPart.Tip -> player.mouldTip = mould.position
        }
    }

    /** Mirrors `giants_foundry_mould_is_usable`: unlocked and within the Smithing level. */
    private fun ProtectedAccess.isUsable(mould: Mould): Boolean {
        val unlock = FoundryData.unlockVarbit(mould)
        if (unlock != null && player.foundryVar(unlock) == 0) {
            return false
        }
        return stat("stat.smithing") >= mould.level
    }

    private fun ProtectedAccess.setMoulds() {
        if (player.mouldForte == 0 || player.mouldBlade == 0 || player.mouldTip == 0) {
            mes("You need to choose a forte, a blade and a tip.")
            return
        }
        ifCloseSub(INTERFACE)
        player.mouldState = 1
        if (player.makingQuestSword()) {
            player.advanceTutorial(SleepingGiants.TUTORIAL_MOULD_SET)
        }
    }

    private fun ProtectedAccess.resetMoulds() {
        player.mouldForte = 0
        player.mouldBlade = 0
        player.mouldTip = 0
    }

    /**
     * Poured metal stays molten in the jig (the jig's varbit has no cooling state) until it's
     * picked up with a bucket of water, ice gloves or smiths gloves (i).
     * https://oldschool.runescape.wiki/w/Transcript:Sleeping_Giants
     */
    private suspend fun ProtectedAccess.pickUp(hot: Boolean) {
        if (hot && !canHandleHotMetal()) {
            mes("The preform is still molten! You need to cool it down.")
            return
        }
        if (!handsFreeForPreform()) {
            return
        }
        player.mouldState = 3
        if (hasIceGloves()) {
            mes("Your Ice gloves cool the preform as you reach for it.", ChatType.Spam)
        } else if (hot) {
            // sigmazx's Sleeping Giants video (youtube.com/watch?v=WnGuNtTJW0M).
            invReplace(inv, BUCKET_WATER, 1, BUCKET_EMPTY)
            mes("You throw the water over the preform.")
        }
        startSword()
        player.mouldAlloyDifficulty = 0
        player.mouldForte = 0
        player.mouldBlade = 0
        player.mouldTip = 0
        player.mouldState = 0
        soundSynth(PICKUP_SYNTH)
        equipPreform()
        if (player.makingQuestSword()) {
            player.advanceTutorial(SleepingGiants.TUTORIAL_PREFORM_TAKEN)
        }
    }

    /** The sword's numbers are set as the preform leaves the jig. */
    private fun ProtectedAccess.startSword() {
        val difficulty = player.mouldAlloyDifficulty
        val words =
            listOfNotNull(
                CommissionWord.forId(player.commissionWord1),
                CommissionWord.forId(player.commissionWord2),
            )
        val moulds =
            listOfNotNull(
                FoundryData.mould(MouldPart.Forte, player.mouldForte),
                FoundryData.mould(MouldPart.Blade, player.mouldBlade),
                FoundryData.mould(MouldPart.Tip, player.mouldTip),
            )
        val mouldScore = moulds.sumOf { mould -> words.sumOf { mould.score(it) } }
        // UNVERIFIED: the quest sword's quality counts its pinned difficulty as the metal
        // score (guess).
        val quality = (difficulty + mouldScore).coerceAtMost(MAX_QUALITY)
        player.preformDifficulty = difficulty
        player.preformQuality = quality
        player.preformStartQuality = quality
        player.preformCompletion = 0
        player.sweetSpots = 0
        val layout =
            if (player.makingQuestSword()) QUEST_LAYOUT
            else rollLayout(FoundryFormulas.sections(difficulty))
        for (section in 1 until SEED_SLOTS) {
            player.setFoundryVar(seedVarbit(section), 0)
        }
        for ((section, tool) in layout.withIndex()) {
            player.setFoundryVar(seedVarbit(section), tool.seed)
        }
        // UNVERIFIED: the start heat when a bucket of water cools the preform instead of ice
        // gloves (guess: the same as with gloves).
        player.preformTemperature = random.of(START_TEMPERATURE.first, START_TEMPERATURE.last)
    }

    /**
     * The trip hammer comes first, no two neighbouring sections use the same tool, and every
     * sword needs all three tools.
     */
    private fun ProtectedAccess.rollLayout(sections: Int): List<FoundryTool> {
        while (true) {
            val layout = mutableListOf(FoundryTool.Hammer)
            repeat(sections - 1) {
                val options = FoundryTool.entries.filter { it != layout.last() }
                layout += options[random.of(0, options.lastIndex)]
            }
            if (layout.containsAll(FoundryTool.entries)) {
                return layout
            }
        }
    }

    private fun ProtectedAccess.canHandleHotMetal(): Boolean =
        inv.contains(BUCKET_WATER) || hasIceGloves()

    private fun ProtectedAccess.hasIceGloves(): Boolean {
        val hands = player.worn[Wearpos.Hands.slot]
        return hands?.isType("obj.ice_gloves") == true ||
            hands?.isType("obj.smithing_uniform_gloves_ice") == true
    }

    private companion object {
        const val INTERFACE = "interface.giants_foundry_mould"
        const val SIDE_MENU = "component.giants_foundry_mould:side_menu"
        const val CONTENT = "component.giants_foundry_mould:content"
        const val SET_BUTTON = "component.giants_foundry_mould:set_button"
        const val RESET_BUTTON = "component.giants_foundry_mould:reset_button"
        const val TAB_CHILDREN = 9
        const val ROW_CHILDREN = 17
        const val SIDE_MENU_CHILDREN = 3 * TAB_CHILDREN
        const val CONTENT_CHILDREN = 11 * ROW_CHILDREN
        const val JIG_EMPTY = "loc.giants_foundry_jig_empty"
        const val JIG_MOULD = "loc.giants_foundry_jig_mould"
        const val JIG_FILLED = "loc.giants_foundry_jig_mould_filled"
        const val JIG_COOLED = "loc.giants_foundry_jig_mould_cooled"
        const val PICKUP_SYNTH = "synth.giants_foundry_preform_pickup"
        const val MAX_QUALITY = 255
        const val SEED_SLOTS = 7
        val START_TEMPERATURE = 980..1000
        val QUEST_LAYOUT =
            listOf(FoundryTool.Hammer, FoundryTool.Grindstone, FoundryTool.PolishingWheel)
        const val BUCKET_WATER = "obj.bucket_water"
        const val BUCKET_EMPTY = "obj.bucket_empty"
    }
}
