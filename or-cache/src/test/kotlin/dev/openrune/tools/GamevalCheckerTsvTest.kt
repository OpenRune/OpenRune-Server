package dev.openrune.tools

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GamevalCheckerTsvTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `finds gamevals in transport fields and conditions`() {
        val root = directory.toFile()
        root.resolve("transports.tsv").writeText(
            "# loc.comment\n" +
                "Open Door loc.door\t5 stat.agility\tobj.key=1\tvarbit.gate>2;varbit.ready=1\tvarp.quest=3\n",
        )

        val (scanned, references) = GamevalChecker.scan(root)
        assertEquals(1, scanned)
        assertEquals(
            listOf("loc.door", "stat.agility", "obj.key", "varbit.gate", "varbit.ready", "varp.quest"),
            references.map { it.key },
        )
        assertEquals(setOf(2), references.map { it.line }.toSet())
    }
}
