package org.rsmod.content.skills.fishing.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.skilling.SkillingAwardResult
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.stat.strengthLvl
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpContentNpc1
import org.rsmod.api.script.onOpContentNpc3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.api.table.fishing.FishingMethodRow
import org.rsmod.api.table.fishing.FishingSpotDefRow
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.content.skills.fishing.FishRow
import org.rsmod.content.skills.fishing.FishingCatchLogic
import org.rsmod.content.skills.fishing.HeronPet.rollHeron
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.fishing.Gate
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Fishing
@Inject
constructor(
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {

    private val vowels = setOf('a', 'e', 'i', 'o', 'u')
    private val rainbowFishId = "obj.hunting_raw_fish_special"
    private val rawSharkId = "obj.raw_shark"
    private val rawAnglerfishId = "obj.raw_anglerfish"
    private var cachedRows: List<FishRow>? = null
    private var typeById: Map<Int, ItemServerType>? = null
    private var cachedCookingXp: Map<String, Int>? = null
    private var methodsById: Map<Int, FishingMethodRow>? = null
    val ProtectedAccess.sharkLureUseQuantity: Int by intVarBit("varbit.shark_lure_use_quantity")

    private fun rows(): List<FishRow> =
        cachedRows
            ?: FishingSpotRow.all()
                .map { r ->
                    FishRow(
                        fishId = r.fish.id,
                        spot = r.spot,
                        method = r.method,
                        level = r.level,
                        xpTenths = r.xp,
                        low = r.low,
                        high = r.high,
                        strXpTenths = r.strXp,
                        agiXpTenths = r.agiXp,
                        countMax = r.count,
                        strReq = r.strReq,
                        agiReq = r.agiReq,
                        baitOverride = if (r.fish.internalName == rainbowFishId) "obj.hunting_stripy_bird_feather" else null,
                    )
                }
                .also { cachedRows = it }

    private fun types(): Map<Int, ItemServerType> =
        typeById
            ?: FishingSpotRow.all().associate { it.fish.id to it.fish }.also { typeById = it }

    private fun method(id: Int): FishingMethodRow {
        val methods =
            methodsById ?: FishingMethodRow.all().associateBy { it.methodId }.also { methodsById = it }
        return methods[id] ?: error("No fishing method row for id=$id")
    }

    override fun ScriptContext.startup() {
        for (spot in FishingSpotDefRow.all()) {
            spot.op1.takeIf { it >= 0 }?.let { m ->
                onOpContentNpc1(spot.content) { fish(it.npc, spot, method(m)) }
            }
            spot.op3.takeIf { it >= 0 }?.let { m ->
                onOpContentNpc3(spot.content) { fish(it.npc, spot, method(m)) }
            }
        }
        onOpHeldU("obj.hammer", "obj.infernal_eel") { crushInfernalEel() }
        onPlayerQueueWithArgs<FishTask>(CATCH_QUEUE) { attemptCatch(it.args) }
    }

    private fun ProtectedAccess.crushInfernalEel() {
        if (player.fishingLvl < 80) {
            mes("You need a Fishing level of 80 to crush infernal eels.")
            return
        }
        invDel(inv, "obj.infernal_eel", 1)
        when (random.of(1, 35)) {
            in 1..29 -> invAdd(inv, "obj.tzhaar_token", random.of(14, 20))
            in 30..31 -> invAdd(inv, "obj.xbows_bolt_tips_onyx", 1)
            else -> invAdd(inv, "obj.lava_shard", random.of(1, 5))
        }
        spam("You smash the infernal eel apart.")
    }

    /**
     * Op entry point. Validates the attempt, plays the animation, and hands the catch cycle over to
     * a weak queue so that anything which interrupts the player also stops the fishing.
     */
    private fun ProtectedAccess.fish(npc: Npc, spot: FishingSpotDefRow, method: FishingMethodRow) {
        if (method.tool == HARPOON && !hasMethodTool(method) && carrying(DRAGON_HARPOON)) {
            mes("You need a Fishing level of $DRAGON_HARPOON_LEVEL to use the dragon harpoon.")
        }

        val attempt = prepare(spot, method, verbose = true) ?: return

        startAnim(attempt)

        clearWeakQueue(CATCH_QUEUE)
        weakQueue(CATCH_QUEUE, rollDelay(attempt.bait), FishTask(npc, npc.uid, spot, method))
    }

    private fun ProtectedAccess.attemptCatch(task: FishTask) {
        // The spot can despawn or hop to another tile while the cycle is running.
        if (task.npc.uid != task.uid) {
            return
        }
        val attempt = prepare(task.spot, task.method, verbose = false) ?: return
        refreshAnim(attempt)
        rollAndAward(attempt)
        weakQueue(CATCH_QUEUE, rollDelay(attempt.bait), task)
    }

    private fun ProtectedAccess.startAnim(attempt: FishAttempt) {
        skillAnimDelay = mapClock + ANIM_REFRESH
        anim(attempt.active.anim)
    }

    private fun ProtectedAccess.refreshAnim(attempt: FishAttempt) {
        if (skillAnimDelay <= mapClock) {
            startAnim(attempt)
        }
    }

    /**
     * Resolves the method actually in use along with the bait and the rows the player can catch.
     * Returns `null` when the attempt cannot go ahead; [verbose] controls whether the reason is
     * reported, since the queue re-checks every cycle and should stay silent.
     */
    private fun ProtectedAccess.prepare(
        spot: FishingSpotDefRow,
        method: FishingMethodRow,
        verbose: Boolean,
    ): FishAttempt? {
        val rows = rows()
        val bareHandFallback = method.fallback.takeIf { it >= 0 }
        val active =
            if (!hasMethodTool(method) && bareHandFallback != null) {
                method(bareHandFallback)
            } else {
                method
            }

        val unlocked = FishingCatchLogic.unlocked(spot.spotId, active.methodId, player.fishingLvl, rows)
        val bait = resolveBait(active)
        val hasBait = bait == null || invTotal(inv, bait) > 0

        when (FishingCatchLogic.attemptGate(active.bait, hasMethodTool(active), hasBait, unlocked)) {
            Gate.NoTool -> {
                if (verbose) mes(active.msg)
                return null
            }
            Gate.NoBait -> {
                if (verbose) mes("You don't have any bait left.")
                return null
            }
            Gate.LevelTooLow -> {
                if (verbose) {
                    val min = FishingCatchLogic.minLevel(spot.spotId, active.methodId, rows)
                    mes("You need a Fishing level of $min to fish here.")
                }
                return null
            }
            Gate.Ok -> {}
        }

        val eligible =
            unlocked.filter {
                player.strengthLvl >= it.strReq &&
                    player.agilityLvl >= it.agiReq &&
                    (it.baitOverride == null || invTotal(inv, it.baitOverride) > 0)
            }
        if (eligible.isEmpty()) {
            if (verbose) {
                val top = unlocked.first()
                if (top.strReq > 0) {
                    mes("You need a Strength and Agility level of ${top.strReq} to catch these fish.")
                } else {
                    mes("You don't have the correct bait to catch these fish.")
                }
            }
            return null
        }

        if (inv.isFull()) {
            if (verbose) {
                mes("Your inventory is too full to hold any more fish.")
                soundSynth("synth.pillory_wrong")
            }
            return null
        }

        return FishAttempt(active, bait, eligible)
    }

    private fun ProtectedAccess.rollAndAward(attempt: FishAttempt) {
        val active = attempt.active
        val activeBait = attempt.bait
        val hasFlakes = invTotal(inv, SPIRIT_FLAKES) > 0
        val harpoonBonus = if (active.tool == HARPOON) harpoonCatchBonus() else 0
        val infernalHarpoon = active.tool == HARPOON && usableInfernalHarpoon()
        val lures = sharkLureQuantity()
        val diabolicWorms = activeBait == DIABOLIC_WORMS

        val caught =
            FishingCatchLogic.rollCatch(attempt.eligible) { row ->
                var l = row.low
                var h = row.high
                if (harpoonBonus > 0) {
                    l = l * (100 + harpoonBonus) / 100
                    h = h * (100 + harpoonBonus) / 100
                }
                if (lures > 0 && row.fishId == rawSharkId.asRSCM()) {
                    val rate = sharkLureRate(lures)
                    l = (l * rate).toInt()
                    h = (h * rate).toInt()
                }
                statRandom("stat.fishing", l, h, invisibleLvls)
            } ?: return

        val type = types()[caught.fishId]!!
        var xp = caught.xpTenths / 10.0 * xpMods.get(player, "stat.fishing")
        if (lures > 0 && caught.fishId == rawSharkId.asRSCM()) {
            xp *= sharkLureXpMultiplier(lures)
        }
        if (diabolicWorms && caught.fishId == rawAnglerfishId.asRSCM()) {
            xp *= DIABOLIC_WORMS_XP_MULTIPLIER
        }
        val count = if (caught.countMax > 1) random.of(1, caught.countMax) else 1
        val product =
            SkillingProduct(
                player = player,
                skill = "stat.fishing",
                item = RSCM.getReverseMapping(RSCMType.OBJ, caught.fishId),
                count = count,
                experience = xp,
                grantsExperience = true,
                source = SkillingProductSource.Fishing(type),
            )
        val item = product.item
        rollHeron(item, sharkLureRarity(if (caught.fishId == rawSharkId.asRSCM()) lures else 0))
        if (awardSkillingProduct(product) != SkillingAwardResult.Success) {
            return
        }

        val name = type.name.lowercase()
        val article = if (active.article == "some") "some" else if (name.firstOrNull() in vowels) "an" else "a"
        spam("You catch $article $name.")
        if (infernalHarpoon) {
            cookInfernalCatch(item, product.count)
        }
        if (caught.strXpTenths > 0) {
            statAdvance("stat.strength", caught.strXpTenths / 10.0 * xpMods.get(player, "stat.strength"))
        }
        if (caught.agiXpTenths > 0) {
            statAdvance("stat.agility", caught.agiXpTenths / 10.0 * xpMods.get(player, "stat.agility"))
        }
        val consumedBait = caught.baitOverride ?: activeBait
        if (consumedBait != null) {
            invDel(inv, consumedBait, 1)
        }
        if (hasFlakes) {
            invDel(inv, SPIRIT_FLAKES, 1)
        }
        if (lures > 0 && caught.fishId == rawSharkId.asRSCM()) {
            invDel(inv, SHARK_LURE, lures)
        }
        rollExtraFish(item, product.count, hasFlakes)
    }

    // Diabolic worms give a 50% chance for the catch roll to come a tick sooner.
    private fun ProtectedAccess.rollDelay(bait: String?): Int =
        if (bait == DIABOLIC_WORMS && random.of(2) == 0) CATCH_CYCLE - 1 else CATCH_CYCLE

    private class FishTask(
        val npc: Npc,
        val uid: NpcUid,
        val spot: FishingSpotDefRow,
        val method: FishingMethodRow,
    )

    private class FishAttempt(
        val active: FishingMethodRow,
        val bait: String?,
        val eligible: List<FishRow>,
    )

    // Pearl rods are equipable, so alternate tools count from the worn slot too.
    private fun ProtectedAccess.hasMethodTool(method: FishingMethodRow): Boolean {
        val tool = method.tool ?: return true
        val altTool = method.altTool
        return invTotal(inv, tool) > 0 ||
            (altTool != null && carrying(altTool)) ||
            (tool == HARPOON && hasWieldableHarpoon())
    }

    // Wieldable harpoons count from the worn slot as well as the inventory.
    private fun ProtectedAccess.hasWieldableHarpoon(): Boolean =
        carrying(BARB_TAIL_HARPOON) ||
            usableDragonHarpoon() ||
            usableInfernalHarpoon() ||
            usableCrystalHarpoon(CRYSTAL_HARPOON) ||
            usableCrystalHarpoon(CRYSTAL_HARPOON_INACTIVE)

    /** Percentage catch-rate bonus the best carried harpoon gives over the regular harpoon. */
    private fun ProtectedAccess.harpoonCatchBonus(): Int =
        when {
            usableCrystalHarpoon(CRYSTAL_HARPOON) -> CRYSTAL_HARPOON_BONUS
            usableInfernalHarpoon() || usableDragonHarpoon() -> DRAGON_HARPOON_BONUS
            else -> 0
        }

    private fun ProtectedAccess.usableDragonHarpoon(): Boolean =
        player.fishingLvl >= DRAGON_HARPOON_LEVEL && carrying(DRAGON_HARPOON)

    private fun ProtectedAccess.usableInfernalHarpoon(): Boolean =
        player.fishingLvl >= INFERNAL_HARPOON_LEVEL && carrying(INFERNAL_HARPOON)

    private fun ProtectedAccess.usableCrystalHarpoon(harpoon: String): Boolean =
        player.fishingLvl >= CRYSTAL_HARPOON_LEVEL &&
            carrying(harpoon) &&
            QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)

    private fun ProtectedAccess.carrying(obj: String): Boolean =
        invTotal(inv, obj) > 0 || obj in player.worn

    /**
     * Resolves which item is actually spent as bait. Diabolic worms stand in for sandworms, and
     * barbarian fishing prefers fish offcuts over every other bait; fine fish offcuts are accepted
     * but not prioritised.
     */
    private fun ProtectedAccess.resolveBait(method: FishingMethodRow): String? {
        val bait = method.bait ?: return null
        if (bait == SANDWORMS && invTotal(inv, DIABOLIC_WORMS) > 0) {
            return DIABOLIC_WORMS
        }
        if (method.tool == BARBARIAN_ROD) {
            if (invTotal(inv, FISH_OFFCUTS) > 0) {
                return FISH_OFFCUTS
            }
            if (invTotal(inv, bait) == 0 && invTotal(inv, FINE_FISH_OFFCUTS) > 0) {
                return FINE_FISH_OFFCUTS
            }
        }
        return bait
    }

    /**
     * Spirit flakes and Rada's blessing each give a chance at a second helping of the catch. The
     * chances stack, the extra fish grants no experience and consumes no extra bait, and it needs a
     * free inventory slot to land in.
     */
    private fun ProtectedAccess.rollExtraFish(item: String, count: Int, hasFlakes: Boolean) {
        val blessing = radasBlessingChance()
        val chance = (if (hasFlakes) SPIRIT_FLAKES_CHANCE else 0) + blessing
        if (chance <= 0 || inv.isFull() || random.of(100) >= chance) {
            return
        }
        invAdd(inv, item, count)
        if (!hasFlakes && blessing > 0) {
            spam("Rada's blessing enabled you to catch an extra fish.")
        }
    }

    private fun ProtectedAccess.radasBlessingChance(): Int =
        when {
            RADAS_BLESSING_4 in player.worn -> 8
            RADAS_BLESSING_3 in player.worn -> 6
            RADAS_BLESSING_2 in player.worn -> 4
            RADAS_BLESSING_1 in player.worn -> 2
            else -> 0
        }

    /**
     * How many shark lures are spent per shark. The selected amount is read from the client-side
     * quantity setting; anything the player cannot afford falls back to using none.
     */
    private fun ProtectedAccess.sharkLureQuantity(): Int {
        val selected = SHARK_LURE_QUANTITIES.getOrElse(sharkLureUseQuantity) { 1 }
        return if (invTotal(inv, SHARK_LURE) >= selected) selected else 0
    }

    private fun sharkLureRate(lures: Int): Double =
        when (lures) {
            5 -> 6.0
            3 -> 4.5
            else -> 3.0
        }

    private fun sharkLureXpMultiplier(lures: Int): Double =
        when (lures) {
            5 -> 0.16
            3 -> 0.20
            else -> 0.25
        }

    /** Shark lures make the heron 4x, 5x or 6x rarer depending on how many are spent. */
    private fun sharkLureRarity(lures: Int): Int =
        when (lures) {
            5 -> 6
            3 -> 5
            1 -> 4
            else -> 1
        }

    /**
     * The infernal harpoon cooks and destroys one in three catches, awarding half of the Cooking
     * experience the fish would have given when cooked normally. Fish with no Cooking entry are
     * left alone.
     */
    private fun ProtectedAccess.cookInfernalCatch(item: String, count: Int) {
        val cookingXp = cookingXpByRaw()[item] ?: return
        if (random.of(3) != 0) {
            return
        }
        invDel(inv, item, count)
        statAdvance("stat.cooking", cookingXp / 2.0 * count * xpMods.get(player, "stat.cooking"))
    }

    private fun cookingXpByRaw(): Map<String, Int> =
        cachedCookingXp
            ?: CookingFoodsRow.all()
                .associate { it.input.internalName to it.xp }
                .also { cachedCookingXp = it }

    private companion object {
        private const val CATCH_QUEUE = "queue.fishing_catch"

        private const val CATCH_CYCLE = 5
        private const val ANIM_REFRESH = 4
        private const val HARPOON = "obj.harpoon"
        private const val DRAGON_HARPOON = "obj.dragon_harpoon"
        private const val DRAGON_HARPOON_LEVEL = 61
        private const val BARB_TAIL_HARPOON = "obj.hunting_barbed_harpoon"
        private const val INFERNAL_HARPOON = "obj.infernal_harpoon"
        private const val INFERNAL_HARPOON_LEVEL = 75
        private const val CRYSTAL_HARPOON = "obj.crystal_harpoon"
        private const val CRYSTAL_HARPOON_INACTIVE = "obj.crystal_harpoon_inactive"
        private const val CRYSTAL_HARPOON_LEVEL = 71
        private const val DRAGON_HARPOON_BONUS = 20
        private const val CRYSTAL_HARPOON_BONUS = 35
        private const val SONG_OF_THE_ELVES = "quest_songoftheelves"

        private const val BARBARIAN_ROD = "obj.brut_fishing_rod"
        private const val SANDWORMS = "obj.piscarilius_sandworms"
        private const val SPIRIT_FLAKES = "obj.spirit_flakes"
        private const val SPIRIT_FLAKES_CHANCE = 50
        private const val SHARK_LURE = "obj.shark_lure"
        private const val DIABOLIC_WORMS = "obj.diabolic_worms"
        private const val DIABOLIC_WORMS_XP_MULTIPLIER = 0.66
        private const val FISH_OFFCUTS = "obj.fish_chunks"
        private const val FINE_FISH_OFFCUTS = "obj.sailing_fine_fish_offcuts"
        private const val RADAS_BLESSING_1 = "obj.zeah_blessing_easy"
        private const val RADAS_BLESSING_2 = "obj.zeah_blessing_medium"
        private const val RADAS_BLESSING_3 = "obj.zeah_blessing_hard"
        private const val RADAS_BLESSING_4 = "obj.zeah_blessing_elite"

        private val SHARK_LURE_QUANTITIES = listOf(1, 3, 5)
    }
}
