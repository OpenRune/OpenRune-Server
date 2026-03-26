package org.alter.game.combat

import org.alter.game.model.EntityType
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeMap
import org.alter.game.model.entity.Pawn
import sun.misc.Unsafe
import java.lang.reflect.Field

/**
 * Minimal Pawn stub for unit tests. Uses Unsafe.allocateInstance to bypass
 * the Pawn(World) constructor so no real World is needed. Only valid for
 * tests that never invoke World-dependent behaviour on this object.
 */
abstract class TestPawn private constructor(world: World) : Pawn(world) {

    companion object {
        private val unsafe: Unsafe by lazy {
            val field: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            field.get(null) as Unsafe
        }

        /**
         * Creates a TestPawn instance without invoking any constructor,
         * avoiding the non-null World parameter check.
         * Initialises the `attr` field via reflection so combat attribute
         * operations work correctly.
         */
        fun create(): Pawn {
            val instance = unsafe.allocateInstance(StubPawn::class.java) as Pawn
            // Unsafe skips field initialisers — manually init attr so CombatSystem can use it
            val attrField = Pawn::class.java.getDeclaredField("attr")
            attrField.isAccessible = true
            attrField.set(instance, AttributeMap())
            return instance
        }
    }

    /**
     * Concrete inner implementation — never instantiated via normal constructor.
     */
    private class StubPawn(world: World) : TestPawn(world) {
        override val entityType: EntityType get() = EntityType.NPC
        override fun cycle() {}
        override fun isRunning(): Boolean = false
        override fun getSize(): Int = 1
        override fun getCurrentHp(): Int = 10
        override fun getMaxHp(): Int = 10
        override fun setCurrentHp(level: Int) {}
        override fun graphic(id: String, height: Int, delay: Int) {}
    }
}
