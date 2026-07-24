package org.rsmod.content.skills.mining.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.SequenceServerType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.config.objParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.skilling.SkillingAwardResult
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.api.script.onOpContentU
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.mining.MiningRocksRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.mining.MiningEquipment.miningGloveExtras
import org.rsmod.content.skills.mining.MiningEquipment.wearingChargedGlory
import org.rsmod.content.skills.mining.configs.MiningParams
import org.rsmod.content.skills.mining.configs.MiningRocks
import org.rsmod.content.skills.mining.configs.depleteRange
import org.rsmod.content.skills.mining.configs.hasDepleteRange
import org.rsmod.content.skills.mining.configs.isGemRock
import org.rsmod.content.skills.mining.configs.isInfinite
import org.rsmod.content.skills.mining.configs.miningXp
import org.rsmod.content.skills.mining.drops.MiningGemDropTables
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Mining
@Inject
constructor(
    private val locRepo: LocRepository,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.rock") { attempt(it.loc, it.type) }
        onOpContentLoc2("content.rock") { prospect(it.type) }
        onOpContentLoc3("content.rock") { mine(it.loc, it.type) }
        onOpContentU("content.rock", "content.mining_pickaxe") { mine(it.loc, it.type) }
    }

    private fun rockData(type: ObjectServerType): MiningRocksRow? = MiningRocks.forLoc(type)

    private suspend fun ProtectedAccess.prospect(type: ObjectServerType) {
        val data = rockData(type)
        mes("You examine the rock for ores...")
        delay(PROSPECT_DELAY)
        mes(prospectMessage(data))
    }

    private fun ProtectedAccess.attempt(rock: BoundLocInfo, type: ObjectServerType) {
        val data = rockData(type) ?: return
        if (player.miningLvl < data.level) {
            mes("You need a Mining level of ${data.level} to mine this rock.")
            return
        }

        if (inv.isFull()) {
            mes(inventoryFullMessage(data))
            soundSynth("synth.pillory_wrong")
            return
        }

        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
            skillAnimDelay = mapClock + 3
            opLoc1(rock)
        } else {
            val pickaxe = findPickaxe(player)
            if (pickaxe == null) {
                mesPickaxeMissing()
                return
            }
            anim(miningAnim(pickaxe, data))
            spam("You swing your pickaxe at the rock.")
            mine(rock, type)
        }
    }

    private fun ProtectedAccess.mine(rock: BoundLocInfo, type: ObjectServerType) {
        val data = rockData(type) ?: return
        val pickaxe = findPickaxe(player)
        if (pickaxe == null) {
            mesPickaxeMissing()
            return
        }

        if (player.miningLvl < data.level) {
            mes("You need a Mining level of ${data.level} to mine this rock.")
            return
        }

        if (inv.isFull()) {
            mes(inventoryFullMessage(data))
            soundSynth("synth.pillory_wrong")
            resetAnim()
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(miningAnim(pickaxe, data))
        }

        val delay = pickaxeActionDelay(pickaxe)
        var minedOre = false

        if (actionDelay < mapClock) {
            actionDelay = mapClock + delay
        } else if (actionDelay == mapClock) {
            actionDelay = mapClock + delay
            when (val gemPre = rollGemPreTable(data)) {
                GemPreRoll.SkipOre -> {
                    opLoc3(rock)
                    return
                }
                is GemPreRoll.Gem -> {
                    awardGemFind(rock, data, gemPre.item)
                    opLoc3(rock)
                    return
                }
                GemPreRoll.KeepOre -> {
                    minedOre = rollOreSuccess(data)
                }
            }
        }

        if (minedOre) {
            val item = resolveMineItem(data) ?: return
            val xp = data.miningXp * xpMods.get(player, "stat.mining")
            val product =
                SkillingProduct(
                    player = player,
                    skill = "stat.mining",
                    item = item,
                    count = 1,
                    experience = xp,
                    grantsExperience = true,
                    source = SkillingProductSource.Mining(rock, data),
                )

            when (awardSkillingProduct(product)) {
                SkillingAwardResult.InventoryFull -> {
                    mes("Your inventory is too full to hold any more ore.")
                    resetAnim()
                    return
                }
                SkillingAwardResult.Cancelled -> Unit
                SkillingAwardResult.Success -> {
                    spam(
                        "You manage to mine some ${getInvObj(InvObj(product.item)).name.lowercase()}.",
                    )
                    soundSynth(ORE_OBTAINED_SOUND)
                }
            }

            if (product.depletes && shouldDeplete(rock, data)) {
                clearDepleteState(rock)
                depleteRock(rock, data)
                return
            }
        }

        opLoc3(rock)
    }

    private fun ProtectedAccess.rollGemPreTable(data: MiningRocksRow): GemPreRoll {
        if (data.isGemRock || data.oreItem == null) {
            return GemPreRoll.KeepOre
        }
        val table =
            if (player.wearingChargedGlory()) {
                MiningGemDropTables.randomGemGlory
            } else {
                MiningGemDropTables.randomGem
            }
        when (val result = table.roll(player, ArgMap()).flatten()) {
            is RollResult.Nothing -> Unit
            is RollResult.Single -> return interpretGemPre(result.result)
            is RollResult.ListOf -> {
                for (drop in result.results) {
                    val outcome = interpretGemPre(drop)
                    if (outcome != GemPreRoll.KeepOre) {
                        return outcome
                    }
                }
            }
        }
        return GemPreRoll.KeepOre
    }

    private fun interpretGemPre(drop: DropRollItem): GemPreRoll {
        if (drop.obj == MiningGemDropTables.KEEP_ORE) {
            return GemPreRoll.KeepOre
        }
        if (drop.isNothing || drop.obj.isEmpty()) {
            return GemPreRoll.SkipOre
        }
        return GemPreRoll.Gem(drop.obj)
    }

    private fun ProtectedAccess.awardGemFind(
        rock: BoundLocInfo,
        data: MiningRocksRow,
        gem: String,
    ) {
        val product =
            SkillingProduct(
                player = player,
                skill = "stat.mining",
                item = gem,
                count = 1,
                experience = 0.0,
                grantsExperience = false,
                source = SkillingProductSource.Mining(rock, data),
                depletes = false,
            )
        when (awardSkillingProduct(product)) {
            SkillingAwardResult.InventoryFull -> {
                mes("Your inventory is too full to hold any more gems.")
                resetAnim()
            }
            SkillingAwardResult.Success -> {
                spam("You manage to mine some ${getInvObj(InvObj(gem)).name.lowercase()}.")
                soundSynth(ORE_OBTAINED_SOUND)
            }
            SkillingAwardResult.Cancelled -> Unit
        }
    }

    private fun ProtectedAccess.rollOreSuccess(data: MiningRocksRow): Boolean {
        val gloryBoost = data.isGemRock && player.wearingChargedGlory()
        val low = if (gloryBoost) data.successRateLow * 3 else data.successRateLow
        val high = if (gloryBoost) data.successRateHigh * 3 else data.successRateHigh
        return statRandom("stat.mining", low, high, invisibleLvls)
    }

    private fun ProtectedAccess.depleteRock(rock: BoundLocInfo, data: MiningRocksRow) {
        val empty = data.emptyRockObject ?: return
        locRepo.change(rock, empty, data.respawnCycles)
        resetAnim()
    }

    private fun ProtectedAccess.shouldDeplete(rock: BoundLocInfo, data: MiningRocksRow): Boolean {
        if (data.isInfinite) {
            return false
        }
        val gloveExtras = player.miningGloveExtras(data)
        if (data.hasDepleteRange) {
            val range = data.depleteRange
            val threshold =
                player.attr
                    .getOrPut(DEPLETION_THRESHOLD_ATTR) { mutableMapOf() }
                    .getOrPut(rock.coords) {
                        random.of(range.first, range.last) + gloveExtras
                    }
            val counts = player.attr.getOrPut(MINED_ORE_COUNT_ATTR) { mutableMapOf() }
            val newCount = (counts[rock.coords] ?: 0) + 1
            counts[rock.coords] = newCount
            return newCount >= threshold
        }
        return handleGloveDeplete(rock, gloveExtras)
    }

    private fun ProtectedAccess.handleGloveDeplete(
        rock: BoundLocInfo,
        extras: Int,
    ): Boolean {
        if (extras <= 0) {
            return true
        }
        val counts = player.attr.getOrPut(DEPLETE_GLOVE_COUNT_ATTR) { mutableMapOf() }
        val current = counts[rock.coords] ?: 0
        return if (current != extras) {
            counts[rock.coords] = current + 1
            false
        } else {
            true
        }
    }

    private fun ProtectedAccess.clearDepleteState(rock: BoundLocInfo) {
        player.attr[MINED_ORE_COUNT_ATTR]?.remove(rock.coords)
        player.attr[DEPLETION_THRESHOLD_ATTR]?.remove(rock.coords)
        player.attr[DEPLETE_GLOVE_COUNT_ATTR]?.remove(rock.coords)
    }

    private fun ProtectedAccess.resolveMineItem(data: MiningRocksRow): String? {
        if (data.isGemRock) {
            return rollGemRock()
        }
        val oreType = data.oreItem ?: return null
        return RSCM.getReverseMapping(RSCMType.OBJ, oreType.id)
    }

    private fun ProtectedAccess.rollGemRock(): String {
        when (val result = MiningGemDropTables.gemRock.roll(player, ArgMap()).flatten()) {
            is RollResult.Single -> {
                if (!result.result.isNothing && result.result.obj.isNotEmpty()) {
                    return result.result.obj
                }
            }
            is RollResult.ListOf -> {
                val gem = result.results.firstOrNull { !it.isNothing && it.obj.isNotEmpty() }
                if (gem != null) {
                    return gem.obj
                }
            }
            is RollResult.Nothing -> Unit
        }
        return "obj.uncut_sapphire"
    }

    private fun inventoryFullMessage(data: MiningRocksRow): String {
        val ore = data.oreItem
        if (ore != null) {
            return "Your inventory is too full to hold any more ${ore.name.lowercase()}."
        }
        return "Your inventory is too full to hold any more ore."
    }

    private fun prospectMessage(data: MiningRocksRow?): String {
        if (data == null) {
            return "There is no ore currently available in this rock."
        }
        if (data.isGemRock) {
            return "This rock contains gems."
        }
        val ore = data.oreItem ?: return "There is no ore currently available in this rock."
        return "This rock contains ${ore.name.lowercase()}."
    }

    private fun ProtectedAccess.miningAnim(pickaxe: InvObj, data: MiningRocksRow): String {
        val pickaxeType = getInvObj(pickaxe)
        val seq =
            if (data.miningWall && pickaxeType.hasParam(MiningParams.skill_wall_anim)) {
                pickaxeType.pickaxeWallAnim
            } else {
                pickaxeType.pickaxeAnim
            }
        return RSCM.getReverseMapping(RSCMType.SEQ, seq.id)
    }

    private fun ProtectedAccess.pickaxeActionDelay(pickaxe: InvObj): Int =
        Companion.pickaxeActionDelay(pickaxe, random)

    private fun ProtectedAccess.mesPickaxeMissing() {
        mes("You need a pickaxe to mine this rock.")
        when {
            player.hasBlockedInfernalPickaxe() ->
                mes("You need a Smithing level of 85 to use the infernal pickaxe.")
            player.hasBlockedCrystalPickaxe() ->
                mes("You need to complete Song of the Elves to use this pickaxe.")
            else -> mes("You do not have a pickaxe which you have the Mining level to use.")
        }
    }

    private sealed interface GemPreRoll {
        data object KeepOre : GemPreRoll

        data object SkipOre : GemPreRoll

        data class Gem(val item: String) : GemPreRoll
    }

    companion object {
        private const val ORE_OBTAINED_SOUND = 3600
        private const val PROSPECT_DELAY = 4
        private const val INFERNAL_SMITHING_REQ = 85
        private const val SONG_OF_THE_ELVES = "quest_songoftheelves"

        private val MINED_ORE_COUNT_ATTR = AttributeKey<MutableMap<CoordGrid, Int>>()
        private val DEPLETION_THRESHOLD_ATTR = AttributeKey<MutableMap<CoordGrid, Int>>()
        private val DEPLETE_GLOVE_COUNT_ATTR = AttributeKey<MutableMap<CoordGrid, Int>>()

        val ItemServerType.pickaxeLevelReq: Int by objParam(params.levelrequire)
        val ItemServerType.pickaxeAnim: SequenceServerType by objParam(params.skill_anim)
        val ItemServerType.pickaxeDelay: Int by objParam(MiningParams.skill_action_delay)
        val ItemServerType.pickaxeWallAnim: SequenceServerType by
            objParam(MiningParams.skill_wall_anim)

        fun pickaxeActionDelay(pickaxe: InvObj, random: org.rsmod.api.random.GameRandom): Int {
            val type = getInvObj(pickaxe)
            val base = type.pickaxeDelay
            return when (type.internalName) {
                "obj.dragon_pickaxe",
                "obj.dragon_pickaxe_pretty",
                "obj.zalcano_pickaxe",
                "obj.infernal_pickaxe",
                "obj.infernal_pickaxe_empty",
                "obj.3a_pickaxe",
                "obj.crystal_pickaxe_inactive", -> if (random.of(6) == 0) 2 else 3
                "obj.crystal_pickaxe" -> if (random.of(4) == 0) 2 else 3
                else -> base
            }
        }

        fun findPickaxe(player: Player): InvObj? {
            val worn = player.wornPickaxe()
            val carried = player.carriedPickaxes()
            val candidates =
                buildList {
                    if (worn != null) {
                        add(worn)
                    }
                    addAll(carried)
                }
            return candidates.maxWithOrNull(pickaxeComparator)
        }

        /** Higher is better: lower action delay first, then higher mining level req. */
        private val pickaxeComparator: Comparator<InvObj> =
            Comparator { left, right ->
                val leftType = getInvObj(left)
                val rightType = getInvObj(right)
                val byDelay = rightType.pickaxeDelay.compareTo(leftType.pickaxeDelay)
                if (byDelay != 0) {
                    return@Comparator byDelay
                }
                leftType.pickaxeLevelReq.compareTo(rightType.pickaxeLevelReq)
            }

        private fun Player.wornPickaxe(): InvObj? {
            val righthand = righthand ?: return null
            return righthand.takeIf { getInvObj(it).isUsablePickaxe(this) }
        }

        private fun Player.carriedPickaxes(): List<InvObj> =
            inv.filterNotNull { getInvObj(it).isUsablePickaxe(this) }

        private fun ItemServerType.isUsablePickaxe(player: Player): Boolean {
            if (!isContentType("content.mining_pickaxe") || player.miningLvl < pickaxeLevelReq) {
                return false
            }
            return when (internalName) {
                "obj.infernal_pickaxe" -> player.smithingLvl >= INFERNAL_SMITHING_REQ
                "obj.crystal_pickaxe",
                "obj.crystal_pickaxe_inactive", ->
                    QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)
                else -> true
            }
        }

        private fun Player.hasBlockedInfernalPickaxe(): Boolean =
            ownsPickaxe("obj.infernal_pickaxe") && smithingLvl < INFERNAL_SMITHING_REQ

        private fun Player.hasBlockedCrystalPickaxe(): Boolean =
            (ownsPickaxe("obj.crystal_pickaxe") || ownsPickaxe("obj.crystal_pickaxe_inactive")) &&
                !QuestRequirements.hasCompleted(this, SONG_OF_THE_ELVES)

        private fun Player.ownsPickaxe(name: String): Boolean =
            (righthand != null && getInvObj(righthand!!).internalName == name) ||
                inv.any { it != null && getInvObj(it).internalName == name }
    }
}
