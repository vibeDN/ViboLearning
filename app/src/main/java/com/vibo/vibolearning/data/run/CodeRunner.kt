package com.vibo.vibolearning.data.run

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class RunResult(val output: String, val ok: Boolean, val source: Source) {
    enum class Source { ONLINE, CANNED, UNAVAILABLE }
}

/**
 * Runs a code sample. There is no on-device C++ toolchain (that would mean
 * bundling clang + headers, ~100 MB), so execution goes through a public
 * compiler service; when it's unreachable we fall back to the sample's
 * pre-recorded [cannedOutput] so lessons still work offline.
 */
interface CodeRunner {
    suspend fun run(language: String, code: String, cannedOutput: String): RunResult
}

/** Never touches the network — always returns the recorded output. */
class CannedRunner : CodeRunner {
    override suspend fun run(language: String, code: String, cannedOutput: String): RunResult =
        if (cannedOutput.isBlank()) {
            RunResult("Для этого примера нет сохранённого вывода.", ok = false, RunResult.Source.UNAVAILABLE)
        } else {
            RunResult(cannedOutput, ok = true, RunResult.Source.CANNED)
        }
}

/** Online compile via wandbox.org, with a canned fallback on any failure. */
class WandboxRunner(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val fallback: CodeRunner = CannedRunner(),
) : CodeRunner {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun run(language: String, code: String, cannedOutput: String): RunResult =
        withContext(Dispatchers.IO) {
            runCatching { compile(language, code) }.getOrNull()
                ?: fallback.run(language, code, cannedOutput)
        }

    private fun compile(language: String, code: String): RunResult {
        val (compiler, options) = toolchain(language)
        val payload = json.encodeToString(
            CompileRequest.serializer(),
            CompileRequest(code = code, compiler = compiler, options = options),
        )
        val request = Request.Builder()
            .url("https://wandbox.org/api/compile.json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("wandbox ${response.code}")
            val body = response.body?.string().orEmpty()
            val result = json.decodeFromString(CompileResponse.serializer(), body)
            val text = buildString {
                result.compilerError?.takeIf { it.isNotBlank() }?.let { append(it); if (!it.endsWith("\n")) append('\n') }
                result.programOutput?.let { append(it) }
                result.programError?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append('\n'); append(it) }
            }.ifBlank { "(программа завершилась без вывода)" }
            return RunResult(text.trimEnd(), ok = result.status == "0", RunResult.Source.ONLINE)
        }
    }

    private fun toolchain(language: String): Pair<String, String> = when (language.lowercase()) {
        "c" -> "gcc-13.2.0" to "warning,c17"
        "python", "py" -> "cpython-3.12.0" to ""
        else -> "gcc-13.2.0" to "warning,gnu++17"
    }

    @Serializable
    private data class CompileRequest(
        val code: String,
        val compiler: String,
        val options: String,
        val save: Boolean = false,
    )

    @Serializable
    private data class CompileResponse(
        val status: String? = null,
        @kotlinx.serialization.SerialName("compiler_error") val compilerError: String? = null,
        @kotlinx.serialization.SerialName("program_output") val programOutput: String? = null,
        @kotlinx.serialization.SerialName("program_error") val programError: String? = null,
    )
}
