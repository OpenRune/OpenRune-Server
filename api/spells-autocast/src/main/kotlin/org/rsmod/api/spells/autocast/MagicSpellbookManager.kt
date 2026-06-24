package org.rsmod.api.spells.autocast

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

@Singleton
public class MagicSpellbookManager
@Inject
constructor(private val autocast: AutocastWeapons) {
    private var Player.spellbook by enumVarBit<Spellbook>("varbit.spellbook")
    private var Player.autocastEnabled by boolVarBit("varbit.autocast_set")
    private var Player.autocastSpell by intVarBit("varbit.autocast_spell")
    private var Player.defensiveCasting by boolVarBit("varbit.autocast_defmode")

    public fun activeSpellbook(player: Player): Spellbook = player.spellbook

    public fun setSpellbook(player: Player, spellbook: Spellbook): ChangeResult {
        val previous = player.spellbook
        if (previous == spellbook) {
            return ChangeResult.Unchanged(previous)
        }

        player.spellbook = spellbook
        clearAutocast(player)
        return ChangeResult.Changed(previous, spellbook)
    }

    public fun clearAutocast(player: Player) {
        val weaponType = getOrNull(player.righthand)
        if (weaponType != null) {
            autocast.reset(player, weaponType)
        }
        player.autocastEnabled = false
        player.autocastSpell = 0
        player.defensiveCasting = false
    }

    public sealed class ChangeResult {
        public data class Changed(
            public val previous: Spellbook,
            public val current: Spellbook,
        ) : ChangeResult()

        public data class Unchanged(public val current: Spellbook) : ChangeResult()
    }
}
