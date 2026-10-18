package org.rsmod.content.other.treasuretrails

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.droptable.ClueScrollDrops
import org.rsmod.api.enums.TrailEnums
import org.rsmod.api.player.events.PlayerEmoteEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.table.TrailTierRow
import org.rsmod.content.other.treasuretrails.TrailFlow.checkSteps
import org.rsmod.content.other.treasuretrails.TrailFlow.openBox
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
) : PluginScript() {
    override fun ScriptContext.startup() {
        ClueScrollDrops.resolve = ::resolveDrop

        val beginner = TrailTiers.tier(TrailTiers.BEGINNER)
        val clue = checkNotNull(beginner.clueObj).symbol()
        onOpHeld1(clue) { read(beginner) }
        onOpHeld2(clue) { checkSteps(beginner) }
        onOpHeld1(beginner.boxObj.symbol()) { openBox(beginner) }
        onOpHeld1(beginner.casketObj.symbol()) { with(rewards) { openCasket(beginner) } }
        onOpHeld1(DEVICE) { feelDevice(beginner) }
        onEvent<PlayerEmoteEvent> { summonUri(player, slot, beginner) }

        onIfClose(TrailRewards.REWARD_INTERFACE) { rewards.closeRewards(player) }
        onIfModalButton("component.trail_rewardscreen:items") {
            if (it.op == IfButtonOp.Op10) {
                objExamine(player.invMap.getOrPut(TrailRewards.REWARD_INV), it.comsub)
            }
        }
    }

    private fun resolveDrop(player: Player, scroll: String): String? {
        if (!ClueTakeHook.boxesUnlocked(player)) return null
        val type = ServerCacheManager.getItem(scroll.asRSCM(RSCMType.OBJ)) ?: return null
        val difficulty = TrailTiers.clueDifficulty(type) ?: return null
        return TrailTiers.tier(difficulty).boxObj.symbol()
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

    private fun summonUri(player: Player, slot: Int, tier: TrailTierRow) {
        if (!TrailFlow.holdsClue(player, tier)) return
        val step = TrailFlow.currentStep(player, tier) ?: return
        if (step.kind != ClueKind.Emote || step.emotes != listOf(slot)) return
        val spot = (ClueSteps.target(player, step) as? ClueTarget.Dig)?.row?.coord ?: return
        if (player.coords.distanceTo(spot) > EMOTE_RADIUS) return
        if (!ClueOutfits.wearing(player, step.outfit)) return
        if (summonedUri(player) != null) return

        val coords = player.coords.translateZ(-1)
        val uri = Npc(tier.uriNpc, coords)
        npcRepo.add(uri, URI_DURATION)
        uri.facePlayer(player)
        worldRepo.spotanimMap(SpotanimType("spotanim.smokepuff".asRSCM(RSCMType.SPOTANIM)), coords)
        VarPlayerIntMapSetter.set(player, SUMMONED_NPC, uri.uid.packed)
    }

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
