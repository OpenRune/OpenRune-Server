package org.rsmod.content.other.treasuretrails

import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.droptable.ClueScrollDrops
import org.rsmod.api.droptable.ClueTier
import org.rsmod.api.enums.TrailEnums
import org.rsmod.api.player.events.PlayerEmoteEvent
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.table.TrailEmoteAreaRow
import org.rsmod.api.table.TrailTierRow
import org.rsmod.content.other.treasuretrails.ClueStash.build
import org.rsmod.content.other.treasuretrails.ClueStash.search
import org.rsmod.content.other.treasuretrails.TrailFlow.checkSteps
import org.rsmod.content.other.treasuretrails.TrailFlow.openBox
import org.rsmod.content.other.treasuretrails.TrailFlow.openScrollCase
import org.rsmod.content.other.treasuretrails.TrailFlow.read
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Not linked / not implemented (audit 2026-10-20), none of it checked in client.
 *
 * Clue objs left out of the roll pool, all because no `trail_mapNN` interface is identified and the
 * row is missing from `TrailMapInterfaceTable`:
 * - Easy maps: `trail_easy_map_exp1`, `trail_clue_easy_map001`..`trail_clue_easy_map005`.
 * - Medium maps: `trail_clue_medium_map001`..`trail_clue_medium_map007`.
 * - Hard maps: `trail_clue_hard_map001`..`trail_clue_hard_map007`.
 * - Elite maps: `trail_elite_map_exp1`..`trail_elite_map_exp6`.
 *
 * Music clue objs, filtered in `TrailFlow.rollableSteps` because the music system isn't developed
 * enough:
 * - Easy: `trail_clue_easy_music001`..`trail_clue_easy_music006`.
 * - Medium: `trail_clue_medium_music001`..`trail_clue_medium_music006`.
 * - Hard: `trail_clue_hard_music001`..`trail_clue_hard_music009`, `trail_hard_music_vm01`.
 * - Elite: `trail_clue_elite_music001`, `trail_clue_elite_music002`.
 *
 * Other clue objs:
 * - `trail_elite_riddle_puzzlebox`: no elite step row references a puzzle box, so nothing hands it
 *   out and it needs no handling.
 * - `trail_easy_emote_sail`, `trail_elite_emote_sail`, `trail_elite_riddle_sail`,
 *   `trail_elite_sextant_sail`, `trail_hard_riddle_sail`, `trail_hard_sextant_sail`,
 *   `trail_medium_anagram_sail`, `trail_medium_sextant_sail`: sailing steps, excluded on purpose.
 * - `sote_clue1`..`sote_clue4`: Song of the Elves clue objs, no handling found.
 * - `trail_hard_fairy_vm01` and `trail_clue_hard_fairy001`..`trail_clue_hard_fairy010`: no fairy
 *   ring code-entry check, the step completes by digging at the coordinate.
 *
 * Elite skill challenges with no code (`dbrow.cluehelper_skillchallenge_<name>`):
 * - `elite_1` enchant dragonstone jewellery: no enchant jewellery spells.
 * - `elite_3` aerial fishing: no aerial fishing.
 * - `elite_4` skullball goal: no skullball.
 * - `elite_5` Ape Atoll agility lap: no agility module.
 * - `elite_7` steal from the Ardougne Castle chest: thieving only has stalls and pickpocketing.
 * - `elite_17` fix a magical lamp in Dorgesh-Kaan: no Dorgesh-Kaan content.
 * - `elite_21` plant a watermelon seed: no farming module.
 *
 * Clue drop sources and rewards with no code referencing them:
 * - `wc_clue_nest_beginner`, `wc_clue_nest_easy`, `wc_clue_nest_medium`, `wc_clue_nest_hard`,
 *   `wc_clue_nest_elite`: woodcutting does not drop them.
 * - `fishing_clue_bottle_beginner`, `fishing_clue_bottle_easy`, `fishing_clue_bottle_medium`,
 *   `fishing_clue_bottle_hard`, `fishing_clue_bottle_elite`: fishing does not drop them.
 * - `bh_clue_box`: no Bounty Hunter content.
 * - `clue_hunter_garb`, `clue_hunter_gloves`, `clue_hunter_trousers`, `clue_hunter_boots`,
 *   `clue_hunter_cloak`: not given anywhere.
 * - `clueless_scroll`: not given anywhere.
 * - Bloodhound pet: exists in `content/other/pets`, `TrailRewards` never rolls it.
 * - Kill steps (`ClueMonsterKillHook`) have no location or mirror-shield check.
 *
 * Other:
 * - STASH units take inventory items only, and `inv.hh_inv` isn't populated so the S.t.a.s.h chart
 *   shows nothing filled.
 * - A dropped clue keeps the old trail's length and progress when a fresh one is picked up.
 */
class TreasureTrailsScript
@Inject
constructor(
    private val rewards: TrailRewards,
    private val encounters: ClueEncounters,
    private val random: GameRandom,
    private val config: ServerConfig,
) : PluginScript() {
    override fun ScriptContext.startup() {
        ClueScrollDrops.resolve = ::resolveDrop
        val capacity = config.gameplay.clueScrolls.baseCapacity
        require(capacity >= 1) { "gameplay.clue-scrolls.base-capacity must be at least 1, was $capacity" }
        TrailTiers.baseCapacity = capacity

        onEvent<SkillingActionCompleteEvent> { ClueChallengeTasks.onSkilling(player, context) }

        for (tier in TrailTiers.playableTiers()) {
            val clues = tier.clueObj?.let(::listOf) ?: TrailTiers.clueObjs(tier.difficulty)
            for (clue in clues) {
                onOpHeld1(clue.symbol()) { read(tier) }
                onOpHeld2(clue.symbol()) { checkSteps(tier) }
            }
            for (scroll in TrailTiers.challengeObjs(tier.difficulty)) {
                onOpHeld1(scroll.symbol()) { readChallenge(scroll) }
            }
            onOpHeld1(tier.boxObj.symbol()) { openBox(tier) }
            onOpHeld1(tier.casketObj.symbol()) { with(rewards) { openCasket(tier) } }
        }
        for (tier in TrailTierRow.all()) {
            onOpHeld1(tier.minorCaseObj.symbol()) {
                openScrollCase(tier, tier.minorCaseObj, tier.minorCaseVarbit)
            }
            onOpHeld1(tier.majorCaseObj.symbol()) {
                openScrollCase(tier, tier.majorCaseObj, tier.majorCaseVarbit)
            }
        }
        for (unit in ClueStash.allUnits()) {
            onOpLoc1(unit) { build(unit) }
            onOpLoc2(unit) { search(unit) }
        }
        for (box in ClueSlidePuzzle.boxes) {
            onOpHeld1(box.symbol()) { with(ClueSlidePuzzle) { open(box) } }
        }
        onIfModalButton(ClueSlidePuzzle.PIECES) { with(ClueSlidePuzzle) { slide(it.comsub) } }
        onOpHeld1(ClueLightBox.BOX) { with(ClueLightBox) { open() } }
        for (index in 0 until ClueLightBox.BUTTONS) {
            onIfModalButton(ClueLightBox.button(index)) { with(ClueLightBox) { press(index) } }
        }
        for (difficulty in listOf(TrailTiers.BEGINNER, TrailTiers.MASTER)) {
            onOpHeld1(TrailTiers.deviceObj(difficulty)) { feelDevice(TrailTiers.tier(difficulty)) }
        }
        onEvent<PlayerEmoteEvent> { onEmote(player, slot) }

        onIfClose(TrailRewards.REWARD_INTERFACE) { rewards.closeRewards(player) }
        onIfModalButton("component.trail_rewardscreen:items") {
            if (it.op == IfButtonOp.Op10) {
                objExamine(player.invMap.getOrPut(TrailRewards.REWARD_INV), it.comsub)
            }
        }
    }

    private fun resolveDrop(player: Player, clue: ClueTier): String? {
        val tier = TrailTiers.tier(clue.difficulty)
        val boxes = ClueTakeHook.boxesUnlocked(player)
        if (!TrailTiers.canReceive(player, tier, boxes)) {
            val missed = if (boxes) "scroll box" else "clue scroll"
            player.mes(
                "You have a sneaking suspicion that you would have received " +
                    "${article(clue)} ${clue.name.lowercase()} $missed."
            )
            return null
        }
        if (boxes) return tier.boxObj.symbol()
        return TrailFlow.freshClue(tier, random).symbol()
    }

    private fun article(clue: ClueTier): String = if (clue.name.first() in "AEIOU") "an" else "a"

    private fun ProtectedAccess.readChallenge(scroll: ItemServerType) {
        val step = TrailTiers.objStep(scroll)?.let(ClueSteps::step) ?: return
        val question = ClueSteps.question(step) ?: return
        ifOpenMainModal("interface.trail_cluetext")
        ifSetText("component.trail_cluetext:text", question.t0)
    }

    private fun ProtectedAccess.feelDevice(tier: TrailTierRow) {
        val step = TrailFlow.currentStep(player, tier)
        val spot =
            step
                ?.takeIf { it.kind == ClueKind.HotCold && TrailFlow.holdsClue(player, tier) }
                ?.let { (ClueSteps.target(player, it) as? ClueTarget.Dig)?.row?.coord }
        if (spot == null) {
            mes("Nothing interesting happens.")
            return
        }
        val distance = player.coords.distanceTo(spot)
        val temperature =
            TrailEnums.trail_hotcold_temperatures
                .mapNotNull { (min, text) -> text?.let { min to it } }
                .sortedByDescending { it.first }
                .first { distance >= it.first }
                .second
        val last = player.vars[LAST_CHECK]
        val previous = if (last == 0) null else CoordGrid(last).distanceTo(spot)
        VarPlayerIntMapSetter.set(player, LAST_CHECK, player.coords.packed)
        val comparison =
            when {
                previous == null || distance <= ClueDigHook.HOT_COLD_DIG_RADIUS -> ""
                distance < previous -> ", and warmer than last time"
                distance > previous -> ", and colder than last time"
                else -> ", and the same temperature as last time"
            }
        mes("The device is $temperature$comparison.")
        if (tier.difficulty == TrailTiers.MASTER) {
            takeInstantHit(HitType.Typeless, random.of(DEVICE_MIN_DAMAGE, DEVICE_MAX_DAMAGE))
        }
    }

    /**
     * Emote steps list one or two emotes. The first of a pair is remembered in a temp varp until
     * the second follows at the same spot; any other emote there starts the pair over.
     */
    private fun onEmote(player: Player, slot: Int) {
        for (trail in TrailFlow.heldTrails(player)) {
            val step = trail.step
            if (step.kind != ClueKind.Emote || !atEmoteSpot(player, step)) continue
            if (!ClueOutfits.wearing(player, step)) continue
            val emotes = step.emotes
            val pending = player.vars[TrailFlow.EMOTE_FIRST] == step.rowId
            val complete =
                when {
                    emotes.size == 1 -> emotes.single() == slot
                    pending && emotes.getOrNull(1) == slot -> true
                    else -> {
                        val first = if (emotes.firstOrNull() == slot) step.rowId else 0
                        VarPlayerIntMapSetter.set(player, TrailFlow.EMOTE_FIRST, first)
                        false
                    }
                }
            if (!complete) continue
            val defeated = player.vars[TrailFlow.ENCOUNTER_DEFEATED] == step.rowId
            if (step.encounters.isEmpty() || defeated) {
                encounters.summonUri(player, trail.tier)
            } else {
                encounters.spawn(player, step)
            }
        }
    }

    private fun atEmoteSpot(player: Player, step: ClueStep): Boolean {
        val spot = (ClueSteps.target(player, step) as? ClueTarget.Dig)?.row?.coord ?: return false
        val spots = listOf(spot) + emoteAreaCoords(step)
        return spots.any { player.coords.distanceTo(it) <= EMOTE_RADIUS }
    }

    private fun emoteAreaCoords(step: ClueStep): List<CoordGrid> =
        TrailEmoteAreaRow.all().filter { it.step.rowId == step.rowId }.flatMap { it.coords }

    private fun CoordGrid.distanceTo(other: CoordGrid): Int =
        if (level != other.level) Int.MAX_VALUE else chebyshevDistance(other)

    private companion object {
        const val DEVICE_MIN_DAMAGE = 3
        const val DEVICE_MAX_DAMAGE = 8
        const val LAST_CHECK = "varp.trail_hotcold_last_check_coord"
        const val EMOTE_RADIUS = 4
    }
}
