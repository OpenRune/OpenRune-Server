package org.rsmod.content.skills.magic.spell.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpPlayerT
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SpellTeleportScript
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
    private val access: ProtectedAccessLauncher,
    private val players: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (teleport in SpellTeleport.entries) {
            val spell = teleport.resolveSpell() ?: continue
            if (teleport.name.startsWith("Teleother")) {
                onOpPlayerT(RSCM.getReverseMapping(RSCMType.COMPONENT, spell.component.packed)) { event ->
                    if (actionDelay <= mapClock && event.target !== player && canTeleport()) {
                        if (offerTeleport(event.target, teleport, requireAdjacent = false)) actionDelay = mapClock + TeleportActionDelay
                        else mes("That player cannot receive a teleport right now. They must enable Accept Aid.")
                    }
                }
            } else {
                onIfOverlayButton(spell.component) { castSpellTeleport(spell, teleport, it.op) }
            }
        }
        onPlayerQueueWithArgs<PendingSpellTeleport>(TeleportQueue) {
            processQueuedTeleport(it.args)
        }
    }

    private fun SpellTeleport.resolveSpell(): MagicSpell? {
        val spellObj = ServerCacheManager.getItem(spellObj.asRSCM(RSCMType.OBJ)) ?: return null
        return spells.getObjSpell(spellObj)
    }

    private suspend fun ProtectedAccess.castSpellTeleport(
        spell: MagicSpell,
        teleport: SpellTeleport,
        op: IfButtonOp,
    ) {
        if (actionDelay > mapClock) {
            return
        }

        val option = teleport.option(op)
        val destination = teleport.destination(spell, option)
        if (destination == null) {
            mes(option.missingDestinationMessage)
            return
        }

        if (!canTeleport()) {
            return
        }

        if (teleport.name.startsWith("Group")) {
            for (target in players) if (target !== player) offerTeleport(target, teleport, requireAdjacent = true)
        }
        startTeleport(teleport, destination)
    }

    private fun ProtectedAccess.offerTeleport(target: Player, teleport: SpellTeleport, requireAdjacent: Boolean): Boolean {
        val origin = coords
        if (target.vars["varbit.option_acceptaid"] == 0 || target.pendingLogout || target.hitpoints == 0 || target.coords.level != origin.level || target.coords.chebyshevDistance(origin) > if (requireAdjacent) 1 else 10) return false
        val spell = teleport.resolveSpell() ?: return false
        val destination = teleport.destination(spell, teleport.option(IfButtonOp.Op1)) ?: return false
        val offeredTile = target.coords
        val expires = mapClock + 20
        return access.launch(target) {
            if (!canTeleport()) return@launch
            mesbox("${spell.name}: accept this teleport?")
            val accepted = choice2("Accept", true, "Decline", false)
            ifCloseChat()
            if (!accepted) return@launch
            if (mapClock > expires || player.pendingLogout || player.hitpoints == 0 || player.vars["varbit.option_acceptaid"] == 0 || coords != offeredTile || !canTeleport()) return@launch
            startTeleport(teleport, destination)
        }
    }

    private fun ProtectedAccess.startTeleport(teleport: SpellTeleport, destination: CoordGrid) {
        beginTeleport()
        val style = teleport.style
        actionDelay = mapClock + TeleportActionDelay
        resetAnim()
        resetSpotanim()
        anim(style.startAnim)
        spotanim(style.spotanim, height = style.spotanimHeight)
        soundSynth(TeleportSound)
        clearQueue(TeleportQueue)
        queue(TeleportQueue, TeleportDelay, PendingSpellTeleport(teleport, destination.packed))
    }

    private fun ProtectedAccess.processQueuedTeleport(task: PendingSpellTeleport) {
        // The outgoing sequences contain invisible/shrunken frames. Stop them even when
        // a late restriction cancels travel; map rebuilds do not reset client animations.
        resetSpotanim()
        if (player.hitpoints == 0) return
        resetAnim()
        if (player.pendingLogout || !canTeleport()) return
        telejump(CoordGrid(task.destination), onArrival = {
            resetAnim(); resetSpotanim()
            task.teleport.style.endAnim?.let { anim(it) }
        })
    }

    private fun ProtectedAccess.canTeleport(): Boolean {
        val denial =
            teleportValidator.validate(player, TeleportType.Standard, areaChecker)
        if (denial == null) {
            return true
        }
        player.clearMapFlag()
        player.mes(denial, ChatType.Engine)
        return false
    }

    private data class PendingSpellTeleport(
        val teleport: SpellTeleport,
        val destination: Int,
    )

    private enum class SpellTeleport(
        val spellObj: String,
        val destination: CoordGrid? = null,
        val destinationLevel: Int? = null,
        val alternate: TeleportOption? = null,
        val missingDestinationMessage: String = "That teleport is not implemented yet.",
        val style: TeleportStyle = TeleportStyle.Standard,
    ) {
        Home(
            "obj.48_home_teleport",
            CoordGrid(3222, 3222, 0),
        ),
        Varrock(
            "obj.25_varrock_teleport",
            alternate = TeleportOption(destination = CoordGrid(3164, 3487, 0)),
        ),
        Lumbridge(
            "obj.31_lumbridge_teleport",
        ),
        Falador(
            "obj.37_falador_teleport",
        ),
        TeleportToHouse(
            "obj.67_house_teleport",
            alternate =
                TeleportOption(
                    missingDestinationMessage =
                        "You need to purchase a house before you can teleport outside it."
                ),
            missingDestinationMessage = "You need to purchase a house before you can use this spell.",
        ),
        Camelot(
            "obj.45_camelot_teleport",
            alternate = TeleportOption(destination = CoordGrid(2725, 3485, 0)),
        ),
        KourendCastle(
            // Cache name is wrong, but its spell params match Kourend Castle Teleport.
            "obj.cert_deadman_level99_lamp",
        ),
        Ardougne(
            "obj.51_ardougne_teleport",
        ),
        CivitasIllaFortis(
            // Cache name is wrong, but its spell params match Civitas illa Fortis Teleport.
            "obj.placeholder_blighted_sack_snare",
        ),
        Watchtower(
            "obj.58_watchtower_teleport",
            destination = CoordGrid(2549, 3112, 2),
            alternate = TeleportOption(destination = CoordGrid(2544, 3095, 0)),
        ),
        Trollheim(
            "obj.61_trollheim_teleport",
        ),
        ApeAtoll(
            "obj.64_ape_atoll_teleport",
            destinationLevel = 1,
        ),
        TeleportBoatToMe(
            "obj.56_teleport_boat_to_me",
            missingDestinationMessage = "Boat teleports need boat-location support before they can be cast.",
        ),
        TeleportMeToBoat(
            "obj.67_teleport_me_to_boat",
            alternate =
                TeleportOption(
                    missingDestinationMessage =
                        "Last boat teleports need boat-location support before they can be cast."
                ),
            missingDestinationMessage = "Boat teleports need boat-location support before they can be cast.",
        ),
        EdgevilleHome("obj.01_zaros_home_tele", CoordGrid(3087, 3496, 0), style = TeleportStyle.Ancient),
        Paddewwa("obj.54_paddewwa_teleport", style = TeleportStyle.Ancient),
        Senntisten("obj.60_senntisten_teleport", style = TeleportStyle.Ancient),
        Kharyrll("obj.66_kharyllyl_teleport", style = TeleportStyle.Ancient),
        Lassar("obj.72_lassar_teleport", style = TeleportStyle.Ancient),
        Dareeyak("obj.78_dareeyak_teleport", style = TeleportStyle.Ancient),
        Carrallanger("obj.84_carrallagar_teleport", style = TeleportStyle.Ancient),
        Annakarl("obj.90_annakarl_teleport", style = TeleportStyle.Ancient),
        Ghorrock("obj.96_ghorrock_teleport", style = TeleportStyle.Ancient),
        LunarHome("obj.01_lunar_home_tele", CoordGrid(2114, 3915), style = TeleportStyle.Lunar),
        Moonclan("obj.69_tele_moonclan", style = TeleportStyle.Lunar),
        GroupMoonclan("obj.70_tele_moonclan_group", CoordGrid(2114, 3915), style = TeleportStyle.Lunar),
        Ourania("obj.71_tele_zmialtar", style = TeleportStyle.Lunar),
        Waterbirth("obj.72_tele_waterbirth", style = TeleportStyle.Lunar),
        GroupWaterbirth("obj.73_tele_waterbirth_group", CoordGrid(2546, 3756), style = TeleportStyle.Lunar),
        Barbarian("obj.75_tele_barb_outpost", style = TeleportStyle.Lunar),
        GroupBarbarian("obj.76_tele_barb_outpost_group", CoordGrid(2543, 3569), style = TeleportStyle.Lunar),
        Khazard("obj.78_tele_port_khazard", style = TeleportStyle.Lunar),
        GroupKhazard("obj.79_tele_port_khazard_group", CoordGrid(2636, 3167), style = TeleportStyle.Lunar),
        FishingGuild("obj.85_tele_fish_guild", style = TeleportStyle.Lunar),
        GroupFishingGuild("obj.86_tele_fish_guild_group", CoordGrid(2611, 3391), style = TeleportStyle.Lunar),
        Catherby("obj.87_tele_catherby", style = TeleportStyle.Lunar),
        GroupCatherby("obj.88_tele_catherby_group", CoordGrid(2801, 3449), style = TeleportStyle.Lunar),
        IcePlateau("obj.89_tele_ghorrock", style = TeleportStyle.Lunar),
        GroupIcePlateau("obj.90_tele_ghorrock_group", CoordGrid(2974, 3938), style = TeleportStyle.Lunar),
        ArceuusHome("obj.deadman_level99_lamp", CoordGrid(1699, 3882), style = TeleportStyle.Arceuus),
        ArceuusLibrary("obj.br_mithril_platebody", style = TeleportStyle.Arceuus),
        DraynorManor("obj.br_mithril_platelegs", style = TeleportStyle.Arceuus),
        Battlefront("obj.23_teleport_battlefront", style = TeleportStyle.Arceuus),
        MindAltar("obj.br_greendhide_body", style = TeleportStyle.Arceuus),
        Respawn("obj.poh_guide_guildtrophy", CoordGrid(3221, 3218), style = TeleportStyle.Arceuus),
        SalveGraveyard("obj.br_greendhide_chaps", style = TeleportStyle.Arceuus),
        Fenkenstrain("obj.br_moonclan_body", style = TeleportStyle.Arceuus),
        WestArdougne("obj.br_moonclan_legs", style = TeleportStyle.Arceuus),
        HarmonyIsland("obj.br_xeric_body", style = TeleportStyle.Arceuus),
        Cemetery("obj.br_xeric_legs", style = TeleportStyle.Arceuus),
        Barrows("obj.br_air_staff", style = TeleportStyle.Arceuus),
        ArceuusApeAtoll("obj.br_dragon_helm", style = TeleportStyle.Arceuus),
        TeleotherLumbridge("obj.74_teleother_lumbridge", CoordGrid(3221, 3218)),
        TeleotherFalador("obj.82_teleother_falador", CoordGrid(2965, 3385)),
        TeleotherCamelot("obj.90_teleother_camelot", CoordGrid(2757, 3478));

        fun option(op: IfButtonOp): TeleportOption {
            return if (op == IfButtonOp.Op2 && alternate != null) {
                alternate
            } else {
                TeleportOption(destination, destinationLevel, missingDestinationMessage)
            }
        }

        fun destination(spell: MagicSpell, option: TeleportOption): CoordGrid? {
            val coord = option.destination ?: destination ?: spell.obj.paramOrNull(params.spell_telecoord)
            val level = option.destinationLevel ?: destinationLevel
            return if (coord != null && level != null) {
                coord.copy(level = level)
            } else {
                coord
            }
        }
    }

    private data class TeleportOption(
        val destination: CoordGrid? = null,
        val destinationLevel: Int? = null,
        val missingDestinationMessage: String = "That teleport is not implemented yet.",
    )

    private enum class TeleportStyle(
        val startAnim: String,
        val endAnim: String?,
        val spotanim: String,
        val spotanimHeight: Int,
    ) {
        Standard(
            startAnim = RSCM.getReverseMapping(RSCMType.SEQ, 714),
            endAnim = RSCM.getReverseMapping(RSCMType.SEQ, 715),
            spotanim = RSCM.getReverseMapping(RSCMType.SPOTANIM, 111),
            spotanimHeight = 92,
        ),
        Lunar(
            startAnim = "seq.lunar_teleport",
            endAnim = null,
            spotanim = "spotanim.lunar_teleport_spotanim",
            spotanimHeight = 0,
        ),
        Arceuus(
            startAnim = "seq.human_spellcast_arceuus_teleport",
            endAnim = null,
            spotanim = "spotanim.arceuus_teleport_spotanim",
            spotanimHeight = 0,
        ),
        Ancient(
            startAnim = "seq.zaros_vertical_casting",
            endAnim = null,
            spotanim = "spotanim.zaros_teleport",
            spotanimHeight = 0,
        ),
    }

    private companion object {
        private const val TeleportQueue = "queue.spell_teleport"
        private const val TeleportSound = "synth.teleport_all"
        private const val TeleportDelay = 4
        private const val TeleportActionDelay = 5
    }
}
