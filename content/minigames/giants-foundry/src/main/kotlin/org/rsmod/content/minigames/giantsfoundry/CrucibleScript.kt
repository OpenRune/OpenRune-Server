package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.locAnimSpecific
import org.rsmod.api.player.protect.ProtectedAccess
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

class CrucibleScript
@Inject
constructor(private val alignment: TextAlignment) : PluginScript() {
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
            chatPlayer(quiz, "I should finish helping Kovac before I try making another sword.")
        }
        return true
    }

    private suspend fun ProtectedAccess.talkToKovacFirst(): Boolean {
        if (
            !player.makingQuestSword() ||
                player.sleepingGiantsTutorial >= SleepingGiants.TUTORIAL_CRATE_SEARCHED
        ) {
            return false
        }
        startDialogue {
            chatPlayer(
                quiz,
                "Maybe I should talk with Kovac first before messing around with anything.",
            )
        }
        return true
    }

    private suspend fun ProtectedAccess.freeSpace(): Int? {
        val space = FoundryFormulas.CRUCIBLE_CAPACITY - counts().sum()
        if (space <= 0) {
            mesbox("The crucible is full. You can't add any more metal to it.")
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
        arriveDelay()
        if (questSwordPoured()) {
            return
        }
        freeSpace() ?: return
        var chosen: SkillMultiSelection? = null
        openSkillMulti(fillMenu()) { chosen = it }
        val selection = chosen ?: return
        val metal = FoundryData.bars.indexOfFirst { it.output.internalName == selection.entry.internal }
        val bar = FoundryData.bars[metal].output
        player.skillmultiPreviousSelection = metal
        if (inv.count(bar.internalName) == 0) {
            objbox(bar.internalName, "You have no ${bar.name}s to add to the crucible.")
            return
        }
        if (!canUse(metal)) {
            return
        }
        addBars(metal, selection.amount)
    }

    private suspend fun ProtectedAccess.addBars(metal: Int, amount: Int) {
        val bar = FoundryData.bars[metal].output
        melt()
        val space = freeSpace() ?: return
        // UNVERIFIED: bars past the crucible's free space stay in the inventory (guess).
        val count = minOf(amount, inv.count(bar.internalName), space)
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
            eventsEndAtMaxCount = true,
            eventsAfterSetup = true,
            padLabels = true,
        )

    private suspend fun ProtectedAccess.addItems(obj: ItemServerType) {
        arriveDelay()
        if (talkToKovacFirst()) {
            return
        }
        val bar = FoundryData.bars.indexOfFirst { it.output.id == obj.id }
        val smithed = FoundryData.smithedItems[obj.id]
        if (bar < 0 && smithed == null) {
            mesbox("You cannot add this item to the crucible.")
            return
        }
        if (questSwordPoured()) {
            return
        }
        val space = freeSpace() ?: return
        if (smithed == null) {
            if (canUse(bar)) {
                addBars(bar, 1)
            }
            return
        }
        val (metal, worth) = smithed
        if (!canUse(metal)) {
            return
        }
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

    private fun ProtectedAccess.addToCrucible(metal: Int, worth: Int): Int {
        val counts = counts()
        player.setFoundryVar(metalVarbit(METALS[metal]), counts[metal] + worth)
        val total = counts.sum() + worth
        player.crucibleState = if (total >= FoundryFormulas.CRUCIBLE_CAPACITY) 2 else 1
        if (player.makingQuestSword()) {
            player.advanceTutorial(
                if (player.crucibleState == 2) SleepingGiants.TUTORIAL_CRUCIBLE_FULL
                else SleepingGiants.TUTORIAL_METAL_ADDED
            )
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

    private suspend fun ProtectedAccess.check() {
        val counts = counts()
        val names = METALS.map { it.replaceFirstChar(Char::uppercaseChar) }
        val text =
            "The crucible currently contains ${counts.sum()} / ${FoundryFormulas.CRUCIBLE_CAPACITY} " +
                "metals:<br>${counts[0]} x ${names[0]}, ${counts[1]} x ${names[1]}, " +
                "${counts[2]} x ${names[2]}, <br>${counts[3]} x ${names[3]}, " +
                "${counts[4]} x ${names[4]}, ${counts[5]} x ${names[5]}."
        mes(text)
        mesbox(text)
    }

    private suspend fun ProtectedAccess.empty() {
        if (player.makingQuestSword()) {
            startDialogue {
                chatPlayer(neutral, "I don't think Kovac would want me to empty this right now.")
            }
            return
        }
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

    private suspend fun ProtectedAccess.pour(crucible: BoundLocInfo) {
        arriveDelay()
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
        val difficulty =
            if (player.makingQuestSword()) QUEST_DIFFICULTY
            else FoundryFormulas.metalScore(counts())

        if (player.makingQuestSword()) {
            val counts = counts()
            player.setFoundryVar(metalVarbit("bronze"), maxOf(0, counts[0] - QUEST_POUR_BRONZE))
            player.setFoundryVar(metalVarbit("iron"), maxOf(0, counts[1] - QUEST_POUR_IRON))
            player.advanceTutorial(SleepingGiants.TUTORIAL_POURED)
        }
        anim(POUR_PLAYER_SEQ)
        soundSynth(POUR_SYNTH)
        player.locAnimSpecific(crucible, POUR_SEQ.asRSCM(RSCMType.SEQ))
        delay(1)
        player.mouldAlloyDifficulty = difficulty
        delay(POUR_DELAY - 1)
        player.mouldState = 2
        for (metal in METALS) {
            player.setFoundryVar(metalVarbit(metal), 0)
        }
        player.crucibleState = 0
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
        const val POUR_PLAYER_SEQ = "seq.human_push"
        const val POUR_SYNTH = "synth.giants_foundry_crucible_pour"
        const val POUR_DELAY = 6
        const val QUEST_DIFFICULTY = 5
        const val QUEST_POUR_BRONZE = 10
        const val QUEST_POUR_IRON = 8
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
    }
}
