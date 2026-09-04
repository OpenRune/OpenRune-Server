package org.rsmod.content.other.special.attacks

import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.content.other.special.attacks.boost.DragonBattleaxeSpecialAttack
import org.rsmod.content.other.special.attacks.boost.StatBoostSpecialAttacks
import org.rsmod.content.other.special.attacks.magic.NightmareStaffSpecialAttacks
import org.rsmod.content.other.special.attacks.magic.StaffOfTheDeadSpecialAttack
import org.rsmod.content.other.special.attacks.melee.AbyssalBludgeonSpecialAttack
import org.rsmod.content.other.special.attacks.melee.AbyssalDaggerSpecialAttack
import org.rsmod.content.other.special.attacks.melee.AbyssalWhipSpecialAttack
import org.rsmod.content.other.special.attacks.melee.AncientGodswordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.AncientMaceSpecialAttack
import org.rsmod.content.other.special.attacks.melee.ArkanBladeSpecialAttack
import org.rsmod.content.other.special.attacks.melee.ArmadylGodswordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.BarrelchestAnchorSpecialAttack
import org.rsmod.content.other.special.attacks.melee.BlueMoonSpearSpecialAttack
import org.rsmod.content.other.special.attacks.melee.BoneDaggerSpecialAttack
import org.rsmod.content.other.special.attacks.melee.BrineSabreSpecialAttack
import org.rsmod.content.other.special.attacks.melee.BurningClawsSpecialAttack
import org.rsmod.content.other.special.attacks.melee.CrimsonKistenSpecialAttack
import org.rsmod.content.other.special.attacks.melee.CrystalHalberdSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DemonbaneSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.DinhsBulwarkSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DogswordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.Dragon2hSwordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonCandleDaggerSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonClawsSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonDaggerSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonHalberdSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonHastaSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonLongswordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonMaceSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonScimitarSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonSwordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DualMacuahuitlSpecialAttack
import org.rsmod.content.other.special.attacks.melee.ElderMaulSpecialAttack
import org.rsmod.content.other.special.attacks.melee.FangOfTheHoundSpecialAttack
import org.rsmod.content.other.special.attacks.melee.GraniteHammerSpecialAttack
import org.rsmod.content.other.special.attacks.melee.GraniteMaulSpecialAttack
import org.rsmod.content.other.special.attacks.melee.ImpactMeleeSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.InfernalTecpatlSpecialAttack
import org.rsmod.content.other.special.attacks.melee.NoxiousHalberdSpecialAttack
import org.rsmod.content.other.special.attacks.melee.OsmumtenFangSpecialAttack
import org.rsmod.content.other.special.attacks.melee.RuneClawsSpecialAttack
import org.rsmod.content.other.special.attacks.melee.SaradominBlessedSwordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.SaradominSwordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.SoulreaperAxeSpecialAttack
import org.rsmod.content.other.special.attacks.melee.StatiusWarhammerSpecialAttack
import org.rsmod.content.other.special.attacks.melee.SunspearSpecialAttack
import org.rsmod.content.other.special.attacks.melee.ThunderKhopeshSpecialAttack
import org.rsmod.content.other.special.attacks.melee.VampyreFlailSpecialAttack
import org.rsmod.content.other.special.attacks.melee.VestaLongswordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.VestaSpearSpecialAttack
import org.rsmod.content.other.special.attacks.melee.VoidwakerSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.ArmadylCrossbowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.BallistaSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.DarkBowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.DorgeshuunCrossbowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.DragonCrossbowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.DragonKnifeSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.DragonThrownaxeSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.EclipseAtlatlSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.MagicBowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.MagicShortbowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.MorrigansJavelinSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.MorrigansThrowingAxeSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.RosewoodBlowpipeSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.RuneThrownaxeSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.SeercullSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.TonalzticsOfRalosSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.ToxicBlowpipeSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.WebweaverBowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.ZaryteCrossbowSpecialAttack
import org.rsmod.plugin.module.PluginModule

// Weapons that need the still-undecided engine-diff-scale subsystems (poison/burn/venom
// services, shove-stun/bind, blowpipe ammo tracking, PvP area attacks, a few extra magic/hybrid
// roll variants) are parked in disabled-tier-b/.
class SpecialAttackModule : PluginModule() {
    override fun bind() {
        addSetBinding<SpecialAttackMap>(DragonBattleaxeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(StatBoostSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(NightmareStaffSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(StaffOfTheDeadSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(AbyssalBludgeonSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(AbyssalDaggerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(AbyssalWhipSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(AncientGodswordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(AncientMaceSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(ArkanBladeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(ArmadylGodswordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(BarrelchestAnchorSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(BlueMoonSpearSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(BoneDaggerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(BrineSabreSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(BurningClawsSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(CrimsonKistenSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(CrystalHalberdSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DemonbaneSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DinhsBulwarkSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DogswordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(Dragon2hSwordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonCandleDaggerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonClawsSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonDaggerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonHalberdSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonHastaSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonLongswordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonMaceSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonScimitarSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonSwordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DualMacuahuitlSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(ElderMaulSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(FangOfTheHoundSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(GraniteHammerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(GraniteMaulSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(ImpactMeleeSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(InfernalTecpatlSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(NoxiousHalberdSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(OsmumtenFangSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(RuneClawsSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(SaradominBlessedSwordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(SaradominSwordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(SoulreaperAxeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(StatiusWarhammerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(SunspearSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(ThunderKhopeshSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(VampyreFlailSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(VestaLongswordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(VestaSpearSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(VoidwakerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(ArmadylCrossbowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(BallistaSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DarkBowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DorgeshuunCrossbowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonCrossbowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonKnifeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonThrownaxeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(EclipseAtlatlSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(MagicBowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(MagicShortbowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(MorrigansJavelinSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(MorrigansThrowingAxeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(RosewoodBlowpipeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(RuneThrownaxeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(SeercullSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(TonalzticsOfRalosSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(ToxicBlowpipeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(WebweaverBowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(ZaryteCrossbowSpecialAttack::class.java)
    }
}
