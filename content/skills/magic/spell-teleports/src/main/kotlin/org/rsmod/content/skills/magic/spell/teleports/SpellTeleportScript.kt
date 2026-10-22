package org.rsmod.content.skills.magic.spell.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.inv.isType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SpellTeleportScript
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
    private val config: ServerConfig,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (teleport in SpellTeleport.entries) {
            val spell = teleport.resolveSpell() ?: continue
            onIfOverlayButton(spell.component) { castSpellTeleport(spell, teleport, it.op) }
        }
        onPlayerQueueWithArgs<PendingSpellTeleport>(TeleportQueue) {
            processQueuedTeleport(it.args)
        }
        onPlayerQueueWithArgs<PendingHomeTeleport>(HomeTeleportQueue) {
            processHomeTeleport(it.args)
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

        if (!teleport.canCast(this)) {
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

        if (teleport.homeTeleport) {
            castHomeTeleport(teleport, destination)
            return
        }

        if (!consumeRequirements(spell, teleport)) {
            return
        }

        val style = teleport.style
        actionDelay = mapClock + TeleportActionDelay
        anim(style.startAnim)
        spotanim(style.spotanim, height = style.spotanimHeight)
        soundSynth(TeleportSound)
        clearQueue(TeleportQueue)
        queue(TeleportQueue, TeleportDelay, PendingSpellTeleport(teleport, destination.packed))
    }

    private fun ProtectedAccess.processQueuedTeleport(task: PendingSpellTeleport) {
        val spell = task.teleport.resolveSpell() ?: return
        if (!canTeleport()) {
            return
        }
        telejump(landingSquare(CoordGrid(task.destination), task.teleport.landingRadius))
        val endAnim = task.teleport.style.endAnim
        if (endAnim != null) anim(endAnim) else resetAnim()
        statAdvance("stat.magic", spell.castXp)
    }

    private fun ProtectedAccess.landingSquare(centre: CoordGrid, radius: Int): CoordGrid {
        if (radius == 0) {
            return centre
        }
        return mapFindSquareLineOfWalk(centre, minRadius = 0, maxRadius = radius) ?: centre
    }

    private fun ProtectedAccess.castHomeTeleport(teleport: SpellTeleport, destination: CoordGrid) {
        // UNVERIFIED: the in-combat refusal shares the moving refusal's message (wiki description)
        if (player.routeDestination.isNotEmpty() || isInCombat()) {
            mes("You can't use that teleport at the moment.")
            return
        }
        val cooldown = config.gameplay.homeTeleportCooldown
        val minutesLeft = cooldown - (epochMinute() - vars[HomeTeleportTimer])
        if (minutesLeft > 0) {
            // UNVERIFIED: singular "minute" when one is left (guess)
            val unit = if (minutesLeft == 1) "minute" else "minutes"
            mes("You need to wait another $minutesLeft $unit to cast this spell.")
            return
        }
        player.clearMapFlag()
        resetAnim()
        resetSpotanim()
        walkTrigger(CancelAnimWalkTrigger)
        clearWeakQueue(HomeTeleportQueue)
        val first = HomeTeleportStage.DrawCircle
        weakQueue(
            HomeTeleportQueue,
            first.tick + SameTickQueue,
            PendingHomeTeleport(teleport, destination.packed, stage = first.ordinal),
        )
    }

    private suspend fun ProtectedAccess.processHomeTeleport(task: PendingHomeTeleport) {
        val stage = HomeTeleportStage.entries[task.stage]
        if (stage.spotanim != null) {
            spotanim(stage.spotanim, height = stage.spotanimHeight)
        }
        anim(stage.anim)
        val next = HomeTeleportStage.entries.getOrNull(stage.ordinal + 1)
        if (next != null) {
            val cycles = next.tick - stage.tick + SameTickQueue
            weakQueue(HomeTeleportQueue, cycles, task.copy(stage = next.ordinal))
            return
        }

        vars[Busy] = 1
        delay(HomeTeleportLandingTick - stage.tick)
        player.clearWalkTrigger()
        val allowed = canTeleport()
        if (allowed) {
            vars[HomeTeleportTimer] = epochMinute()
        }
        vars[Busy] = 0
        if (allowed) {
            telejump(landingSquare(CoordGrid(task.destination), task.teleport.landingRadius))
        }
        resetAnim()
        resetSpotanim()
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

    private fun ProtectedAccess.consumeRequirements(
        spell: MagicSpell,
        teleport: SpellTeleport,
    ): Boolean {
        val castSpell =
            if (teleport == SpellTeleport.ApeAtoll) {
                val banana = ServerCacheManager.getItem(Banana.asRSCM(RSCMType.OBJ)) ?: return false
                val spellWithoutBanana = spell.copy(objReqs = spell.objReqs.withoutBananaReq())
                if (!runes.canCastSpell(player, spellWithoutBanana)) {
                    return false
                }
                if (!deleteBanana(banana)) {
                    mes("You need a banana to cast this spell.")
                    return false
                }
                spellWithoutBanana
            } else {
                spell
            }

        if (castSpell.objReqs.isEmpty()) {
            return runes.canCastSpell(player, castSpell)
        }
        return !runes.attemptCast(player, castSpell).isFailure()
    }

    private fun List<MagicSpell.ObjRequirement>.withoutBananaReq(): List<MagicSpell.ObjRequirement> {
        return filterNot { RSCM.getReverseMapping(RSCMType.OBJ, it.obj.id).contains("banana") }
    }

    private fun ProtectedAccess.deleteBanana(banana: ItemServerType): Boolean {
        val bananaSlot = inv.indexOfFirst { it.isType(banana) }
        if (bananaSlot == -1) {
            return false
        }
        val transaction =
            player.invTransaction(inv, autoCommit = true) {
                val targetInv = select(inv)
                delete {
                    from = targetInv
                    obj = banana.id
                    strictCount = 1
                    strictSlot = bananaSlot
                }
            }
        return transaction.success
    }

    private data class PendingSpellTeleport(
        val teleport: SpellTeleport,
        val destination: Int,
    )

    private data class PendingHomeTeleport(
        val teleport: SpellTeleport,
        val destination: Int,
        val stage: Int,
    )

    private enum class HomeTeleportStage(
        val anim: String,
        val spotanim: String?,
        val tick: Int,
        val spotanimHeight: Int = 0,
    ) {
        DrawCircle("seq.aide_drawing_chalk_circle", "spotanim.aide_chalk_circle", tick = 1),
        SitDown("seq.aide_sitting_down_crosslegged", spotanim = null, tick = 7),
        GetBook("seq.aide_player_getting_book", "spotanim.aide_player_book_get", tick = 13),
        Recite(
            "seq.aide_reciting_incantation",
            "spotanim.aide_player_book_portal",
            tick = 17,
            spotanimHeight = 65526,
        ),
        Teleport("seq.aide_player_teleporting", "spotanim.aide_player_teleport", tick = 21),
    }

    private enum class SpellTeleport(
        val spellObj: String,
        val destination: CoordGrid? = null,
        val destinationLevel: Int? = null,
        val alternate: TeleportOption? = null,
        val requiredQuest: String? = null,
        val lockedMessage: String = "You need to complete the required quest to cast this spell.",
        val missingDestinationMessage: String = "That teleport is not implemented yet.",
        val style: TeleportStyle = TeleportStyle.Standard,
        val landingRadius: Int = LandingRadius,
        val homeTeleport: Boolean = false,
    ) {
        Home(
            "obj.48_home_teleport",
            CoordGrid(3221, 3218, 0),
            landingRadius = 1,
            homeTeleport = true,
        ),
        Varrock(
            "obj.25_varrock_teleport",
            alternate = TeleportOption(destination = CoordGrid(3163, 3478, 0)),
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
            alternate = TeleportOption(destination = CoordGrid(2726, 3484, 0)),
        ),
        KourendCastle(
            // Cache name is wrong, but its spell params match Kourend Castle Teleport.
            "obj.cert_deadman_level99_lamp",
            requiredQuest = "quest_clientofkourend",
            lockedMessage = "You need to complete Client of Kourend to cast this spell.",
        ),
        Ardougne(
            "obj.51_ardougne_teleport",
            requiredQuest = "quest_plaguecity",
            lockedMessage = "You need to complete Plague City to cast this spell.",
        ),
        CivitasIllaFortis(
            // Cache name is wrong, but its spell params match Civitas illa Fortis Teleport.
            "obj.placeholder_blighted_sack_snare",
            requiredQuest = "quest_twilightspromise",
            lockedMessage = "You need to complete Twilight's Promise to cast this spell.",
        ),
        Watchtower(
            "obj.58_watchtower_teleport",
            alternate = TeleportOption(destination = CoordGrid(2544, 3095, 0)),
            requiredQuest = "quest_watchtowerquest",
            lockedMessage = "You need to complete Watchtower to cast this spell.",
        ),
        Trollheim(
            "obj.61_trollheim_teleport",
            requiredQuest = "quest_eadgarsruse",
            lockedMessage = "You need to complete Eadgar's Ruse to cast this spell.",
        ),
        ApeAtoll(
            "obj.64_ape_atoll_teleport",
            destinationLevel = 1,
        ),
        TeleportBoatToMe(
            "obj.56_teleport_boat_to_me",
            requiredQuest = "quest_pandemonium",
            lockedMessage = "You need to complete Pandemonium to cast this spell.",
            missingDestinationMessage = "Boat teleports need boat-location support before they can be cast.",
        ),
        TeleportMeToBoat(
            "obj.67_teleport_me_to_boat",
            alternate =
                TeleportOption(
                    missingDestinationMessage =
                        "Last boat teleports need boat-location support before they can be cast."
                ),
            requiredQuest = "quest_pandemonium",
            lockedMessage = "You need to complete Pandemonium to cast this spell.",
            missingDestinationMessage = "Boat teleports need boat-location support before they can be cast.",
        ),

        // UNVERIFIED: Edgeville and Arceuus home teleports use the Lumbridge cast (wiki description)
        EdgevilleHome(
            "obj.01_zaros_home_tele",
            CoordGrid(3087, 3496, 0),
            landingRadius = 0,
            homeTeleport = true,
        ),
        Paddewwa("obj.54_paddewwa_teleport", style = TeleportStyle.Ancient),
        Senntisten("obj.60_senntisten_teleport", style = TeleportStyle.Ancient),
        Kharyrll("obj.66_kharyllyl_teleport", style = TeleportStyle.Ancient),
        Lassar("obj.72_lassar_teleport", style = TeleportStyle.Ancient),
        Dareeyak("obj.78_dareeyak_teleport", style = TeleportStyle.Ancient),
        Carrallanger("obj.84_carrallagar_teleport", style = TeleportStyle.Ancient),
        Annakarl("obj.90_annakarl_teleport", style = TeleportStyle.Ancient),
        Ghorrock("obj.96_ghorrock_teleport", style = TeleportStyle.Ancient),
        ArceuusHome(
            "obj.deadman_level99_lamp",
            CoordGrid(1699, 3879, 0),
            landingRadius = 0,
            homeTeleport = true,
        ),
        ArceuusLibrary("obj.br_mithril_platebody", style = TeleportStyle.Arceuus),
        DraynorManor("obj.br_mithril_platelegs", style = TeleportStyle.Arceuus),
        Battlefront("obj.23_teleport_battlefront", style = TeleportStyle.Arceuus),
        MindAltar("obj.br_greendhide_body", style = TeleportStyle.Arceuus),
        Respawn(
            "obj.poh_guide_guildtrophy",
            CoordGrid(3221, 3218, 0),
            style = TeleportStyle.Arceuus,
        ),
        SalveGraveyard(
            "obj.br_greendhide_chaps",
            requiredQuest = "quest_priestinperil",
            lockedMessage = "You need to complete Priest in Peril to cast this spell.",
            style = TeleportStyle.Arceuus,
        ),
        FenkenstrainsCastle(
            "obj.br_moonclan_body",
            requiredQuest = "quest_priestinperil",
            lockedMessage = "You need to complete Priest in Peril to cast this spell.",
            style = TeleportStyle.Arceuus,
        ),
        WestArdougne(
            "obj.br_moonclan_legs",
            requiredQuest = "quest_biohazard",
            lockedMessage = "You need to complete Biohazard to cast this spell.",
            style = TeleportStyle.Arceuus,
        ),
        HarmonyIsland(
            "obj.br_xeric_body",
            requiredQuest = "quest_greatbrainrobbery",
            lockedMessage = "You need to complete The Great Brain Robbery to cast this spell.",
            style = TeleportStyle.Arceuus,
        ),
        Cemetery("obj.br_xeric_legs", style = TeleportStyle.Arceuus),
        Barrows(
            "obj.br_air_staff",
            requiredQuest = "quest_priestinperil",
            lockedMessage = "You need to complete Priest in Peril to cast this spell.",
            style = TeleportStyle.Arceuus,
        ),
        ArceuusApeAtoll(
            "obj.br_dragon_helm",
            requiredQuest = "quest_monkeymadness1",
            lockedMessage = "You need to complete Monkey Madness I to cast this spell.",
            style = TeleportStyle.Arceuus,
        );

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

        fun canCast(access: ProtectedAccess): Boolean {
            val questKey = requiredQuest ?: return true
            if (QuestRequirements.hasCompleted(access.player, questKey)) {
                return true
            }
            access.mes(lockedMessage)
            return false
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
        Ancient(
            startAnim = "seq.zaros_vertical_casting",
            endAnim = null,
            spotanim = "spotanim.zaros_teleport",
            spotanimHeight = 0,
        ),
        Arceuus(
            startAnim = "seq.arceuus_necromancy_anim",
            endAnim = null,
            spotanim = "spotanim.arceuus_teleport_spotanim",
            spotanimHeight = 0,
        ),
    }

    private companion object {
        private const val Banana = "obj.banana"
        private const val TeleportQueue = "queue.spell_teleport"
        private const val TeleportSound = "synth.teleport_all"
        private const val TeleportDelay = 4
        private const val TeleportActionDelay = 5

        private const val HomeTeleportQueue = "queue.home_teleport"
        private const val HomeTeleportTimer = "varp.aide_tele_timer"
        private const val HomeTeleportLandingTick = 24

        // A weak queue added before or during queue processing is decremented on that same tick.
        private const val SameTickQueue = 1
        private const val CancelAnimWalkTrigger = "walktrigger.emote_cancelanim"
        private const val Busy = "varbit.busy"

        // UNVERIFIED: radius for Ancient and Arceuus teleports but Paddewwa, Draynor Manor (guess)
        private const val LandingRadius = 2
    }
}
