package org.rsmod.content.skills.magic.arceuus

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType

internal enum class ReanimationSpell(val obj: String, val component: String) {
    Basic("obj.easter_helper", "component.magic_spellbook:reanimation_basic"),
    Adept("obj.cert_chompy_bird_obj", "component.magic_spellbook:reanimation_adept"),
    Expert("obj.evil_bob_amulet_of_manspeak", "component.magic_spellbook:reanimation_expert"),
    Master("obj.magictraining_guardianstatue", "component.magic_spellbook:reanimation_master"),
}

internal enum class ReanimatedHead(
    val spell: ReanimationSpell,
    private val slug: String,
    val prayerXp: Double,
) {
    Goblin(ReanimationSpell.Basic, "goblin", 130.0),
    Monkey(ReanimationSpell.Basic, "monkey", 182.0),
    Imp(ReanimationSpell.Basic, "imp", 286.0),
    Minotaur(ReanimationSpell.Basic, "minotaur", 364.0),
    Scorpion(ReanimationSpell.Basic, "scorpion", 454.0),
    Bear(ReanimationSpell.Basic, "bear", 480.0),
    Unicorn(ReanimationSpell.Basic, "unicorn", 494.0),
    Dog(ReanimationSpell.Adept, "dog", 520.0),
    ChaosDruid(ReanimationSpell.Adept, "chaosdruid", 584.0),
    Giant(ReanimationSpell.Adept, "giant", 650.0),
    Ogre(ReanimationSpell.Adept, "ogre", 716.0),
    Elf(ReanimationSpell.Adept, "elf", 754.0),
    Troll(ReanimationSpell.Adept, "troll", 780.0),
    Horror(ReanimationSpell.Adept, "horror", 832.0),
    Kalphite(ReanimationSpell.Expert, "kalphite", 884.0),
    Dagannoth(ReanimationSpell.Expert, "dagannoth", 936.0),
    Bloodveld(ReanimationSpell.Expert, "bloodveld", 1040.0),
    Tzhaar(ReanimationSpell.Expert, "tzhaar", 1104.0),
    Demon(ReanimationSpell.Expert, "demon", 1170.0),
    Hellhound(ReanimationSpell.Expert, "hellhound", 1200.0),
    Aviansie(ReanimationSpell.Master, "aviansie", 1234.0),
    Abyssal(ReanimationSpell.Master, "abyssal", 1300.0),
    Dragon(ReanimationSpell.Master, "dragon", 1560.0);

    val corpseObj: String
        get() = "obj.arceuus_corpse_$slug"

    val initialCorpseObj: String
        get() = "obj.arceuus_corpse_${slug}_initial"

    val npc: String
        get() = "npc.arceuus_reanimated_$slug"

    val npcId: Int by lazy { npc.asRSCM(RSCMType.NPC) }

    companion object {
        val byNpcId: Map<Int, ReanimatedHead> by lazy { entries.associateBy { it.npcId } }
    }
}
