package org.rsmod.api.db.jdbc

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ForeignPostmasterPidTest {
    @TempDir lateinit var dataDir: Path

    private fun writePid(recordedDir: String) {
        Files.writeString(
            dataDir.resolve("postmaster.pid"),
            "21384\n$recordedDir\n1793827056\n61079\n\nlocalhost\n",
        )
    }

    @Test
    fun `a lock copied from another folder is removed`() {
        writePid("C:/Users/rober/IdeaProjects/OpenRune-Server/.data/postgres")
        assertEquals(
            "C:/Users/rober/IdeaProjects/OpenRune-Server/.data/postgres",
            ForeignPostmasterPid.clearIfForeign(dataDir),
        )
        assertFalse(Files.exists(dataDir.resolve("postmaster.pid")))
    }

    @Test
    fun `this folder's own lock is kept, whatever its slashes`() {
        writePid(dataDir.toAbsolutePath().toString().replace(BACKSLASH, '/') + "/")
        assertNull(ForeignPostmasterPid.clearIfForeign(dataDir))
        assertTrue(Files.exists(dataDir.resolve("postmaster.pid")))
    }

    @Test
    fun `no lock file is fine`() {
        assertNull(ForeignPostmasterPid.clearIfForeign(dataDir))
    }

    @Test
    fun `a lock without a directory line is left alone`() {
        Files.writeString(dataDir.resolve("postmaster.pid"), "21384\n")
        assertNull(ForeignPostmasterPid.clearIfForeign(dataDir))
        assertTrue(Files.exists(dataDir.resolve("postmaster.pid")))
    }

    private companion object {
        const val BACKSLASH = 92.toChar()
    }
}
