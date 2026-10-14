package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillMultiSelection
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The crucible: metal in, preform out. https://oldschool.runescape.wiki/w/Crucible */
class CrucibleScript
@Inject
constructor(
    private val random: GameRandom,
    private val worldRepo: WorldRepository,
    private val alignment: TextAlignment,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (loc in listOf(CRUCIBLE_EMPTY, CRUCIBLE_PARTIAL)) {
            onOpLoc1(loc) { fill() }
            onOpLoc2(loc) { check() }
            onOpLoc3(loc) { empty() }
        }
        onOpLoc1(CRUCIBLE_FULL) { pour(it.loc) }
        onOpLoc2(CRUCIBLE_FULL) { check() }
        onOpLoc3(CRUCIBLE_FULL) { empty() }
        for (loc in listOf(CRUCIBLE_EMPTY, CRUCIBLE_PARTIAL, CRUCIBLE_FULL)) {
            onOpLocU(loc) { addItems(it.objType) }
        }
    }

    private fun ProtectedAccess.counts(): IntArray =
        IntArray(METALS.size) { player.foundryVar(metalVarbit(METALS[it])) }

    private suspend fun ProtectedAccess.questSwordPoured(): Boolean {
        if (
            !player.makingQuestSword() ||
                player.sleepingGiantsTutorial < SleepingGiants.TUTORIAL_POURED
        ) {
            return false
        }
        startDialogue {
            chatPlayer(neutral, "I should finish helping Kovac before I try making another sword.")
        }
        return true
    }

    private fun ProtectedAccess.freeSpace(): Int? {
        val space = FoundryFormulas.CRUCIBLE_CAPACITY - counts().sum()
        if (space <= 0) {
            mes("The crucible is full.")
            return null
        }
        return space
    }

    private fun ProtectedAccess.canUse(metal: Int): Boolean {
        val level = FoundryData.barLevel(FoundryData.bars[metal])
        if (stat("stat.smithing") < level) {
            mes("You need a Smithing level of $level to use that metal.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.fill() {
        if (questSwordPoured()) {
            return
        }
        freeSpace() ?: return
        var chosen: SkillMultiSelection? = null
        openSkillMulti(fillMenu()) { chosen = it }
        val selection = chosen ?: return
        val metal = FoundryData.bars.indexOfFirst { it.output.internalName == selection.entry.internal }
        val bar = FoundryData.bars[metal].output
        if (inv.count(bar.internalName) == 0) {
            objbox(bar.internalName, "You have no ${bar.name}s to add to the crucible.")
            return
        }
        if (!canUse(metal)) {
            return
        }
        melt()
        val space = freeSpace() ?: return
        // UNVERIFIED: bars past the crucible's free space stay in the inventory (guess).
        val count = minOf(selection.amount, inv.count(bar.internalName), space)
        if (count <= 0) {
            return
        }
        invDel(inv, bar.internalName, count)
        val total = addToCrucible(metal, count)
        objbox(bar.internalName, "You add $count ${barName(bar.name, count)} to the crucible.")
        afterAdding(total)
    }

    private fun fillMenu(): SkillMultiConfig =
        SkillMultiConfig(
            actionType = SkillingActionType.SMELT,
            verb = "add",
            entries = FoundryData.bars.map { SkillMultiEntry(it.output.internalName) },
            titleOverride = "What metal would you like to add?",
            showUnavailable = true,
        )

    /** Smithed items used on the crucible melt down for their bars' worth. */
    private suspend fun ProtectedAccess.addItems(obj: ItemServerType) {
        // UNVERIFIED: any other item, bars included, does nothing (guess).
        val (metal, worth) =
            FoundryData.smithedItems[obj.id]
                ?: run {
                    mes(Constants.dm_default)
                    return
                }
        if (questSwordPoured() || !canUse(metal)) {
            return
        }
        val space = freeSpace() ?: return
        val held = inv.count(obj.internalName)
        val chosen = if (held <= 1) held else chooseCount(held)
        if (chosen <= 0) {
            return
        }
        val metalName = metalName(metal)
        val excess = chosen * worth - space
        if (
            excess > 0 &&
                !confirmOverflow(
                    obj.internalName,
                    "Adding this item will overflow the crucible. You will lose $excess bars " +
                        "worth of $metalName. Are you sure you want to add it?",
                )
        ) {
            return
        }
        melt()
        val count = minOf(chosen, inv.count(obj.internalName))
        if (count <= 0) {
            return
        }
        invDel(inv, obj.internalName, count)
        val added = minOf(count * worth, FoundryFormulas.CRUCIBLE_CAPACITY - counts().sum())
        val total = addToCrucible(metal, added)
        val items = if (count == 1) obj.name else "${obj.name}s"
        doubleobjbox(
            obj.internalName,
            FoundryData.bars[metal].output.internalName,
            "You add $count $items to the crucible, worth $added ${barName(metalName, added)}.",
        )
        afterAdding(total)
    }

    private suspend fun ProtectedAccess.chooseCount(held: Int): Int {
        val choice =
            if (held >= 5) {
                choice4("1", 1, "5", 5, "X", -1, "All", held, title = COUNT_TITLE)
            } else {
                choice3("1", 1, "X", -1, "All", held, title = COUNT_TITLE)
            }
        return if (choice == -1) countDialog().coerceAtMost(held) else choice
    }

    /** An objectbox with "No." and "Yes." buttons instead of a continue line. */
    private suspend fun ProtectedAccess.confirmOverflow(obj: String, text: String): Boolean {
        val page = alignment.generateChatPageList(text).first().text
        mes(text, ChatType.Mesbox)
        vars[CHATBOX_UNCLAMP_VARBIT] = 0
        ClientScripts.topLevelChatboxResetBackground(player)
        ifOpenSub(OBJECTBOX, CHATBOX_MODAL, IfSubType.Modal)
        ClientScripts.objboxSetButtons(player, "No.|Yes.")
        ifSetEvents(OBJECTBOX_UNIVERSE, 0..1, IfEvent.PauseButton)
        ifSetObj(OBJECTBOX_ITEM, obj, 400)
        ifSetText(OBJECTBOX_TEXT, page)
        return pauseButton().subcomponent == 1
    }

    private suspend fun ProtectedAccess.melt() {
        anim(MELT_SEQ)
        soundSynth(MELT_SYNTH)
        delay(MELT_DELAY)
    }

    /** Adds [worth] bars of [metal]; returns the crucible's new total. */
    private fun ProtectedAccess.addToCrucible(metal: Int, worth: Int): Int {
        val counts = counts()
        player.setFoundryVar(metalVarbit(METALS[metal]), counts[metal] + worth)
        val total = counts.sum() + worth
        player.crucibleState = if (total >= FoundryFormulas.CRUCIBLE_CAPACITY) 2 else 1
        if (player.crucibleState == 2 && player.makingQuestSword()) {
            player.advanceTutorial(SleepingGiants.TUTORIAL_CRUCIBLE_FULL)
        }
        return total
    }

    private suspend fun ProtectedAccess.afterAdding(total: Int) {
        if (total >= FoundryFormulas.CRUCIBLE_CAPACITY) {
            mesbox(
                "The crucible is now full of molten metal, it is ready to be poured into the " +
                    "mould jig."
            )
        } else {
            mes(
                "You need to add ${FoundryFormulas.CRUCIBLE_CAPACITY - total} more metal bars " +
                    "worth of material to pour the crucible into the mould jig.",
                ChatType.Spam,
            )
        }
    }

    private fun metalName(metal: Int): String = FoundryData.bars[metal].output.name.removeSuffix(" bar")

    private fun barName(metal: String, count: Int): String =
        when {
            metal.endsWith(" bar") -> if (count == 1) metal else "${metal}s"
            count == 1 -> "$metal bar"
            else -> "$metal bars"
        }

    private fun ProtectedAccess.check() {
        val counts = counts()
        if (counts.sum() == 0) {
            mes("The crucible is empty.")
            return
        }
        val parts = METALS.indices.filter { counts[it] > 0 }.map { "${counts[it]} ${METALS[it]}" }
        mes("The crucible contains ${parts.joinToString()} (${counts.sum()}/${FoundryFormulas.CRUCIBLE_CAPACITY}).")
    }

    private suspend fun ProtectedAccess.empty() {
        if (counts().sum() == 0) {
            mes("The crucible is already empty.")
            return
        }
        if (!choice2("Empty the crucible. The metal will be lost.", true, "Keep it.", false)) {
            return
        }
        for (metal in METALS) {
            player.setFoundryVar(metalVarbit(metal), 0)
        }
        player.crucibleState = 0
        mes("You empty the crucible.")
    }

    /** Pours a full crucible into the set moulds and starts the sword's numbers. */
    private suspend fun ProtectedAccess.pour(crucible: BoundLocInfo) {
        if (player.commissionWord1 == 0) {
            mes("You need a commission from Kovac before you can pour a sword.")
            return
        }
        if (player.holdsPreform() || player.preformStored || player.mouldState >= 2) {
            mes("You already have a sword in progress.")
            return
        }
        if (player.mouldState != 1) {
            mes("You need to set up the moulds on the mould jig first.")
            return
        }
        val moulds = selectedMoulds() ?: return
        val level = moulds.maxOf { it.level }
        if (stat("stat.smithing") < level) {
            mes("You need a Smithing level of $level to use those moulds.")
            return
        }
        val counts = counts()
        val metalScore = FoundryFormulas.metalScore(counts)
        val words = listOfNotNull(CommissionWord.forId(player.commissionWord1), CommissionWord.forId(player.commissionWord2))
        val mouldScore = moulds.sumOf { mould -> words.sumOf { mould.score(it) } }

        worldRepo.locAnim(crucible, POUR_SEQ)
        delay(POUR_DELAY)

        for (metal in METALS) {
            player.setFoundryVar(metalVarbit(metal), 0)
        }
        player.crucibleState = 0
        player.mouldAlloyDifficulty = metalScore
        player.preformDifficulty = metalScore
        val quality = (metalScore + mouldScore).coerceAtMost(MAX_QUALITY)
        player.preformStartQuality = quality
        player.preformQuality = quality
        player.preformCompletion = 0
        player.preformTemperature = START_TEMPERATURE
        rollSeeds(metalScore)
        player.sweetSpots = 0
        player.mouldState = 2
        if (player.makingQuestSword()) {
            player.advanceTutorial(SleepingGiants.TUTORIAL_POURED)
        }
        mes("You pour the molten metal into the mould jig.")
    }

    /**
     * Which station each progress section needs. The first is always the trip hammer, and the
     * grindstone and polishing wheel are never first.
     * https://oldschool.runescape.wiki/w/Giants%27_Foundry#Refining_the_sword
     */
    // UNVERIFIED: sections after the first are uniformly random.
    private fun ProtectedAccess.rollSeeds(difficulty: Int) {
        val sections = FoundryFormulas.sections(difficulty)
        if (player.makingQuestSword()) {
            questSeeds(sections)
            return
        }
        player.setFoundryVar(seedVarbit(0), FoundryTool.Hammer.seed)
        for (section in 1 until 7) {
            val seed = if (section < sections) random.of(0, FoundryTool.entries.lastIndex) else 0
            player.setFoundryVar(seedVarbit(section), seed)
        }
    }

    /**
     * The quest sword goes trip hammer, grindstone, polishing wheel, as Kovac describes it
     * (https://oldschool.runescape.wiki/w/Transcript:Sleeping_Giants); Quest Helper waits for
     * 333 and 666 progress, so three sections.
     */
    // UNVERIFIED: if the player's metal gives more than three sections, the middle ones use the
    // grindstone.
    private fun ProtectedAccess.questSeeds(sections: Int) {
        for (section in 0 until 7) {
            val tool =
                when {
                    section >= sections -> null
                    section == 0 -> FoundryTool.Hammer
                    section == sections - 1 -> FoundryTool.PolishingWheel
                    else -> FoundryTool.Grindstone
                }
            player.setFoundryVar(seedVarbit(section), tool?.seed ?: 0)
        }
    }

    private fun ProtectedAccess.selectedMoulds(): List<Mould>? {
        val moulds =
            listOf(
                FoundryData.mould(MouldPart.Forte, player.mouldForte),
                FoundryData.mould(MouldPart.Blade, player.mouldBlade),
                FoundryData.mould(MouldPart.Tip, player.mouldTip),
            )
        if (moulds.any { it == null }) {
            mes("You need to set up the moulds on the mould jig first.")
            return null
        }
        return moulds.filterNotNull()
    }

    private companion object {
        const val CRUCIBLE_EMPTY = "loc.giants_foundry_crucible_empty"
        const val CRUCIBLE_PARTIAL = "loc.giants_foundry_crucible_partial"
        const val CRUCIBLE_FULL = "loc.giants_foundry_crucible_full"
        const val POUR_SEQ = "seq.giants_foundry_crucible_pour01"
        const val POUR_DELAY = 3
        const val MAX_QUALITY = 255
        const val COUNT_TITLE = "How many would you like to add?"
        const val MELT_SEQ = "seq.human_elem2_lever_3way_right"
        const val MELT_SYNTH = "synth.furnace"
        const val MELT_DELAY = 3
        const val CHATBOX_UNCLAMP_VARBIT = "varbit.chatmodal_unclamp"
        const val CHATBOX_MODAL = "component.chatbox:chatmodal"
        const val OBJECTBOX = "interface.objectbox"
        const val OBJECTBOX_UNIVERSE = "component.objectbox:universe"
        const val OBJECTBOX_ITEM = "component.objectbox:item"
        const val OBJECTBOX_TEXT = "component.objectbox:text"

        // UNVERIFIED: not published. The preform starts cold: the Sleeping Giants walkthrough
        // heats it "to maximum" at the lava pool right after pick-up, and Quest Helper waits for
        // a dunk to 720+ (varbit 13948) before the trip hammer.
        const val START_TEMPERATURE = 0
    }
}
