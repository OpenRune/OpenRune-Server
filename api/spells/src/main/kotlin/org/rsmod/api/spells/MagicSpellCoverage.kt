package org.rsmod.api.spells

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.spells.attack.SpellAttackRegistry

@Singleton
public class MagicSpellCoverage
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val attacks: SpellAttackRegistry,
) {
    public fun snapshot(): Snapshot {
        val autocastIds = spells.autocastSpells().entries.associate { it.value.obj.id to it.key }
        val entries =
            spells.allSpells()
                .map { spell ->
                    Entry(
                        spell = spell,
                        autocastId = autocastIds[spell.obj.id],
                        hasSpellAttack = spell in attacks,
                    )
                }
                .sortedWith(compareBy<Entry> { it.spell.levelReq }.thenBy { it.spell.obj.id })

        return Snapshot(entries)
    }

    public data class Snapshot(public val entries: List<Entry>) {
        public val combatSpells: List<Entry> = entries.filter { it.spell.type == MagicSpellType.Combat }

        public val autocastSpells: List<Entry> = entries.filter { it.autocastId != null }

        public val missingCombatAttacks: List<Entry> =
            combatSpells.filterNot { it.hasSpellAttack }

        public val missingAutocastAttacks: List<Entry> =
            autocastSpells.filterNot { it.hasSpellAttack }
    }

    public data class Entry(
        public val spell: MagicSpell,
        public val autocastId: Int?,
        public val hasSpellAttack: Boolean,
    )
}
