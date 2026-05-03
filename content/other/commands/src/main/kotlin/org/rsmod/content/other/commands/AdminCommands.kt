package org.rsmod.content.other.commands

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.mechanics.toxins.impl.PlayerDisease
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.invtx.invClear
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.PlayerInterfaceUpdates
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.api.utils.system.SafeServiceExit
import org.rsmod.game.GameUpdate
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.objtx.TransactionResult
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants
import org.simmetrics.metrics.StringMetrics

class AdminCommands
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val playerList: PlayerList,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val update: GameUpdate,
) : PluginScript() {
    private val logger = InlineLogger()

    private val levenshteinMetric = StringMetrics.levenshtein()

    override fun ScriptContext.startup() {
        onCommand("master", "Max out all stats", ::master)
        onCommand("reset", "Reset all stats", ::reset)
        onCommand("mypos", "Get current coordinates", ::mypos)
        onCommand("tele", "Teleport to coordgrid", ::tele) {
            invalidArgs = "Usage: ::tele mx mz [level](e.g. ::tele 3200 3200 0)"
        }
        onCommand("telezone", "Teleport to zone key", ::teleZone) {
            invalidArgs = "Use as ::telezone zoneX zoneY level (ex: 400 400 0)"
        }
        onCommand("anim", "Play animation", ::anim)
        onCommand("spot", "Play spotanim", ::spotanim) {
            invalidArgs = "Use as ::spot spotanimDebugNameOrId (ex: fx_emote_party01_active)"
        }
        onCommand("object", "Spawn loc", ::locAdd) {
            invalidArgs = "Use as ::object duration locDebugNameOrId (ex: 100 bookcase)"
        }

        onCommand("locadd", "Spawn loc", ::locAdd) {
            invalidArgs = "Use as ::locadd duration locDebugNameOrId (ex: 100 bookcase)"
        }
        onCommand("locdel", "Remove loc", ::locDel) { invalidArgs = "Use as ::locdel duration" }
        onCommand("objectdel", "Remove loc", ::locDel) { invalidArgs = "Use as ::objectdel duration" }

        onCommand("npc", "Spawn npc", ::npcAdd) {
            invalidArgs = "Use as ::npc duration npcDebugNameOrId (ex: 100 prison_pete)"
        }

        onCommand("npcadd", "Spawn npc", ::npcAdd) {
            invalidArgs = "Use as ::npcadd duration npcDebugNameOrId (ex: 100 prison_pete)"
        }

        onCommand("invadd", "Spawn obj into inv", ::invAdd)
        onCommand("item", "Spawn obj into inv", ::invAdd)

        onCommand("invclear", "Remove all objs from inv", ::invClear)
        onCommand("varp", "Set varp value", ::setVarp) {
            invalidArgs = "Use as ::varp debugNameOrId value (ex: option_run 1)"
        }
        onCommand("varbit", "Set varbit value", ::setVarBit) {
            invalidArgs = "Use as ::varbit debugNameOrId value (ex: emote_hotline_bling 1)"
        }
        onCommand("reboot", "Reboots the game world, applying packed changes", ::reboot)
        onCommand("slowreboot", "Reboots the game world, with a timer", ::slowReboot)
        onCommand("poison", "Test player poison (wiki initial damage, optional raw severity)", ::poisonTest) {
            invalidArgs = "Use as ::poison initialDamage [severity] (e.g. ::poison 8 or ::poison 0 36)"
        }
        onCommand("venom", "Test player venom (escalating damage timer)", ::venomTest)
        onCommand("venomclear", "Clears Venom", ::venomClear)
        onCommand("disease", "Test disease (drain per tick, default 3)", ::diseaseTest) {
            invalidArgs = "Use as ::disease [drainPerTick] (e.g. ::disease 5)"
        }
        onCommand("diseaseclear", "Clears disease timer (stats recover via normal regen)", ::diseaseClear)
    }

    private fun poisonTest(cheat: Cheat) =
        with(cheat) {
            val initialDamage = args.getOrNull(0)?.toIntOrNull() ?: 0
            val severity = args.getOrNull(1)?.toIntOrNull() ?: 0
            val ok = PlayerPoison.tryPoison(player, initialDamage = initialDamage, severity = severity)
            player.mes(
                if (ok) {
                    "Poison applied (initialDamage=$initialDamage severityParam=$severity)."
                } else {
                    "Poison not applied (weaker/equal than current, or both inputs zero)."
                },
            )
        }

    private fun venomTest(cheat: Cheat) =
        with(cheat) {
            PlayerVenom.tryVenom(player)
        }

    private fun venomClear(cheat: Cheat) =
        with(cheat) {
            PlayerVenom.clear(player)
        }

    private fun diseaseTest(cheat: Cheat) =
        with(cheat) {
            val drain = args.getOrNull(0)?.toIntOrNull() ?: 3
            val ok = PlayerDisease.tryDisease(player, drain)
            player.mes(
                if (ok) {
                    "Disease applied (drain per tick=$drain)."
                } else {
                    "Disease not applied (no eligible skill)."
                },
            )
        }

    private fun diseaseClear(cheat: Cheat) =
        with(cheat) {
            PlayerDisease.clear(player)
            player.mes("Disease cleared.")
        }

    private fun master(cheat: Cheat) = with(cheat) { player.setStatLevels(level = 99) }

    private fun reset(cheat: Cheat) = with(cheat) { player.setStatLevels(level = 1) }

    private fun mypos(cheat: Cheat) =
        with(cheat) {
            player.mes("${player.coords}:")
            player.mes("  ${ZoneKey.from(player.coords)} - ${ZoneGrid.from(player.coords)}")
            player.mes(
                "  ${MapSquareKey.from(player.coords)} - ${MapSquareGrid.from(player.coords)}"
            )
            player.mes("  BuildArea(${player.buildArea})")
        }

    private fun tele(cheat: Cheat) =
        with(cheat) {
            val args = if (args.size == 1) args[0].split(",") else args
            val x = args[0].toInt()
            val y = args[1].toInt()
            val level = args.getOrNull(2)?.toInt() ?: 0
            val coords = CoordGrid(x,y,level)
            protectedAccess.launch(player) {
                player.mes("Teleported to $coords.")
                telejump(coords)
            }
        }

    private fun teleZone(cheat: Cheat) =
        with(cheat) {
            val args = if (args.size == 1) args[0].split(",") else args
            val zoneX = args[0].toInt()
            val zoneZ = args[1].toInt()
            val level = args[2].toInt()
            val coords = ZoneKey(zoneX, zoneZ, level).toCoords()
            protectedAccess.launch(player) {
                player.mes("Teleported to $coords.")
                telejump(coords)
            }
        }

    private fun anim(cheat: Cheat) =
        with(cheat) {
            val typeId = RSCM.getRSCM("seq.${args.asTypeName()}")
            if (typeId == -1) {
                player.mes("There is no seq mapped to: '${args.asTypeName()}'")
                return
            }
            val type = ServerCacheManager.getAnim(typeId)
            if (type == null) {
                player.mes("That seq does not exist: $typeId")
                return
            }
            player.anim("seq.${args.asTypeName()}")
            player.mes("Anim: '${args.asTypeName()}' (priority=${type.priority})")
            logger.debug { "Anim: $type" }
        }

    private fun spotanim(cheat: Cheat) =
        with(cheat) {
            val (typeName, heightArg) = args.asTypeNameAndNumber(defaultNumber = 0)
            val typeId = "spotanim.${typeName}".asRSCM()
            if (typeId == -1) {
                player.mes("There is no spotanim mapped to: '${typeName}'")
                return
            }

            val height = min(heightArg.toInt(), Short.MAX_VALUE.toInt())
            player.spotanim("spotanim.${typeName}", delay = 0, height = height, slot = 0)
            player.mes("Spotanim: '${typeName}' (height=$height)")
            logger.debug { "Spotanim: $typeName" }
        }

    private fun locAdd(cheat: Cheat) =
        with(cheat) {
            val typeId = "objects.${args[1]}".asRSCM()

            val type = ServerCacheManager.getObject(typeId)!!
            if (type == null) {
                player.mes("That loc does not exist: $typeId")
                return
            }
            val duration = args[0].toInt()
            val angle = args.getOrNull(2)?.toInt() ?: LocAngle.West.id
            val shape = args.getOrNull(3)?.toInt() ?: LocShape.CentrepieceStraight.id
            val layer = LocLayerConstants.of(shape)
            val loc = LocInfo(layer, player.coords, LocEntity(type.id, shape, angle))
            locRepo.add(loc, duration)
            player.mes("Spawned loc '${type.internalName}' (duration: $duration cycles)")
            logger.debug { "Spawned loc: loc=$loc, type=$type" }
        }

    private fun locDel(cheat: Cheat) =
        with(cheat) {
            val zone = ZoneKey.from(player.coords)
            val locs = locRepo.findAll(zone).filter { it.coords == player.coords }.toList()
            if (locs.isEmpty()) {
                player.mes("No loc found on ${player.coords}")
                return
            }
            val duration = args[0].toInt()
            val shape = args.getOrNull(1)?.toIntOrNull() ?: LocShape.CentrepieceStraight.id
            val loc = locs.firstOrNull { it.shapeId == shape }
            if (loc == null) {
                player.mes("No loc with shape `${LocShape[shape]}` found on ${player.coords}")
                return
            }
            val type = ServerCacheManager.getObject(loc.id)!!
            locRepo.del(loc, duration)
            player.mes("Deleted loc `${type.internalName}` (duration: $duration cycles)")
            logger.debug { "Deleted loc: loc=$loc, type=$type" }
        }

    private fun npcAdd(cheat: Cheat) =
        with(cheat) {
            val typeId = "npc.${args[1]}".asRSCM()

            val type = ServerCacheManager.getNpc(typeId)
            if (type == null) {
                player.mes("That npc does not exist: $typeId")
                return
            }
            val duration = args[0].toInt()
            val npc = Npc(type, player.coords)
            npc.mode = NpcMode.None
            npcRepo.add(npc, duration)
            player.mes("Spawned npc `${args[1]}` (duration: $duration cycles)")
        }

    private fun invAdd(cheat: Cheat) =
        with(cheat) {
            val (typeName, countArg) = args.asTypeNameAndNumber(defaultNumber = 1)
            val normalizedName = "obj." + typeName.replace("cert_", "")
            val type = ServerCacheManager.getItem(normalizedName.asRSCM(RSCMType.OBJ))?: return@with
            val spawnCert = typeName.startsWith("cert_")
            val resolvedType =
                if (spawnCert && type.canCert) ServerCacheManager.getItem(type.certlink) else type
            val count = countArg.toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val objName = type.name.ifEmpty { normalizedName }
            if (resolvedType == null) {
                player.mes("Unable to find item: $objName")
                return@with
            }
            val spawned = player.invAdd(player.inv, normalizedName, count, strict = false)
            if (spawned.err is TransactionResult.RestrictedDummyitem) {
                player.mes("You can't spawn this item!")
                return
            }
            player.mes("Spawned inv obj `$objName` x ${spawned.completed().formatAmount}")
        }

    private fun invClear(cheat: Cheat) = with(cheat) { player.invClear(player.inv) }

    private fun setVarp(cheat: Cheat) =
        with(cheat) {
            val typeId = "varp.${args[0]}".asRSCM()

            val type = ServerCacheManager.getVarp(typeId)
            if (type == null) {
                player.mes("That varp does not exist: $typeId")
                return
            }
            val value = args[1].toInt()
            player.vars.backing[type.id] = value
            player.resyncVar(type)
            player.mes("Set varp '${args[0]}' to value: ${player.vars[type]}")
        }

    private fun setVarBit(cheat: Cheat) =
        with(cheat) {
            val typeId = "varbits.${args[0]}".asRSCM()

            val type = ServerCacheManager.getVarbit(typeId)
            if (type == null) {
                player.mes("That varbit does not exist: $typeId")
                return
            }
            val value = args[1].toInt()
            VarPlayerIntMapSetter.set(player, type, value)
            player.mes("Set varbit '${args[0]}' to value: ${player.vars[type]}")
        }

    @OptIn(InternalApi::class)
    private fun Player.setStatLevels(level: Int) {
        val xp = PlayerSkillXPTable.getXPFromLevel(level)
        for (stat in ServerCacheManager.getStats().values) {
            val statInternal = RSCM.getReverseMapping(RSCMType.STAT, stat.id)

            val baseLevel = statMap.getBaseLevel(statInternal)
            val targetLevel = max(stat.minLevel, level)
            if (baseLevel > targetLevel) {
                statRevert(statInternal, targetLevel, xp)
                continue
            }
            val xpDelta = xp - statMap.getXP(statInternal)
            statMap.setCurrentLevel(statInternal, targetLevel.toByte())
            statAdvance(statInternal, xpDelta.toDouble(), rate = 1.0)
        }
    }

    // There is, by design, no helper function to decrease stat xp, as xp reduction is not a
    // standard operation in normal gameplay.
    @OptIn(InternalApi::class)
    private fun Player.statRevert(stat: String, targetLevel: Int, targetXp: Int) {
        statMap.setCurrentLevel(stat, statMap.getBaseLevel(stat))
        val levelDelta = stat(stat) - targetLevel
        require(levelDelta > 0) { "This function can only be used to reduce stat levels." }
        statMap.setXP(stat, targetXp)
        statMap.setBaseLevel(stat, targetLevel.toByte())
        statSub(stat, constant = levelDelta, percent = 0)
        appearance.combatLevel = PlayerSkillXP.calculateCombatLevel(this)
        PlayerInterfaceUpdates.updateCombatLevel(this)
    }

    private fun reboot(cheat: Cheat) {
        logger.info { "Reboot initiated by '${cheat.player.username}'." }
        SafeServiceExit.terminate()
    }

    private fun slowReboot(cheat: Cheat) =
        with(cheat) {
            val cycles = min(args[0].toInt(), 65535)
            if (cycles <= 0) {
                update.clear()
                return@with
            }
            update.startCountdown(cycles)
            for (p in playerList) {
                MiscOutput.updateRebootTimer(p, cycles)
            }
        }

    private fun resolveArgTypeId(arg: String, names: Map<String, Int>): Int? {
        val argAsInt = arg.toIntOrNull()
        if (argAsInt != null) {
            return argAsInt
        }
        val sanitized = arg.replace("-", "_")
        return names[sanitized]
    }

    private fun resolveTypeName(name: String, names: Map<String, Int>): String =
        when {
            name in names -> name
            name.toIntOrNull() != null -> name
            else -> findClosestNameMatch(name, names.keys) ?: name
        }

    private fun List<String>.asTypeNameAndNumber(defaultNumber: Number): Pair<String, String> =
        if (size > 1 && last().toLongOrNull() != null) {
            dropLast(1).joinToString("_") to last()
        } else {
            joinToString("_") to defaultNumber.toString()
        }

    private fun List<String>.asTypeName(): String = joinToString("_")

    private fun findClosestNameMatch(input: String, names: Iterable<String>): String? {
        val normalizedInput = input.replace("_", " ")

        var bestMatchScore = 0.0f
        var bestMatchName: String? = null
        for (name in names) {
            val score = levenshteinMetric.compare(normalizedInput, name.replace("_", " "))
            if (score > bestMatchScore) {
                bestMatchScore = score
                bestMatchName = name
            }
        }

        return if (bestMatchScore >= 0.5) bestMatchName else null
    }
}
