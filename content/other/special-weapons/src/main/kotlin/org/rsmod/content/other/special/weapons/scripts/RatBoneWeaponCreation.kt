package org.rsmod.content.other.special.weapons.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons.Companion.BATTLESTAFF
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons.Companion.BOW
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons.Companion.MACE
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons.Companion.RUNE_MACE
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons.Companion.SPINE
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons.Companion.STAFF
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons.Companion.YEW_SHORTBOW
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons.RatBoneWeapon
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RatBoneWeaponCreation @Inject constructor(private val weapons: RatBoneWeapons) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(SPINE, RUNE_MACE) { mes("I should probably smith this at an anvil...") }
        onOpLocCategoryU("category.anvil", SPINE) { attempt(MACE) }
        onOpHeldU(SPINE, YEW_SHORTBOW) { attempt(BOW) }
        onOpHeldU(SPINE, BATTLESTAFF) { attempt(STAFF) }
    }

    private suspend fun ProtectedAccess.attempt(weapon: RatBoneWeapon) {
        if (weapon.tools.none { inv.contains(it) }) {
            mes("You'll need a ${weapon.toolName} to do that.")
            return
        }
        if (player.stat(weapon.stat) < LEVEL) {
            mes("You need a ${weapon.statName} level of $LEVEL to do that.")
            return
        }
        val created =
            with(weapons) {
                if (!hasMaterials(weapon)) {
                    mes(
                        if (weapon.runes > 0) {
                            "You need 1,000 chaos runes and a Scurrius' spine to create the Bone staff."
                        } else {
                            "Nothing interesting happens."
                        },
                    )
                    return
                }
                create(weapon)
            }
        if (!created) {
            return
        }
        anim(weapon.anim)
        weapon.spotanim?.let { spotanim(it) }
        weapon.sound?.let { soundSynth(it) }
        statAdvance(weapon.stat, XP)
        objbox(
            weapon.product,
            "You attach the Scurrius' spine to the ${weapon.baseName} to create the " +
                "${weapon.productName}.",
        )
    }

    private companion object {
        const val LEVEL = 35
        const val XP = 300.0
    }
}
