package org.rsmod.content.interfaces.combat.tab

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.enums.EnumTypeNonNullMap
import dev.openrune.util.WeaponCategory
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.enums.NamedEnums.weapon_last_stance_varbits
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ui.PlayerInterfaceUpdates
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.boolVarp
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.specials.SpecialAttack
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.SpecialAttackType
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.autocast.AutocastWeapons
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import java.util.Collections
import java.util.WeakHashMap

/*
 * Note: The logic and execution order in this script are designed for emulation accuracy. While
 * this approach is not optimized for efficiency and includes redundant operations, it should not
 * have a big impact on real-world performance.
 */
class CombatTabScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val weaponStyles: AttackStyles,
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val autocast: AutocastWeapons,
    private val energy: SpecialAttackEnergy,
    private val specialReg: SpecialAttackRegistry,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private var Player.combatStance by enumVarp<CombatStance>("varp.com_mode")
    private var Player.meleeStyle by enumVarp<MeleeAttackStyle>("varp.com_stance")
    private var Player.specialType by enumVarp<SpecialAttackType>("varp.sa_attack")
    private var Player.autoRetaliateDisabled by boolVarp("varp.option_nodef")

    private var Player.autocastEnabled by boolVarBit("varbit.autocast_set")
    private var Player.autocastSpell by intVarBit("varbit.autocast_spell")
    private var Player.defensiveCasting by boolVarBit("varbit.autocast_defmode")
    private var Player.spellbook by enumVarBit<Spellbook>("varbit.spellbook")
    private var Player.autocastSetupObj by intVarp("varp.spitfire_coord")
    private var Player.activeAutocastSpellObj by intVarp("varp.autocast_spell_obj")

    private lateinit var stanceSaveVarBits: EnumTypeNonNullMap<Int, Int>

    override fun ScriptContext.startup() {
        stanceSaveVarBits = weapon_last_stance_varbits.filterValuesNotNull()

        onIfOpen("interface.combat_interface") { player.updateCombatTab() }
        onIfClose("interface.autocast") { pendingAutocastDefensiveCast.remove(player) }
        onIfClose("interface.magic_spellbook") { pendingAutocastDefensiveCast.remove(player) }
        onWearposChange { player.onWearposChange(wearpos) }

        onIfOverlayButton("component.combat_interface:retaliate") { player.selectAutoRetaliate() }

        onIfOverlayButton("component.combat_interface:0") { player.selectStance(CombatStance.Stance1) }
        onIfOverlayButton("component.combat_interface:1") { player.selectStance(CombatStance.Stance2) }
        onIfOverlayButton("component.combat_interface:2") { player.selectStance(CombatStance.Stance3) }
        onIfOverlayButton("component.combat_interface:3") { player.selectStance(CombatStance.Stance4) }
        onPlayerQueueWithArgs("queue.attackstyle_change") { player.setStance(it.args) }

        onIfOverlayButton("component.combat_interface:special_attack") { player.toggleSpecialAttack() }
        onIfOverlayButton("component.orbs:specbutton") { player.toggleSpecialAttack() }
        onPlayerQueue("queue.sa_instant_spec") { activateInstantSpecial() }

        onIfOverlayButton("component.combat_interface:autocast_defensive") {
            player.openAutocastSelection(defensiveCast = true)
        }
        onIfOverlayButton("component.combat_interface:autocast_normal") {
            player.openAutocastSelection(defensiveCast = false)
        }

        for ((autocastId, spell) in spells.autocastSpells()) {
            onIfOverlayButton(spell.component) { player.selectAutocastSpell(autocastId, spell, it.op) }
        }
        onIfOverlayButton("component.autocast:spells") { player.selectAutocastSpell(it) }
    }

    private fun Player.updateCombatTab() {
        PlayerInterfaceUpdates.updateCombatTab(this)
    }

    private fun Player.onWearposChange(wearpos: Wearpos) {
        if (wearpos == Wearpos.RightHand || wearpos == Wearpos.LeftHand) {
            loadSavedWeaponStance()
            loadSavedMagicAutocast()
            validateStanceStyle()
            PlayerInterfaceUpdates.updateCombatLevel(this)
        }
    }

    private fun Player.loadSavedWeaponStance() {
        val weaponType = getOrNull(righthand)
        val weaponCategory = WeaponCategory.getOrUnarmed(weaponType?.weaponCategory?.id)

        val varbitID = stanceSaveVarBits.getOrNull(weaponCategory.id) ?: return
        val varbit = ServerCacheManager.getVarbit(varbitID)
        if (varbit != null) {
            val savedStanceVar = vars[varbit]

            // The null fallback means any new weapon categories being worn will default to
            // `Stance1` (usually top-left selection). This is the official behavior when
            // wielding new weapon types.
            val stance = CombatStance[savedStanceVar] ?: CombatStance.Stance1
            combatStance = stance
        }
    }

    private fun Player.loadSavedMagicAutocast() {
        val weaponType = getOrNull(righthand)
        val weaponCategory = WeaponCategory.getOrUnarmed(weaponType?.weaponCategory?.id)
        val autocastVarBits = autocast.getVarBits(weaponCategory)

        if (weaponType == null || autocastVarBits == null) {
            clearActiveAutocast()
            return
        }

        val savedAutocastId = vars[autocastVarBits.autocastId]
        val savedAutocastSpell = spells.getAutocastSpell(savedAutocastId)
        if (savedAutocastId == 0 || savedAutocastSpell == null) {
            clearActiveAutocast()
            return
        }

        // Note: As of writing this logic, the official game does _not_ check that the spell's
        // spellbook param matches the player's current spellbook when switching weapons.

        val isValidStaff = autocast.canStaffAutocast(weaponType, savedAutocastId)
        if (!isValidStaff) {
            clearActiveAutocast()
            return
        }

        // `hasRunes` is responsible for sending the "error" message to the player.
        val hasRunes = runes.hasRunes(this, savedAutocastSpell)
        if (!hasRunes) {
            autocast.reset(this, autocastVarBits)
            clearActiveAutocast()
            return
        }

        val savedDefensiveCast = vars[autocastVarBits.defensiveCast] != 0
        autocastSpell = savedAutocastId
        defensiveCasting = savedDefensiveCast
        autocastEnabled = autocastSpell != 0
        activeAutocastSpellObj = savedAutocastSpell.obj.id
    }

    private fun Player.openAutocastSelection(defensiveCast: Boolean) {
        ifClose(eventBus)

        val weaponType = getOrNull(righthand)
        val weaponCategory = WeaponCategory.getOrUnarmed(weaponType?.weaponCategory?.id)
        val autocastVarBits = autocast.getVarBits(weaponCategory)
        if (weaponType == null || autocastVarBits == null) {
            mes("You need to wield a staff to autocast spells.")
            clearActiveAutocast()
            updateCombatTab()
            return
        }

        if (!autocast.canAutocastSpellbook(weaponType, spellbook)) {
            mes(autocast.unsupportedSpellbookMessage(spellbook))
            clearActiveAutocast()
            updateCombatTab()
            return
        }

        pendingAutocastDefensiveCast[this] = defensiveCast
        syncAutocastSetupObj()
        ifOpenOverlay(
            "interface.autocast",
            "component.toplevel_osrs_stretch:side0",
            eventBus,
        )
        ifSetEvents("component.toplevel_osrs_stretch:stone0", -1..-1, IfEvent.Op1)
        ifSetEvents("component.autocast:spells", 0..58, IfEvent.Op1)
        ClientScripts.toplevelSidebuttonSwitch(this, ToplevelCombatTab)
    }

    private fun Player.syncAutocastSetupObj() {
        val weapon = getOrNull(righthand)
        autocastSetupObj = resolveAutocastSetupObj(weapon)
    }

    private fun Player.resolveAutocastSetupObj(weapon: ItemServerType?): Int =
        if (weapon == null) {
            NullObj
        } else {
            when (spellbook) {
                Spellbook.Standard -> StandardAutocastSelectors[weapon.id] ?: NullObj
                Spellbook.Ancients ->
                    if (autocast.canAutocastSpellbook(weapon, Spellbook.Ancients)) {
                        AncientLayoutObj
                    } else {
                        EmptyLayoutObj
                    }
                Spellbook.Arceuus ->
                    if (autocast.canAutocastSpellbook(weapon, Spellbook.Arceuus)) {
                        ArceuusLayoutObj
                    } else {
                        EmptyLayoutObj
                    }
                else -> EmptyLayoutObj
            }
        }

    private fun Player.clearActiveAutocast() {
        autocastEnabled = false
        autocastSpell = 0
        defensiveCasting = false
        activeAutocastSpellObj = NullObj
    }

    private fun Player.restoreCombatTabOverlay() {
        ifOpenOverlay(
            "interface.combat_interface",
            "component.toplevel_osrs_stretch:side0",
            eventBus,
        )
        ClientScripts.toplevelSidebuttonSwitch(this, ToplevelCombatTab)
        updateCombatTab()
    }

    private fun Player.selectAutocastSpell(autocastId: Int, spell: MagicSpell, op: IfButtonOp) {
        val pendingDefensiveCast = pendingAutocastDefensiveCast[this]
        val opText = spell.component.op.getOrNull(op.slot - 1)
        if (pendingDefensiveCast == null && opText?.contains("autocast", ignoreCase = true) != true) {
            return
        }

        val defensiveCast =
            pendingDefensiveCast ?: opText?.contains("defensive", ignoreCase = true) == true
        selectAutocastSpell(
            autocastId = autocastId,
            spell = spell,
            defensiveCast = defensiveCast,
            restoreCombatTab = pendingDefensiveCast != null,
        )
    }

    private fun Player.selectAutocastSpell(event: IfOverlayButton) {
        val defensiveCast = pendingAutocastDefensiveCast[this] ?: return
        if (event.op != IfButtonOp.Op1) {
            return
        }
        if (event.comsub == AutocastCancelSub) {
            cancelAutocastSelection()
            return
        }

        val selection = resolveAutocastSelection(event) ?: return
        selectAutocastSpell(
            autocastId = selection.autocastId,
            spell = selection.spell,
            defensiveCast = defensiveCast,
            restoreCombatTab = true,
        )
    }

    private fun Player.selectAutocastSpell(
        autocastId: Int,
        spell: MagicSpell,
        defensiveCast: Boolean,
        restoreCombatTab: Boolean,
    ) {
        pendingAutocastDefensiveCast.remove(this)
        ifClose(eventBus)
        if (restoreCombatTab) {
            restoreCombatTabOverlay()
        }

        val weaponType = getOrNull(righthand)
        val weaponCategory = WeaponCategory.getOrUnarmed(weaponType?.weaponCategory?.id)
        val autocastVarBits = autocast.getVarBits(weaponCategory)
        if (weaponType == null || autocastVarBits == null) {
            mes("You need to wield a staff to autocast spells.")
            return
        }

        if (spell.spellbook != spellbook) {
            mes("You can't autocast that spell with your current active spellbook.")
            return
        }

        val canUseStaff = autocast.canStaffAutocast(this, weaponType, autocastId)
        if (!canUseStaff) {
            return
        }

        val canCastSpell = runes.canCastSpell(this, spell)
        if (!canCastSpell) {
            return
        }

        autocast.set(this, autocastVarBits, autocastId, defensiveCast)
        autocastSpell = autocastId
        defensiveCasting = defensiveCast
        autocastEnabled = true
        activeAutocastSpellObj = spell.obj.id
        updateCombatTab()
    }

    private fun Player.cancelAutocastSelection() {
        pendingAutocastDefensiveCast.remove(this)
        ifClose(eventBus)
        restoreCombatTabOverlay()

        val weaponType = getOrNull(righthand)
        val weaponCategory = WeaponCategory.getOrUnarmed(weaponType?.weaponCategory?.id)
        val autocastVarBits = autocast.getVarBits(weaponCategory)
        if (autocastVarBits != null) {
            autocast.reset(this, autocastVarBits)
        }
        autocastSpell = 0
        defensiveCasting = false
        autocastEnabled = false
        activeAutocastSpellObj = NullObj
        updateCombatTab()
    }

    private fun Player.resolveAutocastSelection(event: IfOverlayButton): AutocastSelection? {
        val obj = event.obj
        if (obj != null) {
            autocastSpellByObj(obj)?.let {
                return it
            }
        }

        spells.getAutocastSpell(event.comsub)?.takeIf { it.spellbook == spellbook }?.let {
            return AutocastSelection(event.comsub, it)
        }
        return null
    }

    private fun autocastSpellByObj(obj: ItemServerType): AutocastSelection? =
        spells.autocastSpells()
            .entries
            .firstOrNull { it.value.obj.id == obj.id }
            ?.let { AutocastSelection(it.key, it.value) }

    private fun Player.validateStanceStyle() {
        val weaponType = getOrNull(righthand)
        val startStance = combatStance

        val weaponStyle = weaponStyles.resolve(weaponType, startStance.varValue)
        val validatedStance = if (weaponStyle == null) CombatStance.Stance1 else startStance
        this.combatStance = validatedStance

        val meleeStyle = MeleeAttackStyle.from(weaponStyle)
        if (meleeStyle != null) {
            this.meleeStyle = meleeStyle
        }
    }

    private fun Player.selectAutoRetaliate() {
        ifClose(eventBus)

        if (isAccessProtected) {
            // Unlike changing com_mode, this does not queue the toggle; the click is
            // simply discarded.
            return
        }

        autoRetaliateDisabled = !autoRetaliateDisabled
    }

    private fun Player.selectStance(stance: CombatStance) {
        ifClose(eventBus)

        if (isAccessProtected) {
            clearQueue("queue.attackstyle_change")
            queue("queue.attackstyle_change", 1, stance)
            return
        }

        setStance(stance)
    }

    private fun Player.setStance(stance: CombatStance) {
        val weapon = getOrNull(righthand)
        applyDinhsBulwarkDelay(weapon, stance)
        setWeaponStance(stance)
        validateChangedStanceStyle(weapon)
        saveCurrentStanceStyle()
    }

    private fun Player.applyDinhsBulwarkDelay(weapon: ItemServerType?, stance: CombatStance) {
        if (weapon == null || !weapon.isCategoryType("category.dinhs_bulwark")) {
            return
        }

        // When going from `Block` to `Pummel` while using Dinh's bulwark, there is an 8-cycle
        // delay added to combat (handled through a special queue).
        val wasBlocking = combatStance == CombatStance.Stance4
        if (wasBlocking && combatStance != stance) {
            clearQueue("queue.dinhs_combat_delay")
            longQueueDiscard("queue.dinhs_combat_delay", 8)
        }
    }

    private fun Player.setWeaponStance(stance: CombatStance) {
        combatStance = stance
        PlayerInterfaceUpdates.updateWeaponCategoryText(this)
    }

    private fun Player.validateChangedStanceStyle(weapon: ItemServerType?) {
        val startStance = combatStance

        val attackStyle = weaponStyles.resolve(weapon, startStance.varValue)
        val validated = if (attackStyle == null) CombatStance.Stance1 else startStance
        setWeaponStance(validated)

        // Subtle difference with other "stance style validation" function is that there is
        // an explicit equals condition based on current `meleeStyle`.
        val meleeStyle = MeleeAttackStyle.from(attackStyle)
        if (meleeStyle != null && this.meleeStyle != meleeStyle) {
            this.meleeStyle = meleeStyle
        }
    }

    private fun Player.saveCurrentStanceStyle() {
        val weaponType = getOrNull(righthand)
        val weaponCategory = WeaponCategory.getOrUnarmed(weaponType?.weaponCategory?.id)
        val varbitID = stanceSaveVarBits.getOrNull(weaponCategory.id) ?: return
        val varbit = ServerCacheManager.getVarbit(varbitID) ?: return
        VarPlayerIntMapSetter.set(this, varbit, combatStance.varValue)
    }

    private fun Player.toggleSpecialAttack() {
        when (specialType) {
            SpecialAttackType.None -> enableSpecialAttack()
            SpecialAttackType.Weapon -> resetSpecialType()
            SpecialAttackType.Shield -> {
                // Shield specials can only be reset by reactivating them the same way they
                // were enabled. For example, re-selecting the "Activate" option on a dragonfire
                // shield. They cannot be disabled via the special attack orb or the attack tab
                // special bar.
            }
        }
    }

    private fun Player.enableSpecialAttack() {
        val righthand = righthand ?: return
        when (specialReg[righthand]) {
            is SpecialAttack.Combat -> activateCombatSpecial()
            is SpecialAttack.Instant -> attemptInstantSpecial()
            null -> {
                resetSpecialType()
                mes("This weapon does not have a special attack.")
            }
        }
    }

    private fun Player.activateCombatSpecial() {
        specialType = SpecialAttackType.Weapon
    }

    private fun Player.attemptInstantSpecial() {
        resetSpecialType()

        if ("queue.sa_instant_spec" in queueList) {
            return
        }

        ifClose(eventBus)
        val activated = protectedAccess.launch(this) { activateInstantSpecial() }
        if (!activated) {
            strongQueue("queue.sa_instant_spec", 1)
        }
    }

    private suspend fun ProtectedAccess.activateInstantSpecial() {
        val righthand = player.righthand ?: return
        val special = specialReg[righthand]
        if (special !is SpecialAttack.Instant) {
            return
        }

        val specializedEnergyReq = energy.isSpecializedRequirement(special.energyInHundreds)
        if (!specializedEnergyReq) {
            val hasRequiredEnergy = energy.hasSpecialEnergy(player, special.energyInHundreds)
            if (!hasRequiredEnergy) {
                mes("You don't have enough power left.")
                return
            }
        }

        val activated = special.activate(this)
        if (!specializedEnergyReq && activated) {
            energy.takeSpecialEnergy(player, special.energyInHundreds)
        }
    }

    private fun Player.resetSpecialType() {
        specialType = SpecialAttackType.None
    }

    private data class AutocastSelection(val autocastId: Int, val spell: MagicSpell)

    private companion object {
        val pendingAutocastDefensiveCast: MutableMap<Player, Boolean> =
            Collections.synchronizedMap(WeakHashMap())
        const val AutocastCancelSub = 0
        const val ToplevelCombatTab = 0
        const val NullObj = -1
        val EmptyLayoutObj = obj("obj.coins")
        val AncientLayoutObj = obj("obj.staff_of_zaros")
        val ArceuusLayoutObj = obj("obj.sos_skull_sceptre")
        val SkullSceptreImbuedObj = obj("obj.sos_skull_sceptre_imbued")
        val VoidKnightMaceObj = obj("obj.pest_void_knight_mace")
        val VoidKnightMaceLockedObj = obj("obj.pest_void_knight_mace_trouver")
        val IbansStaffObj = obj("obj.ibanstaff")
        val IbansStaffUpgradedObj = obj("obj.ibanstaff_upgraded")
        val SaradominStaffObj = obj("obj.saradomin_staff")
        val GuthixStaffObj = obj("obj.guthix_staff")
        val ZamorakStaffObj = obj("obj.zamorak_staff")
        val SlayerStaffObj = obj("obj.slayer_staff")
        val SlayerStaffEnchantedObj = obj("obj.slayer_staff_enchanted")
        val StaffOfTheDeadObj = obj("obj.sotd")
        val BrStaffOfTheDeadObj = obj("obj.br_sotd")
        val ToxicStaffOfTheDeadObj = obj("obj.toxic_sotd")
        val ToxicStaffOfTheDeadChargedObj = obj("obj.toxic_sotd_charged")
        val ToxicStaffOfTheDeadDeadmanObj = obj("obj.toxic_sotd_deadman")
        val ToxicStaffOfTheDeadChargedDeadmanObj = obj("obj.toxic_sotd_charged_deadman")
        val StaffOfLightObj = obj("obj.staff_of_light")
        val StaffOfBalanceObj = obj("obj.staff_of_balance")
        val ThammaronsSceptreUnchargedObj = obj("obj.wild_cave_sceptre_uncharged")
        val ThammaronsSceptreChargedObj = obj("obj.wild_cave_sceptre_charged")
        val ThammaronsSceptreUnchargedAObj = obj("obj.wild_cave_sceptre_uncharged_recol")
        val ThammaronsSceptreChargedAObj = obj("obj.wild_cave_sceptre_charged_recol")
        val AccursedSceptreUnchargedObj = obj("obj.wild_cave_accursed_uncharged")
        val AccursedSceptreChargedObj = obj("obj.wild_cave_accursed_charged")
        val AccursedSceptreUnchargedAObj = obj("obj.wild_cave_accursed_uncharged_recol")
        val AccursedSceptreChargedAObj = obj("obj.wild_cave_accursed_charged_recol")

        val StandardAutocastSelectors =
            mapOf(
                SaradominStaffObj to SaradominStaffObj,
                GuthixStaffObj to GuthixStaffObj,
                ZamorakStaffObj to ZamorakStaffObj,
                SkullSceptreImbuedObj to SkullSceptreImbuedObj,
                VoidKnightMaceObj to VoidKnightMaceObj,
                VoidKnightMaceLockedObj to VoidKnightMaceObj,
                IbansStaffObj to IbansStaffObj,
                IbansStaffUpgradedObj to IbansStaffObj,
                SlayerStaffObj to SlayerStaffObj,
                SlayerStaffEnchantedObj to SlayerStaffObj,
                StaffOfTheDeadObj to StaffOfTheDeadObj,
                BrStaffOfTheDeadObj to StaffOfTheDeadObj,
                ToxicStaffOfTheDeadObj to StaffOfTheDeadObj,
                ToxicStaffOfTheDeadChargedObj to StaffOfTheDeadObj,
                ToxicStaffOfTheDeadDeadmanObj to StaffOfTheDeadObj,
                ToxicStaffOfTheDeadChargedDeadmanObj to StaffOfTheDeadObj,
                StaffOfLightObj to StaffOfLightObj,
                StaffOfBalanceObj to StaffOfBalanceObj,
                ThammaronsSceptreUnchargedObj to ThammaronsSceptreUnchargedAObj,
                ThammaronsSceptreChargedObj to ThammaronsSceptreChargedAObj,
                ThammaronsSceptreUnchargedAObj to ThammaronsSceptreUnchargedAObj,
                ThammaronsSceptreChargedAObj to ThammaronsSceptreChargedAObj,
                AccursedSceptreUnchargedObj to AccursedSceptreUnchargedAObj,
                AccursedSceptreChargedObj to AccursedSceptreChargedAObj,
                AccursedSceptreUnchargedAObj to AccursedSceptreUnchargedAObj,
                AccursedSceptreChargedAObj to AccursedSceptreChargedAObj,
            )

        private fun obj(ref: String): Int = ref.asRSCM(RSCMType.OBJ)
    }
}
