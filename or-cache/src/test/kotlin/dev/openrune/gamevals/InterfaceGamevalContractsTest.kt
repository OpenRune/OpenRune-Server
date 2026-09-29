package dev.openrune.gamevals

import java.nio.file.Files
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InterfaceGamevalContractsTest {
    @Test
    fun `collects component keys and ignores edit names`() {
        val root = Files.createTempDirectory("if3-contract")
        try {
            val file = root.resolve("sample.if3")
            file.writeText(
                """
                [[interface]]
                name = "interface.sample"
                width = 100
                height = 100

                [[component]]
                name = "close"
                type = "layer"

                [[component]]
                name = "title"
                type = "text"

                [[edit]]
                name = "not_a_component"
                x = 1
                """.trimIndent(),
            )

            assertEquals(
                setOf("sample:close", "sample:title"),
                InterfaceGamevalContracts.expectedComponents(root.toFile()),
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `fails when a declared component is absent from generated mappings`() {
        val root = Files.createTempDirectory("if3-contract")
        try {
            root.resolve("sample.if3").writeText(
                """
                [[interface]]
                name = "interface.sample"

                [[component]]
                name = "close"
                type = "layer"

                [[component]]
                name = "title"
                type = "text"
                """.trimIndent(),
            )

            val error =
                assertThrows<IllegalStateException> {
                    InterfaceGamevalContracts.verify(
                        root.toFile(),
                        listOf("sample:title=123"),
                    )
                }

            check(error.message.orEmpty().contains("component.sample:close"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `accepts a complete generated mapping set`() {
        val root = Files.createTempDirectory("if3-contract")
        try {
            root.resolve("sample.if3").writeText(
                """
                [[interface]]
                name = "interface.sample"

                [[component]]
                name = "close"
                type = "layer"
                """.trimIndent(),
            )

            InterfaceGamevalContracts.verify(
                root.toFile(),
                listOf("sample:close=72155140"),
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
