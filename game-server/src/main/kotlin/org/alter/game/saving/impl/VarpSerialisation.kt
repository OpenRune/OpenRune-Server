package org.alter.game.saving.impl

import org.alter.game.model.entity.Client
import org.alter.game.saving.DocumentHandler
import org.alter.rscm.RSCM.getRSCM
import org.bson.Document

class VarpSerialisation(override val name: String = "varps") : DocumentHandler {

    override fun fromDocument(client: Client, doc: Document) = doc.forEach { stateKey, idValue ->
        // Handle special case for run varp saved as "run_varp" (always persisted)
        if (stateKey == "run_varp") {
            val runVarpId = getRSCM("varp.option_run")
            idValue?.toString()?.toIntOrNull()?.let { state ->
                client.varps.setState(runVarpId, state)
                return@forEach
            }
        }

        // Handle normal varp format
        stateKey.toIntOrNull()?.let { state ->
            idValue?.toString()?.toIntOrNull()?.let { id ->
                client.varps.setState(id, state)
            }
        }
    }

    override fun asDocument(client: Client): Document {
        return Document().apply {
            val runVarpId = getRSCM("varp.option_run")
            // Save all varps with non-zero state (excluding run varp which is saved separately)
            val allVarps = client.varps.getAll()
            putAll(allVarps
                .filter { it.state != 0 && it.id != runVarpId }
                .associate { it.state.toString() to it.id.toString() })

            // Always save the run varp (option_run) regardless of state, so the setting persists
            val runVarpState = client.varps.getState(runVarpId)
            put("run_varp", runVarpState.toString())
        }
    }

}