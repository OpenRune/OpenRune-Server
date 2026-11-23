import groovy.json.JsonSlurper
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.Executors
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "The remote name generator returns fresh names on each run.")
abstract class GenerateBotNames : DefaultTask() {
    @get:Input
    abstract val namesPerMode: Property<Int>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        namesPerMode.convention(500)
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun generate() {
        val count = namesPerMode.get()
        require(count > 0) { "The name count must be positive." }
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
        val executor = Executors.newFixedThreadPool(modes.size)
        val pools =
            try {
                modes.map { mode -> executor.submit<List<String>> { collect(client, mode, count) } }
                    .map { it.get() }
            } finally {
                executor.shutdownNow()
            }

        val target = outputFile.get().asFile.toPath()
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, "bot-names-", ".tsv.tmp")
        try {
            Files.newBufferedWriter(temporary).use { writer ->
                writer.write("mode\tname\n")
                for ((mode, names) in modes.zip(pools)) {
                    for (name in names) writer.write("$mode\t$name\n")
                }
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun collect(client: HttpClient, mode: String, count: Int): List<String> {
        val names = linkedMapOf<String, String>()
        val json = JsonSlurper()
        val request =
            HttpRequest.newBuilder(URI.create("$api?_sAccountType=$mode"))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Referer", generator)
                .build()
        repeat(count * 10) {
            val result = json.parseText(fetch(client, request, mode)) as? Map<*, *>
            val name = (result?.get("_sResult") as? String)?.trim()
                ?: error("Genr8rs did not return a name for $mode.")
            if (name.length <= 12 && validName.matches(name)) {
                names.putIfAbsent(name.lowercase(), name)
            }
            if (names.size == count) {
                logger.lifecycle("Collected $count bot names for $mode.")
                return names.values.sortedBy { it.lowercase() }
            }
        }
        error("Found only ${names.size} distinct valid names for $mode after ${count * 10} requests.")
    }

    private fun fetch(client: HttpClient, request: HttpRequest, mode: String): String {
        repeat(3) { attempt ->
            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                val status = response.statusCode()
                if (status == 200) return response.body()
                check(status != 429) { "Genr8rs rate-limited $mode. Retry the task later." }
                check(status >= 500) { "Genr8rs returned HTTP $status for $mode." }
                throw IOException("Genr8rs returned HTTP $status for $mode.")
            } catch (error: IOException) {
                if (attempt == 2) throw error
                Thread.sleep(1000L shl attempt)
            }
        }
        error("Unable to reach Genr8rs for $mode.")
    }

    private companion object {
        const val generator = "https://genr8rs.com/OldSchoolRunescape/NameGenerator"
        const val api = "https://genr8rs.com/api/Content/OldSchoolRunescape/NameGenerator"
        val modes = listOf("standard", "iron", "hciron", "uim")
        val validName = Regex("[A-Za-z0-9]+(?: [A-Za-z0-9]+)*")
    }
}
