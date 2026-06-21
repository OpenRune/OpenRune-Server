package org.rsmod.content.quest.manager

import jakarta.inject.Inject
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class QuestRequirementScript
@Inject
constructor(
    private val requirements: QuestRequirementResolver,
) : PluginScript() {
    override fun ScriptContext.startup() {
        QuestRequirements.install(requirements.policy)
    }
}
