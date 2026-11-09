package org.rsmod.content.skills.magic.spell.teleports

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TabletTeleportScript
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (tablet in TABLETS) {
            onOpHeld1(tablet) { breakTablet(it.type, it.slot) }
        }
        for (tablet in NAMED_DESTINATION_TABLETS) {
            onOpHeld2(tablet) { breakTablet(it.type, it.slot) }
        }
        // TODO(content): [diary:varrock] Grand Exchange destination and Toggle need the diary.
        onOpHeld3(VARROCK_TABLET) { mes(VARROCK_DIARY_LOCKED) }
        // TODO(content): [diary:kandarin] Seers' Village destination and Toggle need the diary.
        // TODO(content): [diary:ardougne] Yanille destination and Toggle need the diary.
    }

    suspend fun ProtectedAccess.breakTablet(tablet: ItemServerType, slot: Int) {
        val spellObj = tablet.param(params.spell_parent_obj)
        val spell = checkNotNull(spells.getObjSpell(spellObj)) { "No spell for tablet: $tablet" }
        val quest = spell.questReq
        if (quest != null && !QuestRequirements.hasCompleted(player, quest)) {
            val name = QuestRow.getRow("dbrow.$quest").displayname
            mes("You need to have completed the $name quest to use this spell.")
            return
        }
        if (!canTeleport()) {
            return
        }
        val centre = spellObj.param(params.spell_telecoord)

        vars[BUSY] = 1
        player.clearMapFlag()
        anim(BREAK_ANIM, delay = 16)
        soundSynth(BREAK_SOUND, delay = 15)
        delay(2)
        val type = RSCM.getReverseMapping(RSCMType.OBJ, tablet.id)
        if (invDel(inv, type, count = 1, slot = slot).failure) {
            vars[BUSY] = 0
            resetAnim()
            return
        }
        spotanim(ABSORB_SPOTANIM)
        anim(ABSORB_ANIM)
        delay(2)

        vars[BUSY] = 0
        telejump(landingSquare(centre))
        resetAnim()
    }

    private fun ProtectedAccess.landingSquare(centre: CoordGrid): CoordGrid =
        mapFindSquareLineOfWalk(centre, minRadius = 0, maxRadius = 1) ?: centre

    private fun ProtectedAccess.canTeleport(): Boolean {
        val denial = teleportValidator.validate(player, TeleportType.Standard, areaChecker)
        if (denial == null) {
            return true
        }
        player.clearMapFlag()
        player.mes(denial, ChatType.Engine)
        return false
    }

    companion object {
        const val VARROCK_TABLET = "obj.poh_tablet_varrockteleport"

        val NAMED_DESTINATION_TABLETS =
            listOf(
                VARROCK_TABLET,
                "obj.poh_tablet_camelotteleport",
                "obj.poh_tablet_watchtowerteleport",
            )

        val TABLETS =
            NAMED_DESTINATION_TABLETS +
                listOf(
                    "obj.poh_tablet_lumbridgeteleport",
                    "obj.poh_tablet_faladorteleport",
                    "obj.poh_tablet_kourendteleport",
                    "obj.poh_tablet_ardougneteleport",
                    "obj.poh_tablet_fortisteleport",
                )

        const val VARROCK_DIARY_LOCKED =
            "You must complete the medium, hard or elite tier of the Varrock achievement diary " +
                "to use that option."

        private const val BUSY = "varbit.busy"
        private const val BREAK_ANIM = "seq.poh_smash_magic_tablet"
        private const val BREAK_SOUND = "synth.poh_tablet_break"
        private const val ABSORB_SPOTANIM = "spotanim.poh_absorb_tablet_magic"
        private const val ABSORB_ANIM = "seq.poh_absorb_tablet_teleport"
    }
}
