package org.rsmod.content.other.treasuretrails

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.droptable.ClueScrollDrops
import org.rsmod.api.enums.TrailEnums
import org.rsmod.api.player.events.PlayerEmoteEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
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
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TreasureTrailsScript
@Inject
constructor(
    private val rewards: TrailRewards,
    private val npcRepo: NpcRepository,
    private val npcList: NpcList,
    private val worldRepo: WorldRepository,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        ClueScrollDrops.resolve = ::resolveDrop

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
        onOpHeld1(DEVICE) { feelDevice(TrailTiers.tier(TrailTiers.BEGINNER)) }
        onEvent<PlayerEmoteEvent> { onEmote(player, slot) }

        onIfClose(TrailRewards.REWARD_INTERFACE) { rewards.closeRewards(player) }
        onIfModalButton("component.trail_rewardscreen:items") {
            if (it.op == IfButtonOp.Op10) {
                objExamine(player.invMap.getOrPut(TrailRewards.REWARD_INV), it.comsub)
            }
        }
    }

    /** After X Marks the Spot clues drop as scroll boxes; before it, as a random step obj. */
    private fun resolveDrop(player: Player, scroll: String): String? {
        val type = ServerCacheManager.getItem(scroll.asRSCM(RSCMType.OBJ)) ?: return null
        val difficulty = TrailTiers.clueDifficulty(type) ?: return null
        val tier = TrailTiers.tier(difficulty)
        if (ClueTakeHook.boxesUnlocked(player)) return tier.boxObj.symbol()
        if (difficulty !in TrailTiers.PLAYABLE) return null
        return TrailFlow.freshClue(tier, random).symbol()
    }

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
    }

    /**
     * Emote steps list one or two emotes. The first of a pair is remembered in a temp varp until
     * the second follows at the same spot; any other emote there starts the pair over.
     */
    private fun onEmote(player: Player, slot: Int) {
        for (trail in TrailFlow.heldTrails(player)) {
            val step = trail.step
            if (step.kind != ClueKind.Emote || !atEmoteSpot(player, step)) continue
            if (!ClueOutfits.wearing(player, step.outfit)) continue
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
            if (complete) summonUri(player, trail.tier)
        }
    }

    private fun atEmoteSpot(player: Player, step: ClueStep): Boolean {
        val spot = (ClueSteps.target(player, step) as? ClueTarget.Dig)?.row?.coord ?: return false
        val spots = listOf(spot) + emoteAreaCoords(step)
        return spots.any { player.coords.distanceTo(it) <= EMOTE_RADIUS }
    }

    private fun summonUri(player: Player, tier: TrailTierRow) {
        if (summonedUri(player) != null) return
        VarPlayerIntMapSetter.set(player, TrailFlow.EMOTE_FIRST, 0)
        val coords = player.coords.translateZ(-1)
        val uri = Npc(tier.uriNpc, coords)
        npcRepo.add(uri, URI_DURATION)
        uri.facePlayer(player)
        worldRepo.spotanimMap(SpotanimType("spotanim.smokepuff".asRSCM(RSCMType.SPOTANIM)), coords)
        VarPlayerIntMapSetter.set(player, SUMMONED_NPC, uri.uid.packed)
    }

    private fun emoteAreaCoords(step: ClueStep): List<CoordGrid> =
        TrailEmoteAreaRow.all().filter { it.step.rowId == step.rowId }.flatMap { it.coords }

    private fun summonedUri(player: Player): Npc? {
        val packed = player.vars[SUMMONED_NPC]
        if (packed == 0 || packed == NO_NPC) return null
        return NpcUid(packed).resolve(npcList)
    }

    private fun CoordGrid.distanceTo(other: CoordGrid): Int =
        if (level != other.level) Int.MAX_VALUE else chebyshevDistance(other)

    private companion object {
        const val DEVICE = "obj.beginner_device"
        const val LAST_CHECK = "varp.trail_hotcold_last_check_coord"
        const val SUMMONED_NPC = "varp.cluehelper_npc_id"
        const val NO_NPC = -1
        const val EMOTE_RADIUS = 4
        const val URI_DURATION = 100
    }
}
