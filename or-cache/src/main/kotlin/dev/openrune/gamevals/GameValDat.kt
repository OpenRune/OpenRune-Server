package dev.openrune.gamevals

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object GameValDat {

    fun read(file: File): Map<String, List<String>> {
        if (!file.exists()) return emptyMap()

        val tables = LinkedHashMap<String, List<String>>()
        DataInputStream(FileInputStream(file).buffered()).use { input ->
            repeat(input.readInt()) {
                val nameBytes = ByteArray(input.readShort().toInt())
                input.readFully(nameBytes)
                val table = String(nameBytes, Charsets.UTF_8)

                val entries = ArrayList<String>()
                repeat(input.readInt()) {
                    val entryBytes = ByteArray(input.readShort().toInt())
                    input.readFully(entryBytes)
                    entries += String(entryBytes, Charsets.UTF_8)
                }
                tables[table] = entries
            }
        }
        return tables
    }

    fun write(file: File, tables: Map<String, List<String>>) {
        file.parentFile?.mkdirs()
        DataOutputStream(FileOutputStream(file).buffered()).use { out ->
            out.writeInt(tables.size)
            tables.forEach { (table, entries) ->
                val nameBytes = table.toByteArray(Charsets.UTF_8)
                out.writeShort(nameBytes.size)
                out.write(nameBytes)

                out.writeInt(entries.size)
                entries.forEach { entry ->
                    val bytes = entry.toByteArray(Charsets.UTF_8)
                    out.writeShort(bytes.size)
                    out.write(bytes)
                }
            }
        }
    }
}

object GeneratedGameVals {

    const val FILE_NAME: String = "gamevals_generated.dat"

    val file: File = File("../.data/gamevals-binary/$FILE_NAME")

    fun read(): Map<String, List<String>> = GameValDat.read(file)

    fun replaceTables(tables: Map<String, List<String>>) {
        if (tables.isEmpty()) return
        val merged = LinkedHashMap(read())
        merged.putAll(tables)
        GameValDat.write(file, merged)
    }
}
