package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The crucible: metal in, preform out. https://oldschool.runescape.wiki/w/Crucible */
class CrucibleScript
@Inject
constructor(private val random: GameRandom, private val worldRepo: WorldRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (loc in listOf(CRUCIBLE_EMPTY, CRUCIBLE_PARTIAL)) {
            onOpLoc1(loc) { fill() }
            onOpLoc2(loc) { check() }
            onOpLoc3(loc) { empty() }
        }
        onOpLoc1(CRUCIBLE_FULL) { pour(it.loc) }
        onOpLoc2(CRUCIBLE_FULL) { check() }
        onOpLoc3(CRUCIBLE_FULL) { empty() }
    }

    private fun ProtectedAccess.counts(): IntArray =
        IntArray(METALS.size) { player.foundryVar(metalVarbit(METALS[it])) }

    private suspend fun ProtectedAccess.fill() {
        if (
            player.makingQuestSword() &&
                player.sleepingGiantsTutorial >= SleepingGiants.TUTORIAL_POURED
        ) {
            startDialogue {
                chatPlayer(neutral, "I should finish helping Kovac before I try making another sword.")
            }
            return
        }
        val counts = counts()
        val space = FoundryFormulas.CRUCIBLE_CAPACITY - counts.sum()
        if (space <= 0) {
            mes("The crucible is full.")
            return
        }
        val available = METALS.indices.filter { metalWorth(it) > 0 }
        if (available.isEmpty()) {
            mes("You don't have any metal the crucible can use.")
            return
        }
        val smithing = stat("stat.smithing")
        val usable = available.filter { smithing >= FoundryData.barLevel(FoundryData.bars[it]) }
        if (usable.isEmpty()) {
            mes("You need a Smithing level of ${FoundryData.barLevel(FoundryData.bars[available.first()])} to use that metal.")
            return
        }
        val metal =
            if (usable.size == 1) usable.single()
            else usable[choiceIndex(usable.map { METALS[it].replaceFirstChar(Char::uppercase) })]
        // UNVERIFIED: the prompts. The live game opens a make-X box, "What metal would you like
        // to add?", with all six bars and 1/5/X/All (Sleeping Giants videos, below); OpenRune's
        // `SkillingMenu` can't set that title or show bars the player lacks yet.
        // TODO: destroying excess metal on overfill (with its warning) instead of only taking
        // what fits.
        val requested = countDialog("How many bars' worth?")
        val added = addMetal(metal, minOf(requested, space))
        if (added.isEmpty()) {
            return
        }
        val worth = added.sumOf { it.worth }
        player.setFoundryVar(metalVarbit(METALS[metal]), counts[metal] + worth)
        val total = counts.sum() + worth
        player.crucibleState = if (total >= FoundryFormulas.CRUCIBLE_CAPACITY) 2 else 1
        if (player.crucibleState == 2 && player.makingQuestSword()) {
            player.advanceTutorial(SleepingGiants.TUTORIAL_CRUCIBLE_FULL)
        }
        for (part in added) {
            mesbox(part.message(METALS[metal].replaceFirstChar(Char::uppercase)))
        }
        if (total < FoundryFormulas.CRUCIBLE_CAPACITY) {
            mes(
                "You need to add ${FoundryFormulas.CRUCIBLE_CAPACITY - total} more metal bars worth " +
                    "of material to pour the crucible into the mould jig."
            )
        }
    }

    /**
     * One kind of metal added: bars, or smithed items with their worth. The texts are from
     * Sleeping Giants videos (Slayermusiq1, youtube.com/watch?v=io6mQxi1ddI 4:10-4:33; sigmazx,
     * youtube.com/watch?v=WnGuNtTJW0M), shown as message boxes, e.g. "You add 8 Bronze bars to
     * the crucible." and "You add 2 Bronze battleaxes to the crucible, worth 4 Bronze bars.".
     */
    private class Added(val name: String, val count: Int, val worth: Int, val isBar: Boolean) {
        fun message(metal: String): String {
            val bars = if (worth == 1) "bar" else "bars"
            return if (isBar) {
                "You add $count $metal $bars to the crucible."
            } else {
                val items = if (count == 1 || name.endsWith("s")) name else "${name}s"
                "You add $count $items to the crucible, worth $worth $metal $bars."
            }
        }
    }

    /** Bars' worth of [metal] in the inventory: bars plus melt-down value of smithed items. */
    private fun ProtectedAccess.metalWorth(metal: Int): Int {
        val bars = inv.count(FoundryData.bars[metal].output.internalName)
        val items =
            inv.filterNotNull().sumOf { obj ->
                val (itemMetal, worth) = FoundryData.smithedItems[obj.id] ?: return@sumOf 0
                if (itemMetal == metal) worth * obj.count else 0
            }
        return bars + items
    }

    /** Takes up to [limit] bars' worth of [metal], bars first, then the biggest items that fit. */
    private fun ProtectedAccess.addMetal(metal: Int, limit: Int): List<Added> {
        val parts = mutableListOf<Added>()
        var added = 0
        val bar = FoundryData.bars[metal].output.internalName
        val bars = minOf(inv.count(bar), limit)
        if (bars > 0) {
            invDel(inv, bar, bars)
            added += bars
            parts += Added(FoundryData.bars[metal].output.name, bars, bars, isBar = true)
        }
        val items =
            inv.filterNotNull()
                .mapNotNull { obj -> FoundryData.smithedItems[obj.id]?.takeIf { it.first == metal }?.let { obj.id to it.second } }
                .distinct()
                .sortedByDescending { it.second }
        for ((id, worth) in items) {
            val name = RSCM.getReverseMapping(RSCMType.OBJ, id)
            var count = 0
            while (added + worth <= limit && inv.count(name) > 0) {
                invDel(inv, name, 1)
                added += worth
                count++
            }
            if (count > 0) {
                val display = ServerCacheManager.getItem(id)?.name ?: name
                parts += Added(display, count, count * worth, isBar = false)
            }
        }
        return parts
    }

    private suspend fun ProtectedAccess.choiceIndex(names: List<String>): Int {
        val shown = names.take(4)
        return when (shown.size) {
            2 -> choice2(shown[0], 0, shown[1], 1)
            3 -> choice3(shown[0], 0, shown[1], 1, shown[2], 2)
            else -> choice4(shown[0], 0, shown[1], 1, shown[2], 2, shown[3], 3)
        }
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

        // UNVERIFIED: not published. The preform starts cold: the Sleeping Giants walkthrough
        // heats it "to maximum" at the lava pool right after pick-up, and Quest Helper waits for
        // a dunk to 720+ (varbit 13948) before the trip hammer.
        const val START_TEMPERATURE = 0
    }
}
