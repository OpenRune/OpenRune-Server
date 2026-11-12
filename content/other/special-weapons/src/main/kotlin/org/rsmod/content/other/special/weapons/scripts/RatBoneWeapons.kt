package org.rsmod.content.other.special.weapons.scripts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.special.weapons.magic.BoneStaffWeapons
import org.rsmod.game.inv.isType

class RatBoneWeapons @Inject constructor(private val charges: ObjChargeManager) {
    fun ProtectedAccess.hasMaterials(weapon: RatBoneWeapon, fee: Int = 0): Boolean =
        inv.contains(SPINE) &&
            inv.contains(weapon.base) &&
            invTotal(inv, CHAOS_RUNE) >= weapon.runes &&
            invTotal(inv, COINS) >= fee

    fun ProtectedAccess.create(weapon: RatBoneWeapon, fee: Int = 0): Boolean {
        if (!hasMaterials(weapon, fee)) {
            return false
        }
        val baseSlot = inv.indexOfFirst { it.isType(weapon.base) }
        val transaction =
            player.invTransaction(inv) {
                val from = select(inv)
                delete {
                    this.from = from
                    this.obj = SPINE.asRSCM(RSCMType.OBJ)
                    this.strictCount = 1
                }
                if (weapon.runes > 0) {
                    delete {
                        this.from = from
                        this.obj = CHAOS_RUNE.asRSCM(RSCMType.OBJ)
                        this.strictCount = weapon.runes
                    }
                }
                if (fee > 0) {
                    delete {
                        this.from = from
                        this.obj = COINS.asRSCM(RSCMType.OBJ)
                        this.strictCount = fee
                    }
                }
                delete {
                    this.from = from
                    this.obj = weapon.base.asRSCM(RSCMType.OBJ)
                    this.strictCount = 1
                    this.strictSlot = baseSlot
                }
                insert {
                    this.into = from
                    this.obj = weapon.product.asRSCM(RSCMType.OBJ)
                    this.strictCount = 1
                    this.strictSlot = baseSlot
                }
            }
        if (transaction.failure) {
            return false
        }
        if (weapon.runes > 0) {
            charges.setValue(inv, baseSlot, BoneStaffWeapons.CHARGES_VAROBJ, weapon.runes)
        }
        return true
    }

    class RatBoneWeapon(
        val base: String,
        val baseName: String,
        val product: String,
        val productName: String,
        val stat: String,
        val statName: String,
        val tools: List<String>,
        val toolName: String,
        val anim: String,
        val spotanim: String? = null,
        val sound: String? = null,
        val runes: Int = 0,
    )

    companion object {
        const val SPINE = "obj.rat_boss_spine"
        const val RUNE_MACE = "obj.rune_mace"
        const val YEW_SHORTBOW = "obj.yew_shortbow"
        const val BATTLESTAFF = "obj.battlestaff"
        private const val CHAOS_RUNE = "obj.chaosrune"
        private const val COINS = "obj.coins"

        val MACE =
            RatBoneWeapon(
                base = RUNE_MACE,
                baseName = "Rune mace",
                product = "obj.rat_bone_mace",
                productName = "Bone mace",
                stat = "stat.smithing",
                statName = "Smithing",
                tools = listOf("obj.hammer", "obj.imcando_hammer", "obj.imcando_hammer_offhand"),
                toolName = "hammer",
                anim = "seq.human_smithing",
            )

        val BOW =
            RatBoneWeapon(
                base = YEW_SHORTBOW,
                baseName = "Yew shortbow",
                product = "obj.rat_bone_bow",
                productName = "Bone shortbow",
                stat = "stat.fletching",
                statName = "Fletching",
                tools = listOf("obj.knife"),
                toolName = "Knife",
                anim = "seq.stringing_yew_shortbow",
            )

        val STAFF =
            RatBoneWeapon(
                base = BATTLESTAFF,
                baseName = "Battlestaff",
                product = BoneStaffWeapons.BONE_STAFF,
                productName = "Bone staff",
                stat = "stat.crafting",
                statName = "Crafting",
                tools = listOf("obj.chisel"),
                toolName = "Chisel",
                anim = "seq.human_battlestaff_crafting",
                spotanim = "spotanim.battlestaff_rat_bone_crafting_spotanim",
                sound = "synth.attach_orb",
                runes = 1_000,
            )

        val ALL = listOf(MACE, BOW, STAFF)
    }
}
