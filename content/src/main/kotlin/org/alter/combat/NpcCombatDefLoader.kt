package org.alter.combat

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.combat.NpcCombatDefRegistry
import org.alter.game.model.combat.NpcCombatDef
import org.alter.rscm.RSCM.getRSCM
import java.io.InputStream

/**
 * Loads bulk NPC combat definitions from [NPC_COMBAT_DEFS_RESOURCE] into
 * [NpcCombatDefRegistry] at startup.
 *
 * The TOML file contains an `[npcs.<rscmName>]` table for each NPC.
 * Only the fields present in a given table override [NpcCombatDef.DEFAULT];
 * everything else inherits the default value.
 *
 * Boss or otherwise custom NPCs should skip this loader entirely and register
 * their definitions directly via the [npcCombatDef] DSL in their own plugin.
 */
object NpcCombatDefLoader {

    private val logger = KotlinLogging.logger {}
    private const val NPC_COMBAT_DEFS_RESOURCE = "/org/alter/combat/npc_combat_defs.toml"

    private val tomlMapper = ObjectMapper(TomlFactory()).findAndRegisterModules()

    fun load() {
        val stream: InputStream = NpcCombatDefLoader::class.java.getResourceAsStream(NPC_COMBAT_DEFS_RESOURCE)
            ?: run {
                logger.warn { "NPC combat defs resource not found: $NPC_COMBAT_DEFS_RESOURCE" }
                return
            }

        val root: Map<String, Any?> = stream.use {
            tomlMapper.readValue(it, object : TypeReference<Map<String, Any?>>() {})
        }

        val npcsSection = root["npcs"] as? Map<*, *> ?: run {
            logger.warn { "No [npcs] section found in $NPC_COMBAT_DEFS_RESOURCE" }
            return
        }

        var loaded = 0
        var skipped = 0

        for ((rscmKeyAny, fieldsAny) in npcsSection) {
            val rscmKey = rscmKeyAny as? String ?: continue
            val fields = fieldsAny as? Map<*, *> ?: continue
            val fullName = "npcs.$rscmKey"

            val npcId = try {
                getRSCM(fullName)
            } catch (e: Exception) {
                logger.warn { "Skipping unknown RSCM name '$fullName': ${e.message}" }
                skipped++
                continue
            }

            val def = buildDef(fields, fullName)
            NpcCombatDefRegistry.register(npcId, def)
            loaded++
        }

        logger.info { "Loaded $loaded NPC combat defs from TOML ($skipped skipped — unknown RSCM names)." }
    }

    private fun buildDef(fields: Map<*, *>, @Suppress("UNUSED_PARAMETER") name: String): NpcCombatDef {
        fun int(key: String): Int? = (fields[key] as? Number)?.toInt()

        // combat_style is stored in the TOML for documentation purposes.
        // NpcCombatDef does not carry a combatStyle field; that is set on the
        // Npc entity directly. A future task can extend NpcCombatDef and wire it.

        return NpcCombatDef.DEFAULT.copy(
            hitpoints        = int("hitpoints")         ?: NpcCombatDef.DEFAULT.hitpoints,
            attack           = int("attack")            ?: NpcCombatDef.DEFAULT.attack,
            strength         = int("strength")          ?: NpcCombatDef.DEFAULT.strength,
            defence          = int("defence")           ?: NpcCombatDef.DEFAULT.defence,
            attackSpeed      = int("attack_speed")      ?: NpcCombatDef.DEFAULT.attackSpeed,
            aggressiveRadius = int("aggressive_radius") ?: NpcCombatDef.DEFAULT.aggressiveRadius,
        )
    }
}
