package org.rsmod.content.minigames.giantsfoundry

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
        val counts = counts()
        val space = FoundryFormulas.CRUCIBLE_CAPACITY - counts.sum()
        if (space <= 0) {
            mes("The crucible is full.")
            return
        }
        val smithing = stat("stat.smithing")
        val available =
            FoundryData.bars.withIndex().filter { (_, bar) -> inv.count(bar.output.internalName) > 0 }
        if (available.isEmpty()) {
            mes("You don't have any metal the crucible can use.")
            return
        }
        val usable = available.filter { (_, bar) -> smithing >= FoundryData.barLevel(bar) }
        if (usable.isEmpty()) {
            val (_, bar) = available.first()
            mes("You need a Smithing level of ${FoundryData.barLevel(bar)} to use that metal.")
            return
        }
        val (metal, bar) =
            if (usable.size == 1) {
                usable.single()
            } else {
                val names = usable.map { it.value.output.name }
                usable[choiceIndex(names)]
            }
        // UNVERIFIED: the metal choice and amount prompts; the wiki only says the X amount is
        // remembered.
        // TODO: smithed items (worth bars - 1), and destroying excess metal on overfill (with
        // its warning) instead of only taking what fits.
        val requested = countDialog("How many bars?")
        val amount = minOf(requested, inv.count(bar.output.internalName), space)
        if (amount <= 0) {
            return
        }
        invDel(inv, bar.output.internalName, amount)
        player.setFoundryVar(metalVarbit(METALS[metal]), counts[metal] + amount)
        val total = counts.sum() + amount
        player.crucibleState = if (total >= FoundryFormulas.CRUCIBLE_CAPACITY) 2 else 1
        mes("You add $amount ${bar.output.name.lowercase()}${if (amount == 1) "" else "s"} to the crucible.")
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
        softTimer(JIG_COOLING_TIMER, JIG_COOLING_TICKS)
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
        player.setFoundryVar(seedVarbit(0), FoundryTool.Hammer.seed)
        for (section in 1 until 7) {
            val seed = if (section < sections) random.of(0, FoundryTool.entries.lastIndex) else 0
            player.setFoundryVar(seedVarbit(section), seed)
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

        // UNVERIFIED: starting heat isn't published; the release notes say to cool the preform
        // before the trip hammer, so it starts at the top of the gauge.
        const val START_TEMPERATURE = 1000

        // UNVERIFIED: ticks until a poured preform has cooled in the jig (a guess).
        const val JIG_COOLING_TICKS = 10
    }
}
