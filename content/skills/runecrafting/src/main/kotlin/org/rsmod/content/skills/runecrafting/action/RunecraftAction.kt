package org.rsmod.content.skills.runecrafting.action

import dev.openrune.types.ItemServerType
import kotlin.math.floor
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseRunecraftingLvl
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.ComboruneRecipeRow
import org.rsmod.api.table.runecrafting.RunecraftingRunesRow
import org.rsmod.content.skills.runecrafting.essencepouch.EssencePouch
import org.rsmod.content.skills.runecrafting.items.BloodEssence
import org.rsmod.content.skills.runecrafting.items.BloodEssence.applyBloodRuneBonus
import org.rsmod.content.skills.runecrafting.items.BindingNecklace.consumeChargeAfterCombo
import org.rsmod.content.skills.runecrafting.items.BindingNecklace.isWearing
import org.rsmod.content.skills.runecrafting.items.RaimentsOfTheEye.applyBonus
import org.rsmod.content.skills.runecrafting.magic.MagicImbue.isActive

object RunecraftAction {
    private const val RUNECRAFT_WAIT_CYCLE = 3
    private const val RUNECRAFT_SOUND = 2710
    private const val RUNE_ESSENCE = EssencePouch.RUNE_ESSENCE
    private const val PURE_ESSENCE = EssencePouch.PURE_ESSENCE
    private const val DAEYALT_ESSENCE = EssencePouch.DAEYALT_ESSENCE
    private const val GUARDIAN_ESSENCE = EssencePouch.GUARDIAN_ESSENCE
    private const val DARK_ESSENCE = EssencePouch.DARK_ESSENCE_FRAGMENT
    private const val DAEYALT_XP_MULTIPLIER = 1.5
    private const val OURANIA_XP_MULTIPLIER = 1.7
    private const val CORE_RUNE_MULTIPLIER = 11
    private const val CORE_XP_MULTIPLIER = 10

    private val runecraftingExtract = mapOf(
        "obj.scar_extract_warped" to 250,
        "obj.scar_extract_twisted" to 60,
        "obj.scar_extract_mangled" to 60,
        "obj.scar_extract_scarred" to 60
    )

    suspend fun ProtectedAccess.preCraft() {
        anim("seq.human_runecraft")
        spotanim("spotanim.runecrafting", height = 100)
        soundSynth(RUNECRAFT_SOUND)
        delay(RUNECRAFT_WAIT_CYCLE)
    }

    suspend fun ProtectedAccess.craftRune(
        rune: RunecraftingRunesRow,
        xpMods: XpModifiers,
        ouraniaAltar: Boolean = false,
    ) {
        if (!canCraftRune(rune)) {
            return
        }

        preCraft()

        val validEssence = rune.input.map { it.internalName }.toSet()
        val acceptsDaeyalt = PURE_ESSENCE in validEssence
        val daeyaltEssCount =
            if (acceptsDaeyalt) {
                inv.count(DAEYALT_ESSENCE)
            } else {
                0
            }

        if (daeyaltEssCount > 0) {
            craftDaeyaltEssence(rune, xpMods, daeyaltEssCount, ouraniaAltar)
            return
        }

        craftStandardEssence(rune, xpMods, validEssence, ouraniaAltar)
    }

    private suspend fun ProtectedAccess.craftDaeyaltEssence(
        rune: RunecraftingRunesRow,
        xpMods: XpModifiers,
        daeyaltEssCount: Int,
        ouraniaAltar: Boolean,
    ) {
        if (invDel(inv, DAEYALT_ESSENCE, daeyaltEssCount).failure) {
            return
        }

        val level = player.baseRunecraftingLvl
        val baseMultiplier = getBonusMultiplier(rune.output.internalName, level).toInt()
        val baseProduced = daeyaltEssCount * baseMultiplier
        val produced = applyBonus(baseProduced)

        val xpMultiplier =
            if (ouraniaAltar) {
                DAEYALT_XP_MULTIPLIER * OURANIA_XP_MULTIPLIER
            } else {
                DAEYALT_XP_MULTIPLIER
            }

        finishEssenceCraft(
            rune,
            daeyaltEssCount,
            baseProduced * rune.xp * xpMultiplier,
            xpMods,
            ouraniaAltar,
            produced,
        )
    }

    private suspend fun ProtectedAccess.craftStandardEssence(
        rune: RunecraftingRunesRow,
        xpMods: XpModifiers,
        validEssence: Set<String>,
        ouraniaAltar: Boolean,
    ) {
        val runeEssCount = if (RUNE_ESSENCE in validEssence) inv.count(RUNE_ESSENCE) else 0
        val pureEssCount = if (PURE_ESSENCE in validEssence) inv.count(PURE_ESSENCE) else 0
        val guardianEssCount =
            if (PURE_ESSENCE in validEssence) {
                inv.count(GUARDIAN_ESSENCE)
            } else {
                0
            }
        val darkEssCount =
            if (DARK_ESSENCE in validEssence) {
                inv.count(DARK_ESSENCE)
            } else {
                0
            }
        val totalEssence = runeEssCount + pureEssCount + guardianEssCount + darkEssCount
        if (totalEssence <= 0) {
            return
        }

        if (pureEssCount > 0 && invDel(inv, PURE_ESSENCE, pureEssCount).failure) {
            return
        }
        if (guardianEssCount > 0 && invDel(inv, GUARDIAN_ESSENCE, guardianEssCount).failure) {
            return
        }
        if (darkEssCount > 0 && invDel(inv, DARK_ESSENCE, darkEssCount).failure) {
            return
        }
        if (runeEssCount > 0 && invDel(inv, RUNE_ESSENCE, runeEssCount).failure) {
            return
        }

        val level = player.baseRunecraftingLvl
        val baseMultiplier = getBonusMultiplier(rune.output.internalName, level).toInt()

        val basePure = pureEssCount * baseMultiplier
        val baseGuardian = guardianEssCount * baseMultiplier
        val baseDark = darkEssCount * baseMultiplier
        val baseRune = runeEssCount * baseMultiplier
        val totalXp = (basePure + baseGuardian + baseDark + baseRune) * rune.xp
        val xpMultiplier = if (ouraniaAltar) OURANIA_XP_MULTIPLIER else 1.0
        val produced =
            applyBonus(basePure) +
                applyBonus(baseGuardian) +
                applyBonus(baseDark) +
                applyBonus(baseRune)

        finishEssenceCraft(
            rune,
            totalEssence,
            totalXp * xpMultiplier,
            xpMods,
            ouraniaAltar,
            produced,
        )
    }

    private suspend fun ProtectedAccess.finishEssenceCraft(
        rune: RunecraftingRunesRow,
        essenceConsumed: Int,
        xp: Double,
        xpMods: XpModifiers,
        ouraniaAltar: Boolean,
        producedRunes: Int? = null,
    ) {
        val level = player.baseRunecraftingLvl
        val baseMultiplier = getBonusMultiplier(rune.output.internalName, level).toInt()
        var totalRunes = producedRunes ?: applyBonus(essenceConsumed * baseMultiplier)

        if (inv.contains(rune.extract.internalName)) {
            totalRunes += runecraftingExtract[rune.extract.internalName] ?: 0
        }

        if (
            !ouraniaAltar &&
            rune.output.internalName == BloodEssence.BLOOD_RUNE
        ) {
            applyBloodRuneBonus(essenceConsumed)?.let { bonus ->
                totalRunes += bonus
            }
        }

        invAdd(inv, rune.output.internalName, totalRunes)
        advanceRunecraftingXp(xp, xpMods)
    }

    private suspend fun ProtectedAccess.canCraftRune(rune: RunecraftingRunesRow): Boolean {
        val level = player.baseRunecraftingLvl
        if (level < rune.statReq.first().t1) {
            mesbox(
                "You need Runecrafting level ${rune.statReq.first().t1} to craft ${rune.output.name.lowercase()}s.",
            )
            return false
        }

        val validEssenceIds = rune.input.map { it.internalName }.toSet()
        val acceptsPureSubstitutes = PURE_ESSENCE in validEssenceIds
        val checkIds =
            when {
                acceptsPureSubstitutes ->
                    validEssenceIds + DAEYALT_ESSENCE + GUARDIAN_ESSENCE
                DARK_ESSENCE in validEssenceIds ->
                    validEssenceIds
                else -> validEssenceIds
            }
        val hasEssence = checkIds.any(inv::contains)
        if (!hasEssence) {
            val essenceName = rune.input.first().name.lowercase()
            if (acceptsPureSubstitutes) {
                mesbox(
                    "You do not have any $essenceName, Daeyalt essence, or guardian essence to bind.",
                )
            } else {
                mesbox("You do not have any $essenceName to bind.")
            }
            return false
        }

        return true
    }

    suspend fun ProtectedAccess.craftOurania(xpMods: XpModifiers) {
        val daeyaltCount = inv.count(DAEYALT_ESSENCE)
        if (daeyaltCount > 0) {
            craftOuraniaDaeyalt(xpMods, daeyaltCount)
            return
        }

        if (!hasPureLikeEssence()) {
            mesbox("You do not have any pure essence to bind.")
            return
        }

        preCraft()

        val essenceCount = countPureLikeEssence()
        if (removePureLikeEssence(essenceCount).not()) {
            return
        }

        craftOuraniaBatch(essenceCount, xpMultiplier = OURANIA_XP_MULTIPLIER, xpMods = xpMods)
    }

    private suspend fun ProtectedAccess.craftOuraniaDaeyalt(
        xpMods: XpModifiers,
        daeyaltCount: Int,
    ) {
        preCraft()
        if (invDel(inv, DAEYALT_ESSENCE, daeyaltCount).failure) {
            return
        }
        val xpMultiplier = DAEYALT_XP_MULTIPLIER * OURANIA_XP_MULTIPLIER
        craftOuraniaBatch(daeyaltCount, xpMultiplier = xpMultiplier, xpMods = xpMods)
    }

    private suspend fun ProtectedAccess.craftOuraniaBatch(
        essenceCount: Int,
        xpMultiplier: Double,
        xpMods: XpModifiers,
    ) {
        val level = player.baseRunecraftingLvl
        var totalXp = 0.0

        repeat(essenceCount) {
            val rune = rollOuraniaRune(level)
            val multiplier = getBonusMultiplier(rune.output.internalName, level).toInt()
            val produced = applyBonus(multiplier)
            totalXp += rune.xp * xpMultiplier * multiplier
            invAdd(inv, rune.output.internalName, produced)
        }

        advanceRunecraftingXp(totalXp, xpMods)
    }

    suspend fun ProtectedAccess.craftCore(
        rune: RunecraftingRunesRow,
        coreItem: String,
        xpMods: XpModifiers,
    ) {
        if (!inv.contains(coreItem)) {
            return
        }

        val level = player.baseRunecraftingLvl
        if (level < rune.statReq.first().t1) {
            mesbox(
                "You need Runecrafting level ${rune.statReq.first().t1} to craft ${rune.output.name.lowercase()}s.",
            )
            return
        }

        preCraft()

        if (invDel(inv, coreItem, 1).failure) {
            return
        }

        val multiplier = getBonusMultiplier(rune.output.internalName, level).toInt()
        val produced = applyBonus(CORE_RUNE_MULTIPLIER * multiplier)
        invAdd(inv, rune.output.internalName, produced)
        advanceRunecraftingXp(rune.xp * CORE_XP_MULTIPLIER.toDouble(), xpMods)
    }

    suspend fun ProtectedAccess.craftAether(xpMods: XpModifiers) {
        val aetherRune =
            RunecraftingRunesRow.all().firstOrNull {
                it.output.internalName == "obj.aetherrune"
            } ?: return

        val level = player.baseRunecraftingLvl
        if (level < aetherRune.statReq.first().t1) {
            mesbox("You need Runecrafting level ${aetherRune.statReq.first().t1} to craft aether runes.")
            return
        }

        val guardianCount = inv.count(GUARDIAN_ESSENCE)
        val soulCount = inv.count("obj.soulrune")
        val craftCount = minOf(guardianCount, soulCount)
        if (craftCount <= 0) {
            mes("You need guardian essence and soul runes to craft aether runes.")
            return
        }

        if (!inv.contains("obj.cosmic_soul_catalyst")) {
            mes("You need an aether catalyst to craft aether runes.")
            return
        }

        preCraft()

        if (invDel(inv, "obj.cosmic_soul_catalyst", 1).failure) {
            return
        }
        if (invDel(inv, GUARDIAN_ESSENCE, craftCount).failure) {
            return
        }
        if (invDel(inv, "obj.soulrune", craftCount).failure) {
            return
        }

        var totalRunes = applyBonus(craftCount)
        if (inv.contains("obj.scar_extract_scarred")) {
            totalRunes += runecraftingExtract["obj.scar_extract_scarred"] ?: 0
        }

        invAdd(inv, aetherRune.output.internalName, totalRunes)
        advanceRunecraftingXp(craftCount * aetherRune.xp.toDouble(), xpMods)
    }

    private fun rollOuraniaRune(level: Int): RunecraftingRunesRow {
        val eligible =
            RunecraftingRunesRow.all().filter { row ->
                val output = row.output.internalName
                output !in ouraniaExcludedRunes &&
                    row.statReq.first().t1 <= level &&
                    PURE_ESSENCE in row.input.map { it.internalName }
            }
        return eligible.randomOrNull() ?: RunecraftingRunesRow.all().first { it.output.internalName == "obj.airrune" }
    }

    private val ouraniaExcludedRunes =
        setOf(
            "obj.sunfirerune",
            "obj.wrathrune",
            "obj.aetherrune",
            "obj.soulrune",
        )

    fun getBonusMultiplier(rune: String, level: Int): Double =
        when (rune) {
            "obj.airrune" -> floor(level / 11.0) + 1
            "obj.mindrune" -> floor(level / 14.0) + 1
            "obj.waterrune" -> floor(level / 19.0) + 1
            "obj.earthrune" -> floor(level / 26.0) + 1
            "obj.firerune" -> floor(level / 35.0) + 1
            "obj.bodyrune" -> floor(level / 46.0) + 1
            "obj.cosmicrune" -> floor(level / 59.0) + 1
            "obj.chaosrune" -> floor(level / 74.0) + 1
            "obj.naturerune" -> floor(level / 91.0) + 1
            else -> 1.0
        }

    suspend fun ProtectedAccess.craftCombination(combo: ComboruneRecipeRow, xpMods: XpModifiers) {
        val output = combo.output ?: return
        val input = combo.input ?: return
        val talisman = combo.talisman ?: return
        val xp = combo.xp ?: return

        if (!canCraftCombo(output, input, talisman, combo.statReq.first().t1)) {
            return
        }

        preCraft()

        val craftCount =
            minOf(countPureLikeEssence(), inv.count(input.internalName))
        if (craftCount <= 0) {
            return
        }

        val usingMagicImbue = player.isActive()
        if (!usingMagicImbue && invDel(inv, talisman.internalName, 1).failure) {
            return
        }
        if (removePureLikeEssence(craftCount).not()) {
            return
        }
        val removedRunes = invDel(inv, input.internalName, craftCount)
        if (removedRunes.failure) {
            return
        }

        val wearingBinding = player.isWearing()
        val removedCount = removedRunes.completed()
        var finalCount =
            if (wearingBinding) {
                removedCount
            } else {
                (1..removedCount).count { random.of(100) < 50 }
            }

        if (inv.contains("obj.scar_extract_twisted")) {
            finalCount += runecraftingExtract["obj.scar_extract_twisted"] ?: 0
        }

        invAdd(inv, output.internalName, finalCount)
        advanceRunecraftingXp(finalCount * (xp.toDouble() / 10.0), xpMods)

        if (wearingBinding) {
            consumeChargeAfterCombo()
        }
    }

    fun ProtectedAccess.advanceRunecraftingXp(baseXp: Double, xpMods: XpModifiers) {
        statAdvance("stat.runecrafting", baseXp * xpMods.get(player, "stat.runecrafting"))
    }

    private fun ProtectedAccess.countPureLikeEssence(): Int =
        inv.count(PURE_ESSENCE) + inv.count(GUARDIAN_ESSENCE)

    private fun ProtectedAccess.hasPureLikeEssence(): Boolean = countPureLikeEssence() > 0

    private fun ProtectedAccess.removePureLikeEssence(amount: Int): Boolean {
        var remaining = amount
        if (remaining <= 0) {
            return true
        }

        val pureAvailable = inv.count(PURE_ESSENCE)
        if (pureAvailable > 0) {
            val take = minOf(pureAvailable, remaining)
            if (invDel(inv, PURE_ESSENCE, take).failure) {
                return false
            }
            remaining -= take
        }

        if (remaining > 0 && invDel(inv, GUARDIAN_ESSENCE, remaining).failure) {
            return false
        }

        return true
    }

    private suspend fun ProtectedAccess.canCraftCombo(
        output: ItemServerType,
        input: ItemServerType,
        talisman: ItemServerType,
        requiredLevel: Int?,
    ): Boolean {
        val levelReq = requiredLevel ?: return false
        val level = player.baseRunecraftingLvl
        val outputName = output.name
        val inputName = input.name
        val talismanName = talisman.name

        if (level < levelReq) {
            mesbox("You need Runecrafting level $levelReq to craft ${outputName}s.")
            return false
        }
        if (!hasPureLikeEssence()) {
            mes("You need pure essence to craft ${outputName}s.")
            return false
        }
        if (!inv.contains(input.internalName)) {
            mes("You need ${inputName}s to craft ${outputName}s.")
            return false
        }
        if (!player.isActive() && !inv.contains(talisman.internalName)) {
            mes("You need a $talismanName to craft ${outputName}s.")
            return false
        }
        return true
    }
}
