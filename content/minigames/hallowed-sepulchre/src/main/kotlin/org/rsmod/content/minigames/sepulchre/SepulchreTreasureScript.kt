package org.rsmod.content.minigames.sepulchre

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.enums.HallowedEnums.hallowed_bridge_nails
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The treasure on each floor: the coffins, the Grand Hallowed Coffin at the bottom, and the
 * encounters in front of them (https://oldschool.runescape.wiki/w/Hallowed_Sepulchre#Treasure):
 * the broken bridges, the portal frames and the Saradomin braziers. The grapple pillars, the
 * mended bridges, the conjured portals and the holy barriers are crossed as obstacles.
 */
class SepulchreTreasureScript @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    private lateinit var floors: SepulchreFloors

    override fun ScriptContext.startup() {
        floors = SepulchreFloors.load()
        for (coffin in COFFINS) {
            onOpLoc1(coffin) { searchCoffin(it.loc, grand = false) }
        }
        onOpLoc1(GRAND_COFFIN) { searchCoffin(it.loc, grand = true) }
        onOpLoc1(BRIDGE_BROKEN) { fixBridge() }
        onOpLoc1(BRIDGE_POOL_BROKEN) { fixBridge() }
        onOpLoc1(PORTAL_FRAME) { conjurePortal() }
        onOpLoc1(BRAZIER) { sacrifice() }
        onOpLoc1(BRAZIER_MIRROR) { sacrifice() }
    }

    private suspend fun ProtectedAccess.searchCoffin(loc: BoundLocInfo, grand: Boolean) {
        val floor = floors.at(coords) ?: return
        val looted = lootedVarbit(loc) ?: return
        arriveDelay()
        if (SepulchreClock.isExpired(player.timeRemaining)) {
            // UNVERIFIED: the wording, after the brazier's.
            mes("The magic of the Hallowed Sepulchre prevents you from searching this coffin.")
            return
        }
        if (!canSearch(grand)) {
            return
        }
        anim(PICKLOCK)
        soundSynth(PICKLOCK_SYNTH, delay = PICKLOCK_SYNTH_DELAY)
        delay(3)
        if (!disarms()) {
            // UNVERIFIED: the failure's wording, poison and timing.
            mes("You set off the trap on the coffin and are poisoned!", ChatType.Spam)
            resetAnim()
            PlayerPoison.tryPoison(player, initialDamage = COFFIN_POISON)
            return
        }
        mes("You successfully disarm the trap on the coffin!", ChatType.Spam)
        anim(PUSH_LID)
        statAdvance(THIEVING, THIEVING_XP)
        delay(1)
        mes("You push the coffin lid aside.", ChatType.Spam)
        anim(COFFIN_OPEN)
        delay(2)
        if (vars[looted] != 0) {
            return
        }
        vars[looted] = 1
        if (grand) {
            player.grandCoffinCount++
        } else {
            player.coffinCount++
        }
        loot(floor, grand)
        soundSynth(COFFIN_SYNTH)
    }

    private fun ProtectedAccess.canSearch(grand: Boolean): Boolean {
        val thieving = stat(THIEVING)
        if (!grand) {
            if (thieving < COFFIN_THIEVING) {
                // UNVERIFIED: the wording.
                mes("You need a Thieving level of $COFFIN_THIEVING to search this coffin.")
                return false
            }
            return true
        }
        val skilled =
            thieving >= GRAND_THIEVING ||
                (thieving >= COFFIN_THIEVING && stat(AGILITY) >= GRAND_AGILITY)
        if (!skilled) {
            // UNVERIFIED: the wording.
            mes(
                "You need a Thieving level of $GRAND_THIEVING, or $COFFIN_THIEVING with an " +
                    "Agility level of $GRAND_AGILITY, to search this coffin."
            )
        }
        return skilled
    }

    /**
     * The wiki's measured odds: 41.8% at 66 Thieving rising to 74.6% at 99, plus 7.8 points with
     * a lockpick and 15.6 with the strange old lockpick.
     */
    private fun ProtectedAccess.disarms(): Boolean {
        val level = stat(THIEVING).coerceAtMost(MAX_LEVEL)
        val base = BASE_ODDS + (level - COFFIN_THIEVING) * ODDS_SPAN / LEVEL_SPAN
        val bonus =
            when {
                invTotal(inv, STRANGE_LOCKPICK_FULL) > 0 -> STRANGE_LOCKPICK_BONUS
                invTotal(inv, LOCKPICK) > 0 -> LOCKPICK_BONUS
                else -> 0
            }
        return random.of(PER_MILLE) < base + bonus
    }

    private fun ProtectedAccess.loot(floor: SepulchreFloor, grand: Boolean) {
        val marks =
            if (grand) random.of(GRAND_MARKS) else random.of(floor.coffinMarks)
        invAddOrDrop(objRepo, MARK, marks)
        val tiers = if (grand) List(GRAND_ROLLS) { LootTier.High } else listOf(tierFor(floor))
        for (tier in tiers) {
            for (drop in SepulchreLoot.roll(player, tier)) {
                invAddOrDrop(objRepo, drop.obj, drop.rollCount(random))
            }
        }
        if (grand) {
            if (random.of(ENDURANCE_CHANCE) == 0) {
                invAddOrDrop(objRepo, RING_OF_ENDURANCE)
            }
            if (random.of(GRAND_CLUE_CHANCE) == 0) {
                invAddOrDrop(objRepo, clue(ELITE_CLUE))
            }
            return
        }
        if (random.of(floor.lockpickChance) == 0) {
            invAddOrDrop(objRepo, STRANGE_LOCKPICK_FULL)
        }
        if (random.of(floor.clueChance) == 0) {
            invAddOrDrop(objRepo, clue(floor.clue.internalName))
        }
        if (vars[floor.pageFound] == 0 && random.of(PAGE_CHANCE) == 0) {
            invAddOrDrop(objRepo, floor.page.internalName)
        }
    }

    private fun ProtectedAccess.tierFor(floor: SepulchreFloor): LootTier =
        lootTier(floor.lootTiers, random.of(floor.lootTiers.sum()))

    private fun ProtectedAccess.clue(scroll: String): String =
        player.clueScrollTransformObj(scroll) ?: scroll

    private fun lootedVarbit(loc: BoundLocInfo): String? {
        val type = ServerCacheManager.getObject(loc.id) ?: return null
        if (type.multiVarBit <= 0) {
            return null
        }
        return RSCM.getReverseMapping(RSCMType.VARBIT, type.multiVarBit)
    }

    private suspend fun ProtectedAccess.fixBridge() {
        arriveDelay()
        if (stat(CONSTRUCTION) < BRIDGE_CONSTRUCTION) {
            // UNVERIFIED: the wording.
            mes("You need a Construction level of $BRIDGE_CONSTRUCTION to repair this bridge.")
            return
        }
        val plank = PLANKS.firstOrNull { invTotal(inv, it) >= BRIDGE_PLANKS }
        val nails =
            hallowed_bridge_nails.backing.keys
                .map { it.internalName }
                .firstOrNull { invTotal(inv, it) >= BRIDGE_NAIL_COUNT }
        val tools = SAWS.any { invTotal(inv, it) > 0 } && HAMMERS.any { invTotal(inv, it) > 0 }
        if (plank == null || nails == null || !tools) {
            // UNVERIFIED: the wording.
            mes("You need a saw, a hammer, two planks and five nails to repair this bridge.")
            return
        }
        anim(SMITHING)
        delay(1)
        if (invDel(inv, plank, BRIDGE_PLANKS).failure) {
            return
        }
        if (invDel(inv, nails, BRIDGE_NAIL_COUNT).failure) {
            invAddOrDrop(objRepo, plank, BRIDGE_PLANKS)
            return
        }
        mes("You repair the broken bridge.", ChatType.Spam)
        vars[CONSTRUCTION_STATE] = ENCOUNTER_DONE
        val xpTenths =
            hallowed_bridge_nails.backing.entries.first { it.key.internalName == nails }.value
        statAdvance(CONSTRUCTION, (xpTenths ?: 0) / XP_TENTHS)
    }

    // TODO(content): the enchant spell's runes, and the portal breaking after two failed casts.
    private suspend fun ProtectedAccess.conjurePortal() {
        arriveDelay()
        if (stat(MAGIC) < PORTAL_MAGIC) {
            // UNVERIFIED: the wording.
            mes("You need a Magic level of $PORTAL_MAGIC to power this portal.")
            return
        }
        anim(CASTING)
        soundSynth(ENCHANT_SYNTH)
        delay(2)
        mes("You power the portal with an enchantment spell.", ChatType.Spam)
        vars[MAGIC_STATE] = ENCOUNTER_DONE
        statAdvance(MAGIC, PORTAL_MAGIC_XP)
    }

    private suspend fun ProtectedAccess.sacrifice() {
        arriveDelay()
        if (SepulchreClock.isExpired(player.timeRemaining)) {
            mes("The magic of the Hallowed Sepulchre prevents you from using this brazier.")
            return
        }
        val needed = if (invTotal(inv, HALLOWED_SYMBOL) > 0) 1 else BRAZIER_DUST
        if (invTotal(inv, VAMPYRE_DUST) < needed) {
            mesbox("You need to bring a suitable offering to use this brazier.")
            return
        }
        if (invDel(inv, VAMPYRE_DUST, needed).failure) {
            return
        }
        anim(BONE_SACRIFICE)
        soundSynth(OFFER_SYNTH)
        mes("You see the flame change as your offerings are accepted.", ChatType.Spam)
        vars[PRAYER_STATE] = ENCOUNTER_DONE
        vars[ASHES_OFFERED] = 1
        statAdvance(PRAYER, BRAZIER_PRAYER_XP)
    }

    private companion object {
        val COFFINS =
            listOf(
                "loc.hallowed_reward_coffin_unlooted",
                "loc.hallowed_f5_grapple_coffin_unlooted",
                "loc.hallowed_f5_portal_coffin_unlooted",
                "loc.hallowed_f5_bridge_coffin_unlooted",
            )
        const val GRAND_COFFIN = "loc.hallowed_final_chest_unlooted"
        const val BRIDGE_BROKEN = "loc.hallowed_treasure_construction_ready"
        const val BRIDGE_POOL_BROKEN = "loc.hallowed_treasure_construction_pool_ready"
        const val PORTAL_FRAME = "loc.hallowed_treasure_magic_ready"
        const val BRAZIER = "loc.hallowed_treasure_prayer_ready"
        const val BRAZIER_MIRROR = "loc.hallowed_treasure_prayer_ready_mirror"

        const val THIEVING = "stat.thieving"
        const val CONSTRUCTION = "stat.construction"
        const val MAGIC = "stat.magic"
        const val PRAYER = "stat.prayer"

        const val PICKLOCK = "seq.human_picklock_cagedoor"
        const val PUSH_LID = "seq.ntk_tomb_push_loop"
        const val COFFIN_OPEN = "seq.hallowed_coffin_open"
        const val SMITHING = "seq.human_smithing"
        const val CASTING = "seq.human_casting"
        const val BONE_SACRIFICE = "seq.human_bone_sacrifice"
        const val OFFER_SYNTH = "synth.poh_offer_bones"
        const val PICKLOCK_SYNTH = "synth.pick_lock"
        const val PICKLOCK_SYNTH_DELAY = 5
        const val COFFIN_SYNTH = "synth.coffin_open"
        const val ENCHANT_SYNTH = "synth.enchant_onyx_ring"

        const val MARK = "obj.hallowed_mark"
        const val LOCKPICK = "obj.lockpick"
        const val STRANGE_LOCKPICK_FULL = "obj.strange_old_lockpick_full"
        const val RING_OF_ENDURANCE = "obj.ring_of_endurance_nocharges"
        const val ELITE_CLUE = "obj.trail_elite_emote_exp1"
        const val VAMPYRE_DUST = "obj.vampire_dust"
        const val HALLOWED_SYMBOL = "obj.hallowed_symbol"

        const val CONSTRUCTION_STATE = "varbit.hallowed_construction_state"
        const val MAGIC_STATE = "varbit.hallowed_magic_state"
        const val PRAYER_STATE = "varbit.hallowed_prayer_state"
        const val ASHES_OFFERED = "varbit.hallowed_prayer_ashes_offered"
        const val ENCOUNTER_DONE = 2

        const val COFFIN_THIEVING = 66
        const val GRAND_THIEVING = 84
        const val GRAND_AGILITY = 92
        const val MAX_LEVEL = 99
        const val THIEVING_XP = 200.0
        const val COFFIN_POISON = 4
        const val PER_MILLE = 1000
        const val BASE_ODDS = 418
        const val ODDS_SPAN = 328
        const val LEVEL_SPAN = 33
        const val LOCKPICK_BONUS = 78
        const val STRANGE_LOCKPICK_BONUS = 156
        const val GRAND_ROLLS = 2
        const val ENDURANCE_CHANCE = 200
        const val GRAND_CLUE_CHANCE = 30
        const val PAGE_CHANCE = 10
        val GRAND_MARKS = 8..10

        const val BRIDGE_CONSTRUCTION = 56
        const val BRIDGE_PLANKS = 2
        const val BRIDGE_NAIL_COUNT = 5
        const val PORTAL_MAGIC = 7

        // UNVERIFIED: the wiki's; the only casts captured paid twice it, as the brazier and the
        // nails did at the same rates where Agility and Thieving paid the wiki's.
        const val PORTAL_MAGIC_XP = 200.0
        const val BRAZIER_DUST = 2
        const val BRAZIER_PRAYER_XP = 200.0
        const val XP_TENTHS = 10.0

        val PLANKS =
            listOf("obj.woodplank", "obj.plank_oak", "obj.plank_teak", "obj.plank_mahogany")
        val SAWS = listOf("obj.poh_saw", "obj.eyeglo_crystal_saw")
        val HAMMERS = listOf("obj.hammer", "obj.hallowed_hammer")
    }
}
