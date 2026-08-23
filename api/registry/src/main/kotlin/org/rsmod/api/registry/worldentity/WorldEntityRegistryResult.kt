package org.rsmod.api.registry.worldentity

import kotlin.contracts.contract
import org.rsmod.game.entity.WorldEntity

public fun WorldEntityRegistryResult.Add.isSuccess(): Boolean {
    contract { returns(true) implies (this@isSuccess is WorldEntityRegistryResult.Add.Success) }
    return this is WorldEntityRegistryResult.Add.Success
}

public fun WorldEntityRegistryResult.Delete.isSuccess(): Boolean {
    contract { returns(true) implies (this@isSuccess is WorldEntityRegistryResult.Delete.Success) }
    return this is WorldEntityRegistryResult.Delete.Success
}

public class WorldEntityRegistryResult {
    public sealed class Add {
        public data object Success : Add()

        public sealed class Failure : Add()

        public data object NoAvailableSlot : Failure()
    }

    public sealed class Delete {
        public data object Success : Delete()

        public sealed class Failure : Delete()

        public data object UnexpectedSlot : Failure()

        public data class ListSlotMismatch(val occupiedBy: WorldEntity?) : Failure()
    }
}
