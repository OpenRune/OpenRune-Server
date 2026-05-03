package org.rsmod.api.spells.runes

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.spells.runes.combo.ComboRuneRepository
import org.rsmod.api.spells.runes.compact.CompactRuneRepository
import org.rsmod.api.spells.runes.fake.FakeRuneRepository
import org.rsmod.api.spells.runes.staves.StaffSubstituteRepository
import org.rsmod.api.spells.runes.subs.RuneSubstituteRepository
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType

public object MagicRunes {
    /*
     * Warning: This function is complex, but necessarily so. It replicates subtle behavior from
     * the official game regarding how requirements are validated and consumed - especially combo
     * runes.
     *
     * When a combo rune can fulfill the requirements of **both** of its associated runes (and
     * both are still needed), it is prioritized early and treated as a high-priority substitute.
     * However, if it only satisfies one of the two requirements, the base rune is used instead.
     * If that base rune cannot be validated (e.g., not enough in inventory), the system falls
     * back to using the combo rune - even if it only fulfills a single requirement.
     *
     * This dual-pass structure ensures both priority and fallback behavior are reproduced.
     */
    public fun validateRequirements(
        inv: Inventory,
        worn: Inventory,
        pouch: RunePouch?,
        requirements: RequirementList,
        useFakeRunes: Boolean,
        runeFountain: Boolean,
        compact: CompactRuneRepository,
        unlimited: UnlimitedRuneRepository,
        combos: ComboRuneRepository,
        fakes: FakeRuneRepository,
        runeSubs: RuneSubstituteRepository,
        staffSubs: StaffSubstituteRepository,
    ): List<Validation> {
        val validationList = mutableListOf<Validation>()

        val righthand = worn[Wearpos.RightHand.slot]
        val lefthand = worn[Wearpos.LeftHand.slot]

        // Attempt to validate combo runes that can substitute exactly two required runes.
        for ((combo, rune1, rune2) in combos.comboRunes) {

            val internalNameRune1 = RSCM.getReverseMapping(RSCMType.OBJ, rune1.id)
            val internalNameRune2 = RSCM.getReverseMapping(RSCMType.OBJ, rune2.id)

            val runeReq1 = requirements.findWithRemaining(internalNameRune1) ?: continue
            val runeReq2 = requirements.findWithRemaining(internalNameRune2) ?: continue

            // Combo runes are prioritized early only if both of their associated runes
            // are required. If the player has an unlimited source for either rune, that
            // rune is no longer required, and the combo rune is no longer prioritized.

            val hasUnlimited1 = unlimited.isSource(rune1, righthand, lefthand)
            if (hasUnlimited1) {
                continue
            }

            val hasUnlimited2 = unlimited.isSource(rune2, righthand, lefthand)
            if (hasUnlimited2) {
                continue
            }

            val minRemaining = min(runeReq1.remaining, runeReq2.remaining)
            val validation =
                validateRune(
                    inv = inv,
                    righthand = righthand,
                    lefthand = lefthand,
                    pouch = pouch,
                    rune = combo,
                    required = minRemaining,
                    useFakeRunes = useFakeRunes,
                    runeFountain = runeFountain,
                    compact = compact,
                    unlimited = unlimited,
                    subs = runeSubs,
                    fakes = fakes,
                )
            if (validation.isValid) {
                validationList += validation
                runeReq1.remaining -= minRemaining
                runeReq2.remaining -= minRemaining
            }
        }

        var searchComboRunes = false
        for (req in requirements.reqs) {
            check(req.remaining >= 0) { "Unexpected remain count: $req (list=$requirements)" }
            if (req.remaining == 0) {
                continue
            }

            if (req.wornSlot != null) {
                val validation = validateWorn(worn, req.type, req.wornSlot, staffSubs)
                validationList += validation
                req.remaining = if (validation.isValid) 0 else req.remaining
                continue
            }

            val validation =
                validateRune(
                    inv = inv,
                    righthand = righthand,
                    lefthand = lefthand,
                    pouch = pouch,
                    rune = req.type,
                    required = req.remaining,
                    useFakeRunes = useFakeRunes,
                    runeFountain = runeFountain,
                    compact = compact,
                    unlimited = unlimited,
                    subs = runeSubs,
                    fakes = fakes,
                )

            // If this rune result is invalid, we defer handling it in case a combo rune can still
            // fulfill it.
            if (validation.isInvalid) {
                searchComboRunes = true
                continue
            }

            validationList += validation
            req.remaining = 0
        }

        if (!searchComboRunes) {
            return validationList
        }

        for ((combo, rune1, rune2) in combos.comboRunes) {
            val internalNameRune1 = RSCM.getReverseMapping(RSCMType.OBJ, rune1.id)
            val internalNameRune2 = RSCM.getReverseMapping(RSCMType.OBJ, rune2.id)

            val runeReq1 = requirements.findWithRemaining(internalNameRune1)
            val runeReq2 = requirements.findWithRemaining(internalNameRune2)
            if (runeReq1 == null && runeReq2 == null) {
                continue
            }
            val remaining1 = runeReq1?.remaining ?: 0
            val remaining2 = runeReq2?.remaining ?: 0
            val maxRemaining = max(remaining1, remaining2)
            check(maxRemaining > 0) // `findWithRemaining` should always check that `remaining` > 0.

            val validation =
                validateRune(
                    inv = inv,
                    righthand = righthand,
                    lefthand = lefthand,
                    pouch = pouch,
                    rune = combo,
                    required = maxRemaining,
                    useFakeRunes = useFakeRunes,
                    runeFountain = runeFountain,
                    compact = compact,
                    unlimited = unlimited,
                    subs = runeSubs,
                    fakes = fakes,
                )

            // Only include the combo rune if its validation is successful.
            if (validation.isValid) {
                validationList += validation

                if (runeReq1 != null) {
                    runeReq1.remaining -= maxRemaining
                }

                if (runeReq2 != null) {
                    runeReq2.remaining -= maxRemaining
                }
            }
        }

        // Final pass: validate remaining runes that were skipped earlier to give combo runes a
        // chance to fulfill them.
        val missingRequirements = requirements.reqs.filter { it.remaining > 0 && !it.isWorn }
        if (missingRequirements.isNotEmpty()) {
            val missing = missingRequirements.map { Validation.Invalid.NotEnoughRunes(it.type) }
            validationList += missing
        }
        return validationList
    }

    public fun validateWorn(
        worn: Inventory,
        type: ItemServerType,
        wornSlot: Int,
        staffSubs: StaffSubstituteRepository,
    ): Validation {
        val obj = worn[wornSlot]
        return when {
            obj.isType(type) -> Validation.Valid.Unlimited
            obj != null && staffSubs.isValidSubstitute(type, obj) -> Validation.Valid.Unlimited
            else -> Validation.Invalid.NotWearing(type)
        }
    }

    public fun validateRune(
        inv: Inventory,
        righthand: InvObj?,
        lefthand: InvObj?,
        pouch: RunePouch?,
        rune: ItemServerType,
        required: Int,
        useFakeRunes: Boolean,
        runeFountain: Boolean,
        compact: CompactRuneRepository,
        unlimited: UnlimitedRuneRepository,
        subs: RuneSubstituteRepository,
        fakes: FakeRuneRepository,
    ): Validation {
        require(required > 0) { "`required` must be greater than 0. (required=$required)" }

        val compactId = compact[rune]

        // Note: There are other conditions that should be handled here alongside `runeFountain`,
        // but their use cases are currently unclear. We will skip them for now until more
        // information becomes available.
        if (compactId != null && runeFountain) {
            return Validation.Valid.Unlimited
        }

        if (unlimited.isHighPrioritySource(rune, righthand, lefthand)) {
            return Validation.Valid.Unlimited
        }

        if (useFakeRunes) {
            val fake = fakes[rune]
            if (fake != null) {
                val invFakeSlot = inv.indexOfFirst { it.isType(fake) }
                val invFakeRune = inv[invFakeSlot]
                val count = invFakeRune?.count ?: 0
                return if (count >= required) {

                    val internalName = RSCM.getReverseMapping(RSCMType.OBJ, fake.id)

                    val sources = listOf(Source.InvSource(internalName, invFakeSlot, required))
                    Validation.Valid.HasEnough(sources)
                } else {
                    Validation.Invalid.NotEnoughRunes(rune)
                }
            }
        }

        if (unlimited.isLowPrioritySource(rune, righthand, lefthand)) {
            return Validation.Valid.Unlimited
        }

        val invRuneSlot = inv.indexOfFirst { it.isType(rune) }
        val invRuneObj = inv[invRuneSlot]

        // Fast-path: return single-list `HasEnough` validation when inv has enough runes.
        if (invRuneObj != null && invRuneObj.count >= required) {
            val internalName = RSCM.getReverseMapping(RSCMType.OBJ, rune.id)
            val sources = listOf(Source.InvSource(internalName, invRuneSlot, required))
            return Validation.Valid.HasEnough(sources)
        }

        val sources = mutableListOf<Source>()
        var remaining = required

        // Reduce remaining count from existing inv rune (if available).
        if (invRuneObj != null) {
            remaining -= invRuneObj.count
            val internalName = RSCM.getReverseMapping(RSCMType.OBJ, rune.id)
            sources += Source.InvSource(internalName, invRuneSlot, invRuneObj.count)
        }

        if (pouch != null) {
            val pouchCompactRune1 = pouch.compactId1
            val pouchCompactRune2 = pouch.compactId2
            val pouchCompactRune3 = pouch.compactId3
            val pouchCompactRune4 = pouch.compactId4

            val pouchRuneCount: Int
            val pouchCountVarBit: String?
            when {
                pouchCompactRune1 == compactId -> {
                    pouchRuneCount = pouch.count1
                    pouchCountVarBit = pouch.countVarBit1
                }

                pouchCompactRune2 == compactId -> {
                    pouchRuneCount = pouch.count2
                    pouchCountVarBit = pouch.countVarBit2
                }

                pouchCompactRune3 == compactId -> {
                    pouchRuneCount = pouch.count3
                    pouchCountVarBit = pouch.countVarBit3
                }

                pouchCompactRune4 == compactId -> {
                    pouchRuneCount = pouch.count4
                    pouchCountVarBit = pouch.countVarBit4
                }

                else -> {
                    pouchRuneCount = 0
                    pouchCountVarBit = null
                }
            }

            if (pouchCountVarBit != null && pouchRuneCount >= remaining) {
                sources += Source.VarBitSource(pouchCountVarBit, remaining)
                return Validation.Valid.HasEnough(sources)
            }
        }

        val substitutes = subs[rune]
        if (substitutes != null) {
            for (sub in substitutes) {
                val validate =
                    validateRune(
                        inv = inv,
                        righthand = righthand,
                        lefthand = lefthand,
                        pouch = pouch,
                        rune = sub,
                        required = remaining,
                        useFakeRunes = useFakeRunes,
                        runeFountain = runeFountain,
                        compact = compact,
                        unlimited = unlimited,
                        subs = subs,
                        fakes = fakes,
                    )
                val validResult = validate as? Validation.Valid ?: continue
                return when (validResult) {
                    // If the substitute rune has an unlimited source, return it as `Unlimited`
                    // so that the base rune is not consumed.
                    is Validation.Valid.Unlimited -> {
                        Validation.Valid.Unlimited
                    }
                    // If the substitute rune has enough to satisfy the remaining amount, merge
                    // its sources into the current list and return a `HasEnough` result.
                    is Validation.Valid.HasEnough -> {
                        sources += validResult.sources
                        Validation.Valid.HasEnough(sources)
                    }
                }
            }
        }

        return Validation.Invalid.NotEnoughRunes(rune)
    }

    // Note: We _could_ use an enum config and repository to define these "rune packs," but since
    // they do not change often, it would likely be overengineering at this point. If the need
    // arises later, we can refactor without much effort.
    public fun validateRunePack(
        player: Player,
        spell: ItemServerType,
        useFakeRunes: Boolean,
        allowBlighted: Boolean,
    ): Validation.Valid? {

        return null
    }

    public sealed class Validation {
        public val isValid: Boolean
            get() = this is Valid

        public val isInvalid: Boolean
            get() = this is Invalid

        public sealed class Valid : Validation() {
            public data object Unlimited : Valid()

            public data class HasEnough(val sources: List<Source>) : Valid() {
                public constructor(single: Source) : this(listOf(single))
            }
        }

        public sealed class Invalid : Validation() {
            public data class NotEnoughRunes(val obj: ItemServerType) : Invalid()

            public data class NotWearing(val obj: ItemServerType) : Invalid()
        }
    }

    public sealed class Source {
        public data class InvSource(val obj: String, val slot: Int, val count: Int) :
            Source()

        public data class VarBitSource(val varbit: String, val count: Int) : Source()
    }

    public class RequirementList internal constructor(internal val reqs: List<Requirement>) {
        internal val size: Int
            get() = reqs.size

        internal fun findWithRemaining(obj: String): Requirement? {

            val internalName = RSCM.getReverseMapping(RSCMType.OBJ, obj.asRSCM(RSCMType.OBJ))

            return reqs.firstOrNull { it.type.isType(internalName) && it.remaining > 0 }
        }

        internal data class Requirement(
            val type: ItemServerType,
            var remaining: Int,
            val wornSlot: Int?,
        ) {
            val isWorn: Boolean
                get() = wornSlot != null
        }

        override fun toString(): String = "RequirementList(reqs=$reqs)"

        public companion object {
            public fun from(requirements: List<MagicSpell.ObjRequirement>): RequirementList {
                val mapped = requirements.map { Requirement(it.obj, it.count, it.wornSlot) }
                return RequirementList(mapped)
            }
        }
    }

    public data class RunePouch(
        val compactId1: Int,
        val compactId2: Int,
        val compactId3: Int,
        val compactId4: Int,
        val countVarBit1: String,
        val countVarBit2: String,
        val countVarBit3: String,
        val countVarBit4: String?,
        val count1: Int,
        val count2: Int,
        val count3: Int,
        val count4: Int,
    )
}
