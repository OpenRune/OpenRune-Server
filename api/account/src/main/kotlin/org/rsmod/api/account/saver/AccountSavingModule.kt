package org.rsmod.api.account.saver

import org.rsmod.api.account.autosave.PlayerAutosaveOrchestrator
import org.rsmod.module.ExtendedModule
import org.rsmod.server.services.Service

public object AccountSavingModule : ExtendedModule() {
    override fun bind() {
        bindInstance<AccountSavingService>()
        bindInstance<PlayerAutosaveOrchestrator>()
        addSetBinding<Service>(AccountSavingService::class.java)
    }
}
