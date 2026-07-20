package org.rsmod.api.repo

import jakarta.inject.Inject
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository

public class EntityDelayedProcess
@Inject
constructor(private val npcRepo: NpcRepository, private val objRepo: ObjRepository) {
    public fun process() {
        npcRepo.processDelayedAdd()
        objRepo.processDelayedAdd()
    }

    /**
     * Registers all due delayed NPC/obj map spawns immediately.
     *
     * Call after plugin scripts have started (so `onNpcSpawn` handlers exist) and before the
     * login gate opens, so players never see entities trickle in after joining.
     */
    public fun flush() {
        npcRepo.flushDelayedAdds()
        objRepo.flushDelayedAdds()
    }
}
