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

class TreasureTrailsScript
@Inject
constructor(
    private val rewards: TrailRewards,
    private val emotes: ClueEmotes,
    private val random: GameRandom,
    private val config: ServerConfig,
) : PluginScript() {
    override fun ScriptContext.startup() {
        ClueScrollDrops.resolve = ::resolveDrop
        TrailEvents.eventBus = eventBus
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
        for (part in MasterParts.partObjs) {
            onOpHeld1(part) { with(MasterParts) { combine() } }
            onOpHeld2(part) { with(MasterParts) { inspect() } }
        }
        onOpHeld1(ClueLightBox.BOX) { with(ClueLightBox) { open() } }
        for (index in 0 until ClueLightBox.BUTTONS) {
            onIfModalButton(ClueLightBox.button(index)) { with(ClueLightBox) { press(index) } }
        }
        for (difficulty in listOf(TrailTiers.BEGINNER, TrailTiers.MASTER)) {
            onOpHeld1(TrailTiers.deviceObj(difficulty)) { feelDevice(TrailTiers.tier(difficulty)) }
        }
        onEvent<PlayerEmoteEvent> { emotes.onEmote(player, slot) }

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
        return TrailFlow.freshClue(player, tier, random).symbol()
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

    private fun CoordGrid.distanceTo(other: CoordGrid): Int =
        if (level != other.level) Int.MAX_VALUE else chebyshevDistance(other)

    private companion object {
        const val DEVICE_MIN_DAMAGE = 3
        const val DEVICE_MAX_DAMAGE = 8
        const val LAST_CHECK = "varp.trail_hotcold_last_check_coord"
    }
}
